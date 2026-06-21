package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.AudioResourceCreateRequest;
import com.naon.grid.backend.rest.request.AudioResourceQueryRequest;
import com.naon.grid.backend.rest.vo.AudioResourceVO;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceQueryCriteria;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 音频资源包装器
 */
public class AudioResourceWrapper {

    public static AudioResourceDto toDto(AudioResourceCreateRequest request) {
        AudioResourceDto dto = new AudioResourceDto();
        dto.setTextContent(request.getTextContent());
        dto.setSourceType(request.getSourceType());
        dto.setFileUrl(request.getFileUrl());
        dto.setFileFormat(request.getFileFormat());
        dto.setFileSize(request.getFileSize());
        return dto;
    }

    public static AudioResourceQueryCriteria toCriteria(AudioResourceQueryRequest request) {
        AudioResourceQueryCriteria criteria = new AudioResourceQueryCriteria();
        criteria.setSourceType(request.getSourceType() != null ? request.getSourceType().getCode() : null);
        return criteria;
    }

    public static List<AudioResourceVO> toVOList(List<AudioResourceDto> resources) {
        return resources.stream().map(AudioResourceWrapper::toVO).collect(Collectors.toList());
    }

    public static AudioResourceVO toVO(AudioResourceDto dto) {
        AudioResourceVO vo = new AudioResourceVO();
        vo.setId(dto.getId());
        vo.setTextContent(dto.getTextContent());
        vo.setSourceType(dto.getSourceType());
        vo.setFileUrl(dto.getFileUrl());
        vo.setFileFormat(dto.getFileFormat());
        vo.setFileSize(dto.getFileSize());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
}
