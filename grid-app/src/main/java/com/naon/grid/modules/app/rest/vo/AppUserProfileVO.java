package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppUserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像URL")
    private String avatarUrl;

    @ApiModelProperty("性别：0-未知 1-男 2-女")
    private Integer gender;

    @ApiModelProperty("HSK等级")
    private String hskLevel;

    @ApiModelProperty("个性签名")
    private String signature;

    @ApiModelProperty("用户类型")
    private String userType;

    @ApiModelProperty("所属区域")
    private String region;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("邮箱是否验证：0-否 1-是")
    private Integer emailVerified;

    @ApiModelProperty("是否有密码（社交登录用户无密码）")
    private Boolean hasPassword;

    @ApiModelProperty("注册时间")
    private String createdAt;
}
