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
package com.naon.grid.service.impl;

import com.naon.grid.config.ChatDeepSeekConfig;
import com.naon.grid.enums.ChatProviderEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.service.ChatProvider;
import com.naon.grid.service.dto.ChatRequest;
import com.naon.grid.service.dto.ChatResponse;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek 对话 Provider
 * @author nano
 * @date 2026-06-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekChatProvider implements ChatProvider {

    private final ChatDeepSeekConfig chatDeepSeekConfig;

    @Override
    public ChatProviderEnum getProvider() {
        return ChatProviderEnum.DEEPSEEK;
    }

    @Override
    public ChatResponse chat(ChatRequest request, String systemPrompt) {
        try {
            String apiKey = chatDeepSeekConfig.getApiKey();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .readTimeout(Duration.ofSeconds(120))
                    .writeTimeout(Duration.ofSeconds(30))
                    .build();

            OpenAiService service = new OpenAiService(apiKey, client);

            List<ChatMessage> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new ChatMessage("system", systemPrompt));
            }
            messages.add(new ChatMessage("user", request.getUserPrompt()));

            ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder()
                    .model(request.getModel())
                    .messages(messages);

            if (request.getTemperature() != null) {
                requestBuilder.temperature(request.getTemperature());
            }
            if (request.getTopP() != null) {
                requestBuilder.topP(request.getTopP());
            }
            if (request.getMaxTokens() != null) {
                requestBuilder.maxTokens(request.getMaxTokens());
            }

            ChatCompletionResult result = service.createChatCompletion(requestBuilder.build());

            String content = "";
            Integer inputTokens = null;
            Integer outputTokens = null;
            Integer totalTokens = null;

            if (result.getChoices() != null && !result.getChoices().isEmpty()) {
                content = result.getChoices().get(0).getMessage().getContent();
            }

            if (result.getUsage() != null) {
                inputTokens = result.getUsage().getPromptTokens();
                outputTokens = result.getUsage().getCompletionTokens();
                totalTokens = result.getUsage().getTotalTokens();
            }

            return ChatResponse.builder()
                    .requestId(result.getId())
                    .content(content)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(totalTokens)
                    .build();

        } catch (Exception e) {
            log.error("DeepSeek 对话失败", e);
            throw new BadRequestException("DeepSeek 对话失败: " + e.getMessage());
        }
    }
}
