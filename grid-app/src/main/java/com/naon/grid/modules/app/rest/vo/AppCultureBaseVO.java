package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户端文化基础VO（不包含审计字段）
 */
@Getter
@Setter
public class AppCultureBaseVO implements Serializable {

    @ApiModelProperty(value = "文化唯一ID")
    private Long id;

    @ApiModelProperty(value = "文化名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "项目")
    private String project;

    @ApiModelProperty(value = "分类")
    private String category;
}
