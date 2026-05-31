# 日志系统优化设计文档

**日期**: 2026-05-31
**作者**: Claude
**状态**: 待审核

---

## 1. 背景

当前项目使用两套独立的日志系统：
1. 数据库日志（sys_log 表）- 通过 AOP 切面记录操作日志
2. 控制台日志 - 使用 Spring Boot 默认配置，无文件存储

存在的问题：
- 日志仅输出到控制台，重启后丢失
- 没有日志滚动策略
- INFO/ERROR 日志未分离
- 两套日志系统维护成本高
- 数据库日志实际使用价值有限

---

## 2. 设计目标

- ✅ 移除数据库日志存储
- ✅ 日志持久化到文件
- ✅ INFO/ERROR 分离（INFO 包含 ERROR 以便查看上下文）
- ✅ 按天+大小滚动策略
- ✅ 异步日志写入
- ✅ 统一日志格式（含 TraceId）
- ✅ 保留敏感数据脱敏功能

---

## 3. 日志文件结构

```
logs/
├── info.log          (INFO + WARN + ERROR，全链路上下文)
├── error.log         (仅 ERROR，快速查错)
└── archive/          (历史日志归档目录)
    ├── info-2026-05-30.0.log
    ├── info-2026-05-30.1.log
    ├── error-2026-05-30.0.log
    └── ...
```

---

## 4. 日志格式

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n
```

### 示例输出

```
2026-05-31 14:30:45.123 [http-nio-8000-exec-1] [abc123xy] INFO  c.n.g.a.a.AppAuthController - 登录请求开始
2026-05-31 14:30:45.456 [http-nio-8000-exec-1] [abc123xy] INFO  c.n.g.s.i.UserServiceImpl - 验证用户
2026-05-31 14:30:45.789 [http-nio-8000-exec-1] [abc123xy] ERROR c.n.g.e.h.GlobalExceptionHandler - 数据库连接失败
```

### 格式说明

| 占位符 | 说明 |
|--------|------|
| `%d{yyyy-MM-dd HH:mm:ss.SSS}` | 时间戳，精确到毫秒 |
| `[%thread]` | 线程名 |
| `[%X{traceId}]` | TraceId（从 MDC 获取） |
| `%-5level` | 日志级别，左对齐，占 5 字符 |
| `%logger{36}` | Logger 名称，缩写到 36 字符 |
| `%msg` | 日志消息 |
| `%n` | 换行符 |

---

## 5. 滚动策略

### 5.1 通用配置

| 配置项 | 值 |
|--------|-----|
| 按天分割 | 是，每天 00:00 |
| 按大小分割 | 是，单文件 100MB |
| 保留天数 | 30 天 |
| 压缩归档 | 是（.gz） |

### 5.2 文件命名规则

- 当前日志：`info.log`、`error.log`
- 归档日志：`info-2026-05-31.0.log.gz`、`error-2026-05-31.0.log.gz`
- 同一天多个文件：`.0`、`.1`、`.2` 后缀

---

## 6. 异步配置

### 6.1 AsyncAppender 参数

| 参数 | 值 | 说明 |
|------|-----|------|
| queueSize | 512 | 队列大小 |
| discardingThreshold | 128 | 队列剩余 128 时开始丢弃 DEBUG/INFO |
| neverBlock | false | 队列满时阻塞（保证 ERROR 不丢失） |
| includeCallerData | false | 不包含调用者数据（提升性能） |

### 6.2 丢弃策略

- ERROR：永远不丢弃
- WARN：队列剩余 < 64 时丢弃
- INFO/DEBUG：队列剩余 < 128 时丢弃

---

## 7. Logback 配置文件

位置：`grid-bootstrap/src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 变量定义 -->
    <property name="LOG_PATH" value="logs"/>
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n"/>
    <property name="MAX_FILE_SIZE" value="100MB"/>
    <property name="MAX_HISTORY" value="30"/>

    <!-- 控制台 Appender（开发环境） -->
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

---

## 8. 代码调整清单

### 8.1 删除的文件

| 文件 | 说明 |
|------|------|
| `grid-logging/src/main/java/com/naon/grid/domain/SysLog.java` | JPA 实体 |
| `grid-logging/src/main/java/com/naon/grid/repository/LogRepository.java` | Spring Data JPA Repository |
| `grid-logging/src/main/java/com/naon/grid/service/SysLogService.java` | 服务接口 |
| `grid-logging/src/main/java/com/naon/grid/service/impl/SysLogServiceImpl.java` | 服务实现 |
| `grid-logging/src/main/java/com/naon/grid/service/dto/SysLog*.java` | DTO 文件 |
| `grid-logging/src/main/java/com/naon/grid/service/mapstruct/*.java` | MapStruct Mapper |
| `grid-logging/src/main/java/com/naon/grid/rest/SysLogController.java` | REST 控制器 |

### 8.2 修改的文件

| 文件 | 修改内容 |
|------|----------|
| `grid-logging/src/main/java/com/naon/grid/aspect/LogAspect.java` | 移除数据库保存逻辑，改为直接调用 log.info()/log.error() |
| `grid-logging/src/main/java/com/naon/grid/annotation/Log.java` | 保留（可选，用于标记需要记录的方法） |

### 8.3 保留的文件

| 文件 | 说明 |
|------|------|
| `grid-common/src/main/java/com/naon/grid/logging/TraceFilter.java` | TraceId 生成和传递 |
| `grid-common/src/main/java/com/naon/grid/logging/RequestLogFilter.java` | 请求/响应日志 |
| `grid-common/src/main/java/com/naon/grid/logging/SensitiveDataMasker.java` | 敏感数据脱敏 |
| `grid-common/src/main/java/com/naon/grid/logging/LogConstants.java` | 日志常量 |
| `grid-common/src/main/java/com/naon/grid/config/CustomP6SpyLogger.java` | SQL 日志格式化 |
| `grid-common/src/main/java/com/naon/grid/exception/handler/GlobalExceptionHandler.java` | 全局异常处理 |

### 8.4 新增的文件

| 文件 | 说明 |
|------|------|
| `grid-bootstrap/src/main/resources/logback-spring.xml` | Logback 配置文件 |

---

## 9. LogAspect 调整后代码

```java
package com.naon.grid.aspect;

import lombok.extern.slf4j.Slf4j;
import com.naon.grid.annotation.Log;
import com.naon.grid.utils.RequestHolder;
import com.naon.grid.utils.SecurityUtils;
import com.naon.grid.utils.StringUtils;
import com.naon.grid.utils.ThrowableUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
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

        log.info("[操作日志] 用户: {}, IP: {}, 操作: {}, 方法: {}",
            username, ip, description, joinPoint.getSignature().toShortString());

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
            var signature = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
            var method = signature.getMethod();
            var annotation = method.getAnnotation(Log.class);
            return annotation != null ? annotation.value() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
```

---

## 10. 数据库表处理

### 10.1 选项 A：删除 sys_log 表

```sql
DROP TABLE IF EXISTS sys_log;
```

### 10.2 选项 B：保留表但不再写入

保留表结构作为历史记录参考，应用不再写入新数据。

**建议**：选择 B，保留历史数据供查询。

---

## 11. 回滚方案

如果需要回滚：

1. 恢复删除的文件（从 Git 历史）
2. 移除 logback-spring.xml
3. 恢复 LogAspect 原代码
4. 重启应用

---

## 12. 测试验证

| 测试项 | 验证点 |
|--------|--------|
| 日志文件生成 | 确认 logs/info.log 和 logs/error.log 正常生成 |
| 日志格式 | 确认包含时间、线程、traceId、级别、类名 |
| 异步写入 | 高并发下不影响请求响应 |
| 滚动策略 | 手动触发或等待自动分割验证 |
| TraceId 传递 | 同一请求的所有日志 traceId 一致 |
| 敏感数据脱敏 | password/token 等字段正确脱敏 |

---

## 13. 附录

### 13.1 参考资料

- [Logback 官方文档](http://logback.qos.ch/documentation.html)
- [Spring Boot Logging Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.logging)
- [SLF4J MDC 文档](http://www.slf4j.org/manual.html#mdc)

### 13.2 变更历史

| 版本 | 日期 | 作者 | 说明 |
|------|------|------|------|
| 1.0 | 2026-05-31 | Claude | 初始版本 |

