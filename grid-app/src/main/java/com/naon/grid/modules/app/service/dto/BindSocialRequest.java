package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class BindSocialRequest {

    @NotBlank(message = "第三方平台不能为空")
    private String provider;

    @NotBlank(message = "身份令牌不能为空")
    private String idToken;
}
