package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppExerciseQuestionDetailVO implements Serializable {

    @ApiModelProperty(value = "题目ID")
    private Long id;

    @ApiModelProperty(value = "题目类型")
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

    @ApiModelProperty(value = "听力音频")
    private AudioVO audio;

    @ApiModelProperty(value = "听力文本")
    private String audioText;

    @ApiModelProperty(value = "排序号（值越大越靠前）")
    private Integer sort;

    @ApiModelProperty(value = "子题列表")
    private List<AppExerciseQuestionDetailVO> children;

    // ===== 嵌套 VO =====

    @Getter
    @Setter
    public static class AudioVO implements Serializable {
        @ApiModelProperty(value = "音频文件地址")
        private String audioUrl;
    }

    @Getter
    @Setter
    public static class ImageVO implements Serializable {
        @ApiModelProperty(value = "图片文件地址")
        private String imageUrl;
    }

    @Getter
    @Setter
    public static class QuestionContentVO implements Serializable {
        @ApiModelProperty(value = "内容文案")
        private String contentText;

        @ApiModelProperty(value = "内容图片")
        private ImageVO image;
    }

    @Getter
    @Setter
    public static class QuestionOptionVO implements Serializable {
        @ApiModelProperty(value = "选项标识，如 A、B、C、D")
        private String option;

        @ApiModelProperty(value = "选项文案")
        private String optionText;

        @ApiModelProperty(value = "选项图片")
        private ImageVO image;
    }
}
