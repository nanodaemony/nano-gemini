package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ExerciseQuestionSearchRequest implements Serializable {

    @ApiModelProperty(value = "题干模糊搜索关键词")
    private String keyword;

    @ApiModelProperty(value = "题目类型过滤，参考枚举：QuestionTypeEnum")
    private String questionType;
}
