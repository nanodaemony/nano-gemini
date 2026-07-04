# Google / Twitter OAuth 登录实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 grid-app 新增 Google 和 Twitter OAuth 登录，客户端传入 ID Token，服务端 JWKS 公钥验签后自动创建/关联账号并下发 JWT。

**Architecture:** `IdTokenVerifier` 通过 JWKS 公钥验证 Google/Twitter ID Token（RS256 签名），提取 `sub`/`email`/`name`/`picture` 等 claims。核心社交登录逻辑实现在 `AppAuthServiceImpl` 中，复用已有的 `generateToken`/`convertToDTO` 等私有方法。无邮箱时（Twitter）抛 `BindEmailRequiredException`，Controller 捕获后返回 `{requireBindEmail, bindToken}` 让前端引导用户绑定邮箱。bindToken 为 UUID，关联的 social claims 存 Redis 5 分钟。

**Tech Stack:** Java 8, Spring Boot 2.7.18, jjwt 0.11.5, Fastjson2, JPA/Hibernate, MySQL, Redis

## Global Constraints

- Java 8 兼容（无 `var`、无 text blocks、无 record）
- 遵循现有 Controller → Service → Repository 分层
- Controller 使用 `@AnonymousPostMapping` 标记公开端点
- DTO 使用 Lombok `@Data`，验证用 `javax.validation.constraints`
- 密码为 `null` 表示 OAuth 用户（`GridUser.password` 已 `nullable`）
- `.env` 文件管理配置，使用 `@ConfigurationProperties` 注入
- `GridUserAuth` 表已存在，无需 DDL 变更

---

### Task 1: 创建 SocialLoginProperties 配置类

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/config/SocialLoginProperties.java`

**Interfaces:**
- Produces: `SocialLoginProperties.getProvider(String name)` → `ProviderConfig` (包含 `clientId`, `issuer`, `jwksUrl` 字段)

- [ ] **Step 1: 编写 SocialLoginProperties**

```java
package com.naon.grid.modules.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "social")
public class SocialLoginProperties {

    private Map<String, ProviderConfig> providers;

    @Data
    public static class ProviderConfig {
        private String clientId;
        private String issuer;
        private String jwksUrl;
    }

    public ProviderConfig getProvider(String name) {
        return providers != null ? providers.get(name) : null;
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd grid-app && mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/config/SocialLoginProperties.java
git commit -m "feat: add SocialLoginProperties config class for OAuth providers"
```

---

### Task 2: 创建 DTO 和内部模型类

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SocialLoginDTO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SocialBindEmailDTO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/security/SocialUserInfo.java`

**Interfaces:**
- Produces:
  - `SocialLoginDTO`: `provider` (String, @NotBlank), `idToken` (String, @NotBlank), `deviceId` (String, @NotBlank), `deviceName` (String)
  - `SocialBindEmailDTO`: `bindToken` (String, @NotBlank), `email` (String, @NotBlank @Email), `code` (String, @NotBlank), `deviceId` (String, @NotBlank), `deviceName` (String)
  - `SocialUserInfo`: `provider`, `providerId`, `email`, `emailVerified` (boolean), `name`, `picture`, `expireTime` (Date) — 使用 Lombok `@Builder`

- [ ] **Step 1: 编写 SocialLoginDTO**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class SocialLoginDTO {

    @NotBlank(message = "第三方平台不能为空")
    private String provider;

    @NotBlank(message = "身份令牌不能为空")
    private String idToken;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private String deviceName;
}
```

- [ ] **Step 2: 编写 SocialBindEmailDTO**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class SocialBindEmailDTO {

    @NotBlank(message = "绑定令牌不能为空")
    private String bindToken;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private String deviceName;
}
```

- [ ] **Step 3: 编写 SocialUserInfo**

```java
package com.naon.grid.modules.app.security;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class SocialUserInfo {

    private String provider;
    private String providerId;
    private String email;
    private boolean emailVerified;
    private String name;
    private String picture;
    private Date expireTime;
}
```

- [ ] **Step 4: 验证编译**

```bash
cd grid-app && mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SocialLoginDTO.java \
        grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SocialBindEmailDTO.java \
        grid-app/src/main/java/com/naon/grid/modules/app/security/SocialUserInfo.java
git commit -m "feat: add SocialLoginDTO, SocialBindEmailDTO, SocialUserInfo for OAuth login"
```

---

### Task 3: 创建 IdTokenVerifier（JWKS 验签）

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/security/IdTokenVerifier.java`

**Interfaces:**
- Consumes: `SocialLoginProperties` (Task 1), `SocialUserInfo` (Task 2)
- Produces: `IdTokenVerifier.verify(String provider, String idToken)` → `SocialUserInfo`

- [ ] **Step 1: 编写 IdTokenVerifier**

```java
package com.naon.grid.modules.app.security;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.config.SocialLoginProperties;
import com.naon.grid.modules.app.config.SocialLoginProperties.ProviderConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdTokenVerifier {

    private final SocialLoginProperties socialLoginProperties;

    public SocialUserInfo verify(String provider, String idToken) {
        ProviderConfig config = socialLoginProperties.getProvider(provider);
        if (config == null) {
            throw new BadRequestException("不支持的第三方登录方式: " + provider);
        }

        // Parse JWT header to get kid
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new BadRequestException("登录验证失败");
        }
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        JSONObject header = JSON.parseObject(headerJson);
        String kid = header.getString("kid");
        String alg = header.getString("alg");

        if (!"RS256".equals(alg)) {
            throw new BadRequestException("登录验证失败");
        }

        // Fetch JWKS and build public key
        PublicKey publicKey = getPublicKey(config.getJwksUrl(), kid);

        // Verify signature and extract claims
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();
        } catch (Exception e) {
            log.warn("ID token verification failed for provider={}: {}", provider, e.getMessage());
            throw new BadRequestException("登录验证失败");
        }

        // Validate audience (aud can be String or array)
        if (!audienceMatches(claims.get("aud"), config.getClientId())) {
            throw new BadRequestException("登录验证失败");
        }

        // Validate issuer
        String iss = claims.getIssuer();
        String expectedIss = config.getIssuer();
        if (iss == null || !issuerMatches(iss, expectedIss)) {
            throw new BadRequestException("登录验证失败");
        }

        // Validate expiration
        if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
            throw new BadRequestException("登录凭证已过期，请重新授权");
        }

        String email = claims.get("email", String.class);
        Boolean emailVerified = claims.get("email_verified", Boolean.class);

        return SocialUserInfo.builder()
                .provider(provider)
                .providerId(claims.getSubject())
                .email(email != null ? email.trim().toLowerCase() : null)
                .emailVerified(emailVerified != null && emailVerified)
                .name(claims.get("name", String.class))
                .picture(claims.get("picture", String.class))
                .expireTime(claims.getExpiration())
                .build();
    }

    private boolean audienceMatches(Object aud, String clientId) {
        if (aud instanceof String) {
            return aud.equals(clientId);
        }
        if (aud instanceof java.util.List) {
            return ((java.util.List<?>) aud).contains(clientId);
        }
        return false;
    }

    private boolean issuerMatches(String iss, String expectedIss) {
        if (iss.equals(expectedIss)) {
            return true;
        }
        // Google: "accounts.google.com" vs "https://accounts.google.com"
        if (iss.startsWith("https://")) {
            return iss.substring("https://".length()).equals(expectedIss);
        }
        return ("https://" + iss).equals(expectedIss);
    }

    private PublicKey getPublicKey(String jwksUrl, String kid) {
        try {
            String jwksJson = fetchJwks(jwksUrl);
            JSONObject jwks = JSON.parseObject(jwksJson);
            JSONArray keys = jwks.getJSONArray("keys");
            if (keys == null || keys.isEmpty()) {
                throw new BadRequestException("登录验证失败");
            }

            for (int i = 0; i < keys.size(); i++) {
                JSONObject key = keys.getJSONObject(i);
                if (kid != null && kid.equals(key.getString("kid"))) {
                    return buildRsaPublicKey(key.getString("n"), key.getString("e"));
                }
            }

            // If kid not found and only one key, try it (edge case)
            if (kid == null && keys.size() == 1) {
                JSONObject key = keys.getJSONObject(0);
                return buildRsaPublicKey(key.getString("n"), key.getString("e"));
            }

            throw new BadRequestException("登录验证失败");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch/parse JWKS from {}: {}", jwksUrl, e.getMessage());
            throw new BadRequestException("登录验证失败");
        }
    }

    private PublicKey buildRsaPublicKey(String modulusBase64, String exponentBase64) throws Exception {
        byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusBase64);
        byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentBase64);
        BigInteger modulus = new BigInteger(1, modulusBytes);
        BigInteger exponent = new BigInteger(1, exponentBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    private String fetchJwks(String jwksUrl) throws Exception {
        java.net.URL url = new java.net.URL(jwksUrl);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        try (Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } finally {
            conn.disconnect();
        }
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd grid-app && mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/security/IdTokenVerifier.java
git commit -m "feat: add IdTokenVerifier for Google/Twitter JWKS-based ID token verification"
```

---

### Task 4: 更新 AppErrorCode

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/enums/AppErrorCode.java`

**Interfaces:**
- Produces: 以下新错误码 `SOCIAL_AUTH_FAILED(1102)`, `SOCIAL_BIND_TOKEN_EXPIRED(1103)`, `SOCIAL_EMAIL_CONFLICT(1104)`, `SOCIAL_ACCOUNT_DISABLED(1105)`, `SOCIAL_PROVIDER_UNSUPPORTED(1106)`

- [ ] **Step 1: 在 `INVALID_PASSWORD` 行后添加新错误码**

```java
    // 第三方登录相关 1102-1109
    SOCIAL_AUTH_FAILED(1102, "第三方登录验证失败"),
    SOCIAL_BIND_TOKEN_EXPIRED(1103, "操作超时，请重新登录"),
    SOCIAL_EMAIL_CONFLICT(1104, "该邮箱已绑定其他登录方式"),
    SOCIAL_ACCOUNT_DISABLED(1105, "账号已被禁用"),
    SOCIAL_PROVIDER_UNSUPPORTED(1106, "不支持的第三方登录方式"),
```

- [ ] **Step 2: 验证编译**

```bash
cd grid-app && mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/enums/AppErrorCode.java
git commit -m "feat: add social login error codes (1102-1106)"
```

---

### Task 5: 创建 BindEmailRequiredException + 更新 AppAuthService 接口

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/exception/BindEmailRequiredException.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/AppAuthService.java`

**Interfaces:**
- Consumes: —
- Produces: `BindEmailRequiredException` (携带 bindToken), `AppAuthService.socialLogin()` → TokenDTO, `AppAuthService.sendBindCode()` → void, `AppAuthService.socialBindEmail()` → TokenDTO

- [ ] **Step 1: 编写 BindEmailRequiredException**

```java
package com.naon.grid.modules.app.exception;

import lombok.Getter;

@Getter
public class BindEmailRequiredException extends RuntimeException {

    private final String bindToken;

    public BindEmailRequiredException(String bindToken) {
        super("需要绑定邮箱");
        this.bindToken = bindToken;
    }
}
```

- [ ] **Step 2: 更新 AppAuthService 接口**

在文件末尾 `sendCode` 方法声明后、闭 `}` 前添加三个方法，并添加 import：

```java
import com.naon.grid.modules.app.service.dto.SocialLoginDTO;
import com.naon.grid.modules.app.service.dto.SocialBindEmailDTO;
```

方法声明：

```java
    TokenDTO socialLogin(SocialLoginDTO socialLoginDTO, javax.servlet.http.HttpServletRequest request);
    void sendBindCode(SendCodeDTO dto);
    TokenDTO socialBindEmail(SocialBindEmailDTO socialBindEmailDTO, javax.servlet.http.HttpServletRequest request);
```

- [ ] **Step 3: 验证编译（预期 Impl 编译失败）**

```bash
cd grid-app && mvn compile -DskipTests
```
Expected: BUILD FAILURE — `AppAuthServiceImpl` 未实现新接口方法

- [ ] **Step 4: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/exception/BindEmailRequiredException.java \
        grid-app/src/main/java/com/naon/grid/modules/app/service/AppAuthService.java
git commit -m "feat: add BindEmailRequiredException and social login methods to AppAuthService interface"
```

---

### Task 6: 实现 AppAuthServiceImpl 社交登录核心逻辑

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java`

**Interfaces:**
- Consumes: `IdTokenVerifier` (Task 3), `GridUserAuthRepository`, `SocialLoginDTO`, `SocialBindEmailDTO`, `BindEmailRequiredException` (Task 5)
- Produces: `socialLogin()`, `sendBindCode()`, `socialBindEmail()` 三个方法 + 辅助私有方法

- [ ] **Step 1: 添加新的依赖注入和 import**

在 `AppAuthServiceImpl` 类中：

添加到现有的 `private final` 字段列表末尾（在 `EmailService` 之后）：

```java
    private final GridUserAuthRepository userAuthRepository;
    private final IdTokenVerifier idTokenVerifier;
```

在文件顶部 import 区，紧接现有 import 之后添加：

```java
import com.alibaba.fastjson2.JSON;
import com.naon.grid.modules.app.domain.GridUserAuth;
import com.naon.grid.modules.app.exception.BindEmailRequiredException;
import com.naon.grid.modules.app.repository.GridUserAuthRepository;
import com.naon.grid.modules.app.security.IdTokenVerifier;
import com.naon.grid.modules.app.security.SocialUserInfo;
import com.naon.grid.modules.app.service.dto.SocialLoginDTO;
import com.naon.grid.modules.app.service.dto.SocialBindEmailDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
```

- [ ] **Step 2: 实现 socialLogin 方法**

在 `sendCode()` 方法之后、`buildCodeEmail()` 方法之前添加：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO socialLogin(SocialLoginDTO socialLoginDTO, HttpServletRequest request) {
        String provider = socialLoginDTO.getProvider().toLowerCase();
        SocialUserInfo socialUser = idTokenVerifier.verify(provider, socialLoginDTO.getIdToken());

        // 1. 查 GridUserAuth — providerId 优先匹配
        Optional<GridUserAuth> existingAuth =
                userAuthRepository.findByProviderAndProviderId(provider, socialUser.getProviderId());
        if (existingAuth.isPresent()) {
            GridUser user = userRepository.findById(existingAuth.get().getUserId())
                    .orElseThrow(() -> new BadRequestException("用户不存在"));
            if (user.getStatus() == 0) {
                throw new BadRequestException("账号已被禁用");
            }
            updateSocialAuth(existingAuth.get(), socialLoginDTO.getIdToken(), socialUser);
            updateLoginMetadata(user, request);
            fillUserProfile(user, socialUser);
            return generateToken(user, socialLoginDTO.getDeviceId(), socialLoginDTO.getDeviceName());
        }

        // 2. 无邮箱 → 需要绑定
        if (socialUser.getEmail() == null || socialUser.getEmail().isEmpty()) {
            String bindTokenId = UUID.randomUUID().toString();
            Map<String, Object> bindClaims = new HashMap<>();
            bindClaims.put("provider", provider);
            bindClaims.put("providerId", socialUser.getProviderId());
            bindClaims.put("name", socialUser.getName());
            bindClaims.put("picture", socialUser.getPicture());
            String bindJson = JSON.toJSONString(bindClaims);
            redisUtils.set("social:bind:" + bindTokenId, bindJson, 300, java.util.concurrent.TimeUnit.SECONDS);
            throw new BindEmailRequiredException(bindTokenId);
        }

        // 3. 有邮箱 → 按邮箱查找或创建用户
        return createOrLinkSocialUser(provider, socialUser, socialLoginDTO.getIdToken(),
                socialLoginDTO.getDeviceId(), socialLoginDTO.getDeviceName(), request);
    }

    private TokenDTO createOrLinkSocialUser(String provider, SocialUserInfo socialUser,
                                             String idToken, String deviceId, String deviceName,
                                             HttpServletRequest request) {
        String normalizedEmail = normalizeEmail(socialUser.getEmail());
        Optional<GridUser> existingUser = userRepository.findByEmail(normalizedEmail);

        GridUser user;
        boolean isNewUser = false;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getStatus() == 0) {
                throw new BadRequestException("账号已被禁用");
            }
        } else {
            user = createSocialUser(socialUser, request);
            isNewUser = true;
        }

        // 写 GridUserAuth 关联
        GridUserAuth auth = new GridUserAuth();
        auth.setUserId(user.getId());
        auth.setProvider(provider);
        auth.setProviderId(socialUser.getProviderId());
        auth.setProviderName(socialUser.getName());
        auth.setProviderAvatar(socialUser.getPicture());
        auth.setAccessToken(idToken);
        auth.setExpireTime(socialUser.getExpireTime());
        userAuthRepository.save(auth);

        if (!isNewUser) {
            fillUserProfile(user, socialUser);
        }
        updateLoginMetadata(user, request);
        return generateToken(user, deviceId, deviceName);
    }

    private GridUser createSocialUser(SocialUserInfo socialUser, HttpServletRequest request) {
        String normalizedEmail = normalizeEmail(socialUser.getEmail());
        String ip = StringUtils.getIp(request);
        String region = regionResolver.resolve(ip);

        GridUser user = new GridUser();
        user.setEmail(normalizedEmail);
        user.setEmailVerified(socialUser.isEmailVerified() ? 1 : 0);
        user.setPassword(null);
        user.setNickname(socialUser.getName() != null ? socialUser.getName() : normalizedEmail.split("@")[0]);
        user.setAvatar(socialUser.getPicture());
        user.setGender(0);
        user.setStatus(1);
        user.setUserType("NORMAL");
        user.setRegion(region);
        user.setRegisterIp(ip);
        user.setRegisterAuditStatus("APPROVED");
        user.setReferralCode(generateReferralCode(userRepository));
        userRepository.save(user);

        GridUserRole normalRole = new GridUserRole();
        normalRole.setUserId(user.getId());
        normalRole.setRoleCode("NORMAL");
        normalRole.setRoleName("普通用户");
        userRoleRepository.save(normalRole);

        try {
            entitlementEngine.grant(user.getId(), "TRIAL", null, "PLUS", 7, region);
        } catch (Exception e) {
            log.error("Failed to grant trial for social userId={}", user.getId(), e);
            try {
                subscriptionService.grantTrial(user.getId());
            } catch (Exception ex) {
                log.error("Fallback grantTrial also failed for userId={}", user.getId(), ex);
            }
        }

        return user;
    }

    private void fillUserProfile(GridUser user, SocialUserInfo socialUser) {
        boolean changed = false;
        // Fill name/avatar if user hasn't set them yet
        if ((user.getNickname() == null || user.getNickname().isEmpty())
                && socialUser.getName() != null && !socialUser.getName().isEmpty()) {
            user.setNickname(socialUser.getName());
            changed = true;
        }
        if ((user.getAvatar() == null || user.getAvatar().isEmpty())
                && socialUser.getPicture() != null && !socialUser.getPicture().isEmpty()) {
            user.setAvatar(socialUser.getPicture());
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private void updateSocialAuth(GridUserAuth auth, String idToken, SocialUserInfo socialUser) {
        auth.setAccessToken(idToken);
        auth.setExpireTime(socialUser.getExpireTime());
        if (socialUser.getName() != null) {
            auth.setProviderName(socialUser.getName());
        }
        if (socialUser.getPicture() != null) {
            auth.setProviderAvatar(socialUser.getPicture());
        }
        userAuthRepository.save(auth);
    }

    private void updateLoginMetadata(GridUser user, HttpServletRequest request) {
        user.setLastLoginTime(new Date());
        user.setLastLoginIp(StringUtils.getIp(request));
        String currentRegion = regionResolver.resolve(StringUtils.getIp(request));
        user.setRegion(currentRegion);
        userRepository.save(user);
    }

    @Override
    public void sendBindCode(SendCodeDTO dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());

        // 不检查邮箱是否已注册（允许绑定已有账号）

        String cooldownKey = "email:code:cooldown:" + normalizedEmail;
        if (redisUtils.hasKey(cooldownKey)) {
            throw new BadRequestException("验证码已发送，请60秒后重试");
        }

        String code = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        redisUtils.set(cooldownKey, "1", 60, java.util.concurrent.TimeUnit.SECONDS);
        emailService.sendHtmlEmail(normalizedEmail, "有路中文 - 邮箱验证码", buildCodeEmail(code));

        String codeKey = "email:code:" + normalizedEmail;
        redisUtils.set(codeKey, code, 5, java.util.concurrent.TimeUnit.MINUTES);
        log.info("Bind verification code sent to: {}", normalizedEmail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO socialBindEmail(SocialBindEmailDTO socialBindEmailDTO, HttpServletRequest request) {
        String bindTokenId = socialBindEmailDTO.getBindToken();
        String redisKey = "social:bind:" + bindTokenId;
        String bindJson = redisUtils.getAndDel(redisKey);
        if (bindJson == null) {
            throw new BadRequestException("操作超时，请重新登录");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> bindClaims = JSON.parseObject(bindJson, Map.class);
        String provider = (String) bindClaims.get("provider");
        String providerId = (String) bindClaims.get("providerId");

        // Verify email code
        String normalizedEmail = normalizeEmail(socialBindEmailDTO.getEmail());
        String codeKey = "email:code:" + normalizedEmail;
        String savedCode = redisUtils.getAndDel(codeKey);
        if (savedCode == null) {
            throw new BadRequestException("验证码不存在或已过期");
        }
        if (!savedCode.equals(socialBindEmailDTO.getCode())) {
            throw new BadRequestException("验证码错误");
        }

        // Find or create user by email
        Optional<GridUser> existingUser = userRepository.findByEmail(normalizedEmail);
        GridUser user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getStatus() == 0) {
                throw new BadRequestException("账号已被禁用");
            }
        } else {
            String ip = StringUtils.getIp(request);
            String region = regionResolver.resolve(ip);
            user = new GridUser();
            user.setEmail(normalizedEmail);
            user.setEmailVerified(1);
            user.setPassword(null);
            user.setNickname(bindClaims.get("name") != null
                    ? (String) bindClaims.get("name") : normalizedEmail.split("@")[0]);
            user.setAvatar((String) bindClaims.get("picture"));
            user.setGender(0);
            user.setStatus(1);
            user.setUserType("NORMAL");
            user.setRegion(region);
            user.setRegisterIp(ip);
            user.setRegisterAuditStatus("APPROVED");
            user.setReferralCode(generateReferralCode(userRepository));
            userRepository.save(user);

            GridUserRole normalRole = new GridUserRole();
            normalRole.setUserId(user.getId());
            normalRole.setRoleCode("NORMAL");
            normalRole.setRoleName("普通用户");
            userRoleRepository.save(normalRole);

            try {
                entitlementEngine.grant(user.getId(), "TRIAL", null, "PLUS", 7, region);
            } catch (Exception e) {
                log.error("Failed to grant trial for socialBind userId={}", user.getId(), e);
                try {
                    subscriptionService.grantTrial(user.getId());
                } catch (Exception ex) {
                    log.error("Fallback grantTrial also failed for userId={}", user.getId(), ex);
                }
            }
        }

        // Write GridUserAuth
        Optional<GridUserAuth> existingAuth =
                userAuthRepository.findByProviderAndProviderId(provider, providerId);
        if (!existingAuth.isPresent()) {
            GridUserAuth auth = new GridUserAuth();
            auth.setUserId(user.getId());
            auth.setProvider(provider);
            auth.setProviderId(providerId);
            auth.setProviderName((String) bindClaims.get("name"));
            auth.setProviderAvatar((String) bindClaims.get("picture"));
            userAuthRepository.save(auth);
        }

        updateLoginMetadata(user, request);
        return generateToken(user, socialBindEmailDTO.getDeviceId(), socialBindEmailDTO.getDeviceName());
    }
```

- [ ] **Step 3: 验证编译**

```bash
cd grid-app && mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java
git commit -m "feat: implement socialLogin, sendBindCode, socialBindEmail in AppAuthServiceImpl"
```

---

### Task 7: 更新 AppAuthController

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppAuthController.java`

**Interfaces:**
- Consumes: `AppAuthService` (更新的接口), `BindEmailRequiredException` (Task 5)
- Produces: 3 个新端点 `POST /api/app/auth/social-login`, `POST /api/app/auth/send-bind-code`, `POST /api/app/auth/social-bind-email`

- [ ] **Step 1: 在 AppAuthController 中添加 3 个端点方法**

在 `sendCode()` 方法之后、`getPublicKey()` 方法之前（即类末尾的 `}` 之前），以及类 import 区添加新 import：

新 import（添加在现有 import 末尾）：

```java
import com.naon.grid.modules.app.exception.BindEmailRequiredException;
import com.naon.grid.modules.app.service.dto.SocialLoginDTO;
import com.naon.grid.modules.app.service.dto.SocialBindEmailDTO;
import com.naon.grid.modules.app.service.dto.SendCodeDTO;
```

如果 `SendCodeDTO` 尚未 import，补充之。检查现有 import，`SendCodeDTO` 应在 sendCode 方法中已有 import。

三个新端点方法：

```java
    @Log("APP第三方登录")
    @ApiOperation("第三方登录")
    @AnonymousPostMapping("/social-login")
    public ResponseEntity<?> socialLogin(@Validated @RequestBody SocialLoginDTO socialLoginDTO,
                                          HttpServletRequest request) {
        try {
            TokenDTO tokenDTO = appAuthService.socialLogin(socialLoginDTO, request);
            return ResponseEntity.ok(tokenDTO);
        } catch (BindEmailRequiredException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("requireBindEmail", true);
            result.put("bindToken", e.getBindToken());
            return ResponseEntity.ok(result);
        }
    }

    @Log("发送邮箱绑定验证码")
    @ApiOperation("第三方登录-发送邮箱绑定验证码")
    @AnonymousPostMapping("/send-bind-code")
    public ResponseEntity<Map<String, String>> sendBindCode(@Validated @RequestBody SendCodeDTO sendCodeDTO) {
        appAuthService.sendBindCode(sendCodeDTO);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送，5分钟内有效");
        return ResponseEntity.ok(result);
    }

    @Log("APP第三方登录邮箱绑定")
    @ApiOperation("第三方登录-绑定邮箱")
    @AnonymousPostMapping("/social-bind-email")
    public ResponseEntity<TokenDTO> socialBindEmail(@Validated @RequestBody SocialBindEmailDTO socialBindEmailDTO,
                                                     HttpServletRequest request) {
        TokenDTO tokenDTO = appAuthService.socialBindEmail(socialBindEmailDTO, request);
        return ResponseEntity.ok(tokenDTO);
    }
```

- [ ] **Step 2: 验证编译**

```bash
cd grid-app && mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppAuthController.java
git commit -m "feat: add social-login, send-bind-code, social-bind-email endpoints to AppAuthController"
```

---

### Task 8: 更新配置文件

**Files:**
- Modify: `grid-bootstrap/src/main/resources/application.yml`
- Modify: `.env.example`

**Interfaces:**
- Produces: `social.providers.google.client-id`, `social.providers.google.issuer`, `social.providers.google.jwks-url` 配置项（及 Twitter 对应项）

- [ ] **Step 1: 在 application.yml 中添加 social 配置段**

在 `grid-bootstrap/src/main/resources/application.yml` 末尾添加：

```yaml
# ============================================================
# 第三方登录配置
# ============================================================
social:
  providers:
    google:
      client-id: ${GOOGLE_CLIENT_ID:}
      issuer: ${GOOGLE_ISSUER:https://accounts.google.com}
      jwks-url: ${GOOGLE_JWKS_URL:https://www.googleapis.com/oauth2/v3/certs}
    twitter:
      client-id: ${TWITTER_CLIENT_ID:}
      issuer: ${TWITTER_ISSUER:https://api.twitter.com}
      jwks-url: ${TWITTER_JWKS_URL:https://api.twitter.com/2/oauth2/jwks}
```

- [ ] **Step 2: 在 .env.example 中添加配置项**

在 `.env.example` 末尾添加：

```
# ============================================================
# 第三方登录 OAuth 配置
# ============================================================
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
TWITTER_CLIENT_ID=your-twitter-client-id
```

- [ ] **Step 3: 验证配置加载**

```bash
cd grid-bootstrap && mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add grid-bootstrap/src/main/resources/application.yml .env.example
git commit -m "feat: add Google/Twitter OAuth client configuration to application.yml and .env.example"
```

---

### Task 9: 全量构建验证

**Files:** 无（验证所有已修改文件）

- [ ] **Step 1: 全量编译**

```bash
mvn clean compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 全量打包**

```bash
mvn clean package -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 检查所有新增/修改文件在 git 中的状态**

```bash
git status
```
Expected: working tree clean（所有已提交）

- [ ] **Step 4: 查看最终 diff 摘要**

```bash
git log --oneline -9
```
Expected: 本次功能的 8 个 commits 均可见

---

## 完成检查清单

- [ ] `POST /api/app/auth/social-login` — Google 有邮箱用户 → 创建账号 + 返回 JWT
- [ ] `POST /api/app/auth/social-login` — Google 有邮箱且邮箱已注册 → 自动关联 + 返回 JWT
- [ ] `POST /api/app/auth/social-login` — Twitter 无邮箱 → 返回 `{requireBindEmail: true, bindToken}`
- [ ] `POST /api/app/auth/social-login` — 已绑定过的用户再次登录 → 直接返回 JWT
- [ ] `POST /api/app/auth/send-bind-code` — 发送验证码（不检查邮箱是否已注册）
- [ ] `POST /api/app/auth/social-bind-email` — 绑定新邮箱 + 创建账号 + 返回 JWT
- [ ] `POST /api/app/auth/social-bind-email` — 绑定已注册邮箱 + 自动关联 + 返回 JWT
- [ ] ID Token 签名无效 → 返回错误
- [ ] ID Token 已过期 → 返回 "登录凭证已过期"
- [ ] bindToken 过期 → 返回 "操作超时"
- [ ] 被封禁用户通过第三方登录 → 返回 "账号已被禁用"
- [ ] 现有密码登录/注册流程不受影响
