# .env 配置方案设计文档

## 概述

将项目中的敏感配置（MySQL、Redis、JWT、RSA等）从 YAML 文件中移出，使用 `.env` 文件统一管理，避免敏感信息提交到 Git。

## 背景

当前 `application-dev.yml` 中直接硬编码了云服务器的数据库密码、Redis 密码等敏感信息，存在安全风险。

## 方案选型

使用 **dotenv-java** 库加载 `.env` 文件。

### 为什么选择 dotenv-java？

1. 轻量级，简单易用
2. 支持多位置加载，适配开发和生产环境
3. 不破坏现有 Spring Boot 配置体系
4. 成熟稳定，社区广泛使用

## 设计详情

### 1. 文件结构

```
nano-gemini/
├── .env                    ← 本地环境配置（.gitignore）
├── .env.example            ← 配置模板（提交到 Git）
├── .gitignore              ← 添加 .env 规则
├── pom.xml                 ← 添加 dotenv-java 依赖
├── docs/superpowers/specs/
│   └── 2026-05-18-env-config-design.md
└── grid-system/
    └── src/main/java/com/naon/grid/
        └── AppRun.java     ← 修改启动类，加载 .env
```

### 2. .env 加载策略

dotenv-java 按以下优先级加载配置：

1. **当前工作目录**（jar 包同级目录或项目根目录）
2. **系统环境变量**（兜底方案）

这样：
- **开发环境**：在项目根目录放 `.env`
- **生产环境**：在 jar 包同级目录放 `.env`

### 3. 配置项映射

| 环境变量名 | YAML 配置路径 | 说明 |
|-----------|--------------|------|
| DB_HOST | spring.datasource.druid.url | MySQL 主机 |
| DB_PORT | spring.datasource.druid.url | MySQL 端口 |
| DB_NAME | spring.datasource.druid.url | MySQL 数据库名 |
| DB_USER | spring.datasource.druid.username | MySQL 用户名 |
| DB_PWD | spring.datasource.druid.password | MySQL 密码 |
| REDIS_HOST | spring.redis.host | Redis 主机 |
| REDIS_PORT | spring.redis.port | Redis 端口 |
| REDIS_PWD | spring.redis.password | Redis 密码 |
| REDIS_DB | spring.redis.database | Redis DB 索引 |
| JWT_SECRET | jwt.base64-secret | JWT Base64 密钥 |
| RSA_PRIVATE_KEY | rsa.private_key | RSA 私钥 |
| DRUID_PASSWORD | spring.datasource.druid.stat-view-servlet.login-password | Druid 控制台密码 |
| S3_ACCESS_KEY | amz.s3.accessKey | S3 Access Key |
| S3_SECRET_KEY | amz.s3.secretKey | S3 Secret Key |

### 4. .env.example 模板内容

```bash
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

## 实现步骤

### 步骤 1：添加 dotenv-java 依赖

在根目录 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>dotenv-java</artifactId>
    <version>3.0.0</version>
</dependency>
```

### 步骤 2：修改 AppRun.java 启动类

在 `SpringApplication.run()` 之前加载 `.env` 文件：

```java
import io.github.cdimascio.dotenv.Dotenv;

public static void main(String[] args) {
    // 加载 .env 文件
    Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();
    // 将 .env 中的配置设置到系统属性
    dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    
    SpringApplication.run(AppRun.class, args);
}
```

### 步骤 3：修改配置文件

所有敏感配置都改用 `${ENV_VAR:default}` 格式：

- `application.yml`
- `application-dev.yml`
- `application-prod.yml`

**示例：**
```yaml
spring:
  redis:
    host: ${REDIS_HOST:127.0.0.1}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PWD:}
```

### 步骤 4：创建 .env.example 模板

在项目根目录创建 `.env.example`，内容见上方。

### 步骤 5：更新 .gitignore

在 `.gitignore` 中添加：
```
# 本地环境配置
.env
```

### 步骤 6：清理现有敏感配置

从 `application-dev.yml` 中移除硬编码的密码，改用环境变量。

## 使用说明

### 开发环境

1. 复制 `.env.example` 为 `.env`
2. 修改 `.env` 中的配置为你的本地环境值
3. 正常启动项目即可

### 生产环境

1. 将 `.env` 文件放在 jar 包同级目录
2. 或者直接设置系统环境变量
3. 启动 jar 包

## 回退方案

如果 dotenv-java 不适合，可以随时回退：
1. 移除 AppRun.java 中的 dotenv 代码
2. 移除 pom.xml 中的依赖
3. 直接在配置文件中硬编码或设置系统环境变量

## 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|-----|------|------|---------|
| .env 文件被误提交 | 中 | 低 | .gitignore 保护，代码审查 |
| .env 文件权限过大 | 低 | 中 | 文档提醒设置正确权限 |
| 忘记配置 .env 导致启动失败 | 中 | 低 | 提供默认值和清晰的错误提示 |
