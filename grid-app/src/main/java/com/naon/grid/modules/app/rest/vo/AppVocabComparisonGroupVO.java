package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

@Data
public class AppVocabComparisonGroupVO {

    @ApiModelProperty(value = "辨析组ID")
    private Long groupId;

    @ApiModelProperty(value = "辨析组标识")
    private String groupKey;

    @ApiModelProperty(value = "条目列表")
    private List<AppItemVO> items;

    @ApiModelProperty(value = "情景对话列表")
    private List<AppChatVO> chats;

    @Data
    public static class AppItemVO {
        @ApiModelProperty(value = "词汇ID")
        private Long wordId;

        @ApiModelProperty(value = "词汇词头")
        private String word;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "用法对比")
        private String usageComparison;

        @ApiModelProperty(value = "用法对比外文翻译")
        private List<TextTranslationVO> usageComparisonTranslations;

        @ApiModelProperty(value = "通用用法")
        private String commonUsage;

        @ApiModelProperty(value = "通用用法外文翻译")
        private List<TextTranslationVO> commonUsageTranslations;

        @ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
        private String exampleSentences;

        @ApiModelProperty(value = "组内排序权重")
        private Integer order;
    }

    @Data
    public static class AppChatVO {
        @ApiModelProperty(value = "角色")
        private String role;

        @ApiModelProperty(value = "中文对话内容")
        private String content;

        @ApiModelProperty(value = "拼音")
        private String pinyin;

        @ApiModelProperty(value = "翻译")
        private List<TextTranslationVO> translations;

        @ApiModelProperty(value = "音频URL")
        private String audioUrl;

        @ApiModelProperty(value = "组内排序权重")
        private Integer order;
    }
}
