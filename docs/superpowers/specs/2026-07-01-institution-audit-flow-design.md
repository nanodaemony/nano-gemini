# 机构审核入驻流程设计

## 概述

机构申请入驻需要经过"提交申请 → 后台审核 → 审核通过/驳回"的流程。审核通过后才创建机构管理员用户，并通过邮件告知结果。

## 数据模型变更

### grid_organization 新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `admin_password` | `VARCHAR(100)` | 申请时提交的管理员密码（BCrypt加密），审核通过后用于创建 grid_user，用完清空 |
| `reject_reason` | `VARCHAR(500)` | 审核驳回原因 |
| `referred_by` | `VARCHAR(32)` | 申请时填写的邀请码，用于追溯推荐来源 |

### SQL 变更（normal_user.sql）

```sql
-- grid_organization 表新增字段
ALTER TABLE `grid_organization`
    ADD COLUMN `admin_password` VARCHAR(100) COMMENT '管理员密码(BCrypt)，审核通过后使用',
    ADD COLUMN `reject_reason` VARCHAR(500) COMMENT '审核驳回原因',
    ADD COLUMN `referred_by` VARCHAR(32) COMMENT '申请时填写的邀请码';
```

> **说明**：当前数据库无数据，直接将上述字段加入 `normal_user.sql` 中 `grid_organization` 的 CREATE TABLE 即可。

### GridOrganization.java 新增字段

```java
@Column(length = 100)
private String adminPassword;

@Column(length = 500)
private String rejectReason;

@Column(length = 32)
private String referredBy;
```

### 机构邀请码机制

机构自身的推荐码不存储在 `grid_organization` 表，而是审核通过创建 `grid_user`（管理员）时，为其生成 `referral_code`（UR 前缀），即**机构的邀请码 = 管理员的 referral_code**。

## 流程设计

### 1. 用户侧：机构申请/查询页（1 页面，3 接口）

打开申请页，输入**邮箱+密码**进入：

```
                ┌─────────────────────────────────┐
                │    邮箱+密码查询                 │
                └──────────┬──────────────────────┘
                           │
         ┌─────────────────┼──────────────────┐
         ▼                 ▼                  ▼
    无记录            PENDING              REJECTED
    ┌──────┐       ┌───────────┐       ┌──────────────┐
    │显示  │       │显示       │       │显示原始数据   │
    │申请  │       │"审核中"   │       │+驳回原因     │
    │表单  │       │           │       │+可修改重新提交│
    └──────┘       └───────────┘       └──────────────┘
```

#### 接口 1：新申请

```
POST /api/app/institution/register

请求体 (InstitutionRegisterDTO):
{
  "name": "XX中文学校",          // 机构名称
  "nameEn": "XX Chinese School", // 英文名(选填)
  "orgType": "SCHOOL",           // 机构类型
  "contactName": "张三",          // 联系人姓名
  "contactPhone": "139xxxx",     // 联系电话(选填)
  "adminEmail": "admin@xxx.com", // 管理员邮箱（同时也是联系人邮箱、登录账号、审核通知接收邮箱）
  "adminPassword": "加密后密码",  // 管理员密码(RSA加密传输)
  "referredBy": "AGxxxx"         // 邀请码(选填)
}

返回: { "message": "提交成功，请等待审核" }
```

**逻辑变化**（对比当前实现）：
- ✅ 创建 `GridOrganization`（auditStatus=PENDING），`admin_password` 存 BCrypt 加密密码
- ❌ 不再创建 `GridUser` 和 `GridUserRole`
- ❌ 不再返回 JWT token

#### 接口 2：查询申请状态

```
POST /api/app/institution/application/query

请求体:
{
  "email": "admin@xxx.com",
  "password": "RSA加密后的密码"
}

返回:
// PENDING
{ "status": "PENDING", "message": "您的申请正在审核中，请耐心等待" }

// APPROVED
{ "status": "APPROVED", "message": "审核已通过，请登录" }

// REJECTED
{
  "status": "REJECTED",
  "message": "审核未通过",
  "data": {
    "name": "XX中文学校",
    "nameEn": "...",
    "orgType": "SCHOOL",
    "contactName": "张三",
    "contactPhone": "...",
    "rejectReason": "机构名称与营业执照不一致"
  }
}

// 邮箱或密码错误
{ "status": "ERROR", "message": "邮箱或密码不正确" }
```

> **身份验证**：用 `adminEmail` 在 `grid_organization` 中查找 `contact_email` 字段匹配，同时 BCrypt 校验密码。

#### 接口 3：重新提交

```
PUT /api/app/institution/application

请求体: 同 POST /register（InstitutionRegisterDTO）

逻辑:
- 检测到该 adminEmail 存在 REJECTED 记录
- 更新所有字段，auditStatus 重置为 PENDING
- 清除 reject_reason
- admin_password 若有变化则重新加密写入
- 返回: { "message": "重新提交成功，请等待审核" }
```

### 2. 后台管理侧（3 页面，4 接口）

#### 页面 1：待审核/已入驻/已驳回列表页

```
GET /api/institutions?page=0&size=20&auditStatus=PENDING
GET /api/institutions?page=0&size=20&auditStatus=APPROVED
GET /api/institutions?page=0&size=20&auditStatus=REJECTED
```

> 现有 `findByAuditStatus` 需要改为分页查询（继承 `JpaSpecificationExecutor` 的 `findAll(Specification, Pageable)` 即可）。

#### 页面 2：申请详情页

```
GET /api/institutions/{id}

返回: GridOrganization 完整数据（不返回 admin_password）
```

#### 页面 3：审核操作

```
POST /api/institutions/{id}/approve?plan=INST_STARTER
  → 创建 grid_user（管理员）+ grid_user_role（NORMAL）
  → 设置 org.maxMembers/maxAdmins（按套餐）
  → 设置 currentMembers = 1
  → 清空 admin_password
  → 发送审核通过邮件

POST /api/institutions/{id}/reject?reason=机构名称与营业执照不一致
  → 设置 auditStatus = REJECTED
  → 写入 reject_reason
  → 发送审核驳回邮件
```

### 3. 审核通过时创建管理员用户的逻辑

```java
// 在 OrganizationServiceImpl.approve() 中新增：

// 1. 从 org 获取加密密码
String encryptedPassword = org.getAdminPassword();

// 2. 创建管理员用户
GridUser admin = new GridUser();
admin.setEmail(org.getContactEmail());  // 申请时填的 adminEmail，存入 contact_email 字段
admin.setPassword(encryptedPassword);   // 直接使用已加密的密码
admin.setNickname(org.getContactName());
admin.setUserType("INSTITUTION");
admin.setOrgId(org.getId());
admin.setOrgRole("ADMIN");
admin.setRegisterAuditStatus("APPROVED");
admin.setRegion(org.getRegion());
// 生成机构邀请码
String referralCode = "UR" + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
admin.setReferralCode(referralCode);
admin.setStatus(1);
userRepository.save(admin);

// 3. 创建 NORMAL 角色
GridUserRole normalRole = new GridUserRole();
normalRole.setUserId(admin.getId());
normalRole.setRoleCode("NORMAL");
normalRole.setRoleName("普通用户");
userRoleRepository.save(normalRole);

// 4. 清空 admin_password
org.setAdminPassword(null);
```

### 4. 邮箱通知

审核通过邮件模板：
```
主题: 您的机构【机构名称】入驻审核已通过

正文:
您好，您的机构【机构名称】已通过审核，现已正式入驻有路中文平台。

您可以点击以下链接使用邮箱【adminEmail】登录平台：
[登录链接]

请及时设置机构信息并开始管理您的成员。
```

审核驳回邮件模板：
```
主题: 您的机构【机构名称】入驻审核未通过

正文:
您好，您的机构【机构名称】审核未通过，原因如下：
[驳回原因]

您可根据驳回原因修改信息后重新提交申请。
```

> 邮件发送复用 `grid-tools` 模块 `EmailService` 的现有能力。

## 变更清单

### 新增文件

| 文件 | 说明 |
|------|------|
| `ApplicationQueryDTO.java` | 查询申请状态的请求体 |
| `ApplicationResubmitDTO.java` | 重新提交的请求体(可复用 InstitutionRegisterDTO) |
| 邮件模板 | 审核通过/驳回两个模板 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `GridOrganization.java` | 加 `adminPassword`、`rejectReason`、`referredBy` 字段 |
| `InstitutionRegisterDTO.java` | 加 `referredBy` 字段，去掉 `contactEmail`/`deviceId`/`deviceName`（仅保留 `adminEmail` 统一作为联系+登录邮箱）|
| `OrganizationService.java` | 接口改注册返回值为 void，新增 `queryApplication()`、`resubmit()` 方法 |
| `OrganizationServiceImpl.java` | 重写 `register()` 逻辑，重写 `approve()`，改造 `reject()` |
| `AppInstitutionController.java` | 改注册返回类型，新增查询/重新提交端点 |
| `InstitutionAuditController.java` | 加分页条件查询、详情查询接口 |
| `normal_user.sql` | grid_organization 表加 3 个字段 |

### 无需改动

- `AppAuthServiceImpl.login()` — INSTITUTION 用户审核通过后直接登录（`registerAuditStatus` 是 `APPROVED`）
- `GridUserRepository` — `findByOrgIdAndOrgRole` 和 `findByEmail` 已存在
- `AgentService` / `AgentServiceImpl` — AGENT 审批流程保持不变

## 注意事项

1. **注册接口不再返回 token**，前端需要改为"提交成功"页面，而非自动跳转登录
2. **`registerAuditStatus` 字段** AGENT 类型仍会使用（PENDING→APPROVED），保留不变
3. **`contactEmail` 字段** 被移除，`adminEmail` 同时作为联系人邮箱、登录账号和审核通知接收邮箱。`GridOrganization.contact_email` 字段复用，存入 `adminEmail`
4. **`deviceId`/`deviceName`** 不再需要（注册不返回 token 就不需要注册设备）
5. **重新申请时的密码处理**：如果用户未修改密码，`PUT` 接口可以传空或原值，但为了安全，前端应要求重新输入密码
