package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppCharRadicalBaseVO implements Serializable {

    @ApiModelProperty(value = "部首ID")
    private Long id;

    @ApiModelProperty(value = "部首字符")
    private String radical;

    @ApiModelProperty(value = "部首名称")
    private String radicalName;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "关联部首ID")
    private Long relationId;
}
