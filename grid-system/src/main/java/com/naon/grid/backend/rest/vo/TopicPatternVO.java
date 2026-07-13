package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class TopicPatternVO {

    @ApiModelProperty(value = "句式ID")
    private Long id;

    @ApiModelProperty(value = "句式文本")
    private String pattern;

    @ApiModelProperty(value = "句式标签")
    private List<String> tags;

    @ApiModelProperty(value = "示意图资源ID")
    private Long imageId;

    @ApiModelProperty(value = "排序")
    private Integer order;

    @ApiModelProperty(value = "情景对话列表")
    private List<TopicChatVO> chats;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "AI已审核的字段名列表")
    private List<String> aiReviewedFields;
}
