package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppGrammarPointSearchRequest implements Serializable {

    @ApiModelProperty(value = "搜索关键词（模糊匹配语法点名称）")
    private String keyword;
}
