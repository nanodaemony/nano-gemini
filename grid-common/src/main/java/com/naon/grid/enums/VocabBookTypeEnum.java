package com.naon.grid.enums;

import lombok.Getter;

/**
 * 汉字书类型枚举
 */
@Getter
public enum CharBookTypeEnum {

    HANDWRITING("HANDWRITING", "手写汉字书"),
    ;

    private final String code;
    private final String description;

    CharBookTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
