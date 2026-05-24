package com.naon.grid.backend.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.naon.grid.annotation.Query;
import java.io.Serializable;

@Data
public class AudioResourceQueryCriteria implements Serializable {

    @ApiModelProperty(value = "业务类型")
    @Query
    private String bizType;
}
