package com.naon.grid.backend.service.character.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CharComparisonDto implements Serializable {

    @ApiModelProperty(value = "辨析ID")
    private Integer id;

    @ApiModelProperty(value = "汉字ID")
    private Integer charId;

    @ApiModelProperty(value = "辨析汉字")
    private String comparisonChar;

    @ApiModelProperty(value = "辨析拼音")
    private String comparisonPinyin;

    @ApiModelProperty(value = "辨析汉字翻译")
    private List<TextTranslation> comparisonCharTranslations;

    @ApiModelProperty(value = "对比辨析说明外文翻译")
    private List<TextTranslation> comparisonDescTranslations;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
