package com.naon.grid.backend.service.charradical.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CharRadicalDto extends BaseDTO {

    @ApiModelProperty(value = "部首ID")
    private Long id;

    @ApiModelProperty(value = "部首名称")
    private String radical;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "关联部首ID")
    private Long relationId;

    @ApiModelProperty(value = "演化解说")
    private String evolutionDesc;

    @ApiModelProperty(value = "演化解说外文翻译")
    private List<TextTranslation> evolutionDescTranslations;

    @ApiModelProperty(value = "演化解说图片（路径或资源ID）")
    private String evolutionImageId;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核, published=已发布")
    private String editStatus;
}
