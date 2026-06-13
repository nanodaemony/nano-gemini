package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户端汉字书下的汉字VO
 */
@Getter
@Setter
public class AppCharBookCharVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;
}
