package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class AppTopicPatternVO {

    @ApiModelProperty(value = "句式文本")
    private String pattern;

    @ApiModelProperty(value = "示意图")
    private AppTopicBaseVO.ImageVO image;

    @ApiModelProperty(value = "排序")
    private Integer order;

    @ApiModelProperty(value = "情景对话列表")
    private List<AppTopicChatVO> chats;
}
