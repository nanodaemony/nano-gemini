package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.GrammarPointCreateRequest;
import com.naon.grid.backend.rest.request.GrammarPointQueryRequest;
import com.naon.grid.backend.rest.vo.GrammarPointBaseVO;
import com.naon.grid.backend.rest.vo.GrammarPointCreateVO;
import com.naon.grid.backend.rest.vo.GrammarPointVO;
import com.naon.grid.backend.rest.wrapper.GrammarPointWrapper;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarErrorDto;
import com.naon.grid.backend.service.grammar.dto.GrammarMeaningDto;
import com.naon.grid.backend.service.grammar.dto.GrammarNoticeDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarStructureDto;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;
import com.naon.grid.modules.system.service.AiContentMarkerService;
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
import java.util.stream.Collectors;

import static com.naon.grid.backend.rest.wrapper.GrammarPointWrapper.toBaseVOList;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：语法-语法点管理")
@RequestMapping("/api/grammar")
public class GrammarPointController {

    private final GrammarPointService grammarPointService;

    private final AiContentMarkerService aiContentMarkerService;

    @Log("新增语法点")
    @ApiOperation("新增语法点")
    @AnonymousPostMapping
    public ResponseEntity<GrammarPointCreateVO> create(@Valid @RequestBody GrammarPointCreateRequest request) {
        GrammarPointCreateVO vo = new GrammarPointCreateVO();
        vo.setId(grammarPointService.create(GrammarPointWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("修改语法点内容")
    @ApiOperation("修改语法点内容")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody GrammarPointCreateRequest request) {
        grammarPointService.update(id, GrammarPointWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("语法点草稿审核通过")
    @ApiOperation("语法点草稿审核通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        grammarPointService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布语法点")
    @ApiOperation("发布语法点（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        grammarPointService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询语法点详情")
    @ApiOperation("根据ID查询语法点详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<GrammarPointVO> findById(@PathVariable Long id) {
        GrammarPointDto dto = grammarPointService.findById(id);
        List<String> entityKeys = collectGrammarEntityKeys(dto);
        Map<String, List<String>> aiMarkers = aiContentMarkerService.batchQuery(entityKeys);
        return new ResponseEntity<>(GrammarPointWrapper.toVO(dto, aiMarkers), HttpStatus.OK);
    }

    /** 从 GrammarPointDto 树中收集所有子实体的 entity key */
    private List<String> collectGrammarEntityKeys(GrammarPointDto dto) {
        List<String> keys = new ArrayList<>();
        if (dto.getMeanings() != null) {
            for (GrammarMeaningDto m : dto.getMeanings()) {
                keys.addAll(AiContentMarkerHelper.collectOne("grammar_meaning", m.getId()));
                if (m.getSentences() != null) {
                    keys.addAll(AiContentMarkerHelper.collect("example_sentence",
                            m.getSentences().stream()
                                    .map(ExampleSentenceDto::getId)
                                    .collect(Collectors.toList())));
                }
            }
        }
        if (dto.getStructures() != null) {
            for (GrammarStructureDto s : dto.getStructures()) {
                keys.addAll(AiContentMarkerHelper.collectOne("grammar_structure", s.getId()));
                if (s.getSentences() != null) {
                    keys.addAll(AiContentMarkerHelper.collect("example_sentence",
                            s.getSentences().stream()
                                    .map(ExampleSentenceDto::getId)
                                    .collect(Collectors.toList())));
                }
            }
        }
        if (dto.getNotices() != null) {
            for (GrammarNoticeDto n : dto.getNotices()) {
                keys.addAll(AiContentMarkerHelper.collectOne("grammar_notice", n.getId()));
                if (n.getSentences() != null) {
                    keys.addAll(AiContentMarkerHelper.collect("example_sentence",
                            n.getSentences().stream()
                                    .map(ExampleSentenceDto::getId)
                                    .collect(Collectors.toList())));
                }
            }
        }
        if (dto.getErrors() != null) {
            for (GrammarErrorDto e : dto.getErrors()) {
                keys.addAll(AiContentMarkerHelper.collectOne("grammar_error", e.getId()));
            }
        }
        return keys;
    }

    @Log("查询语法点列表")
    @ApiOperation("分页查询语法点列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<GrammarPointBaseVO>> queryAll(GrammarPointQueryRequest request, Pageable pageable) {
        PageResult<GrammarPointDto> pageResult = grammarPointService.queryAll(GrammarPointWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除语法点")
    @ApiOperation("删除语法点")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        grammarPointService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线语法点")
    @ApiOperation("下线语法点")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        grammarPointService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
