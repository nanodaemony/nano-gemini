package com.naon.grid.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 练习题业务标签枚举
 */
@Getter
public enum QuestionBizTagEnum {

    CHARACTER("CHARACTER", "汉字"),
    VOCABULARY("VOCABULARY", "词汇"),
    GRAMMAR("GRAMMAR", "语法"),
    TOPIC("TOPIC", "话题"),
    CULTURE("CULTURE", "文化"),
    CHARACTER_CHALLENGE("CHARACTER_CHALLENGE", "汉字大挑战"),
    VOCABULARY_CHALLENGE("VOCABULARY_CHALLENGE", "词汇大挑战");

    private final String code;
    private final String description;

    QuestionBizTagEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static QuestionBizTagEnum fromCode(String code) {
        for (QuestionBizTagEnum tag : values()) {
            if (tag.code.equalsIgnoreCase(code)) {
                return tag;
            }
        }
        throw new IllegalArgumentException("Unknown question biz tag: " + code);
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
