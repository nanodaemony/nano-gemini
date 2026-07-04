# Google / Twitter OAuth 登录设计文档

## 概述

为 grid-app 新增第三方 OAuth 登录功能，支持 Google 和 Twitter 账号登录。采用「客户端获取 ID Token → 服务端验证 → 创建/关联账号 → 下发 JWT」的模式。

## 接口设计

新增 3 个端点，均标记 `@AnonymousAccess`：

### POST /api/app/auth/social-login

统一的第三方登录入口。

**请求体：**

```json
{
  "provider": "google | twitter",
  "idToken": "第三方返回的 ID Token (JWT)",
  "deviceId": "客户端设备标识",
  "deviceName": "可选，设备名称"
}
```

**正常响应（同 login 接口）：**

```json
{
  "accessToken": "JWT",
  "refreshToken": "UUID",
  "user": { "id": 1, "email": "...", "nickname": "...", "avatar": "..." }
}
```

**无邮箱时的特殊响应（仅 Twitter 可能出现）：**

```json
{
  "requireBindEmail": true,
  "bindToken": "临时 JWT，5 分钟有效"
}
```

### POST /api/app/auth/send-bind-code

第三方登录无邮箱时，发送邮箱绑定验证码。

**请求体：**

```json
{
  "email": "user@example.com"
}
```

逻辑复用现有 `sendCode`，但不检查邮箱是否已注册（因为场景包括关联已有账号）。

### POST /api/app/auth/social-bind-email

无邮箱用户绑定邮箱并完成注册/登录。

**请求体：**

```json
{
  "bindToken": "social-login 返回的 bindToken",
  "email": "user@example.com",
  "code": "6位验证码",
  "deviceId": "客户端设备标识",
  "deviceName": "可选"
}
```

**响应：** 同正常 social-login 响应，返回 accessToken + refreshToken + user。

## 核心流程

### social-login 主流程

```
1. 根据 provider 获取对应 JWKS 公钥
2. 验证 ID Token（签名、aud、iss、exp）
3. 提取 claims: sub, email, email_verified, name, picture
4. 查 GridUserAuth (provider + provider_id)
   → 已存在: 直接获取 user，跳到步骤 7
5. [无邮箱] → 生成 bindToken，返回 {requireBindEmail: true, bindToken}
6. [有邮箱]
   → 查 email 是否已注册
      → 已注册: 自动关联，写 GridUserAuth 到已有 user
      → 未注册: 创建新 GridUser（password=null），写 GridUserAuth
7. 更新 GridUserAuth 的 access_token、provider_name、provider_avatar、expire_time
8. 更新 GridUser.nickname、avatar（如为空则从第三方填充）
9. 更新登录元数据（lastLoginTime、lastLoginIp、region）
10. 生成 JWT + refreshToken，写 GridUserToken
11. 返回 TokenDTO
```

### social-bind-email 子流程

```
1. 验证 bindToken（JWT 签名 + 过期时间）
2. 提取 bindToken 中的 social claims
3. 验证邮箱验证码（Redis email:code:{email}）
4. 查 email 是否已注册
   → 已注册: 将第三方身份关联到已有 user
   → 未注册: 创建新 GridUser（password=null, email_verified=1）
5. 写 GridUserAuth
6. 生成 JWT + refreshToken
7. 返回 TokenDTO
```

## 账户匹配优先级

1. **provider_id 匹配优先**：先查 `GridUserAuth` 表中 `(provider, provider_id)` 是否已存在
2. **邮箱匹配次之**：provider_id 不存在时，按邮箱查找已注册用户并自动关联
3. **均不匹配**：创建新用户

## ID Token 验证

- 使用各平台 JWKS 端点获取公钥，本地缓存 1 小时
- Google JWKS: `https://www.googleapis.com/oauth2/v3/certs`
- Twitter JWKS: 通过 OIDC discovery `https://api.twitter.com/2/oauth2/jwks`（以实际文档为准）
- 验证项：
  - JWT 签名（RS256）
  - `aud` 与配置的 Client ID 一致
  - `iss` 与平台一致（Google: `accounts.google.com` 或 `https://accounts.google.com`）
  - `exp` 未过期

## 数据模型

复用已有表，无需新增或变更：

### GridUserAuth（已有）

| 字段 | 来源 | 说明 |
|---|---|---|
| user_id | 自动关联或新建 | FK → grid_user.id |
| provider | 请求参数 | `google` / `twitter` |
| provider_id | ID Token `sub` | 第三方用户唯一标识 |
| provider_name | ID Token `name` | 第三方昵称 |
| provider_avatar | ID Token `picture` | 第三方头像 URL |
| access_token | 原始 idToken | 存证备查 |
| expire_time | ID Token `exp` | Token 有效期限 |
| UNIQUE | (provider, provider_id) | 防止重复绑定 |

### GridUser（已有，关键字段使用）

- `password`: Google/Twitter 用户设为 `null`（字段已 nullable）
- `email`: 来自 ID Token 或绑定输入
- `email_verified`: Google 且 `email_verified=true` → 1；Twitter 绑定后 → 1
- `avatar`: 优先用第三方 `picture`
- `nickname`: 优先用第三方 `name`，否则用邮箱前缀
- `userType`: `"NORMAL"`（默认值）

### Redis 临时 Key

```
social:bind:{uuid}  TTL: 5分钟
Value: {"provider":"twitter","providerId":"xxx","name":"...","avatar":"..."}
```

## 配置

`.env` 新增：

```
GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com
TWITTER_CLIENT_ID=xxx
```

Spring 中新建 `SocialLoginProperties` 配置类，按 provider 管理 clientId。

## 异常场景

| 场景 | 处理 |
|---|---|
| ID Token 签名无效 | 统一返回 "登录验证失败" |
| ID Token 已过期 | 返回 "登录凭证已过期，请重新授权" |
| `aud` 不匹配 | 统一返回 "登录验证失败" |
| 同一 Google 账号重复登录 | provider_id 匹配后直接登录，无副作用 |
| 邮箱已注册且为密码账号 | 自动关联到已有账号（有邮箱验证码确认） |
| 邮箱已绑定其他第三方账号 | 拒绝：返回 "该邮箱已绑定其他登录方式" |
| 用户被封禁 (status=0) | 拒绝：返回 "账号已被禁用" |
| bindToken 过期 | 返回 "操作超时，请重新登录" |
| 同一 provider_id 绑定两个用户 | UNIQUE 约束阻止 |
| Twitter 无邮箱用户绑定时输入已有邮箱 | 自动关联到已有账号 |

## 安全考虑

- `bindToken` 为短期 JWT（5 分钟），用服务端 JWT 密钥签发，不可篡改
- 绑定邮箱需验证码确认，防止邮箱劫持
- ID Token `aud` 校验防止跨应用 token 滥用
- 错误信息统一，不泄露账号是否存在
- `GridUserAuth.access_token` 仅存证，不用于续期

## 影响范围

### 新增文件
- `SocialLoginDTO.java` — social-login 请求 DTO
- `SocialBindEmailDTO.java` — social-bind-email 请求 DTO
- `SocialLoginService.java` / `SocialLoginServiceImpl.java` — 核心逻辑
- `GoogleIdTokenVerifier.java` — Google ID Token 验证
- `TwitterIdTokenVerifier.java` — Twitter ID Token 验证
- `SocialLoginProperties.java` — 配置类
- `SocialLoginVO.java` — 响应 VO
- `SocialLoginWrapper.java` — DTO→VO 转换

### 修改文件
- `AppAuthController.java` — 新增 3 个端点方法
- `AppAuthService.java` / `AppAuthServiceImpl.java` — 新增 socialLogin、socialBindEmail 方法
- `AppErrorCode.java` — 新增错误码（如需）
- `application.yml` / `.env` — 新增配置项

### 不受影响
- 现有密码登录/注册流程完全不变
- JWT Token 生成/验证逻辑不变
- 安全过滤器链不变
