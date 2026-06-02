package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum PublishStatusEnum {
    UNPUBLISHED("unpublished", "未发布"),
    PUBLISHED("published", "已发布");

    private final String code;
    private final String description;

    PublishStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
