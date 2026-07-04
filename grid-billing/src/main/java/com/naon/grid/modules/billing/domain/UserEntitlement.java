package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "user_entitlement", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "entitlementId"})
}, indexes = {
    @Index(name = "idx_expire", columnList = "expireAt")
})
public class UserEntitlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer entitlementId;

    private LocalDateTime expireAt;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
