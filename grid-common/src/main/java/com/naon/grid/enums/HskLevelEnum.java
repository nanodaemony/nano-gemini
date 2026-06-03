package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum HskLevelEnum {
    HSK1("1", "HSK1"),
    HSK2("2", "HSK2"),
    HSK3("3", "HSK3"),
    HSK4("4", "HSK4"),
    HSK5("5", "HSK5"),
    HSK6("6", "HSK6"),
    HSK789("789", "HSK7-9")
    ;

    private final String code;
    private final String description;

    HskLevelEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static HskLevelEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (HskLevelEnum level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }

    public static HskLevelEnum fromDescription(String description) {
        if (description == null) {
            return null;
        }
        for (HskLevelEnum level : values()) {
            if (level.getDescription().equalsIgnoreCase(description)) {
                return level;
            }
        }
        return null;
    }
}
