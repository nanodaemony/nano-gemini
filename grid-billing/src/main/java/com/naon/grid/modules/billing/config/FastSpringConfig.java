package com.naon.grid.modules.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "billing.fastspring")
public class FastSpringConfig {
    /** FastSpring API Key (Store-level) */
    private String apiKey;
    /** Webhook HMAC secret for signature verification */
    private String webhookSecret;
    /** Your FastSpring store ID (subdomain prefix) */
    private String storeId;
    /** FastSpring API base URL, default: https://api.fastspring.com */
    private String baseUrl = "https://api.fastspring.com";
    /** FastSpring hosted checkout domain */
    private String checkoutDomain;
    /** Merchant display name shown in checkout */
    private String merchantDisplayName = "YourRoad 有路中文";
    /** URL to redirect after successful payment */
    private String successUrl;
    /** URL to redirect after cancelled payment */
    private String cancelUrl;
}
