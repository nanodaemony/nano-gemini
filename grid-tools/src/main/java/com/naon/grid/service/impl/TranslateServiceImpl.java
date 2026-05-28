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
import com.alibaba.dashscope.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.config.AliTranslateConfig;
import com.naon.grid.domain.TranslateRecord;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.repository.TranslateRecordRepository;
import com.naon.grid.service.TranslateService;
import com.naon.grid.service.dto.TranslateRequest;
import com.naon.grid.service.dto.TranslateResponse;
import com.naon.grid.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * 翻译服务实现
 * @author nano
 * @date 2026-05-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateServiceImpl implements TranslateService {

    private final AliTranslateConfig aliTranslateConfig;
    private final TranslateRecordRepository translateRecordRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TranslateResponse translate(TranslateRequest request) {
        // 验证必选参数
        if (StringUtils.isBlank(request.getSourceText())) {
            throw new BadRequestException("源文本不能为空");
        }
        if (StringUtils.isBlank(request.getTargetLanguage())) {
            throw new BadRequestException("目标语言不能为空");
        }

        // 验证文本长度（不超过500字）
        if (request.getSourceText().length() > 500) {
            throw new BadRequestException("源文本长度不能超过500字");
        }

        // 设置 DashScope 配置
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";

        String targetText = null;
        String requestId = null;

        try {
            // 构建翻译提示词
            String prompt = buildTranslatePrompt(request.getSourceText(), request.getTargetLanguage());

            // 构建请求参数
            Generation gen = new Generation();
            GenerationParam param = GenerationParam.builder()
                    .apiKey(aliTranslateConfig.getApiKey())
                    .model(request.getModel())
                    .prompt(prompt)
                    .build();

            // 调用 API
            GenerationResult result = gen.call(param);

            // 获取翻译结果
            targetText = result.getOutput().getText();
            requestId = result.getRequestId();

            log.info("翻译成功，sourceText: {}, targetLanguage: {}, requestId: {}",
                    request.getSourceText(), request.getTargetLanguage(), requestId);

            // 保存记录
            TranslateRecord record = new TranslateRecord();
            record.setSourceText(request.getSourceText());
            record.setTargetText(targetText);
            record.setTargetLanguage(request.getTargetLanguage());
            record.setModel(request.getModel());
            record.setRequestId(requestId);
            translateRecordRepository.save(record);

            return new TranslateResponse(record.getId(), request.getSourceText(), targetText, request.getTargetLanguage());

        } catch (Exception e) {
            log.error("翻译失败: {}", e.getMessage(), e);
            throw new BadRequestException("翻译失败: " + e.getMessage());
        }
    }

    /**
     * 构建翻译提示词
     */
    private String buildTranslatePrompt(String text, String targetLanguage) {
        // 常用语言映射到完整名称
        String languageName = getLanguageFullName(targetLanguage);
        return String.format("请将以下中文文本翻译成%s，只返回译文，不要添加任何解释：\n\n%s", languageName, text);
    }

    /**
     * 获取语言完整名称
     */
    private String getLanguageFullName(String languageCode) {
        switch (languageCode.toLowerCase()) {
            case "en": return "英语";
            case "ja": return "日语";
            case "ko": return "韩语";
            case "fr": return "法语";
            case "de": return "德语";
            case "es": return "西班牙语";
            case "ru": return "俄语";
            default: return languageCode;
        }
    }
}
