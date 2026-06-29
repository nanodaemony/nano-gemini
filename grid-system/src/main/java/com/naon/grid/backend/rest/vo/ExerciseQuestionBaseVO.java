package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class ExerciseQuestionBaseVO implements Serializable {

    @ApiModelProperty(value = "题目ID")
    private Long id;

    @ApiModelProperty(value = "题目类型，参考枚举：QuestionTypeEnum")
    private String questionType;

    @ApiModelProperty(value = "题干")
    private String stem;

    @ApiModelProperty(value = "听力音频ID")
    private Long audioId;

    @ApiModelProperty(value = "排序号（值越大越靠前）")
    private Integer sort;

    @ApiModelProperty(value = "子题数量")
    private Integer childCount;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
