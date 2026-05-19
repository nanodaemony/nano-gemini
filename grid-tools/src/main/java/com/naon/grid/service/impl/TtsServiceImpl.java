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
import cn.hutool.core.util.IdUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
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
import com.naon.grid.service.dto.TtsRequest;
import com.naon.grid.service.dto.TtsResponse;
import com.naon.grid.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

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
        // 验证必选参数
        if (StringUtils.isBlank(request.getVoice())) {
            throw new BadRequestException("音色名称不能为空");
        }
        if (StringUtils.isBlank(request.getText())) {
            throw new BadRequestException("合成文本不能为空");
        }

        // 设置 DashScope 配置
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";

        String originalAudioUrl = null;
        String requestId = null;

        try {
            // 构建请求参数
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

            // 调用 API
            MultiModalConversationResult result = conv.call(param);

            // 获取音频 URL
            originalAudioUrl = result.getOutput().getAudio().getUrl();
            requestId = result.getRequestId();

            log.info("TTS 合成成功，阿里云临时 URL: {}, requestId: {}", originalAudioUrl, requestId);

            // 下载音频
            byte[] audioBytes;
            try (InputStream in = new URL(originalAudioUrl).openStream()) {
                audioBytes = in.readAllBytes();
            }

            // 上传到自己的 OSS
            String finalAudioUrl = uploadToOss(audioBytes, request);

            // 保存记录
            TtsRecord record = new TtsRecord();
            record.setVoice(request.getVoice());
            record.setText(request.getText());
            record.setInstructions(request.getInstructions());
            record.setModel(request.getModel());
            record.setLanguageType(request.getLanguageType());
            record.setFinalAudioUrl(finalAudioUrl);
            record.setRequestId(requestId);
            ttsRecordRepository.save(record);

            return new TtsResponse(finalAudioUrl);

        } catch (Exception e) {
            log.error("TTS 合成失败: {}", e.getMessage(), e);
            throw new BadRequestException("语音合成失败: " + e.getMessage());
        }
    }

    /**
     * 上传音频到自己的 OSS
     */
    private String uploadToOss(byte[] audioBytes, TtsRequest request) {
        String bucketName = aliOssConfig.getBucketName();
        // 生成文件名
        String fileRealName = IdUtil.simpleUUID() + ".wav";
        String originalFilename = "tts_" + System.currentTimeMillis() + ".wav";

        // 生成路径
        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(OssBusinessType.TTS.getValue());
        String timeFolder = DateUtil.format(new Date(), aliOssConfig.getTimeformat());
        pathBuilder.append("/").append(timeFolder);
        String filePath = pathBuilder.toString() + "/" + fileRealName;

        // 构建访问 URL
        String fileUrl = aliOssConfig.getDomain() + "/" + filePath;

        try {
            // 上传到 OSS
            ossClient.putObject(bucketName, filePath, new ByteArrayInputStream(audioBytes));

            // 保存记录到 oss_resource_meta
            AliOssStorage storage = new AliOssStorage();
            storage.setFileName(originalFilename);
            storage.setFileRealName(fileRealName);
            storage.setFileSize(com.naon.grid.utils.FileUtil.getSize(audioBytes.length));
            storage.setFileMimeType("audio/wav");
            storage.setFileType("wav");
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
}
