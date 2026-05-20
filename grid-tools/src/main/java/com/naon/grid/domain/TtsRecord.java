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
 * 阿里云 TTS 语音合成记录实体类
 * @author nano
 * @date 2025-05-19
 */
@Getter
@Setter
@Entity
@Table(name = "tts_record")
public class TtsRecord extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "音色名称")
    private String voice;

    @ApiModelProperty(value = "合成文本")
    @Column(columnDefinition = "text")
    private String text;

    @ApiModelProperty(value = "控制指令")
    @Column(columnDefinition = "text")
    private String instructions;

    @ApiModelProperty(value = "模型名称")
    private String model;

    @ApiModelProperty(value = "语言类型")
    private String languageType;

    @ApiModelProperty(value = "OSS 最终音频 URL")
    private String finalAudioUrl;

    @ApiModelProperty(value = "阿里云请求 ID")
    private String requestId;
}
