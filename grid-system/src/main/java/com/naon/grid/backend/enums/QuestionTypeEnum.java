package com.naon.grid.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 练习题目类型枚举
 */
public enum QuestionTypeEnum {

    SINGLE_CHOOSE(1, "SINGLE_CHOOSE", "单选题"),

    MULTI_CHOOSE(2, "MULTI_CHOOSE", "多选题"),

    ;

    private final Integer id;

    private final String code;

    private final String description;

    QuestionTypeEnum(Integer id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static QuestionTypeEnum fromCode(String code) {
        for (QuestionTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown audio source type: " + code);
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
