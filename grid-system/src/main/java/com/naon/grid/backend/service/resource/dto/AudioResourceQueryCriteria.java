package com.naon.grid.backend.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.naon.grid.annotation.Query;
import java.io.Serializable;

@Data
public class AudioResourceQueryCriteria implements Serializable {

    @ApiModelProperty(value = "来源类型: tts/upload")
    @Query
    private String sourceType;
}
