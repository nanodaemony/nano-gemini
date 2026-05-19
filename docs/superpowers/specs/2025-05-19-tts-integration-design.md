# 阿里云 TTS 语音合成集成设计文档

> 日期：2025-05-19
> 模块：grid-tools

## 一、概述

本功能集成阿里云百炼的非实时语音合成服务，支持将文本转换为语音，并将生成的音频文件上传到自己的阿里云 OSS 存储。

## 二、模块结构

在 `grid-tools` 模块中新增/修改以下内容：

1. **修改**：`OssBusinessType.java` - 添加 TTS 业务类型

```
grid-tools/src/main/java/com/naon/grid/
├── config/
│   └── AliTtsConfig.java                 # TTS 配置类
├── domain/
│   └── TtsRecord.java                    # TTS 记录实体类
├── repository/
│   └── TtsRecordRepository.java          # 数据访问层
├── service/
│   ├── TtsService.java                   # Service 接口
│   ├── dto/
│   │   ├── TtsRequest.java               # 请求 DTO
│   │   └── TtsResponse.java              # 响应 DTO
│   └── impl/
│       └── TtsServiceImpl.java            # Service 实现
└── rest/
    └── TtsController.java                # REST 接口
```

## 三、配置管理

### 3.1 .env 配置

在 `.env` 和 `.env.example` 中添加：

```bash
# 阿里云百炼 TTS 配置
DASHSCOPE_API_KEY=你的API Key
```

### 3.2 application.yml 配置

在 `grid-system/src/main/resources/config/application.yml` 中添加：

```yaml
# 阿里云百炼 TTS 配置
ali:
  tts:
    api-key: ${DASHSCOPE_API_KEY:}
```

### 3.3 常量配置

DashScope base URL 写死在代码中：

- 北京地域：`https://dashscope.aliyuncs.com/api/v1`

## 四、数据库设计

### 4.1 表结构

表名：`tts_record`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| voice | VARCHAR(100) | 音色 | NOT NULL |
| text | TEXT | 合成文本 | NOT NULL |
| instructions | TEXT | 控制指令 | NULLABLE |
| model | VARCHAR(100) | 模型名称 | NOT NULL |
| language_type | VARCHAR(50) | 语言类型 | NULLABLE |
| final_audio_url | VARCHAR(500) | OSS 最终音频 URL | NOT NULL |
| request_id | VARCHAR(100) | 阿里云请求 ID | NULLABLE |
| error_msg | TEXT | 错误信息 | NULLABLE |
| create_by | VARCHAR(255) | 创建者 | 继承 BaseEntity |
| update_by | VARCHAR(255) | 更新者 | 继承 BaseEntity |
| create_time | DATETIME | 创建时间 | 继承 BaseEntity |
| update_time | DATETIME | 更新时间 | 继承 BaseEntity |

## 五、API 设计

### 5.1 语音合成接口

**URL**: `POST /api/tts/generate`

**请求参数**:
```json
{
  "voice": "Cherry",                  // 必选，音色名称
  "text": "需要合成的文本内容",        // 必选，待合成的文本
  "instructions": "语速较快，年轻女声", // 可选，控制指令
  "model": "qwen-tts-flash",          // 可选，模型名称，默认 qwen-tts-flash
  "languageType": "Chinese"           // 可选，语言类型
}
```

**响应参数**:
```json
{
  "audioUrl": "https://your-oss-domain.com/tts/2025-05/xxx.wav"
}
```

## 六、核心流程

1. **接收请求参数**：验证必选参数（voice、text）
2. **调用 DashScope SDK**：
   - 设置 API Key 和 Base URL
   - 构建请求参数（模型、文本、音色、指令等）
   - 调用语音合成 API
3. **获取临时 URL**：
   - 从响应中提取阿里云返回的临时音频 URL
   - 在日志中记录该 URL（用于调试）
4. **下载音频文件**：
   - 从临时 URL 下载音频数据到内存或临时文件
5. **上传到自己的 OSS**：
   - 调用 `AliOssStorageService` 上传音频
   - 指定业务类型为 `TTS`
6. **保存记录**：
   - 只有成功完成所有步骤后才写入数据库
7. **返回结果**：
   - 返回 OSS 上的最终音频 URL

## 七、枚举修改

在 `OssBusinessType.java` 中添加：

```java
/** 语音合成 */
TTS("tts"),
```

## 八、依赖管理

在 `grid-tools/pom.xml` 中添加以下依赖：

```xml
<!-- DashScope SDK -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dashscope-sdk-java</artifactId>
    <version>2.22.17</version>
    <scope>compile</scope>
</dependency>

<!-- Gson (DashScope SDK 需要) -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.13.1</version>
</dependency>
```

## 九、注意事项

1. **音频 URL 有效期**：阿里云返回的临时 URL 有效期为 24 小时，我们只在日志中记录
2. **同步模式**：整个流程是同步的，成功后才写入数据库，因此不需要 status 字段
3. **音色选择**：参考阿里云文档支持的音色列表
4. **模型选择**：
   - 默认：`qwen-tts-flash`
   - 可选：`qwen-tts`、`cosyvoice-v3.5-plus` 等
5. **指令控制**：仅在使用支持指令的模型（如 qwen3-tts-instruct-flash）时生效
