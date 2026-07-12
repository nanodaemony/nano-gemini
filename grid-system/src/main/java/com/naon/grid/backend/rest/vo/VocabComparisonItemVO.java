package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

@Data
public class VocabComparisonItemVO {

    @ApiModelProperty(value = "条目ID")
    private Long id;

    @ApiModelProperty(value = "词汇ID")
    private Long wordId;

    @ApiModelProperty(value = "词汇词头")
    private String word;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "用法对比")
    private String usageComparison;

    @ApiModelProperty(value = "用法对比外文翻译")
    private List<TextTranslationVO> usageComparisonTranslations;

    @ApiModelProperty(value = "通用用法")
    private String commonUsage;

    @ApiModelProperty(value = "通用用法外文翻译")
    private List<TextTranslationVO> commonUsageTranslations;

    @ApiModelProperty(value = "组内排序权重")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "已人工审核的AI字段名列表（是 aiGeneratedFields 的子集）")
    private List<String> aiReviewedFields;
}
