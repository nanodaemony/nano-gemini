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
public class GrammarPointVO implements Serializable {

    @ApiModelProperty(value = "语法点ID")
    private Long id;

    @ApiModelProperty(value = "语法点名称")
    private String name;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "项目")
    private String project;

    @ApiModelProperty(value = "类别")
    private String category;

    @ApiModelProperty(value = "细目")
    private String subCategory;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @ApiModelProperty(value = "语法意义列表")
    private List<GrammarMeaningVO> meanings;

    @ApiModelProperty(value = "语法结构列表")
    private List<GrammarStructureVO> structures;

    @ApiModelProperty(value = "语法注意列表")
    private List<GrammarNoticeVO> notices;

    @ApiModelProperty(value = "语法偏误列表")
    private List<GrammarErrorVO> errors;

    @ApiModelProperty(value = "语法题目ID列表")
    private List<Long> questionIds;

    @Getter
    @Setter
    public static class GrammarMeaningVO implements Serializable {

        @ApiModelProperty(value = "语法意义ID")
        private Long id;

        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "语法意义内容")
        private String meaningContent;

        @ApiModelProperty(value = "语法意义外文翻译")
        private List<TextTranslationVO> meaningContentTranslations;

        @ApiModelProperty(value = "语法意义图片ID")
        private Long imageId;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceVO> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面）")
        private Integer order;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class GrammarStructureVO implements Serializable {

        @ApiModelProperty(value = "语法结构ID")
        private Long id;

        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "结构文本")
        private String structureContent;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceVO> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面）")
        private Integer order;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class GrammarNoticeVO implements Serializable {

        @ApiModelProperty(value = "语法注意ID")
        private Long id;

        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "注意内容")
        private String noticeContent;

        @ApiModelProperty(value = "注意内容外文翻译")
        private List<TextTranslationVO> noticeContentTranslations;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceVO> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面）")
        private Integer order;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class GrammarErrorVO implements Serializable {

        @ApiModelProperty(value = "语法偏误ID")
        private Long id;

        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "偏误描述")
        private String errorContent;

        @ApiModelProperty(value = "偏误分析")
        private String errorAnalysis;

        @ApiModelProperty(value = "偏误分析外文翻译")
        private List<TextTranslationVO> errorAnalysisTranslations;

        @ApiModelProperty(value = "排序权重（值大的排前面）")
        private Integer order;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }
}
