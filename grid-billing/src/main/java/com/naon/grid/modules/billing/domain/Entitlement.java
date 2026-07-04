package com.naon.grid.modules.billing.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "entitlement")
public class Entitlement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, unique = true, nullable = false)
    private String code;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 50)
    private String moduleCode;

    private Integer sortOrder = 0;

    private Integer status = 1;
}
