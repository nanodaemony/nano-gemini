package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TopicQueryRequest {

    @ApiModelProperty(value = "模糊搜索（话题名称）")
    private String blurry;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;
}
