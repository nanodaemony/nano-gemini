package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class ExerciseQuestionVO implements Serializable {

    @ApiModelProperty(value = "题目ID")
    private Long id;

    @ApiModelProperty(value = "题目类型，参考枚举：QuestionTypeEnum")
    private String questionType;

    @ApiModelProperty(value = "题干")
    private String stem;

    @ApiModelProperty(value = "题目内容材料")
    private QuestionContentVO content;

    @ApiModelProperty(value = "选项列表")
    private List<QuestionOptionVO> options;

    @ApiModelProperty(value = "答案列表")
    private List<String> answer;

    @ApiModelProperty(value = "解析")
    private String explanation;

    @ApiModelProperty(value = "听力音频ID")
    private Long audioId;

    @ApiModelProperty(value = "听力文本")
    private String audioText;

    @ApiModelProperty(value = "排序号（值越大越靠前）")
    private Integer sort;

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

    @ApiModelProperty(value = "子题列表")
    private List<ExerciseQuestionVO> children;

    @Getter
    @Setter
    public static class QuestionContentVO implements Serializable {
        @ApiModelProperty(value = "内容文案")
        private String contentText;

        @ApiModelProperty(value = "内容图片ID")
        private String contentImageId;
    }

    @Getter
    @Setter
    public static class QuestionOptionVO implements Serializable {
        @ApiModelProperty(value = "选项标识，如 A、B、C、D")
        private String option;

        @ApiModelProperty(value = "选项文案")
        private String optionText;

        @ApiModelProperty(value = "选项图片ID")
        private String optionImageId;
    }
}
