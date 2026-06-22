package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    PENDING("PENDING", "待支付"),
    PAID("PAID", "已支付"),
    REFUNDING("REFUNDING", "退款中"),
    REFUNDED("REFUNDED", "已退款"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String description;

    OrderStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
