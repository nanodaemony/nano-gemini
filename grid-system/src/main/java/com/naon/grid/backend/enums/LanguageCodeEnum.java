package com.naon.grid.backend.enums;

/**
 * 语言Code枚举
 */
public enum LanguageCodeEnum {

    CHINESE(0, "cn","中文"),

    ENGLISH(1, "en", "英语"),

    MALASYIAN(2, "mys","马来西亚语")

    ;

    private final Integer id;

    private final String code;

    private final String description;

    LanguageCodeEnum(Integer id, String code, String description) {
        this.code = code;
        this.id = id;
        this.description = description;
    }

}
