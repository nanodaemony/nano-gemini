package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "entitlement_source", indexes = {
    @Index(name = "idx_user_product", columnList = "userId, productCode, status"),
    @Index(name = "idx_source", columnList = "sourceType, sourceId")
})
public class EntitlementSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 30, nullable = false)
    private String sourceType;

    @Column(length = 100)
    private String sourceId;

    @Column(length = 50, nullable = false)
    private String productCode;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    @Column(nullable = false)
    private Integer durationDays;

    private LocalDateTime expireAt;

    @Column(length = 10)
    private String region;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(length = 500)
    private String remark;

    private LocalDateTime createTime;
}
