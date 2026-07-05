package com.naon.grid.modules.app.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
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

    @Column
    private Integer referrerOrgId;

    @Column
    private Long referredId;

    @Column
    private Integer referredOrgId;

    @Column(length = 32, nullable = false)
    private String referralCode;

    @Column(length = 30, nullable = false)
    private String eventType;

    @Column
    private Long orderId;

    @Column(length = 20)
    private String rewardStatus = "PENDING";

    @Column
    private LocalDateTime settleTime;

    @Column
    private LocalDateTime createTime;
}
