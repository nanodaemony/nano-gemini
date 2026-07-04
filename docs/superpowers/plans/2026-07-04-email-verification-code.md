# 邮箱验证码注册 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 App 用户注册流程中新增邮箱验证码校验，注册前必须先通过邮箱验证。

**Architecture:** 新增 `POST /api/app/auth/send-code` 端点发送验证码（Redis 存储，5分钟有效，60秒冷却），修改 `register` 端点增加验证码校验。复用现有 `EmailService` 和 `RedisUtils`。

**Tech Stack:** Spring Boot 2.7.18, Redis (RedisUtils), JavaMailSender (EmailService), JSR-303 Validation

## Global Constraints

- Java 8
- 密码传输必须 RSA 加密（客户端用公钥加密）
- 密码存储必须 BCrypt
- 所有 Controller 端点遵循 Swagger/Knife4j 注解风格
- 异常统一通过 `BadRequestException` 抛出
- 遵守 Wrapper 模式：Controller 不包含转换逻辑

---

### Task 1: 创建 SendCodeDTO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SendCodeDTO.java`

**Interfaces:**
- Produces: `SendCodeDTO` 类，包含 `email` 字段（`@NotBlank` + `@Email`），被 Task 3 和 Task 4 使用

- [ ] **Step 1: 创建 SendCodeDTO**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class SendCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SendCodeDTO.java
git commit -m "feat: add SendCodeDTO for email verification code request"
```

---

### Task 2: AppAuthService 接口新增 sendCode 方法

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/AppAuthService.java`

**Interfaces:**
- Consumes: `SendCodeDTO` (Task 1)
- Produces: `void sendCode(SendCodeDTO dto)` 方法签名，被 Task 4 和 Task 6 使用

- [ ] **Step 1: 新增方法签名**

在 `AppAuthService` 接口中加入：

```java
package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.service.dto.LoginDTO;
import com.naon.grid.modules.app.service.dto.RegisterDTO;
import com.naon.grid.modules.app.service.dto.SendCodeDTO;
import com.naon.grid.modules.system.service.dto.TokenDTO;

import javax.servlet.http.HttpServletRequest;

public interface AppAuthService {
    TokenDTO register(RegisterDTO registerDTO, HttpServletRequest request);
    TokenDTO login(LoginDTO loginDTO, HttpServletRequest request);
    void logout(Long userId, String deviceId);
    TokenDTO refreshToken(String refreshToken);
    void sendCode(SendCodeDTO dto);
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/AppAuthService.java
git commit -m "feat: add sendCode method to AppAuthService interface"
```

---

### Task 3: RegisterDTO 新增 code 字段

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/RegisterDTO.java`

**Interfaces:**
- Produces: `RegisterDTO.code` 字段（`@NotBlank`），被 Task 5 接收

- [ ] **Step 1: 在 RegisterDTO 中新增 code 字段**

当前 RegisterDTO 内容：
```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private String deviceName;

    private String referralCode;
}
```

在 `referralCode` 字段之后新增：

```java
    @NotBlank(message = "验证码不能为空")
    private String code;
```

完整文件变为：

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private String deviceName;

    private String referralCode;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/RegisterDTO.java
git commit -m "feat: add code field to RegisterDTO for email verification"
```

---

### Task 4: AppAuthServiceImpl 实现 sendCode 方法

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java`

**Interfaces:**
- Consumes: `SendCodeDTO` (Task 1), `AppAuthService.sendCode` 签名 (Task 2)
- Produces: `sendCode()` 实现，完成验证码生成、Redis 存储、冷却控制和邮件发送

- [ ] **Step 1: 新增依赖注入和 sendCode 实现**

当前 `AppAuthServiceImpl` 的字段和构造函数：
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuthServiceImpl implements AppAuthService {

    private final GridUserRepository userRepository;
    private final GridUserRoleRepository userRoleRepository;
    private final GridUserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppTokenProvider appTokenProvider;
    private final DeviceManager deviceManager;
    private final SubscriptionService subscriptionService;
    private final EntitlementEngine entitlementEngine;
    private final ReferralService referralService;
    private final RegionResolver regionResolver;
```

在 `regionResolver` 之后新增 `RedisUtils` 和 `EmailService` 字段：

```java
    private final RedisUtils redisUtils;
    private final EmailService emailService;
```

在 `refreshToken` 方法之后、`logout` 方法之前，新增 `sendCode` 方法实现：

```java
    @Override
    public void sendCode(SendCodeDTO dto) {
        // 1. 检查邮箱是否已被注册
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("邮箱已被注册");
        }

        // 2. 检查冷却期（60秒内不允许重复发送）
        String cooldownKey = "email:code:cooldown:" + dto.getEmail();
        if (redisUtils.hasKey(cooldownKey)) {
            throw new BadRequestException("验证码已发送，请60秒后重试");
        }

        // 3. 生成6位数字验证码
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        // 4. 存入Redis（5分钟有效）
        String codeKey = "email:code:" + dto.getEmail();
        redisUtils.set(codeKey, code, 5, TimeUnit.MINUTES);

        // 5. 设置冷却标记（60秒）
        redisUtils.set(cooldownKey, "1", 60, TimeUnit.SECONDS);

        // 6. 发送邮件
        emailService.sendHtmlEmail(dto.getEmail(), "有路中文 - 邮箱验证码", buildCodeEmail(code));

        log.info("Verification code sent to: {}", dto.getEmail());
    }

    private String buildCodeEmail(String code) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;\">"
                + "<h2 style=\"color: #333;\">有路中文</h2>"
                + "<p>您的邮箱验证码是：</p>"
                + "<div style=\"font-size: 28px; font-weight: bold; color: #1890ff; letter-spacing: 6px; padding: 12px 0;\">"
                + code
                + "</div>"
                + "<p style=\"color: #999; font-size: 14px;\">验证码5分钟内有效，请勿转发给他人。</p>"
                + "<p style=\"color: #999; font-size: 14px;\">如非本人操作，请忽略此邮件。</p>"
                + "</div>";
    }
```

需要新增的 import：
```java
import com.naon.grid.modules.app.service.dto.SendCodeDTO;
import com.naon.grid.service.EmailService;
import com.naon.grid.utils.RedisUtils;
import java.util.concurrent.TimeUnit;
```

- [ ] **Step 2: 编译验证**

```bash
cd grid-app && mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java
git commit -m "feat: implement sendCode with Redis-backed verification code and email sending"
```

---

### Task 5: AppAuthServiceImpl.register() 新增验证码校验

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java`

**Interfaces:**
- Consumes: `RegisterDTO.code` (Task 3)
- Modifies: `register()` 方法，在邮箱唯一性检查之后、密码解密之前，插入验证码校验逻辑

- [ ] **Step 1: 在 register() 方法中新增验证码校验**

当前 `register()` 方法开头：
```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO register(RegisterDTO registerDTO, HttpServletRequest request) {
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new BadRequestException("邮箱已被注册");
        }

        String decryptedPassword;
```

在 `existsByEmail` 检查之后、`decryptedPassword` 声明之前，新增：

```java
        // 校验邮箱验证码
        String codeKey = "email:code:" + registerDTO.getEmail();
        String savedCode = redisUtils.get(codeKey, String.class);
        if (savedCode == null) {
            throw new BadRequestException("验证码不存在或已过期");
        }
        if (!savedCode.equals(registerDTO.getCode())) {
            throw new BadRequestException("验证码错误");
        }
        // 验证通过，删除验证码（一次性使用）
        redisUtils.del(codeKey);
```

完整变更后的 `register()` 方法开头部分：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO register(RegisterDTO registerDTO, HttpServletRequest request) {
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new BadRequestException("邮箱已被注册");
        }

        // 校验邮箱验证码
        String codeKey = "email:code:" + registerDTO.getEmail();
        String savedCode = redisUtils.get(codeKey, String.class);
        if (savedCode == null) {
            throw new BadRequestException("验证码不存在或已过期");
        }
        if (!savedCode.equals(registerDTO.getCode())) {
            throw new BadRequestException("验证码错误");
        }
        // 验证通过，删除验证码（一次性使用）
        redisUtils.del(codeKey);

        String decryptedPassword;
        try {
            decryptedPassword = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, registerDTO.getPassword());
        } catch (Exception e) {
            throw new BadRequestException("密码解密失败");
        }
        // ... 后续逻辑不变
```

- [ ] **Step 2: 编译验证**

```bash
cd grid-app && mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java
git commit -m "feat: add email verification code validation in register"
```

---

### Task 6: AppAuthController 新增 sendCode 端点

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppAuthController.java`

**Interfaces:**
- Consumes: `AppAuthService.sendCode()` (Task 2)
- Produces: `POST /api/app/auth/send-code` 端点

- [ ] **Step 1: 新增 sendCode 端点**

在 `AppAuthController` 的 `getPublicKey` 方法之前新增：

```java
    @Log("发送邮箱验证码")
    @ApiOperation("发送邮箱验证码")
    @AnonymousPostMapping("/send-code")
    public ResponseEntity<Map<String, String>> sendCode(@Validated @RequestBody SendCodeDTO sendCodeDTO) {
        appAuthService.sendCode(sendCodeDTO);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送，5分钟内有效");
        return ResponseEntity.ok(result);
    }
```

需要新增的 import：
```java
import com.naon.grid.modules.app.service.dto.SendCodeDTO;
```

- [ ] **Step 2: 编译验证**

```bash
cd grid-app && mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppAuthController.java
git commit -m "feat: add POST /api/app/auth/send-code endpoint for email verification"
```

---

### Task 7: 全量编译与功能验证

**Files:**
- 无新增或修改文件

- [ ] **Step 1: 全项目编译**

```bash
cd grid-bootstrap && mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 功能验证清单**

| # | 测试场景 | 预期结果 |
|---|---------|---------|
| 1 | `POST /api/app/auth/send-code` `{"email":"new@test.com"}` | 返回 200，收到验证码邮件 |
| 2 | 立即再次调用 sendCode（同邮箱） | 返回 400 "验证码已发送，请60秒后重试" |
| 3 | `POST /api/app/auth/send-code` `{"email":"已注册的邮箱"}` | 返回 400 "邮箱已被注册" |
| 4 | `POST /api/app/auth/register` 不带 code 字段 | 返回 400 校验失败 "验证码不能为空" |
| 5 | `POST /api/app/auth/register` 带错误 code | 返回 400 "验证码错误" |
| 6 | 等待5分钟后 register 带正确 code | 返回 400 "验证码不存在或已过期" |
| 7 | sendCode → 输入正确code → register 完整流程 | 返回 200，注册成功，返回 Token |
| 8 | 同一 code 再次用于 register | 返回 400 "验证码不存在或已过期"（已被删除） |
| 9 | `POST /api/app/auth/login` 正常登录 | 不受影响，正常返回 Token |
| 10 | `POST /api/app/institution/register` | 不受影响，正常提交申请 |

- [ ] **Step 3: 提交（如有遗漏修复）**

```bash
git add -A && git commit -m "chore: final verification and fixes for email code feature"
```
