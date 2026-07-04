package com.naon.grid.modules.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "social")
public class SocialLoginProperties {

    private Map<String, ProviderConfig> providers;

    @Data
    public static class ProviderConfig {
        private String clientId;
        private String issuer;
        private String jwksUrl;
    }

    public ProviderConfig getProvider(String name) {
        return providers != null ? providers.get(name) : null;
    }
}
