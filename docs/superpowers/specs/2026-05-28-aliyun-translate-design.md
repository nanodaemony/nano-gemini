# 阿里云百炼翻译集成设计文档

> 日期：2026-05-28
> 模块：grid-tools

## 一、概述

本功能集成阿里云百炼（DashScope）的通用翻译服务，实现中文文本到其他指定语言的翻译，仅后台管理员使用，不开放给前端用户。功能会保存翻译记录到数据库。

## 二、模块结构

在 `grid-tools` 模块中新增以下内容：

```
grid-tools/src/main/java/com/naon/grid/
├── config/
│   └── AliTranslateConfig.java           # 翻译配置类
├── domain/
│   └── TranslateRecord.java              # 翻译记录实体类
├── repository/
│   └── TranslateRecordRepository.java    # 数据访问层
├── service/
│   ├── TranslateService.java             # Service 接口
│   ├── dto/
│   │   ├── TranslateRequest.java         # 请求 DTO
│   │   └── TranslateResponse.java        # 响应 DTO
│   └── impl/
│       └── TranslateServiceImpl.java      # Service 实现
└── rest/
    └── TranslateController.java           # REST 接口
```

## 三、配置管理

### 3.1 .env 配置

在 `.env` 和 `.env.example` 中添加：

```bash
# 阿里云百炼翻译配置（可复用 DASHSCOPE_API_KEY）
DASHSCOPE_API_KEY=你的API Key
```

### 3.2 application.yml 配置

在 `grid-system/src/main/resources/config/application.yml` 中添加：

```yaml
# 阿里云百炼翻译配置
ali:
  translate:
    api-key: ${DASHSCOPE_API_KEY:}
```

### 3.3 常量配置

DashScope base URL 写死在代码中：

- 北京地域：`https://dashscope.aliyuncs.com/api/v1`

## 四、数据库设计

### 4.1 表结构

表名：`translate_record`

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| id | BIGINT | 主键 | AUTO_INCREMENT |
| source_text | TEXT | 源文本（中文） | NOT NULL |
| target_text | TEXT | 目标文本（译文） | NOT NULL |
| target_language | VARCHAR(50) | 目标语言代码 | NOT NULL |
| model | VARCHAR(100) | 使用的模型 | NOT NULL |
| request_id | VARCHAR(100) | 阿里云请求 ID | NULLABLE |
| create_by | VARCHAR(255) | 创建者 | 继承 BaseEntity |
| update_by | VARCHAR(255) | 更新者 | 继承 BaseEntity |
| create_time | DATETIME | 创建时间 | 继承 BaseEntity |
| update_time | DATETIME | 更新时间 | 继承 BaseEntity |

## 五、API 设计

### 5.1 翻译接口

**URL**: `POST /api/translate`

**请求参数**:
```json
{
  "sourceText": "需要翻译的中文文本",  // 必选，中文原文（不超过500字）
  "targetLanguage": "en",            // 必选，目标语言代码（如 en, ja, ko 等）
  "model": "qwen-max"                // 可选，模型名称，默认 qwen-plus
}
```

**响应参数**:
```json
{
  "recordId": 1,
  "sourceText": "需要翻译的中文文本",
  "targetText": "Translated text",
  "targetLanguage": "en"
}
```

## 六、核心流程

1. **接收请求参数**：验证必选参数（sourceText、targetLanguage）
2. **验证文本长度**：不超过 500 字
3. **调用 DashScope SDK**：
   - 设置 API Key 和 Base URL
   - 构建翻译请求参数
   - 调用通用翻译 API
4. **提取翻译结果**：从响应中获取译文和 requestId
5. **保存记录**：将原文、译文、目标语言、模型、requestId 等存入数据库
6. **返回结果**：返回译文和记录 ID

## 七、依赖管理

依赖已存在于 `grid-tools/pom.xml` 中：

- dashscope-sdk-java:2.22.17
- gson:2.13.1

无需新增依赖。

## 八、成本评估

基于阿里云百炼 DashScope 定价（参考同类模型）：

- **通用翻译模型**：约 ¥0.002 - ¥0.004 / 千字符
- **按次估算**：每次翻译 100 字约 ¥0.0002 - ¥0.0004
- **月度估算**：每天 100 次约 ¥0.6 - ¥1.2 / 月

具体价格以阿里云官方最新定价为准。

## 九、注意事项

1. **文本长度限制**：单次翻译不超过 500 字
2. **同步模式**：整个流程是同步的
3. **不需要缓存**：按需求直接调用 API
4. **语言代码**：使用标准语言代码（en=英语, ja=日语, ko=韩语, fr=法语, de=德语, es=西班牙语, ru=俄语等）
5. **模型选择**：
   - 默认：`qwen-plus`（性价比较高）
   - 可选：`qwen-max`（质量更高但更贵）、`qwen-turbo`（更快更便宜）
