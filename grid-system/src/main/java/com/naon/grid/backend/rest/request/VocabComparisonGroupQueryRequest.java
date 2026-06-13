package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class VocabComparisonGroupQueryRequest implements Serializable {

    @ApiModelProperty(value = "词汇文本（精确匹配）")
    private String word;

    @ApiModelProperty(value = "词汇ID（精确匹配）")
    private Long wordId;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;
}
