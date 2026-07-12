package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class GrammarComparisonGroupCreateRequest {

    @NotBlank
    @ApiModelProperty(value = "辨析组标识", required = true)
    private String groupKey;

    @ApiModelProperty(value = "练习题ID列表JSON")
    private String exerciseQuestionIds;

    @ApiModelProperty(value = "组排序权重")
    private Integer groupOrder;

    @Valid
    @ApiModelProperty(value = "条目列表")
    private List<GrammarItemRequest> items;

    @Valid
    @ApiModelProperty(value = "情景对话列表")
    private List<GrammarChatRequest> chats;

    @Data
    public static class GrammarItemRequest {
        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "语法点名称")
        private String grammarName;

        @ApiModelProperty(value = "用法对比：该语法点与其他语法点的差异说明")
        private String usageComparison;

        @ApiModelProperty(value = "用法对比外文翻译")
        private List<TextTranslationRequest> usageComparisonTranslations;

        @ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
        private String exampleSentences;

        @ApiModelProperty(value = "用法例句ID（关联example_sentence表）")
        private Long usageSentenceId;

        @ApiModelProperty(value = "组内排序权重")
        private Integer order;

        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
    }

    @Data
    public static class GrammarChatRequest {
        @NotBlank
        @ApiModelProperty(value = "角色: teacher=老师, student=学生", required = true)
        private String role;

        @NotBlank
        @ApiModelProperty(value = "中文对话内容", required = true)
        private String content;

        @ApiModelProperty(value = "对话例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "对话例句翻译")
        private List<TextTranslationRequest> translations;

        @ApiModelProperty(value = "对话例句音频资源ID")
        private Long audioId;

        @ApiModelProperty(value = "组内排序权重")
        private Integer order;

        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
    }
}
