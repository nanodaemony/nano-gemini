package com.naon.grid.modules.app.security;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class SocialUserInfo {

    private String provider;
    private String providerId;
    private String email;
    private boolean emailVerified;
    private String name;
    private String picture;
    private Date expireTime;
}
