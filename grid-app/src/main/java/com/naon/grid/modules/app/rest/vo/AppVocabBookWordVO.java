package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户端词汇书下的词汇VO
 */
@Getter
@Setter
public class AppVocabBookWordVO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇")
    private String word;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;
}
