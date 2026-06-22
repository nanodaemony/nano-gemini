package com.naon.grid.modules.app.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AppAuthenticationToken extends AbstractAuthenticationToken {

    private final Long userId;
    private final String username;
    private final String deviceId;
    private final List<String> roles;
    private final String userType;
    private final Integer orgId;
    private final String orgRole;
    private final String region;

    public AppAuthenticationToken(Long userId, String username, String deviceId, List<String> roles) {
        this(userId, username, deviceId, roles, null, null, null, null);
    }

    public AppAuthenticationToken(Long userId, String username, String deviceId,
                                   List<String> roles, String userType,
                                   Integer orgId, String orgRole, String region) {
        super(roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList()));
        this.userId = userId;
        this.username = username;
        this.deviceId = deviceId;
        this.roles = roles;
        this.userType = userType;
        this.orgId = orgId;
        this.orgRole = orgRole;
        this.region = region;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public List<String> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public String getUserType() {
        return userType;
    }

    public Integer getOrgId() {
        return orgId;
    }

    public String getOrgRole() {
        return orgRole;
    }

    public String getRegion() {
        return region;
    }
}
