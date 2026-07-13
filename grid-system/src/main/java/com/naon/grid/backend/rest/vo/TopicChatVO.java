package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class TopicChatVO {

    @ApiModelProperty(value = "对话ID")
    private Long id;

    @ApiModelProperty(value = "角色")
    private String role;

    @ApiModelProperty(value = "对话内容")
    private String content;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "翻译列表")
    private List<TextTranslationVO> translations;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "排序")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "AI已审核的字段名列表")
    private List<String> aiReviewedFields;
}
