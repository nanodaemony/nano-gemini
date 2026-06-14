package com.naon.grid.enums;

import lombok.Getter;

/**
 * 词汇书类型枚举
 */
@Getter
public enum VocabBookTypeEnum {

    HSK("HSK", "HSK词汇"),

    ;

    private final String code;
    private final String description;

    VocabBookTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
