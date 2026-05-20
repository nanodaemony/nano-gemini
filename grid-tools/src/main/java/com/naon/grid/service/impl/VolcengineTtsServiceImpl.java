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
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aliyun.oss.OSS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.config.AliOssConfig;
import com.naon.grid.config.VolcengineTtsConfig;
import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.domain.TtsRecord;
import com.naon.grid.domain.enums.OssBusinessType;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.repository.AliOssStorageRepository;
import com.naon.grid.repository.TtsRecordRepository;
import com.naon.grid.service.VolcengineTtsService;
import com.naon.grid.service.dto.VolcengineTtsRequest;
import com.naon.grid.service.dto.VolcengineTtsResponse;
import com.naon.grid.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * 火山引擎 TTS 语音合成 Service 实现
 * @author nano
 * @date 2026-05-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VolcengineTtsServiceImpl implements VolcengineTtsService {

    private final VolcengineTtsConfig volcengineTtsConfig;
    private final AliOssConfig aliOssConfig;
    private final OSS ossClient;
    private final TtsRecordRepository ttsRecordRepository;
    private final AliOssStorageRepository aliOssStorageRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VolcengineTtsResponse generate(VolcengineTtsRequest request) {
        if (StringUtils.isBlank(request.getText())) {
            throw new BadRequestException("合成文本不能为空");
        }
        if (StringUtils.isBlank(request.getSpeaker())) {
            throw new BadRequestException("音色不能为空");
        }
        if (StringUtils.isBlank(request.getApiResourceId())) {
            throw new BadRequestException("apiResourceId 不能为空");
        }

        String apiRequestId = UUID.randomUUID().toString();
        String apiKey = StringUtils.isNotBlank(volcengineTtsConfig.getApiKey())
                ? volcengineTtsConfig.getApiKey()
                : System.getenv("VOLCENGINE_API_KEY");

        if (StringUtils.isBlank(apiKey)) {
            throw new BadRequestException("VOLCENGINE_API_KEY 未配置");
        }

        try {
            JSONObject volcengineRequest = buildVolcengineRequest(request);
            String requestBody = volcengineRequest.toJSONString();
            log.debug("Volcengine TTS request: {}", requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(volcengineTtsConfig.getBaseUrl()))
                    .header("Content-Type", "application/json")
                    .header("X-Api-Key", apiKey)
                    .header("X-Api-Resource-Id", request.getApiResourceId())
                    .header("X-Api-Request-Id", apiRequestId)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (response.statusCode() != 200) {
                throw new BadRequestException("火山引擎 TTS 请求失败: " + response.statusCode());
            }

            byte[] audioBytes = parseVolcengineResponse(response.body());
            String finalAudioUrl = uploadToOss(audioBytes, request);

            TtsRecord record = new TtsRecord();
            record.setVoice(request.getSpeaker());
            record.setText(request.getText());
            record.setModel(request.getModel());
            record.setVendor("volcengine");
            record.setFinalAudioUrl(finalAudioUrl);
            record.setRequestId(apiRequestId);
            ttsRecordRepository.save(record);

            return new VolcengineTtsResponse(finalAudioUrl);

        } catch (Exception e) {
            log.error("Volcengine TTS 合成失败: {}", e.getMessage(), e);
            throw new BadRequestException("语音合成失败: " + e.getMessage());
        }
    }

    private JSONObject buildVolcengineRequest(VolcengineTtsRequest request) {
        JSONObject result = new JSONObject();

        JSONObject user = new JSONObject();
        user.put("uid", "tts-batch");
        result.put("user", user);

        JSONObject reqParams = new JSONObject();
        reqParams.put("text", request.getText());
        reqParams.put("speaker", request.getSpeaker());

        if (StringUtils.isNotBlank(request.getModel())) {
            reqParams.put("model", request.getModel());
        }

        JSONObject audioParams = new JSONObject();
        if (request.getAudioParams() != null) {
            VolcengineTtsRequest.AudioParams params = request.getAudioParams();
            audioParams.put("format", StringUtils.isNotBlank(params.getFormat()) ? params.getFormat() : "mp3");
            audioParams.put("sample_rate", params.getSampleRate() != null ? params.getSampleRate() : 24000);
            if (params.getSpeechRate() != null) {
                audioParams.put("speech_rate", params.getSpeechRate());
            }
            if (params.getLoudnessRate() != null) {
                audioParams.put("loudness_rate", params.getLoudnessRate());
            }
        } else {
            audioParams.put("format", "mp3");
            audioParams.put("sample_rate", 24000);
        }
        reqParams.put("audio_params", audioParams);

        if (request.getContextTexts() != null && !request.getContextTexts().isEmpty()) {
            JSONObject additions = new JSONObject();
            additions.put("context_texts", request.getContextTexts());
            reqParams.put("additions", additions);
        }

        result.put("req_params", reqParams);
        return result;
    }

    private byte[] parseVolcengineResponse(java.io.InputStream inputStream) throws Exception {
        java.io.ByteArrayOutputStream audioBuffer = new java.io.ByteArrayOutputStream();
        java.io.InputStreamReader isr = new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8);
        java.io.BufferedReader reader = new java.io.BufferedReader(isr);

        StringBuilder jsonBuffer = new StringBuilder();
        int braceCount = 0;
        boolean inJson = false;

        int c;
        while ((c = reader.read()) != -1) {
            char ch = (char) c;

            if (ch == '{') {
                braceCount++;
                inJson = true;
                jsonBuffer.append(ch);
            } else if (ch == '}') {
                braceCount--;
                jsonBuffer.append(ch);

                if (braceCount == 0 && inJson) {
                    JSONObject chunk = JSON.parseObject(jsonBuffer.toString());
                    String data = chunk.getString("data");
                    if (StringUtils.isNotBlank(data)) {
                        byte[] decoded = Base64.getDecoder().decode(data);
                        audioBuffer.write(decoded);
                    }
                    jsonBuffer.setLength(0);
                    inJson = false;
                }
            } else if (inJson) {
                jsonBuffer.append(ch);
            }
        }

        return audioBuffer.toByteArray();
    }

    private String uploadToOss(byte[] audioBytes, VolcengineTtsRequest request) {
        String bucketName = aliOssConfig.getBucketName();
        String format = (request.getAudioParams() != null && StringUtils.isNotBlank(request.getAudioParams().getFormat()))
                ? request.getAudioParams().getFormat()
                : "mp3";
        String fileRealName = IdUtil.simpleUUID() + "." + format;
        String originalFilename = "tts-volcengine-" + System.currentTimeMillis() + "." + format;

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
            storage.setFileMimeType("audio/" + format);
            storage.setFileType(format);
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
