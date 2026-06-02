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
import java.util.Map;

/**
 * 大模型对话请求 DTO
 * 用于向阿里云百炼或 DeepSeek 发起对话请求
 * @author nano
 * @date 2026-06-02
 */
@Data
@ApiModel(description = "大模型对话请求参数")
public class ChatRequest {

    @NotNull(message = "厂商不能为空")
    @ApiModelProperty(value = "大模型厂商", required = true, allowableValues = "ALIYUN,DEEPSEEK", example = "ALIYUN")
    private ChatProviderEnum provider;

    @NotBlank(message = "模型名称不能为空")
    @ApiModelProperty(value = "模型名称", required = true, notes = "阿里云: qwen-plus/qwen-max/qwen-turbo; DeepSeek: deepseek-chat", example = "qwen-plus")
    private String model;

    @ApiModelProperty(value = "预设提示词名称", notes = "与 systemPrompt 二选一，优先使用预设提示词", example = "common_assistant")
    private String promptName;

    @ApiModelProperty(value = "系统提示词", notes = "与 promptName 二选一，设定模型角色和行为", example = "你是一个乐于助人的AI助手，请用友好、专业的语气回答用户的问题。")
    private String systemPrompt;

    @NotBlank(message = "用户提示词不能为空")
    @ApiModelProperty(value = "用户输入提示词", required = true, notes = "用户的问题或指令", example = "你好，请介绍一下你自己")
    private String userPrompt;

    @ApiModelProperty(value = "温度参数", notes = "控制输出的随机性，范围 0-1，值越大越随机", example = "0.7")
    private Double temperature = 0.7;

    @ApiModelProperty(value = "最大 token 数", notes = "限制响应的最大长度", example = "2000")
    private Integer maxTokens;

    @ApiModelProperty(value = "top_p 参数", notes = "核采样参数，范围 0-1，与 temperature 一般只设置一个", example = "0.9")
    private Double topP;

    @ApiModelProperty(value = "占位符替换参数", notes = "用于替换预设提示词中的 {{key}} 占位符", example = "{\"key\": \"value\"}")
    private Map<String, String> placeholderValues;

    @ApiModelProperty(value = "用户 ID", notes = "可选，用于记录和追踪特定用户的对话", example = "10086")
    private Long userId;
}
