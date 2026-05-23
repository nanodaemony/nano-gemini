
package com.naon.grid.modules.app.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "grid_user_auth")
public class GridUserAuth implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank
    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @NotBlank
    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Column(name = "provider_avatar", length = 500)
    private String providerAvatar;

    @Column(name = "access_token", length = 500)
    private String accessToken;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "expire_time")
    private Date expireTime;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = new Date();
        }
        if (this.updateTime == null) {
            this.updateTime = new Date();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Date();
    }
}
