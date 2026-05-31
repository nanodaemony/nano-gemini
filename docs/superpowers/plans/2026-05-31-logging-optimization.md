# 日志系统优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除数据库日志存储，配置 Logback 实现文件日志（按天+大小滚动、INFO/ERROR分离、异步写入）

**Architecture:** 
- 创建 logback-spring.xml 配置文件
- 简化 LogAspect，移除数据库保存逻辑
- 删除不再需要的数据库相关类（SysLog、LogRepository、SysLogService等）
- 保留 TraceFilter、RequestLogFilter、SensitiveDataMasker

**Tech Stack:** SLF4J + Logback + Spring Boot 2.7

---

## File Structure Map

| 操作 | 文件路径 | 说明 |
|------|----------|------|
| 创建 | `grid-bootstrap/src/main/resources/logback-spring.xml` | Logback 配置文件 |
| 修改 | `grid-logging/src/main/java/com/naon/grid/aspect/LogAspect.java` | 简化，移除数据库逻辑 |
| 删除 | `grid-logging/src/main/java/com/naon/grid/domain/SysLog.java` | JPA实体 |
| 删除 | `grid-logging/src/main/java/com/naon/grid/repository/LogRepository.java` | Repository |
| 删除 | `grid-logging/src/main/java/com/naon/grid/service/SysLogService.java` | Service接口 |
| 删除 | `grid-logging/src/main/java/com/naon/grid/service/impl/SysLogServiceImpl.java` | Service实现 |
| 删除 | `grid-logging/src/main/java/com/naon/grid/rest/SysLogController.java` | REST Controller |
| 删除 | `grid-logging/src/main/java/com/naon/grid/service/dto/SysLogErrorDto.java` | DTO |
| 删除 | `grid-logging/src/main/java/com/naon/grid/service/dto/SysLogQueryCriteria.java` | DTO |
| 删除 | `grid-logging/src/main/java/com/naon/grid/service/dto/SysLogSmallDto.java` | DTO |
| 删除 | `grid-logging/src/main/java/com/naon/grid/service/mapstruct/LogErrorMapper.java` | Mapper |
| 删除 | `grid-logging/src/main/java/com/naon/grid/service/mapstruct/LogSmallMapper.java` | Mapper |

---

### Task 1: 创建 Logback 配置文件

**Files:**
- Create: `grid-bootstrap/src/main/resources/logback-spring.xml`

- [ ] **Step 1: 创建 logback-spring.xml 文件**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 变量定义 -->
    <property name="LOG_PATH" value="logs"/>
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n"/>
    <property name="MAX_FILE_SIZE" value="100MB"/>
    <property name="MAX_HISTORY" value="30"/>

    <!-- 控制台 Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- INFO 文件 Appender（包含 INFO+WARN+ERROR） -->
    <appender name="INFO_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/info.log</file>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/archive/info-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>${MAX_FILE_SIZE}</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>${MAX_HISTORY}</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- ERROR 文件 Appender（仅 ERROR） -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/error.log</file>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/archive/error-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>${MAX_FILE_SIZE}</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>${MAX_HISTORY}</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 异步 INFO Appender -->
    <appender name="ASYNC_INFO" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>512</queueSize>
        <discardingThreshold>128</discardingThreshold>
        <neverBlock>false</neverBlock>
        <appender-ref ref="INFO_FILE"/>
    </appender>

    <!-- 异步 ERROR Appender -->
    <appender name="ASYNC_ERROR" class="ch.qos.logback.classic.AsyncAppender">
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <neverBlock>false</neverBlock>
        <appender-ref ref="ERROR_FILE"/>
    </appender>

    <!-- Spring Profile: dev -->
    <springProfile name="dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="ASYNC_INFO"/>
            <appender-ref ref="ASYNC_ERROR"/>
        </root>
        <!-- 细化 SQL 日志 -->
        <logger name="org.hibernate.SQL" level="DEBUG"/>
        <logger name="org.hibernate.type.descriptor.sql.BasicBinder" level="TRACE"/>
    </springProfile>

    <!-- Spring Profile: prod -->
    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="ASYNC_INFO"/>
            <appender-ref ref="ASYNC_ERROR"/>
        </root>
    </springProfile>

    <!-- Spring Profile: quartz -->
    <springProfile name="quartz">
        <root level="INFO">
            <appender-ref ref="ASYNC_INFO"/>
            <appender-ref ref="ASYNC_ERROR"/>
        </root>
    </springProfile>

</configuration>
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la grid-bootstrap/src/main/resources/`
Expected: 看到 `logback-spring.xml` 文件

- [ ] **Step 3: Commit**

```bash
git add grid-bootstrap/src/main/resources/logback-spring.xml
git commit -m "feat: add logback configuration for file logging"
```

---

### Task 2: 简化 LogAspect（移除数据库逻辑）

**Files:**
- Modify: `grid-logging/src/main/java/com/naon/grid/aspect/LogAspect.java`

- [ ] **Step 1: 读取当前 LogAspect 文件**

- [ ] **Step 2: 替换为简化版本**

```java
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
            var method = signature.getMethod();
            var annotation = method.getAnnotation(Log.class);
            return annotation != null ? annotation.value() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn clean compile -pl grid-logging -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-logging/src/main/java/com/naon/grid/aspect/LogAspect.java
git commit -m "refactor: simplify LogAspect to use file logging only"
```

---

### Task 3: 删除数据库相关的 Service 和 DTO 文件

**Files:**
- Delete: `grid-logging/src/main/java/com/naon/grid/service/SysLogService.java`
- Delete: `grid-logging/src/main/java/com/naon/grid/service/impl/SysLogServiceImpl.java`
- Delete: `grid-logging/src/main/java/com/naon/grid/service/dto/SysLogErrorDto.java`
- Delete: `grid-logging/src/main/java/com/naon/grid/service/dto/SysLogQueryCriteria.java`
- Delete: `grid-logging/src/main/java/com/naon/grid/service/dto/SysLogSmallDto.java`
- Delete: `grid-logging/src/main/java/com/naon/grid/service/mapstruct/LogErrorMapper.java`
- Delete: `grid-logging/src/main/java/com/naon/grid/service/mapstruct/LogSmallMapper.java`

- [ ] **Step 1: 删除 Service 和 DTO 文件**

```bash
rm grid-logging/src/main/java/com/naon/grid/service/SysLogService.java
rm grid-logging/src/main/java/com/naon/grid/service/impl/SysLogServiceImpl.java
rm grid-logging/src/main/java/com/naon/grid/service/dto/SysLogErrorDto.java
rm grid-logging/src/main/java/com/naon/grid/service/dto/SysLogQueryCriteria.java
rm grid-logging/src/main/java/com/naon/grid/service/dto/SysLogSmallDto.java
rm grid-logging/src/main/java/com/naon/grid/service/mapstruct/LogErrorMapper.java
rm grid-logging/src/main/java/com/naon/grid/service/mapstruct/LogSmallMapper.java
```

- [ ] **Step 2: 验证文件已删除**

Run: `ls -la grid-logging/src/main/java/com/naon/grid/service/`
Expected: 不包含已删除的文件

- [ ] **Step 3: Commit**

```bash
git add -u
git commit -m "refactor: remove unused SysLog service and DTO files"
```

---

### Task 4: 删除 Repository 和 Controller 文件

**Files:**
- Delete: `grid-logging/src/main/java/com/naon/grid/repository/LogRepository.java`
- Delete: `grid-logging/src/main/java/com/naon/grid/rest/SysLogController.java`

- [ ] **Step 1: 删除 Repository 和 Controller 文件**

```bash
rm grid-logging/src/main/java/com/naon/grid/repository/LogRepository.java
rm grid-logging/src/main/java/com/naon/grid/rest/SysLogController.java
```

- [ ] **Step 2: 验证文件已删除**

Run: `ls -la grid-logging/src/main/java/com/naon/grid/repository/`
Run: `ls -la grid-logging/src/main/java/com/naon/grid/rest/`
Expected: 不包含已删除的文件

- [ ] **Step 3: Commit**

```bash
git add -u
git commit -m "refactor: remove LogRepository and SysLogController"
```

---

### Task 5: 删除 SysLog 实体类

**Files:**
- Delete: `grid-logging/src/main/java/com/naon/grid/domain/SysLog.java`

- [ ] **Step 1: 删除 SysLog.java 文件**

```bash
rm grid-logging/src/main/java/com/naon/grid/domain/SysLog.java
```

- [ ] **Step 2: 验证文件已删除**

Run: `ls -la grid-logging/src/main/java/com/naon/grid/domain/`
Expected: 不包含 SysLog.java

- [ ] **Step 3: Commit**

```bash
git add -u
git commit -m "refactor: remove SysLog entity"
```

---

### Task 6: 全项目编译验证

**Files:**
- (no changes, just verification)

- [ ] **Step 1: 执行 Maven 编译**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: 如果编译失败，检查并修复问题**

检查是否有其他地方引用了已删除的类，按需修复

- [ ] **Step 3: Commit（如有修复）**

仅在需要修复时执行:
```bash
git add -u
git commit -m "fix: resolve compilation errors from logging refactor"
```

---

### Task 7: 启动应用验证日志功能

**Files:**
- (no changes, just verification)

- [ ] **Step 1: 启动应用**

Run: `cd grid-bootstrap && mvn spring-boot:run -Dspring-boot.run.profiles=dev`
Expected: 应用正常启动

- [ ] **Step 2: 验证日志文件生成**

在另一个终端执行:
```bash
ls -la logs/
```
Expected: 看到 `info.log` 和 `error.log` 文件

- [ ] **Step 3: 验证日志格式**

查看 info.log 前几行:
```bash
head -20 logs/info.log
```
Expected: 看到包含时间、线程、traceId、级别的日志

- [ ] **Step 4: 停止应用**

在运行 Spring Boot 的终端按 Ctrl+C

---

## Plan Self-Review

**1. Spec coverage:**
- ✅ Logback 配置（Task 1）
- ✅ LogAspect 简化（Task 2）
- ✅ 删除数据库相关类（Task 3-5）
- ✅ 编译验证（Task 6）
- ✅ 启动验证（Task 7）

**2. Placeholder scan:**
- ✅ 无占位符
- ✅ 所有代码块完整
- ✅ 所有命令明确

**3. Type consistency:**
- ✅ 文件路径一致
- ✅ 代码引用一致

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-31-logging-optimization.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
