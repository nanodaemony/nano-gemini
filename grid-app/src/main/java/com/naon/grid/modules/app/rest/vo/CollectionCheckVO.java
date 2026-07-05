package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
public class CollectionCheckVO implements Serializable {

    @ApiModelProperty("是否已收藏")
    private Boolean collected;

    @ApiModelProperty("收藏记录ID（已收藏时返回）")
    private Long itemId;

    @ApiModelProperty("所在收藏夹名称（已收藏时返回）")
    private String folderName;
}
