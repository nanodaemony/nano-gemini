package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.CultureCreateRequest;
import com.naon.grid.backend.rest.request.CultureQueryRequest;
import com.naon.grid.backend.rest.vo.CultureBaseVO;
import com.naon.grid.backend.rest.vo.CultureCreateVO;
import com.naon.grid.backend.rest.vo.CultureVO;
import com.naon.grid.backend.rest.wrapper.CultureWrapper;
import com.naon.grid.backend.service.culture.CultureService;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureQueryCriteria;
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
@Api(tags = "后台：文化点管理")
@RequestMapping("/api/culture")
public class CultureController {

    private final CultureService cultureService;

    @Log("新增文化点")
    @ApiOperation("新增文化点")
    @AnonymousPostMapping
    public ResponseEntity<CultureCreateVO> create(@Valid @RequestBody CultureCreateRequest request) {
        CultureCreateVO vo = new CultureCreateVO();
        vo.setId(cultureService.create(CultureWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("修改文化点内容")
    @ApiOperation("修改文化点内容")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody CultureCreateRequest request) {
        cultureService.update(id, CultureWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("文化点草稿审核通过")
    @ApiOperation("文化点草稿审核通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        cultureService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布文化点")
    @ApiOperation("发布文化点（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        cultureService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线文化点")
    @ApiOperation("下线文化点")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        cultureService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除文化点")
    @ApiOperation("删除文化点")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cultureService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询文化点详情")
    @ApiOperation("根据ID查询文化点详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<CultureVO> findById(@PathVariable Long id) {
        CultureDto dto = cultureService.findById(id);
        return new ResponseEntity<>(CultureWrapper.toVO(dto), HttpStatus.OK);
    }

    @Log("查询文化点列表")
    @ApiOperation("分页查询文化点列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<CultureBaseVO>> queryAll(CultureQueryRequest request, Pageable pageable) {
        CultureQueryCriteria criteria = CultureWrapper.toCriteria(request);
        PageResult<CultureDto> pageResult = cultureService.queryAll(criteria, pageable);
        return new ResponseEntity<>(
                new PageResult<>(CultureWrapper.toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()),
                HttpStatus.OK);
    }
}
