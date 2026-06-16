# Subscription (会员订阅) 设计文档

> **日期**: 2026-06-16
> **状态**: 已定稿
> **涉及模块**: grid-app (APP 端), grid-common (注解/枚举)

---

## 1. 概述

为 APP 端用户增加订阅付费功能，只有订阅用户才能访问特定内容。支付系统独立于本设计——会员体系只消费支付结果。

### 核心设计原则

- **最小改动**：不新建表，不加字段，完全复用现有 `grid_user_role` 表
- **注解驱动**：通过 `@RequireSubscription` 注解声明式控制接口访问权限
- **层级兼容**：SVIP 自动包含 VIP 权限，无需为高级别用户重复标注低级别注解
- **支付解耦**：支付系统只需通知"用户 X 购买了 Y 级别 N 天"，会员系统负责写入角色和过期时间

---

## 2. 数据库设计

### 零表变更

直接使用现有 `grid_user_role` 表，不做任何结构修改：

```sql
CREATE TABLE `grid_user_role` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `role_code`   VARCHAR(30) NOT NULL COMMENT '角色编码：NORMAL/VIP/SVIP',
    `role_name`   VARCHAR(50) NOT NULL COMMENT '角色名称：普通用户/VIP会员/SVIP会员',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间（会员用，NULL=永久有效）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_code`),
    KEY `idx_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP用户角色表';
```

### 角色数据模型

| 用户情况 | `role_code` 记录 |
|---------|-----------------|
| 普通用户 | `NORMAL`（`expire_time`=NULL） |
| VIP 会员 | `NORMAL` + `VIP`（`expire_time`=到期日） |
| SVIP 会员 | `NORMAL` + `SVIP`（`expire_time`=到期日） |
| VIP→SVIP 升级 | `NORMAL` + `VIP` + `SVIP`（旧 VIP 条目保留不动） |
| 会员过期 | `NORMAL`（过期角色记录保留，逻辑删除） |

**注意**：SVIP 用户**不**自动持有 `VIP` 角色。SVIP 能访问 VIP 内容是通过注解的层级逻辑（`UserLevel.includes()`）实现的，不是通过双重角色。

---

## 3. 注解与枚举

### 3.1 `UserLevel` 枚举

```java
package com.naon.grid.enums;

/**
 * 会员级别。级别数值越高，权限越大。
 * VIP=1, SVIP=2 且 SVIP.includes(VIP)=true
 */
public enum UserLevel {
    VIP(1),
    SVIP(2);

    private final int level;

    UserLevel(int level) {
        this.level = level;
    }

    /**
     * 当前级别是否包含 other 级别的权限。
     * 例：SVIP.includes(VIP) → true
     */
    public boolean includes(UserLevel other) {
        return this.level >= other.level;
    }
}
```

### 3.2 `@RequireSubscription` 注解

```java
package com.naon.grid.annotation;

import com.naon.grid.enums.UserLevel;
import java.lang.annotation.*;

/**
 * 标注在 Controller 方法上，表示该接口需要指定级别的会员订阅才能访问。
 * 由 SubscriptionAspect 切面处理鉴权逻辑。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSubscription {
    UserLevel value() default UserLevel.VIP;
}
```

**使用示例：**

```java
@RequireSubscription(UserLevel.VIP)
@GetMapping("/api/app/vocab/word/{id}")
public ResponseEntity<> getWordDetail(@PathVariable Integer id) { ... }

@RequireSubscription(UserLevel.SVIP)
@GetMapping("/api/app/grammar/comparison")
public ResponseEntity<> getGrammarComparison() { ... }

// SVIP 用户可以访问上面两个接口
```

---

## 4. AOP 鉴权切面

### 4.1 鉴权流程

```
请求到达 → SubscriptionAspect 拦截
  ├─ 1. 从 SecurityContext 获取 AppAuthenticationToken
  ├─ 2. 从 token 的 roles 中快速检查是否有 VIP/SVIP
  │    ├─ 无 → 抛出 SUBSCRIPTION_REQUIRED(1200)
  │    └─ 有 → 继续
  ├─ 3. 查数据库验证 role 的 expire_time 是否有效
  │    ├─ 已过期 → 抛出 SUBSCRIPTION_EXPIRED(1201)
  │    └─ 有效 → 继续
  ├─ 4. 比较用户级别 vs 注解要求的级别
  │    ├─ 用户级别不够 → 抛出 SUBSCRIPTION_REQUIRED(1200)
  │    └─ 满足 → 放行
  └─ 5. 执行原始方法
```

### 4.2 新增数据库查询方法

在 `GridUserRoleRepository` 中新增：

```java
/**
 * 查询用户当前有效的最高会员级别
 * 规则：expire_time 为 NULL（永久）或 > 当前时间
 */
@Query("SELECT r.roleCode FROM GridUserRole r " +
       "WHERE r.userId = :userId " +
       "AND r.roleCode IN ('VIP', 'SVIP') " +
       "AND (r.expireTime IS NULL OR r.expireTime > :now)")
List<String> findValidSubscriptionRoles(@Param("userId") Long userId,
                                        @Param("now") Date now);
```

### 4.3 错误码

在 `AppErrorCode` 中新增：

```java
SUBSCRIPTION_REQUIRED(1200, "需要订阅后才能访问此内容"),
SUBSCRIPTION_EXPIRED(1201, "订阅已过期，请续费"),
```

---

## 5. 试用机制

### 5.1 注册自动送试用

修改 `AppAuthServiceImpl.register()`，在创建 NORMAL 角色后，追加：

```java
// 如果配置了试用天数 > 0
if (subscriptionProperties.getTrialDays() > 0) {
    GridUserRole trialRole = new GridUserRole();
    trialRole.setUserId(user.getId());
    trialRole.setRoleCode("VIP");
    trialRole.setRoleName("VIP会员");
    trialRole.setExpireTime(
        DateUtils.addDays(new Date(), subscriptionProperties.getTrialDays())
    );
    userRoleRepository.save(trialRole);
}
```

试用天数通过配置控制：

```yaml
app:
  subscription:
    trial-days: 7          # 设为 0 关闭自动试用
```

### 5.2 后台手动发放（预留）

V1 不在后台管理中实现 UI 界面，但预留接口位置供后续开发：

```
POST /api/admin/subscription/grant
Body: { userId, level: "VIP", days: 30 }
```

---

## 6. 订阅查询 API

所有用户（含未订阅用户）均可访问：

```
GET /api/app/subscription/my

Response 200:
{
  "level": "VIP",              // NORMAL / VIP / SVIP
  "expireTime": "2026-07-16T00:00:00Z",  // null 表示未订阅或仅 NORMAL
  "expiringSoon": false        // 15天内即将到期
}
```

### 6.1 响应 DTO

```java
@Data
public class AppSubscriptionVO {
    private String level;          // "NORMAL" / "VIP" / "SVIP"
    private Date expireTime;       // null = 无订阅
    private boolean expiringSoon;  // expireTime 在 15 天内
}
```

---

## 7. 升级逻辑（VIP → SVIP）

当 VIP 用户购买 SVIP：

1. 在 `grid_user_role` 写入新条目：`role_code = "SVIP"`，`expire_time = 新到期时间`
2. 已有的 `VIP` 角色记录**保留不动**（不影响鉴权，JWT 刷新后不再携带过期角色）
3. 升级时 **不折算** VIP 的剩余时长，SVIP 从购买日开始算新周期（V1 简化策略）

---

## 8. 订阅购买接口（对接支付系统）

### 8.1 下单接口

```
POST /api/app/subscription/create-order
Body: { level: "VIP", periodMonths: 1 }
Response: { orderId: 123, ...（支付所需信息）}
```

该接口仅创建订单，不处理支付。支付系统回调后通知会员系统。

### 8.2 支付回调

支付系统回调（或内部调用）：

```
POST /api/app/subscription/activate
Body: { userId, level: "VIP", days: 30 }
Response: 200 OK
```

`activate` 方法的逻辑：
1. 检查是否已有同级别角色
   - 有且在有效期内 → 在原有 `expire_time` 上续期（延长）
   - 有但已过期 → 更新 `expire_time` 为当前时间 + newDays
   - 无 → 插入新角色
2. 处理升级（见第 7 节）

后续集成具体支付渠道（如 StoreKit、Google Play、支付宝等）时，只需适配回调格式并调用 `activate` 接口即可。

---

## 9. JWT 角色刷新

用户登录时，`AppAuthServiceImpl.generateToken()` 已从数据库读取当前所有角色并写入 JWT：

```java
List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
        .map(GridUserRole::getRoleCode)
        .collect(Collectors.toList());
```

订阅变更后，用户需要**重新登录**或**刷新 Token** 才能获得更新后的角色 JWT。

**V1 简化**：订阅操作完成后不清除已有 Token（让用户手动刷新或等 Token 自然过期）。
**Future**：可在订阅激活时清除该用户的旧 Token，强制重新登录。

---

## 10. 文件清单

| 操作 | 路径 | 说明 |
|------|------|------|
| 新增 | `grid-common/.../annotation/RequireSubscription.java` | 自定义注解 |
| 新增 | `grid-common/.../enums/UserLevel.java` | 会员级别枚举 |
| 新增 | `grid-app/.../aspect/SubscriptionAspect.java` | AOP 鉴权切面 |
| 新增 | `grid-app/.../rest/AppSubscriptionController.java` | 订阅查询/购买 API |
| 新增 | `grid-app/.../service/dto/AppSubscriptionVO.java` | 订阅状态响应 DTO |
| 新增 | `grid-app/.../service/dto/CreateOrderDTO.java` | 下单请求 DTO |
| 新增 | `grid-app/.../service/dto/ActivateSubscriptionDTO.java` | 激活订阅请求 DTO |
| 新增 | `grid-app/.../service/SubscriptionService.java` | 订阅服务接口 |
| 新增 | `grid-app/.../service/impl/SubscriptionServiceImpl.java` | 订阅服务实现 |
| 修改 | `grid-app/.../enums/AppErrorCode.java` | 新增错误码 |
| 修改 | `grid-app/.../service/impl/AppAuthServiceImpl.java` | 注册时增加试用角色 |
| 修改 | `grid-app/.../repository/GridUserRoleRepository.java` | 新增查询方法 |

---

## 11. 配置项

```yaml
app:
  subscription:
    trial-days: 7               # 注册自动试用天数，0=关闭
```

---

## 12. 未包含的内容（后续迭代）

| 功能 | 说明 |
|------|------|
| 支付渠道对接 | 支付独立，本设计只消费支付结果 |
| 续费提醒推送 | 到期前推送（依赖 push 能力） |
| 订阅历史记录 | 变更历史查询（`subscription_log` 表） |
| 后台管理界面 | 会员列表、手动发放、统计 |
| 强制 Token 刷新 | 订阅变更时清除旧 Token |
| 邀请注册送会员 | 需配合邀请系统 |
| 按 Feature 粒度授权 | 当前是按级别（VIP/SVIP）整体控制 |
