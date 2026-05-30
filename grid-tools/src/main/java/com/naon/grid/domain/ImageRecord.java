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
 * 千问文生图（Qwen-Image）生成记录实体类
 * @author nano
 * @date 2026-05-30
 */
@Getter
@Setter
@Entity
@Table(name = "image_record")
public class ImageRecord extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "正向提示词")
    @Column(columnDefinition = "text")
    private String prompt;

    @ApiModelProperty(value = "反向提示词")
    @Column(columnDefinition = "text")
    private String negativePrompt;

    @ApiModelProperty(value = "模型名称")
    private String model;

    @ApiModelProperty(value = "分辨率")
    private String size;

    @ApiModelProperty(value = "图像数量")
    private Integer imageCount;

    @ApiModelProperty(value = "是否开启 Prompt 改写")
    private Boolean promptExtend;

    @ApiModelProperty(value = "是否添加水印")
    private Boolean watermark;

    @ApiModelProperty(value = "随机数种子")
    private Long seed;

    @ApiModelProperty(value = "OSS 最终图像 URL")
    private String finalImageUrl;

    @ApiModelProperty(value = "阿里云请求 ID")
    private String requestId;
}
