package com.naon.grid.backend.rest.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CultureVO implements Serializable {

    private Long id;
    private String name;
    private String pinyin;
    private Long audioId;
    private List<TextTranslationVO> translations;
    private Long coverImageId;
    private String level;
    private String project;
    private String category;

    private String oneSentenceIntro;
    private List<TextTranslationVO> oneSentenceIntroTranslations;
    private Long oneSentenceIntroAudioId;
    private Long oneSentenceIntroImageId;

    private String detailedIntro;
    private List<TextTranslationVO> detailedIntroTranslations;
    private Long detailedIntroAudioId;
    private Long detailedIntroImageId;

    private List<Long> sentenceIds;
    private List<Long> questionIds;

    private List<CultureKeywordVO> keywords;
    private List<ExampleSentenceVO> sentences;

    private String publishStatus;
    private String editStatus;
    private String createBy;
    private String updateBy;
    private Timestamp createTime;
    private Timestamp updateTime;

    @Getter
    @Setter
    public static class CultureKeywordVO implements Serializable {
        private Long id;
        private String keyword;
        private String keywordDescription;
        private List<TextTranslationVO> keywordTranslations;
        private List<TextTranslationVO> keywordDescriptionTranslations;
        private Long audioId;
        private Long imageId;
        private Integer order;
        private Timestamp createTime;
        private Timestamp updateTime;
        private List<String> aiGeneratedFields;
        private List<String> aiReviewedFields;
    }
}
