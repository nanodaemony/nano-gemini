package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum SourceTypeEnum {
    TRIAL("TRIAL", "注册试用"),
    PURCHASE("PURCHASE", "购买"),
    INSTITUTION("INSTITUTION", "机构授权"),
    REFERRAL("REFERRAL", "推荐奖励"),
    ADMIN_GRANT("ADMIN_GRANT", "后台发放");

    private final String code;
    private final String description;

    SourceTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
