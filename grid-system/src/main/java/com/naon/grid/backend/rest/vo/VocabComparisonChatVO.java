package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

@Data
public class VocabComparisonChatVO {

    @ApiModelProperty(value = "对话ID")
    private Long id;

    @ApiModelProperty(value = "角色")
    private String role;

    @ApiModelProperty(value = "中文对话内容")
    private String content;

    @ApiModelProperty(value = "对话例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "对话例句翻译")
    private List<TextTranslationVO> translations;

    @ApiModelProperty(value = "对话例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "组内排序权重")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;
}
