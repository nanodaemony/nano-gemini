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

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseEntity;
import javax.persistence.*;
import java.io.Serializable;

/**
 * 翻译记录实体类
 * @author nano
 * @date 2026-05-28
 */
@Getter
@Setter
@Entity
@Table(name = "translate_record")
public class TranslateRecord extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "源文本（中文）")
    @Column(columnDefinition = "text")
    private String sourceText;

    @ApiModelProperty(value = "目标文本（译文）")
    @Column(columnDefinition = "text")
    private String targetText;

    @ApiModelProperty(value = "目标语言代码")
    private String targetLanguage;

    @ApiModelProperty(value = "使用的模型")
    private String model;

    @ApiModelProperty(value = "阿里云请求 ID")
    private String requestId;
}
