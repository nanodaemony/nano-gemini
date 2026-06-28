package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "grid_order")
public class GridOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, unique = true, nullable = false)
    private String orderNo;

    @Column(nullable = false)
    private Long userId;

    private Integer orgId;

    @Column(length = 50, nullable = false)
    private String productCode;

    @Column(length = 10, nullable = false)
    private String region;

    @Column(length = 20, nullable = false)
    private String billingCycle;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 10, nullable = false)
    private String currency;

    @Column(length = 30, nullable = false)
    private String status = "PENDING";

    @Column(length = 30)
    private String paymentMethod;

    private LocalDateTime paidAt;

    private LocalDateTime expireAt;

    private LocalDateTime createTime;

    @Column(length = 30)
    private String channel;

    @Column(length = 200)
    private String channelOrderId;

    @Column(length = 200)
    private String channelSubId;

    @Column(length = 64)
    private String invoiceNo;
}
