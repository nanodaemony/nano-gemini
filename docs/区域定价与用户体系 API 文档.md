# 有路中文 YourRoad — 区域定价与用户体系 API 文档

> 版本：v1.0
> 日期：2026-06-22
> 适用前端：Web / App
> 基础地址：`http://localhost:8000`

---

## 目录

1. [认证相关](#1-认证相关)
2. [用户注册与登录](#2-用户注册与登录)
3. [订阅与权益接口](#3-订阅与权益接口)
4. [机构自助注册](#4-机构自助注册)
5. [代理商自助注册](#5-代理商自助注册)
6. [订单与支付接口](#6-订单与支付接口)
7. [推荐体系](#7-推荐体系)
8. [管理后台接口](#8-管理后台接口)
9. [JWT Token 结构](#9-jwt-token-结构)
10. [区域判定说明](#10-区域判定说明)
11. [错误码说明](#11-错误码说明)

---

## 1. 认证相关

### 1.1 获取 RSA 公钥

登录/注册时密码需用 RSA 加密传输，客户端先获取公钥。

```
GET /api/app/auth/public-key
```

**响应：**
```json
{
    "publicKey": "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC..."
}
```

**前端说明：** 使用该公钥加密密码后再传递。加密方式：`RSA/ECB/PKCS1Padding`。

---

## 2. 用户注册与登录

### 2.1 普通用户注册

```
POST /api/app/auth/register
Content-Type: application/json
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | string | 是 | 邮箱，唯一 |
| `password` | string | 是 | RSA 加密后的密码 |
| `nickname` | string | 否 | 昵称 |
| `deviceId` | string | 是 | 设备 ID |
| `deviceName` | string | 否 | 设备名称 |
| `referralCode` | string | 否 | **推荐码（新增）** |

**示例：**
```json
{
    "email": "user@example.com",
    "password": "encrypted_password_here",
    "nickname": "小明",
    "deviceId": "device-uuid-123",
    "deviceName": "Chrome Browser",
    "referralCode": "URABC123"
}
```

**注册流程说明：**
1. 检测邮箱唯一性
2. 解析 IP → 自动判定区域 (A/B/C/D/E)
3. 生成用户专属推荐码（格式：`UR` + 6 位大写字母数字）
4. 如果传了 `referralCode`，记录推荐关系
5. 自动发放 **7 天 PLUS 试用**权益
6. 返回登录 Token

**响应：**
```json
{
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "a1b2c3d4e5f6...",
    "expiresIn": 604800,
    "user": {
        "id": 1,
        "email": "user@example.com",
        "nickname": "小明",
        "avatar": null,
        "gender": 0,
        "roles": ["NORMAL"],
        "userType": "NORMAL",
        "orgRole": null,
        "region": "C"
    }
}
```

### 2.2 用户登录

```
POST /api/app/auth/login
Content-Type: application/json
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | string | 是 | 邮箱 |
| `password` | string | 是 | RSA 加密后的密码 |
| `deviceId` | string | 是 | 设备 ID |
| `deviceName` | string | 否 | 设备名称 |

**限制：**
- 机构/代理商用户在审核通过（`APPROVED`）前无法登录
- 审核中提示："您的账号正在审核中，审核通过后方可登录"
- 审核驳回提示："您的账号审核未通过"

**响应：** 同注册响应

### 2.3 刷新 Token

```
POST /api/app/auth/refresh
Content-Type: application/json
```

```json
{
    "refreshToken": "a1b2c3d4e5f6..."
}
```

### 2.4 退出登录

```
POST /api/app/auth/logout?deviceId=xxx
Authorization: Bearer <token>
```

---

## 3. 订阅与权益接口

### 3.1 查询订阅状态

```
GET /api/app/subscription/my
Authorization: Bearer <token>
```

**说明：** 查询当前用户的会员权益到期时间。支持多来源权益叠加计算（个人购买 + 机构授权 + 推荐奖励 + 试用）。

**响应：**
```json
{
    "level": "VIP",
    "expireTime": "2027-06-22T10:00:00.000+00:00",
    "expiringSoon": false
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `level` | string | `NORMAL` / `VIP`（只要有任何有效权益即为 VIP） |
| `expireTime` | datetime | null 表示无权益 / NORMAL 状态 |
| `expiringSoon` | boolean | 是否 15 天内到期 |

**前端逻辑：**
- `level = NORMAL` → 展示无会员或试用已过期
- `level = VIP` 且 `expiringSoon = true` → 展示续费提醒
- 注册时自动发放 7 天试用 → 注册后立即调用此接口可看到 VIP + 7天后到期

---

## 4. 机构自助注册

### 4.1 注册机构

```
POST /api/app/institution/register
Content-Type: application/json
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 机构名称 |
| `nameEn` | string | 否 | 机构英文名 |
| `orgType` | string | 是 | `UNIVERSITY` / `SCHOOL` / `TRAINING` / `OTHER` |
| `contactName` | string | 是 | 联系人姓名 |
| `contactEmail` | string | 是 | 联系邮箱 |
| `adminEmail` | string | 是 | 管理员账号邮箱（可与联系邮箱不同） |
| `adminPassword` | string | 是 | 管理员密码（RSA 加密） |
| `deviceId` | string | 是 | 设备 ID |
| `deviceName` | string | 否 | 设备名称 |

**注册流程：**
1. 创建机构记录，`audit_status = PENDING`
2. 创建管理员账号（`user_type = INSTITUTION`, `org_role = ADMIN`, `register_audit_status = PENDING`）
3. 管理员登录时会提示"您的账号正在审核中"
4. 后台审核通过后，管理员方可登录
5. 审核通过时自动发放 **30 天机构试用**

**响应：** 返回 Token（但用户暂时无法使用，需等待审核通过）

### 4.2 注册须知（前端展示）

- 机构注册后需要平台管理员审核
- 审核期间机构管理员无法登录
- 审核通过后发放 30 天试用
- 试用人数上限由审核时选择的套餐决定（Starter 30人 / Basic 100人 / Pro 500人）

---

## 5. 代理商自助注册

### 5.1 注册代理商

```
POST /api/app/agent/register
Content-Type: application/json
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 代理商名称 |
| `contactName` | string | 否 | 联系人姓名 |
| `contactEmail` | string | 否 | 联系邮箱 |
| `adminEmail` | string | 是 | 管理员邮箱 |
| `adminPassword` | string | 是 | 密码（RSA 加密） |
| `deviceId` | string | 是 | 设备 ID |
| `deviceName` | string | 否 | 设备名称 |

**流程：** 同机构注册，需审核。审核通过后自动生成代理商专用推荐码（格式：`AG` + 8 位大写字母数字）。

**响应：** 同机构注册。

---

## 6. 订单与支付接口

### 6.1 创建订单

```
POST /api/app/orders/create
Authorization: Bearer <token>
Content-Type: application/json
```

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `productCode` | string | 是 | 产品代码：`PLUS` / `VOCAB` / `GRAMMAR` / `CHARACTER` / `CONFUSING_WORDS` / `CULTURE` / `TOPIC` |
| `billingCycle` | string | 是 | `MONTHLY` / `QUARTERLY` / `YEARLY` |
| `orgId` | int | 否 | 机构下单时传入机构 ID（需有 ADMIN 角色） |

**说明：**
- 区域由后端根据请求 IP 自动判定，前端无需传入
- 价格根据产品 + 区域 + 计费周期自动查询
- 机构下单时，当前用户必须是该机构的 ADMIN

**示例：**
```json
{
    "productCode": "PLUS",
    "billingCycle": "YEARLY"
}
```

**响应：**
```json
{
    "orderNo": "ORD20260622143022A1B2C3D4",
    "productCode": "PLUS",
    "billingCycle": "YEARLY",
    "amount": 99.99,
    "currency": "USD",
    "status": "PENDING",
    "redirectUrl": null
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `orderNo` | string | 订单号（格式：ORD + yyyyMMddHHmmss + 8位大写） |
| `amount` | decimal | 金额（根据区域和周期自动计算） |
| `currency` | string | `USD` / `EUR` / `CNY` |
| `status` | string | `PENDING`：待支付 |
| `redirectUrl` | string | 支付跳转链接（第一期返回 null） |

### 6.2 支付回调（模拟）

```
POST /api/app/orders/callback
Content-Type: application/json
```

**说明：** 第一期模拟回调接口，实际支付接入时由支付平台回调。当前为 mock 实现，调用后直接标记支付成功并发放权益。

**请求参数：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `order_no` | string | 是 | 订单号 |
| `payment_method` | string | 是 | `WECHAT` / `ALIPAY` / `STRIPE` |

**示例：**
```json
{
    "order_no": "ORD20260622143022A1B2C3D4",
    "payment_method": "ALIPAY"
}
```

**响应：**
- 成功：HTTP 200, body: `"SUCCESS"`
- 失败：HTTP 400, body: `"FAILED"`

**支付成功后的处理（前端需了解）：**
1. 订单状态更新为 `PAID`
2. 生成支付流水记录
3. 调用权益引擎发放权益（到期时间按堆叠算法计算）
4. 如果是被推荐用户首次购买，触发推荐奖励结算

---

## 7. 推荐体系

### 7.1 推荐流程说明

三种推荐方式共用推荐码机制：

| 推荐人类型 | 推荐码格式 | 奖励方式 |
|-----------|-----------|---------|
| 普通用户 | `UR` + 6位大写字母数字 | 增加 30 天 PLUS 会员时长 |
| 机构用户 | `UR` + 6位大写字母数字 | 增加机构人数额度或会员时长 |
| 代理商 | `AG` + 8位大写字母数字 | 按订单金额比例返现 |

### 7.2 用户注册时使用推荐码

用户注册时，在 `RegisterDTO.referralCode` 字段传入推荐码：
- 注册接口自动匹配推荐码归属（普通用户/机构/代理商）
- 记录推荐关系
- 被推荐用户首次下单支付成功时触发奖励结算

### 7.3 获取推荐信息

```
GET /api/app/referral/info
Authorization: Bearer <token>
```

**说明：** 第一期占位接口，返回 `"Referral system active"`。

---

## 8. 管理后台接口

> 这些接口供管理后台（Web）使用，需要管理员权限。

### 8.1 产品管理

#### 获取所有产品列表

```
GET /api/products
Authorization: Bearer <admin_token>
```

**响应：**
```json
[
    {
        "id": 1,
        "code": "PLUS",
        "name": "全平台Plus会员",
        "productType": "PLUS",
        "description": null,
        "sortOrder": 1,
        "status": 1,
        "createTime": "2026-06-22 10:00:00",
        "updateTime": "2026-06-22 10:00:00"
    },
    {
        "id": 2,
        "code": "VOCAB",
        "name": "词汇模块",
        "productType": "SINGLE_MODULE",
        "sortOrder": 2,
        "status": 1,
        ...
    }
]
```

#### 获取产品区域定价

```
GET /api/products/{code}/pricing?region=C
Authorization: Bearer <admin_token>
```

**参数：**
| 参数 | 必填 | 说明 |
|------|------|------|
| `region` | 否 | 区域代码，默认 `C`（中国大陆） |

**产品代码对照：**

| code | 说明 | 类型 |
|------|------|------|
| `PLUS` | 全平台 Plus 会员（包含所有模块） | PLUS |
| `VOCAB` | 词汇模块 | SINGLE_MODULE |
| `GRAMMAR` | 语法模块 | SINGLE_MODULE |
| `CHARACTER` | 汉字模块 | SINGLE_MODULE |
| `CONFUSING_WORDS` | 易混淆词辨析模块 | SINGLE_MODULE |
| `CULTURE` | 文化模块 | SINGLE_MODULE |
| `TOPIC` | 话题模块 | SINGLE_MODULE |
| `INST_STARTER` | 机构版 Starter | INSTITUTION |
| `INST_BASIC` | 机构版 Basic | INSTITUTION |
| `INST_PRO` | 机构版 Pro | INSTITUTION |

### 8.2 机构审核

#### 获取待审核机构列表

```
GET /api/institutions/pending
Authorization: Bearer <admin_token>
```

#### 审核通过

```
POST /api/institutions/{id}/approve?plan=INST_STARTER
Authorization: Bearer <admin_token>
```

| 参数 | 说明 |
|------|------|
| `plan` | 机构套餐代码：`INST_STARTER`（30人） / `INST_BASIC`（100人） / `INST_PRO`（500人） |

**审核通过后自动：**
1. 设置机构 `audit_status = APPROVED`
2. 设置套餐人数上限
3. 激活管理员账号（`register_audit_status = APPROVED`）
4. 发放 30 天 PLUS 试用

#### 审核驳回

```
POST /api/institutions/{id}/reject?reason=资料不完整
Authorization: Bearer <admin_token>
```

### 8.3 代理商审核

#### 获取待审核代理商列表

```
GET /api/agents/pending
Authorization: Bearer <admin_token>
```

#### 审核通过

```
POST /api/agents/{id}/approve
Authorization: Bearer <admin_token>
```

#### 审核驳回

```
POST /api/agents/{id}/reject?reason=xxx
Authorization: Bearer <admin_token>
```

---

## 9. JWT Token 结构

### 9.1 Token Claims

```json
{
    "userId": 1,
    "username": "user@example.com",
    "deviceId": "device-uuid-123",
    "roles": ["NORMAL"],
    "type": "access",
    "userType": "NORMAL",
    "orgId": null,
    "orgRole": null,
    "region": "C",
    "sub": "user@example.com",
    "iat": 1687411200,
    "exp": 1688016000
}
```

### 9.2 userType 说明

| 值 | 说明 | 对应角色 |
|------|------|---------|
| `NORMAL` | 普通个人用户 | 标准注册用户 |
| `INSTITUTION` | 机构用户 | 属于某个机构的成员 |
| `AGENT` | 代理商用户 | 代理商管理员 |

### 9.3 orgRole 说明（userType = INSTITUTION 时）

| 值 | 说明 |
|------|------|
| `ADMIN` | 机构管理员，可管理成员、代机构下单 |
| `MEMBER` | 机构普通成员 |

### 9.4 region 说明

| 值 | 适用地区 |
|-----|---------|
| `A` | 北美、西欧、北欧 |
| `B` | 日本、韩国、澳大利亚、新西兰、中东高收入国家、新加坡及港澳台 |
| `C` | 中国大陆 |
| `D` | 东南亚(除新加坡)、东欧、拉美 |
| `E` | 非洲、南亚、中亚及部分低收入地区 |

---

## 10. 区域判定说明

### 10.1 判定机制

- 每个请求由 `RegionInterceptor` 自动拦截
- 通过 IP 地址 → ip2region 本地库 → 映射到 A/B/C/D/E 五区
- 区域信息存储在 JWT 的 `region` claim 中
- 登录时更新用户区域

### 10.2 前端注意事项

- 无需自行计算区域
- 创建订单时后端自动使用请求 IP 对应的区域价格
- 区域由用户在结算页可见（通过 JWT 中 `region` 字段展示）
- 区域不匹配时，第一期仅记录警告日志，不影响使用

---

## 11. 错误码说明

### 11.1 通用错误码

| HTTP 状态码 | errorCode | 说明 |
|------------|-----------|------|
| 400 | 1000 | 用户名已存在 |
| 400 | 1001 | 手机号已注册 |
| 400 | 1002 | 手机号或密码错误 |
| 400 | 1003 | 账号已被禁用 |
| 401 | 1004 | Token 已过期 |
| 401 | 1005 | 无效的 Token |
| 400 | 1100 | 手机号格式错误 |
| 400 | 1101 | 密码格式错误 |

### 11.2 订阅相关错误码

| HTTP 状态码 | errorCode | 说明 |
|------------|-----------|------|
| 400 | 1200 | 需要订阅后才能访问此内容（前端应跳转到会员购买页面） |
| 400 | 1201 | 订阅已过期，请续费 |

### 11.3 权限相关错误码

| HTTP 状态码 | errorCode | 说明 |
|------------|-----------|------|
| 403 | 1403 | 没有权限（如非机构管理员操作管理功能） |

### 11.4 自定义业务错误

业务错误通过 `BadRequestException` 返回，格式为：

```json
{
    "status": 400,
    "message": "具体的错误信息",
    "timestamp": "2026-06-22 10:00:00"
}
```

常见错误消息：
- `"邮箱已被注册"` — 注册时邮箱重复
- `"邮箱或密码错误"` — 登录凭据错误
- `"您的账号正在审核中，审核通过后方可登录"` — 机构/代理账号待审核
- `"您的账号审核未通过"` — 机构/代理账号被驳回
- `"产品不存在: xxx"` — 产品代码无效
- `"该产品在A区没有YEARLY定价"` — 该区域无对应定价
- `"机构不存在"` / `"代理商不存在"` — ID 无效

---

## 附录：完整 API 路径一览

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | `/api/app/auth/public-key` | 获取 RSA 公钥 | 匿名 |
| POST | `/api/app/auth/register` | 用户注册 | 匿名 |
| POST | `/api/app/auth/login` | 用户登录 | 匿名 |
| POST | `/api/app/auth/refresh` | 刷新 Token | 匿名 |
| POST | `/api/app/auth/logout` | 退出登录 | Bearer |
| GET | `/api/app/subscription/my` | 查询订阅状态 | Bearer |
| POST | `/api/app/subscription/create-order` | 创建订阅（旧版，暂保留） | Bearer |
| POST | `/api/app/institution/register` | 机构自助注册 | 匿名 |
| POST | `/api/app/agent/register` | 代理商自助注册 | 匿名 |
| POST | `/api/app/orders/create` | 创建订单 | Bearer |
| POST | `/api/app/orders/callback` | 支付回调（模拟） | 匿名 |
| GET | `/api/app/referral/info` | 推荐信息 | Bearer |
| GET | `/api/products` | 产品列表（管理后台） | Bearer |
| GET | `/api/products/{code}/pricing` | 产品定价（管理后台） | Bearer |
| GET | `/api/institutions/pending` | 待审核机构列表 | Bearer |
| POST | `/api/institutions/{id}/approve` | 审核通过机构 | Bearer |
| POST | `/api/institutions/{id}/reject` | 驳回机构 | Bearer |
| GET | `/api/agents/pending` | 待审核代理商列表 | Bearer |
| POST | `/api/agents/{id}/approve` | 审核通过代理 | Bearer |
| POST | `/api/agents/{id}/reject` | 驳回代理 | Bearer |
