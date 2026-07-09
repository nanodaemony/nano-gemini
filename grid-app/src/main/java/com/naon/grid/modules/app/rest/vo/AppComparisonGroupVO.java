package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppComparisonGroupVO implements Serializable {

    @ApiModelProperty(value = "辨析组ID")
    private Long groupId;

    @ApiModelProperty(value = "辨析组标识")
    private String groupKey;

    @ApiModelProperty(value = "辨析类型：vocab / grammar")
    private String type;

    @ApiModelProperty(value = "条目列表")
    private List<AppComparisonItemVO> items;
}
