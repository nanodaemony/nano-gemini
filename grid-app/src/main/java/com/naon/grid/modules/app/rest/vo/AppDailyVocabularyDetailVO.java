package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
public class AppDailyVocabularyDetailVO implements Serializable {

    @ApiModelProperty(value = "ID")
    private Long id;

    @ApiModelProperty(value = "词目")
    private String phrase;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词目翻译（按语言筛选后的单条）")
    private TextTranslationVO phraseTranslation;

    @ApiModelProperty(value = "通俗中文讲解")
    private String plainExplanation;

    @ApiModelProperty(value = "讲解翻译（按语言筛选后的单条）")
    private TextTranslationVO explanationTranslation;

    @ApiModelProperty(value = "出处/典故")
    private String originStory;

    @ApiModelProperty(value = "例句")
    private VocabExampleVO exampleSentence;

    @ApiModelProperty(value = "词目发音")
    private AudioVO audio;

    @ApiModelProperty(value = "配图")
    private ImageVO image;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;

    @ApiModelProperty(value = "关联词汇ID")
    private Long relatedWordId;

    // === 内嵌 VO ===

    @Getter
    @Setter
    public static class AudioVO implements Serializable {
        @ApiModelProperty(value = "音频URL")
        private String audioUrl;
    }

    @Getter
    @Setter
    public static class ImageVO implements Serializable {
        @ApiModelProperty(value = "图片URL")
        private String imageUrl;
    }

    @Getter
    @Setter
    public static class VocabExampleVO implements Serializable {
        @ApiModelProperty(value = "例句中文文案")
        private String sentence;

        @ApiModelProperty(value = "例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "例句音频")
        private AudioVO audio;

        @ApiModelProperty(value = "例句翻译（按语言筛选后的单条）")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "例句图片")
        private ImageVO image;
    }
}
