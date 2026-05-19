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
import com.naon.grid.service.TtsService;
import com.naon.grid.service.dto.TtsRequest;
import com.naon.grid.service.dto.TtsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 阿里云 TTS 语音合成管理
 * @author nano
 * @date 2025-05-19
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tts")
@Api(tags = "工具：阿里云 TTS 语音合成")
public class TtsController {

    private final TtsService ttsService;

    @Log("语音合成")
    @ApiOperation("语音合成")
    @PostMapping("/generate")
    public ResponseEntity<TtsResponse> generate(@Validated @RequestBody TtsRequest request) {
        TtsResponse response = ttsService.generate(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
