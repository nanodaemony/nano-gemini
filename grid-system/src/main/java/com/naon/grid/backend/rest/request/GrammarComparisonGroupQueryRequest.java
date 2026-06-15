package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class GrammarComparisonGroupQueryRequest implements Serializable {

    @ApiModelProperty(value = "语法点ID（精确匹配）")
    private Long grammarId;

    @ApiModelProperty(value = "语法点名称（精确匹配）")
    private String grammarName;

    @ApiModelProperty(value = "辨析组标识（模糊匹配）")
    private String groupKey;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;
}
