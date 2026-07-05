package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class CollectionFolderVO implements Serializable {

    @ApiModelProperty("收藏夹ID")
    private Long id;

    @ApiModelProperty("收藏夹名称")
    private String name;

    @ApiModelProperty("封面图资源ID")
    private Long coverImageId;

    @ApiModelProperty("是否默认收藏夹")
    private Boolean isDefault;

    @ApiModelProperty("是否置顶")
    private Boolean isPinned;

    @ApiModelProperty("有效收藏数量")
    private Long itemCount;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
