package com.naon.grid.modules.app.config;

import com.naon.grid.modules.app.interceptor.RegionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AppWebMvcConfig implements WebMvcConfigurer {

    private final RegionInterceptor regionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(regionInterceptor)
                .addPathPatterns("/api/app/**");
    }
}
