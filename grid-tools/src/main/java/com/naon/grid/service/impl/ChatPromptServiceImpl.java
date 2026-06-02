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
import com.naon.grid.repository.ChatPromptRepository;
import com.naon.grid.service.ChatPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 预设提示词服务实现
 * @author nano
 * @date 2026-06-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPromptServiceImpl implements ChatPromptService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final ChatPromptRepository chatPromptRepository;

    @Override
    public ChatPrompt findByName(String name) {
        return chatPromptRepository.findByNameAndStatus(name, 1).orElse(null);
    }

    @Override
    public String replacePlaceholders(String template, Map<String, String> values) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        if (values == null || values.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = values.get(key);
            if (replacement != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(result, matcher.group());
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
