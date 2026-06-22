package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum RegionEnum {
    A("A", "北美、西欧、北欧"),
    B("B", "日韩澳新、中东高收入、新加坡及港澳台"),
    C("C", "中国大陆"),
    D("D", "东南亚(除新加坡)、东欧、拉美"),
    E("E", "非洲、南亚、中亚及部分低收入地区");

    private final String code;
    private final String description;

    RegionEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RegionEnum fromCode(String code) {
        for (RegionEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
