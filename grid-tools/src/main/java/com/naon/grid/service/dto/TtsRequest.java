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

/**
 * TTS 语音合成请求 DTO
 * @author nano
 * @date 2025-05-19
 */
@Data
public class TtsRequest {

    @ApiModelProperty(value = "音色名称", required = true, example = "cherry")
    private String voice;

    @ApiModelProperty(value = "合成文本", required = true)
    private String text;

    @ApiModelProperty(value = "控制指令", example = "语速较快，年轻女声")
    private String instructions;

    @ApiModelProperty(value = "模型名称", example = "qwen-tts-flash")
    private String model = "qwen-tts-flash";

    @ApiModelProperty(value = "语言类型", example = "chinese")
    private String languageType;
}
