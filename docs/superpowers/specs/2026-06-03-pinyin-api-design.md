# 中文拼音生成 API 设计文档

**日期**: 2026-06-03
**作者**: nano
**状态**: 待审核

## 概述

为中文文案生成带声调的拼音字符串，基于现有的大模型对话接口实现。

## 需求背景

用户需要将中文文本转换为带声调的拼音格式，例如：
- 输入："你好，我喜欢吃米饭，你喜欢吃吗？"
- 输出："Nǐ hǎo, wǒ xǐhuan chī mǐfàn, nǐ xǐhuan chī ma?"

## 设计方案

### 架构概览

- 在 `LlmChatController` 中添加新端点 `POST /api/llm-chat/pinyin`
- 创建专用的请求/响应 DTO：`PinyinRequest`、`PinyinResponse`
- 在 `LlmChatConstants` 中存放优化后的 Prompt 和温度参数
- 内部复用 `ChatService` 和 `ChatRecord` 记录机制

### 组件设计

#### 1. LlmChatConstants.java

新增常量：

| 常量名 | 类型 | 值 | 说明 |
|--------|------|-----|------|
| `PINYIN_SYSTEM_PROMPT` | String | 见下方 | 优化后的拼音生成系统提示词 |
| `PINYIN_DEFAULT_TEMPERATURE` | double | 0.05 | 低温设置，降低输出随机性 |

**优化后的 Prompt**:
```
你是一个专业的中文拼音转换工具。请将用户输入的中文文本转换为带声调的拼音。

要求：
1. 拼音格式：使用带声调的拉丁字母表示（如 Nǐ hǎo）
2. 标点符号：保留原文中的所有标点符号，位置不变
3. 分词处理：中文词之间用空格分隔（如 "xǐhuan" 而不是 "xǐ huan"）
4. 大小写：句子首字母大写，其余小写（专有名词除外）
5. 直接输出：只输出拼音结果，不要输出任何其他内容、说明或解释

示例：
输入："你好，我喜欢吃米饭，你喜欢吃吗？"
输出："Nǐ hǎo, wǒ xǐhuan chī mǐfàn, nǐ xǐhuan chī ma?"
```

#### 2. PinyinRequest.java

位置：`grid-tools/src/main/java/com/naon/grid/service/dto/PinyinRequest.java`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `provider` | ChatProviderEnum | 是 | 大模型厂商（ALIYUN/DEEPSEEK）|
| `model` | String | 是 | 模型名称 |
| `chineseText` | String | 是 | 待转换的中文文案 |

#### 3. PinyinResponse.java

位置：`grid-tools/src/main/java/com/naon/grid/service/dto/PinyinResponse.java`

| 字段 | 类型 | 说明 |
|------|------|------|
| `pinyin` | String | 生成的拼音字符串 |
| `requestId` | String | 厂商请求 ID（用于排查）|
| `inputTokens` | Integer | 输入 token 数 |
| `outputTokens` | Integer | 输出 token 数 |
| `totalTokens` | Integer | 总 token 数 |
| `latencyMs` | Long | 请求耗时（毫秒）|

#### 4. LlmChatController.java

新增端点：

```java
@Log("中文拼音生成")
@ApiOperation(value = "中文拼音生成", notes = "将中文文案转换为带声调的拼音")
@AnonymousPostMapping("/pinyin")
public ResponseEntity<PinyinResponse> pinyin(@Validated @RequestBody PinyinRequest request)
```

内部逻辑：
1. 组装 `ChatRequest` 对象
2. 设置 `systemPrompt` 为 `LlmChatConstants.PINYIN_SYSTEM_PROMPT`
3. 设置 `temperature` 为 `LlmChatConstants.PINYIN_DEFAULT_TEMPERATURE`
4. 设置 `userPrompt` 为请求中的 `chineseText`
5. 调用 `chatService.chat()`
6. 将 `ChatResponse` 转换为 `PinyinResponse` 返回

### 数据流程

```
请求 → PinyinRequest → Controller 组装 ChatRequest → ChatService → ChatProvider → LLM
响应 ← PinyinResponse ← 提取 ChatResponse.content ← ChatResponse ← 保存 ChatRecord ←
```

### 复用现有机制

- ChatRecord 复用：通过调用 chatService.chat()，自动保存调用记录到 ChatRecord 表
- ChatProvider 复用：无需修改厂商实现
- 异常处理复用：使用现有的 BadRequestException 等

### API 使用示例

**请求**:
```http
POST /api/llm-chat/pinyin
Content-Type: application/json

{
  "provider": "ALIYUN",
  "model": "qwen-plus",
  "chineseText": "如果你有任何问题、想法或需要协助的地方——无论是认真探讨还是轻松闲聊，我都非常乐意陪伴"
}
```

**响应**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "pinyin": "Rúguǒ nǐ yǒu rènhé wèntí、xiǎngfǎ huò xūyào xiézhù de dìfāng——wúlùn shì rènzhēn tàntǎo háishì qīngsōng xiánliáo, wǒ dōu fēicháng lèyì péibàn",
  "requestId": "xxx",
  "inputTokens": 123,
  "outputTokens": 45,
  "totalTokens": 168,
  "latencyMs": 890
}
```

## 实现清单

- [ ] 更新 `LlmChatConstants.java`，添加 Prompt 和温度常量
- [ ] 创建 `PinyinRequest.java` DTO
- [ ] 创建 `PinyinResponse.java` DTO
- [ ] 在 `LlmChatController.java` 中添加 `/pinyin` 端点
- [ ] 编译验证
