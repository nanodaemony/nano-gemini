package com.naon.grid.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 练习题目类型枚举
 */
@Getter
public enum QuestionTypeEnum {

    SINGLE_CHOICE(1, "SINGLE_CHOICE", "单选题"),
    MULTI_CHOICE(2, "MULTI_CHOICE", "多选题"),
    TRUE_FALSE(3, "TRUE_FALSE", "判断题"),
    FILL_IN_BLANK(4, "FILL_IN_BLANK", "填空题"),
    WORD_SORT(5, "WORD_SORT", "词语排序题"),
    SHORT_ANSWER(6, "SHORT_ANSWER", "简答题"),
    REWRITE_SENTENCE(7, "REWRITE_SENTENCE", "改写句子题"),
    LISTENING(8, "LISTENING", "听力理解题"),
    PICTURE_DESCRIPTION(9, "PICTURE_DESCRIPTION", "看图写话题");

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
        throw new IllegalArgumentException("Unknown question type: " + code);
    }

    @JsonValue
    public String getCode() {
        return code;
    }

}