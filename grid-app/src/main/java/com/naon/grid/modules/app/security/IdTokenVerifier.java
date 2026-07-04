package com.naon.grid.modules.app.security;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.config.SocialLoginProperties;
import com.naon.grid.modules.app.config.SocialLoginProperties.ProviderConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdTokenVerifier {

    private final SocialLoginProperties socialLoginProperties;

    public SocialUserInfo verify(String provider, String idToken) {
        ProviderConfig config = socialLoginProperties.getProvider(provider);
        if (config == null) {
            throw new BadRequestException("不支持的第三方登录方式: " + provider);
        }

        // Parse JWT header to get kid
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new BadRequestException("登录验证失败");
        }
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        JSONObject header = JSON.parseObject(headerJson);
        String kid = header.getString("kid");
        String alg = header.getString("alg");

        if (!"RS256".equals(alg)) {
            throw new BadRequestException("登录验证失败");
        }

        // Fetch JWKS and build public key
        PublicKey publicKey = getPublicKey(config.getJwksUrl(), kid);

        // Verify signature and extract claims
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();
        } catch (Exception e) {
            log.warn("ID token verification failed for provider={}: {}", provider, e.getMessage());
            throw new BadRequestException("登录验证失败");
        }

        // Validate audience (aud can be String or array)
        if (!audienceMatches(claims.get("aud"), config.getClientId())) {
            throw new BadRequestException("登录验证失败");
        }

        // Validate issuer
        String iss = claims.getIssuer();
        String expectedIss = config.getIssuer();
        if (iss == null || !issuerMatches(iss, expectedIss)) {
            throw new BadRequestException("登录验证失败");
        }

        // Validate expiration
        if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
            throw new BadRequestException("登录凭证已过期，请重新授权");
        }

        String email = claims.get("email", String.class);
        Boolean emailVerified = claims.get("email_verified", Boolean.class);

        return SocialUserInfo.builder()
                .provider(provider)
                .providerId(claims.getSubject())
                .email(email != null ? email.trim().toLowerCase() : null)
                .emailVerified(emailVerified != null && emailVerified)
                .name(claims.get("name", String.class))
                .picture(claims.get("picture", String.class))
                .expireTime(claims.getExpiration())
                .build();
    }

    private boolean audienceMatches(Object aud, String clientId) {
        if (aud instanceof String) {
            return aud.equals(clientId);
        }
        if (aud instanceof java.util.List) {
            return ((java.util.List<?>) aud).contains(clientId);
        }
        return false;
    }

    private boolean issuerMatches(String iss, String expectedIss) {
        if (iss.equals(expectedIss)) {
            return true;
        }
        // Google: "accounts.google.com" vs "https://accounts.google.com"
        if (iss.startsWith("https://")) {
            return iss.substring("https://".length()).equals(expectedIss);
        }
        return ("https://" + iss).equals(expectedIss);
    }

    private PublicKey getPublicKey(String jwksUrl, String kid) {
        try {
            String jwksJson = fetchJwks(jwksUrl);
            JSONObject jwks = JSON.parseObject(jwksJson);
            JSONArray keys = jwks.getJSONArray("keys");
            if (keys == null || keys.isEmpty()) {
                throw new BadRequestException("登录验证失败");
            }

            for (int i = 0; i < keys.size(); i++) {
                JSONObject key = keys.getJSONObject(i);
                if (kid != null && kid.equals(key.getString("kid"))) {
                    return buildRsaPublicKey(key.getString("n"), key.getString("e"));
                }
            }

            // If kid not found and only one key, try it (edge case)
            if (kid == null && keys.size() == 1) {
                JSONObject key = keys.getJSONObject(0);
                return buildRsaPublicKey(key.getString("n"), key.getString("e"));
            }

            throw new BadRequestException("登录验证失败");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch/parse JWKS from {}: {}", jwksUrl, e.getMessage());
            throw new BadRequestException("登录验证失败");
        }
    }

    private PublicKey buildRsaPublicKey(String modulusBase64, String exponentBase64) throws Exception {
        byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusBase64);
        byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentBase64);
        BigInteger modulus = new BigInteger(1, modulusBytes);
        BigInteger exponent = new BigInteger(1, exponentBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
    }

    private String fetchJwks(String jwksUrl) throws Exception {
        java.net.URL url = new java.net.URL(jwksUrl);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        try (Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } finally {
            conn.disconnect();
        }
    }
}
