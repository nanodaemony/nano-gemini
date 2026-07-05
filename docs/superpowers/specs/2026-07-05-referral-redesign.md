# 邀请与奖励系统重构设计

## 概述

将现有邀请系统从「单条邀请关系 + 支付时实时结算」重构为「邀请事件流水 + 每日批量结算」模型。普通用户和机构用户的邀请奖励统一为会员天数，代理机构的佣金结算留待后续。

## 动机

- 现有 `settleReferralReward()` 从未被调用，实时结算与需求方向相反
- 缺少里程碑追踪（注册 vs 订阅）
- 缺少定时批量结算机制
- 社交登录不支持邀请码
- 机构间邀请逻辑不完整

## 奖励规则矩阵

每日 JOB 扫描所有 `reward_status = 'PENDING'` 的记录，按邀请人聚合计算：

| 邀请人类型 | 被邀请对象 | 事件 | 奖励 |
|-----------|-----------|------|------|
| NORMAL | 普通用户 | REGISTER | 邀请人 +1 天 |
| NORMAL | 普通用户 | SUBSCRIBE | 邀请人 +10 天 |
| NORMAL | 机构 | REGISTER | 邀请人 +10 天 |
| NORMAL | 机构 | SUBSCRIBE | 邀请人 +100 天 |
| INSTITUTION | 普通用户 | REGISTER | 每满 100 个 → 全员 +1 天 |
| INSTITUTION | 普通用户 | SUBSCRIBE | 每满 10 个 → 全员 +1 天 |
| INSTITUTION | 机构 | REGISTER | 每个 → 全员 +1 天 |
| INSTITUTION | 机构 | SUBSCRIBE | 每个 → 全员 +10 天 |

- NORMAL/AGENT：奖励发放给邀请人个人
- INSTITUTION：奖励发放给邀请机构所有成员（`grid_user.org_id = referrer_org_id`）
- 机构阈值奖励：不足阈值的记录保留 PENDING，累积到后续周期

## 数据模型

### referral_record 表（重建）

```sql
DROP TABLE IF EXISTS `referral_record`;

CREATE TABLE `referral_record` (
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `referrer_id`     BIGINT NOT NULL COMMENT '邀请人用户ID',
    `referrer_type`   VARCHAR(20) NOT NULL COMMENT 'NORMAL / INSTITUTION / AGENT',
    `referrer_org_id` INT COMMENT '邀请人所属机构ID',
    `referred_id`     BIGINT COMMENT '被邀请人用户ID',
    `referred_org_id` INT COMMENT '被邀请机构ID',
    `referral_code`   VARCHAR(32) NOT NULL COMMENT '使用的邀请码',
    `event_type`      VARCHAR(30) NOT NULL COMMENT 'REGISTER / SUBSCRIBE',
    `order_id`        BIGINT COMMENT '关联订单ID（SUBSCRIBE 事件）',
    `reward_status`   VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SETTLED',
    `settle_time`     DATETIME COMMENT '结算时间',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_referrer` (`referrer_id`),
    KEY `idx_referrer_org` (`referrer_org_id`),
    KEY `idx_referred` (`referred_id`),
    KEY `idx_reward_status` (`reward_status`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请记录表（事件模型）';
```

相比旧表的变更：
- 新增 `event_type`、`referrer_org_id`、`referred_org_id`
- 移除 `reward_amount`、`reward_type`（奖励规则由 JOB 矩阵决定，不存记录）
- `reward_status` 简化：PENDING / SETTLED（移除 PAID）
- 新增 `idx_reward_status`、`idx_event_type`、`idx_create_time` 索引

## 事件写入点

### REGISTER 事件

| 场景 | 位置 | 变更 |
|------|------|------|
| 邮箱注册 | `AppAuthServiceImpl.register()` | 调整调用 `recordEvent()` |
| 社交登录注册 | `AppAuthServiceImpl.createSocialUser()` | 新增 referralCode 参数支持 |
| 机构审批通过 | `OrganizationServiceImpl.approve()` | 调整调用 `recordEvent()`，传入 referred_org_id |

### SUBSCRIBE 事件

| 场景 | 位置 | 变更 |
|------|------|------|
| 支付回调 | `PaymentServiceImpl.handlePaymentCallback()` | 新增：查找该用户的 REGISTER 记录，写入 SUBSCRIBE 事件 |

SUBSCRIBE 事件写入逻辑：
1. 支付成功后，查找该用户最近的 REGISTER 类型 ReferralRecord
2. 如果存在（说明是通过邀请码注册的），以相同 referrer 信息写入一条 event_type=SUBSCRIBE 的记录
3. 如果用户是机构成员购买的，以机构维度查找

## 每日结算 JOB

```
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2:00
```

### 执行流程

1. 查询所有 `reward_status = 'PENDING'` 的记录
2. 按 `(referrer_id, referrer_type, referrer_org_id, event_type, referred_org_id是否为NULL)` 分组
3. 对每个分组按奖励矩阵计算应发天数：
   - NORMAL 类型：直接累计天数
   - INSTITUTION 邀请机构：每条直接换算
   - INSTITUTION 邀请普通用户：`floor(count / 阈值)`，剩余保留 PENDING
4. 调用 `entitlementService.grantEntitlements()` 发放：
   - 个人奖励：`grantEntitlements(referrerId, allEntitlementIds, "REFERRAL", batchId, totalDays, null)`
   - 机构全员奖励：遍历 `userRepository.findByOrgId(orgId)`，逐个调用
5. 将已结算的记录批量标记为 SETTLED，写入 `settle_time`

### 权益表交互

每日 JOB 聚合后一次性调用 `grantEntitlements()`，每个邀请人每天最多产生 6 条 `user_entitlement_record`（6 个权益各一条），而非每个邀请事件产生 6 条。`user_entitlement` 汇总表始终每用户 6 行，`expire_at` 被延后。

## 接口变更

### ReferralService 接口

```java
public interface ReferralService {
    /**
     * 记录邀请事件（注册/订阅等里程碑）
     */
    Long recordEvent(String referralCode, Long referredUserId, String eventType);

    /**
     * 记录邀请事件（机构场景）
     */
    Long recordEvent(String referralCode, Long referredUserId, String eventType, Integer referredOrgId);

    /**
     * 每日结算邀请奖励（由定时任务调用）
     */
    void settlePendingRewards();
}
```

删除：
- `processReferral()` → 改为 `recordEvent()`
- `settleReferralReward()` → 删除，不再需要

### SocialLoginDTO

新增字段：
```java
private String referralCode;  // 可选，邀请码
```

### AppReferralController

保留，后续可扩展查询接口（如查看我的邀请记录、邀请统计等），本期不做。

## 需要修改的文件

### 新增
- `ReferralSettlementJob.java` — 定时任务

### 修改
- `ReferralRecord.java` — 实体字段更新
- `ReferralRecordRepository.java` — 查询方法更新
- `ReferralService.java` — 接口重构
- `ReferralServiceImpl.java` — 实现重构
- `AppAuthServiceImpl.java` — register() + createSocialUser()
- `AppAuthController.java` — 社交登录接口参数
- `SocialLoginDTO.java` — 新增 referralCode
- `OrganizationServiceImpl.java` — approve() 调整 recordEvent 调用
- `PaymentServiceImpl.java` — handlePaymentCallback() 新增 SUBSCRIBE 事件写入

### 删除
- 旧 `ReferralRecord` 的 `rewardAmount`、`rewardType` 字段及相关逻辑

### SQL
- `app_ext.sql` — 更新 referral_record 建表语句

## 不纳入本期

- AGENT 佣金结算逻辑（AGENT 事件按 NORMAL 规则处理，发会员天数）
- AppReferralController 业务接口（查询邀请记录/统计等）
- 批量 grantEntitlements 优化（当前逐用户调用，机构规模 <500 人时可接受）
