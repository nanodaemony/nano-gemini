package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ExerciseOptionRequest implements Serializable {

    @ApiModelProperty(value = "选项标识，如 A、B、C、D")
    private String option;

    @ApiModelProperty(value = "选项文案")
    private String text;
}
