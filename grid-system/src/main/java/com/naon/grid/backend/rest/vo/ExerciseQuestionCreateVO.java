package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ExerciseQuestionCreateVO implements Serializable {

    @ApiModelProperty(value = "新建题目ID")
    private Long id;
}
