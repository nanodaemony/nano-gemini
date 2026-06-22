package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum EntitlementStatusEnum {
    ACTIVE("ACTIVE", "有效"),
    REVOKED("REVOKED", "已撤销"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String description;

    EntitlementStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
