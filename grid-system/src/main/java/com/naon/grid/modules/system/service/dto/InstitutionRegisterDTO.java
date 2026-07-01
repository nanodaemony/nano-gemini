package com.naon.grid.modules.system.service.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class InstitutionRegisterDTO {
    @NotBlank(message = "机构名称不能为空")
    private String name;

    private String nameEn;

    @NotBlank(message = "机构类型不能为空")
    private String orgType;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    private String contactPhone;

    @NotBlank(message = "管理员邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String adminEmail;

    @NotBlank(message = "管理员密码不能为空")
    private String adminPassword;

    private String referredBy;
}
