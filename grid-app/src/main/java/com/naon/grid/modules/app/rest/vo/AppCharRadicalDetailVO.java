package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppCharRadicalDetailVO implements Serializable {

    // 部首基本信息
    @ApiModelProperty(value = "部首ID")
    private Long id;

    @ApiModelProperty(value = "部首字符")
    private String radical;

    @ApiModelProperty(value = "部首名称")
    private String radicalName;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "演化解说")
    private String evolutionDesc;

    @ApiModelProperty(value = "关联部首ID")
    private Long relationId;

    // 字节汉字列表
    @ApiModelProperty(value = "当前页关联汉字列表")
    private List<AppRadicalCharVO> characters;

    // 是否有下一页
    @ApiModelProperty(value = "是否还有下一页")
    private Boolean hasNext;
}
