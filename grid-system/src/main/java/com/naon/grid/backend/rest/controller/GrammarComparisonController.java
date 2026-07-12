package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.GrammarComparisonGroupCreateRequest;
import com.naon.grid.backend.rest.request.GrammarComparisonGroupQueryRequest;
import com.naon.grid.backend.rest.vo.GrammarComparisonGroupBaseVO;
import com.naon.grid.backend.rest.vo.GrammarComparisonGroupCreateVO;
import com.naon.grid.backend.rest.vo.GrammarComparisonGroupVO;
import com.naon.grid.backend.rest.wrapper.GrammarComparisonGroupWrapper;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonChatDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonItemDto;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;
import com.naon.grid.modules.system.service.AiContentMarkerService;
import com.naon.grid.modules.system.service.AiContentMarkerService.MarkerFields;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：语法-语法辨析")
@RequestMapping("/api/grammar/comparison")
public class GrammarComparisonController {

    private final GrammarComparisonGroupService grammarComparisonGroupService;

    private final AiContentMarkerService aiContentMarkerService;

    @Log("新增语法辨析组")
    @ApiOperation("新增语法辨析组")
    @AnonymousPostMapping
    public ResponseEntity<GrammarComparisonGroupCreateVO> create(@Valid @RequestBody GrammarComparisonGroupCreateRequest request) {
        GrammarComparisonGroupCreateVO vo = new GrammarComparisonGroupCreateVO();
        vo.setId(grammarComparisonGroupService.create(GrammarComparisonGroupWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("更新语法辨析组")
    @ApiOperation("更新语法辨析组")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody GrammarComparisonGroupCreateRequest request) {
        grammarComparisonGroupService.update(id, GrammarComparisonGroupWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("审核语法辨析组")
    @ApiOperation("语法辨析组草稿通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        grammarComparisonGroupService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布语法辨析组")
    @ApiOperation("发布语法辨析组（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        grammarComparisonGroupService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询语法辨析组详情")
    @ApiOperation("根据ID查询语法辨析组详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<GrammarComparisonGroupVO> findById(@PathVariable Long id) {
        GrammarComparisonGroupDto dto = grammarComparisonGroupService.findById(id);
        List<String> entityKeys = collectGrammarComparisonEntityKeys(dto);
        Map<String, MarkerFields> aiMarkers = aiContentMarkerService.batchQuery(entityKeys);
        return new ResponseEntity<>(
                GrammarComparisonGroupWrapper.toVO(dto, aiMarkers), HttpStatus.OK);
    }

    /** 从 GrammarComparisonGroupDto 树中收集所有子实体的 entity key */
    private List<String> collectGrammarComparisonEntityKeys(GrammarComparisonGroupDto dto) {
        List<String> keys = new ArrayList<>();
        if (dto.getItems() != null) {
            for (GrammarComparisonItemDto item : dto.getItems()) {
                keys.addAll(AiContentMarkerHelper.collectOne("grammar_comparison_item", item.getId()));
            }
        }
        if (dto.getChats() != null) {
            for (GrammarComparisonChatDto chat : dto.getChats()) {
                keys.addAll(AiContentMarkerHelper.collectOne("grammar_comparison_chat", chat.getId()));
            }
        }
        return keys;
    }

    @Log("查询语法辨析组列表")
    @ApiOperation("分页查询语法辨析组列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<GrammarComparisonGroupBaseVO>> queryAll(
            GrammarComparisonGroupQueryRequest request, Pageable pageable) {
        PageResult<GrammarComparisonGroupDto> pageResult =
                grammarComparisonGroupService.queryAll(GrammarComparisonGroupWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(GrammarComparisonGroupWrapper.toBaseVOList(pageResult.getContent()),
                        pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除语法辨析组")
    @ApiOperation("删除语法辨析组")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        grammarComparisonGroupService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线语法辨析组")
    @ApiOperation("下线语法辨析组")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        grammarComparisonGroupService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
