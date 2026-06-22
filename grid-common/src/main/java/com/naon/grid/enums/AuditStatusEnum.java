package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum AuditStatusEnum {
    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String description;

    AuditStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
