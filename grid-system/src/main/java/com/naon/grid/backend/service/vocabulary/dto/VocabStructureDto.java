package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabStructureDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "自增ID, 结构搭配ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "所属义项ID")
    private Integer senseId;

    @ApiModelProperty(value = "结构搭配文案")
    private String pattern;

    @ApiModelProperty(value = "结构搭配释义")
    private String patternDef;

    @ApiModelProperty(value = "结构搭配释义外文翻译列表")
    private List<TextTranslation> patternDefTranslations;

    @ApiModelProperty(value = "搭配排序权重")
    private Integer structureOrder;

    @ApiModelProperty(value = "结构例句ID列表（JSON 数组格式）")
    private String sentenceIds;

    @ApiModelProperty(value = "结构例句列表（通过通用例句表存储）")
    private List<ExampleSentenceDto> structureSentences;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
