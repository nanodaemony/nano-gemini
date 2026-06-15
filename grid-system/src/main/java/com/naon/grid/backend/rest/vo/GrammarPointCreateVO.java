package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class GrammarPointCreateVO implements Serializable {

    @ApiModelProperty(value = "语法点ID")
    private Long id;
}
