package com.naon.grid.enums;

import lombok.Getter;

/**
 * 词汇关联类型枚举
 */
@Getter
public enum VocabRelationTypeEnum {

    SYNONYMS_WORDS("SYNONYMS_WORDS", "近义词"),

    ANTONYMS_WORDS("ANTONYMS", "反义词"),

    SEQUENTIAL_WORDS("SEQUENTIAL_WORDS", "正序词"),

    REVERSE_SEQUENTIAL_WORDS("REVERSE_SEQUENTIAL_WORDS", "逆序词"),

    JUMBLED_WORDS("JUMBLED_WORDS", "乱序词")
    ;

    private final String code;
    private final String description;

    VocabRelationTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
