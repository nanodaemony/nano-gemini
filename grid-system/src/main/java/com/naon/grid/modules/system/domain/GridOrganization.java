package com.naon.grid.modules.system.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "grid_organization")
public class GridOrganization extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 200)
    private String nameEn;

    @Column(length = 20, nullable = false)
    private String orgType;

    @Column(length = 100)
    private String contactName;

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 50)
    private String contactPhone;

    @Column(length = 100)
    private String adminPassword;

    @Column(length = 500)
    private String rejectReason;

    @Column(length = 32)
    private String referredBy;

    @Column(length = 100)
    private String country;

    @Column(length = 10)
    private String region;

    private Integer status = 1;

    @Column(length = 20, nullable = false)
    private String auditStatus = "PENDING";

    private Integer maxMembers = 0;
    private Integer maxAdmins = 0;
    private Integer currentMembers = 0;
    private LocalDateTime expireTime;
}
