package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum BillingCycleEnum {
    MONTHLY("MONTHLY", "月度", 30),
    QUARTERLY("QUARTERLY", "季度", 90),
    YEARLY("YEARLY", "年度", 365);

    private final String code;
    private final String description;
    private final int days;

    BillingCycleEnum(String code, String description, int days) {
        this.code = code;
        this.description = description;
        this.days = days;
    }

    public static BillingCycleEnum fromCode(String code) {
        for (BillingCycleEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
