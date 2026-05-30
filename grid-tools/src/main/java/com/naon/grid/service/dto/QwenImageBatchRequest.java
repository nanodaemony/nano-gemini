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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * 千问文生图（Qwen-Image）批量生成请求 DTO
 * @author nano
 * @date 2026-05-30
 */
@Data
public class QwenImageBatchRequest {

    @NotBlank(message = "提示词不能为空")
    @ApiModelProperty(value = "正向提示词（图像描述）", required = true,
            notes = "支持中英文。qwen-image-2.0系列上限1300 Token，其他模型800 Token")
    private String prompt;

    @ApiModelProperty(value = "模型名称", example = "qwen-image-2.0-pro",
            notes = "可选：qwen-image-2.0-pro（推荐）、qwen-image-2.0、qwen-image-max、qwen-image-plus、qwen-image")
    private String model = "qwen-image-2.0-pro";

    @ApiModelProperty(value = "反向提示词",
            notes = "不希望在图像中出现的内容。长度不超过500字符")
    private String negativePrompt;

    @ApiModelProperty(value = "输出图像分辨率", example = "2048*2048",
            notes = "qwen-image-2.0系列默认2048*2048，总像素需在512*512~2048*2048之间")
    private String size;

    @Min(value = 1, message = "生成数量不能少于1")
    @Max(value = 6, message = "生成数量不能超过6")
    @ApiModelProperty(value = "生成图像数量", example = "4",
            notes = "取值范围 1-6。仅 qwen-image-2.0 系列支持多张，max/plus 系列固定为 1")
    private Integer n = 4;

    @ApiModelProperty(value = "是否开启 Prompt 智能改写", example = "true",
            notes = "默认true。开启后模型将对正向提示词进行优化")
    private Boolean promptExtend;

    @ApiModelProperty(value = "是否添加水印", example = "false",
            notes = "默认false。true时在右下角添加 Qwen-Image 水印")
    private Boolean watermark;

    @ApiModelProperty(value = "随机数种子",
            notes = "取值范围 [0, 2147483647]。相同 seed 可使生成内容保持相对稳定")
    private Long seed;
}
