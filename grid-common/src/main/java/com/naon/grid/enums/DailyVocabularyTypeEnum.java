package com.naon.grid.enums;

import lombok.Getter;

/**
 * 每日一词类型枚举
 */
@Getter
public enum DailyVocabularyTypeEnum {

    IDIOM("IDIOM", "成语"),
    PROVERB("PROVERB", "谚语"),
    COLLOQUIALISM("COLLOQUIALISM", "惯用语"),
    XIEHOUYU("XIEHOUYU", "歇后语"),
    NEOLOGISM("NEOLOGISM", "新词新语");

    private final String code;
    private final String description;

    DailyVocabularyTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DailyVocabularyTypeEnum fromCode(String code) {
        for (DailyVocabularyTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
