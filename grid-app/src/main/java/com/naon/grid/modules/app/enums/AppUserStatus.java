package com.naon.grid.modules.app.enums;

import lombok.Getter;

@Getter
public enum AppUserStatus {
    DISABLED(0, "禁用"),
    ENABLED(1, "正常");

    private final Integer code;
    private final String description;

    AppUserStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
