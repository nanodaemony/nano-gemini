package com.naon.grid.modules.billing.gateway;

import com.naon.grid.modules.billing.service.PaymentGateway;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Abstract base for payment gateway implementations.
 * Provides shared logic for signature verification, retry, and logging.
 */
@Slf4j
public abstract class AbstractPaymentGateway implements PaymentGateway {

    protected static final int MAX_RETRIES = 3;
    protected static final long RETRY_DELAY_MS = 1000L;

    /**
     * Verify HMAC-SHA256 webhook signature.
     * Subclasses call this with gateway-specific secret.
     */
    protected boolean verifyHmacSha256(String payload, String signature, String secret) {
        if (payload == null || payload.isEmpty() || signature == null || signature.isEmpty()) {
            log.warn("Empty payload or signature for webhook verification");
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(computed);
            boolean valid = expected.equals(signature);
            if (!valid) {
                log.warn("Webhook signature mismatch: expected={}, received={}", expected, signature);
            }
            return valid;
        } catch (Exception e) {
            log.error("Webhook signature verification error", e);
            return false;
        }
    }

    /** Sleep with exponential backoff for retries */
    protected void retryDelay(int attempt) {
        try {
            Thread.sleep(RETRY_DELAY_MS * (1L << attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
