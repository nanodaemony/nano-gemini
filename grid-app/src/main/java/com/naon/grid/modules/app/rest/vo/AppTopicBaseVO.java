package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AppTopicBaseVO {

    @ApiModelProperty(value = "话题ID")
    private Long id;

    @ApiModelProperty(value = "话题名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "翻译")
    private com.naon.grid.backend.rest.vo.TextTranslationVO translation;

    @ApiModelProperty(value = "封面图")
    private ImageVO coverImage;

    @ApiModelProperty(value = "音频")
    private AudioVO audio;

    @ApiModelProperty(value = "句式数量")
    private Integer patternCount;

    @Data
    public static class ImageVO {
        @ApiModelProperty(value = "图片URL")
        private String imageUrl;
    }

    @Data
    public static class AudioVO {
        @ApiModelProperty(value = "音频URL")
        private String audioUrl;
    }
}
