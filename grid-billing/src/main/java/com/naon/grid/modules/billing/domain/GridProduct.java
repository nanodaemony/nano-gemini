package com.naon.grid.modules.billing.domain;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.ProductTypeEnum;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "grid_product")
public class GridProduct extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, length = 50, nullable = false)
    private String code;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 30, nullable = false)
    private String productType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer sortOrder;

    private Integer status = 1;
}
