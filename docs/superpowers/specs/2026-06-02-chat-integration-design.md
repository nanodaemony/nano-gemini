# 大模型对话集成设计文档

> 日期：2026-06-02
> 模块：grid-tools

## 一、概述

本功能集成多家大模型厂商的对话能力，目前支持阿里云百炼和 DeepSeek。提供统一的对话接口，支持预设提示词管理（含占位符替换），并记录所有对话请求到数据库。

## 二、模块结构

在 `grid-tools` 模块中新增以下内容：

```
grid-tools/src/main/java/com/naon/grid/
├── config/
│   ├── ChatAliyunConfig.java              # 阿里云聊天配置
│   └── ChatDeepSeekConfig.java            # DeepSeek 聊天配置
├── domain/
│   ├── ChatPrompt.java                    # 预设提示词实体
│   └── ChatRecord.java                    # 对话记录实体
├── enums/
│   └── ChatProviderEnum.java              # 厂商枚举
├── repository/
│   ├── ChatPromptRepository.java          # 预设提示词数据访问
│   └── ChatRecordRepository.java          # 对话记录数据访问
├── service/
│   ├── ChatProvider.java                  # Provider 接口
│   ├── ChatService.java                   # 门面 Service 接口
│   ├── ChatPromptService.java             # 预设提示词 Service 接口
│   ├── dto/
│   │   ├── ChatRequest.java               # 对话请求 DTO
│   │   └── ChatResponse.java              # 对话响应 DTO
│   └── impl/
│       ├── AliyunChatProvider.java        # 阿里云 Provider 实现
│       ├── DeepSeekChatProvider.java      # DeepSeek Provider 实现
│       ├── ChatServiceImpl.java           # 门面 Service 实现
│       └── ChatPromptServiceImpl.java     # 预设提示词 Service 实现
└── rest/
    └── ChatController.java                # REST 接口
```

## 三、配置管理

### 3.1 .env 配置

在 `.env` 和 `.env.example` 中添加：

```bash
# 阿里云百炼 Chat 配置
DASHSCOPE_API_KEY=你的API Key

# DeepSeek Chat 配置
DEEPSEEK_API_KEY=你的API Key
```

### 3.2 application.yml 配置

在 `grid-bootstrap/src/main/resources/config/application.yml` 中添加：

```yaml
# 大模型 Chat 配置
chat:
  aliyun:
    api-key: ${DASHSCOPE_API_KEY:}
    base-url: ${ALI_CHAT_BASE_URL:https://dashscope.aliyuncs.com/api/v1}
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:}
    base-url: ${DEEPSEEK_CHAT_BASE_URL:https://api.deepseek.com/v1}
```

## 四、数据库设计

### 4.1 预设提示词表 `chat_prompt`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| name | VARCHAR(100) | 提示词名称，唯一 | NOT NULL, UNIQUE |
| description | VARCHAR(500) | 描述说明 | NULLABLE |
| system_prompt | TEXT | 系统提示词内容 | NOT NULL |
| model | VARCHAR(100) | 推荐使用的模型 | NULLABLE |
| temperature | DECIMAL(3,2) | 推荐温度参数 | NULLABLE |
| status | TINYINT | 状态：1-有效，0-无效 | NOT NULL, DEFAULT 1 |
| create_by | VARCHAR(255) | 创建者 | 继承 BaseEntity |
| update_by | VARCHAR(255) | 更新者 | 继承 BaseEntity |
| create_time | DATETIME | 创建时间 | 继承 BaseEntity |
| update_time | DATETIME | 更新时间 | 继承 BaseEntity |

### 4.2 对话记录表 `chat_record`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| provider | VARCHAR(50) | 厂商：ALIYUN/DEEPSEEK | NOT NULL |
| model | VARCHAR(100) | 使用的模型 | NOT NULL |
| prompt_name | VARCHAR(100) | 使用的预设提示词名称 | NULLABLE |
| system_prompt | TEXT | 实际使用的系统提示词 | NULLABLE |
| user_prompt | TEXT | 用户输入提示词 | NOT NULL |
| assistant_response | TEXT | 模型原始响应 | NOT NULL |
| temperature | DECIMAL(3,2) | 温度参数 | NULLABLE |
| max_tokens | INT | 最大 token 数 | NULLABLE |
| top_p | DECIMAL(3,2) | top_p 参数 | NULLABLE |
| request_id | VARCHAR(100) | 厂商请求 ID | NULLABLE |
| input_tokens | INT | 输入 token 数 | NULLABLE |
| output_tokens | INT | 输出 token 数 | NULLABLE |
| total_tokens | INT | 总 token 数 | NULLABLE |
| latency_ms | INT | 请求耗时（毫秒） | NULLABLE |
| user_id | BIGINT | 用户 ID | NULLABLE |
| extra_params | JSON | 其他额外参数 | NULLABLE |
| create_time | DATETIME | 创建时间 | NOT NULL |

## 五、API 设计

### 5.1 对话接口

**URL**: `POST /api/chat/completions`

**请求参数**:
```json
{
  "provider": "ALIYUN",
  "model": "qwen-plus",
  "promptName": "common_assistant",
  "systemPrompt": "你是一个助手",
  "userPrompt": "你好",
  "temperature": 0.7,
  "maxTokens": 2000,
  "topP": 0.9,
  "placeholderValues": {
    "key": "value"
  },
  "userId": 123
}
```

字段说明：
- `provider`: 必选，厂商：ALIYUN/DEEPSEEK
- `model`: 必选，模型名称
- `promptName`: 可选，预设提示词名称，与 `systemPrompt` 二选一
- `systemPrompt`: 可选，系统提示词，与 `promptName` 二选一
- `userPrompt`: 必选，用户输入提示词
- `temperature`: 可选，默认 0.7
- `maxTokens`: 可选，最大 token 数
- `topP`: 可选，top_p 参数
- `placeholderValues`: 可选，占位符替换参数
- `userId`: 可选，用户 ID

**响应参数**:
```json
{
  "requestId": "xxx",
  "content": "模型原始响应内容",
  "inputTokens": 100,
  "outputTokens": 50,
  "totalTokens": 150,
  "latencyMs": 1200
}
```

## 六、核心流程

### 6.1 对话流程

1. **接收请求参数**：验证必选参数（provider、model、userPrompt）
2. **解析系统提示词**：
   - 如果传了 `promptName`，从数据库查询预设提示词
   - 如果传了 `placeholderValues`，执行占位符替换
   - 否则使用 `systemPrompt`
3. **选择 Provider**：根据 provider 参数选择对应的 ChatProvider 实现
4. **调用大模型 API**：记录开始时间，调用厂商 API
5. **计算耗时**：请求结束后计算耗时（毫秒）
6. **保存对话记录**：将请求参数、响应结果、token 使用量、耗时等存入数据库
7. **返回结果**：返回响应内容

### 6.2 占位符替换逻辑

- 占位符格式：`{{placeholderName}}`
- 实现：使用正则 `\{\{(\w+)\}\}` 匹配所有占位符
- 替换：从 `placeholderValues` Map 中获取对应值进行替换
- 缺失处理：如果某个占位符没有对应值，保留原样

## 七、依赖管理

### 7.1 新增依赖

DeepSeek 使用 OpenAI 兼容的 API，需要添加：

```xml
<!-- OpenAI Java SDK (用于 DeepSeek) -->
<dependency>
    <groupId>com.theokanning.openai-gpt3-java</groupId>
    <artifactId>service</artifactId>
    <version>0.18.2</version>
</dependency>
```

阿里云已有依赖：
- dashscope-sdk-java:2.22.17

## 八、枚举设计

### 8.1 ChatProviderEnum

```java
public enum ChatProviderEnum {
    ALIYUN,
    DEEPSEEK
}
```

## 九、注意事项

1. **预设提示词**：不提供 CRUD 接口，直接在数据库维护
2. **占位符格式**：使用 `{{variable}}` 格式
3. **单轮对话**：本次不支持多轮对话，每次请求独立
4. **匿名访问**：接口不需要鉴权
5. **Token 统计**：input_tokens、output_tokens、total_tokens 都是可选字段
6. **配置结构**：chat 作为第一层，厂商作为第二层
