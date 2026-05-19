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
package com.naon.grid.service.mapstruct;

import com.naon.grid.base.BaseMapper;
import com.naon.grid.domain.AliOssStorage;
import com.naon.grid.service.dto.AliOssStorageDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * 阿里云 OSS 存储 MapStruct 映射
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AliOssStorageMapper extends BaseMapper<AliOssStorageDto, AliOssStorage> {
}
