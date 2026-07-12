package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "billing_invoice")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, unique = true, nullable = false)
    private String invoiceNo;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    private Integer orgId;

    @Column(length = 30, nullable = false)
    private String invoiceType;

    @Column(length = 30, nullable = false)
    private String invoiceFormat;

    @Column(length = 10, nullable = false)
    private String currency;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(length = 200)
    private String buyerName;

    @Column(length = 100)
    private String buyerTaxId;

    @Column(length = 500)
    private String buyerAddress;

    @Column(length = 200)
    private String buyerEmail;

    @Column(length = 200)
    private String sellerName;

    @Column(length = 100)
    private String sellerTaxId;

    @Column(length = 500)
    private String sellerAddress;

    @Column(length = 1000)
    private String notes;

    @Column(length = 500)
    private String pdfUrl;

    @Column(length = 500)
    private String fastspringUrl;

    @Column(length = 10)
    private String region;

    @Column(length = 20, nullable = false)
    private String status;

    private LocalDateTime issuedAt;

    private LocalDateTime createTime;
}
