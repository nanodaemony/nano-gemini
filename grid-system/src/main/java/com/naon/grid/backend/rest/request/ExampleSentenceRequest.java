package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 例句内容请求
 */
@Data
public class ExampleSentenceRequest implements Serializable {

    @ApiModelProperty(value = "例句ID, 新增时不传, 更新时传")
    private Long id;

    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "例句外文翻译")
    private List<TextTranslationRequest> translations;

    @ApiModelProperty(value = "例句图片ID")
    private Long imageId;

    @ApiModelProperty(value = "排序权重")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;

}
