package com.naon.grid.backend.service.topic.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TopicPatternDto {

    @ApiModelProperty(value = "句式ID(新增时为null)")
    private Long id;

    @ApiModelProperty(value = "句式文本")
    private String pattern;

    @ApiModelProperty(value = "句式标签（如[\"口语\",\"正式\"]）")
    private List<String> tags;

    @ApiModelProperty(value = "句式示意图资源ID")
    private Long imageId;

    @ApiModelProperty(value = "排序权重")
    private Integer order;

    @ApiModelProperty(value = "情景对话列表")
    private List<TopicChatDto> chats;

    @ApiModelProperty(value = "AI生成的字段名列表(Java字段名驼峰)")
    private List<String> aiGeneratedFields;
}
