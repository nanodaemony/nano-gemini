package com.naon.grid.enums;

import lombok.Getter;

/**
 * 词性枚举
 */
@Getter
public enum PartOfSpeechEnum {
    NOUN("n.", "名词", "noun"),
    VERB("v.", "动词", "verb"),
    VERB_OBJECT("v.o.", "动宾", "verb-object"),
    AUXILIARY_VERB("aux.v.", "助动词", "auxiliary verb"),
    ADJECTIVE("adj.", "形容词", "adjective"),
    ADVERB("adv.", "副词", "adverb"),
    CONJUNCTION("conj.", "连词", "conjunction"),
    MEASURE_WORD("m.w.", "量词", "measure word"),
    NUMERAL("num.", "数词", "numeral"),
    QUANTIFIER("quant.", "数量词", "quantity"),
    PRONOUN("pron.", "代词", "pronoun"),
    PREPOSITION("prep.", "介词", "preposition"),
    PARTICLE("parti.", "助词", "particle"),
    MODAL_PARTICLE("m.p.", "语气词", "modal particle"),
    INTERJECTION("interj.", "叹词", "interjection"),
    ONOMATOPOEIA("omo.", "拟声词", "onomatopoeia"),
    IDIOM("idiom", "成语、俗语、惯用语等", "idiom"),
    CHUNK("chunk", "语块", "chunk"),
    MORPHEME("morpheme", "语素", "morpheme")
    ;

    private final String code;
    private final String desc;
    private final String fullName;

    PartOfSpeechEnum(String code, String desc, String fullName) {
        this.code = code;
        this.desc = desc;
        this.fullName = fullName;
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
