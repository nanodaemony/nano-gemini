# Subscription (会员订阅) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 APP 端用户实现会员订阅功能，通过 `@RequireSubscription` 注解控制接口访问权限，支持 VIP / SVIP 两级会员层级和注册自动试用。

**Architecture:** 纯注解方案，零数据库变更。`@RequireSubscription` 注解标注在 Controller 方法上，`SubscriptionAspect` (AOP @Around) 拦截校验。会员级别使用 `UserLevel` 枚举（VIP=1, SVIP=2，SVIP.includes(VIP)=true）。JWT 中的 roles 做快速预检，数据库 `expire_time` 做精确校验。订阅购买与支付解耦，支付系统通过 `activate` 接口通知会员系统。

**Tech Stack:** Spring Boot 2.7.18, Spring AOP (AspectJ), Spring Security, JPA, Java 8

---

## 文件清单

| 操作 | 路径 | 说明 |
|------|------|------|
| 新增 | `grid-common/src/main/java/com/naon/grid/enums/UserLevel.java` | 会员级别枚举 |
| 新增 | `grid-common/src/main/java/com/naon/grid/annotation/RequireSubscription.java` | 会员鉴权注解 |
| 新增 | `grid-app/src/main/java/com/naon/grid/modules/app/aspect/SubscriptionAspect.java` | AOP 鉴权切面 |
| 新增 | `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/AppSubscriptionVO.java` | 订阅状态响应 VO |
| 新增 | `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/CreateOrderDTO.java` | 下单请求 DTO |
| 新增 | `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/ActivateSubscriptionDTO.java` | 激活订阅请求 DTO |
| 新增 | `grid-app/src/main/java/com/naon/grid/modules/app/service/SubscriptionService.java` | 订阅服务接口 |
| 新增 | `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/SubscriptionServiceImpl.java` | 订阅服务实现 |
| 新增 | `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppSubscriptionController.java` | 订阅相关 API |
| 修改 | `grid-app/src/main/java/com/naon/grid/modules/app/enums/AppErrorCode.java` | 新增错误码 |
| 修改 | `grid-app/src/main/java/com/naon/grid/modules/app/repository/GridUserRoleRepository.java` | 新增数据库查询方法 |
| 修改 | `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java` | 注册时增加试用角色 |
| 修改 | `grid-system/src/main/resources/config/application.yml` | （可选）添加订阅配置项 |

---

### Task 1: UserLevel 枚举 + @RequireSubscription 注解

**Files:**
- Create: `grid-common/src/main/java/com/naon/grid/enums/UserLevel.java`
- Create: `grid-common/src/main/java/com/naon/grid/annotation/RequireSubscription.java`

- [ ] **Step 1: 创建 UserLevel 枚举**

```java
package com.naon.grid.enums;

/**
 * 会员级别。
 * VIP=1, SVIP=2
 * 级别数值越高，权限越大。SVIP.includes(VIP)=true
 */
public enum UserLevel {
    VIP(1),
    SVIP(2);

    private final int level;

    UserLevel(int level) {
        this.level = level;
    }

    /**
     * 当前级别是否包含 other 级别的权限。
     * 例：SVIP.includes(VIP) → true
     */
    public boolean includes(UserLevel other) {
        return this.level >= other.level;
    }
}
```

- [ ] **Step 2: 创建 @RequireSubscription 注解**

```java
package com.naon.grid.annotation;

import com.naon.grid.enums.UserLevel;
import java.lang.annotation.*;

/**
 * 标注在 Controller 方法上，表示该接口需要指定级别的会员订阅才能访问。
 * 由 SubscriptionAspect 切面处理鉴权逻辑。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSubscription {
    UserLevel value() default UserLevel.VIP;
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-common/src/main/java/com/naon/grid/enums/UserLevel.java
git add grid-common/src/main/java/com/naon/grid/annotation/RequireSubscription.java
git commit -m "feat: add UserLevel enum and @RequireSubscription annotation"
```

---

### Task 2: 错误码 + Repository 查询方法

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/enums/AppErrorCode.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/repository/GridUserRoleRepository.java`

- [ ] **Step 1: 在 AppErrorCode 末尾（SYSTEM_ERROR 之前）添加错误码**

在 `AppErrorCode.java` 的 `SYSTEM_ERROR(5000, "系统繁忙，请稍后重试")` 之前添加：

```java
// 订阅相关 1200-1299
SUBSCRIPTION_REQUIRED(1200, "需要订阅后才能访问此内容"),
SUBSCRIPTION_EXPIRED(1201, "订阅已过期，请续费"),
```

完整插入位置（在 `DEVICE_LIMIT_EXCEEDED` 和 `INVALID_PHONE` 之间）：

```java
    DEVICE_LIMIT_EXCEEDED(1006, "设备数量超出限制"),

    // 订阅相关 1200-1299
    SUBSCRIPTION_REQUIRED(1200, "需要订阅后才能访问此内容"),
    SUBSCRIPTION_EXPIRED(1201, "订阅已过期，请续费"),

    // 参数错误 1100-1199
    INVALID_PHONE(1100, "手机号格式错误"),
```

- [ ] **Step 2: 在 GridUserRoleRepository 中添加查询方法**

```java
/**
 * 查询用户当前有效的会员角色编码
 * expire_time 为 NULL（永久有效）或 > 当前时间
 */
@Query("SELECT r.roleCode FROM GridUserRole r " +
       "WHERE r.userId = :userId " +
       "AND r.roleCode IN ('VIP', 'SVIP') " +
       "AND (r.expireTime IS NULL OR r.expireTime > :now)")
List<String> findValidSubscriptionRoles(@Param("userId") Long userId,
                                        @Param("now") Date now);
```

完整文件确认 —— `GridUserRoleRepository.java` 最终内容：

```java
package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.GridUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridUserRoleRepository extends JpaRepository<GridUserRole, Long>, JpaSpecificationExecutor<GridUserRole> {

    List<GridUserRole> findByUserId(Long userId);

    Optional<GridUserRole> findByUserIdAndRoleCode(Long userId, String roleCode);

    List<GridUserRole> findByUserIdAndExpireTimeAfterOrExpireTimeIsNull(Long userId, Date now);

    /**
     * 查询用户当前有效的会员角色编码
     * expire_time 为 NULL（永久有效）或 > 当前时间
     */
    @Query("SELECT r.roleCode FROM GridUserRole r " +
           "WHERE r.userId = :userId " +
           "AND r.roleCode IN ('VIP', 'SVIP') " +
           "AND (r.expireTime IS NULL OR r.expireTime > :now)")
    List<String> findValidSubscriptionRoles(@Param("userId") Long userId,
                                            @Param("now") Date now);
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/enums/AppErrorCode.java
git add grid-app/src/main/java/com/naon/grid/modules/app/repository/GridUserRoleRepository.java
git commit -m "feat: add subscription error codes and repository query"
```

---

### Task 3: SubscriptionAspect AOP 切面

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/aspect/SubscriptionAspect.java`

- [ ] **Step 1: 创建 SubscriptionAspect 切面**

创建 `grid-app/src/main/java/com/naon/grid/modules/app/aspect/SubscriptionAspect.java`：

```java
package com.naon.grid.modules.app.aspect;

import com.naon.grid.annotation.RequireSubscription;
import com.naon.grid.enums.UserLevel;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.enums.AppErrorCode;
import com.naon.grid.modules.app.repository.GridUserRoleRepository;
import com.naon.grid.modules.app.security.AppAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SubscriptionAspect {

    private final GridUserRoleRepository userRoleRepository;

    @Pointcut("@annotation(com.naon.grid.annotation.RequireSubscription)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object checkSubscription(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取当前认证信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AppAuthenticationToken)) {
            throw new BadRequestException(AppErrorCode.SUBSCRIPTION_REQUIRED.getCode(),
                    AppErrorCode.SUBSCRIPTION_REQUIRED.getMessage());
        }

        AppAuthenticationToken appAuth = (AppAuthenticationToken) authentication;
        Long userId = appAuth.getUserId();

        // 2. 从 JWT roles 快速预检（不进数据库）
        List<String> jwtRoles = appAuth.getRoles();
        boolean hasSubscriptionInJwt = jwtRoles.contains("VIP") || jwtRoles.contains("SVIP");
        if (!hasSubscriptionInJwt) {
            throw new BadRequestException(AppErrorCode.SUBSCRIPTION_REQUIRED.getCode(),
                    AppErrorCode.SUBSCRIPTION_REQUIRED.getMessage());
        }

        // 3. 查数据库验证 expire_time 是否有效
        List<String> validRoles = userRoleRepository.findValidSubscriptionRoles(userId, new Date());
        if (validRoles.isEmpty()) {
            throw new BadRequestException(AppErrorCode.SUBSCRIPTION_EXPIRED.getCode(),
                    AppErrorCode.SUBSCRIPTION_EXPIRED.getMessage());
        }

        // 4. 将有效的角色编码转换为 UserLevel，取最高级别
        UserLevel highestLevel = null;
        for (String roleCode : validRoles) {
            try {
                UserLevel level = UserLevel.valueOf(roleCode);
                if (highestLevel == null || level.includes(highestLevel)) {
                    highestLevel = level;
                }
            } catch (IllegalArgumentException e) {
                // 忽略非会员角色编码
            }
        }

        if (highestLevel == null) {
            throw new BadRequestException(AppErrorCode.SUBSCRIPTION_REQUIRED.getCode(),
                    AppErrorCode.SUBSCRIPTION_REQUIRED.getMessage());
        }

        // 5. 从注解获取所需级别
        RequireSubscription requireSubscription = getAnnotation(joinPoint);
        UserLevel requiredLevel = requireSubscription.value();

        // 6. 层级比较
        if (!highestLevel.includes(requiredLevel)) {
            throw new BadRequestException(AppErrorCode.SUBSCRIPTION_REQUIRED.getCode(),
                    AppErrorCode.SUBSCRIPTION_REQUIRED.getMessage());
        }

        return joinPoint.proceed();
    }

    private RequireSubscription getAnnotation(ProceedingJoinPoint joinPoint) {
        // 从方法上获取注解
        String methodName = joinPoint.getSignature().getName();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        for (java.lang.reflect.Method method : targetClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                RequireSubscription annotation = method.getAnnotation(RequireSubscription.class);
                if (annotation != null) {
                    return annotation;
                }
            }
        }
        // 不应走到这里（pointcut 确保有注解才会进入）
        throw new IllegalStateException("@RequireSubscription annotation not found");
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/aspect/SubscriptionAspect.java
git commit -m "feat: add SubscriptionAspect AOP for @RequireSubscription checking"
```

---

### Task 4: SubscriptionService + DTOs

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/AppSubscriptionVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/CreateOrderDTO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/ActivateSubscriptionDTO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/SubscriptionService.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/SubscriptionServiceImpl.java`

- [ ] **Step 1: 创建 AppSubscriptionVO（认证用户查询自己订阅状态的响应 VO）**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import java.util.Date;

/**
 * 用户订阅状态 VO
 */
@Data
public class AppSubscriptionVO {
    /** 会员级别：NORMAL / VIP / SVIP */
    private String level;
    /** 过期时间，null 表示未订阅 */
    private Date expireTime;
    /** 是否即将到期（15天内） */
    private boolean expiringSoon;
}
```

- [ ] **Step 2: 创建 CreateOrderDTO**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建订阅订单请求 DTO
 */
@Data
public class CreateOrderDTO {
    @NotBlank(message = "会员级别不能为空")
    private String level;    // "VIP" / "SVIP"

    @NotNull(message = "订阅时长不能为空")
    private Integer periodMonths;  // 订阅月数
}
```

- [ ] **Step 3: 创建 ActivateSubscriptionDTO（支付回调/内部调用激活订阅）**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 激活订阅请求 DTO（支付系统回调时调用）
 */
@Data
public class ActivateSubscriptionDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "会员级别不能为空")
    private String level;    // "VIP" / "SVIP"

    @NotNull(message = "订阅天数不能为空")
    private Integer days;    // 订阅有效天数
}
```

- [ ] **Step 4: 创建 SubscriptionService 接口**

```java
package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.service.dto.ActivateSubscriptionDTO;
import com.naon.grid.modules.app.service.dto.AppSubscriptionVO;

public interface SubscriptionService {

    /**
     * 查询当前用户订阅状态
     */
    AppSubscriptionVO getMySubscription(Long userId);

    /**
     * 激活订阅（支付回调时调用）
     * 支持：新购、续期、升级
     */
    void activateSubscription(ActivateSubscriptionDTO dto);

    /**
     * 注册时自动发放试用会员
     */
    void grantTrial(Long userId);
}
```

- [ ] **Step 5: 创建 SubscriptionServiceImpl 实现**

```java
package com.naon.grid.modules.app.service.impl;

import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.GridUserRole;
import com.naon.grid.modules.app.repository.GridUserRoleRepository;
import com.naon.grid.modules.app.service.SubscriptionService;
import com.naon.grid.modules.app.service.dto.ActivateSubscriptionDTO;
import com.naon.grid.modules.app.service.dto.AppSubscriptionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final GridUserRoleRepository userRoleRepository;

    @Value("${app.subscription.trial-days:7}")
    private int trialDays;

    @Override
    public AppSubscriptionVO getMySubscription(Long userId) {
        List<GridUserRole> roles = userRoleRepository.findByUserIdAndExpireTimeAfterOrExpireTimeIsNull(userId, new Date());

        AppSubscriptionVO vo = new AppSubscriptionVO();
        vo.setLevel("NORMAL");
        vo.setExpireTime(null);
        vo.setExpiringSoon(false);

        // 查找最高级别的会员角色
        GridUserRole highest = null;
        for (GridUserRole role : roles) {
            if ("SVIP".equals(role.getRoleCode())) {
                highest = role;
                break;  // SVIP 是最高级
            }
            if ("VIP".equals(role.getRoleCode())) {
                highest = role;
                // 继续检查是否有 SVIP
            }
        }

        if (highest != null) {
            vo.setLevel(highest.getRoleCode());
            vo.setExpireTime(highest.getExpireTime());

            // 检查是否 15 天内到期
            if (highest.getExpireTime() != null) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, 15);
                vo.setExpiringSoon(highest.getExpireTime().before(cal.getTime()));
            }
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateSubscription(ActivateSubscriptionDTO dto) {
        String level = dto.getLevel();
        if (!"VIP".equals(level) && !"SVIP".equals(level)) {
            throw new BadRequestException("不支持的会员级别: " + level);
        }

        // 查询用户已有的同级别角色
        Optional<GridUserRole> existingRole = userRoleRepository
                .findByUserIdAndRoleCode(dto.getUserId(), level);

        if (existingRole.isPresent()) {
            GridUserRole role = existingRole.get();
            Date now = new Date();

            // 如果已过期，从当前时间重新计算
            if (role.getExpireTime() != null && role.getExpireTime().before(now)) {
                role.setExpireTime(addDays(now, dto.getDays()));
                role.setRoleName(level + "会员");
            } else {
                // 续期：延长 expire_time
                role.setExpireTime(addDays(role.getExpireTime() != null ? role.getExpireTime() : now, dto.getDays()));
            }
            userRoleRepository.save(role);
        } else {
            // 新购：创建新角色
            GridUserRole newRole = new GridUserRole();
            newRole.setUserId(dto.getUserId());
            newRole.setRoleCode(level);
            newRole.setRoleName(level + "会员");
            newRole.setExpireTime(addDays(new Date(), dto.getDays()));
            userRoleRepository.save(newRole);
        }

        log.info("Subscription activated: userId={}, level={}, days={}", dto.getUserId(), level, dto.getDays());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantTrial(Long userId) {
        if (trialDays <= 0) {
            return;
        }

        GridUserRole trialRole = new GridUserRole();
        trialRole.setUserId(userId);
        trialRole.setRoleCode("VIP");
        trialRole.setRoleName("VIP会员");
        trialRole.setExpireTime(addDays(new Date(), trialDays));
        userRoleRepository.save(trialRole);

        log.info("Trial granted: userId={}, days={}", userId, trialDays);
    }

    private static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }
}
```

- [ ] **Step 6: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/AppSubscriptionVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/CreateOrderDTO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/ActivateSubscriptionDTO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/service/SubscriptionService.java
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/SubscriptionServiceImpl.java
git commit -m "feat: add subscription service and DTOs"
```

---

### Task 5: AppSubscriptionController（订阅 API）

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppSubscriptionController.java`

- [ ] **Step 1: 创建 AppSubscriptionController**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.modules.app.rest.vo.AppCharBookListVO;
import com.naon.grid.modules.app.security.AppAuthenticationToken;
import com.naon.grid.modules.app.service.SubscriptionService;
import com.naon.grid.modules.app.service.dto.ActivateSubscriptionDTO;
import com.naon.grid.modules.app.service.dto.AppSubscriptionVO;
import com.naon.grid.modules.app.service.dto.CreateOrderDTO;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/subscription")
@Api(tags = "用户：订阅接口")
public class AppSubscriptionController {

    private final SubscriptionService subscriptionService;

    @ApiOperation("查询我的订阅状态")
    @GetMapping("/my")
    public ResponseEntity<AppSubscriptionVO> getMySubscription() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        AppSubscriptionVO vo = subscriptionService.getMySubscription(userId);
        return ResponseEntity.ok(vo);
    }

    @ApiOperation("创建订阅订单（预留）")
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@Validated @RequestBody CreateOrderDTO dto) {
        Long userId = AppSecurityUtils.getCurrentUserId();

        // V1 简化：仅验证参数并返回 orderId，不做实际支付处理
        // 后续对接 StoreKit / Google Play / 支付宝时完善
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", System.currentTimeMillis());
        result.put("level", dto.getLevel());
        result.put("periodMonths", dto.getPeriodMonths());
        result.put("userId", userId);
        // 实际支付信息由支付渠道 SDK 处理，此处仅预留
        return ResponseEntity.ok(result);
    }

    @ApiOperation("激活订阅（支付回调/内部调用）")
    @AnonymousPostMapping("/activate")
    @Log("激活订阅")
    public ResponseEntity<Void> activateSubscription(@Validated @RequestBody ActivateSubscriptionDTO dto) {
        subscriptionService.activateSubscription(dto);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppSubscriptionController.java
git commit -m "feat: add subscription controller with query and activate APIs"
```

---

### Task 6: 注册自动试用

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java`

- [ ] **Step 1: 修改 AppAuthServiceImpl 增加试用**

注入 `SubscriptionService` 并在 `register()` 末尾调用 `grantTrial()`：

在 `AppAuthServiceImpl.java` 中：

1. 在 `private final DeviceManager deviceManager;` 之后添加：
```java
private final SubscriptionService subscriptionService;
```

2. 在 `userRoleRepository.save(normalRole);` 之后（即 `generateToken` 调用之前）添加：
```java
// 注册自动送试用
subscriptionService.grantTrial(user.getId());
```

完整修改后的 `register()` 方法中的相关代码段（在 save user 并创建 normalRole 之后）：

```java
        user = userRepository.save(user);

        GridUserRole normalRole = new GridUserRole();
        normalRole.setUserId(user.getId());
        normalRole.setRoleCode("NORMAL");
        normalRole.setRoleName("普通用户");
        userRoleRepository.save(normalRole);

        // 注册自动送试用
        subscriptionService.grantTrial(user.getId());

        return generateToken(user, registerDTO.getDeviceId(), registerDTO.getDeviceName());
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java
git commit -m "feat: grant trial subscription on user registration"
```

---

### Task 7: 编译验证

- [ ] **Step 1: 编译项目**

```bash
mvn clean compile -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: 修复任何编译错误并重新编译直至通过**

---

### Task 8: 在现有 Controller 上标注 @RequireSubscription（示例性标注）

**说明：** 此步骤展示如何将注解用于实际 Controller 方法。以下仅作示例，具体标注哪些接口由后续产品需求决定。

- [ ] **Step 1: 在 AppCharCharacterController 的 detail 接口上标注 @RequireSubscription（示例）**

将 `AppCharCharacterController.java` 中的 `getDetail` 方法的注解从 `@AnonymousGetMapping` 改为 `@GetMapping` 并添加 `@RequireSubscription(UserLevel.VIP)`：

```java
@RequireSubscription(UserLevel.VIP)
@ApiOperation("根据ID查询汉字详情")
@GetMapping("/{id}")
public ResponseEntity<AppCharCharacterDetailVO> getDetail(
        @PathVariable Integer id,
        @RequestParam String language) {
```

注意：当前 `getDetail` 使用 `@AnonymousGetMapping`，表示公开访问。改为 `@GetMapping` + `@RequireSubscription` 后需要登录并且有 VIP 订阅。

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java
git commit -m "feat: add @RequireSubscription to character detail endpoint as example"
```

---

### Task 9: 配置项（可选）

**Files:**
- Modify: `grid-system/src/main/resources/config/application.yml`

由于 `@Value` 已提供默认值，此步骤可选。如需显式配置，在 `application.yml` 中添加：

```yaml
app:
  subscription:
    trial-days: 7
```

- [ ] **Step 1: 如有需要，添加配置**

```bash
git add grid-system/src/main/resources/config/application.yml
git commit -m "chore: add subscription trial-days config"
```

---

## 计划自检

**Spec Coverage:**
- UserLevel 枚举 + @RequireSubscription 注解 → Task 1
- 错误码 + Repository 查询方法 → Task 2
- SubscriptionAspect AOP 鉴权 → Task 3
- SubscriptionService (查询/激活/试用) → Task 4
- 订阅 API 控制器 → Task 5
- 注册自动试用 → Task 6
- 编译验证 → Task 7
- 示例性注解标注 → Task 8

**No Placeholders:** 所有步骤包含完整代码和命令。

**Type Consistency:** UserLevel 枚举在 Task 1 定义，Task 3 的 Aspect 中使用 `UserLevel.valueOf()` 和 `includes()`，Task 4 的 DTO 和 Task 5 控制器之间类型衔接一致。

---

Plan complete and saved to `docs/superpowers/plans/2026-06-16-subscription.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
