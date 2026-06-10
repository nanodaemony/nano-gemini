package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 例句内容请求
 */
@Data
public class SentenceContentRequest implements Serializable {

    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "例句音频资源ID")
    private long audioId;

    @ApiModelProperty(value = "例句外文翻译")
    private List<TextTranslationRequest> translations;

    @ApiModelProperty(value = "例句图片ID")
    private long imageId;

    @ApiModelProperty(value = "排序权重")
    private int order;

}
