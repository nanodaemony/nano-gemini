package com.naon.grid.modules.system.service.dto;

import lombok.Data;

@Data
public class TokenDTO {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private AppUserDTO user;
}
