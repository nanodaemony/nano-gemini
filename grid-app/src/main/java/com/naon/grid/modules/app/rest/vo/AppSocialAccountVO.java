package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppSocialAccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("绑定记录ID")
    private Long id;

    @ApiModelProperty("第三方平台")
    private String provider;

    @ApiModelProperty("第三方平台用户名")
    private String providerName;

    @ApiModelProperty("第三方平台头像")
    private String providerAvatar;

    @ApiModelProperty("绑定时间")
    private String createdAt;
}
