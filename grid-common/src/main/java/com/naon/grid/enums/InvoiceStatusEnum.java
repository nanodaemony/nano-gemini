package com.naon.grid.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InvoiceStatusEnum {
    DRAFT("DRAFT", "草稿"),
    ISSUED("ISSUED", "已开具"),
    VOIDED("VOIDED", "已作废");

    private final String code;
    private final String description;
}
