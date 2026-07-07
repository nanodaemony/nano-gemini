package com.naon.grid.modules.system.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class AppUserDTO {

    private Long id;
    private String email;
    private String nickname;
    private Long avatar;
    private Integer gender;
    private String hskLevel;
    private String signature;
    private List<String> roles;
    private String userType;
    private String orgRole;
    private String region;
}
