package com.naon.grid.backend.service.topic.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TopicDto extends BaseDTO {

    @ApiModelProperty(value = "话题ID")
    private Long id;

    @ApiModelProperty(value = "话题名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "封面图片资源ID")
    private Long coverImageId;

    @ApiModelProperty(value = "多语言翻译")
    private List<TextTranslation> translations;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    @ApiModelProperty(value = "句式列表")
    private List<TopicPatternDto> patterns;

    @ApiModelProperty(value = "句式数量")
    private Integer patternCount;

    @ApiModelProperty(value = "AI生成的字段名列表(Java字段名驼峰)")
    private List<String> aiGeneratedFields;
}
