package com.naon.grid.domain.common;

import lombok.Data;

/**
 * 文本翻译对象
 */
@Data
public class TextTranslation {

    /**
     * 语种枚举, 参考枚举：LanguageCodeEnum 的 code 字段
     */
    private String language;

    /**
     * 翻译文案
     */
    private String translation;

}
