package com.naon.grid.modules.billing.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundResponse {
    private String refundId;
    private String status;
}
