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
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.service.OssResourceService;
import com.naon.grid.service.dto.AliOssStorageDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OSS 资源查询
 * @author nano
 * @date 2026-05-30
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ossResource")
@Api(tags = "工具：OSS 资源查询")
public class OssResourceController {

    private final OssResourceService ossResourceService;

    @ApiOperation("查询 OSS 资源信息")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AliOssStorageDto> getResource(@PathVariable Long id) {
        AliOssStorageDto dto = ossResourceService.findById(id);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }
}
