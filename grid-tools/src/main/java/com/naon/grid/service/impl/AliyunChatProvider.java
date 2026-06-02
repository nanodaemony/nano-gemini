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

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.utils.Constants;
import com.naon.grid.config.ChatAliyunConfig;
import com.naon.grid.enums.ChatProviderEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.service.ChatProvider;
import com.naon.grid.service.dto.ChatRequest;
import com.naon.grid.service.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云百炼对话 Provider
 * @author nano
 * @date 2026-06-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunChatProvider implements ChatProvider {

    private final ChatAliyunConfig chatAliyunConfig;

    @Override
    public ChatProviderEnum getProvider() {
        return ChatProviderEnum.ALIYUN;
    }

    @Override
    public ChatResponse chat(ChatRequest request, String systemPrompt) {
        try {
            if (chatAliyunConfig.getBaseUrl() != null) {
                Constants.baseHttpApiUrl = chatAliyunConfig.getBaseUrl();
            }

            List<Message> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(Message.builder().role(Role.SYSTEM.getValue()).content(systemPrompt).build());
            }
            messages.add(Message.builder().role(Role.USER.getValue()).content(request.getUserPrompt()).build());

            GenerationParam.Builder paramBuilder = GenerationParam.builder()
                    .apiKey(chatAliyunConfig.getApiKey())
                    .model(request.getModel())
                    .messages(messages);

            if (request.getTemperature() != null) {
                paramBuilder.temperature(request.getTemperature().floatValue());
            }
            if (request.getTopP() != null) {
                paramBuilder.topP(request.getTopP().floatValue());
            }
            if (request.getMaxTokens() != null) {
                paramBuilder.maxTokens(request.getMaxTokens());
            }

            Generation gen = new Generation();
            GenerationResult result = gen.call(paramBuilder.build());

            String content = "";
            Integer inputTokens = null;
            Integer outputTokens = null;
            Integer totalTokens = null;

            if (result.getOutput() != null && result.getOutput().getChoices() != null && !result.getOutput().getChoices().isEmpty()) {
                content = result.getOutput().getChoices().get(0).getMessage().getContent();
            }

            if (result.getUsage() != null) {
                inputTokens = result.getUsage().getInputTokens() != null ? result.getUsage().getInputTokens().intValue() : null;
                outputTokens = result.getUsage().getOutputTokens() != null ? result.getUsage().getOutputTokens().intValue() : null;
                totalTokens = result.getUsage().getTotalTokens() != null ? result.getUsage().getTotalTokens().intValue() : null;
            }

            return ChatResponse.builder()
                    .requestId(result.getRequestId())
                    .content(content)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(totalTokens)
                    .build();

        } catch (Exception e) {
            log.error("阿里云对话失败", e);
            throw new BadRequestException("阿里云对话失败: " + e.getMessage());
        }
    }
}
