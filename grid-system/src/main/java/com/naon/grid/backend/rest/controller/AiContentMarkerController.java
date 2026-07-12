package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.modules.system.service.AiContentMarkerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：AI内容标记管理")
@RequestMapping("/api/ai-content-markers")
public class AiContentMarkerController {

    private final AiContentMarkerService aiContentMarkerService;

    @ApiOperation("设置字段审核状态")
    @AnonymousPutMapping("/{entityType}/{entityId}/review")
    public ResponseEntity<Void> reviewField(@PathVariable String entityType,
                                            @PathVariable Long entityId,
                                            @RequestBody ReviewRequest request) {
        aiContentMarkerService.reviewField(entityType, entityId,
                request.getFieldName(), Boolean.TRUE.equals(request.getReviewed()));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Data
    public static class ReviewRequest {
        @NotBlank
        private String fieldName;
        @NotNull
        private Boolean reviewed;
    }
}
