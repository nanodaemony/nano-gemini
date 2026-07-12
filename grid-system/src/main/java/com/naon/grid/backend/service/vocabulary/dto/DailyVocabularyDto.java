package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DailyVocabularyDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "每日一词ID")
    private Integer id;

    @ApiModelProperty(value = "词目")
    private String phrase;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词目翻译列表")
    private List<TextTranslation> phraseTranslations;

    @ApiModelProperty(value = "发音音频ID")
    private Long audioId;

    @ApiModelProperty(value = "AI配图ID")
    private Long imageId;

    @ApiModelProperty(value = "通俗中文讲解")
    private String plainExplanation;

    @ApiModelProperty(value = "讲解翻译列表")
    private List<TextTranslation> explanationTranslations;

    @ApiModelProperty(value = "出处/典故")
    private String originStory;

    @ApiModelProperty(value = "例句")
    private ExampleSentenceDto exampleSentence;

    @ApiModelProperty(value = "例句ID（创建/编辑时直接传 ID）")
    private Long exampleSentenceId;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;

    @ApiModelProperty(value = "排序")
    private Integer order;

    @ApiModelProperty(value = "关联词汇ID")
    private Long relatedWordId;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;
}
