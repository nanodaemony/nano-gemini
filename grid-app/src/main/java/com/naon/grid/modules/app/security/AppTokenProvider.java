package com.naon.grid.modules.app.security;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.naon.grid.modules.security.config.SecurityProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AppTokenProvider implements InitializingBean {

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String AUTHORITIES_UID_KEY = "uid";
    public static final String DEVICE_ID_KEY = "did";
    public static final String TOKEN_TYPE_KEY = "type";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private Key signingKey;
    private JwtParser jwtParser;
    private final SecurityProperties properties;

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

    public String createToken(Long userId, String username, String deviceId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(AUTHORITIES_UID_KEY, userId);
        claims.put(DEVICE_ID_KEY, deviceId);
        claims.put(TOKEN_TYPE_KEY, TOKEN_TYPE_ACCESS);
        claims.put("jti", IdUtil.simpleUUID());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
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
}
