package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class ExerciseQuestionCreateRequest implements Serializable {

    @NotBlank
    @ApiModelProperty(value = "题目类型，参考枚举：QuestionTypeEnum", required = true)
    private String questionType;

    @ApiModelProperty(value = "题干")
    private String stem;

    @ApiModelProperty(value = "题目内容材料")
    private QuestionContentRequest content;

    @ApiModelProperty(value = "选项列表")
    @Valid
    private List<QuestionOptionRequest> options;

    @ApiModelProperty(value = "答案列表")
    private List<String> answer;

    @ApiModelProperty(value = "解析")
    private String explanation;

    @ApiModelProperty(value = "听力音频ID")
    private Long audioId;

    @ApiModelProperty(value = "听力文本（音频对应的文本内容）")
    private String audioText;

    @ApiModelProperty(value = "排序号（值越大越靠前）")
    private Integer sort;

    @Valid
    @ApiModelProperty(value = "子题列表")
    private List<ExerciseQuestionCreateRequest> children;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;

    @Getter
    @Setter
    public static class QuestionContentRequest implements Serializable {
        @ApiModelProperty(value = "内容文案")
        private String contentText;

        @ApiModelProperty(value = "内容图片ID")
        private String contentImageId;
    }

    @Getter
    @Setter
    public static class QuestionOptionRequest implements Serializable {
        @ApiModelProperty(value = "选项标识，如 A、B、C、D")
        private String option;

        @ApiModelProperty(value = "选项文案")
        private String optionText;

        @ApiModelProperty(value = "选项图片ID")
        private String optionImageId;
    }
}
