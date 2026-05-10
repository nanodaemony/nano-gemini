package com.naon.grid.modules.app.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

/**
 * APP用户实体
 */
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

    @NotBlank(message = "用户名不能为空")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Column(name = "phone", nullable = false, unique = true, length = 20)
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "gender")
    private Integer gender = 0;

    @NotNull
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "register_ip", length = 50)
    private String registerIp;

    @Column(name = "last_login_time")
    private Date lastLoginTime;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "wx_openid", length = 50)
    private String wxOpenid;

    @Column(name = "wx_unionid", length = 50)
    private String wxUnionid;
}
