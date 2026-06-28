package com.naon.grid.modules.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "billing.photonpay")
public class PhotonPayConfig {
    private String apiKey;
    private String apiSecret;
    private String webhookSecret;
    private String baseUrl = "https://sandbox.photonpay.com";
    private String merchantDisplayName = "YourRoad 有路中文";
    private String merchantDescription = "International Chinese Learning Platform";
    private String returnUrl = "https://yourroad.com/payment/return";
    private String cancelUrl = "https://yourroad.com/payment/cancel";
    private String webhookUrl = "https://yourroad.com/api/app/payments/webhook/photonpay";
    private ApplePayConfig applePay = new ApplePayConfig();

    @Data
    public static class ApplePayConfig {
        private boolean enabled = true;
        private String merchantId;
        private String merchantName = "YourRoad";
        private String supportedNetworks = "visa,mastercard,discover";
    }
}
