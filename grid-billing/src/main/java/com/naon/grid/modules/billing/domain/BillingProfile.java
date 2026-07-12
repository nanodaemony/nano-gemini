package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "billing_profile")
public class BillingProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Long userId;

    private Integer orgId;

    @Column(length = 200)
    private String companyName;

    @Column(length = 100)
    private String taxId;

    @Column(length = 500)
    private String billingAddress;

    @Column(length = 200)
    private String billingEmail;

    @Column(length = 50)
    private String billingPhone;

    @Column(length = 10)
    private String region;

    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
