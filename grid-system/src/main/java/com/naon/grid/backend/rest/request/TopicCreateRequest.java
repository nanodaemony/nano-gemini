package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class TopicCreateRequest {

    @NotBlank
    @ApiModelProperty(value = "话题名称", required = true)
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "封面图片资源ID")
    private Long coverImageId;

    @ApiModelProperty(value = "多语言翻译")
    private List<TextTranslationRequest> translations;

    @Valid
    @ApiModelProperty(value = "句式列表")
    private List<TopicPatternRequest> patterns;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;

    @Data
    public static class TopicPatternRequest {

        @NotBlank
        @ApiModelProperty(value = "句式文本", required = true)
        private String pattern;

        @ApiModelProperty(value = "句式示意图资源ID")
        private Long imageId;

        @ApiModelProperty(value = "排序权重")
        private Integer order;

        @Valid
        @ApiModelProperty(value = "情景对话列表")
        private List<TopicChatRequest> chats;

        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
    }

    @Data
    public static class TopicChatRequest {

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
