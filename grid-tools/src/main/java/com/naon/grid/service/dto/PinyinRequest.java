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

import com.naon.grid.enums.ChatProviderEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 拼音生成请求 DTO
 * @author nano
 * @date 2026-06-03
 */
@Data
@ApiModel(description = "拼音生成请求参数")
public class PinyinRequest {

    @NotNull(message = "厂商不能为空")
    @ApiModelProperty(value = "大模型厂商", required = true, allowableValues = "ALIYUN,DEEPSEEK", example = "ALIYUN")
    private ChatProviderEnum provider;

    @NotBlank(message = "模型名称不能为空")
    @ApiModelProperty(value = "模型名称", required = true, notes = "阿里云: qwen-plus/qwen-max/qwen-turbo; DeepSeek: deepseek-chat", example = "qwen-plus")
    private String model;

    @NotBlank(message = "中文文案不能为空")
    @ApiModelProperty(value = "待转换的中文文案", required = true, example = "你好，我喜欢吃米饭")
    private String chineseText;
}
