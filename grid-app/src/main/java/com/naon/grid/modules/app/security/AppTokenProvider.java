package com.naon.grid.modules.app.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import com.naon.grid.modules.security.config.SecurityProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AppTokenProvider implements InitializingBean {

    public static final String AUTHORITIES_UID_KEY = "userId";
    public static final String DEVICE_ID_KEY = "deviceId";
    public static final String ROLES_KEY = "roles";
    public static final String TOKEN_TYPE_KEY = "type";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String USER_TYPE_KEY = "userType";
    public static final String ORG_ID_KEY = "orgId";
    public static final String ORG_ROLE_KEY = "orgRole";
    public static final String REGION_KEY = "region";

    private Key signingKey;
    private JwtParser jwtParser;
    private final SecurityProperties properties;

    @Value("${app.auth.token-expire-seconds:604800}")
    private long tokenExpireSeconds;

    public AppTokenProvider(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        byte[] keyBytes = Decoders.BASE64.decode(properties.getBase64Secret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build();
    }

    public String createToken(Long userId, String username, String deviceId, List<String> roles) {
        return createToken(userId, username, deviceId, roles, null, null, null, null);
    }

    public String createToken(Long userId, String username, String deviceId,
                               List<String> roles, String userType,
                               Integer orgId, String orgRole, String region) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(AUTHORITIES_UID_KEY, userId);
        claims.put(DEVICE_ID_KEY, deviceId);
        claims.put(ROLES_KEY, roles);
        claims.put(TOKEN_TYPE_KEY, TOKEN_TYPE_ACCESS);
        claims.put(USER_TYPE_KEY, userType);
        if (orgId != null) claims.put(ORG_ID_KEY, orgId);
        if (orgRole != null) claims.put(ORG_ROLE_KEY, orgRole);
        if (region != null) claims.put(REGION_KEY, region);

        long now = System.currentTimeMillis();
        Date validity = new Date(now + tokenExpireSeconds * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public Claims getClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get(AUTHORITIES_UID_KEY, Long.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get(ROLES_KEY, List.class);
    }

    public String getDeviceIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get(DEVICE_ID_KEY, String.class);
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.trace("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }
}
