package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabWordCreateRequest implements Serializable {

    @ApiModelProperty(value = "词汇")
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "标准拼音（含声调）")
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String hskLevel;

    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseRequest> senses;

    @ApiModelProperty(value = "练习题列表")
    private List<VocabExerciseRequest> exercises;

    @Getter
    @Setter
    public static class VocabSenseRequest implements Serializable {
        @ApiModelProperty(value = "自增ID, 义项ID")
        private Integer id;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "中文释义")
        private String chineseDef;

        @ApiModelProperty(value = "中文释义音频资源ID")
        private Long defAudioId;

        @ApiModelProperty(value = "外文翻译列表")
        private String translations;

        @ApiModelProperty(value = "近义词列表")
        private String synonyms;

        @ApiModelProperty(value = "反义词列表")
        private String antonyms;

        @ApiModelProperty(value = "正序关联词汇")
        private String relatedForward;

        @ApiModelProperty(value = "逆序关联词汇")
        private String relatedBackward;

        @ApiModelProperty(value = "义项排序权重")
        private Integer senseOrder;

        @ApiModelProperty(value = "搭配列表")
        private List<VocabStructureRequest> structures;
    }

    @Getter
    @Setter
    public static class VocabStructureRequest implements Serializable {
        @ApiModelProperty(value = "自增ID, 结构搭配ID")
        private Integer id;

        @ApiModelProperty(value = "结构搭配文案")
        private String pattern;

        @ApiModelProperty(value = "搭配排序权重")
        private Integer structureOrder;

        @ApiModelProperty(value = "例句列表")
        private List<VocabExampleRequest> examples;
    }

    @Getter
    @Setter
    public static class VocabExerciseRequest implements Serializable {
        @ApiModelProperty(value = "练习题目唯一ID")
        private Integer id;

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
    }

    @Getter
    @Setter
    public static class VocabExampleRequest implements Serializable {
        @ApiModelProperty(value = "例句唯一ID")
        private Integer id;

        @ApiModelProperty(value = "例句中文文案")
        private String sentence;

        @ApiModelProperty(value = "例句音频资源ID")
        private Long audioId;

        @ApiModelProperty(value = "例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "例句外文翻译列表")
        private String translations;

        @ApiModelProperty(value = "例句排序权重")
        private Integer exampleOrder;
    }
}
