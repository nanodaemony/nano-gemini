package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 文本翻译Request
 */
@Getter
@Setter
public class TextTranslationRequest implements Serializable {

    @ApiModelProperty(value = "语种, 参考LanguageCodeEnum的code字段")
    private String language;

    @ApiModelProperty(value = "翻译文案")
    private String translation;

}
