
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
@Table(name = "grid_user_role")
public class GridUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotBlank
    @Column(name = "role_code", nullable = false, length = 30)
    private String roleCode;

    @NotBlank
    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "expire_time")
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
