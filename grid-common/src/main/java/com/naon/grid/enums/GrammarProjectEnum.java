package com.naon.grid.enums;

import lombok.Getter;

/**
 * 语法项目枚举（12类）
 */
@Getter
public enum GrammarProjectEnum {
    MORPHEME("morpheme", "语素"),
    WORD_CLASS("word_class", "词类"),
    PHRASE("phrase", "短语"),
    FIXED_FORMAT("fixed_format", "固定格式"),
    SENTENCE_COMPONENT("sentence_component", "句子成分"),
    SENTENCE_TYPE("sentence_type", "句子的类型"),
    ACTION_ASPECT("action_aspect", "动作的态"),
    SPECIAL_EXPRESSION("special_expression", "特殊表达法"),
    EMPHASIS_METHOD("emphasis_method", "强调的方法"),
    QUESTION_METHOD("question_method", "提问的方法"),
    SPOKEN_FORMAT("spoken_format", "口语格式"),
    SENTENCE_GROUP("sentence_group", "句群")
    ;

    private final String code;
    private final String desc;

    GrammarProjectEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static GrammarProjectEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (GrammarProjectEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static GrammarProjectEnum fromDesc(String desc) {
        if (desc == null) {
            return null;
        }
        for (GrammarProjectEnum item : values()) {
            if (item.getDesc().equals(desc)) {
                return item;
            }
        }
        return null;
    }
}
