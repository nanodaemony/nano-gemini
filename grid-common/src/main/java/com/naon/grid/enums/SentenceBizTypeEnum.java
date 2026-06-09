package com.naon.grid.enums;

import lombok.Getter;

/**
 * 例句业务类型枚举
 */
@Getter
public enum SentenceBizTypeEnum {

    VOCAB_SENSE_DEF_IMAGE_SENTENCE("VOCAB_SENSE_DEF_IMAGE_SENTENCE", "词汇义项释义图片例句, bizId=词汇义项ID, 一个义项最多有一条释义图片例句(也可能没有图片例句)"),

    VOCAB_SENSE_STRUCTURE_SENTENCE("VOCAB_SENSE_STRUCTURE", "词汇义项结构例句, bizId=词汇义项结构ID, 一个词汇义项的结构ID可能有多个例句"),

    CHAR_WORD_SENTENCE("CHAR_WORD_SENTENCE", "汉字组词例句, bizId=汉字组词ID, 一个汉字组成只有一个例句")

    ;

    private final String code;
    private final String description;

    SentenceBizTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
