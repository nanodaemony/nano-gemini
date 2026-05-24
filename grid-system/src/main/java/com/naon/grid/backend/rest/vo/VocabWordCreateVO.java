package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
public class VocabWordCreateVO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;
}
