# App 用户认证与鉴权设计

## 概述

为 App 端用户设计独立的认证与鉴权体系，与后台 Admin 完全分离。支持邮箱密码登录、Google 第三方登录（预留），采用纯 JWT + Refresh Token 方案。

## 目标

- 支持全球用户使用
- 与后台 Admin 认证完全隔离
- 支持邮箱登录、Google 登录（预留）
- 简单角色体系（NORMAL、VIP）
- 支持多设备同时在线

---

## 一、数据库设计

### 1.1 grid_user 表（App 用户表）

```sql
CREATE TABLE IF NOT EXISTS `grid_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `password` VARCHAR(100) DEFAULT NULL COMMENT '密码（BCrypt加密）',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号（选填，暂不使用）',
    `phone_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '手机号是否验证：0-否 1-是',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `email_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '邮箱是否验证：0-否 1-是',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `register_ip` VARCHAR(50) DEFAULT NULL COMMENT '注册IP',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP用户表';
```

### 1.2 grid_user_auth 表（第三方登录关联表）

```sql
CREATE TABLE IF NOT EXISTS `grid_user_auth` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `provider` VARCHAR(30) NOT NULL COMMENT '第三方提供商：google/wechat/github/apple等',
    `provider_id` VARCHAR(100) NOT NULL COMMENT '第三方唯一标识',
    `provider_name` VARCHAR(100) DEFAULT NULL COMMENT '第三方用户名',
    `provider_avatar` VARCHAR(500) DEFAULT NULL COMMENT '第三方头像',
    `access_token` VARCHAR(500) DEFAULT NULL COMMENT '第三方访问令牌',
    `refresh_token` VARCHAR(500) DEFAULT NULL COMMENT '第三方刷新令牌',
    `expire_time` DATETIME DEFAULT NULL COMMENT '令牌过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider` (`provider`, `provider_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP用户第三方登录关联表';
```

### 1.3 grid_user_role 表（用户角色表）

```sql
CREATE TABLE IF NOT EXISTS `grid_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_code` VARCHAR(30) NOT NULL COMMENT '角色编码：NORMAL/VIP',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称：普通用户/VIP用户',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间（VIP用）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_code`),
    KEY `idx_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP用户角色表';
```

### 1.4 grid_user_token 表（Refresh Token 存储表）

```sql
CREATE TABLE IF NOT EXISTS `grid_user_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `device_id` VARCHAR(100) NOT NULL COMMENT '设备ID',
    `device_name` VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
    `refresh_token` VARCHAR(500) NOT NULL COMMENT '刷新令牌',
    `access_token` VARCHAR(500) DEFAULT NULL COMMENT '当前访问令牌',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_device` (`user_id`, `device_id`),
    KEY `idx_refresh_token` (`refresh_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP用户Token表';
```

---

## 二、架构设计

### 2.1 双 Security 配置隔离

```
┌─────────────────────────────────────────────────────────────────┐
│                         Spring Boot App                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────┐        ┌──────────────────────┐      │
│  │  SpringSecurity      │        │   AppSecurity        │      │
│  │  Config (@Order=2)   │        │   Config (@Order=1)  │      │
│  │                      │        │                      │      │
│  │  - 路径: 除/api/app  │        │   - 路径: /api/app/**│      │
│  │  - TokenFilter       │        │   - AppTokenFilter   │      │
│  │  - Redis在线状态     │        │   - 纯JWT方案        │      │
│  └──────────────────────┘        └──────────────────────┘      │
│           │                               │                    │
│  ┌────────▼───────┐            ┌─────────▼────────┐            │
│  │   sys_user    │            │    grid_user     │            │
│  │   (后台用户)   │            │    (App用户)      │            │
│  └────────────────┘            └──────────────────┘            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 关键类设计

| 类 | 说明 |
|---|------|
| `AppSecurityConfig` | App 端安全配置，@Order(1)，限定 /api/app/** |
| `AppTokenFilter` | App Token 过滤器 |
| `AppTokenProvider` | App Token 生成与解析 |
| `AppUserDetailsService` | App 用户详情加载 |
| `AppAuthService` | App 认证服务 |
| `AppSecurityUtils` | App 安全工具类（获取当前用户） |

---

## 三、认证流程

### 3.1 邮箱注册

**接口：** `POST /api/app/auth/register`

**请求体：**
```json
{
  "email": "user@gmail.com",
  "password": "RSA加密后的密码",
  "nickname": "昵称(可选)",
  "deviceId": "设备唯一标识",
  "deviceName": "设备名称(可选)"
}
```

**流程：**
1. 检查邮箱是否已注册
2. RSA 解密密码
3. BCrypt 加密存储密码
4. 创建 `grid_user` 记录
5. 分配默认 `NORMAL` 角色（插入 `grid_user_role`）
6. 生成 Access Token + Refresh Token
7. 存储 Refresh Token 到 `grid_user_token`
8. 返回 Token 对

**响应：**
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMi...",
    "refreshToken": "uuid-string",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "email": "user@gmail.com",
      "nickname": "昵称",
      "roles": ["NORMAL"]
    }
  }
}
```

### 3.2 邮箱登录

**接口：** `POST /api/app/auth/login`

**请求体：**
```json
{
  "email": "user@gmail.com",
  "password": "RSA加密后的密码",
  "deviceId": "设备唯一标识",
  "deviceName": "设备名称(可选)"
}
```

**流程：**
1. 根据邮箱查询用户
2. 检查用户状态（是否禁用）
3. RSA 解密密码，BCrypt 验证
4. 更新最后登录时间和 IP
5. 生成 Access Token + Refresh Token
6. 更新或插入 `grid_user_token`
7. 返回 Token 对

### 3.3 Token 刷新

**接口：** `POST /api/app/auth/refresh`

**请求体：**
```json
{
  "refreshToken": "刷新令牌"
}
```

**流程：**
1. 查询 `grid_user_token` 表验证 Refresh Token
2. 检查是否过期
3. 生成新的 Access Token + Refresh Token
4. 旧 Refresh Token 失效，新 Token 入库
5. 返回新 Token 对

### 3.4 登出

**接口：** `POST /api/app/auth/logout`

**请求体：**
```json
{
  "deviceId": "设备唯一标识"
}
```

**流程：**
1. 获取当前登录用户 ID
2. 删除 `grid_user_token` 中对应用户+设备的记录

### 3.5 Google 登录（预留）

**接口：** `POST /api/app/auth/google`

**请求体：**
```json
{
  "idToken": "Google ID Token",
  "deviceId": "设备唯一标识",
  "deviceName": "设备名称(可选)"
}
```

---

## 四、鉴权设计

### 4.1 Access Token 结构（JWT）

```json
{
  "sub": "user@gmail.com",
  "userId": 123,
  "deviceId": "device-uuid",
  "roles": ["NORMAL", "VIP"],
  "type": "access",
  "iat": 1716500000,
  "exp": 1717104800
}
```

**有效期：7天**

### 4.2 Refresh Token 策略

- 随机 UUID 字符串
- 有效期：30天
- 每次刷新时轮换（旧 Token 删除，新 Token 生成）
- 存储在 `grid_user_token` 表

### 4.3 AppTokenFilter 工作流程

```
请求到达
    ↓
匹配: /api/app/** ?
    ↓ (是)
提取 Header: Authorization: Bearer xxx
    ↓
AppTokenProvider 验证签名和过期时间
    ↓
解析 claims: userId, deviceId, roles
    ↓
构建 AppAuthenticationToken
    ↓
设置 SecurityContextHolder
    ↓
放行 → Controller
```

### 4.4 鉴权注解使用

```java
// 只要登录就能访问
@GetMapping("/profile")
public ResponseEntity<AppUserDTO> getProfile() { ... }

// 需要特定角色
@PreAuthorize("hasRole('VIP')")
@GetMapping("/vip-feature")
public ResponseEntity<?> vipFeature() { ... }

// 匿名访问
@AnonymousGetMapping("/public-data")
public ResponseEntity<?> getPublicData() { ... }
```

---

## 五、安全配置

### 5.1 AppSecurityConfig

```java
@Configuration
@Order(1)
@RequiredArgsConstructor
public class AppSecurityConfig {

    private final ApplicationContext applicationContext;
    private final AppTokenProvider appTokenProvider;
    private final SecurityProperties securityProperties;

    @Bean
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        Map<String, Set<String>> anonymousUrls = AnonTagUtils.getAnonymousUrl(applicationContext);
        AppTokenFilter appTokenFilter = new AppTokenFilter(appTokenProvider, securityProperties);

        return http
            .securityMatcher("/api/app/**")
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> {
                auth.antMatchers(HttpMethod.GET, anonymousUrls.get("GET").toArray(new String[0])).permitAll();
                auth.antMatchers(HttpMethod.POST, anonymousUrls.get("POST").toArray(new String[0])).permitAll();
                auth.antMatchers(HttpMethod.PUT, anonymousUrls.get("PUT").toArray(new String[0])).permitAll();
                auth.antMatchers(HttpMethod.PATCH, anonymousUrls.get("PATCH").toArray(new String[0])).permitAll();
                auth.antMatchers(HttpMethod.DELETE, anonymousUrls.get("DELETE").toArray(new String[0])).permitAll();
                auth.antMatchers(anonymousUrls.get("ALL").toArray(new String[0])).permitAll();
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(appTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### 5.2 SpringSecurityConfig 调整

```java
@Configuration
@Order(2)  // 加上优先级
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SpringSecurityConfig {
    // 其余保持不变
}
```

---

## 六、API 接口清单

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | `/api/app/auth/register` | 邮箱注册 | ❌ |
| POST | `/api/app/auth/login` | 邮箱登录 | ❌ |
| POST | `/api/app/auth/refresh` | 刷新 Token | ❌ |
| POST | `/api/app/auth/logout` | 登出 | ✅ |
| GET | `/api/app/users/profile` | 获取当前用户信息 | ✅ |
| PUT | `/api/app/users/profile` | 更新用户信息 | ✅ |
| POST | `/api/app/auth/google` | Google 登录（预留） | ❌ |

---

## 七、角色设计

| 角色编码 | 角色名称 | 说明 |
|---------|---------|------|
| `NORMAL` | 普通用户 | 默认角色 |
| `VIP` | VIP 用户 | 付费用户，有过期时间 |

---

## 八、配置项

```yaml
# application.yml
app:
  auth:
    token-expire-seconds: 604800        # Access Token 有效期：7天
    refresh-token-expire-seconds: 2592000  # Refresh Token 有效期：30天
```

---

## 九、迁移说明

1. 需要将现有的 `V1__Create_grid_user_table.sql` 替换为新的表结构
2. 新增 Flyway 迁移脚本创建其他三个表
