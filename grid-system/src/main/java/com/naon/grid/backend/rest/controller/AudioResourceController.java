package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.rest.request.AudioResourceQueryRequest;
import com.naon.grid.backend.rest.vo.AudioResourceVO;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceQueryCriteria;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Api(tags = "词汇：音频资源管理")
@RequestMapping("/api/audio-resource")
public class AudioResourceController {

    private final AudioResourceService audioResourceService;

    @Log("查询音频资源列表")
    @ApiOperation("分页查询音频资源列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<AudioResourceVO>> queryAll(AudioResourceQueryRequest request, Pageable pageable) {
        PageResult<AudioResourceDto> pageResult = audioResourceService.queryAll(toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(toVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("查询音频资源详情")
    @ApiOperation("根据ID查询音频资源详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AudioResourceVO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(toVO(audioResourceService.findById(id)), HttpStatus.OK);
    }

    private AudioResourceQueryCriteria toCriteria(AudioResourceQueryRequest request) {
        AudioResourceQueryCriteria criteria = new AudioResourceQueryCriteria();
        criteria.setBizType(request.getBizType());
        return criteria;
    }

    private List<AudioResourceVO> toVOList(List<AudioResourceDto> resources) {
        return resources.stream().map(this::toVO).collect(Collectors.toList());
    }

    private AudioResourceVO toVO(AudioResourceDto dto) {
        AudioResourceVO vo = new AudioResourceVO();
        vo.setId(dto.getId());
        vo.setBizType(dto.getBizType());
        vo.setTextContent(dto.getTextContent());
        vo.setSourceType(dto.getSourceType());
        vo.setFileUrl(dto.getFileUrl());
        vo.setFileFormat(dto.getFileFormat());
        vo.setFileSize(dto.getFileSize());
        vo.setTtsRecordId(dto.getTtsRecordId());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
}
