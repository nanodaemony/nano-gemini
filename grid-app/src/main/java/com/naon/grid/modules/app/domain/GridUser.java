
package com.naon.grid.modules.app.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "grid_user")
public class GridUser extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, length = 50)
    private String username;

    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @NotNull
    @Column(name = "phone_verified", nullable = false)
    private Integer phoneVerified = 0;

    @Email
    @Column(name = "email", length = 100)
    private String email;

    @NotNull
    @Column(name = "email_verified", nullable = false)
    private Integer emailVerified = 0;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "gender")
    private Integer gender = 0;

    @Column(length = 20)
    private String userType = "NORMAL";

    private Integer orgId;

    @Column(length = 20)
    private String orgRole;

    private Integer agentId;

    @Column(length = 32)
    private String referralCode;

    @Column(length = 32)
    private String referredBy;

    @Column(length = 10)
    private String region;

    @Column(length = 20)
    private String registerAuditStatus = "APPROVED";

    @NotNull
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "register_ip", length = 50)
    private String registerIp;

    @Column(name = "last_login_time")
    private Date lastLoginTime;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;
}
