package com.naon.grid.backend.service.vocabcomparison.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class VocabComparisonGroupQueryCriteria implements Serializable {

    @ApiModelProperty(value = "词汇文本（精确匹配）")
    private String word;

    @ApiModelProperty(value = "词汇ID（精确匹配）")
    private Long wordId;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;
}
