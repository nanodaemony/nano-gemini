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

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.http_tts.HttpSpeechSynthesisParam;
import com.alibaba.dashscope.audio.http_tts.HttpSpeechSynthesisResult;
import com.alibaba.dashscope.audio.http_tts.HttpSpeechSynthesizer;
import com.alibaba.dashscope.utils.Constants;
import com.aliyun.oss.OSS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.config.AliOssConfig;
import com.naon.grid.config.AliTtsConfig;
import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.domain.TtsRecord;
import com.naon.grid.domain.enums.OssBusinessType;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.repository.AliOssStorageRepository;
import com.naon.grid.repository.TtsRecordRepository;
import com.naon.grid.service.TtsService;
import com.naon.grid.service.dto.CosyVoiceTtsRequest;
import com.naon.grid.service.dto.TtsRequest;
import com.naon.grid.service.dto.TtsResponse;
import com.naon.grid.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云 TTS 语音合成 Service 实现
 * @author nano
 * @date 2025-05-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsServiceImpl implements TtsService {

    private final AliTtsConfig aliTtsConfig;
    private final AliOssConfig aliOssConfig;
    private final OSS ossClient;
    private final TtsRecordRepository ttsRecordRepository;
    private final AliOssStorageRepository aliOssStorageRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TtsResponse generate(TtsRequest request) {
        if (StringUtils.isBlank(request.getVoice())) {
            throw new BadRequestException("音色名称不能为空");
        }
        if (StringUtils.isBlank(request.getText())) {
            throw new BadRequestException("合成文本不能为空");
        }

        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";

        try {
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationParam.MultiModalConversationParamBuilder builder = MultiModalConversationParam.builder()
                    .apiKey(aliTtsConfig.getApiKey())
                    .model(request.getModel())
                    .text(request.getText())
                    .parameter("voice", request.getVoice());

            if (StringUtils.isNotBlank(request.getLanguageType())) {
                builder.parameter("language_type", request.getLanguageType());
            }

            if (StringUtils.isNotBlank(request.getInstructions())) {
                builder.parameter("instructions", request.getInstructions());
                builder.parameter("optimize_instructions", true);
            }

            MultiModalConversationParam param = builder.build();
            MultiModalConversationResult result = conv.call(param);

            String originalAudioUrl = result.getOutput().getAudio().getUrl();
            String requestId = result.getRequestId();

            log.info("TTS 合成成功，阿里云临时 URL: {}, requestId: {}", originalAudioUrl, requestId);

            String finalAudioUrl = downloadAndUploadToOss(originalAudioUrl, "wav");
            saveTtsRecord(request.getVoice(), request.getText(), request.getInstructions(),
                    request.getModel(), request.getLanguageType(), finalAudioUrl, requestId);

            return new TtsResponse(finalAudioUrl);

        } catch (Exception e) {
            log.error("TTS 合成失败: {}", e.getMessage(), e);
            throw new BadRequestException("语音合成失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TtsResponse cosyVoiceGenerate(CosyVoiceTtsRequest request) {
        if (StringUtils.isBlank(request.getVoice())) {
            throw new BadRequestException("音色名称不能为空");
        }
        if (StringUtils.isBlank(request.getText())) {
            throw new BadRequestException("合成文本不能为空");
        }

        try {
            HttpSpeechSynthesizer synthesizer = new HttpSpeechSynthesizer();

            HttpSpeechSynthesisParam.HttpSpeechSynthesisParamBuilder<?, ?> builder =
                    HttpSpeechSynthesisParam.builder()
                            .apiKey(aliTtsConfig.getApiKey())
                            .model(request.getModel())
                            .text(request.getText())
                            .voice(request.getVoice());

            if (StringUtils.isNotBlank(request.getFormat())) {
                builder.format(request.getFormat());
            }
            if (request.getSampleRate() != null) {
                builder.sampleRate(request.getSampleRate());
            }
            if (request.getVolume() != null) {
                builder.volume(request.getVolume());
            }
            if (request.getRate() != null) {
                builder.rate(request.getRate());
            }
            if (request.getPitch() != null) {
                builder.pitch(request.getPitch());
            }

            Map<String, Object> extraParams = new HashMap<>();
            if (request.getEnableSsml() != null) {
                extraParams.put("enable_ssml", request.getEnableSsml());
            }
            if (request.getWordTimestampEnabled() != null) {
                extraParams.put("word_timestamp_enabled", request.getWordTimestampEnabled());
            }
            if (request.getSeed() != null) {
                extraParams.put("seed", request.getSeed());
            }
            if (request.getLanguageHints() != null && !request.getLanguageHints().isEmpty()) {
                extraParams.put("language_hints", request.getLanguageHints());
            }
            if (StringUtils.isNotBlank(request.getInstruction())) {
                extraParams.put("instruction", request.getInstruction());
            }
            if (request.getBitRate() != null) {
                extraParams.put("bit_rate", request.getBitRate());
            }
            if (request.getEnableAigcTag() != null) {
                extraParams.put("enable_aigc_tag", request.getEnableAigcTag());
            }
            if (StringUtils.isNotBlank(request.getAigcPropagator())) {
                extraParams.put("aigc_propagator", request.getAigcPropagator());
            }
            if (StringUtils.isNotBlank(request.getAigcPropagateId())) {
                extraParams.put("aigc_propagate_id", request.getAigcPropagateId());
            }
            if (request.getHotFix() != null && !request.getHotFix().isEmpty()) {
                extraParams.put("hot_fix", request.getHotFix());
            }
            if (request.getEnableMarkdownFilter() != null) {
                extraParams.put("enable_markdown_filter", request.getEnableMarkdownFilter());
            }

            if (!extraParams.isEmpty()) {
                builder.parameters(extraParams);
            }

            HttpSpeechSynthesisParam param = builder.build();
            HttpSpeechSynthesisResult result = synthesizer.call(param);

            String originalAudioUrl = result.getAudioInfo().getUrl();
            String requestId = result.getRequestId();

            log.info("CosyVoice 合成成功，临时 URL: {}, requestId: {}", originalAudioUrl, requestId);

            String format = StringUtils.isNotBlank(request.getFormat()) ? request.getFormat() : "mp3";
            String finalAudioUrl = downloadAndUploadToOss(originalAudioUrl, format);

            String languageType = null;
            if (request.getLanguageHints() != null && !request.getLanguageHints().isEmpty()) {
                languageType = request.getLanguageHints().get(0);
            }
            saveTtsRecord(request.getVoice(), request.getText(), request.getInstruction(),
                    request.getModel(), languageType, finalAudioUrl, requestId);

            return new TtsResponse(finalAudioUrl);

        } catch (Exception e) {
            log.error("CosyVoice 合成失败: {}", e.getMessage(), e);
            throw new BadRequestException("语音合成失败: " + e.getMessage());
        }
    }

    /**
     * 从临时 URL 下载音频并上传到自有 OSS
     */
    private String downloadAndUploadToOss(String audioUrl, String format) {
        byte[] audioBytes;
        try (InputStream in = new URL(audioUrl).openStream()) {
            audioBytes = IoUtil.readBytes(in);
        } catch (Exception e) {
            log.error("下载音频失败: {}", e.getMessage(), e);
            throw new BadRequestException("音频下载失败: " + e.getMessage());
        }

        String bucketName = aliOssConfig.getBucketName();
        String extension = format != null ? format : "mp3";
        String fileRealName = IdUtil.simpleUUID() + "." + extension;
        String originalFilename = "tts_" + System.currentTimeMillis() + "." + extension;

        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(OssBusinessType.TTS.getValue());
        String timeFolder = DateUtil.format(new Date(), aliOssConfig.getTimeformat());
        pathBuilder.append("/").append(timeFolder);
        String filePath = pathBuilder.toString() + "/" + fileRealName;

        String fileUrl = aliOssConfig.getDomain() + "/" + filePath;

        try {
            ossClient.putObject(bucketName, filePath, new ByteArrayInputStream(audioBytes));

            AliOssStorage storage = new AliOssStorage();
            storage.setFileName(originalFilename);
            storage.setFileRealName(fileRealName);
            storage.setFileSize(com.naon.grid.utils.FileUtil.getSize(audioBytes.length));
            storage.setFileMimeType("audio/" + extension);
            storage.setFileType(extension);
            storage.setFileUrl(fileUrl);
            storage.setBucketName(bucketName);
            storage.setBusinessType(OssBusinessType.TTS.getValue());
            aliOssStorageRepository.save(storage);

            return fileUrl;

        } catch (Exception e) {
            log.error("上传音频到 OSS 失败: {}", e.getMessage(), e);
            throw new BadRequestException("音频上传失败: " + e.getMessage());
        }
    }

    /**
     * 保存 TTS 合成记录
     */
    private void saveTtsRecord(String voice, String text, String instructions,
                               String model, String languageType, String finalAudioUrl, String requestId) {
        TtsRecord record = new TtsRecord();
        record.setVoice(voice);
        record.setText(text);
        record.setInstructions(instructions);
        record.setModel(model);
        record.setLanguageType(languageType);
        record.setFinalAudioUrl(finalAudioUrl);
        record.setRequestId(requestId);
        ttsRecordRepository.save(record);
    }
}
