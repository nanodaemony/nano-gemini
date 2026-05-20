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

import java.util.List;

/**
 * 火山引擎 TTS 语音合成请求 DTO
 * @author nano
 * @date 2026-05-20
 */
@Data
public class VolcengineTtsRequest {

    @ApiModelProperty(value = "合成文本", required = true)
    private String text;

    @ApiModelProperty(value = "音色", required = true, example = "zh_female_shuangkuaisisi_moon_bigtts")
    private String speaker;

    @ApiModelProperty(value = "API Resource Id", required = true, example = "seed-tts-2.0")
    private String apiResourceId;

    @ApiModelProperty(value = "模型名称", example = "seed-tts-2.0-standard")
    private String model;

    @ApiModelProperty(value = "音频参数")
    private AudioParams audioParams;

    @ApiModelProperty(value = "语音指令")
    private List<String> contextTexts;

    @Data
    public static class AudioParams {
        @ApiModelProperty(value = "音频格式", example = "mp3")
        private String format = "mp3";

        @ApiModelProperty(value = "采样率", example = "24000")
        private Integer sampleRate = 24000;

        @ApiModelProperty(value = "语速", example = "0")
        private Integer speechRate = 0;

        @ApiModelProperty(value = "音量", example = "0")
        private Integer loudnessRate = 0;
    }
}
