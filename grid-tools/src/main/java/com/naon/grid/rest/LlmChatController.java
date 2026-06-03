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
