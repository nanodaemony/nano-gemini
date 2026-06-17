package com.naon.grid.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "dynamic_config")
@Getter
@Setter
public class DynamicConfig extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String namespace;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "config_key", nullable = false, length = 200)
    private String configKey;

    @Column(length = 2000)
    private String value;

    @Column(length = 500)
    private String description;

    /**
     * 状态：1=启用，0=禁用（软删除）
     */
    private Integer status = 1;
}
