package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;

@Getter
@Setter
public class VocabExerciseDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "练习题目唯一ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "题目类型")
    private String questionType;

    @ApiModelProperty(value = "练习题干描述")
    private String questionText;

    @ApiModelProperty(value = "选项列表")
    private String options;

    @ApiModelProperty(value = "答案列表")
    private String answers;

    @ApiModelProperty(value = "练习题目排序权重")
    private Integer exerciseOrder;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
