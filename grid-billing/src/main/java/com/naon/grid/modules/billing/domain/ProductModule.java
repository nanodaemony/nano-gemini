package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "product_module", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "moduleCode"})
})
public class ProductModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(length = 50, nullable = false)
    private String moduleCode;
}
