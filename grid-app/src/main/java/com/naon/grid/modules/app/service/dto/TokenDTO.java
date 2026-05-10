package com.naon.grid.modules.app.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TokenDTO {
    @ApiModelProperty(value = "访问Token")
    private String token;

    @ApiModelProperty(value = "刷新Token")
    private String refreshToken;

    @ApiModelProperty(value = "过期时间（秒）")
    private Long expiresIn;

    @ApiModelProperty(value = "用户信息")
    private AppUserDTO user;
}
