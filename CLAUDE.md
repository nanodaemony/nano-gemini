# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码库中工作提供指导。

## 项目概述

这是"Little Grid" (grid)，一个用于汉语教学的服务端系统，使用 Spring Boot 2.7.18 开发。它是一个中文语言学习平台，包含：
- 管理后台系统 (grid-system)：包含后台用户鉴权、汉语功能管理等功能。
- 面向终端用户的 App API (grid-app)，包含普通用户鉴权、词汇和汉字管理等普通用户使用的业务功能。
- 工具相关：AI 集成，如LLM 对话、TTS、翻译、图像生成等。

## 技术栈

- **框架**: Spring Boot 2.7.18
- **ORM**: Spring Data JPA
- **数据库**: MySQL
- **缓存**: Redis + Redisson
- **安全**: Spring Security + JWT
- **API 文档**: Knife4j (Swagger)
- **构建**: Maven
- **Java 版本**: 1.8
- **JSON**: Fastjson2

## 项目结构

```
grid (根父 pom)
├── grid-common      # 公共模块：工具类、注解、配置、异常处理
├── grid-logging     # 日志模块（基于 AOP）
├── grid-tools       # 第三方工具集成：OSS、TTS、翻译、LLM 对话、邮件
├── grid-system      # 核心后台模块：用户、角色、菜单等
├── grid-app         # 普通 App API 模块：认证、词汇、汉字
└── grid-bootstrap   # 启动引导模块（主应用程序）
```

**依赖关系**:
- grid-bootstrap → grid-app + grid-system
- grid-app → grid-system
- grid-system → grid-tools
- grid-tools → grid-common + grid-logging

## 如何构建和运行

### 前置要求
- MySQL 数据库
- Redis 服务器
- Maven 3+
- JDK 8

### 配置说明

项目使用 `.env` 文件进行配置（dotenv-java）：
- `.env.{profile}` 用于特定环境配置（如 `.env.dev`、`.env.prod`）
- 如果特定环境文件不存在，则回退到 `.env`
- `.env.example` 提供配置模板

**关键配置项**:
- 数据库: `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PWD`
- Redis: `REDIS_HOST`、`REDIS_PORT`、`REDIS_DB`、`REDIS_PWD`
- 阿里云服务: `DASHSCOPE_API_KEY`（用于 TTS、翻译、图像、对话）
- 阿里云 OSS: `ALI_OSS_*`
- 火山引擎 TTS: `VOLCENGINE_API_KEY`
- DeepSeek 对话: `DEEPSEEK_API_KEY`

### 构建

```bash
# 构建所有模块（跳过测试）
mvn clean install -DskipTests

# 或在根目录执行
mvn clean package -DskipTests
```

### 运行

```bash
# 从 grid-bootstrap 模块运行
cd grid-bootstrap
mvn spring-boot:run

# 或运行 JAR 包
java -jar grid-bootstrap/target/grid-bootstrap-2.7-exec.jar

# 指定 profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 访问地址

- 应用: http://localhost:8000
- Swagger/Knife4j: http://localhost:8000/doc.html
- Druid 监控: http://localhost:8000/druid

## 常见开发任务

### 需要了解的关键模块

**grid-common**:
- 自定义注解: `@Log`、`@Limit`、`@Query`、`@AnonymousAccess` 等
- 基类: `BaseEntity`、`BaseDTO`、`BaseMapper`
- 工具类: `RedisUtils`、`FileUtil`、`ValidationUtil`、`DateUtil`、`EncryptUtils`
- 异常处理: `GlobalExceptionHandler`
- 配置: Redis、异步、Web、安全、Swagger

**grid-tools**:
- `/api/llm-chat/completions` - LLM 对话（阿里云/DeepSeek）
- `/api/llm-chat/pinyin` - 中文转拼音
- `/api/tts` - 语音合成（阿里云/火山引擎）
- `/api/translate` - 翻译（阿里云）
- `/api/image` - 图像生成（阿里云）
- `/api/oss` - 阿里云 OSS 存储
- `/api/email` - 邮件服务

**grid-system**:
- 管理用户、角色、菜单、部门
- JWT Token 安全认证
- 系统监控、Quartz 定时任务
- 审计日志
- 汉语学习相关的后台管理接口

**grid-app**:
- `/api/app/auth` - App 用户认证/注册
- 其他汉语学习相关的业务接口
- 独立的 App 安全认证（AppTokenFilter、AppTokenProvider）

### 重要设计模式

- **草稿工作流**: 汉字和词汇使用草稿内容，带有发布/下线状态
- **JSON 序列化**: 翻译字段和列表使用 JSON 列存储
- **软删除**: 实体使用状态标志而非物理删除
- **App 独立认证**: grid-app 有自己独立于管理后台的认证系统

## 数据库字段设计规范

- **命名**: 汉语相关的业务表名带模块前缀（`char_*`、`vocab_*`），字段 snake_case，SQL 保留字加反引号（如 `` `character` ``）
- **主键**: 统一 `id` + `IDENTITY`；业务主表用 `Integer`，资源类表（如 `audio_resource`）用 `Long`
- **审计字段**: 核心内容相关的主表继承 `BaseEntity`，自动带 `create_by`/`update_by`/`create_time`/`update_time`；子表通常只带时间戳
- **三状态模型**（业务实体）:
  - `status` (`Integer`, `StatusEnum`) — 软删除：`1`=可用 / `0`=已删除
  - `publish_status` (`varchar(20)`, `PublishStatusEnum`) — 是否对外发布：`unpublished` / `published`
  - `edit_status` (`varchar(20)`, `EditStatusEnum`) — 编辑流程：`draft` / `reviewed` / `published`
- **草稿工作流**: 主表带 `draft_content` (JSON)。创建/更新只写草稿，发布时才回写主表和子表
- **JSON 字段**: `draft_content`、`translations`、`synonyms` 等用 JSON 列，Fastjson2 序列化。JSON 字段的字段类型为 VARCHAR 或者 text，具体视字段长度而定
- **子表关联**: 用 `{主表}_id` 字段（如 `vocab_sense.word_id`），不使用 JPA `@ManyToOne`，不要使用数据库外键

## 注意事项

- pom.xml 中默认跳过测试
- 使用 MapStruct 进行 DTO/实体映射
- 使用 Lombok 减少样板代码
