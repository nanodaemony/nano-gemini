package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum UserTypeEnum {
    NORMAL("NORMAL", "普通用户"),
    INSTITUTION("INSTITUTION", "机构用户"),
    AGENT("AGENT", "代理商用户");

    private final String code;
    private final String description;

    UserTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserTypeEnum fromCode(String code) {
        for (UserTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return NORMAL;
    }
}
