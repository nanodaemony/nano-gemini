package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 例句信息VO
 */
@Getter
@Setter
public class AppExampleSentenceVO implements Serializable {

    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "例句外文翻译")
    private TextTranslationVO translation;

    @ApiModelProperty(value = "例句图片ID")
    private AppCharCharacterDetailVO.ImageVO image;

    @ApiModelProperty(value = "排序权重")
    private Integer order;

}