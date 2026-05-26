package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum HskLevelEnum {
    HSK1(1, "HSK1"),
    HSK2(2, "HSK2"),
    HSK3(3, "HSK3"),
    HSK4(4, "HSK4"),
    HSK5(5, "HSK5"),
    HSK6(6, "HSK6"),
    HSK7(7, "HSK7"),
    HSK8(8, "HSK8"),
    HSK9(9, "HSK9");

    private final Integer code;
    private final String description;

    HskLevelEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static HskLevelEnum fromCode(Integer code) {
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
