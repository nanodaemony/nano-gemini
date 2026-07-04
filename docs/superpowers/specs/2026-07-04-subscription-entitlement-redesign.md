# 订阅与权益体系重构设计

> 日期：2026-07-04
> 状态：已定稿
> 涉及模块：grid-billing（重构）、grid-app（重构）、grid-common（扩展）
> 替代：2026-06-16-subscription-design.md、2026-06-22-tiered-pricing-entitlement-design.md

---

## 1. 概述

对现有订阅与权益体系进行重构。核心变化：

- **三表分离**：权益元数据（`entitlement`）+ 权益流水（`user_entitlement_record`）+ 权益汇总（`user_entitlement`），替代现有的单表 `entitlement_source`
- **汇总表为权威源**：鉴权直接查汇总表，不再每次实时堆叠计算
- **商品→权益映射**：商品表通过 `entitlement_ids` JSON 字段关联权益，替代 `product_module` 中间表
- **移除遗留模型**：废弃 `grid_user_role` / `@RequireSubscription` / `UserLevel` / `SubscriptionAspect`，统一使用 `@RequireProduct` + `EntitlementService`

### 1.1 业务场景

- 普通用户注册 → 获得 7 天全模块试用
- 机构用户注册 → 获得 30 天全模块试用
- 购买模块商品 → 获得对应模块使用权（有时效）
- "免费三个单词" 不走权益体系，由独立访问控制机制实现

---

## 2. 数据模型

### 2.1 权益元数据表 `entitlement`

定义「权益是什么」。一个权益对应一个业务模块的访问权限。

```sql
CREATE TABLE `entitlement` (
    `id`          INT AUTO_INCREMENT COMMENT '主键ID',
    `code`        VARCHAR(50) NOT NULL COMMENT '权益唯一标识, 如 VOCAB_ACCESS',
    `name`        VARCHAR(200) NOT NULL COMMENT '权益名称, 如"词汇模块使用权"',
    `module_code` VARCHAR(50) COMMENT '关联的业务模块, BizModuleEnum',
    `sort_order`  INT DEFAULT 0 COMMENT '排序',
    `status`      INT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `create_by`   VARCHAR(50) COMMENT '创建人',
    `update_by`   VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权益元数据表';
```

**种子数据：**

| code | name | module_code |
|------|------|-------------|
| VOCAB_ACCESS | 词汇模块使用权 | VOCAB |
| GRAMMAR_ACCESS | 语法模块使用权 | GRAMMAR |
| CHARACTER_ACCESS | 汉字模块使用权 | CHARACTER |
| CONFUSING_WORDS_ACCESS | 易混淆词辨析使用权 | CONFUSING_WORDS |
| CULTURE_ACCESS | 文化模块使用权 | CULTURE |
| TOPIC_ACCESS | 话题模块使用权 | TOPIC |

### 2.2 用户权益流水表 `user_entitlement_record`

每次权益发放的不可变记录，类似账本流水。

```sql
CREATE TABLE `user_entitlement_record` (
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `entitlement_id`  INT NOT NULL COMMENT '权益ID, 关联 entitlement.id',
    `source_type`     VARCHAR(30) NOT NULL COMMENT '来源类型: TRIAL / PURCHASE / ADMIN_GRANT',
    `source_id`       VARCHAR(100) COMMENT '来源业务ID, 如订单号',
    `duration_days`   INT NOT NULL COMMENT '本次获得的有效天数',
    `expire_at`       DATETIME COMMENT '本次原始到期时间 (granted_at + duration_days)',
    `region`          VARCHAR(10) COMMENT '授予时的区域',
    `remark`          VARCHAR(500) COMMENT '备注',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_entitlement` (`user_id`, `entitlement_id`),
    KEY `idx_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权益流水表';
```

### 2.3 用户权益汇总表 `user_entitlement`

按 `user_id + entitlement_id` 唯一，汇总堆叠后的最终到期时间。鉴权时直接查此表。

```sql
CREATE TABLE `user_entitlement` (
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `entitlement_id`  INT NOT NULL COMMENT '权益ID',
    `expire_at`       DATETIME COMMENT '堆叠后的最终到期时间',
    `status`          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / EXPIRED',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次获得时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_entitlement` (`user_id`, `entitlement_id`),
    KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权益汇总表';
```

### 2.4 商品表 `grid_product`

```sql
ALTER TABLE `grid_product`
    ADD COLUMN `entitlement_ids` VARCHAR(500) COMMENT '购买后获得的权益ID列表, JSON数组 ["VOCAB_ACCESS","GRAMMAR_ACCESS"]',
    ADD COLUMN `institution_config` VARCHAR(500) COMMENT '机构商品配置, JSON {"maxMembers":30,"maxAdmins":1}, 普通商品为NULL',
    ADD COLUMN `cover_image` VARCHAR(500) COMMENT '封面图URL';
```

**种子数据：**

```sql
-- PLUS 商品包含全部 6 个权益
('PLUS', '全平台Plus会员', 'PLUS',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 NULL, NULL, 1, 1),

-- 单模块
('VOCAB', '词汇模块', 'SINGLE_MODULE', '["VOCAB_ACCESS"]', NULL, NULL, 2, 1),
('GRAMMAR', '语法模块', 'SINGLE_MODULE', '["GRAMMAR_ACCESS"]', NULL, NULL, 3, 1),
('CHARACTER', '汉字模块', 'SINGLE_MODULE', '["CHARACTER_ACCESS"]', NULL, NULL, 4, 1),
('CONFUSING_WORDS', '易混淆词辨析模块', 'SINGLE_MODULE', '["CONFUSING_WORDS_ACCESS"]', NULL, NULL, 5, 1),
('CULTURE', '文化模块', 'SINGLE_MODULE', '["CULTURE_ACCESS"]', NULL, NULL, 6, 1),
('TOPIC', '话题模块', 'SINGLE_MODULE', '["TOPIC_ACCESS"]', NULL, NULL, 7, 1),

-- 机构商品
('INST_STARTER', 'Institution Starter', 'INSTITUTION',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 '{"maxMembers":30,"maxAdmins":1}', NULL, 10, 1),
('INST_BASIC', 'Institution Basic', 'INSTITUTION',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 '{"maxMembers":100,"maxAdmins":2}', NULL, 11, 1),
('INST_PRO', 'Institution Pro', 'INSTITUTION',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 '{"maxMembers":500,"maxAdmins":5}', NULL, 12, 1);
```

### 2.5 区域定价表 `region_pricing`

结构和现有基本一致，保持。

```sql
CREATE TABLE `region_pricing` (
    `id`              INT AUTO_INCREMENT PRIMARY KEY,
    `product_id`      INT NOT NULL,
    `region`          VARCHAR(10) NOT NULL COMMENT 'A/B/C/D/E',
    `billing_cycle`   VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY/YEARLY',
    `price`           DECIMAL(12,2) NOT NULL,
    `currency`        VARCHAR(10) NOT NULL COMMENT 'USD/EUR/CNY',
    `status`          INT NOT NULL DEFAULT 1,
    `create_by`       VARCHAR(50), `update_by` VARCHAR(50),
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_product_region_cycle` (`product_id`, `region`, `billing_cycle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域定价表';
```

### 2.6 订阅记录表 `payment_subscription`

轻量化，仅记录与支付渠道的关联关系。

```sql
CREATE TABLE `payment_subscription` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT NOT NULL,
    `product_code`    VARCHAR(50) NOT NULL COMMENT '订阅的商品代码',
    `channel`         VARCHAR(30) NOT NULL COMMENT 'STRIPE / PHOTONPAY',
    `channel_sub_id`  VARCHAR(200) COMMENT '渠道侧订阅ID',
    `status`          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / CANCELLED / EXPIRED',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `cancel_at`       DATETIME COMMENT '取消时间',
    KEY `idx_user` (`user_id`),
    KEY `idx_channel_sub` (`channel`, `channel_sub_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅记录表';
```

扣款周期、金额、下次扣款时间全归 Stripe/PhotonPay 管。Webhook 通知扣款成功时，根据 `channel_sub_id` 查找 → 查商品包含的权益 → 逐条写流水+更新汇总。

### 2.7 用户表扩展 `grid_user`

```sql
ALTER TABLE `grid_user`
    ADD COLUMN `country` VARCHAR(50) COMMENT '注册国家' AFTER `avatar`,
    ADD COLUMN `region` VARCHAR(10) COMMENT '区域 A/B/C/D/E' AFTER `country`;
```

### 2.8 订单表 `grid_order`

保持现有结构不变。

### 2.9 支付流水表 `payment_record`

保持现有结构不变。

---

## 3. 枚举

### 3.1 BizModuleEnum（新增）

```java
public enum BizModuleEnum {
    VOCAB("VOCAB", "词汇"),
    GRAMMAR("GRAMMAR", "语法"),
    CHARACTER("CHARACTER", "汉字"),
    CONFUSING_WORDS("CONFUSING_WORDS", "易混淆词辨析"),
    CULTURE("CULTURE", "文化"),
    TOPIC("TOPIC", "话题");
}
```

位置：`grid-common/src/main/java/com/naon/grid/enums/BizModuleEnum.java`

### 3.2 保留的枚举

- `ProductTypeEnum` — PLUS / SINGLE_MODULE / INSTITUTION / ENTERPRISE，保持不变
- `BillingCycleEnum` — MONTHLY / QUARTERLY / YEARLY，保持不变
- `EntitlementStatusEnum` — ACTIVE / REVOKED / EXPIRED，用于 `user_entitlement.status`

---

## 4. 核心逻辑

### 4.1 发放权益（grantEntitlements）

```
输入：userId, entitlementIds[], sourceType, sourceId, durationDays, region, remark

同一事务内，对每个 entitlementId：
  1. INSERT INTO user_entitlement_record
  2. UPSERT INTO user_entitlement：
     - 查询现有记录：SELECT * WHERE user_id=? AND entitlement_id=?
     - 无记录：
         INSERT (expire_at = NOW() + durationDays)
     - 有记录：
         cursor = MAX(现有expire_at, NOW())
         UPDATE expire_at = cursor + durationDays
```

**注册时调用：**

```
普通用户注册：
  grantEntitlements(userId,
    [VOCAB_ACCESS, GRAMMAR_ACCESS, CHARACTER_ACCESS,
     CONFUSING_WORDS_ACCESS, CULTURE_ACCESS, TOPIC_ACCESS],
    'TRIAL', null, 7, region)

机构用户注册（审核通过后）：
  grantEntitlements(userId,
    [VOCAB_ACCESS, GRAMMAR_ACCESS, CHARACTER_ACCESS,
     CONFUSING_WORDS_ACCESS, CULTURE_ACCESS, TOPIC_ACCESS],
    'TRIAL', null, 30, region)
```

**支付成功后调用：**

```
支付回调成功：
  1. 根据商品 code 查询 GridProduct
  2. 解析 entitlement_ids JSON → 权益ID 列表
  3. 从 region_pricing 获取对应 billingCycle 的天数
  4. grantEntitlements(userId, entitlementIds, 'PURCHASE', orderNo, days, region)
```

### 4.2 鉴权（hasModuleAccess）

```
输入：userId, moduleCode

1. 根据 moduleCode 查 entitlement 表 → entitlementId
2. 查 user_entitlement 表：
   SELECT * WHERE user_id=? AND entitlement_id=? AND expire_at > NOW()
3. 有记录且 status=ACTIVE → true，否则 → false
```

不再需要 PLUS 通配逻辑——PLUS 商品在购买时已将全部 6 个权益分别写入汇总表，每个模块独立判断即可。

### 4.3 检查是否领取过试用

```
SELECT COUNT(*) FROM user_entitlement_record
WHERE user_id=? AND source_type='TRIAL'
```

如果 `COUNT > 0`，说明已领过试用。

---

## 5. 数据流

### 5.1 注册 → 试用权益

```
用户注册 (country=US → region=A)
  → 创建 grid_user
  → 检查 source_type='TRIAL' 是否存在 → 不存在
  → grantEntitlements(6个权益, TRIAL, 7天)
  → 写入 user_entitlement_record(6条) + user_entitlement(6条)
```

### 5.2 购买 → 支付 → 权益

```
下单 POST /api/app/orders/create (productCode=PLUS, billingCycle=YEARLY)
  → 从 JWT 获取 region=A
  → 查 region_pricing: product=PLUS, region=A, cycle=YEARLY → $99.99
  → 创建 grid_order (PENDING)

支付回调 (Webhook)
  → 更新 grid_order → PAID
  → 创建 payment_record
  → 查询 grid_product.entitlement_ids → [VOCAB_ACCESS, ..., 6个]
  → grantEntitlements(userId, 6个权益, PURCHASE, orderNo, 365天)
  → 如果有 payment_subscription → 更新其状态
```

### 5.3 鉴权

```
请求 GET /api/app/vocab/word/123 (@RequireProduct("VOCAB"))
  → ProductAccessAspect 拦截
  → hasModuleAccess(userId, "VOCAB")
  → 查 entitlement: module_code=VOCAB → id=1 (VOCAB_ACCESS)
  → 查 user_entitlement: user_id + entitlement_id=1 → expire_at > NOW() ✓
  → 放行
```

---

## 6. 需要移除的内容

| 移除项 | 说明 |
|--------|------|
| `entitlement_source` 表 + 实体 + Repository | 被 `user_entitlement_record` + `user_entitlement` 替代 |
| `product_module` 表 + 实体 + Repository | 商品→权益映射改为 `grid_product.entitlement_ids` JSON |
| `grid_user_role` 表 + 实体 + Repository | 遗留 VIP/SVIP 角色模型 |
| `UserLevel` 枚举 | SIP/VIP 级别不再使用 |
| `@RequireSubscription` 注解 | 被 `@RequireProduct` 替代 |
| `SubscriptionAspect` | 被 `ProductAccessAspect` 替代 |
| `SubscriptionService` + `SubscriptionServiceImpl` | 被 `EntitlementService` 替代 |
| `AppSubscriptionController` 相关端点 | 订阅查询端点重新设计 |
| `EntitlementEngine` 接口 + `EntitlementEngineImpl` | 重构为 `EntitlementService`，不再实时堆叠 |

---

## 7. 模块文件组织

### 7.1 grid-billing（重构）

```
com.naon.grid.modules.billing/
├── domain/
│   ├── GridProduct.java          # 扩展 entitlementIds, institutionConfig, coverImage
│   ├── Entitlement.java          # [新增] 权益元数据
│   ├── UserEntitlementRecord.java # [新增] 权益流水
│   ├── UserEntitlement.java      # [新增] 权益汇总
│   ├── RegionPricing.java        # 保持
│   ├── GridOrder.java            # 保持
│   ├── PaymentRecord.java        # 保持
│   └── PaymentSubscription.java  # 精简
├── repository/
│   ├── GridProductRepository.java
│   ├── EntitlementRepository.java        # [新增]
│   ├── UserEntitlementRecordRepository.java # [新增]
│   ├── UserEntitlementRepository.java    # [新增]
│   ├── RegionPricingRepository.java
│   ├── GridOrderRepository.java
│   ├── PaymentRecordRepository.java
│   └── PaymentSubscriptionRepository.java
├── service/
│   ├── EntitlementService.java           # [新增, 替代 EntitlementEngine]
│   ├── OrderService.java
│   ├── PaymentService.java
│   └── ProductService.java
├── service/impl/
│   ├── EntitlementServiceImpl.java
│   ├── OrderServiceImpl.java
│   ├── PaymentServiceImpl.java
│   └── ProductServiceImpl.java
└── service/dto/
    ├── EntitlementResult.java
    ├── OrderCreateRequest.java
    ├── OrderCreateResponse.java
    └── ...
```

### 7.2 grid-app（重构）

```
com.naon.grid.modules.app/
├── annotation/
│   └── RequireProduct.java       # 保持
├── aspect/
│   └── ProductAccessAspect.java  # 改为调用 EntitlementService
├── rest/
│   ├── AppOrderController.java
│   ├── AppSubscriptionController.java  # 重新设计
│   └── PaymentWebhookController.java
└── service/
    └── ...                       # 移除 SubscriptionService
```

---

## 8. 需清理的遗留文件

| 文件 | 路径 |
|------|------|
| EntitlementSource.java | grid-billing/.../domain/ |
| EntitlementSourceRepository.java | grid-billing/.../repository/ |
| EntitlementEngine.java | grid-billing/.../service/ |
| EntitlementEngineImpl.java | grid-billing/.../service/impl/ |
| ProductModule.java | grid-billing/.../domain/ |
| ProductModuleRepository.java | grid-billing/.../repository/ |
| GridUserRole.java | grid-app/.../domain/ |
| GridUserRoleRepository.java | grid-app/.../repository/ |
| UserLevel.java | grid-common/.../enums/ |
| RequireSubscription.java | grid-common/.../annotation/ |
| SubscriptionAspect.java | grid-app/.../aspect/ |
| SubscriptionService.java | grid-app/.../service/ |
| SubscriptionServiceImpl.java | grid-app/.../service/impl/ |
| EntitlementEngineImplTest.java | grid-billing/.../test/ |
