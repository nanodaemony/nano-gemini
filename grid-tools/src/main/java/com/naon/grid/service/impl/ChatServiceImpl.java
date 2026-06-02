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

import com.naon.grid.domain.ChatPrompt;
import com.naon.grid.domain.ChatRecord;
import com.naon.grid.enums.ChatProviderEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.repository.ChatRecordRepository;
import com.naon.grid.service.ChatProvider;
import com.naon.grid.service.ChatPromptService;
import com.naon.grid.service.ChatService;
import com.naon.grid.service.dto.ChatRequest;
import com.naon.grid.service.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 大模型对话服务实现
 * @author nano
 * @date 2026-06-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final List<ChatProvider> chatProviders;
    private final ChatPromptService chatPromptService;
    private final ChatRecordRepository chatRecordRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponse chat(ChatRequest request) {
        String resolvedSystemPrompt = resolveSystemPrompt(request);

        long startTime = System.currentTimeMillis();

        ChatProvider provider = findProvider(request.getProvider());
        ChatResponse response = provider.chat(request, resolvedSystemPrompt);

        long latencyMs = (int) (System.currentTimeMillis() - startTime);
        response.setLatencyMs(latencyMs);

        saveChatRecord(request, resolvedSystemPrompt, response);

        return response;
    }

    private String resolveSystemPrompt(ChatRequest request) {
        String systemPrompt = request.getSystemPrompt();

        if (request.getPromptName() != null && !request.getPromptName().isEmpty()) {
            ChatPrompt chatPrompt = chatPromptService.findByName(request.getPromptName());
            if (chatPrompt == null) {
                throw new BadRequestException("找不到预设提示词: " + request.getPromptName());
            }
            systemPrompt = chatPrompt.getSystemPrompt();

            if (request.getTemperature() == null && chatPrompt.getTemperature() != null) {
                request.setTemperature(chatPrompt.getTemperature().doubleValue());
            }
            if (request.getModel() == null && chatPrompt.getModel() != null) {
                request.setModel(chatPrompt.getModel());
            }
        }

        if (request.getPlaceholderValues() != null && !request.getPlaceholderValues().isEmpty()) {
            systemPrompt = chatPromptService.replacePlaceholders(systemPrompt, request.getPlaceholderValues());
        }

        return systemPrompt;
    }

    private ChatProvider findProvider(ChatProviderEnum providerEnum) {
        return chatProviders.stream()
                .filter(p -> p.getProvider() == providerEnum)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("不支持的厂商: " + providerEnum));
    }

    private void saveChatRecord(ChatRequest request, String systemPrompt, ChatResponse response) {
        ChatRecord record = new ChatRecord();
        record.setProvider(request.getProvider());
        record.setModel(request.getModel());
        record.setPromptName(request.getPromptName());
        record.setSystemPrompt(systemPrompt);
        record.setUserPrompt(request.getUserPrompt());
        record.setAssistantResponse(response.getContent());
        record.setTemperature(request.getTemperature() != null ? BigDecimal.valueOf(request.getTemperature()) : null);
        record.setMaxTokens(request.getMaxTokens());
        record.setTopP(request.getTopP() != null ? BigDecimal.valueOf(request.getTopP()) : null);
        record.setRequestId(response.getRequestId());
        record.setInputTokens(response.getInputTokens());
        record.setOutputTokens(response.getOutputTokens());
        record.setTotalTokens(response.getTotalTokens());
        record.setLatencyMs(response.getLatencyMs());
        record.setUserId(request.getUserId());

        if (request.getPlaceholderValues() != null && !request.getPlaceholderValues().isEmpty()) {
            record.setExtraParams(com.alibaba.fastjson.JSON.toJSONString(request.getPlaceholderValues()));
        }

        chatRecordRepository.save(record);
    }
}
