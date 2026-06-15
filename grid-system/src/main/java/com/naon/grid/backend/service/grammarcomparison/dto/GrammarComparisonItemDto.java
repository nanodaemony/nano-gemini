package com.naon.grid.backend.service.grammarcomparison.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GrammarComparisonItemDto {

    @ApiModelProperty(value = "条目ID（新增时为null）")
    private Long id;

    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @ApiModelProperty(value = "语法点名称")
    private String grammarName;

    @ApiModelProperty(value = "用法对比：该语法点与其他语法点的差异说明")
    private String usageComparison;

    @ApiModelProperty(value = "用法对比外文翻译")
    private List<TextTranslation> usageComparisonTranslations;

    @ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
    private String exampleSentences;

    @ApiModelProperty(value = "用法例句ID（关联example_sentence表）")
    private Long usageSentenceId;

    @ApiModelProperty(value = "组内排序权重")
    private Integer order;
}
