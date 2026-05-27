package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class AudioResourceQueryRequest implements Serializable {

    @ApiModelProperty(value = "来源类型: tts/upload")
    private String sourceType;
}
