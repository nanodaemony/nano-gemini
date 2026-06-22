package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum ProductTypeEnum {
    PLUS("PLUS", "全平台会员"),
    SINGLE_MODULE("SINGLE_MODULE", "单模块"),
    INSTITUTION("INSTITUTION", "机构套餐"),
    ENTERPRISE("ENTERPRISE", "企业定制");

    private final String code;
    private final String description;

    ProductTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
