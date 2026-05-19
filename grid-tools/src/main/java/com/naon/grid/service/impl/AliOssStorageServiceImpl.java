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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.aliyun.oss.OSS;
import com.naon.grid.config.AliOssConfig;
import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.repository.AliOssStorageRepository;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.service.dto.AliOssStorageQueryCriteria;
import com.naon.grid.service.mapstruct.AliOssStorageMapper;
import com.naon.grid.utils.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * 阿里云 OSS 存储 Service 实现
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliOssStorageServiceImpl implements AliOssStorageService {

    private final OSS ossClient;
    private final AliOssConfig aliOssConfig;
    private final AliOssStorageRepository aliOssStorageRepository;
    private final AliOssStorageMapper aliOssStorageMapper;

    @Override
    public PageResult<AliOssStorageDto> queryAll(AliOssStorageQueryCriteria criteria, Pageable pageable) {
        Page<AliOssStorage> page = aliOssStorageRepository.findAll((root, criteriaQuery, criteriaBuilder)
                -> QueryHelp.getPredicate(root, criteria, criteriaBuilder), pageable);
        return PageUtil.toPage(page.map(aliOssStorageMapper::toDto));
    }

    @Override
    public List<AliOssStorageDto> queryAll(AliOssStorageQueryCriteria criteria) {
        return aliOssStorageMapper.toDto(aliOssStorageRepository.findAll((root, criteriaQuery, criteriaBuilder)
                -> QueryHelp.getPredicate(root, criteria, criteriaBuilder)));
    }

    @Override
    public AliOssStorageDto findById(Long id) {
        AliOssStorage storage = aliOssStorageRepository.findById(id).orElseGet(AliOssStorage::new);
        ValidationUtil.isNull(storage.getId(), "AliOssStorage", "id", id);
        return aliOssStorageMapper.toDto(storage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AliOssStorage upload(MultipartFile file) {
        String bucketName = aliOssConfig.getBucketName();
        // 检查存储桶是否存在，不存在则创建
        if (!bucketExists(bucketName)) {
            log.warn("存储桶 {} 不存在，尝试创建...", bucketName);
            createBucket(bucketName);
            log.info("存储桶 {} 创建成功。", bucketName);
        }
        // 获取原始文件名
        String originalName = file.getOriginalFilename();
        if (StringUtils.isBlank(originalName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        // 生成存储路径和文件名（UUID）
        String folder = DateUtil.format(new Date(), aliOssConfig.getTimeformat());
        String extension = FileUtil.getExtensionName(originalName);
        String fileRealName = IdUtil.simpleUUID() + "." + extension;
        String filePath = folder + "/" + fileRealName;
        // 构建访问 URL
        String fileUrl = aliOssConfig.getDomain() + "/" + filePath;

        AliOssStorage storage = new AliOssStorage();
        try {
            // 上传文件到 OSS
            ossClient.putObject(bucketName, filePath, file.getInputStream());

            // 设置存储对象属性
            storage.setFileName(originalName);
            storage.setFileRealName(fileRealName);
            storage.setFileSize(FileUtil.getSize(file.getSize()));
            storage.setFileMimeType(FileUtil.getMimeType(originalName));
            storage.setFileType(extension);
            storage.setFileUrl(fileUrl);
            storage.setBucketName(bucketName);

            // 保存到数据库
            aliOssStorageRepository.save(storage);
        } catch (IOException e) {
            log.error("上传文件到 OSS 失败: {}", e.getMessage(), e);
            throw new BadRequestException("文件上传失败: " + e.getMessage());
        }
        return storage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AliOssStorage resources) {
        AliOssStorage storage = aliOssStorageRepository.findById(resources.getId()).orElseGet(AliOssStorage::new);
        ValidationUtil.isNull(storage.getId(), "AliOssStorage", "id", resources.getId());
        storage.copy(resources);
        aliOssStorageRepository.save(storage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(List<Long> ids) {
        String bucketName = aliOssConfig.getBucketName();
        if (!bucketExists(bucketName)) {
            throw new BadRequestException("存储桶不存在，请检查配置或权限。");
        }
        for (Long id : ids) {
            String fileRealName = aliOssStorageRepository.selectFileRealNameById(id);
            String fileUrl = aliOssStorageRepository.selectFileUrlById(id);
            if (fileUrl == null) {
                log.warn("未找到 ID 为 {} 的文件记录，跳过删除", id);
                continue;
            }
            // 从 URL 中解析出 OSS 的 object key
            String objectKey = extractObjectKeyFromUrl(fileUrl, aliOssConfig.getDomain());
            try {
                // 从 OSS 删除
                ossClient.deleteObject(bucketName, objectKey);
                // 从数据库删除
                aliOssStorageRepository.deleteById(id);
            } catch (Exception e) {
                log.error("从 OSS 删除文件时出错: {}", e.getMessage(), e);
                throw new BadRequestException("删除文件失败: " + e.getMessage());
            }
        }
    }

    @Override
    public void download(List<AliOssStorageDto> all, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AliOssStorageDto storage : all) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("原始文件名", storage.getFileName());
            map.put("存储文件名", storage.getFileRealName());
            map.put("文件大小", storage.getFileSize());
            map.put("文件 MIME 类型", storage.getFileMimeType());
            map.put("文件类型", storage.getFileType());
            map.put("访问 URL", storage.getFileUrl());
            map.put("存储桶", storage.getBucketName());
            map.put("创建者", storage.getCreateBy());
            map.put("创建时间", storage.getCreateTime());
            map.put("更新者", storage.getUpdateBy());
            map.put("更新时间", storage.getUpdateTime());
            list.add(map);
        }
        FileUtil.downloadExcel(list, response);
    }

    /**
     * 检查存储桶是否存在
     */
    private boolean bucketExists(String bucketName) {
        try {
            return ossClient.doesBucketExist(bucketName);
        } catch (Exception e) {
            log.error("检查 OSS 存储桶时出错: {}", e.getMessage(), e);
            throw new BadRequestException("检查存储桶失败: " + e.getMessage());
        }
    }

    /**
     * 创建存储桶
     */
    private void createBucket(String bucketName) {
        try {
            ossClient.createBucket(bucketName);
        } catch (Exception e) {
            log.error("创建 OSS 存储桶时出错: {}", e.getMessage(), e);
            throw new BadRequestException("创建存储桶失败: " + e.getMessage());
        }
    }

    /**
     * 从完整 URL 中提取 OSS object key
     * 例如: https://domain.com/folder/filename -> folder/filename
     */
    private String extractObjectKeyFromUrl(String fileUrl, String domain) {
        if (fileUrl.startsWith(domain)) {
            return fileUrl.substring(domain.length() + 1); // +1 for the "/"
        }
        // 如果 domain 不包含协议，也要处理
        if (domain.startsWith("http://") || domain.startsWith("https://")) {
            String domainWithoutProtocol = domain.replaceFirst("https?://", "");
            if (fileUrl.contains(domainWithoutProtocol)) {
                int index = fileUrl.indexOf(domainWithoutProtocol) + domainWithoutProtocol.length();
                String keyPart = fileUrl.substring(index);
                if (keyPart.startsWith("/")) {
                    keyPart = keyPart.substring(1);
                }
                return keyPart;
            }
        }
        // 兜底方案
        int protocolIndex = fileUrl.indexOf("://");
        if (protocolIndex != -1) {
            String afterProtocol = fileUrl.substring(protocolIndex + 3);
            int firstSlashIndex = afterProtocol.indexOf("/");
            if (firstSlashIndex != -1) {
                return afterProtocol.substring(firstSlashIndex + 1);
            }
        }
        log.warn("无法从 URL {} 中提取 object key，尝试直接解析", fileUrl);
        return fileUrl;
    }
}
