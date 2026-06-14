package com.naon.grid.enums;

import lombok.Getter;

/**
 * 语法类别枚举
 */
@Getter
public enum GrammarCategoryEnum {
    // ========== 语素 ==========
    PREFIX("prefix", "前缀", GrammarProjectEnum.MORPHEME),
    SUFFIX("suffix", "后缀", GrammarProjectEnum.MORPHEME),
    QUASI_PREFIX("quasi_prefix", "类前缀", GrammarProjectEnum.MORPHEME),
    QUASI_SUFFIX("quasi_suffix", "类后缀", GrammarProjectEnum.MORPHEME),

    // ========== 词类 ==========
    NOUN("noun", "名词", GrammarProjectEnum.WORD_CLASS),
    VERB("verb", "动词", GrammarProjectEnum.WORD_CLASS),
    ADJECTIVE("adjective", "形容词", GrammarProjectEnum.WORD_CLASS),
    NUMERAL("numeral", "数词", GrammarProjectEnum.WORD_CLASS),
    MEASURE_WORD("measure_word", "量词", GrammarProjectEnum.WORD_CLASS),
    PRONOUN("pronoun", "代词", GrammarProjectEnum.WORD_CLASS),
    ADVERB("adverb", "副词", GrammarProjectEnum.WORD_CLASS),
    PREPOSITION("preposition", "介词", GrammarProjectEnum.WORD_CLASS),
    CONJUNCTION("conjunction", "连词", GrammarProjectEnum.WORD_CLASS),
    PARTICLE("particle", "助词", GrammarProjectEnum.WORD_CLASS),
    INTERJECTION("interjection", "叹词", GrammarProjectEnum.WORD_CLASS),
    ONOMATOPOEIA("onomatopoeia", "拟声词", GrammarProjectEnum.WORD_CLASS),

    // ========== 短语 ==========
    STRUCTURE_TYPE("structure_type", "结构类型", GrammarProjectEnum.PHRASE),
    FUNCTION_TYPE("function_type", "功能类型", GrammarProjectEnum.PHRASE),
    FIXED_PHRASE("fixed_phrase", "固定短语", GrammarProjectEnum.PHRASE),

    // ========== 句子成分 ==========
    SUBJECT("subject", "主语", GrammarProjectEnum.SENTENCE_COMPONENT),
    PREDICATE("predicate", "谓语", GrammarProjectEnum.SENTENCE_COMPONENT),
    OBJECT("object", "宾语", GrammarProjectEnum.SENTENCE_COMPONENT),
    ATTRIBUTIVE("attributive", "定语", GrammarProjectEnum.SENTENCE_COMPONENT),
    ADVERBIAL("adverbial", "状语", GrammarProjectEnum.SENTENCE_COMPONENT),
    COMPLEMENT("complement", "补语", GrammarProjectEnum.SENTENCE_COMPONENT),

    // ========== 句子的类型 ==========
    SENTENCE_PATTERN("sentence_pattern", "句型", GrammarProjectEnum.SENTENCE_TYPE),
    SENTENCE_CATEGORY("sentence_category", "句类", GrammarProjectEnum.SENTENCE_TYPE),
    SPECIAL_SENTENCE_PATTERN("special_sentence_pattern", "特殊句型", GrammarProjectEnum.SENTENCE_TYPE),
    COMPLEX_SENTENCE("complex_sentence", "复句", GrammarProjectEnum.SENTENCE_TYPE),

    // ========== 特殊表达法 ==========
    NUMBER_EXPRESSION("number_expression", "数的表示法", GrammarProjectEnum.SPECIAL_EXPRESSION),
    TIME_EXPRESSION("time_expression", "时间表示法", GrammarProjectEnum.SPECIAL_EXPRESSION),
    ;

    private final String code;
    private final String desc;
    private final GrammarProjectEnum project;

    GrammarCategoryEnum(String code, String desc, GrammarProjectEnum project) {
        this.code = code;
        this.desc = desc;
        this.project = project;
    }

    public static GrammarCategoryEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (GrammarCategoryEnum item : values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    public static GrammarCategoryEnum fromDesc(String desc) {
        if (desc == null) {
            return null;
        }
        for (GrammarCategoryEnum item : values()) {
            if (item.getDesc().equals(desc)) {
                return item;
            }
        }
        return null;
    }
}
