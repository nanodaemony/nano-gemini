package com.naon.grid.backend.service.vocabcomparison.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VocabComparisonItemDto {

    @ApiModelProperty(value = "条目ID（新增时为null）")
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
    private List<TextTranslation> usageComparisonTranslations;

    @ApiModelProperty(value = "通用用法")
    private String commonUsage;

    @ApiModelProperty(value = "通用用法外文翻译")
    private List<TextTranslation> commonUsageTranslations;

    @ApiModelProperty(value = "组内排序权重")
    private Integer order;
}
