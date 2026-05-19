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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseEntity;
import javax.persistence.*;
import java.io.Serializable;

/**
 * 阿里云 OSS 存储实体类
 * @author Zheng Jie
 * @date 2025-05-19
 */
@Getter
@Setter
@Entity
@Table(name = "oss_resource_meta")
public class AliOssStorage extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "原始文件名")
    private String fileName;

    @ApiModelProperty(value = "OSS 上存储的文件名 (UUID)")
    private String fileRealName;

    @ApiModelProperty(value = "文件大小")
    private String fileSize;

    @ApiModelProperty(value = "文件 MIME 类型")
    private String fileMimeType;

    @ApiModelProperty(value = "文件类型")
    private String fileType;

    @ApiModelProperty(value = "OSS 完整访问 URL")
    private String fileUrl;

    @ApiModelProperty(value = "存储桶名称")
    private String bucketName;

    public void copy(AliOssStorage source) {
        BeanUtil.copyProperties(source, this, CopyOptions.create().setIgnoreNullValue(true));
    }
}
