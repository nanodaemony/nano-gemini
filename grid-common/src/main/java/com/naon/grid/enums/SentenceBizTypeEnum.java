package com.naon.grid.enums;

import lombok.Getter;

/**
 * 例句业务类型枚举
 */
@Getter
public enum SentenceBizTypeEnum {

    VOCAB_SENSE_DEF_IMAGE_SENTENCE("VOCAB_SENSE_DEF_IMAGE_SENTENCE", "词汇义项释义图片例句, bizId=词汇义项ID, 一个义项最多有一条释义图片例句(也可能没有图片例句)"),

    VOCAB_SENSE_STRUCTURE_SENTENCE("VOCAB_SENSE_STRUCTURE", "词汇义项结构例句, bizId=词汇义项结构ID, 一个词汇义项的结构ID可能有多个例句"),

    CHAR_WORD_SENTENCE("CHAR_WORD_SENTENCE", "汉字组词例句, bizId=汉字组词ID, 一个汉字组词只有一个例句"),

    GRAMMAR_MEANING_SENTENCE("GRAMMAR_MEANING_SENTENCE", "语法意义例句, bizId=语法意义ID, 一个语法意义可能有多个例句"),

    GRAMMAR_STRUCTURE_SENTENCE("GRAMMAR_STRUCTURE_SENTENCE", "语法结构例句, bizId=语法结构ID, 一个语法结构可能有多个例句"),

    GRAMMAR_NOTICE_SENTENCE("GRAMMAR_NOTICE_SENTENCE", "语法注意例句, bizId=语法注意ID, 一个语法注意可能有多个例句, 目前仅有例句信息没有其他拼音、音频等资源"),
    ;

    private final String code;
    private final String description;

    SentenceBizTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
