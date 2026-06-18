package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppRadicalCharVO implements Serializable {

    @ApiModelProperty(value = "汉字ID")
    private Integer id;

    @ApiModelProperty(value = "汉字字形")
    private String character;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "拼音")
    private String pinyin;
}
