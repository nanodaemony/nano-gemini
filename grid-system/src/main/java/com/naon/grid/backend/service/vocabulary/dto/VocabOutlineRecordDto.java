package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class VocabOutlineRecordDto implements Serializable {

    @ApiModelProperty(value = "主键ID")
    private Integer id;

    @ApiModelProperty(value = "词汇文本")
    private String word;

    @ApiModelProperty(value = "未搜到次数")
    private Integer searchCount;

    @ApiModelProperty(value = "处理状态, 0:未处理 1:已处理")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
