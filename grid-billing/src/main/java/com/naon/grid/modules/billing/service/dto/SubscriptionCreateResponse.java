package com.naon.grid.modules.billing.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionCreateResponse {
    private String subscriptionId;
    private String initialOrderId;
    private String paymentUrl;
    private String status;
}
