package com.naon.grid.modules.system.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "grid_agent")
public class GridAgent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 100)
    private String contactName;

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 50)
    private String contactPhone;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(length = 32, unique = true, nullable = false)
    private String referralCode;

    private Integer status = 1;

    @Column(length = 20, nullable = false)
    private String auditStatus = "PENDING";
}
