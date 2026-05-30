/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.naon.grid.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.service.ImageService;
import com.naon.grid.service.dto.QwenImageBatchRequest;
import com.naon.grid.service.dto.QwenImageBatchResponse;
import com.naon.grid.service.dto.QwenImageRequest;
import com.naon.grid.service.dto.QwenImageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 阿里云 千问文生图（Qwen-Image）管理
 * @author nano
 * @date 2026-05-30
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/image")
@Api(tags = "工具：阿里云千问文生图（Qwen-Image）")
public class AliYunImageController {

    private final ImageService imageService;

    @Log("文生图生成")
    @ApiOperation("文生图生成（千问 Qwen-Image，单图）")
    @AnonymousPostMapping("/generate")
    public ResponseEntity<QwenImageResponse> generate(@Validated @RequestBody QwenImageRequest request) {
        QwenImageResponse response = imageService.generate(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Log("文生图批量生成")
    @ApiOperation("文生图批量生成（千问 Qwen-Image，多图）")
    @AnonymousPostMapping("/generateBatch")
    public ResponseEntity<QwenImageBatchResponse> generateBatch(@Validated @RequestBody QwenImageBatchRequest request) {
        QwenImageBatchResponse response = imageService.generateBatch(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
