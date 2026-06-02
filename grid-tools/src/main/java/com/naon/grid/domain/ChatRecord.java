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
package com.naon.grid.domain;

import com.naon.grid.enums.ChatProviderEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 对话记录实体类
 * @author nano
 * @date 2026-06-02
 */
@Getter
@Setter
@Entity
@Table(name = "chat_record")
public class ChatRecord implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "厂商")
    @Column(name = "provider", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ChatProviderEnum provider;

    @ApiModelProperty(value = "使用的模型")
    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @ApiModelProperty(value = "使用的预设提示词名称")
    @Column(name = "prompt_name", length = 100)
    private String promptName;

    @ApiModelProperty(value = "实际使用的系统提示词")
    @Column(name = "system_prompt", columnDefinition = "text")
    private String systemPrompt;

    @ApiModelProperty(value = "用户输入提示词")
    @Column(name = "user_prompt", columnDefinition = "text", nullable = false)
    private String userPrompt;

    @ApiModelProperty(value = "模型原始响应")
    @Column(name = "assistant_response", columnDefinition = "text", nullable = false)
    private String assistantResponse;

    @ApiModelProperty(value = "温度参数")
    @Column(name = "temperature", precision = 3, scale = 2)
    private BigDecimal temperature;

    @ApiModelProperty(value = "最大 token 数")
    @Column(name = "max_tokens")
    private Integer maxTokens;

    @ApiModelProperty(value = "top_p 参数")
    @Column(name = "top_p", precision = 3, scale = 2)
    private BigDecimal topP;

    @ApiModelProperty(value = "厂商请求 ID")
    @Column(name = "request_id", length = 100)
    private String requestId;

    @ApiModelProperty(value = "输入 token 数")
    @Column(name = "input_tokens")
    private Integer inputTokens;

    @ApiModelProperty(value = "输出 token 数")
    @Column(name = "output_tokens")
    private Integer outputTokens;

    @ApiModelProperty(value = "总 token 数")
    @Column(name = "total_tokens")
    private Integer totalTokens;

    @ApiModelProperty(value = "请求耗时（毫秒）")
    @Column(name = "latency_ms")
    private Integer latencyMs;

    @ApiModelProperty(value = "用户 ID")
    @Column(name = "user_id")
    private Long userId;

    @ApiModelProperty(value = "其他额外参数 (JSON)")
    @Column(name = "extra_params", columnDefinition = "json")
    private String extraParams;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private Timestamp createTime;
}
