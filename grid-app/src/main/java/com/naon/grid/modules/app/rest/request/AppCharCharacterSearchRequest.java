package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户端汉字搜索请求
 */
@Data
public class AppCharCharacterSearchRequest implements Serializable {

    @ApiModelProperty(value = "汉字模糊查询关键词")
    private String blurry;
}
