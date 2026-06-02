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
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话响应 DTO
 * @author nano
 * @date 2026-06-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    @ApiModelProperty(value = "厂商请求 ID")
    private String requestId;

    @ApiModelProperty(value = "模型原始响应内容", required = true)
    private String content;

    @ApiModelProperty(value = "输入 token 数")
    private Integer inputTokens;

    @ApiModelProperty(value = "输出 token 数")
    private Integer outputTokens;

    @ApiModelProperty(value = "总 token 数")
    private Integer totalTokens;

    @ApiModelProperty(value = "请求耗时（毫秒）")
    private Long latencyMs;
}
