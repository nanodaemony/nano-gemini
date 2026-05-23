package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class AppUserDTO {

    private Long id;
    private String email;
    private String nickname;
    private String avatar;
    private Integer gender;
    private List<String> roles;
}
