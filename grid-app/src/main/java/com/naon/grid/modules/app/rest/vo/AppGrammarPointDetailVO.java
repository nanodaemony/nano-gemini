package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppGrammarPointDetailVO implements Serializable {

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

    @ApiModelProperty(value = "语法意义列表")
    private List<GrammarMeaningVO> meanings;

    @ApiModelProperty(value = "语法结构列表")
    private List<GrammarStructureVO> structures;

    @ApiModelProperty(value = "语法注意列表")
    private List<GrammarNoticeVO> notices;

    @ApiModelProperty(value = "语法偏误列表")
    private List<GrammarErrorVO> errors;

    @ApiModelProperty(value = "关联题目ID列表")
    private List<Long> questionIds;

    @ApiModelProperty(value = "关联辨析组列表")
    private List<GrammarComparisonVO> comparisons;

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
    public static class ExampleVO implements Serializable {
        @ApiModelProperty(value = "例句中文文案")
        private String sentence;

        @ApiModelProperty(value = "例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "例句外文翻译（按语言筛选后的单条）")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "例句音频")
        private AudioVO audio;

        @ApiModelProperty(value = "例句图片")
        private ImageVO image;

        @ApiModelProperty(value = "排序")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarMeaningVO implements Serializable {
        @ApiModelProperty(value = "语法意义ID")
        private Long id;

        @ApiModelProperty(value = "语法意义内容")
        private String content;

        @ApiModelProperty(value = "外文翻译（按语言筛选后的单条）")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "图片")
        private ImageVO image;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleVO> sentences;

        @ApiModelProperty(value = "排序")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarStructureVO implements Serializable {
        @ApiModelProperty(value = "语法结构ID")
        private Long id;

        @ApiModelProperty(value = "结构文本")
        private String content;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleVO> sentences;

        @ApiModelProperty(value = "排序")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarNoticeVO implements Serializable {
        @ApiModelProperty(value = "语法注意ID")
        private Long id;

        @ApiModelProperty(value = "注意内容")
        private String content;

        @ApiModelProperty(value = "外文翻译（按语言筛选后的单条）")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleVO> sentences;

        @ApiModelProperty(value = "排序")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarErrorVO implements Serializable {
        @ApiModelProperty(value = "语法偏误ID")
        private Long id;

        @ApiModelProperty(value = "偏误描述")
        private String content;

        @ApiModelProperty(value = "偏误分析")
        private String analysis;

        @ApiModelProperty(value = "偏误分析外文翻译（按语言筛选后的单条）")
        private TextTranslationVO analysisTranslation;

        @ApiModelProperty(value = "排序")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarComparisonVO implements Serializable {
        @ApiModelProperty(value = "辨析组ID")
        private Long id;

        @ApiModelProperty(value = "辨析组标识（如\"会vs能\"）")
        private String groupKey;

        @ApiModelProperty(value = "辨析条目列表")
        private List<ComparisonItemVO> items;
    }

    @Getter
    @Setter
    public static class ComparisonItemVO implements Serializable {
        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "语法点名称")
        private String grammarName;

        @ApiModelProperty(value = "用法对比说明")
        private String usageComparison;

        @ApiModelProperty(value = "用法对比外文翻译（按语言筛选后的单条）")
        private TextTranslationVO usageComparisonTranslation;

        @ApiModelProperty(value = "例句文本（含正误标记）")
        private String exampleSentences;

        @ApiModelProperty(value = "用法例句")
        private ExampleVO usageSentence;
    }
}
