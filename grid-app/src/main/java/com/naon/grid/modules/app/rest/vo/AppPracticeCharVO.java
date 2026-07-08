package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppPracticeCharVO implements Serializable {

    @ApiModelProperty(value = "汉字ID")
    private Integer id;

    @ApiModelProperty(value = "汉字字形")
    private String character;
}
