package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class VocabRelationDto implements Serializable {

    @ApiModelProperty(value = "关联ID")
    private Long id;
    @ApiModelProperty(value = "词汇ID")
    private Integer wordId;
    @ApiModelProperty(value = "义项ID")
    private Integer senseId;
    @ApiModelProperty(value = "当前词汇")
    private String word;
    @ApiModelProperty(value = "关联类型")
    private String relationType;
    @ApiModelProperty(value = "关联词汇ID")
    private Long relationWordId;
    @ApiModelProperty(value = "关联义项ID")
    private Long relationSenseId;
    @ApiModelProperty(value = "关联词汇")
    private String relationWord;
    @ApiModelProperty(value = "排序权重")
    private Integer order;
    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;
    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
