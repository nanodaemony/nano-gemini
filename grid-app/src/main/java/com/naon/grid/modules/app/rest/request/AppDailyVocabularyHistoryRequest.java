package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppDailyVocabularyHistoryRequest implements Serializable {

    @ApiModelProperty(value = "类型筛选")
    private String phraseType;

    @ApiModelProperty(value = "关键词搜索（词目）")
    private String keyword;

    @ApiModelProperty(value = "月份 yyyy-MM")
    private String month;

    @ApiModelProperty(value = "页码")
    private Integer page = 0;

    @ApiModelProperty(value = "每页条数")
    private Integer size = 20;
}
