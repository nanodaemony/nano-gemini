package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DailyVocabularyVO implements Serializable {

    @ApiModelProperty(value = "ID")
    private Integer id;

    @ApiModelProperty(value = "词目")
    private String phrase;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词目翻译列表（全部语言）")
    private List<TextTranslationVO> phraseTranslations;

    @ApiModelProperty(value = "发音音频ID")
    private Long audioId;

    @ApiModelProperty(value = "AI配图ID")
    private Long imageId;

    @ApiModelProperty(value = "通俗中文讲解")
    private String plainExplanation;

    @ApiModelProperty(value = "讲解翻译列表（全部语言）")
    private List<TextTranslationVO> explanationTranslations;

    @ApiModelProperty(value = "出处/典故")
    private String originStory;

    @ApiModelProperty(value = "例句")
    private ExampleSentenceVO exampleSentence;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;

    @ApiModelProperty(value = "排序")
    private Integer order;

    @ApiModelProperty(value = "关联词汇ID")
    private Long relatedWordId;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "已人工审核的AI字段名列表（是 aiGeneratedFields 的子集）")
    private List<String> aiReviewedFields;
}
