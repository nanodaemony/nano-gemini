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
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 翻译请求 DTO
 * @author nano
 * @date 2026-05-28
 */
@Data
public class TranslateRequest {

    @NotBlank(message = "源文本不能为空")
    @ApiModelProperty(value = "源文本（中文）", required = true)
    private String sourceText;

    @NotBlank(message = "目标语言不能为空")
    @ApiModelProperty(value = "目标语言代码", required = true)
    private String targetLanguage;

    @ApiModelProperty(value = "模型名称")
    private String model = "qwen-plus";
}
