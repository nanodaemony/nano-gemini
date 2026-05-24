package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class VocabWordQueryRequest implements Serializable {

    @ApiModelProperty(value = "词汇模糊查询")
    private String blurry;
}
