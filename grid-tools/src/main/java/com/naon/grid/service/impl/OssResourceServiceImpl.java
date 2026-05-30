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
package com.naon.grid.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.OssResourceService;
import com.naon.grid.service.dto.AliOssStorageDto;
import org.springframework.stereotype.Service;

/**
 * OSS 资源查询 Service 实现
 * @author nano
 * @date 2026-05-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssResourceServiceImpl implements OssResourceService {

    private final AliOssStorageService aliOssStorageService;

    @Override
    public AliOssStorageDto findById(Long id) {
        return aliOssStorageService.findById(id);
    }
}
