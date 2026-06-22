package com.naon.grid.modules.app.security;

import com.naon.grid.config.properties.SecurityProperties;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AppTokenFilter extends GenericFilterBean {

    private final AppTokenProvider appTokenProvider;
    private final SecurityProperties securityProperties;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String token = resolveToken(httpServletRequest);

        if (StringUtils.hasText(token) && appTokenProvider.validateToken(token)) {
            Long userId = appTokenProvider.getUserIdFromToken(token);
            String username = appTokenProvider.getClaims(token).getSubject();
            String deviceId = appTokenProvider.getDeviceIdFromToken(token);
            List<String> roles = appTokenProvider.getRolesFromToken(token);
            Claims claims = appTokenProvider.getClaims(token);
            String userType = claims.get(AppTokenProvider.USER_TYPE_KEY, String.class);
            Integer orgId = claims.get(AppTokenProvider.ORG_ID_KEY, Integer.class);
            String orgRole = claims.get(AppTokenProvider.ORG_ROLE_KEY, String.class);
            String region = claims.get(AppTokenProvider.REGION_KEY, String.class);

            AppAuthenticationToken authentication = new AppAuthenticationToken(userId, username, deviceId, roles, userType, orgId, orgRole, region);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(securityProperties.getHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(securityProperties.getTokenStartWith())) {
            return bearerToken.substring(securityProperties.getTokenStartWith().length());
        }
        return null;
    }
}
