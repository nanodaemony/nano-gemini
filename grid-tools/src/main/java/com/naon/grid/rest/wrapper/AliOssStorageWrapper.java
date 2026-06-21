package com.naon.grid.rest.wrapper;

import com.naon.grid.rest.vo.AliOssStorageVO;
import com.naon.grid.service.dto.AliOssStorageDto;

/**
 * 阿里云OSS存储包装器
 *
 * @author Zheng Jie
 */
public class AliOssStorageWrapper {

    public static AliOssStorageVO toVO(AliOssStorageDto dto) {
        if (dto == null) {
            return null;
        }
        AliOssStorageVO vo = new AliOssStorageVO();
        vo.setId(dto.getId());
        vo.setFileName(dto.getFileName());
        vo.setFileRealName(dto.getFileRealName());
        vo.setFileSize(dto.getFileSize());
        vo.setFileMimeType(dto.getFileMimeType());
        vo.setFileType(dto.getFileType());
        vo.setFileUrl(dto.getFileUrl());
        vo.setBucketName(dto.getBucketName());
        vo.setBusinessType(dto.getBusinessType());
        vo.setCustomPath(dto.getCustomPath());
        return vo;
    }
}
