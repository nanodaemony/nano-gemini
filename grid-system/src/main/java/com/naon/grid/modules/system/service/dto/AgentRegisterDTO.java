package com.naon.grid.modules.system.service.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class AgentRegisterDTO {
    @NotBlank(message = "代理商名称不能为空")
    private String name;

    private String contactName;

    @Email(message = "邮箱格式不正确")
    private String contactEmail;

    @NotBlank(message = "管理员邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String adminEmail;

    @NotBlank(message = "管理员密码不能为空")
    private String adminPassword;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private String deviceName;
}
