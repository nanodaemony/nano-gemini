package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ExerciseOptionVO implements Serializable {

    @ApiModelProperty(value = "选项标识，如 A、B、C、D")
    private String option;

    @ApiModelProperty(value = "选项文案")
    private String text;
}
