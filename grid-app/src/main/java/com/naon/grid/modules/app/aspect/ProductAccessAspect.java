package com.naon.grid.modules.app.aspect;

import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.annotation.RequireOrgRole;
import com.naon.grid.modules.app.annotation.RequireProduct;
import com.naon.grid.modules.app.enums.AppErrorCode;
import com.naon.grid.modules.app.security.AppAuthenticationToken;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ProductAccessAspect {

    private final EntitlementEngine entitlementEngine;

    @Pointcut("@annotation(com.naon.grid.modules.app.annotation.RequireProduct)")
    public void pointcut() {}

    @Around("pointcut()")
    public Object checkAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AppAuthenticationToken)) {
            throw new BadRequestException(AppErrorCode.SUBSCRIPTION_REQUIRED.getMessage());
        }

        AppAuthenticationToken appAuth = (AppAuthenticationToken) authentication;
        Long userId = appAuth.getUserId();

        // Get region from request attribute (set by RegionInterceptor)
        String currentRegion = "C";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String regionAttr = (String) request.getAttribute("_region");
            if (regionAttr != null) currentRegion = regionAttr;
        }

        // Region validation (Phase 1: warn only)
        entitlementEngine.isValidForRegion(userId, currentRegion);

        // Get annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireProduct annotation = AnnotationUtils.findAnnotation(method, RequireProduct.class);
        if (annotation == null) return joinPoint.proceed();

        // Check product access
        String[] requiredProducts = annotation.value();
        if (requiredProducts.length > 0) {
            boolean hasAccess = false;
            for (String productCode : requiredProducts) {
                if (entitlementEngine.hasAccess(userId, productCode)) {
                    hasAccess = true;
                    break;
                }
            }
            if (!hasAccess) {
                log.warn("Product access denied: userId={}, required={}", userId, requiredProducts);
                throw new BadRequestException(AppErrorCode.SUBSCRIPTION_REQUIRED.getMessage());
            }
        }

        // Check org role
        RequireOrgRole requiredOrgRole = annotation.orgRole();
        if (requiredOrgRole == RequireOrgRole.ADMIN) {
            if (!"ADMIN".equals(appAuth.getOrgRole())) {
                log.warn("Org role access denied: userId={}, required=ADMIN, actual={}",
                        userId, appAuth.getOrgRole());
                throw new BadRequestException(AppErrorCode.FORBIDDEN.getMessage());
            }
        } else if (requiredOrgRole == RequireOrgRole.MEMBER) {
            if (appAuth.getOrgId() == null) {
                log.warn("Org role access denied: userId={}, required=MEMBER, not in any org", userId);
                throw new BadRequestException(AppErrorCode.FORBIDDEN.getMessage());
            }
        }

        return joinPoint.proceed();
    }
}
