package com.naon.grid.backend.rest.vo;

import com.naon.grid.backend.rest.request.TextTranslationRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 例句信息VO
 */
@Getter
@Setter
public class ExampleSentenceVO implements Serializable {

    @ApiModelProperty(value = "例句ID")
    private long id;

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
