package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class VocabComparisonGroupCreateVO {
    @ApiModelProperty(value = "辨析组ID")
    private Long id;
}
