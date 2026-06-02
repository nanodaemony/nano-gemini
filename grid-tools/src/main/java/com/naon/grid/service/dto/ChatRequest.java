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
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 对话请求 DTO
 * @author nano
 * @date 2026-06-02
 */
@Data
public class ChatRequest {

    @NotNull(message = "厂商不能为空")
    @ApiModelProperty(value = "厂商：ALIYUN/DEEPSEEK", required = true)
    private ChatProviderEnum provider;

    @NotBlank(message = "模型名称不能为空")
    @ApiModelProperty(value = "模型名称", required = true)
    private String model;

    @ApiModelProperty(value = "预设提示词名称")
    private String promptName;

    @ApiModelProperty(value = "系统提示词")
    private String systemPrompt;

    @NotBlank(message = "用户提示词不能为空")
    @ApiModelProperty(value = "用户输入提示词", required = true)
    private String userPrompt;

    @ApiModelProperty(value = "温度参数，默认 0.7")
    private Double temperature = 0.7;

    @ApiModelProperty(value = "最大 token 数")
    private Integer maxTokens;

    @ApiModelProperty(value = "top_p 参数")
    private Double topP;

    @ApiModelProperty(value = "占位符替换参数")
    private Map<String, String> placeholderValues;

    @ApiModelProperty(value = "用户 ID")
    private Long userId;
}
