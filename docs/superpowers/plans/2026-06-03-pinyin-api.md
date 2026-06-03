# 中文拼音生成 API 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现中文文案生成带声调拼音的 API 端点，基于现有的大模型对话接口。

**Architecture:** 在 LlmChatController 中添加 `/pinyin` 端点，创建专用 DTO，内部复用 ChatService 和 ChatRecord 机制。

**Tech Stack:** Spring Boot 2.7, Lombok, Swagger, Fastjson

---

### Task 1: 更新 LlmChatConstants 添加常量

**Files:**
- Modify: `grid-tools/src/main/java/com/naon/grid/constants/LlmChatConstants.java`

- [ ] **Step 1: 修改 LlmChatConstants.java**

添加 PINYIN_SYSTEM_PROMPT 和 PINYIN_DEFAULT_TEMPERATURE 常量：

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.constants;

/**
 * 大模型对话相关常量
 */
public class LlmChatConstants {

    /**
     * 拼音生成系统提示词
     */
    public static final String PINYIN_SYSTEM_PROMPT = "你是一个专业的中文拼音转换工具。请将用户输入的中文文本转换为带声调的拼音。\n" +
            "\n" +
            "要求：\n" +
            "1. 拼音格式：使用带声调的拉丁字母表示（如 Nǐ hǎo）\n" +
            "2. 标点符号：保留原文中的所有标点符号，位置不变\n" +
            "3. 分词处理：中文词之间用空格分隔（如 \"xǐhuan\" 而不是 \"xǐ huan\"）\n" +
            "4. 大小写：句子首字母大写，其余小写（专有名词除外）\n" +
            "5. 直接输出：只输出拼音结果，不要输出任何其他内容、说明或解释\n" +
            "\n" +
            "示例：\n" +
            "输入：\"你好，我喜欢吃米饭，你喜欢吃吗？\"\n" +
            "输出：\"Nǐ hǎo, wǒ xǐhuan chī mǐfàn, nǐ xǐhuan chī ma?\"";

    /**
     * 拼音生成默认温度参数
     * 使用低温降低输出随机性
     */
    public static final double PINYIN_DEFAULT_TEMPERATURE = 0.05;
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile -pl grid-tools -am
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/constants/LlmChatConstants.java
git commit -m "feat: add pinyin constants to LlmChatConstants"
```

---

### Task 2: 创建 PinyinRequest DTO

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/PinyinRequest.java`

- [ ] **Step 1: 创建 PinyinRequest.java**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.service.dto;

import com.naon.grid.enums.ChatProviderEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 拼音生成请求 DTO
 * @author nano
 * @date 2026-06-03
 */
@Data
@ApiModel(description = "拼音生成请求参数")
public class PinyinRequest {

    @NotNull(message = "厂商不能为空")
    @ApiModelProperty(value = "大模型厂商", required = true, allowableValues = "ALIYUN,DEEPSEEK", example = "ALIYUN")
    private ChatProviderEnum provider;

    @NotBlank(message = "模型名称不能为空")
    @ApiModelProperty(value = "模型名称", required = true, notes = "阿里云: qwen-plus/qwen-max/qwen-turbo; DeepSeek: deepseek-chat", example = "qwen-plus")
    private String model;

    @NotBlank(message = "中文文案不能为空")
    @ApiModelProperty(value = "待转换的中文文案", required = true, example = "你好，我喜欢吃米饭")
    private String chineseText;
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile -pl grid-tools -am
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/dto/PinyinRequest.java
git commit -m "feat: add PinyinRequest DTO"
```

---

### Task 3: 创建 PinyinResponse DTO

**Files:**
- Create: `grid-tools/src/main/java/com/naon/grid/service/dto/PinyinResponse.java`

- [ ] **Step 1: 创建 PinyinResponse.java**

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.service.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼音生成响应 DTO
 * @author nano
 * @date 2026-06-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "拼音生成响应结果")
public class PinyinResponse {

    @ApiModelProperty(value = "拼音结果", required = true, example = "Nǐ hǎo, wǒ xǐhuan chī mǐfàn")
    private String pinyin;

    @ApiModelProperty(value = "厂商请求 ID", notes = "由大模型厂商返回的唯一请求标识，用于问题排查")
    private String requestId;

    @ApiModelProperty(value = "输入 token 数", notes = "用户输入的 token 数量，用于计费")
    private Integer inputTokens;

    @ApiModelProperty(value = "输出 token 数", notes = "模型输出的 token 数量，用于计费")
    private Integer outputTokens;

    @ApiModelProperty(value = "总 token 数", notes = "输入和输出的 token 总数，用于计费")
    private Integer totalTokens;

    @ApiModelProperty(value = "请求耗时", notes = "从收到请求到返回响应的总耗时，单位毫秒")
    private Long latencyMs;
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile -pl grid-tools -am
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/dto/PinyinResponse.java
git commit -m "feat: add PinyinResponse DTO"
```

---

### Task 4: 在 LlmChatController 中添加 /pinyin 端点

**Files:**
- Modify: `grid-tools/src/main/java/com/naon/grid/rest/LlmChatController.java`

- [ ] **Step 1: 修改 LlmChatController.java**

添加必要的 import 和新的端点：

```java
/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.rest;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.constants.LlmChatConstants;
import com.naon.grid.service.ChatService;
import com.naon.grid.service.dto.ChatRequest;
import com.naon.grid.service.dto.ChatResponse;
import com.naon.grid.service.dto.PinyinRequest;
import com.naon.grid.service.dto.PinyinResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 大模型对话控制器
 * 提供与阿里云百炼、DeepSeek 等大模型的对话能力
 * @author nano
 * @date 2026-06-02
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Api(tags = "工具：大模型对话")
@RequestMapping("/api/llm-chat")
public class LlmChatController {

    private final ChatService chatService;

    /**
     * 大模型对话接口
     *
     * 使用示例 1 - 直接对话：
     * <pre>{@code
     * {
     *   "provider": "ALIYUN",
     *   "model": "qwen-plus",
     *   "systemPrompt": "你是一个乐于助人的AI助手",
     *   "userPrompt": "你好，请介绍一下你自己",
     *   "temperature": 0.7
     * }
     * }</pre>
     *
     * 使用示例 2 - 使用预设提示词：
     * <pre>{@code
     * {
     *   "provider": "ALIYUN",
     *   "model": "qwen-plus",
     *   "promptName": "common_assistant",
     *   "userPrompt": "你好，请介绍一下你自己",
     *   "temperature": 0.7
     * }
     * }</pre>
     *
     * 使用示例 3 - 带占位符的预设提示词：
     * <pre>{@code
     * {
     *   "provider": "ALIYUN",
     *   "model": "qwen-plus",
     *   "promptName": "translator",
     *   "userPrompt": "你好",
     *   "temperature": 0.7,
     *   "placeholderValues": {
     *     "key": "value"
     *   }
     * }
     * }</pre>
     */
    @Log("大模型对话")
    @ApiOperation(value = "大模型对话", notes = "支持阿里云百炼和 DeepSeek，支持自定义提示词或预设提示词")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "对话成功"),
            @ApiResponse(code = 400, message = "请求参数错误"),
            @ApiResponse(code = 500, message = "服务内部错误")
    })
    @AnonymousPostMapping("/completions")
    public ResponseEntity<ChatResponse> chat(@Validated @RequestBody ChatRequest request) {
        return new ResponseEntity<>(chatService.chat(request), HttpStatus.OK);
    }

    /**
     * 中文拼音生成接口
     *
     * 使用示例：
     * <pre>{@code
     * {
     *   "provider": "ALIYUN",
     *   "model": "qwen-plus",
     *   "chineseText": "你好，我喜欢吃米饭"
     * }
     * }</pre>
     */
    @Log("中文拼音生成")
    @ApiOperation(value = "中文拼音生成", notes = "将中文文案转换为带声调的拼音")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "转换成功"),
            @ApiResponse(code = 400, message = "请求参数错误"),
            @ApiResponse(code = 500, message = "服务内部错误")
    })
    @AnonymousPostMapping("/pinyin")
    public ResponseEntity<PinyinResponse> pinyin(@Validated @RequestBody PinyinRequest request) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setProvider(request.getProvider());
        chatRequest.setModel(request.getModel());
        chatRequest.setSystemPrompt(LlmChatConstants.PINYIN_SYSTEM_PROMPT);
        chatRequest.setUserPrompt(request.getChineseText());
        chatRequest.setTemperature(LlmChatConstants.PINYIN_DEFAULT_TEMPERATURE);

        ChatResponse chatResponse = chatService.chat(chatRequest);

        PinyinResponse response = PinyinResponse.builder()
                .pinyin(chatResponse.getContent())
                .requestId(chatResponse.getRequestId())
                .inputTokens(chatResponse.getInputTokens())
                .outputTokens(chatResponse.getOutputTokens())
                .totalTokens(chatResponse.getTotalTokens())
                .latencyMs(chatResponse.getLatencyMs())
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn clean compile -pl grid-tools -am
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-tools/src/main/java/com/naon/grid/rest/LlmChatController.java
git commit -m "feat: add pinyin endpoint to LlmChatController"
```

---

## 自审检查

- [x] **Spec coverage**: 所有 spec 需求都已覆盖（常量、DTO、端点、复用 ChatService/ChatRecord）
- [x] **Placeholder scan**: 无 TBD/TODO，所有代码都已提供
- [x] **Type consistency**: 类型、方法签名一致

---
