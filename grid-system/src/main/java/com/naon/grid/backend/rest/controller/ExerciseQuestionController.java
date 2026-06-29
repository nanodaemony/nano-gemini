package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.ExerciseQuestionCreateRequest;
import com.naon.grid.backend.rest.request.ExerciseQuestionQueryRequest;
import com.naon.grid.backend.rest.vo.ExerciseQuestionBaseVO;
import com.naon.grid.backend.rest.vo.ExerciseQuestionCreateVO;
import com.naon.grid.backend.rest.vo.ExerciseQuestionVO;
import com.naon.grid.backend.rest.wrapper.ExerciseQuestionWrapper;
import com.naon.grid.backend.service.question.ExerciseQuestionService;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
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
@Api(tags = "后台：练习题目管理")
@RequestMapping("/api/exercise-question")
public class ExerciseQuestionController {

    private final ExerciseQuestionService exerciseQuestionService;

    @Log("新增题目")
    @ApiOperation("新增题目")
    @AnonymousPostMapping
    public ResponseEntity<ExerciseQuestionCreateVO> create(@Valid @RequestBody ExerciseQuestionCreateRequest request) {
        ExerciseQuestionCreateVO vo = new ExerciseQuestionCreateVO();
        vo.setId(exerciseQuestionService.create(ExerciseQuestionWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("修改题目内容")
    @ApiOperation("修改题目内容")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody ExerciseQuestionCreateRequest request) {
        exerciseQuestionService.update(id, ExerciseQuestionWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("题目草稿审核通过")
    @ApiOperation("题目草稿审核通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        exerciseQuestionService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布题目")
    @ApiOperation("发布题目（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        exerciseQuestionService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询题目详情")
    @ApiOperation("根据ID查询题目详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<ExerciseQuestionVO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(ExerciseQuestionWrapper.toVO(exerciseQuestionService.findById(id)), HttpStatus.OK);
    }

    @Log("查询题目列表")
    @ApiOperation("分页查询题目列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<ExerciseQuestionBaseVO>> queryAll(ExerciseQuestionQueryRequest request, Pageable pageable) {
        PageResult<ExerciseQuestionDto> pageResult = exerciseQuestionService.queryAll(
                ExerciseQuestionWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(ExerciseQuestionWrapper.toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()),
                HttpStatus.OK);
    }

    @Log("删除题目")
    @ApiOperation("删除题目")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        exerciseQuestionService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线题目")
    @ApiOperation("下线题目")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        exerciseQuestionService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
