# App 用户个人中心设计

## 概述

实现 App 端用户个人中心相关接口，包括个人信息查询/更新、头像上传、修改密码、账号注销、三方账号绑定/解绑，同时新增 HSK 等级和个性签名字段。

## 数据库变更

修改 `sql/normal_user.sql` 中 `grid_user` 建表语句（直接修改 DDL，无需 ALTER TABLE 迁移）：

1. **avatar**: `VARCHAR(500)` → `BIGINT DEFAULT NULL`，存储 `oss_resource_meta.id`
2. **hsk_level**: 新增 `VARCHAR(20) DEFAULT '0'`，对应 `HskLevelEnum`，默认 `'0'` 表示未设置
3. **signature**: 新增 `VARCHAR(200) DEFAULT NULL`，个性签名，可空

```sql
CREATE TABLE `grid_user` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username`        VARCHAR(50)  NULL UNIQUE,
    `password`        VARCHAR(100) NULL,
    `phone`           VARCHAR(20)  NULL UNIQUE,
    `phone_verified`  TINYINT      NOT NULL DEFAULT 0,
    `email`           VARCHAR(100) NULL UNIQUE,
    `email_verified`  TINYINT      NOT NULL DEFAULT 0,
    `nickname`        VARCHAR(50)  NULL,
    `avatar`          BIGINT       DEFAULT NULL,
    `gender`          TINYINT      DEFAULT 0,
    `hsk_level`       VARCHAR(20)  DEFAULT '0',
    `signature`       VARCHAR(200) DEFAULT NULL,
    -- ... 其他字段保持不变
);
```

## 实体变更

### GridUser.java

- `avatar`: `String` → `Long`（对应 `oss_resource_meta.id`）
- 新增 `hskLevel`: `String`，`@Column(name = "hsk_level")`
- 新增 `signature`: `String`

### GridUserAuth.java

不变。

## API 设计

所有接口挂载在 `AppUserController` 下，base path: `/api/app/user`，全部需要 App Token 认证。

### 1. 查询个人信息

```
GET /api/app/user/profile
```

**响应** `AppUserProfileVO`:
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "小明",
  "avatarUrl": "https://oss-cn-beijing.aliyuncs.com/avatar/2026-07/uuid.png",
  "gender": 1,
  "hskLevel": "3",
  "signature": "我爱学中文",
  "userType": "NORMAL",
  "region": "A",
  "phone": "138****0000",
  "emailVerified": 1,
  "createdAt": "2026-07-01T12:00:00"
}
```

**数据流**: Token → userId → `GridUserRepository.findById()` → Controller 通过 `OssResourceService.findById()` 解析 `avatar` ID 为 URL → `AppUserWrapper.toProfileVO(user, avatarUrl)`

### 2. 更新个人信息

```
PUT /api/app/user/profile
```

**请求** `UpdateProfileRequest`:
```json
{
  "nickname": "新昵称",       // 可选
  "gender": 2,                // 可选，0/1/2
  "hskLevel": "3",            // 可选
  "signature": "新签名"        // 可选
}
```

- 仅更新非 null 字段
- `email` 不可通过此接口更新
- 返回完整 `AppUserProfileVO`

### 3. 更新头像

```
PUT /api/app/user/avatar
```

**请求** `UpdateAvatarRequest`:
```json
{
  "ossImageId": 12345
}
```

- 通过 `OssResourceService.findById(ossImageId)` 验证资源存在
- `user.setAvatar(ossImageId)` → 保存
- 返回更新后的 `AppUserProfileVO`（含解析后的 avatarUrl）

### 4. 修改密码

```
PUT /api/app/user/password
```

**请求** `ChangePasswordRequest`:
```json
{
  "oldPassword": "RSA加密的旧密码",
  "newPassword": "RSA加密的新密码"
}
```

- RSA 解密 → BCrypt 验证旧密码
- BCrypt 加密新密码 → 保存
- 调用 `DeviceManager.removeAllDevices(userId)` 清除所有设备 token，强制重新登录
- 无密码用户（社交登录）不可修改密码

### 5. 注销账号

```
DELETE /api/app/user/account
```

**请求** `DeleteAccountRequest`:
```json
{
  "password": "RSA加密的密码",
  "emailCode": "邮箱验证码"
}
```

- 验证密码 + 邮箱验证码（双重验证）
- `user.setStatus(0)` 软删除，用户数据保留
- 清除所有设备 token
- 无密码用户（社交登录）仅需邮箱验证码

### 6. 查询已绑定三方账号

```
GET /api/app/user/social-accounts
```

**响应** `List<AppSocialAccountVO>`:
```json
[
  {
    "id": 1,
    "provider": "google",
    "providerName": "John Doe",
    "providerAvatar": "https://lh3.googleusercontent.com/...",
    "createdAt": "2026-07-01T12:00:00"
  }
]
```

### 7. 绑定三方账号

```
POST /api/app/user/social-accounts
```

**请求** `BindSocialRequest`:
```json
{
  "provider": "google",
  "idToken": "eyJhbGciOiJSUzI1Ni..."
}
```

- 复用 `IdTokenVerifier.verify(provider, idToken)` 验证 token
- 检查 `provider + providerId` 未被其他用户绑定
- 创建 `GridUserAuth` 关联到当前 userId
- 若用户无昵称，从社交信息填充；头像仅存储在 `GridUserAuth.providerAvatar` 中，不写入 `GridUser.avatar`（avatar 现在是 BIGINT OSS ID 类型，社交头像 URL 不能直接存入）

### 8. 解绑三方账号

```
DELETE /api/app/user/social-accounts/{authId}
```

- 验证 `GridUserAuth` 属于当前用户
- 安全检查：至少保留一种登录方式
  - 用户有密码 → 允许解绑
  - 用户无密码且只有一个社交绑定 → 拒绝，提示"请先设置密码"
- 删除 `GridUserAuth` 记录

## 注册接口变更

`RegisterDTO` 新增可选字段：
- `hskLevel`: String，可选，默认 `"0"`
- `signature`: String，可选

`AppAuthServiceImpl.register()` 在创建 `GridUser` 时设置这些字段。

## 文件清单

### 修改

| 文件 | 变更 |
|------|------|
| `sql/normal_user.sql` | avatar 改 BIGINT，新增 hsk_level、signature |
| `GridUser.java` | avatar 类型改为 Long，新增 hskLevel、signature |
| `RegisterDTO.java` | 新增 hskLevel、signature 字段 |
| `AppAuthServiceImpl.java` | register() 处理新字段 |

### 新增

| 文件 | 说明 |
|------|------|
| `grid-app/.../rest/AppUserController.java` | 8 个接口的 Controller |
| `grid-app/.../service/AppUserService.java` | 接口定义 |
| `grid-app/.../service/impl/AppUserServiceImpl.java` | 实现类 |
| `grid-app/.../service/dto/UpdateProfileRequest.java` | 更新个人信息请求 |
| `grid-app/.../service/dto/UpdateAvatarRequest.java` | 更新头像请求 |
| `grid-app/.../service/dto/ChangePasswordRequest.java` | 修改密码请求 |
| `grid-app/.../service/dto/DeleteAccountRequest.java` | 注销账号请求 |
| `grid-app/.../service/dto/BindSocialRequest.java` | 绑定三方账号请求 |
| `grid-app/.../rest/vo/AppUserProfileVO.java` | 个人信息响应 VO |
| `grid-app/.../rest/vo/AppSocialAccountVO.java` | 三方账号响应 VO |
| `grid-app/.../rest/wrapper/AppUserWrapper.java` | 静态转换方法 |

## 边界情况

- **未设置头像的用户**：`avatar` 为 NULL，返回 VO 中 `avatarUrl` 为 null
- **社交登录用户无密码**：修改密码接口返回错误；注销仅需邮箱验证码
- **邮箱未验证用户**：注销时不可使用邮箱验证码；`emailVerified=0` 时提示先验证
- **Provider 未配置**：`IdTokenVerifier` 抛出 `BadRequestException`
- **社交账号已被其他用户绑定**：返回错误提示

## 不纳入当前范围

- 手机号绑定/修改
- 邮箱修改（用户明确说邮箱不可修改）
- 用户名修改
- 学习统计数据（依赖其他模块，后续迭代）
