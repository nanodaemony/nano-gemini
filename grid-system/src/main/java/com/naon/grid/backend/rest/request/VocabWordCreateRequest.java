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
public class VocabWordCreateRequest implements Serializable {

    @NotBlank
    @ApiModelProperty(value = "词汇词头", required = true)
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "词头拼音")
    private String pinyin;

    @ApiModelProperty(value = "词汇音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @Valid
    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseRequest> senses;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @Getter
    @Setter
    public static class VocabSenseRequest implements Serializable {

        @ApiModelProperty(value = "义项ID（新增时不传，更新时传）")
        private Integer id;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "中文释义")
        private String chineseDef;

        @ApiModelProperty(value = "中文释义外文翻译")
        private List<TextTranslationRequest> defTranslations;

        @ApiModelProperty(value = "中文释义音频ID")
        private Long defAudioId;

        @ApiModelProperty(value = "中文释义图片ID")
        private Long defImageId;

        @ApiModelProperty("释义图片例句信息(可能没有, 但释义有图片时就需要有)")
        private ExampleSentenceRequest defImageSentence;

        @ApiModelProperty(value = "近义词列表")
        private List<VocabRelationRequest> synonymWords;

        @ApiModelProperty(value = "反义词列表")
        private List<VocabRelationRequest> antonymWords;

        @ApiModelProperty(value = "正序关联词汇")
        private List<VocabRelationRequest> sequentialWords;

        @ApiModelProperty(value = "逆序关联词汇")
        private List<VocabRelationRequest> reverseSequentialWords;

        @ApiModelProperty(value = "逆序关联词汇")
        private List<VocabRelationRequest> jumbledWords;

        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;

        @ApiModelProperty(value = "义项排序权重，值大的排前面", required = true)
        private Integer order;

        @Valid
        @ApiModelProperty(value = "搭配列表")
        private List<VocabStructureRequest> structures;
    }

    @Getter
    @Setter
    public static class VocabStructureRequest implements Serializable {
        @ApiModelProperty(value = "结构ID（新增时不传，更新时传）")
        private Integer id;

        @NotBlank
        @ApiModelProperty(value = "结构文案", required = true)
        private String pattern;

        @ApiModelProperty(value = "结构释义")
        private String patternDef;

        @ApiModelProperty(value = "结构释义外文翻译列表")
        private List<TextTranslationRequest> patternDefTranslations;

        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;

        @ApiModelProperty(value = "结构排序权重，值大的排前面", required = true)
        private Integer order;

        @Valid
        @ApiModelProperty(value = "结构例句")
        private List<ExampleSentenceRequest> structureSentences;
    }

}
