package com.naon.grid.modules.billing.service.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TaxInfo {
    /** Region code A/B/C/D/E */
    private String region;
    /** Tax jurisdiction (country code or "GLOBAL") */
    private String jurisdiction;
    /** Tax type: VAT/GST/SALES_TAX/DIGITAL_SERVICE_TAX */
    private String taxType;
    /** Tax rate as decimal, e.g. 0.20 for 20% */
    private BigDecimal taxRate;
    /** Tax amount */
    private BigDecimal taxAmount;
}
