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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            // TODO: 实现 DeepSeek API 调用
            log.info("DeepSeek 对话请求，provider: {}, model: {}, userPrompt: {}",
                    request.getProvider(), request.getModel(), request.getUserPrompt());

            return ChatResponse.builder()
                    .requestId("deepseek-request-id")
                    .content("DeepSeek 功能待实现")
                    .build();

        } catch (Exception e) {
            log.error("DeepSeek 对话失败", e);
            throw new BadRequestException("DeepSeek 对话失败: " + e.getMessage());
        }
    }
}
