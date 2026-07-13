package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AppTopicSearchRequest {

    @ApiModelProperty(value = "搜索关键词（模糊匹配话题名称）")
    private String blurry;
}
