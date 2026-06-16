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
            UserLevel level = UserLevel.fromRoleCode(roleCode);
            if (level != null) {
                if (highestLevel == null || level.includes(highestLevel)) {
                    highestLevel = level;
                }
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
        throw new IllegalStateException("@RequireSubscription annotation not found on method: " + methodName);
    }
}
