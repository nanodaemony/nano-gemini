package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Getter
@Setter
public class CultureCreateRequest {

    @NotBlank
    @ApiModelProperty(value = "文化点名称", required = true)
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "名称音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "名称多语言翻译")
    private List<TextTranslationRequest> translations;

    @ApiModelProperty(value = "封面图片资源ID")
    private Long coverImageId;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "一级项目")
    private String project;

    @ApiModelProperty(value = "二级项目")
    private String category;

    @ApiModelProperty(value = "一句话介绍")
    private String oneSentenceIntro;

    @ApiModelProperty(value = "一句话介绍翻译")
    private List<TextTranslationRequest> oneSentenceIntroTranslations;

    @ApiModelProperty(value = "一句话介绍音频ID")
    private Long oneSentenceIntroAudioId;

    @ApiModelProperty(value = "一句话介绍图片ID")
    private Long oneSentenceIntroImageId;

    @ApiModelProperty(value = "详细介绍")
    private String detailedIntro;

    @ApiModelProperty(value = "详细介绍翻译")
    private List<TextTranslationRequest> detailedIntroTranslations;

    @ApiModelProperty(value = "详细介绍音频ID")
    private Long detailedIntroAudioId;

    @ApiModelProperty(value = "详细介绍图片ID")
    private Long detailedIntroImageId;

    @ApiModelProperty(value = "学一学例句ID列表")
    private List<Long> sentenceIds;

    @ApiModelProperty(value = "练一练习题ID列表")
    private List<Long> questionIds;

    @ApiModelProperty(value = "关键词列表")
    private List<CultureKeywordRequest> keywords;

    @ApiModelProperty(value = "AI生成字段")
    private List<String> aiGeneratedFields;

    @Getter
    @Setter
    public static class CultureKeywordRequest {
        private Long id;
        @NotBlank
        private String keyword;
        private String keywordDescription;
        private List<TextTranslationRequest> keywordTranslations;
        private List<TextTranslationRequest> keywordDescriptionTranslations;
        private Long audioId;
        private Long imageId;
        private Integer order;
        private List<String> aiGeneratedFields;
    }
}
