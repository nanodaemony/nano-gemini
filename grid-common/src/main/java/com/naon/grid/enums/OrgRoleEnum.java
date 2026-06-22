package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum OrgRoleEnum {
    ADMIN("ADMIN", "机构管理员"),
    MEMBER("MEMBER", "机构成员");

    private final String code;
    private final String description;

    OrgRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
