package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ExerciseQuestionQueryRequest implements Serializable {

    @ApiModelProperty(value = "题干模糊搜索")
    private String blurry;

    @ApiModelProperty(value = "题目类型")
    private String questionType;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;

    @ApiModelProperty(value = "业务标签过滤，参考枚举：QuestionBizTagEnum")
    private String tags;
}
