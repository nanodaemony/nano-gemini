package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
public class AppDailyVocabularyBaseVO implements Serializable {

    @ApiModelProperty(value = "ID")
    private Long id;

    @ApiModelProperty(value = "词目")
    private String phrase;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "配图URL")
    private String imageUrl;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;
}
