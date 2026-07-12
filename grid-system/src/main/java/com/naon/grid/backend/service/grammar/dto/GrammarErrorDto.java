package com.naon.grid.backend.service.grammar.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class GrammarErrorDto implements Serializable {

    @ApiModelProperty(value = "语法偏误ID")
    private Long id;

    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @ApiModelProperty(value = "偏误描述")
    private String errorContent;

    @ApiModelProperty(value = "偏误分析")
    private String errorAnalysis;

    @ApiModelProperty(value = "偏误分析外文翻译")
    private List<TextTranslation> errorAnalysisTranslations;

    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "有效状态")
    private Integer status;
}
