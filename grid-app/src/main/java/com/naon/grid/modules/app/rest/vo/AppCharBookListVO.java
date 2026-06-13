package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户端汉字书列表VO
 */
@Getter
@Setter
public class AppCharBookListVO implements Serializable {

    @ApiModelProperty(value = "汉字书ID")
    private Long id;

    @ApiModelProperty(value = "汉字书类型")
    private String type;

    @ApiModelProperty(value = "汉字书名称")
    private String name;

    @ApiModelProperty(value = "汉字书子名称")
    private String subName;

    @ApiModelProperty(value = "汉字书封面图")
    private String coverImage;

    @ApiModelProperty(value = "汉字书描述")
    private String desc;
}
