package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppVocabWordSearchRequest implements Serializable {

    @ApiModelProperty(value = "搜索关键词")
    private String blurry;
}
