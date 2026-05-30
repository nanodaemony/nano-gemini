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
package com.naon.grid.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片上传至 OSS 后的结果包装对象
 * @author nano
 * @date 2026-05-30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OssImageResult {

    @ApiModelProperty(value = "OSS 存储记录 ID")
    private Long imageId;

    @ApiModelProperty(value = "生成图像的 OSS 永久 URL")
    private String imageUrl;
}
