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
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.naon.grid.enums.ChatProviderEnum;
import com.naon.grid.exception.BadRequestException;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
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
                .build();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

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

        // 2. 调用大模型（provider 和 model 可选，默认 ALIYUN / qwen-plus）
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setProvider(request.getProvider() != null ? request.getProvider() : ChatProviderEnum.ALIYUN);
        chatRequest.setModel(request.getModel() != null ? request.getModel() : "qwen-plus");
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
                } else {
                    log.warn("跳过无效对话条目（索引 {}）：role 或 content 为空", i);
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

    @Data
    public static class VocabComparisonDialogueRequest {
        @NotEmpty(message = "词汇列表不能为空")
        @Size(max = 5, message = "词汇数量不能超过5个")
        @ApiModelProperty(value = "词汇列表", required = true)
        private List<VocabWordInfo> words;

        @ApiModelProperty(value = "大模型厂商", example = "ALIYUN")
        private ChatProviderEnum provider;

        @ApiModelProperty(value = "模型名称", example = "qwen-plus")
        private String model;
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
}
