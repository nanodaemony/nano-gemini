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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
                : System.getProperty("VOLCENGINE_API_KEY");

        if (StringUtils.isBlank(apiKey)) {
            throw new BadRequestException("VOLCENGINE_API_KEY 未配置");
        }

        try {
            JSONObject volcengineRequest = buildVolcengineRequest(request);
            String requestBody = volcengineRequest.toJSONString();
            log.debug("Volcengine TTS request: {}", requestBody);

            byte[] audioBytes = sendVolcengineRequest(request.getApiResourceId(), apiKey, apiRequestId, requestBody);
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

    private byte[] sendVolcengineRequest(String apiResourceId, String apiKey, String apiRequestId, String requestBody) throws Exception {
        URL url = new URL(volcengineTtsConfig.getBaseUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Api-Key", apiKey);
            conn.setRequestProperty("X-Api-Resource-Id", apiResourceId);
            conn.setRequestProperty("X-Api-Request-Id", apiRequestId);
            conn.setDoOutput(true);
            conn.setDoInput(true);

            log.info("火山引擎请求 URL: {}", url);
            log.info("火山引擎请求 Body: {}", requestBody);
            log.info("X-Api-Resource-Id: {}", apiResourceId);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            log.info("火山引擎响应 Code: {}", responseCode);

            // 打印响应头
            log.info("=== 火山引擎响应头 ===");
            conn.getHeaderFields().forEach((key, values) -> {
                if (key != null) {
                    log.info("{}: {}", key, String.join(", ", values));
                }
            });

            if (responseCode != 200) {
                // 读取并打印错误响应
                try (java.io.InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = readAllBytes(errorStream);
                        log.error("火山引擎错误响应: {}", errorResponse);
                        throw new BadRequestException("火山引擎 TTS 请求失败: " + responseCode + " - " + errorResponse);
                    }
                }
                throw new BadRequestException("火山引擎 TTS 请求失败: " + responseCode);
            }

            return parseVolcengineResponse(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    private String readAllBytes(java.io.InputStream in) throws Exception {
        java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        return os.toString(StandardCharsets.UTF_8.name());
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
        InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(isr);

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
