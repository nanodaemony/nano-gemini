package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "user_entitlement_record", indexes = {
    @Index(name = "idx_user_entitlement", columnList = "userId, entitlementId"),
    @Index(name = "idx_source", columnList = "sourceType, sourceId")
})
public class UserEntitlementRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer entitlementId;

    @Column(length = 30, nullable = false)
    private String sourceType;

    @Column(length = 100)
    private String sourceId;

    @Column(nullable = false)
    private Integer durationDays;

    private LocalDateTime expireAt;

    @Column(length = 10)
    private String region;

    @Column(length = 500)
    private String remark;

    private LocalDateTime createTime;
}
