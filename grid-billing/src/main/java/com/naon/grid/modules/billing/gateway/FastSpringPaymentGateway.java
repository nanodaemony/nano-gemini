package com.naon.grid.modules.billing.gateway;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.naon.grid.modules.billing.config.FastSpringConfig;
import com.naon.grid.modules.billing.gateway.AbstractPaymentGateway;
import com.naon.grid.modules.billing.service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * FastSpring Merchant of Record payment gateway implementation.
 * Uses FastSpring Sessions v2 API + Webhooks.
 *
 * FastSpring acts as Merchant of Record: handles global tax (VAT/GST/Sales Tax),
 * payment processing, subscription management, and invoice generation.
 * We only need to create checkout sessions and handle webhooks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FastSpringPaymentGateway extends AbstractPaymentGateway {

    private final FastSpringConfig config;

    @Override
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        log.info("FastSpring: creating one-time payment session for order {}", request.getOrderNo());

        String sessionUrl = config.getBaseUrl() + "/sessions";
        Map<String, Object> body = buildSessionBody(
                request.getOrderNo(), request.getProductCode(),
                request.getAmount(), request.getCurrency(),
                request.getDescription(), null,
                request.getReturnUrl(), request.getCancelUrl());

        String transactionId = "FS_" + IdUtil.fastSimpleUUID().toUpperCase();
        try {
            HttpResponse resp = HttpRequest.post(sessionUrl)
                    .header("Content-Type", "application/json")
                    .basicAuth(config.getApiKey(), "")
                    .body(JSON.toJSONString(body))
                    .timeout(30000)
                    .execute();

            if (resp.isOk()) {
                JSONObject result = JSON.parseObject(resp.body());
                String checkoutUrl = result.getString("url");
                String sessionId = result.getString("id");
                log.info("FastSpring session created: sessionId={}, url={}", sessionId, checkoutUrl);
                return PaymentCreateResponse.builder()
                        .transactionId(sessionId != null ? sessionId : transactionId)
                        .paymentUrl(checkoutUrl)
                        .status("PENDING")
                        .build();
            } else {
                log.error("FastSpring session creation failed: HTTP {} body={}", resp.getStatus(), resp.body());
            }
        } catch (Exception e) {
            log.error("FastSpring createPayment error for order {}", request.getOrderNo(), e);
        }

        // Fallback for development: return a placeholder
        String fallbackUrl = config.getCheckoutDomain() + "/checkout?order=" + request.getOrderNo();
        return PaymentCreateResponse.builder()
                .transactionId(transactionId)
                .paymentUrl(fallbackUrl)
                .status("PENDING")
                .build();
    }

    @Override
    public SubscriptionCreateResponse createSubscription(SubscriptionCreateRequest request) {
        log.info("FastSpring: creating subscription session for order {}, cycle={}",
                request.getOrderNo(), request.getBillingCycle());

        String sessionUrl = config.getBaseUrl() + "/sessions";
        Map<String, Object> body = buildSessionBody(
                request.getOrderNo(), request.getProductCode(),
                request.getAmount(), request.getCurrency(),
                request.getDescription(), request.getBillingCycle(),
                request.getReturnUrl(), request.getCancelUrl());

        String subId = "FS_SUB_" + IdUtil.fastSimpleUUID().toUpperCase();
        try {
            HttpResponse resp = HttpRequest.post(sessionUrl)
                    .header("Content-Type", "application/json")
                    .basicAuth(config.getApiKey(), "")
                    .body(JSON.toJSONString(body))
                    .timeout(30000)
                    .execute();

            if (resp.isOk()) {
                JSONObject result = JSON.parseObject(resp.body());
                String checkoutUrl = result.getString("url");
                String sessionId = result.getString("id");
                log.info("FastSpring subscription session created: sessionId={}", sessionId);
                return SubscriptionCreateResponse.builder()
                        .subscriptionId(sessionId != null ? sessionId : subId)
                        .initialOrderId(sessionId != null ? sessionId : "FS_" + IdUtil.fastSimpleUUID().toUpperCase())
                        .paymentUrl(checkoutUrl)
                        .status("PENDING")
                        .build();
            } else {
                log.error("FastSpring subscription session failed: HTTP {} body={}", resp.getStatus(), resp.body());
            }
        } catch (Exception e) {
            log.error("FastSpring createSubscription error for order {}", request.getOrderNo(), e);
        }

        String fallbackUrl = config.getCheckoutDomain() + "/subscription?order=" + request.getOrderNo();
        return SubscriptionCreateResponse.builder()
                .subscriptionId(subId)
                .initialOrderId("FS_" + IdUtil.fastSimpleUUID().toUpperCase())
                .paymentUrl(fallbackUrl)
                .status("PENDING")
                .build();
    }

    @Override
    public void cancelSubscription(String channelSubId) {
        log.info("FastSpring: cancelling subscription {}", channelSubId);
        String url = config.getBaseUrl() + "/subscriptions/" + channelSubId;
        try {
            HttpResponse resp = HttpRequest.delete(url)
                    .basicAuth(config.getApiKey(), "")
                    .timeout(30000)
                    .execute();
            if (!resp.isOk()) {
                log.error("FastSpring cancel subscription failed: HTTP {} body={}", resp.getStatus(), resp.body());
            }
        } catch (Exception e) {
            log.error("FastSpring cancelSubscription error for {}", channelSubId, e);
        }
    }

    @Override
    public TransactionQueryResponse queryTransaction(String channelOrderId) {
        log.info("FastSpring: querying order {}", channelOrderId);
        String url = config.getBaseUrl() + "/orders/" + channelOrderId;
        try {
            HttpResponse resp = HttpRequest.get(url)
                    .basicAuth(config.getApiKey(), "")
                    .timeout(15000)
                    .execute();
            if (resp.isOk()) {
                JSONObject order = JSON.parseObject(resp.body());
                return TransactionQueryResponse.builder()
                        .transactionId(channelOrderId)
                        .status(order.getString("status"))
                        .build();
            }
        } catch (Exception e) {
            log.error("FastSpring queryTransaction error for {}", channelOrderId, e);
        }
        return TransactionQueryResponse.builder()
                .transactionId(channelOrderId)
                .status("UNKNOWN")
                .build();
    }

    @Override
    public RefundResponse refund(String channelOrderId, BigDecimal amount) {
        log.info("FastSpring: refunding order {}, amount={}", channelOrderId, amount);
        String url = config.getBaseUrl() + "/returns";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order", channelOrderId);
        body.put("amount", amount != null ? amount : 0);
        body.put("reason", "Customer requested refund");

        String refundId = "FS_REF_" + IdUtil.fastSimpleUUID().toUpperCase();
        try {
            HttpResponse resp = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .basicAuth(config.getApiKey(), "")
                    .body(JSON.toJSONString(body))
                    .timeout(30000)
                    .execute();
            if (resp.isOk()) {
                JSONObject result = JSON.parseObject(resp.body());
                return RefundResponse.builder()
                        .refundId(result.getString("id") != null ? result.getString("id") : refundId)
                        .status("PENDING")
                        .build();
            }
        } catch (Exception e) {
            log.error("FastSpring refund error for {}", channelOrderId, e);
        }
        return RefundResponse.builder()
                .refundId(refundId)
                .status("PENDING")
                .build();
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        return verifyHmacSha256(payload, signature, config.getWebhookSecret());
    }

    // ─── Private helpers ───

    private Map<String, Object> buildSessionBody(String orderNo, String productCode,
                                                  BigDecimal amount, String currency,
                                                  String description, String billingCycle,
                                                  String returnUrl, String cancelUrl) {
        Map<String, Object> body = new LinkedHashMap<>();

        // Contact — FastSpring requires at least an email placeholder;
        // real email is filled by user on FastSpring checkout page
        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("email", "");
        body.put("contact", contact);

        // Items
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("product", productCode);
        item.put("quantity", 1);
        if (amount != null) {
            item.put("price", amount);
        }
        if (billingCycle != null) {
            Map<String, Object> subConfig = new LinkedHashMap<>();
            subConfig.put("interval", mapBillingCycle(billingCycle));
            item.put("subscription", subConfig);
        }
        body.put("items", Collections.singletonList(item));

        // Currency
        body.put("currency", currency != null ? currency : "USD");

        // Tags — pass our internal orderNo as metadata
        Map<String, Object> tags = new LinkedHashMap<>();
        tags.put("orderNo", orderNo);
        tags.put("productCode", productCode);
        body.put("tags", tags);

        // Redirect URLs
        if (returnUrl != null && !returnUrl.isEmpty()) {
            body.put("returnUrl", returnUrl);
        }
        if (cancelUrl != null && !cancelUrl.isEmpty()) {
            body.put("cancelUrl", cancelUrl);
        }

        return body;
    }

    private String mapBillingCycle(String cycle) {
        if (cycle == null) return "year";
        switch (cycle.toUpperCase()) {
            case "MONTHLY":  return "month";
            case "QUARTERLY": return "quarter";
            case "YEARLY":   return "year";
            default:         return "year";
        }
    }
}
