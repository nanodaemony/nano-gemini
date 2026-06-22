# 有路中文 YourRoad 区域分层定价与权益体系设计

> 日期：2026-06-22
> 状态：设计已确认
> 涉及模块：grid-billing（新增）、grid-app（扩展）、grid-system（扩展）

---

## 1. 概述

本文档为"有路中文 YourRoad"平台设计完整的区域分层定价体系、用户类型扩展及权益堆叠引擎。原需求文档见 `docs/需求/有路中文YourRoad区域分层定价体系.md`。

### 1.1 核心目标

- 支持三种用户类型：普通用户（NORMAL）、机构用户（INSTITUTION）、代理商（AGENT）
- 实现 A-E 五区域分层定价（基于用户 IP 自动判定）
- 设计权益堆叠引擎（EntitlementEngine），支持多来源权益叠加与撤销重算
- 预留支付接口（WeChat/Alipay/Stripe），第一期空实现
- 设计推荐体系：个人/机构/代理商三种推荐奖励机制
- 机构自助注册 + 后台审核流程
- 代理商通过推荐码跟踪收益，预留后台扩展点

### 1.2 设计原则

- **来源独立**：每条权益来源独立记录，不修改用户单一到期时间
- **堆叠重算**：撤销任一条权益来源时，重跑堆叠算法自动得到正确到期时间
- **实时区域**：每次请求通过 IP 判定区域，购买时锁定区域，使用时校验是否一致
- **渐进交付**：第一期核心流程走通，预留二期扩展点

---

## 2. 系统架构

### 2.1 模块结构

```
grid/
├── grid-billing/                  # [新增] 产品、定价、权益、订单、支付
├── grid-app/                      # [扩展] 用户类型扩展、机构、代理、推荐码、区域拦截
├── grid-system/                   # [扩展] 产品管理、机构审核、代理审核（管理后台）
├── grid-common/                   # [扩展] 新增注解、枚举
├── grid-bootstrap/                # [扩展] 包扫描配置
└── ... 其余现有模块不变
```

### 2.2 模块依赖

```
grid-bootstrap ─→ grid-app
                ─→ grid-system
                ─→ grid-billing        (新增)

grid-app       ─→ grid-system
                ─→ grid-billing        (新增依赖)

grid-system    ─→ grid-tools
                ─→ grid-billing        (新增依赖)

grid-billing   ─→ grid-common          (新增模块)
                ─→ grid-logging
```

### 2.3 包扫描

```java
@SpringBootApplication(scanBasePackages = {
    "com.naon.grid.modules.billing",
    "com.naon.grid.modules.app",
    "com.naon.grid.modules.system",
    "com.naon.grid.common"
})
```

---

## 3. 数据模型

### 3.1 表命名约定

| 表名 | 所属模块 | 说明 |
|------|---------|------|
| `grid_user` | grid-app | 已有，扩展字段 |
| `grid_user_role` | grid-app | 已有，保持兼容 |
| `grid_user_auth` | grid-app | 已有 |
| `grid_user_token` | grid-app | 已有 |
| `grid_organization` | grid-app | 机构 |
| `grid_agent` | grid-app | 代理商 |
| `referral_record` | grid-app | 推荐记录 |
| `grid_product` | grid-billing | 产品定义 |
| `product_module` | grid-billing | 产品-模块关联 |
| `region_pricing` | grid-billing | 区域定价 |
| `entitlement_source` | grid-billing | 权益来源（堆叠核心） |
| `grid_order` | grid-billing | 订单 |
| `payment_record` | grid-billing | 支付流水 |

### 3.2 用户表扩展（grid_user）

现有 `grid_user` 表扩展以下字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `user_type` | VARCHAR(20) NOT NULL DEFAULT 'NORMAL' | NORMAL / INSTITUTION / AGENT |
| `org_id` | INT NULL | 所属机构ID（机构成员时） |
| `org_role` | VARCHAR(20) NULL | 机构内角色: ADMIN / MEMBER |
| `agent_id` | INT NULL | 所属代理ID（代理用户时） |
| `referral_code` | VARCHAR(32) NULL UNIQUE | 我的推荐码（自动生成） |
| `referred_by` | VARCHAR(32) NULL | 注册时填的推荐码 |
| `region` | VARCHAR(10) NULL | 注册时判定的区域 A/B/C/D/E |
| `register_audit_status` | VARCHAR(20) NULL DEFAULT 'APPROVED' | PENDING / APPROVED / REJECTED |

常规用户注册 `register_audit_status` 直接为 `APPROVED`。
机构/代理商注册为 `PENDING`，审核通过后改为 `APPROVED`。

### 3.3 机构表（grid_organization）

```sql
CREATE TABLE grid_organization (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL COMMENT '机构名称',
    name_en         VARCHAR(200) COMMENT '机构英文名',
    org_type        VARCHAR(20) NOT NULL COMMENT 'UNIVERSITY/SCHOOL/TRAINING/OTHER',
    contact_name    VARCHAR(100) COMMENT '联系人姓名',
    contact_email   VARCHAR(255) COMMENT '联系邮箱',
    contact_phone   VARCHAR(50) COMMENT '联系电话',
    country         VARCHAR(100) COMMENT '所在国家',
    region          VARCHAR(10) COMMENT '区域 A/B/C/D/E',
    status          INT NOT NULL DEFAULT 1 COMMENT '1=可用 0=已删除',
    audit_status    VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    max_members     INT NOT NULL DEFAULT 0 COMMENT '最大成员数（套餐限制）',
    max_admins      INT NOT NULL DEFAULT 0 COMMENT '最大管理员数',
    current_members INT NOT NULL DEFAULT 0 COMMENT '当前成员数',
    expire_time     DATETIME COMMENT '机构有效到期时间',
    create_by       VARCHAR(50),
    update_by       VARCHAR(50),
    create_time     DATETIME,
    update_time     DATETIME
);
```

### 3.4 代理商表（grid_agent）

```sql
CREATE TABLE grid_agent (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(200) NOT NULL COMMENT '代理商名称',
    contact_name    VARCHAR(100),
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(50),
    commission_rate DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '返现比例 %',
    referral_code   VARCHAR(32) NOT NULL UNIQUE COMMENT '代理专用推荐码',
    status          INT NOT NULL DEFAULT 1,
    audit_status    VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    create_by       VARCHAR(50),
    update_by       VARCHAR(50),
    create_time     DATETIME,
    update_time     DATETIME
);
```

### 3.5 产品表（grid_product）

```sql
CREATE TABLE grid_product (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE COMMENT '产品代码 PLUS/VOCAB/GRAMMAR/CHARACTER/CONFUSING_WORDS/CULTURE/TOPIC',
    name            VARCHAR(200) NOT NULL,
    product_type    VARCHAR(30) NOT NULL COMMENT 'PLUS / SINGLE_MODULE / INSTITUTION / ENTERPRISE',
    description     TEXT,
    sort_order      INT DEFAULT 0,
    status          INT NOT NULL DEFAULT 1,
    create_by       VARCHAR(50),
    update_by       VARCHAR(50),
    create_time     DATETIME,
    update_time     DATETIME
);

CREATE TABLE product_module (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    product_id      INT NOT NULL,
    module_code     VARCHAR(50) NOT NULL COMMENT 'VOCAB/GRAMMAR/CHARACTER/CONFUSING_WORDS/CULTURE/TOPIC',
    UNIQUE KEY uk_product_module (product_id, module_code)
);
```

**产品代码定义：**

| 产品代码 | 类型 | 包含模块 |
|---------|------|---------|
| `PLUS` | PLUS | VOCAB, GRAMMAR, CHARACTER, CONFUSING_WORDS, CULTURE, TOPIC |
| `VOCAB` | SINGLE_MODULE | 仅词汇 |
| `GRAMMAR` | SINGLE_MODULE | 仅语法 |
| `CHARACTER` | SINGLE_MODULE | 仅汉字 |
| `CONFUSING_WORDS` | SINGLE_MODULE | 仅易混淆词辨析 |
| `CULTURE` | SINGLE_MODULE | 仅文化 |
| `TOPIC` | SINGLE_MODULE | 仅话题 |
| `INST_STARTER` | INSTITUTION | 机构套餐 Starter |
| `INST_BASIC` | INSTITUTION | 机构套餐 Basic |
| `INST_PRO` | INSTITUTION | 机构套餐 Pro |

### 3.6 区域定价表（region_pricing）

```sql
CREATE TABLE region_pricing (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    product_id      INT NOT NULL,
    region          VARCHAR(10) NOT NULL COMMENT 'A/B/C/D/E',
    billing_cycle   VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY/YEARLY',
    price           DECIMAL(12,2) NOT NULL COMMENT '金额',
    currency        VARCHAR(10) NOT NULL COMMENT 'USD/EUR/CNY',
    status          INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_product_region_cycle (product_id, region, billing_cycle)
);
```

### 3.7 权益来源表（entitlement_source）— 核心

```sql
CREATE TABLE entitlement_source (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL COMMENT '用户ID',
    source_type     VARCHAR(30) NOT NULL COMMENT 'TRIAL / PURCHASE / INSTITUTION / REFERRAL / ADMIN_GRANT',
    source_id       VARCHAR(100) COMMENT '来源业务ID（订单号/机构ID等）',
    product_code    VARCHAR(50) NOT NULL COMMENT 'PLUS / VOCAB / ...',
    granted_at      DATETIME NOT NULL COMMENT '授予时间',
    duration_days   INT NOT NULL COMMENT '有效天数',
    expire_at       DATETIME COMMENT '实际到期（通过堆叠算法计算的缓存值）',
    region          VARCHAR(10) COMMENT '购买/授予时的区域',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / REVOKED / EXPIRED',
    remark          VARCHAR(500),
    create_time     DATETIME,
    INDEX idx_user_product (user_id, product_code, status),
    INDEX idx_source (source_type, source_id)
);
```

### 3.8 订单表（grid_order）

```sql
CREATE TABLE grid_order (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id         INT NOT NULL COMMENT '下单用户',
    org_id          INT COMMENT '机构下单时为机构ID',
    product_code    VARCHAR(50) NOT NULL,
    region          VARCHAR(10) NOT NULL,
    billing_cycle   VARCHAR(20) NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    currency        VARCHAR(10) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/REFUNDING/REFUNDED/EXPIRED',
    payment_method  VARCHAR(30) COMMENT 'WECHAT/ALIPAY/STRIPE',
    paid_at         DATETIME,
    expire_at       DATETIME,
    create_time     DATETIME
);
```

### 3.9 支付流水表（payment_record）

```sql
CREATE TABLE payment_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT NOT NULL,
    payment_method  VARCHAR(30) NOT NULL,
    transaction_id  VARCHAR(200) COMMENT '支付平台交易号',
    amount          DECIMAL(12,2) NOT NULL,
    currency        VARCHAR(10) NOT NULL,
    status          VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED/REFUND',
    raw_callback    TEXT COMMENT '原始回调JSON',
    create_time     DATETIME
);
```

### 3.10 推荐记录表（referral_record）

```sql
CREATE TABLE referral_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    referrer_id     INT NOT NULL COMMENT '推荐人用户ID',
    referrer_type   VARCHAR(20) NOT NULL COMMENT 'NORMAL/INSTITUTION/AGENT',
    referred_id     INT COMMENT '被推荐人用户ID（个人推荐时）',
    referral_code   VARCHAR(32) NOT NULL COMMENT '使用的推荐码',
    order_id        BIGINT COMMENT '关联订单',
    reward_status   VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/SETTLED/PAID',
    reward_amount   DECIMAL(12,2) COMMENT '奖励金额',
    reward_type     VARCHAR(20) COMMENT 'EXTEND_DAYS / CASH / MEMBER_COUNT',
    create_time     DATETIME,
    settle_time     DATETIME
);
```

---

## 4. 核心算法：权益堆叠引擎

### 4.1 堆叠规则

每条 `entitlement_source` 独立记录，互不修改对方的到期时间。

**新增权益（grantEntitlement）：**

```
输入：用户ID、产品代码、来源类型、来源ID、天数
流程：
  1. 查询该用户该产品下所有 ACTIVE 状态的 entitlement_source
  2. 按 granted_at 升序排列
  3. cursor = now（当前时间）
  4. 遍历排序后的来源：
     cursor = max(cursor, source.granted_at) + source.duration_days
  5. 新来源的 granted_at = now，duration_days = 输入天数
     cursor = max(cursor, now) + duration_days
  6. 新来源的 expire_at = cursor（缓存）
  7. 保存新来源
  8. 返回 cursor 作为新的有效到期时间
```

**撤销权益（revokeSource）：**

```
输入：来源ID
流程：
  1. 将来源 status 改为 REVOKED
  2. 此后 computeEntitlement 自动排除此来源
  3. 不做任何到期时间的"扣减"操作
```

**计算用户权益（computeEntitlement）：**

```
输入：用户ID
流程：
  1. 查询所有 ACTIVE 的 entitlement_source（含已过期的）
  2. 按 product_code 分组
  3. 对每组运行堆叠算法得到 productExpireAt
  4. 对 GROUP_CONCAT(productExpireAt) → 最晚的为 overallExpireAt
  5. 返回：每个产品的权益到期 + 总体到期
```

### 4.2 模块权限判定

```java
hasAccess(userId, moduleCode):
  1. computeEntitlement(userId)
  2. 查找是否有 PLUS 权益 → 有则全部通过
  3. 查找是否有 moduleCode 对应的产品权益
  4. 都无 → false
```

### 4.3 区域校验

```java
isValidForRegion(userId, currentRegion):
  1. 查询所有 ACTIVE entitlement_source，取其中的 region 字段
  2. 当前区域 currentRegion 与任一来源的 region 匹配 → 通过
  3. 不匹配 → 记录日志 + 警告 + 放行（第一期策略）
```

---

## 5. 业务流程

### 5.1 个人注册流程

```
POST /api/app/auth/register (email, password, referralCode?)
  → 创建 grid_user (user_type=NORMAL)
  → 解析 IP → 判定区域 → 写入 grid_user.region
  → 自动生成 referral_code (Base62编码)
  → 如果 referralCode 有值：
      → 查找该推荐码归属（grid_user / grid_agent）
      → 创建 referral_record (status=PENDING)
      → 如果归属是 AGENT → 关联 agent_id
  → 创建 grid_user_role (NORMAL)
  → 调用 EntitlementEngine.grantEntitlement(用户ID, "TRIAL", null, "PLUS", 7)
  → 返回 JWT (含 userType, region)
```

### 5.2 机构自助注册流程

```
POST /api/app/institution/register
  (name, contactEmail, contactPhone, adminEmail, adminPassword, ...)
  → 创建 grid_organization (audit_status=PENDING)
  → 创建 grid_user (user_type=INSTITUTION, org_id=xxx, org_role=ADMIN, register_audit_status=PENDING)
  → 区域判定 → 同时写入 organization.region 和 user.region
  → 返回 "审核中" 提示

审核通过（admin）：
  POST /api/institutions/{id}/approve?plan=INST_STARTER
  → audit_status=APPROVED
  → 设置 organization.max_members / max_admins / expire_time
  → 更新 grid_user.register_audit_status=APPROVED（管理员的用户）
  → 发放30天机构试用：
      → 为管理员调用 grantEntitlement(管理员ID, "INSTITUTION", orgId, "PLUS", 30)
      → 更新 organization.current_members

审核驳回：
  POST /api/institutions/{id}/reject?reason=xxx
  → audit_status=REJECTED
  → 通知管理员（预留）
```

### 5.3 代理商注册流程

```
POST /api/app/agent/register
  → 创建 grid_agent (audit_status=PENDING, referral_code自动生成)
  → 创建 grid_user (user_type=AGENT, agent_id=xxx, register_audit_status=PENDING)
  → 返回 "审核中" 提示

审核通过（admin）：
  POST /api/agents/{id}/approve
  → audit_status=APPROVED
  → register_audit_status=APPROVED
  → 用户可以登录

审核驳回：
  POST /api/agents/{id}/reject?reason=xxx
```

### 5.4 购买与支付流程

```
下单：
  POST /api/app/orders/create (productCode, billingCycle)
  → 从 request attribute 取 region
  → 查询 region_pricing 获取价格
  → 创建 grid_order (status=PENDING)
  → 返回订单信息

支付回调（模拟/第一期空实现）：
  POST /api/app/orders/callback
  → 校验签名（mock）
  → 更新 grid_order (status=PAID, paid_at=now)
  → 创建 payment_record
  → 调用 EntitlementEngine.grantEntitlement(userId, "PURCHASE", orderNo, productCode, days)
  → 如果是机构购买套餐 → 更新 organization.max_members / max_admins / expire_time
  → 如果有推荐记录 → 更新 referral_record 关联 order_id，发放推荐奖励

机构下单（管理员代机构下单）：
  POST /api/app/orders/create (productCode, billingCycle, orgId)
  → 校验当前用户 org_role=ADMIN
  → 同上流程
  → 支付成功后：为机构所有当前成员调用 grantEntitlement
```

### 5.5 推荐奖励发放

```
被推荐用户首次支付成功时：
  → referral_record 关联 order_id
  → 根据 referrer_type 发放奖励：

  NORMAL → grantEntitlement(referrerId, "REFERRAL", recordId, "PLUS", 30)
            // 推荐人增加30天PLUS权益

  INSTITUTION → 更新组织 max_members += 1 或 grantEntitlement

  AGENT → reward_amount = order.amount × agent.commission_rate
           reward_status = SETTLED
           // 后续后台可查看待提现汇总
```

---

## 6. 认证与访问控制

### 6.1 JWT 扩展

```json
{
  "userId": 1,
  "username": "user@example.com",
  "roles": ["NORMAL"],
  "userType": "INSTITUTION",
  "orgId": 5,
  "orgRole": "ADMIN",
  "region": "A"
}
```

### 6.2 区域拦截器

```java
public class RegionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        String ip = getClientIp(request);
        String region = ipRegionResolver.resolve(ip);  // ip2region 本地库
        request.setAttribute("_region", region);
        return true;
    }
}

// 注册到 WebMvcConfigurer
public class AppWebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RegionInterceptor())
                .addPathPatterns("/api/app/**");
    }
}
```

### 6.3 @RequireProduct 注解

```java
@Target(METHOD)
@Retention(RUNTIME)
public @interface RequireProduct {
    String[] value() default {};           // 产品代码
    RequireOrgRole orgRole() default NONE; // 机构角色
}

// 拦截器/Aspect
@Around("@annotation(requireProduct)")
public Object checkAccess(ProceedingJoinPoint pjp, RequireProduct requireProduct) {
    Long userId = AppSecurityUtils.getCurrentUserId();
    String region = (String) request.getAttribute("_region");
    
    // 1. 校验区域
    entitlementEngine.isValidForRegion(userId, region);  // 第一期仅警告日志
    
    // 2. 校验产品权限
    for (String productCode : requireProduct.value()) {
        if (!entitlementEngine.hasAccess(userId, productCode)) {
            throw new AppException(SUBSCRIPTION_REQUIRED);
        }
    }
    
    // 3. 校验机构角色
    if (requireProduct.orgRole() != NONE) {
        AppAuthenticationToken auth = (AppAuthenticationToken) SecurityContext.getContext();
        // 校验 orgRole 与 currentUser 的 orgRole
    }
    
    return pjp.proceed();
}
```

### 6.4 旧 @RequireSubscription 兼容

已有的 `@RequireSubscription` 保持可用，但推荐新代码使用 `@RequireProduct`。旧注解在 `SubscriptionAspect` 中改为调用 `EntitlementEngine` 的等价逻辑：

- `@RequireSubscription(VIP)` → 检查是否有 `PLUS` 权益或任一有效权益
- `@RequireSubscription(SVIP)` → 检查是否有 `PLUS` 权益

---

## 7. 请求防护说明

- **账号共享/异常访问**：第一期不做主动拦截，保留需求文档中的声明条款
- **批量下载/导出**：试用期间不开放，由 `@RequireProduct` + 试用期判断控制
- **区域不匹配**：第一期日志记录 + 放行，不阻断

---

## 8. 模块文件组织

### 8.1 grid-billing（新增模块）

```
com.naon.grid.modules.billing/
├── domain/
│   ├── GridProduct.java
│   ├── ProductModule.java
│   ├── RegionPricing.java
│   ├── EntitlementSource.java
│   ├── GridOrder.java
│   └── PaymentRecord.java
├── repository/
│   ├── GridProductRepository.java
│   ├── ProductModuleRepository.java
│   ├── RegionPricingRepository.java
│   ├── EntitlementSourceRepository.java
│   ├── GridOrderRepository.java
│   └── PaymentRecordRepository.java
├── dto/
│   ├── EntitlementResult.java            (VO: 产品权益到期列表)
│   ├── OrderCreateRequest.java
│   ├── OrderCreateResponse.java
│   └── PaymentCallbackDTO.java
├── service/
│   ├── EntitlementEngine.java            (核心堆叠算法)
│   ├── OrderService.java
│   ├── PaymentService.java
│   └── ProductService.java
├── service/impl/
│   ├── EntitlementEngineImpl.java
│   ├── OrderServiceImpl.java
│   ├── PaymentServiceImpl.java
│   └── ProductServiceImpl.java
└── config/
    └── BillingConfig.java                (JPA/包扫描配置)
```

### 8.2 grid-app 扩展

```
com.naon.grid.modules.app/
├── domain/
│   ├── GridOrganization.java         [新增]
│   ├── GridAgent.java                [新增]
│   ├── ReferralRecord.java           [新增]
│   └── GridUser.java                 [扩展现有]
├── repository/
│   ├── GridOrganizationRepository.java
│   ├── GridAgentRepository.java
│   └── ReferralRecordRepository.java
├── service/
│   ├── OrganizationService.java
│   ├── AgentService.java
│   ├── ReferralService.java
│   └── RegionResolver.java           (接口)
├── service/impl/
│   ├── OrganizationServiceImpl.java
│   ├── AgentServiceImpl.java
│   ├── ReferralServiceImpl.java
│   └── IpRegionResolver.java         (ip2region实现)
├── rest/
│   ├── OrganizationRegisterController.java
│   ├── AgentRegisterController.java
│   ├── OrderController.java
│   └── ReferralController.java
├── annotation/
│   └── RequireProduct.java           [新增]
├── aspect/
│   └── ProductAccessAspect.java      [新增, 替代旧SubscriptionAspect]
└── interceptor/
    └── RegionInterceptor.java
```

### 8.3 grid-system 扩展

```
com.naon.grid.modules.system/
└── rest/
    ├── ProductController.java         (CRUD + 定价管理)
    ├── InstitutionAuditController.java
    └── AgentAuditController.java
```

---

## 9. 后续可扩展点（本期不做）

- **机构管理后台**：成员管理、用量统计、订单管理
- **代理商管理后台**：下级销售员管理、收益明细、提现
- **支付接入**：微信支付 / 支付宝 / Stripe 真实接入
- **公平使用限制**：主动拦截批量抓取、账号共享
- **多语言/多币种实时结算**：根据用户实际支付地区转换
- **区域不匹配自动拦截**：第二期改为硬拦截

---

## 10. 附录：定价数据初始化

以下为个人版定价的种子数据参考：

### 区域代码

| 区域 | 适用地区 |
|------|---------|
| A | 北美、西欧、北欧 |
| B | 日本、韩国、澳大利亚、新西兰、中东高收入国家、新加坡及港澳台 |
| C | 中国大陆 |
| D | 东南亚(除新加坡)、东欧、拉美 |
| E | 非洲、南亚、中亚及部分低收入地区 |

### 全平台 Plus 定价

| 区域 | 月度 | 季度 | 年度 |
|------|------|------|------|
| A | $11.99 | $29.99 | $99.99 |
| B | $9.99 | $22.99 | $79.99 |
| C | ¥69 | ¥129 | ¥399 |
| D | $7.99 | $18.99 | $59.99 |
| E | $5.99 | $11.99 | $39.99 |

### 单模块年度定价

| 模块/区域 | A | B | C | D | E |
|-----------|---|---|---|---|---|
| 词汇 | $39.99 | $29.99 | ¥149 | $24.99 | $16.99 |
| 语法 | $29.99 | $24.99 | ¥169 | $19.99 | $12.99 |
| 汉字 | $19.99 | $14.99 | ¥99 | $12.99 | $8.99 |
| 易混淆词辨析 | $19.99 | $14.99 | ¥99 | $12.99 | $8.99 |
| 文化 | $19.99 | $14.99 | ¥99 | $12.99 | $8.99 |
| 话题 | $24.99 | $19.99 | ¥59 | $16.99 | $11.99 |
| Plus 年费 | $99.99 | $79.99 | ¥399 | $59.99 | $39.99 |
