package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RefreshTokenDTO {

    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
