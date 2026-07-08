# Device Session Limit — 设计规格

**日期**: 2026-07-08  
**状态**: 设计中  
**目标**: 限制同一账号同时登录的设备数量，防止账号共享滥用，为订阅付费做准备

---

## 1. 背景与动机

当前系统（grid-app 模块）的认证流程：

1. 用户登录/注册（`AppAuthServiceImpl.generateToken()`）→ 生成 JWT（7天有效）+ refresh token（30天有效）
2. `DeviceManager.registerDevice()` 将 token 存入 `grid_user_token` 表
3. `AppTokenFilter` 仅校验 JWT 签名和过期时间，**不查数据库或 Redis**
4. JWT 是无状态的，签发后直到过期前始终有效

这意味着用户可以在任意多个设备上同时登录，无法限制并发会话数量。为了支持订阅付费，需要限制同一账号的最大同时登录设备数。

---

## 2. 需求摘要

| 项目 | 决策 |
|------|------|
| 最大同时登录设备数 | 3（可配置） |
| 超出上限时的策略 | 自动踢除最早登录的设备 |
| 被踢设备的 Token 生效窗口 | 立即可失效（每次请求校验 Redis 会话） |
| 设备标识 | 复用现有 `deviceId` 字段（客户端传入，已在 JWT claims 中） |

---

## 3. 设计方案

### 3.1 核心思路：Redis 会话管理 + 现有 DB token 表双轨

- **Redis Hash** 存储活跃会话，提供 O(1) 的会话校验
- **DB `grid_user_token` 表** 继续管理 refresh token 持久化
- `AppTokenFilter` 每次请求校验 Redis，被踢设备立即 401

### 3.2 Redis 数据结构

```
Key:   app:sessions:{userId}
Type:  Hash
Field: deviceId (String)
Value: JSON {"loginTime": <epoch_seconds>, "deviceName": "<string>"}
TTL:   与 JWT access token 有效期一致，每次添加/校验时刷新
```

**为什么用 Hash 而不是 Set？**

- `HLEN` → 设备计数
- `HEXISTS`（即 `hHasKey`）→ O(1) 检查设备是否有效（Filter 每请求调用）
- `HGETALL`（即 `hmget`）→ 登录时遍历找出最旧设备
- `HSET` / `HDEL` → 增删设备

所有操作都复用现有 `RedisUtils` 方法，无需新增任何 Redis 工具方法。

### 3.3 配置

在 `application.yml` 中新增一个配置项，通过 `@Value` 注入：

```yaml
app:
  auth:
    max-devices: 3  # 可配置为 1 即变单设备模式
```

### 3.4 流程

#### 3.4.1 登录（login / register / socialLogin / socialBindEmail）

```
用户提交凭证
  ↓
凭证校验通过
  ↓
hmget("app:sessions:{userId}") → 获取当前所有活跃session
  ↓
size >= maxDevices ?
  ├─ 是 → 遍历 sessions，找 loginTime 最小的 deviceId
  │       → HDEL 移除最旧设备
  │       → DB: 删除该设备对应的 GridUserToken 记录
  └─ 否 → 继续
  ↓
HSET deviceId → {"loginTime": now, "deviceName": "..."}
EXPIRE key → tokenExpireSeconds
  ↓
generateToken() → JWT + refresh token → DB 写 GridUserToken
  ↓
返回 token 给客户端
```

#### 3.4.2 TokenFilter 请求校验（AppTokenFilter）

```
提取 request header → JWT token
  ↓
现有: 校验签名 + 过期时间
  ↓
【新增】从 JWT claims 提取 userId + deviceId
  ↓
hHasKey("app:sessions:{userId}", deviceId) ?
  ├─ false → 不设置 SecurityContext（后续安全框架返回 401）
  └─ true → EXPIRE key（刷新 TTL，保持活跃）→ 继续
```

#### 3.4.3 Refresh Token（refreshToken）

```
客户端提交 refreshToken
  ↓
现有: 查 DB GridUserToken
  ↓
【新增】hHasKey("app:sessions:{userId}", deviceId) ?
  ├─ false → 拒绝，返回错误"设备已下线"
  └─ true → 继续
  ↓
现有: 删除旧 DB 记录
  ↓
generateToken() → 新 JWT + 新 refresh token
  ↓
HSET 更新 Redis（刷新 deviceName 等）+ EXPIRE 刷新 TTL
  ↓
返回新 token
```

#### 3.4.4 登出（logout）

```
POST /logout?deviceId={deviceId}
  ↓
HDEL("app:sessions:{userId}", deviceId)  // Redis 移除
  ↓
DB: deleteByUserIdAndDeviceId(userId, deviceId)  // 现有逻辑
```

### 3.5 容错设计

Redis 不可用时，降级到 DB 校验，避免阻断正常业务：

```java
// AppTokenFilter 中
try {
    boolean active = redisUtils.hHasKey(sessionKey, deviceId);
    if (!active) {
        // 不设置 Authentication → 401
        filterChain.doFilter(request, response);
        return;
    }
} catch (Exception e) {
    log.warn("Redis session check failed for userId={}, fallback to DB", userId, e);
    // 降级: 查 DB 确认该 device 是否有有效 token
    if (!userTokenRepository.findByUserIdAndDeviceId(userId, deviceId).isPresent()) {
        filterChain.doFilter(request, response);
        return;
    }
}
```

### 3.6 配置默认值

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `app.auth.max-devices` | 3 | 最大同时登录设备数 |
| `app.auth.token-expire-seconds` | 604800（不改） | JWT 有效期，Redis session TTL 与此保持一致 |

### 3.7 边界与限制

- **WebSocket / 长连接**: 不在此次设计范围内。WebSocket 有自己的连接生命周期，如需限制需单独设计。
- **管理后台（grid-system）**: 不受影响，此限制仅针对 `/api/app/**` 路径。
- **设备 ID 唯一性**: 依赖客户端传入的 `deviceId`。如果客户端故意传不同的 `deviceId`，无法通过服务端区分。这是当前架构的固有限制，订阅付费场景下可接受。
- **JWT TTL 保持不变**: 虽然 JWT 有效期为 7 天，但由于 `AppTokenFilter` 每次请求校验 Redis 会话，被踢设备会立即失效，不需要缩短 JWT 有效期。

---

## 4. 涉及文件

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `grid-bootstrap/.../config/application.yml` | 新增配置 | `app.auth.max-devices: 3` |
| `grid-app/.../security/AppTokenFilter.java` | 修改 | 新增构造参数 `RedisUtils`、新增 Redis 会话校验逻辑 |
| `grid-app/.../config/AppSecurityConfig.java` | 修改 | `AppTokenFilter` 构造传入 `RedisUtils` |
| `grid-app/.../service/impl/AppAuthServiceImpl.java` | 修改 | login/register/refresh/logout/socialLogin 中增加 Redis 会话管理 |
| `grid-app/.../security/DeviceManager.java` | 无改动 | 继续负责 DB 设备记录管理 |
| `grid-app/.../security/SessionManager.java` | 新建 | Redis 会话管理（增/删/查/踢） |

### 不需要改动的

- JWT 结构（deviceId 已在 claims 中）
- 客户端 API 接口签名（deviceId 已在 LoginDTO/RegisterDTO 中）
- 数据库表结构
- `GridUserToken` 实体
- `RedisUtils` 工具类

---

## 5. 测试要点

1. **正常登录**: 新设备登录成功，Redis Hash 中新增 deviceId
2. **设备数未达上限**: 第 1-3 个设备登录均成功
3. **超出上限**: 第 4 个设备登录时，最早登录的设备被踢
4. **被踢设备请求 API**: 返回 401
5. **被踢设备 refresh token**: 返回错误
6. **登出**: Redis + DB 同步清理，释放一个设备名额
7. **Redis 不可用**: 降级到 DB 校验，业务不中断
8. **单设备模式**: `max-devices: 1` 下新登录踢旧登录
