package com.naon.grid.enums;

import lombok.Getter;

/**
 * 例句业务类型枚举
 */
@Getter
public enum SentenceBizTypeEnum {

    VOCAB_COMPARISON_CHAT("VOCAB_COMPARISON_CHAT", "词汇辨析情景对话（保留仅作标记，不再用于查询）"),
    ;

    private final String code;
    private final String description;

    SentenceBizTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
