package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CollectionFolderDetailVO implements Serializable {

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

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("按业务类型分组的收藏列表")
    private List<CollectionGroupVO> groups;
}
