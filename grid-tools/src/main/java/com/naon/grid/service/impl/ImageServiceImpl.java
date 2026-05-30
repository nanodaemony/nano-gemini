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
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.utils.Constants;
import com.aliyun.oss.OSS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.config.AliOssConfig;
import com.naon.grid.config.AliYunImageConfig;
import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.domain.ImageRecord;
import com.naon.grid.domain.enums.OssBusinessType;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.repository.AliOssStorageRepository;
import com.naon.grid.repository.ImageRecordRepository;
import com.naon.grid.service.ImageService;
import com.naon.grid.service.dto.QwenImageBatchRequest;
import com.naon.grid.service.dto.QwenImageBatchResponse;
import com.naon.grid.service.dto.QwenImageRequest;
import com.naon.grid.service.dto.QwenImageResponse;
import com.naon.grid.utils.FileUtil;
import com.naon.grid.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * 千问文生图（Qwen-Image）Service 实现
 * @author nano
 * @date 2026-05-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    /** seed 允许的最大值 */
    private static final long MAX_SEED = 2147483647L;

    /** 不支持 n>1 的模型前缀（需精确匹配，避免误伤 qwen-image-2.0 系列） */
    private static final String[] SINGLE_ONLY_MODEL_PREFIXES = {
            "qwen-image-max", "qwen-image-plus"
    };

    private final AliYunImageConfig aliYunImageConfig;
    private final AliOssConfig aliOssConfig;
    private final OSS ossClient;
    private final ImageRecordRepository imageRecordRepository;
    private final AliOssStorageRepository aliOssStorageRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QwenImageResponse generate(QwenImageRequest request) {
        if (StringUtils.isBlank(request.getPrompt())) {
            throw new BadRequestException("提示词不能为空");
        }
        validateSeed(request.getSeed());

        Constants.baseHttpApiUrl = aliYunImageConfig.getBaseUrl();

        try {
            // 构建请求参数（不传 n，默认 1 张）
            Map<String, Object> parameters = buildCommonParameters(request.getPromptExtend(),
                    request.getWatermark(), request.getNegativePrompt(),
                    request.getSize(), null, request.getSeed());

            // 调用 API
            MultiModalConversationResult result = callApi(
                    aliYunImageConfig.getApiKey(), request.getModel(), request.getPrompt(), parameters);

            String requestId = result.getRequestId();

            // 从响应中提取图片 URL
            List<Map<String, Object>> contentList = extractContentList(result);
            String imageUrl = extractImageUrlFromContent(contentList.get(0));

            log.info("文生图成功，prompt: {}, model: {}, requestId: {}, 临时URL: {}",
                    StringUtils.truncate(request.getPrompt(), 50), request.getModel(), requestId, imageUrl);

            // 下载并上传到 OSS
            String finalUrl = downloadAndUploadToOss(imageUrl);

            // 保存记录
            saveImageRecord(request.getPrompt(), request.getNegativePrompt(), request.getModel(),
                    request.getSize(), 1, request.getPromptExtend(), request.getWatermark(),
                    request.getSeed(), finalUrl, requestId);

            return new QwenImageResponse(finalUrl);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("文生图生成失败: {}", e.getMessage(), e);
            throw new BadRequestException("文生图生成失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QwenImageBatchResponse generateBatch(QwenImageBatchRequest request) {
        if (StringUtils.isBlank(request.getPrompt())) {
            throw new BadRequestException("提示词不能为空");
        }
        validateSeed(request.getSeed());

        int imageCount = (request.getN() != null && request.getN() >= 1) ? request.getN() : 4;
        if (imageCount > 6) {
            throw new BadRequestException("批量生成数量不能超过 6 张");
        }
        // qwen-image-max/plus/image 系列不支持多图
        if (imageCount > 1 && isSingleOnlyModel(request.getModel())) {
            throw new BadRequestException("模型 " + request.getModel() + " 不支持多图生成（n 固定为 1）");
        }

        Constants.baseHttpApiUrl = aliYunImageConfig.getBaseUrl();

        try {
            // 构建请求参数（传入 n）
            Map<String, Object> parameters = buildCommonParameters(request.getPromptExtend(),
                    request.getWatermark(), request.getNegativePrompt(),
                    request.getSize(), imageCount, request.getSeed());

            // 调用 API
            MultiModalConversationResult result = callApi(
                    aliYunImageConfig.getApiKey(), request.getModel(), request.getPrompt(), parameters);

            String requestId = result.getRequestId();
            List<Map<String, Object>> contentList = extractContentList(result);

            if (contentList.size() < imageCount) {
                log.warn("API 返回图片数({})少于请求数({})", contentList.size(), imageCount);
            }

            log.info("文生图批量生成成功，prompt: {}, model: {}, requestId: {}, 请求/返回: {}/{}",
                    StringUtils.truncate(request.getPrompt(), 50), request.getModel(),
                    requestId, imageCount, contentList.size());

            // 处理每张图片
            List<String> ossUrls = new ArrayList<>();
            for (int i = 0; i < contentList.size(); i++) {
                String tempImageUrl = extractImageUrlFromContent(contentList.get(i));
                String finalUrl = downloadAndUploadToOss(tempImageUrl);
                ossUrls.add(finalUrl);

                // 每张图保存一条独立记录
                saveImageRecord(request.getPrompt(), request.getNegativePrompt(), request.getModel(),
                        request.getSize(), imageCount, request.getPromptExtend(), request.getWatermark(),
                        request.getSeed(), finalUrl, requestId);
            }

            return new QwenImageBatchResponse(ossUrls);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("文生图批量生成失败: {}", e.getMessage(), e);
            throw new BadRequestException("文生图批量生成失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否是不支持多图的模型
     * qwen-image-max / qwen-image-plus（含日期变体）→ 不支持
     * qwen-image（裸名）→ 不支持
     * qwen-image-2.0 系列 → 支持
     */
    private boolean isSingleOnlyModel(String model) {
        if (model == null) return false;
        // 精确匹配裸名 "qwen-image"
        if ("qwen-image".equals(model)) return true;
        // 前缀匹配 max/plus 系列（含日期变体如 qwen-image-plus-2026-01-09）
        for (String prefix : SINGLE_ONLY_MODEL_PREFIXES) {
            if (model.startsWith(prefix)) return true;
        }
        return false;
    }

    // ==================== 内部方法 ====================

    /**
     * 校验 seed 范围
     */
    private void validateSeed(Long seed) {
        if (seed != null && (seed < 0 || seed > MAX_SEED)) {
            throw new BadRequestException("随机数种子超出范围 [0, 2147483647]: " + seed);
        }
    }

    /**
     * 构建通用请求参数 Map
     */
    private Map<String, Object> buildCommonParameters(Boolean promptExtend, Boolean watermark,
                                                       String negativePrompt, String size,
                                                       Integer n, Long seed) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("prompt_extend", promptExtend != null ? promptExtend : true);
        parameters.put("watermark", watermark != null ? watermark : false);

        if (StringUtils.isNotBlank(negativePrompt)) {
            parameters.put("negative_prompt", negativePrompt);
        }
        if (StringUtils.isNotBlank(size)) {
            parameters.put("size", size);
        }
        if (n != null && n > 1) {
            parameters.put("n", n);
        }
        if (seed != null) {
            parameters.put("seed", seed);
        }
        return parameters;
    }

    /**
     * 调用千问文生图 API
     */
    private MultiModalConversationResult callApi(String apiKey, String model, String prompt,
                                                  Map<String, Object> parameters) throws Exception {
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Collections.singletonList(Collections.singletonMap("text", prompt)))
                .build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(Collections.singletonList(userMessage))
                .parameters(parameters)
                .build();

        MultiModalConversation conv = new MultiModalConversation();
        return conv.call(param);
    }

    /**
     * 从 API 响应中提取 content 列表
     * 响应结构: output.choices[0].message.content[].image
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractContentList(MultiModalConversationResult result) {
        if (result.getOutput() == null || result.getOutput().getChoices() == null
                || result.getOutput().getChoices().isEmpty()) {
            throw new BadRequestException("文生图响应中未找到图片内容（output/choices 为空）");
        }

        List<Map<String, Object>> contentList = result.getOutput().getChoices()
                .get(0).getMessage().getContent();

        if (contentList == null || contentList.isEmpty()) {
            throw new BadRequestException("文生图响应中未找到图片内容（content 为空）");
        }
        return contentList;
    }

    /**
     * 从单个 content 条目中提取 image URL
     */
    private String extractImageUrlFromContent(Map<String, Object> contentItem) {
        String imageUrl = (String) contentItem.get("image");
        if (StringUtils.isBlank(imageUrl)) {
            throw new BadRequestException("文生图响应项中缺少 image 字段");
        }
        return imageUrl;
    }

    /**
     * 从临时 URL 下载图片并上传到自有 OSS
     */
    private String downloadAndUploadToOss(String imageUrl) {
        byte[] imageBytes;
        try (InputStream in = new URL(imageUrl).openStream()) {
            imageBytes = IoUtil.readBytes(in);
        } catch (Exception e) {
            log.error("下载图片失败: {}", e.getMessage(), e);
            throw new BadRequestException("图片下载失败: " + e.getMessage());
        }

        String bucketName = aliOssConfig.getBucketName();
        String extension = "png";
        String fileRealName = IdUtil.simpleUUID() + "." + extension;
        String originalFilename = "image_" + System.currentTimeMillis() + "." + extension;

        StringBuilder pathBuilder = new StringBuilder();
        pathBuilder.append(OssBusinessType.IMAGE.getValue());
        String timeFolder = DateUtil.format(new Date(), aliOssConfig.getTimeformat());
        pathBuilder.append("/").append(timeFolder);
        String filePath = pathBuilder.toString() + "/" + fileRealName;

        String fileUrl = aliOssConfig.getDomain() + "/" + filePath;

        try {
            ossClient.putObject(bucketName, filePath, new ByteArrayInputStream(imageBytes));

            AliOssStorage storage = new AliOssStorage();
            storage.setFileName(originalFilename);
            storage.setFileRealName(fileRealName);
            storage.setFileSize(FileUtil.getSize(imageBytes.length));
            storage.setFileMimeType("image/png");
            storage.setFileType(extension);
            storage.setFileUrl(fileUrl);
            storage.setBucketName(bucketName);
            storage.setBusinessType(OssBusinessType.IMAGE.getValue());
            aliOssStorageRepository.save(storage);

            return fileUrl;

        } catch (Exception e) {
            log.error("上传图片到 OSS 失败: {}", e.getMessage(), e);
            throw new BadRequestException("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 保存文生图生成记录
     */
    private void saveImageRecord(String prompt, String negativePrompt, String model,
                                  String size, Integer imageCount, Boolean promptExtend,
                                  Boolean watermark, Long seed, String finalImageUrl,
                                  String requestId) {
        ImageRecord record = new ImageRecord();
        record.setPrompt(prompt);
        record.setNegativePrompt(negativePrompt);
        record.setModel(model);
        record.setSize(size);
        record.setImageCount(imageCount);
        record.setPromptExtend(promptExtend);
        record.setWatermark(watermark);
        record.setSeed(seed);
        record.setFinalImageUrl(finalImageUrl);
        record.setRequestId(requestId);
        imageRecordRepository.save(record);
    }
}
