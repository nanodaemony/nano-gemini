package com.naon.grid.modules.app.enums;

import lombok.Getter;

/**
 * 收藏业务类型枚举
 */
@Getter
public enum CollectionBizTypeEnum {

    CHARACTER("CHARACTER", "汉字"),
    VOCABULARY("VOCABULARY", "词汇"),
    RADICAL("RADICAL", "部首"),
    GRAMMAR("GRAMMAR", "语法"),
    GRAMMAR_COMPARISON("GRAMMAR_COMPARISON", "语法辨析"),
    VOCAB_COMPARISON("VOCAB_COMPARISON", "词汇辨析");

    private final String code;
    private final String description;

    CollectionBizTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CollectionBizTypeEnum fromCode(String code) {
        for (CollectionBizTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
