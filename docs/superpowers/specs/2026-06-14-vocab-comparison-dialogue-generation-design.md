# 词汇辨析对话生成接口设计

## 概述

在 `LlmChatController` 中新增一个 API 端点，通过大模型根据词汇辨析组的条目信息自动生成师生情景对话，替代后台管理员手动编写对话内容的流程。

## 接口定义

### 端点

```
POST /api/llm-chat/vocab-comparison-dialogue
```

标注 `@AnonymousPostMapping`（当前无鉴权，后续按需调整）。

### 请求体

```java
@Data
public static class VocabComparisonDialogueRequest {
    @NotEmpty
    @ApiModelProperty(value = "词汇列表", required = true)
    private List<VocabWordInfo> words;
}

@Data
public static class VocabWordInfo {
    @NotBlank
    @ApiModelProperty(value = "词汇词头", required = true)
    private String word;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "用法对比")
    private String usageComparison;
}
```

### 响应体

```java
@Data
public static class VocabComparisonDialogueResponse {
    @ApiModelProperty(value = "情景对话列表")
    private List<VocabChatBaseVO> chats;
}

@Data
public static class VocabChatBaseVO {
    @ApiModelProperty(value = "角色: teacher=老师, student=学生")
    private String role;

    @ApiModelProperty(value = "中文对话内容")
    private String content;
}
```

相比于 `VocabComparisonGroupCreateRequest.VocabChatRequest`，`VocabChatBaseVO` 省略了 `pinyin`、`translations`、`audioId`、`order` 字段——这些字段后续通过其他流程补充。

## 实现方案

采用与现有 `/pinyin` 端点相同的**Controller 直调**模式：

1. `LlmChatController` 新增方法，接收 `VocabComparisonDialogueRequest`
2. 构造 `ChatRequest`：
   - `systemPrompt` 从 `LlmChatConstants` 取对话生成提示词
   - `userPrompt` 为格式化后的词汇信息
   - `temperature` 固定为 `0.8`（对话需要一定创造性）
3. 调用 `chatService.chat()`
4. 将 LLM 返回的纯文本（期望 JSON 数组）解析为 `List<VocabChatBaseVO>`
5. 返回 `VocabComparisonDialogueResponse`

### 数据流

```
请求 → Controller 参数校验
  → 构造 ChatRequest (systemPrompt + userPrompt)
  → chatService.chat()
  → 原始文本响应
  → JSON 解析 → List<VocabChatBaseVO>
  → 封装成 VocabComparisonDialogueResponse 返回
```

## 系统提示词

存储在 `LlmChatConstants` 中：

```
你是一位专业的对外汉语教师。你的任务是根据提供的词汇信息，生成一段 4-5 轮的师生情景对话，帮助学生辨析这些近义词的用法区别。

对话结构要求：
1. 对话以老师引入（lead-in）开始，引出这几个词汇
2. 学生提出疑问或尝试造句，展现对该组词汇的混淆
3. 老师通过对比分析和例句帮助学生区分
4. 最后老师做一次总结归纳

角色规范：
- "teacher"：老师。用清晰、准确的语言解释差异，穿插贴近生活的例句
- "student"：学生。提出自然且有代表性的问题，展现从疑惑到理解的学习过程

内容要求：
- 对话必须自然流畅，模拟真实课堂场景
- 每个词汇至少在对话中出现一次
- 例句应贴合日常生活场景，便于学生理解

输出格式：
严格按照以下 JSON 数组格式输出，不要包含 markdown 代码块标记或任何说明文字：
[{"role": "teacher", "content": "..."}, {"role": "student", "content": "..."}]
```

## 异常处理

| 场景 | 处理方式 |
|------|----------|
| 词汇列表为空 | `@NotEmpty` 校验失败 → 400 |
| LLM 返回非合法 JSON | `BadRequestException("对话生成失败，请重试")` |
| JSON 数组中元素缺 `role` 或 `content` | 跳过无效元素，保留有效对话轮次 |

## 修改文件清单

| 文件 | 改动 |
|------|------|
| `grid-tools/.../LlmChatController.java` | 新增端点 + inner class DTO |
| `grid-tools/.../LlmChatConstants.java` | 新增 `DIALOGUE_SYSTEM_PROMPT` 和 `DIALOGUE_DEFAULT_TEMPERATURE` |
