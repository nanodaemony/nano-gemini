package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.backend.rest.request.AudioResourceCreateRequest;
import com.naon.grid.backend.rest.request.AudioResourceQueryRequest;
import com.naon.grid.backend.rest.vo.AudioResourceCreateVO;
import com.naon.grid.backend.rest.vo.AudioResourceVO;
import com.naon.grid.backend.rest.wrapper.AudioResourceWrapper;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：音频-音频资源管理")
@RequestMapping("/api/audio-resource")
public class AudioResourceController {

    private final AudioResourceService audioResourceService;

    @Log("创建音频资源")
    @ApiOperation("创建音频资源")
    @AnonymousPostMapping
    public ResponseEntity<AudioResourceCreateVO> create(@Valid @RequestBody AudioResourceCreateRequest request) {
        AudioResourceCreateVO vo = new AudioResourceCreateVO();
        vo.setId(audioResourceService.create(AudioResourceWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("查询音频资源详情")
    @ApiOperation("根据ID查询音频资源详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AudioResourceVO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(AudioResourceWrapper.toVO(audioResourceService.findById(id)), HttpStatus.OK);
    }

    @Log("查询音频资源列表")
    @ApiOperation("分页查询音频资源列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<AudioResourceVO>> queryAll(AudioResourceQueryRequest request, Pageable pageable) {
        PageResult<AudioResourceDto> pageResult = audioResourceService.queryAll(AudioResourceWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(AudioResourceWrapper.toVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除音频资源")
    @ApiOperation("删除音频资源（软删除）")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        audioResourceService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
