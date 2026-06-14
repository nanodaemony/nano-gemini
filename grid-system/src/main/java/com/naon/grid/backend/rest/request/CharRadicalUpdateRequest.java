package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class CharRadicalUpdateRequest {

    @ApiModelProperty(value = "部首名称")
    private String radical;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "关联部首ID")
    private Long relationId;

    @ApiModelProperty(value = "演化解说")
    private String evolutionDesc;

    @ApiModelProperty(value = "演化解说外文翻译")
    private List<TextTranslationRequest> evolutionDescTranslations;

    @ApiModelProperty(value = "演化解说图片（路径或资源ID）")
    private String evolutionImageId;
}
