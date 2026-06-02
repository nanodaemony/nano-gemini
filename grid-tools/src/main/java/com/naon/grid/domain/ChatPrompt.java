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

import com.naon.grid.base.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预设提示词实体类
 * @author nano
 * @date 2026-06-02
 */
@Getter
@Setter
@Entity
@Table(name = "chat_prompt")
public class ChatPrompt extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "提示词名称")
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @ApiModelProperty(value = "描述说明")
    @Column(name = "description", length = 500)
    private String description;

    @ApiModelProperty(value = "系统提示词内容")
    @Column(name = "system_prompt", columnDefinition = "text", nullable = false)
    private String systemPrompt;

    @ApiModelProperty(value = "推荐使用的模型")
    @Column(name = "model", length = 100)
    private String model;

    @ApiModelProperty(value = "推荐温度参数")
    @Column(name = "temperature", precision = 3, scale = 2)
    private BigDecimal temperature;

    @ApiModelProperty(value = "状态：1-有效，0-无效")
    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
