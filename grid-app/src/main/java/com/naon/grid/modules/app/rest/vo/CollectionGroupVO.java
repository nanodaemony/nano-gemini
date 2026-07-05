package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CollectionGroupVO implements Serializable {

    @ApiModelProperty("业务类型")
    private String bizType;

    @ApiModelProperty("该类型下的收藏内容列表")
    private List<CollectionItemVO> items;
}
