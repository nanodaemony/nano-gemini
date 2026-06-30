package com.naon.grid.backend.service.grammar.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class GrammarQuestionDto implements Serializable {

    @ApiModelProperty(value = "语法题目关联ID")
    private Long id;

    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @ApiModelProperty(value = "语法题目ID列表")
    private List<Long> questionIds;

    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer order;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
