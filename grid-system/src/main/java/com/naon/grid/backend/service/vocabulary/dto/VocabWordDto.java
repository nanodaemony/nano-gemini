package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabWordDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇")
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "标准拼音（含声调）")
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String hskLevel;

    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseDto> senses;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;
}
