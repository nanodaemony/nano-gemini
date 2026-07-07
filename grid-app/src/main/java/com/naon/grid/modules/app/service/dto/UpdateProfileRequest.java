package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    private Integer gender;

    @Size(max = 20, message = "HSK等级长度不能超过20")
    private String hskLevel;

    @Size(max = 200, message = "个性签名长度不能超过200")
    private String signature;
}
