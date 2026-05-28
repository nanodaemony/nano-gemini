package com.naon.grid.backend.rest.request;

import com.naon.grid.backend.enums.AudioSourceTypeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class AudioResourceQueryRequest implements Serializable {

    @ApiModelProperty(value = "来源类型: tts/upload")
    private AudioSourceTypeEnum sourceType;
}
