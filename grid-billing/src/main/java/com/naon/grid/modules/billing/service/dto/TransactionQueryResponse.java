package com.naon.grid.modules.billing.service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionQueryResponse {
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime paidAt;
}
