package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.VocabComparisonGroupCreateRequest;
import com.naon.grid.backend.rest.request.VocabComparisonGroupQueryRequest;
import com.naon.grid.backend.rest.vo.VocabComparisonGroupBaseVO;
import com.naon.grid.backend.rest.vo.VocabComparisonGroupCreateVO;
import com.naon.grid.backend.rest.vo.VocabComparisonGroupVO;
import com.naon.grid.backend.rest.wrapper.VocabComparisonGroupWrapper;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
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
@Api(tags = "后台：词汇-词汇辨析")
@RequestMapping("/api/vocab/comparison")
public class VocabComparisonController {

    private final VocabComparisonGroupService vocabComparisonGroupService;

    @Log("新增辨析组")
    @ApiOperation("新增辨析组")
    @AnonymousPostMapping
    public ResponseEntity<VocabComparisonGroupCreateVO> create(@Valid @RequestBody VocabComparisonGroupCreateRequest request) {
        VocabComparisonGroupCreateVO vo = new VocabComparisonGroupCreateVO();
        vo.setId(vocabComparisonGroupService.create(VocabComparisonGroupWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("更新辨析组")
    @ApiOperation("更新辨析组")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody VocabComparisonGroupCreateRequest request) {
        vocabComparisonGroupService.update(id, VocabComparisonGroupWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("审核辨析组")
    @ApiOperation("辨析组草稿通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        vocabComparisonGroupService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布辨析组")
    @ApiOperation("发布辨析组（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        vocabComparisonGroupService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询辨析组详情")
    @ApiOperation("根据ID查询辨析组详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<VocabComparisonGroupVO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(
                VocabComparisonGroupWrapper.toVO(vocabComparisonGroupService.findById(id)), HttpStatus.OK);
    }

    @Log("查询辨析组列表")
    @ApiOperation("分页查询辨析组列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<VocabComparisonGroupBaseVO>> queryAll(
            VocabComparisonGroupQueryRequest request, Pageable pageable) {
        PageResult<VocabComparisonGroupDto> pageResult =
                vocabComparisonGroupService.queryAll(VocabComparisonGroupWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(VocabComparisonGroupWrapper.toBaseVOList(pageResult.getContent()),
                        pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除辨析组")
    @ApiOperation("删除辨析组")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vocabComparisonGroupService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线辨析组")
    @ApiOperation("下线辨析组")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        vocabComparisonGroupService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
