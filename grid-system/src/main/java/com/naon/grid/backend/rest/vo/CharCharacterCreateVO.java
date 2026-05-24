package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class CharCharacterCreateVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;
}
