package com.naon.grid.modules.billing.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "region_pricing", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "region", "billingCycle"})
})
public class RegionPricing extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(length = 10, nullable = false)
    private String region;

    @Column(length = 20, nullable = false)
    private String billingCycle;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(length = 10, nullable = false)
    private String currency;

    private Integer status = 1;
}
