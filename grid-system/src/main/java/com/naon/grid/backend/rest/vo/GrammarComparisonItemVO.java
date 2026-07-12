package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class GrammarComparisonItemVO {

    @ApiModelProperty(value = "条目ID")
    private Long id;

    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @ApiModelProperty(value = "语法点名称")
    private String grammarName;

    @ApiModelProperty(value = "用法对比")
    private String usageComparison;

    @ApiModelProperty(value = "用法对比外文翻译")
    private List<TextTranslationVO> usageComparisonTranslations;

    @ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
    private String exampleSentences;

    @ApiModelProperty(value = "用法例句ID（关联example_sentence表）")
    private Long usageSentenceId;

    @ApiModelProperty(value = "组内排序权重")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "已人工审核的AI字段名列表（是 aiGeneratedFields 的子集）")
    private List<String> aiReviewedFields;
}
