package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 用户端文化详情VO（不包含审计字段）
 */
@Getter
@Setter
public class AppCultureDetailVO implements Serializable {

    @ApiModelProperty(value = "文化唯一ID")
    private Long id;

    @ApiModelProperty(value = "文化名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源")
    private AudioVO audio;

    @ApiModelProperty(value = "文化名称翻译")
    private TextTranslationVO translation;

    @ApiModelProperty(value = "封面图片")
    private ImageVO coverImage;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "项目")
    private String project;

    @ApiModelProperty(value = "分类")
    private String category;

    @ApiModelProperty(value = "一句话介绍")
    private String oneSentenceIntro;

    @ApiModelProperty(value = "一句话介绍翻译")
    private TextTranslationVO oneSentenceIntroTranslation;

    @ApiModelProperty(value = "一句话介绍音频")
    private AudioVO oneSentenceIntroAudio;

    @ApiModelProperty(value = "一句话介绍图片")
    private ImageVO oneSentenceIntroImage;

    @ApiModelProperty(value = "详细介绍")
    private String detailedIntro;

    @ApiModelProperty(value = "详细介绍翻译")
    private TextTranslationVO detailedIntroTranslation;

    @ApiModelProperty(value = "详细介绍音频")
    private AudioVO detailedIntroAudio;

    @ApiModelProperty(value = "详细介绍图片")
    private ImageVO detailedIntroImage;

    @ApiModelProperty(value = "关键词列表")
    private List<CultureKeywordVO> keywords;

    @ApiModelProperty(value = "例句列表")
    private List<AppExampleSentenceVO> sentences;

    @ApiModelProperty(value = "题目列表")
    private List<AppExerciseQuestionDetailVO> questions;

    // ===== 嵌套 VO =====

    @Getter
    @Setter
    public static class CultureKeywordVO implements Serializable {

        @ApiModelProperty(value = "关键词")
        private String keyword;

        @ApiModelProperty(value = "关键词描述")
        private String keywordDescription;

        @ApiModelProperty(value = "关键词翻译")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "关键词描述翻译")
        private TextTranslationVO descriptionTranslation;

        @ApiModelProperty(value = "关键词音频")
        private AudioVO audio;

        @ApiModelProperty(value = "关键词图片")
        private ImageVO image;

        @ApiModelProperty(value = "排序权重")
        private Integer order;
    }

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
}
