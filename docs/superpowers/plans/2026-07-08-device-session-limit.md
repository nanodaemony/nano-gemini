# Device Session Limit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Limit concurrent device sessions per user account using Redis Hash, with configurable max devices and automatic eviction of oldest session.

**Architecture:** New `SessionManager` class manages active sessions in Redis Hash (`app:sessions:{userId}`). `AppTokenFilter` validates every request against Redis with DB fallback. `AppAuthServiceImpl` enforces device limit on login (auto-kick oldest). DeviceManager remains unchanged, managing DB token records.

**Tech Stack:** Spring Boot 2.7.18, Redis (Spring Data Redis + RedisUtils), JPA, Lombok, Fastjson2

## Global Constraints

- `app.auth.max-devices` default: 3 (configurable in YAML)
- Redis key pattern: `app:sessions:{userId}` (Hash, field=deviceId)
- Session TTL matches JWT `token-expire-seconds` (default 604800)
- Redis failure must never block authentication (fallback to DB `grid_user_token`)
- `AppTokenFilter` skip authentication → security framework returns 401 (no explicit error response in filter)
- All path references in this plan relative to repo root

---

### Task 1: Create SessionManager

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/security/SessionManager.java`

**Interfaces:**
- Consumes: `RedisUtils` (existing component, methods: `hHasKey`, `hmget`, `hset`, `hdel`, `expire`)
- Produces: `SessionManager` class with methods:
  - `boolean isActive(Long userId, String deviceId)` — throws on Redis failure
  - `int getActiveCount(Long userId)` — throws on Redis failure
  - `void addSession(Long userId, String deviceId, String deviceName)` — best-effort
  - `void removeSession(Long userId, String deviceId)` — best-effort
  - `String findOldestDeviceId(Long userId)` — returns null if empty, throws on Redis failure

- [ ] **Step 1: Create SessionManager.java**

```java
package com.naon.grid.modules.app.security;

import com.alibaba.fastjson2.JSON;
import com.naon.grid.utils.RedisUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis 会话管理器 — 追踪活跃设备，限制同时登录设备数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {

    private static final String SESSION_KEY_PREFIX = "app:sessions:";

    private final RedisUtils redisUtils;

    @Value("${app.auth.token-expire-seconds:604800}")
    private long tokenExpireSeconds;

    @Value("${app.auth.max-devices:3}")
    private int maxDevices;

    // ── public API ──

    /** O(1) 检查设备是否在活跃会话中。Redis 不可用时抛出异常，由调用方决定降级策略。 */
    public boolean isActive(Long userId, String deviceId) {
        return redisUtils.hHasKey(sessionKey(userId), deviceId);
    }

    /** 获取当前活跃设备数。Redis 不可用时抛出异常。 */
    public int getActiveCount(Long userId) {
        Map<Object, Object> sessions = redisUtils.hmget(sessionKey(userId));
        return sessions == null ? 0 : sessions.size();
    }

    /** 添加或更新活跃会话（hset 覆盖写入），并刷新 key 的 TTL。 */
    public void addSession(Long userId, String deviceId, String deviceName) {
        try {
            String key = sessionKey(userId);
            SessionData data = new SessionData(
                    System.currentTimeMillis() / 1000,
                    deviceName != null ? deviceName : ""
            );
            redisUtils.hset(key, deviceId, JSON.toJSONString(data));
            redisUtils.expire(key, tokenExpireSeconds);
        } catch (Exception e) {
            log.warn("Failed to add session to Redis userId={} deviceId={}", userId, deviceId, e);
        }
    }

    /** 从活跃会话中移除指定设备。 */
    public void removeSession(Long userId, String deviceId) {
        try {
            redisUtils.hdel(sessionKey(userId), deviceId);
        } catch (Exception e) {
            log.warn("Failed to remove session from Redis userId={} deviceId={}", userId, deviceId, e);
        }
    }

    /** 遍历所有会话，找出 loginTime 最早的 deviceId。返回 null 表示无活跃会话。 */
    public String findOldestDeviceId(Long userId) {
        Map<Object, Object> sessions = redisUtils.hmget(sessionKey(userId));
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }
        String oldest = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<Object, Object> entry : sessions.entrySet()) {
            String did = (String) entry.getKey();
            String json = entry.getValue() != null ? entry.getValue().toString() : null;
            if (json == null) continue;
            try {
                SessionData data = JSON.parseObject(json, SessionData.class);
                if (data != null && data.loginTime < oldestTime) {
                    oldestTime = data.loginTime;
                    oldest = did;
                }
            } catch (Exception e) {
                log.warn("Corrupt session data for deviceId={} userId={}", did, userId);
            }
        }
        return oldest;
    }

    // ── internal ──

    private String sessionKey(Long userId) {
        return SESSION_KEY_PREFIX + userId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SessionData {
        private long loginTime;
        private String deviceName;
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd grid-app && mvn compile -pl . -am -DskipTests -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/security/SessionManager.java
git commit -m "feat: add SessionManager for Redis-based device session tracking

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: Modify AppTokenFilter — Add session validation

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/security/AppTokenFilter.java`

**Interfaces:**
- Consumes: `AppTokenProvider` (existing), `SecurityProperties` (existing), `SessionManager` (Task 1), `GridUserTokenRepository` (existing)
- Produces: Enhanced filter that validates active sessions per-request

**Current constructor** (2 args, manual creation in AppSecurityConfig):
```java
public AppTokenFilter(AppTokenProvider appTokenProvider, SecurityProperties securityProperties)
```

**New constructor** (4 args):
```java
public AppTokenFilter(AppTokenProvider appTokenProvider, SecurityProperties securityProperties,
                      SessionManager sessionManager, GridUserTokenRepository userTokenRepository)
```

Note: `@RequiredArgsConstructor` on the class will auto-generate a 4-arg constructor once the new `final` fields are added. The manual construction in `AppSecurityConfig` (Task 3) must also be updated.

- [ ] **Step 1: Add new fields and update doFilter**

Read `AppTokenFilter.java`, then replace the entire file:

```java
package com.naon.grid.modules.app.security;

import com.naon.grid.config.properties.SecurityProperties;
import com.naon.grid.modules.app.repository.GridUserTokenRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppTokenFilter extends GenericFilterBean {

    private final AppTokenProvider appTokenProvider;
    private final SecurityProperties securityProperties;
    private final SessionManager sessionManager;
    private final GridUserTokenRepository userTokenRepository;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        // 仅处理 App 端请求
        if (!httpServletRequest.getRequestURI().startsWith("/api/app/")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        String token = resolveToken(httpServletRequest);

        if (StringUtils.hasText(token) && appTokenProvider.validateToken(token)) {
            Long userId = appTokenProvider.getUserIdFromToken(token);
            String username = appTokenProvider.getClaims(token).getSubject();
            String deviceId = appTokenProvider.getDeviceIdFromToken(token);
            List<String> roles = appTokenProvider.getRolesFromToken(token);
            Claims claims = appTokenProvider.getClaims(token);
            String userType = claims.get(AppTokenProvider.USER_TYPE_KEY, String.class);
            Integer orgId = claims.get(AppTokenProvider.ORG_ID_KEY, Integer.class);
            String orgRole = claims.get(AppTokenProvider.ORG_ROLE_KEY, String.class);
            String region = claims.get(AppTokenProvider.REGION_KEY, String.class);

            // 【新增】会话校验 + Redis 降级
            if (!isSessionActive(userId, deviceId)) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }

            AppAuthenticationToken authentication = new AppAuthenticationToken(
                    userId, username, deviceId, roles, userType, orgId, orgRole, region);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    /** 校验设备会话是否活跃。Redis 不可用时降级查 DB。 */
    private boolean isSessionActive(Long userId, String deviceId) {
        try {
            return sessionManager.isActive(userId, deviceId);
        } catch (Exception e) {
            log.warn("Redis session check failed for userId={}, falling back to DB", userId, e);
            return userTokenRepository.findByUserIdAndDeviceId(userId, deviceId).isPresent();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(securityProperties.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(securityProperties.getTokenStartWith())) {
            return bearerToken.substring(securityProperties.getTokenStartWith().length());
        }
        return null;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/security/AppTokenFilter.java
git commit -m "feat: add Redis session validation to AppTokenFilter with DB fallback

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: Modify AppSecurityConfig — Pass new dependencies

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/config/AppSecurityConfig.java`

**Interfaces:**
- Consumes: `SessionManager` (Task 1), `GridUserTokenRepository` (existing)
- Produces: Updated `AppTokenFilter` construction with 4 args

- [ ] **Step 1: Update constructor + filter creation**

Read `AppSecurityConfig.java`, then replace the file:

```java
package com.naon.grid.modules.app.config;

import com.naon.grid.config.properties.SecurityProperties;
import com.naon.grid.modules.app.repository.GridUserTokenRepository;
import com.naon.grid.modules.app.security.AppTokenFilter;
import com.naon.grid.modules.app.security.AppTokenProvider;
import com.naon.grid.modules.app.security.SessionManager;
import com.naon.grid.utils.AnonTagUtils;
import com.naon.grid.utils.enums.RequestMethodEnum;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;
import java.util.Set;

@Configuration
@Order(1)
@EnableWebSecurity
public class AppSecurityConfig {

    private final ApplicationContext applicationContext;
    private final AppTokenProvider appTokenProvider;
    private final SecurityProperties securityProperties;
    private final SessionManager sessionManager;
    private final GridUserTokenRepository userTokenRepository;

    public AppSecurityConfig(ApplicationContext applicationContext,
                             AppTokenProvider appTokenProvider,
                             SecurityProperties securityProperties,
                             SessionManager sessionManager,
                             GridUserTokenRepository userTokenRepository) {
        this.applicationContext = applicationContext;
        this.appTokenProvider = appTokenProvider;
        this.securityProperties = securityProperties;
        this.sessionManager = sessionManager;
        this.userTokenRepository = userTokenRepository;
    }

    @Bean
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        Map<String, Set<String>> anonymousUrls = AnonTagUtils.getAnonymousUrl(applicationContext);
        AppTokenFilter appTokenFilter = new AppTokenFilter(
                appTokenProvider, securityProperties, sessionManager, userTokenRepository);

        return http
                .requestMatchers(matchers -> matchers.antMatchers("/api/app/**"))
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeHttpRequests(auth -> {
                    auth.antMatchers(HttpMethod.GET, anonymousUrls.get(RequestMethodEnum.GET.getType()).toArray(new String[0])).permitAll();
                    auth.antMatchers(HttpMethod.POST, anonymousUrls.get(RequestMethodEnum.POST.getType()).toArray(new String[0])).permitAll();
                    auth.antMatchers(HttpMethod.PUT, anonymousUrls.get(RequestMethodEnum.PUT.getType()).toArray(new String[0])).permitAll();
                    auth.antMatchers(HttpMethod.PATCH, anonymousUrls.get(RequestMethodEnum.PATCH.getType()).toArray(new String[0])).permitAll();
                    auth.antMatchers(HttpMethod.DELETE, anonymousUrls.get(RequestMethodEnum.DELETE.getType()).toArray(new String[0])).permitAll();
                    auth.antMatchers(anonymousUrls.get(RequestMethodEnum.ALL.getType()).toArray(new String[0])).permitAll();
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(appTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/config/AppSecurityConfig.java
git commit -m "feat: wire SessionManager + GridUserTokenRepository into AppTokenFilter

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: Modify AppAuthServiceImpl — Enforce device limit

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java`

**Interfaces:**
- Consumes: `SessionManager` (Task 1)
- Produces: Device-limited login/register/refresh/logout

**Changes summary:**
1. Add `SessionManager` field + `maxDevices` config + import
2. In `generateToken()`: enforce device limit before generating tokens
3. In `refreshToken()`: validate session before allowing refresh
4. In `logout()`: remove from Redis session

- [ ] **Step 1: Add dependencies — field, config, import**

Read the current `AppAuthServiceImpl.java`.

(a) Add import among existing imports:

```java
import com.naon.grid.modules.app.security.SessionManager;
```

(b) Add `maxDevices` field alongside the existing `@Value` fields (near line 83):

```java
    @Value("${app.auth.max-devices:3}")
    private int maxDevices;
```

(c) Add `SessionManager` field to the existing `@RequiredArgsConstructor` fields (near line 67):

```java
    private final SessionManager sessionManager;
```

- [ ] **Step 2: Modify generateToken() — enforce device limit**

Replace the existing `generateToken` method (lines ~592-614) with:

```java
    private TokenDTO generateToken(GridUser user, String deviceId, String deviceName) {
        // ── 设备会话限制 ──
        try {
            if (!sessionManager.isActive(user.getId(), deviceId)) {
                // 新设备 ─ 检查是否达到上限
                int activeCount = sessionManager.getActiveCount(user.getId());
                if (activeCount >= maxDevices) {
                    String oldestDeviceId = sessionManager.findOldestDeviceId(user.getId());
                    if (oldestDeviceId != null) {
                        log.info("Device limit reached for userId={}, evicting oldest device={}",
                                user.getId(), oldestDeviceId);
                        sessionManager.removeSession(user.getId(), oldestDeviceId);
                        userTokenRepository.deleteByUserIdAndDeviceId(user.getId(), oldestDeviceId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Session enforcement failed for userId={}, skipping device limit check", user.getId(), e);
            // Redis 不可用时跳过限制，不阻断登录
        }
        sessionManager.addSession(user.getId(), deviceId, deviceName);

        // ── 原有逻辑 ──
        List<String> roles = new ArrayList<>();

        Integer orgId = user.getOrgId();
        String accessToken = appTokenProvider.createToken(
                user.getId(), user.getEmail(), deviceId, roles,
                user.getUserType(),
                orgId != null ? orgId.intValue() : null,
                user.getOrgRole(),
                user.getRegion());
        String refreshToken = IdUtil.simpleUUID();

        Date expireTime = new Date(System.currentTimeMillis() + refreshTokenExpireSeconds * 1000);
        deviceManager.registerDevice(user.getId(), deviceId, deviceName, refreshToken, accessToken, expireTime);

        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setAccessToken(accessToken);
        tokenDTO.setRefreshToken(refreshToken);
        tokenDTO.setExpiresIn(tokenExpireSeconds);
        tokenDTO.setUser(convertToDTO(user, roles));

        return tokenDTO;
    }
```

- [ ] **Step 3: Modify refreshToken() — validate session before refresh**

Replace the existing `refreshToken` method (lines ~208-228) with:

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO refreshToken(String refreshToken) {
        Optional<GridUserToken> tokenOpt = userTokenRepository.findByRefreshToken(refreshToken);
        if (!tokenOpt.isPresent()) {
            throw new BadRequestException("刷新令牌无效");
        }

        GridUserToken userToken = tokenOpt.get();
        if (userToken.getExpireTime().before(new Date())) {
            userTokenRepository.delete(userToken);
            throw new BadRequestException("刷新令牌已过期");
        }

        // 【新增】校验设备会话是否仍活跃
        try {
            if (!sessionManager.isActive(userToken.getUserId(), userToken.getDeviceId())) {
                throw new BadRequestException("设备已下线，请重新登录");
            }
        } catch (Exception e) {
            log.warn("Redis session check failed during refresh for userId={}, proceeding",
                    userToken.getUserId(), e);
            // Redis 不可用时放行
        }

        GridUser user = userRepository.findById(userToken.getUserId())
                .orElseThrow(() -> new BadRequestException("用户不存在"));

        userTokenRepository.delete(userToken);

        return generateToken(user, userToken.getDeviceId(), userToken.getDeviceName());
    }
```

- [ ] **Step 4: Modify logout() — remove Redis session**

Replace the existing `logout` method (lines ~586-591) with:

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(Long userId, String deviceId) {
        sessionManager.removeSession(userId, deviceId);
        userTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId);
    }
```

- [ ] **Step 5: Verify compilation**

```bash
cd grid-app && mvn compile -pl . -am -DskipTests -q
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java
git commit -m "feat: enforce device session limit in login/register/refresh/logout

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: Add configuration

**Files:**
- Modify: `grid-bootstrap/src/main/resources/config/application.yml`

- [ ] **Step 1: Add max-devices config**

Add the following block under the existing root-level config, after the `rsa:` block (line ~85):

```yaml
# App 端配置
app:
  auth:
    max-devices: 3  # 最大同时登录设备数，设为 1 即为单设备模式
```

- [ ] **Step 2: Commit**

```bash
git add grid-bootstrap/src/main/resources/config/application.yml
git commit -m "feat: add app.auth.max-devices config (default 3)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Verification Checklist

After all tasks are complete, verify end-to-end:

1. **Compile**: `mvn compile -pl grid-app -am -DskipTests -q` → BUILD SUCCESS
2. **Start application** with Redis + MySQL running
3. **Login device A**: `POST /api/app/auth/login` → 200, token returned
4. **Login device B**: different deviceId → 200
5. **Login device C**: different deviceId → 200
6. **Login device D**: different deviceId → 200, device A should be evicted (check Redis: `HGETALL app:sessions:{userId}` should show B, C, D but not A)
7. **Device A API call**: `GET /api/app/char/book` with device A's token → 401
8. **Logout device B**: `POST /api/app/auth/logout?deviceId=B` → 200; Redis `HGETALL` should show only C, D
9. **Redis down simulation**: Stop Redis → API calls with valid tokens should still succeed (DB fallback)
