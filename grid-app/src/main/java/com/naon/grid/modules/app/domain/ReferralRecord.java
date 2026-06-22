package com.naon.grid.modules.app.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "referral_record")
public class ReferralRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long referrerId;

    @Column(length = 20, nullable = false)
    private String referrerType;

    private Long referredId;

    @Column(length = 32, nullable = false)
    private String referralCode;

    private Long orderId;

    @Column(length = 20)
    private String rewardStatus = "PENDING";

    @Column(precision = 12, scale = 2)
    private BigDecimal rewardAmount;

    @Column(length = 20)
    private String rewardType;

    private LocalDateTime createTime;
    private LocalDateTime settleTime;
}
