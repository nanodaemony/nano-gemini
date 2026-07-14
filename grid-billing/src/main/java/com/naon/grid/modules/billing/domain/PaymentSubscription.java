package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "payment_subscription", indexes = {
    @Index(name = "idx_user", columnList = "userId"),
    @Index(name = "idx_channel_sub", columnList = "channel, channelSubId")
})
public class PaymentSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 50, nullable = false)
    private String productCode;

    @Column(length = 30, nullable = false)
    private String channel = "FASTSPRING";

    @Column(length = 200)
    private String channelSubId;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    private LocalDateTime createTime;

    private LocalDateTime cancelAt;
}
