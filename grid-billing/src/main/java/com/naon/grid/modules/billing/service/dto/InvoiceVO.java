package com.naon.grid.modules.billing.service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceVO {
    private String invoiceNo;
    private String orderNo;
    private String productCode;
    private String invoiceFormat;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String buyerName;
    private String buyerTaxId;
    private String buyerEmail;
    private String status;
    private String pdfUrl;
    private String fastspringUrl;
    private LocalDateTime issuedAt;
}
