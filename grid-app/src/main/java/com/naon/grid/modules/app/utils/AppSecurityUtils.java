package com.naon.grid.modules.app.utils;

import com.naon.grid.modules.app.security.AppAuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AppSecurityUtils {

    public static String header;
    public static String tokenStartWith;

    @Value("${jwt.header}")
    public void setHeader(String header) {
        AppSecurityUtils.header = header;
    }

    @Value("${jwt.token-start-with}")
    public void setTokenStartWith(String tokenStartWith) {
        AppSecurityUtils.tokenStartWith = tokenStartWith;
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AppAuthenticationToken) {
            return ((AppAuthenticationToken) authentication).getUserId();
        }
        return null;
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return null;
    }

    public static String getCurrentDeviceId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AppAuthenticationToken) {
            return ((AppAuthenticationToken) authentication).getDeviceId();
        }
        return null;
    }
}
