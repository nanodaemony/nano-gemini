package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum BizModuleEnum {
    VOCAB("VOCAB", "词汇"),
    GRAMMAR("GRAMMAR", "语法"),
    CHARACTER("CHARACTER", "汉字"),
    CONFUSING_WORDS("CONFUSING_WORDS", "易混淆词辨析"),
    CULTURE("CULTURE", "文化"),
    TOPIC("TOPIC", "话题");

    private final String code;
    private final String name;

    BizModuleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BizModuleEnum fromCode(String code) {
        for (BizModuleEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
