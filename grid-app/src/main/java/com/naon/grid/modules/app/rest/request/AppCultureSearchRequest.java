package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户端文化搜索请求
 */
@Data
public class AppCultureSearchRequest implements Serializable {

    @ApiModelProperty(value = "文化模糊查询关键词")
    private String blurry;
}
