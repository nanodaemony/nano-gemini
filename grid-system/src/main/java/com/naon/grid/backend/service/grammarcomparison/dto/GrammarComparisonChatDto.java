package com.naon.grid.backend.service.grammarcomparison.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GrammarComparisonChatDto {

    @ApiModelProperty(value = "对话ID（新增时为null）")
    private Long id;

    @ApiModelProperty(value = "角色: teacher=老师, student=学生")
    private String role;

    @ApiModelProperty(value = "中文对话内容")
    private String content;

    @ApiModelProperty(value = "对话例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "对话例句翻译")
    private List<TextTranslation> translations;

    @ApiModelProperty(value = "对话例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "关联的example_sentence_id")
    private Long exampleSentenceId;

    @ApiModelProperty(value = "组内排序权重")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;
}
