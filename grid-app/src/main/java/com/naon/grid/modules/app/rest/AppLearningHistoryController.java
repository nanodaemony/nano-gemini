package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.Log;
import com.naon.grid.modules.app.rest.request.AddLearningHistoryRequest;
import com.naon.grid.modules.app.rest.vo.LearningHistoryItemVO;
import com.naon.grid.modules.app.service.LearningHistoryService;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/learning-history")
@Api(tags = "用户：学习记录接口")
public class AppLearningHistoryController {

    private final LearningHistoryService learningHistoryService;

    @Log("添加学习记录")
    @ApiOperation("添加或更新学习记录（重复学习自动提序）")
    @PostMapping
    public ResponseEntity<Void> addRecord(
            @Validated @RequestBody AddLearningHistoryRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        learningHistoryService.addRecord(userId, request.getBizType(), request.getContentId());
        return ResponseEntity.ok().build();
    }

    @ApiOperation("查询最近学习记录（最多50条，按时间倒序）")
    @GetMapping
    public ResponseEntity<List<LearningHistoryItemVO>> getHistory() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(learningHistoryService.getHistory(userId));
    }

    @Log("删除学习记录")
    @ApiOperation("删除单条学习记录")
    @DeleteMapping("/{bizType}/{contentId}")
    public ResponseEntity<Void> removeRecord(
            @PathVariable String bizType,
            @PathVariable Long contentId) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        learningHistoryService.removeRecord(userId, bizType, contentId);
        return ResponseEntity.ok().build();
    }

    @Log("清空学习记录")
    @ApiOperation("清空所有学习记录")
    @DeleteMapping
    public ResponseEntity<Void> clearAll() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        learningHistoryService.clearAll(userId);
        return ResponseEntity.ok().build();
    }
}
