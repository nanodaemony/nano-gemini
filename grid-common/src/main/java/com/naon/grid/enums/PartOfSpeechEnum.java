package com.naon.grid.enums;

import lombok.Getter;

/**
 * 词性枚举
 */
@Getter
public enum PartOfSpeechEnum {
    NOUN("n.", "名词"),
    VERB("v.", "动词"),
    VERB_OBJECT("v.o.", "动宾"),
    ADJECTIVE("adj.", "形容词"),
    ADVERB("adv.", "副词"),
    CONJUNCTION("conj.", "连词"),
    MEASURE_WORD("m.w.", "量词"),
    NUMERAL("num.", "数词"),
    PRONOUN("pron.", "代词"),
    PREPOSITION("prep.", "介词"),
    PARTICLE("parti.", "助词"),
    INTERJECTION("interj.", "叹词"),
    ONOMATOPOEIA("omo.", "拟声词"),
    QUANTIFIER("quant.", "数量词"),
    CHUNK("chunk", "语块"),
    MORPHEME("morpheme", "语素")
    ;

    private final String code;
    private final String desc;

    PartOfSpeechEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PartOfSpeechEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PartOfSpeechEnum pos : values()) {
            if (pos.getCode().equals(code)) {
                return pos;
            }
        }
        return null;
    }

    public static PartOfSpeechEnum fromDesc(String desc) {
        if (desc == null) {
            return null;
        }
        for (PartOfSpeechEnum pos : values()) {
            if (pos.getDesc().equals(desc)) {
                return pos;
            }
        }
        return null;
    }
}
