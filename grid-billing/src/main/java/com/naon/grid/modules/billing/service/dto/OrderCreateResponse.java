package com.naon.grid.modules.billing.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class OrderCreateResponse {
    private String orderNo;
    private String productCode;
    private String billingCycle;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String redirectUrl; // 支付跳转链接（第一期返回null）
}
