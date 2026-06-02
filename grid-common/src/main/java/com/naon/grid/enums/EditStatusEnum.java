package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum EditStatusEnum {
    DRAFT("draft", "草稿"),
    REVIEWED("reviewed", "已审核"),
    PUBLISHED("published", "已发布");

    private final String code;
    private final String description;

    EditStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
