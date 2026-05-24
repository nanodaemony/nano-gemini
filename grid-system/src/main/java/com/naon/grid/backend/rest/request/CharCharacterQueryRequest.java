package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharCharacterQueryRequest implements Serializable {

    @ApiModelProperty(value = "汉字或拼音模糊查询")
    private String blurry;
}
