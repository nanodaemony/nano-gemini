package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.CharRadicalQueryRequest;
import com.naon.grid.backend.rest.request.CharRadicalUpdateRequest;
import com.naon.grid.backend.rest.vo.CharRadicalBaseVO;
import com.naon.grid.backend.rest.vo.CharRadicalVO;
import com.naon.grid.backend.rest.wrapper.CharRadicalWrapper;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
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
@Api(tags = "后台：汉字-部首管理")
@RequestMapping("/api/char/radical")
public class CharRadicalController {

    private final CharRadicalService charRadicalService;

    @Log("修改部首")
    @ApiOperation("修改部首")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody CharRadicalUpdateRequest request) {
        charRadicalService.update(id, CharRadicalWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("部首草稿审核通过")
    @ApiOperation("部首草稿审核通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        charRadicalService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布部首")
    @ApiOperation("发布部首（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        charRadicalService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询部首详情")
    @ApiOperation("根据ID查询部首详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<CharRadicalVO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(
                CharRadicalWrapper.toVO(charRadicalService.findById(id)), HttpStatus.OK);
    }

    @Log("查询部首列表")
    @ApiOperation("分页查询部首列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<CharRadicalBaseVO>> queryAll(
            CharRadicalQueryRequest request, Pageable pageable) {
        PageResult<CharRadicalDto> pageResult =
                charRadicalService.queryAll(CharRadicalWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(CharRadicalWrapper.toBaseVOList(pageResult.getContent()),
                        pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除部首")
    @ApiOperation("删除部首")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        charRadicalService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线部首")
    @ApiOperation("下线部首")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        charRadicalService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
