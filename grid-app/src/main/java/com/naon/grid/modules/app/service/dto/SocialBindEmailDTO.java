package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class SocialBindEmailDTO {

    @NotBlank(message = "绑定令牌不能为空")
    private String bindToken;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private String deviceName;
}
