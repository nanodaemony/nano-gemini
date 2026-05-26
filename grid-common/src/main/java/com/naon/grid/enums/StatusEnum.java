package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum StatusEnum {
    DISABLED(0, "不可用"),
    ENABLED(1, "可用");

    private final Integer code;
    private final String description;

    StatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
