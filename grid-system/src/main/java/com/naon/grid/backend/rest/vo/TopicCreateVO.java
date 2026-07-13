package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TopicCreateVO {

    @ApiModelProperty(value = "话题ID")
    private Long id;
}
