package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private String deviceName;

    private String referralCode; // 新增：注册时选填推荐码
}
