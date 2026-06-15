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
public class GrammarPointCreateRequest implements Serializable {

    @NotBlank
    @ApiModelProperty(value = "语法点名称", required = true)
    private String name;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "项目")
    private String project;

    @ApiModelProperty(value = "类别")
    private String category;

    @ApiModelProperty(value = "细目")
    private String subCategory;

    @Valid
    @ApiModelProperty(value = "语法意义列表")
    private List<GrammarMeaningRequest> meanings;

    @Valid
    @ApiModelProperty(value = "语法结构列表")
    private List<GrammarStructureRequest> structures;

    @Valid
    @ApiModelProperty(value = "语法注意列表")
    private List<GrammarNoticeRequest> notices;

    @Valid
    @ApiModelProperty(value = "语法偏误列表")
    private List<GrammarErrorRequest> errors;

    @Getter
    @Setter
    public static class GrammarMeaningRequest implements Serializable {

        @ApiModelProperty(value = "语法意义ID, 新增时不传, 更新时传")
        private Long id;

        @NotBlank
        @ApiModelProperty(value = "语法意义内容", required = true)
        private String meaningContent;

        @ApiModelProperty(value = "语法意义外文翻译")
        private List<TextTranslationRequest> meaningContentTranslations;

        @ApiModelProperty(value = "语法意义图片ID")
        private Long imageId;

        @Valid
        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceRequest> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面，不传默认 0）")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarStructureRequest implements Serializable {

        @ApiModelProperty(value = "语法结构ID, 新增时不传, 更新时传")
        private Long id;

        @NotBlank
        @ApiModelProperty(value = "结构文本", required = true)
        private String structureContent;

        @Valid
        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceRequest> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面，不传默认 0）")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarNoticeRequest implements Serializable {

        @ApiModelProperty(value = "语法注意ID, 新增时不传, 更新时传")
        private Long id;

        @NotBlank
        @ApiModelProperty(value = "注意内容", required = true)
        private String noticeContent;

        @ApiModelProperty(value = "注意内容外文翻译")
        private List<TextTranslationRequest> noticeContentTranslations;

        @Valid
        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceRequest> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面，不传默认 0）")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarErrorRequest implements Serializable {

        @ApiModelProperty(value = "语法偏误ID, 新增时不传, 更新时传")
        private Long id;

        @NotBlank
        @ApiModelProperty(value = "偏误描述", required = true)
        private String errorContent;

        @ApiModelProperty(value = "偏误分析")
        private String errorAnalysis;

        @ApiModelProperty(value = "偏误分析外文翻译")
        private List<TextTranslationRequest> errorAnalysisTranslations;

        @ApiModelProperty(value = "排序权重（值大的排前面，不传默认 0）")
        private Integer order;
    }
}
