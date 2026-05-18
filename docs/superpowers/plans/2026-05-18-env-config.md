# .env 配置实现计划

&gt; **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将项目中的敏感配置迁移到 .env 文件，使用 dotenv-java 加载，避免敏感信息提交到 Git。

**Architecture:** 在 Spring Boot 启动前使用 dotenv-java 加载 .env 文件到系统属性，然后在配置文件中通过 `${ENV_VAR}` 引用这些变量。

**Tech Stack:** Spring Boot 2.7.18, dotenv-java 3.0.0, Maven

---

## 文件变更清单

| 操作 | 文件路径 | 说明 |
|-----|---------|------|
| Modify | `pom.xml` | 添加 dotenv-java 依赖 |
| Modify | `grid-system/src/main/java/com/naon/grid/AppRun.java` | 添加 .env 加载代码 |
| Create | `.env.example` | 配置模板文件 |
| Modify | `.gitignore` | 添加 .env 规则 |
| Modify | `grid-system/src/main/resources/config/application.yml` | 使用环境变量 |
| Modify | `grid-system/src/main/resources/config/application-dev.yml` | 使用环境变量 |
| Modify | `grid-system/src/main/resources/config/application-prod.yml` | 使用环境变量 |

---

### Task 1: 添加 dotenv-java 依赖

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 在 pom.xml 中添加依赖**

在 `pom.xml` 的 `&lt;dependencies&gt;` 节点中添加：

```xml
        <!-- dotenv-java - .env 文件支持 -->
        <dependency>
            <groupId>io.github.cdimascio</groupId>
            <artifactId>dotenv-java</artifactId>
            <version>3.0.0</version>
        </dependency>
```

添加位置建议放在 `fastjson2` 依赖之后。

- [ ] **Step 2: 验证 Maven 依赖**

Run:
```bash
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add pom.xml
git commit -m "feat: add dotenv-java dependency for .env support"
```

---

### Task 2: 修改 AppRun.java 启动类

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/AppRun.java`

- [ ] **Step 1: 读取 AppRun.java 的当前内容**

先查看现有代码：

```bash
Read "C:\Users\nano\Desktop\nano-gemini\grid-system\src\main\java\com\naon\grid\AppRun.java"
```

- [ ] **Step 2: 修改 AppRun.java，添加 dotenv 加载代码**

在 `main` 方法的开头添加 dotenv 加载逻辑：

```java
package com.naon.grid;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppRun {

    public static void main(String[] args) {
        // 加载 .env 文件
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        // 将 .env 中的配置设置到系统属性
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(AppRun.class, args);
    }
}
```

- [ ] **Step 3: 编译验证**

Run:
```bash
cd grid-system
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/AppRun.java
git commit -m "feat: load .env file on startup"
```

---

### Task 3: 创建 .env.example 模板文件

**Files:**
- Create: `.env.example`

- [ ] **Step 1: 创建 .env.example 文件**

在项目根目录创建 `.env.example`，内容如下：

```bash
# ==============================================
# .env 配置模板
# 复制此文件为 .env 并修改实际值
# .env 文件已加入 .gitignore，不会被提交
# ==============================================

# MySQL 配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=eladmin
DB_USER=root
DB_PWD=123456

# Redis 配置
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PWD=
REDIS_DB=0

# JWT 配置
JWT_SECRET=ZmQ0ZGI5NjQ0MDQwY2I4MjMxY2Y3ZmI3MjdhN2ZmMjNhODViOTg1ZGE0NTBjMGM4NDA5NzYxMjdjOWMwYWRmZTBlZjlhNGY3ZTg4Y2U3YTE1ODVkZDU5Y2Y3OGYwZWE1NzUzNWQ2YjFjZDc0NGMxZWU2MmQ3MjY1NzJmNTE0MzI=

# RSA 配置
RSA_PRIVATE_KEY=MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEA0vfvyTdGJkdbHkB8mp0f3FE0GYP3AYPaJF7jUd1M0XxFSE2ceK3k2kw20YvQ09NJKk+OMjWQl9WitG9pB6tSCQIDAQABAkA2SimBrWC2/wvauBuYqjCFwLvYiRYqZKThUS3MZlebXJiLB+Ue/gUifAAKIg1avttUZsHBHrop4qfJCwAI0+YRAiEA+W3NK/RaXtnRqmoUUkb59zsZUBLpvZgQPfj1MhyHDz0CIQDYhsAhPJ3mgS64NbUZmGWuuNKp5coY2GIj/zYDMJp6vQIgUueLFXv/eZ1ekgz2Oi67MNCk5jeTF2BurZqNLR3MSmUCIFT3Q6uHMtsB9Eha4u7hS31tj1UWE+D+ADzp59MGnoftAiBeHT7gDMuqeJHPL4b+kC+gzV4FGTfhR9q3tTbklZkD2A==

# Druid 控制台密码
DRUID_PASSWORD=123456

# S3 配置
S3_ACCESS_KEY=填写你的AccessKey
S3_SECRET_KEY=填写你的SecretKey
```

- [ ] **Step 2: 提交**

```bash
git add .env.example
git commit -m "feat: add .env.example template"
```

---

### Task 4: 更新 .gitignore

**Files:**
- Modify: `.gitignore`

- [ ] **Step 1: 读取 .gitignore 的当前内容**

查看是否已存在 .gitignore：

```bash
ls -la "C:\Users\nano\Desktop\nano-gemini\.gitignore" || echo "No .gitignore"
```

- [ ] **Step 2: 创建或更新 .gitignore**

如果不存在，创建一个；如果存在，添加以下内容：

```
# 本地环境配置
.env
```

- [ ] **Step 3: 提交**

```bash
git add .gitignore
git commit -m "feat: ignore .env file"
```

---

### Task 5: 修改 application.yml 主配置文件

**Files:**
- Modify: `grid-system/src/main/resources/config/application.yml`

- [ ] **Step 1: 修改 Redis 配置**

将 Redis 配置改为使用环境变量：

```yaml
  redis:
    database: ${REDIS_DB:0}
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PWD:}
```

- [ ] **Step 2: 修改 JWT 配置**

将 jwt.base64-secret 改为使用环境变量：

```yaml
jwt:
  header: Authorization
  token-start-with: Bearer
  base64-secret: ${JWT_SECRET:ZmQ0ZGI5NjQ0MDQwY2I4MjMxY2Y3ZmI3MjdhN2ZmMjNhODViOTg1ZGE0NTBjMGM4NDA5NzYxMjdjOWMwYWRmZTBlZjlhNGY3ZTg4Y2U3YTE1ODVkZDU5Y2Y3OGYwZWE1NzUzNWQ2YjFjZDc0NGMxZWU2MmQ3MjY1NzJmNTE0MzI=}
```

- [ ] **Step 3: 修改 RSA 配置**

将 rsa.private_key 改为使用环境变量：

```yaml
rsa:
  private_key: ${RSA_PRIVATE_KEY:MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEA0vfvyTdGJkdbHkB8mp0f3FE0GYP3AYPaJF7jUd1M0XxFSE2ceK3k2kw20YvQ09NJKk+OMjWQl9WitG9pB6tSCQIDAQABAkA2SimBrWC2/wvauBuYqjCFwLvYiRYqZKThUS3MZlebXJiLB+Ue/gUifAAKIg1avttUZsHBHrop4qfJCwAI0+YRAiEA+W3NK/RaXtnRqmoUUkb59zsZUBLpvZgQPfj1MhyHDz0CIQDYhsAhPJ3mgS64NbUZmGWuuNKp5coY2GIj/zYDMJp6vQIgUueLFXv/eZ1ekgz2Oi67MNCk5jeTF2BurZqNLR3MSmUCIFT3Q6uHMtsB9Eha4u7hS31tj1UWE+D+ADzp59MGnoftAiBeHT7gDMuqeJHPL4b+kC+gzV4FGTfhR9q3tTbklZkD2A==}
```

- [ ] **Step 4: 修改 S3 配置**

将 amz.s3.accessKey 和 secretKey 改为使用环境变量：

```yaml
amz:
  s3:
    region: test
    endPoint: https://s3.test.com
    domain: https://s3.test.com
    accessKey: ${S3_ACCESS_KEY:}
    secretKey: ${S3_SECRET_KEY:}
    defaultBucket: 填写你的存储桶名称
    timeformat: yyyy-MM
```

- [ ] **Step 5: 提交**

```bash
git add grid-system/src/main/resources/config/application.yml
git commit -m "refactor: use env vars in application.yml"
```

---

### Task 6: 修改 application-dev.yml 开发环境配置

**Files:**
- Modify: `grid-system/src/main/resources/config/application-dev.yml`

- [ ] **Step 1: 修改 MySQL 配置**

更新 Druid 数据源配置，使用环境变量：

```yaml
spring:
  redis:
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PWD:}
  datasource:
    druid:
      db-type: com.alibaba.druid.pool.DruidDataSource
      driverClassName: com.p6spy.engine.spy.P6SpyDriver
      url: jdbc:p6spy:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:eladmin}?serverTimezone=Asia/Shanghai&characterEncoding=utf8&useSSL=false
      username: ${DB_USER:root}
      password: ${DB_PWD:123456}
```

- [ ] **Step 2: 修改 Druid 控制台密码**

```yaml
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        reset-enable: false
        login-username: admin
        login-password: ${DRUID_PASSWORD:123456}
```

- [ ] **Step 3: 修改 JWT 配置**

```yaml
jwt:
  base64-secret: ${JWT_SECRET:ZmQ0ZGI5NjQ0MDQwY2I4MjMxY2Y3ZmI3MjdhN2ZmMjNhODViOTg1ZGE0NTBjMGM4NDA5NzYxMjdjOWMwYWRmZTBlZjlhNGY3ZTg4Y2U3YTE1ODVkZDU5Y2Y3OGYwZWE1NzUzNWQ2YjFjZDc0NGMxZWU2MmQ3MjY1NzJmNTE0MzI=}
```

- [ ] **Step 4: 提交**

```bash
git add grid-system/src/main/resources/config/application-dev.yml
git commit -m "refactor: use env vars in application-dev.yml"
```

---

### Task 7: 修改 application-prod.yml 生产环境配置

**Files:**
- Modify: `grid-system/src/main/resources/config/application-prod.yml`

- [ ] **Step 1: 修改 MySQL 配置**

更新 Druid 数据源配置，使用环境变量：

```yaml
spring:
  redis:
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PWD:}
  datasource:
    druid:
      db-type: com.alibaba.druid.pool.DruidDataSource
      driverClassName: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:eladmin}?serverTimezone=Asia/Shanghai&characterEncoding=utf8&useSSL=false
      username: ${DB_USER:root}
      password: ${DB_PWD:123456}
```

- [ ] **Step 2: 修改 Druid 控制台密码**

```yaml
      stat-view-servlet:
        allow:
        enabled: true
        url-pattern: /druid/*
        reset-enable: false
        login-username: admin
        login-password: ${DRUID_PASSWORD:123456}
```

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/resources/config/application-prod.yml
git commit -m "refactor: use env vars in application-prod.yml"
```

---

### Task 8: 创建本地 .env 文件并测试

**Files:**
- Create: `.env`

- [ ] **Step 1: 从 .env.example 复制创建 .env**

```bash
cp .env.example .env
```

- [ ] **Step 2: 验证 .gitignore 是否生效**

检查 .env 是否被 Git 忽略：

```bash
git status
```
Expected: `.env` 文件不应出现在变更列表中

- [ ] **Step 3: 编译并测试启动**

Run:
```bash
mvn clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交（不提交 .env）**

只提交文档（如果有需要）：

```bash
# 确保 .env 没有被添加
git status
```

---

## 验证清单

- [ ] 所有敏感配置已从 YAML 文件中移除，改用环境变量
- [ ] .env.example 模板已创建
- [ ] .gitignore 已配置忽略 .env
- [ ] dotenv-java 依赖已添加
- [ ] AppRun.java 已修改加载 .env
- [ ] 项目能够正常编译
- [ ] 使用默认值时能够正常启动（无 .env 文件）
- [ ] 使用 .env 文件时能够正常加载配置

---

## 部署说明

### 开发环境
1. 复制 `.env.example` → `.env`
2. 修改 `.env` 中的配置值
3. 正常启动

### 生产环境
**方式 1：使用 .env 文件**
- 将 `.env` 放在 jar 包同级目录
- 启动 jar：`java -jar app.jar`

**方式 2：使用系统环境变量**
- 直接设置系统环境变量（如 Docker、K8s、云服务商配置）
- 不需要 .env 文件
