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

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import com.naon.grid.annotation.Log;
import com.naon.grid.service.TranslateService;
import com.naon.grid.service.dto.TranslateRequest;
import com.naon.grid.service.dto.TranslateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 翻译控制器
 * <p>
 * 阿里云大模型语言参考：<a href="https://help.aliyun.com/zh/model-studio/machine-translation#038d2865bbydc">...</a>
 * @author nano
 * @date 2026-05-28
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "工具：翻译管理")
@RequestMapping("/api/translate")
public class TranslateController {

    private final TranslateService translateService;

    /**
     * 进行翻译
     * @param request 请求
     * {
     *   "model": "qwen-mt-flash",
     *   "sourceText": "我喜欢吃苹果，你喜欢吗",
     *   "targetLanguage": "th"
     * }
     * @return 翻译结果
     */
    @Log("执行翻译")
    @ApiOperation("执行翻译")
    @AnonymousPostMapping
    public ResponseEntity<TranslateResponse> translate(@Validated @RequestBody TranslateRequest request) {
        return new ResponseEntity<>(translateService.translate(request), HttpStatus.OK);
    }
}
