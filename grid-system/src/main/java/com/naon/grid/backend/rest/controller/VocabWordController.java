package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.VocabWordCreateRequest;
import com.naon.grid.backend.rest.request.VocabWordQueryRequest;
import com.naon.grid.backend.rest.vo.VocabOutlineRecordVO;
import com.naon.grid.backend.rest.vo.VocabWordBaseVO;
import com.naon.grid.backend.rest.vo.VocabWordCreateVO;
import com.naon.grid.backend.rest.vo.VocabWordVO;
import com.naon.grid.backend.rest.wrapper.VocabWordWrapper;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordQueryCriteria;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabOutlineRecordMapper;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：词汇-词汇管理")
@RequestMapping("/api/vocabulary")
public class VocabWordController {

    private final VocabWordService vocabWordService;

    private final VocabOutlineRecordService vocabOutlineRecordService;

    private final VocabOutlineRecordMapper vocabOutlineRecordMapper;

    @Log("新增词汇")
    @ApiOperation("新增词汇")
    @AnonymousPostMapping
    public ResponseEntity<VocabWordCreateVO> create(@Valid @RequestBody VocabWordCreateRequest request) {
        VocabWordCreateVO vo = new VocabWordCreateVO();
        vo.setId(vocabWordService.create(VocabWordWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("更新词汇内容")
    @ApiOperation("更新词汇内容")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @Valid @RequestBody VocabWordCreateRequest request) {
        vocabWordService.update(id, VocabWordWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("词汇草稿通过")
    @ApiOperation("词汇草稿通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Integer id) {
        vocabWordService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布词汇")
    @ApiOperation("发布词汇（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishVocab(@PathVariable Integer id) {
        vocabWordService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询词汇详情")
    @ApiOperation("根据ID查询词汇详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<VocabWordVO> findById(@PathVariable Integer id) {
        return new ResponseEntity<>(VocabWordWrapper.toVO(vocabWordService.findById(id)), HttpStatus.OK);
    }

    @Log("查询词汇列表")
    @ApiOperation("分页查询词汇列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<VocabWordBaseVO>> queryAll(VocabWordQueryRequest request, Pageable pageable) {
        PageResult<VocabWordDto> pageResult = vocabWordService.queryAll(VocabWordWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(VocabWordWrapper.toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除词汇")
    @ApiOperation("删除词汇")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        vocabWordService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线词汇")
    @ApiOperation("下线词汇")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Integer id) {
        vocabWordService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询纲外词列表")
    @ApiOperation("分页查询纲外词列表")
    @AnonymousGetMapping("/outline")
    public ResponseEntity<PageResult<VocabOutlineRecordVO>> queryOutline(
            VocabOutlineRecordQueryCriteria criteria,
            Pageable pageable) {
        // 默认按搜索次数降序、创建时间降序
        if (pageable.getSort().isEmpty()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "searchCount")
                            .and(Sort.by(Sort.Direction.DESC, "createTime"))
            );
        }
        PageResult<VocabOutlineRecordDto> pageResult = vocabOutlineRecordService.queryAll(criteria, pageable);
        List<VocabOutlineRecordVO> vos = pageResult.getContent().stream()
                .map(vocabOutlineRecordMapper::toVo)
                .collect(Collectors.toList());
        return new ResponseEntity<>(new PageResult<>(vos, pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("标记纲外词已处理")
    @ApiOperation("标记纲外词为已处理")
    @AnonymousPutMapping("/outline/{id}/complete")
    public ResponseEntity<Void> completeOutline(@PathVariable Integer id) {
        vocabOutlineRecordService.markAsCompleted(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
