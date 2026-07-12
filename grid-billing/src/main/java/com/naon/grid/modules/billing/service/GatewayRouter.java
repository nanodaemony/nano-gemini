package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.service.impl.PhotonPayPaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Routes payment requests to the appropriate PaymentGateway implementation
 * based on configuration and context.
 *
 * Default gateway is determined by {@code billing.payment.gateway} config property.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayRouter {

    private final Map<String, PaymentGateway> gatewayMap;

    @Value("${billing.payment.gateway:fastspring}")
    private String defaultGateway;

    /**
     * Resolve the active payment gateway.
     * Currently returns the configured default gateway.
     * Future: can route based on region, currency, or payment method preference.
     */
    public PaymentGateway resolve() {
        return resolve(defaultGateway);
    }

    /**
     * Resolve gateway by name (e.g. "fastspring", "photonpay").
     */
    public PaymentGateway resolve(String gatewayName) {
        String name = gatewayName != null ? gatewayName.toLowerCase() : defaultGateway;
        PaymentGateway gw = gatewayMap.get(name + "PaymentGateway");
        if (gw != null && !(gw instanceof PhotonPayPaymentGateway)) {
            return gw;
        }
        // Fallback: search by bean name containing the gateway name
        for (Map.Entry<String, PaymentGateway> entry : gatewayMap.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name) && !(entry.getValue() instanceof PhotonPayPaymentGateway)) {
                return entry.getValue();
            }
        }
        throw new IllegalStateException("No active PaymentGateway found for: " + gatewayName
                + ". Available: " + gatewayMap.keySet());
    }

    /** Get the name of the currently active gateway */
    public String getActiveGatewayName() {
        return defaultGateway.toUpperCase();
    }
}
