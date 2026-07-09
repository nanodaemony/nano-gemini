package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppSearchResultVO implements Serializable {

    @ApiModelProperty(value = "词汇搜索结果")
    private List<AppVocabWordBaseVO> vocab;

    @ApiModelProperty(value = "汉字搜索结果")
    private List<AppCharCharacterBaseVO> character;

    @ApiModelProperty(value = "语法搜索结果")
    private List<AppGrammarPointBaseVO> grammar;

    @ApiModelProperty(value = "辨析搜索结果")
    private List<AppComparisonGroupVO> comparison;
}
