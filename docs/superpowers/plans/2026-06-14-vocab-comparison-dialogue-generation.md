# 词汇辨析对话生成 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 LlmChatController 中新增 /api/llm-chat/vocab-comparison-dialogue 端点，通过大模型根据词汇列表自动生成师生情景对话。

**Architecture:** 采用与已有 /pinyin 端点相同的 Controller 直调模式——Controller 接收结构化请求，构造 ChatRequest 调用 chatService.chat()，解析 LLM 返回的 JSON 文本为结构化对话数组。

**Tech Stack:** Spring Boot 2.7.18, Fastjson2, Java 8

---

### Task 1: 新增 LlmChatConstants 常量

**Files:**
- Modify: `grid-tools/src/main/java/com/naon/grid/constants/LlmChatConstants.java`

- [ ] **Step 1: 追加对话生成提示词常量和温度常量**

在 `LlmChatConstants.java` 中 `PINYIN_DEFAULT_TEMPERATURE` 之后追加：

```java
    /**
     * 词汇辨析对话生成系统提示词
     */
    public static final String DIALOGUE_SYSTEM_PROMPT = "你是一位专业的对外汉语教师。你的任务是根据提供的词汇信息，生成一段 4-5 轮的师生情景对话，帮助学生辨析这些近义词的用法区别。\n" +
            "\n" +
            "对话结构要求：\n" +
            "1. 对话以老师引入（lead-in）开始，引出这几个词汇\n" +
            "2. 学生提出疑问或尝试造句，展现对该组词汇的混淆\n" +
            "3. 老师通过对比分析和例句帮助学生区分\n" +
            "4. 最后老师做一次总结归纳\n" +
            "\n" +
            "角色规范：\n" +
            "- \"teacher\"：老师。用清晰、准确的语言解释差异，穿插贴近生活的例句\n" +
            "- \"student\"：学生。提出自然且有代表性的问题，展现从疑惑到理解的学习过程\n" +
            "\n" +
            "内容要求：\n" +
            "- 对话必须自然流畅，模拟真实课堂场景\n" +
            "- 每个词汇至少在对话中出现一次\n" +
            "- 例句应贴合日常生活场景，便于学生理解\n" +
            "\n" +
            "输出格式：\n" +
            "严格按照以下 JSON 数组格式输出，不要包含 markdown 代码块标记或任何说明文字：\n" +
            "[{\"role\": \"teacher\", \"content\": \"...\"}, {\"role\": \"student\", \"content\": \"...\"}]";

    /**
     * 词汇辨析对话生成默认温度参数
     * 使用较高温度增加对话的多样性和自然度
     */
    public static final double DIALOGUE_DEFAULT_TEMPERATURE = 0.8;
```

- [ ] **Step 2: Commit**

```bash
cd C:\Users\nano\Desktop\nano-gemini
git add grid-tools/src/main/java/com/naon/grid/constants/LlmChatConstants.java
git commit -m "feat: 新增词汇辨析对话生成提示词常量"
```

### Task 2: 新增对话生成端点 + inner class DTO

**Files:**
- Modify: `grid-tools/src/main/java/com/naon/grid/rest/LlmChatController.java`

- [ ] **Step 1: 添加验证和 JSON 解析相关 import**

在已有 import 后追加：

```java
import com.naon.grid.exception.BadRequestException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
```

注意 `javax.validation.Valid` 和 `javax.validation.constraints.NotEmpty` 已在类路径中（其他 Controller 在用）。

- [ ] **Step 2: 新增 inner class DTO（在 LlmChatController 类体内）**

在类结尾 `}` 之前追加：

```java
    @Data
    public static class VocabComparisonDialogueRequest {
        @NotEmpty(message = "词汇列表不能为空")
        @ApiModelProperty(value = "词汇列表", required = true)
        private List<VocabWordInfo> words;
    }

    @Data
    public static class VocabWordInfo {
        @NotBlank(message = "词汇词头不能为空")
        @ApiModelProperty(value = "词汇词头", required = true)
        private String word;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "用法对比")
        private String usageComparison;
    }

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

- [ ] **Step 3: 新增对话生成端点方法（在 pinyin 方法之后）**

```java
    @Log("词汇辨析对话生成")
    @ApiOperation(value = "词汇辨析对话生成", notes = "根据词汇列表生成师生情景对话，帮助辨析近义词用法区别")
    @AnonymousPostMapping("/vocab-comparison-dialogue")
    public ResponseEntity<VocabComparisonDialogueResponse> generateDialogue(
            @Validated @RequestBody VocabComparisonDialogueRequest request) {

        // 1. 构造 userPrompt：格式化词汇信息
        StringBuilder userPromptBuilder = new StringBuilder("请为以下词汇生成辨析情景对话：\n");
        for (int i = 0; i < request.getWords().size(); i++) {
            VocabWordInfo word = request.getWords().get(i);
            userPromptBuilder.append("\n").append(i + 1).append(". ").append(word.getWord());
            if (word.getPartOfSpeech() != null && !word.getPartOfSpeech().isEmpty()) {
                userPromptBuilder.append("（").append(word.getPartOfSpeech()).append("）");
            }
            if (word.getUsageComparison() != null && !word.getUsageComparison().isEmpty()) {
                userPromptBuilder.append("\n   用法对比：").append(word.getUsageComparison());
            }
        }

        // 2. 调用大模型
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setProvider(ChatProviderEnum.ALIYUN);
        chatRequest.setModel("qwen-plus");
        chatRequest.setSystemPrompt(LlmChatConstants.DIALOGUE_SYSTEM_PROMPT);
        chatRequest.setUserPrompt(userPromptBuilder.toString());
        chatRequest.setTemperature(LlmChatConstants.DIALOGUE_DEFAULT_TEMPERATURE);

        ChatResponse chatResponse = chatService.chat(chatRequest);

        // 3. 解析 JSON 响应
        List<VocabChatBaseVO> dialogues = parseDialogueResponse(chatResponse.getContent());

        // 4. 返回
        VocabComparisonDialogueResponse response = new VocabComparisonDialogueResponse();
        response.setChats(dialogues);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * 解析 LLM 返回的 JSON 字符串为对话列表
     */
    private List<VocabChatBaseVO> parseDialogueResponse(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new BadRequestException("对话生成失败，模型返回为空");
        }

        try {
            String trimmed = jsonText.trim();
            // 处理 LLM 可能用 markdown 代码块包裹的情况
            if (trimmed.startsWith("```")) {
                trimmed = trimmed.replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            JSONArray jsonArray = JSON.parseArray(trimmed);
            List<VocabChatBaseVO> result = new ArrayList<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                com.alibaba.fastjson2.JSONObject obj = jsonArray.getJSONObject(i);
                String role = obj.getString("role");
                String content = obj.getString("content");
                if (role != null && !role.isEmpty() && content != null && !content.isEmpty()) {
                    VocabChatBaseVO vo = new VocabChatBaseVO();
                    vo.setRole(role);
                    vo.setContent(content);
                    result.add(vo);
                }
            }

            if (result.isEmpty()) {
                throw new BadRequestException("对话生成失败，生成的对话内容为空");
            }

            return result;
        } catch (Exception e) {
            log.error("解析对话生成响应失败: {}", e.getMessage());
            throw new BadRequestException("对话生成失败，请重试");
        }
    }
```

- [ ] **Step 4: 确认 import 完整**

确保 `LlmChatController.java` 顶部包含以下新增 import（如果 IDE 自动优化则无需手动加）：

```java
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.naon.grid.enums.ChatProviderEnum;
import com.naon.grid.exception.BadRequestException;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
```

注意 `ChatProviderEnum` 可能需要补充——查看 `ChatRequest` 中已有 `import com.naon.grid.enums.ChatProviderEnum;`，确认 Controller 中已引用。

- [ ] **Step 5: Commit**

```bash
cd C:\Users\nano\Desktop\nano-gemini
git add grid-tools/src/main/java/com/naon/grid/rest/LlmChatController.java
git commit -m "feat: 新增词汇辨析对话生成端点 /api/llm-chat/vocab-comparison-dialogue"
```

### Task 3: 编译验证

- [ ] **Step 1: 编译 grid-tools 模块确认无编译错误**

```bash
cd C:\Users\nano\Desktop\nano-gemini
mvn compile -pl grid-tools -am -DskipTests -q
```

Expected: BUILD SUCCESS（无输出，-q 静默模式）

- [ ] **Step 2: 完整编译确认无影响**

```bash
cd C:\Users\nano\Desktop\nano-gemini
mvn compile -DskipTests -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 提交 plan 文档**

```bash
cd C:\Users\nano\Desktop\nano-gemini
git add docs/superpowers/plans/2026-06-14-vocab-comparison-dialogue-generation.md
git commit -m "docs: 词汇辨析对话生成实现计划"
```
