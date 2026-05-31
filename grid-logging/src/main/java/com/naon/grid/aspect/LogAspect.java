/*
 * Copyright 2019-2025 Zheng Jie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.naon.grid.aspect;

import lombok.extern.slf4j.Slf4j;
import com.naon.grid.annotation.Log;
import com.naon.grid.utils.RequestHolder;
import com.naon.grid.utils.SecurityUtils;
import com.naon.grid.utils.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 日志切面 - 简化版，仅打文件日志
 */
@Component
@Aspect
@Slf4j
public class LogAspect {

    @Pointcut("@annotation(com.naon.grid.annotation.Log)")
    public void logPointcut() {}

    @Around("logPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String username = getUsername();
        String ip = StringUtils.getIp(RequestHolder.getHttpServletRequest());
        String description = getDescription(joinPoint);
        String methodName = joinPoint.getSignature().toShortString();

        log.info("[操作日志] 用户: {}, IP: {}, 操作: {}, 方法: {}",
            username, ip, description, methodName);

        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - startTime;
            log.info("[操作日志] 完成 - 用户: {}, 操作: {}, 耗时: {}ms", username, description, cost);
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("[操作日志] 异常 - 用户: {}, 操作: {}, 耗时: {}ms, 错误: {}",
                username, description, cost, e.getMessage(), e);
            throw e;
        }
    }

    private String getUsername() {
        try {
            return SecurityUtils.getCurrentUsername();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String getDescription(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            java.lang.reflect.Method method = signature.getMethod();
            Log annotation = method.getAnnotation(Log.class);
            return annotation != null ? annotation.value() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
