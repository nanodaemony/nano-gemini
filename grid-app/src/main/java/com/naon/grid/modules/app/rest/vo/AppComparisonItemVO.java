package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppComparisonItemVO implements Serializable {

    @ApiModelProperty(value = "词汇ID（词汇辨析）")
    private Long wordId;

    @ApiModelProperty(value = "词汇（词汇辨析）")
    private String word;

    @ApiModelProperty(value = "语法点ID（语法辨析）")
    private Long grammarId;

    @ApiModelProperty(value = "语法点名称（语法辨析）")
    private String grammarName;
}
