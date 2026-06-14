# 指定源语言翻译功能设计文档

> 日期：2026-06-14
> 模块：grid-tools
> 基于：docs/superpowers/specs/2026-05-28-aliyun-translate-design.md

## 一、概述

在现有中文→X 翻译接口基础上，新增一个独立接口，支持指定**源语言**和**目标语言**进行翻译（如英语→日语、日语→中文等）。翻译记录表增加 `source_language` 字段以完整记录翻译方向。

## 二、接口设计

### 2.1 新增接口

| 项目 | 值 |
|------|-----|
| 路径 | `POST /api/translate/direct` |
| 方法 | `translateDirect()` |
| 权限 | `@AnonymousPostMapping` |
| 日志 | `@Log("指定源语言翻译")` |

### 2.2 请求 DTO — `TranslateDirectRequest`

```json
{
  "sourceText": "Hello, how are you?",     // 必填，源文本
  "sourceLanguage": "en",                  // 可选，默认 "zh"
  "targetLanguage": "ja",                  // 必填，目标语言代码
  "model": "qwen-plus"                     // 可选，默认 "qwen-plus"
}
```

### 2.3 响应 DTO — `TranslateDirectResponse`

```json
{
  "recordId": 1,
  "sourceText": "Hello, how are you?",
  "sourceLanguage": "en",
  "targetText": "こんにちは、お元気ですか？",
  "targetLanguage": "ja"
}
```

## 三、服务层设计

### 3.1 `TranslateService` 接口

新增方法：

```java
TranslateDirectResponse translateDirect(TranslateDirectRequest request);
```

### 3.2 `TranslateServiceImpl` 实现

**核心流程**：

1. 验证必选参数（sourceText、targetLanguage）
2. 验证文本长度（不超过 500 字）
3. 查询 `LanguageCodeEnum` 验证 sourceLanguage 和 targetLanguage 是否支持
4. 构建动态 prompt：`"请将以下{sourceLanguage英文名}翻译成{targetLanguage英文名}，只返回译文，不要添加任何解释：\n\n{sourceText}"`
5. 调用 DashScope API（复用现有 SDK 调用逻辑）
6. 提取翻译结果和 requestId
7. 保存记录（含 sourceLanguage）
8. 返回 TranslateDirectResponse

**公共方法抽取**：

提取 `callDashScope(model, prompt)` 私有方法，供现有的 `translate()` 和新增的 `translateDirect()` 共用 API 调用逻辑。

### 3.3 现有 `translate()` 方法改动

最小改动：保存记录时增加 `sourceLanguage = "zh"`，确保旧记录显式标记源语言。

## 四、数据库变更

### 4.1 `translate_record` 表

新增字段：

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| source_language | VARCHAR(50) | 源语言代码 | NULLABLE，默认 NULL |

```sql
ALTER TABLE translate_record 
ADD COLUMN source_language VARCHAR(50) NULL COMMENT '源语言代码' AFTER source_text;
```

现有旧记录的 `source_language` 保持 NULL。

### 4.2 `TranslateRecord` 实体

新增字段：

```java
@ApiModelProperty(value = "源语言代码")
@Column(length = 50)
private String sourceLanguage;
```

## 五、涉及文件清单

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `TranslateController.java` | 新增方法 | 新增 `translateDirect()` 端点 |
| `TranslateService.java` | 新增方法 | 新增 `translateDirect()` 接口方法 |
| `TranslateServiceImpl.java` | 新增方法 + 重构 | 新增实现 + 抽取公共 `callDashScope()` |
| `TranslateDirectRequest.java` | **新建** | 请求 DTO |
| `TranslateDirectResponse.java` | **新建** | 响应 DTO |
| `TranslateRecord.java` | 新增字段 | 增加 `sourceLanguage` 字段 |
| 数据库迁移 | 执行 SQL | 新增 `source_language` 列 |

## 六、兼容性说明

- 现有 `POST /api/translate` 接口完全不变，返回结构不变
- 现有翻译记录的 `source_language` 为 NULL，不影响查询和展示
- 新接口的 `sourceLanguage` 默认值为 `zh`，不传时行为等价于中文→X
