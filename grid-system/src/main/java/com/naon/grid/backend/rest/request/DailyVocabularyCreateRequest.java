package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DailyVocabularyCreateRequest implements Serializable {

    @NotBlank
    @ApiModelProperty(value = "词目", required = true)
    private String phrase;

    @ApiModelProperty(value = "类型: IDIOM/PROVERB/COLLOQUIALISM/XIEHOUYU/NEOLOGISM")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词目翻译列表")
    private List<TextTranslationRequest> phraseTranslations;

    @ApiModelProperty(value = "发音音频ID")
    private Long audioId;

    @ApiModelProperty(value = "AI配图ID")
    private Long imageId;

    @ApiModelProperty(value = "通俗中文讲解")
    private String plainExplanation;

    @ApiModelProperty(value = "讲解翻译列表")
    private List<TextTranslationRequest> explanationTranslations;

    @ApiModelProperty(value = "出处/典故")
    private String originStory;

    @ApiModelProperty(value = "例句ID")
    private Long exampleSentenceId;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;

    @ApiModelProperty(value = "排序（同日期最小=主推）")
    private Integer order;

    @ApiModelProperty(value = "关联词汇ID")
    private Long relatedWordId;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;
}
