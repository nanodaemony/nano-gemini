package com.naon.grid.modules.app.config;

import com.naon.grid.config.properties.SecurityProperties;
import com.naon.grid.modules.app.security.AppTokenFilter;
import com.naon.grid.modules.app.security.AppTokenProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 注册 App Token 过滤器为全局 Servlet Filter（Ordered.HIGHEST_PRECEDENCE），
 * 在所有 Spring Security FilterChain 之前运行，确保 /api/app/** 请求的
 * JWT 认证独立于 admin SecurityFilterChain 链作用域问题。
 */
@Configuration
public class AppSecurityConfig {

    @Bean
    public FilterRegistrationBean<AppTokenFilter> appTokenFilterRegistration(
            AppTokenProvider appTokenProvider, SecurityProperties securityProperties) {
        FilterRegistrationBean<AppTokenFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AppTokenFilter(appTokenProvider, securityProperties));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("appTokenFilter");
        return registration;
    }
}
