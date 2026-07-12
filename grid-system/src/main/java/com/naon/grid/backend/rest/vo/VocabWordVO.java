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
public class VocabWordVO implements Serializable {

    @ApiModelProperty(value = "词汇ID")
    private Integer id;

    @ApiModelProperty(value = "词汇词头")
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "词汇拼音")
    private String pinyin;

    @ApiModelProperty(value = "词汇音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseVO> senses;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    private String updateBy;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Getter
    @Setter
    public static class VocabSenseVO implements Serializable {
        
        @ApiModelProperty(value = "词汇义项ID")
        private Integer id;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "中文释义")
        private String chineseDef;

        @ApiModelProperty(value = "中文释义外文翻译")
        private List<TextTranslationVO> defTranslations;

        @ApiModelProperty(value = "中文释义音频ID")
        private Long defAudioId;

        @ApiModelProperty(value = "中文释义图片ID")
        private Long defImageId;

        @ApiModelProperty(value = "中文释义图片例句信息(可能没有)")
        private ExampleSentenceVO defImageSentence;

        @ApiModelProperty(value = "近义词列表")
        private List<VocabRelationVO> synonymWords;

        @ApiModelProperty(value = "反义词列表")
        private List<VocabRelationVO> antonymWords;

        @ApiModelProperty(value = "正序关联词汇")
        private List<VocabRelationVO> sequentialWords;

        @ApiModelProperty(value = "逆序关联词汇")
        private List<VocabRelationVO> reverseSequentialWords;

        @ApiModelProperty(value = "乱序关联词汇")
        private List<VocabRelationVO> jumbledWords;

        @ApiModelProperty(value = "搭配列表")
        private List<VocabStructureVO> structures;

        @ApiModelProperty(value = "义项排序权重，值大的排前面")
        private Integer order;

        @ApiModelProperty(value = "AI生成的字段名列表")
        private List<String> aiGeneratedFields;

        @ApiModelProperty(value = "已人工审核的AI字段名列表（是 aiGeneratedFields 的子集）")
        private List<String> aiReviewedFields;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class VocabStructureVO implements Serializable {

        @ApiModelProperty(value = "结构ID")
        private Integer id;

        @ApiModelProperty(value = "所属词汇ID")
        private Integer wordId;

        @ApiModelProperty(value = "所属义项ID")
        private Integer senseId;

        @ApiModelProperty(value = "结构文案")
        private String pattern;

        @ApiModelProperty(value = "结构释义")
        private String patternDef;

        @ApiModelProperty(value = "结构释义外文翻译列表")
        private List<TextTranslationVO> patternDefTranslations;

        @ApiModelProperty(value = "结构例句列表")
        private List<ExampleSentenceVO> structureExamples;

        @ApiModelProperty(value = "结构排序权重(值大的排前面)")
        private Integer order;

        @ApiModelProperty(value = "AI生成的字段名列表")
        private List<String> aiGeneratedFields;

        @ApiModelProperty(value = "已人工审核的AI字段名列表（是 aiGeneratedFields 的子集）")
        private List<String> aiReviewedFields;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

}
