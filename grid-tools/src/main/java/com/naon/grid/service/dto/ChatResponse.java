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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大模型对话响应 DTO
 * 包含模型的原始响应内容和相关统计信息
 * @author nano
 * @date 2026-06-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "大模型对话响应结果")
public class ChatResponse {

    @ApiModelProperty(value = "厂商请求 ID", notes = "由大模型厂商返回的唯一请求标识，用于问题排查", example = "9645a4db-40d2-9e0c-8110-119a4a191946")
    private String requestId;

    @ApiModelProperty(value = "模型原始响应内容", required = true, notes = "模型生成的完整回答", example = "你好！我是一个AI助手，很高兴为你服务。有什么我可以帮助你的吗？")
    private String content;

    @ApiModelProperty(value = "输入 token 数", notes = "用户输入的 token 数量，用于计费", example = "32")
    private Integer inputTokens;

    @ApiModelProperty(value = "输出 token 数", notes = "模型输出的 token 数量，用于计费", example = "64")
    private Integer outputTokens;

    @ApiModelProperty(value = "总 token 数", notes = "输入和输出的 token 总数，用于计费", example = "96")
    private Integer totalTokens;

    @ApiModelProperty(value = "请求耗时", notes = "从收到请求到返回响应的总耗时，单位毫秒", example = "1234")
    private Long latencyMs;
}
