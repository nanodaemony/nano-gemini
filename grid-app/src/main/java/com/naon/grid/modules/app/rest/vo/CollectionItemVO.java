package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class CollectionItemVO implements Serializable {

    @ApiModelProperty("收藏记录ID")
    private Long id;

    @ApiModelProperty("内容ID")
    private Long contentId;

    @ApiModelProperty("内容文本")
    private String contentText;

    @ApiModelProperty("内容展示名称（动态查询获得）")
    private String contentName;

    @ApiModelProperty("收藏时间")
    private LocalDateTime createTime;
}
