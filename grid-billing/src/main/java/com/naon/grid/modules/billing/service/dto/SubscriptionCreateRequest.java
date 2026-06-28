package com.naon.grid.modules.billing.service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class SubscriptionCreateRequest {
    private String orderNo;
    private String productCode;
    private BigDecimal amount;
    private String currency;
    private String billingCycle;
    private String description;
    private String returnUrl;
    private String cancelUrl;
}
