package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.DailyVocabularyCreateRequest;
import com.naon.grid.backend.rest.request.DailyVocabularyQueryRequest;
import com.naon.grid.backend.rest.vo.DailyVocabularyBaseVO;
import com.naon.grid.backend.rest.vo.DailyVocabularyCreateVO;
import com.naon.grid.backend.rest.vo.DailyVocabularyVO;
import com.naon.grid.backend.rest.wrapper.DailyVocabularyWrapper;
import com.naon.grid.backend.service.vocabulary.DailyVocabularyService;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：每日一词管理")
@RequestMapping("/api/daily-vocabulary")
public class DailyVocabularyController {

    private final DailyVocabularyService dailyVocabularyService;
    private final AiContentMarkerService aiContentMarkerService;

    @Log("新增每日一词")
    @ApiOperation("新增每日一词")
    @AnonymousPostMapping
    public ResponseEntity<DailyVocabularyCreateVO> create(@Valid @RequestBody DailyVocabularyCreateRequest request) {
        DailyVocabularyCreateVO vo = new DailyVocabularyCreateVO();
        vo.setId(dailyVocabularyService.create(DailyVocabularyWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("编辑每日一词")
    @ApiOperation("编辑每日一词内容")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody DailyVocabularyCreateRequest request) {
        dailyVocabularyService.update(id, DailyVocabularyWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询每日一词详情")
    @ApiOperation("根据ID查询每日一词详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<DailyVocabularyVO> findById(@PathVariable Integer id) {
        DailyVocabularyDto dto = dailyVocabularyService.findById(id);
        List<String> keys = AiContentMarkerHelper.collectOne("daily_vocabulary", dto.getId());
        Map<String, MarkerFields> aiMarkers = aiContentMarkerService.batchQuery(keys);
        return new ResponseEntity<>(DailyVocabularyWrapper.toVO(dto, aiMarkers), HttpStatus.OK);
    }

    @Log("查询每日一词列表")
    @ApiOperation("分页查询每日一词列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<DailyVocabularyBaseVO>> queryAll(
            DailyVocabularyQueryRequest request, Pageable pageable) {
        PageResult<DailyVocabularyDto> pageResult = dailyVocabularyService.queryAll(
                DailyVocabularyWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(DailyVocabularyWrapper.toBaseVOList(pageResult.getContent()),
                        pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("审核每日一词")
    @ApiOperation("审核通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Integer id) {
        dailyVocabularyService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布每日一词")
    @ApiOperation("发布每日一词（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable Integer id) {
        dailyVocabularyService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线每日一词")
    @ApiOperation("下线每日一词")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Integer id) {
        dailyVocabularyService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除每日一词")
    @ApiOperation("删除每日一词")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        dailyVocabularyService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("设置展示日期")
    @ApiOperation("设置每日一词展示日期")
    @AnonymousPutMapping("/{id}/schedule")
    public ResponseEntity<Void> schedule(@PathVariable Integer id,
                                         @RequestParam LocalDate date) {
        dailyVocabularyService.schedule(id, date);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("批量排期")
    @ApiOperation("批量设置每日一词展示日期")
    @AnonymousPostMapping("/batch-schedule")
    public ResponseEntity<Void> batchSchedule(@RequestParam List<Integer> ids,
                                              @RequestParam List<LocalDate> dates) {
        dailyVocabularyService.batchSchedule(ids, dates);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
