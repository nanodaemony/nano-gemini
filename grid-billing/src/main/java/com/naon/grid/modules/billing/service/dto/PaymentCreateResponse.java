package com.naon.grid.modules.billing.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCreateResponse {
    private String transactionId;
    private String paymentUrl;
    private String status;
}
