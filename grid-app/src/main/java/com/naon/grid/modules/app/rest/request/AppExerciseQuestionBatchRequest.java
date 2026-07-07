package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AppExerciseQuestionBatchRequest implements Serializable {

    @ApiModelProperty(value = "题目ID列表", required = true)
    private List<Long> ids;
}
