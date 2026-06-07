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
public class CharDiscriminationDto implements Serializable {

    @ApiModelProperty(value = "辨析唯一ID")
    private Integer id;

    @ApiModelProperty(value = "汉字ID")
    private Integer charId;

    @ApiModelProperty(value = "辨析汉字")
    private String discrimChar;

    @ApiModelProperty(value = "辨析拼音")
    private String discrimPinyin;

    @ApiModelProperty(value = "辨析汉字翻译")
    private List<TextTranslation> discrimCharTranslations;

    @ApiModelProperty(value = "对比翻译")
    private List<TextTranslation> comparisonTranslations;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer discriminationOrder;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
