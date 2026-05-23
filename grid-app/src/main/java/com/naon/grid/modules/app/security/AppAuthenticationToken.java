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

    public AppAuthenticationToken(Long userId, String username, String deviceId, List<String> roles) {
        super(roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList()));
        this.userId = userId;
        this.username = username;
        this.deviceId = deviceId;
        this.roles = roles;
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
}
