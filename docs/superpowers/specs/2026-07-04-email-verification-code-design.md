# 邮箱验证码注册设计

## 概述

在 App 端用户注册流程中新增邮箱验证码校验环节，防止批量注册、无效邮箱和自动化攻击。

## 目标

- 注册前必须通过邮箱验证码验证
- 防止同一邮箱短时间内重复发送验证码
- 最小改动，复用现有 Redis + EmailService 基础设施
- 不影响现有登录、Token 刷新等其他流程

---

## 一、流程设计

```
用户填写邮箱
    ↓
POST /api/app/auth/send-code { email }
    ↓
检查邮箱是否已被注册 → 是 → 返回错误"邮箱已被注册"
    ↓ 否
检查 Redis 冷却期 → 是 → 返回错误"请60秒后再试"
    ↓ 否
生成6位随机数字验证码 → 存入 Redis (TTL 300s)
    ↓
设置冷却标记 Redis (TTL 60s)
    ↓
发送 HTML 邮件
    ↓
返回 { message: "验证码已发送，5分钟内有效" }

用户填写完整注册信息（含验证码）
    ↓
POST /api/app/auth/register { email, password, nickname, deviceId, code }
    ↓
从 Redis 读取验证码 → 为空 → 返回错误"验证码不存在或已过期"
    ↓
比对验证码 → 不匹配 → 返回错误"验证码错误"
    ↓ 匹配成功
删除 Redis 验证码
    ↓
继续原有注册逻辑（RSA解密、BCrypt加密、创建用户、发放试用等）
```

---

## 二、API 设计

### 2.1 发送验证码

```
POST /api/app/auth/send-code    @AnonymousPostMapping
```

**请求体：**

| 字段 | 类型 | 校验 |
|------|------|------|
| email | String | @NotBlank + @Email |

**响应（成功）：**
```json
{
  "message": "验证码已发送，5分钟内有效"
}
```

**响应（邮箱已注册）：**
```json
{
  "code": 400,
  "message": "邮箱已被注册"
}
```

**响应（冷却期内）：**
```json
{
  "code": 400,
  "message": "验证码已发送，请60秒后重试"
}
```

### 2.2 注册接口变更

`POST /api/app/auth/register` — `RegisterDTO` 新增字段：

| 字段 | 类型 | 校验 |
|------|------|------|
| code | String | @NotBlank(message = "验证码不能为空") |

---

## 三、Redis Key 设计

遵循项目已有 Redis key 命名惯例（参考后台验证码 `properties.getCodeKey()`）。

| Key | 值 | TTL | 说明 |
|-----|-----|-----|------|
| `email:code:{email}` | 6位数字字符串 | 300s (5分钟) | 验证码 |
| `email:code:cooldown:{email}` | "1" | 60s | 发送冷却标记 |

---

## 四、邮件内容

```html
<div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
  <h2 style="color: #333;">有路中文</h2>
  <p>您的邮箱验证码是：</p>
  <div style="font-size: 28px; font-weight: bold; color: #1890ff; letter-spacing: 6px; padding: 12px 0;">
    {code}
  </div>
  <p style="color: #999; font-size: 14px;">验证码5分钟内有效，请勿转发给他人。</p>
  <p style="color: #999; font-size: 14px;">如非本人操作，请忽略此邮件。</p>
</div>
```

---

## 五、涉及文件

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `grid-app/.../rest/AppAuthController.java` | 修改 | 新增 `sendCode()` 端点 |
| `grid-app/.../service/AppAuthService.java` | 修改 | 接口新增 `sendCode()` 方法 |
| `grid-app/.../service/impl/AppAuthServiceImpl.java` | 修改 | 实现 `sendCode()`，`register()` 增加验证码校验 |
| `grid-app/.../service/dto/RegisterDTO.java` | 修改 | 新增 `code` 字段 |
| `grid-app/.../service/dto/SendCodeDTO.java` | **新增** | `email` 字段 |

---

## 六、实现细节

### 6.1 AppAuthServiceImpl.sendCode()

```java
@Override
public void sendCode(SendCodeDTO dto) {
    // 1. 检查邮箱是否已被注册
    if (userRepository.existsByEmail(dto.getEmail())) {
        throw new BadRequestException("邮箱已被注册");
    }
    // 2. 检查冷却期
    String cooldownKey = "email:code:cooldown:" + dto.getEmail();
    if (redisUtils.hasKey(cooldownKey)) {
        throw new BadRequestException("验证码已发送，请60秒后重试");
    }
    // 3. 生成6位验证码
    String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));
    // 4. 存入Redis（5分钟有效）
    String codeKey = "email:code:" + dto.getEmail();
    redisUtils.set(codeKey, code, 5, TimeUnit.MINUTES);
    // 5. 设置冷却标记
    redisUtils.set(cooldownKey, "1", 60, TimeUnit.SECONDS);
    // 6. 发送邮件
    emailService.sendHtmlEmail(dto.getEmail(), "有路中文 - 邮箱验证码", buildCodeEmail(code));
}
```

### 6.2 AppAuthServiceImpl.register() 变更

在现有 `existsByEmail` 检查之后，新增验证码校验：

```java
// 新增：校验邮箱验证码
String codeKey = "email:code:" + registerDTO.getEmail();
String savedCode = redisUtils.get(codeKey, String.class);
if (savedCode == null) {
    throw new BadRequestException("验证码不存在或已过期");
}
if (!savedCode.equals(registerDTO.getCode())) {
    throw new BadRequestException("验证码错误");
}
redisUtils.del(codeKey);
// 继续原有注册逻辑...
```

### 6.3 RedisUtils 补充

需要确认 `RedisUtils` 是否有 `hasKey` 方法。如果没有，使用 `redisUtils.get(key, String.class)` 判空来替代冷却检查。

---

## 七、安全考量

1. **冷却期**：同一邮箱 60 秒内只能发送一次，防止邮件轰炸
2. **验证码一次性**：验证成功后立即删除 Redis key，防止重复使用
3. **验证码有效期**：5 分钟，平衡安全性和用户体验
4. **不泄露邮箱存在性**：仅在用户主动输入验证码并提交注册时才暴露邮箱是否已注册（`sendCode` 接口会返回"邮箱已被注册"，这是可接受的，因为邮箱注册本身就是公开操作）
5. **邮件发送失败处理**：`EmailService` 发送失败会抛 `RuntimeException`，Controller 返回 500，验证码不会因此泄漏（Redis 中的验证码无人知晓）

---

## 八、不涉及的范围

- 不修改登录流程
- 不修改机构注册流程（机构注册已有完整的审核机制）
- 不新增数据库表
- 不修改前端（前端需自行适配新增的 `sendCode` 接口和 `register` 的 `code` 字段）
