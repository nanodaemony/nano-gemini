
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
@Table(name = "grid_user_token")
public class GridUserToken implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @NotBlank
    @Column(name = "refresh_token", nullable = false, length = 500)
    private String refreshToken;

    @Column(name = "access_token", length = 500)
    private String accessToken;

    @NotNull
    @Column(name = "expire_time", nullable = false)
    private Date expireTime;

    @Column(name = "create_time", nullable = false, updatable = false)
    private Date createTime;

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = new Date();
        }
    }
}
