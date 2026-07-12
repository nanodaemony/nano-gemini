package com.naon.grid.modules.app.rest;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.modules.app.repository.ReferralRecordRepository;
import com.naon.grid.modules.app.service.ReferralService;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.service.GatewayRouter;
import com.naon.grid.modules.billing.service.PaymentGateway;
import com.naon.grid.modules.billing.service.PaymentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/payments")
@Api(tags = "支付：Webhook 回调")
public class PaymentWebhookController {

    private final GatewayRouter gatewayRouter;
    private final PaymentService paymentService;
    private final GridOrderRepository orderRepository;
    private final ReferralService referralService;
    private final ReferralRecordRepository referralRecordRepository;

    // Simple in-memory idempotency guard for webhook dedup
    private final java.util.Set<String> processedEventIds = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    @ApiOperation("PhotonPay 支付回调 Webhook")
    @AnonymousPostMapping("/webhook/photonpay")
    public ResponseEntity<String> handlePhotonPayWebhook(
            HttpServletRequest request,
            @RequestBody String payload) {

        // TODO: 确认 PhotonPay Webhook 签名的 Header 名称
        String signature = request.getHeader("X-PhotonPay-Signature");
        PaymentGateway photonGateway = gatewayRouter.resolve("photonpay");
        if (!photonGateway.verifyWebhookSignature(payload, signature)) {
            log.warn("PhotonPay webhook signature verification failed");
            return ResponseEntity.status(401).body("INVALID_SIGNATURE");
        }

        JSONObject data;
        try {
            data = JSON.parseObject(payload);
        } catch (Exception e) {
            log.error("Failed to parse webhook payload", e);
            return ResponseEntity.badRequest().body("INVALID_PAYLOAD");
        }

        String eventType = data.getString("event_type");
        JSONObject eventData = data.getJSONObject("data");
        log.info("PhotonPay webhook received: eventType={}", eventType);

        switch (eventType != null ? eventType : "") {
            case "payment.succeeded":
                // TODO: 确认 PhotonPay 回调中订单号字段名
                String orderNo = eventData != null ? eventData.getString("merchant_order_no") : null;
                if (orderNo != null) {
                    paymentService.handlePaymentCallback(orderNo, "PHOTONPAY", eventData);

                    // Record SUBSCRIBE referral event
                    try {
                        GridOrder order = orderRepository.findByOrderNo(orderNo).orElse(null);
                        if (order != null) {
                            referralRecordRepository
                                .findFirstByReferredIdAndEventTypeOrderByCreateTimeDesc(
                                    order.getUserId(), "REGISTER")
                                .ifPresent(regRecord -> {
                                    referralService.recordEvent(regRecord.getReferralCode(),
                                        order.getUserId(), "SUBSCRIBE", order.getOrgId());
                                });
                        }
                    } catch (Exception e) {
                        log.warn("Failed to record SUBSCRIBE referral event for orderNo={}: {}",
                                orderNo, e.getMessage());
                    }
                }
                break;
            case "payment.refunded":
                log.info("Payment refunded: {}", eventData);
                break;
            case "subscription.cancelled":
                log.info("Subscription cancelled: {}", eventData);
                break;
            case "subscription.failed":
                log.warn("Subscription payment failed: {}", eventData);
                break;
            default:
                log.debug("Unhandled webhook event: {}", eventType);
        }

        return ResponseEntity.ok("OK");
    }

    @ApiOperation("FastSpring 支付回调 Webhook")
    @AnonymousPostMapping("/webhook/fastspring")
    public ResponseEntity<String> handleFastSpringWebhook(
            HttpServletRequest request,
            @RequestBody String payload) {

        // Verify signature
        String signature = request.getHeader("x-fastspring-signature");
        PaymentGateway gateway = gatewayRouter.resolve("fastspring");
        if (!gateway.verifyWebhookSignature(payload, signature)) {
            log.warn("FastSpring webhook signature verification failed");
            return ResponseEntity.status(401).body("INVALID_SIGNATURE");
        }

        JSONObject data;
        try {
            data = JSON.parseObject(payload);
        } catch (Exception e) {
            log.error("Failed to parse FastSpring webhook payload", e);
            return ResponseEntity.badRequest().body("INVALID_PAYLOAD");
        }

        // Idempotency: skip already-processed events
        String eventId = data.getString("id");
        if (eventId != null && !processedEventIds.add(eventId)) {
            log.info("FastSpring webhook already processed: eventId={}", eventId);
            return ResponseEntity.ok("OK");
        }

        String eventType = data.getString("event");
        JSONObject eventData = data.getJSONObject("data");
        log.info("FastSpring webhook received: event={}", eventType);

        if (eventType == null) {
            return ResponseEntity.ok("OK");
        }

        switch (eventType) {
            case "order.completed":
                handleFastSpringOrderCompleted(eventData);
                break;
            case "subscription.activated":
                log.info("FastSpring subscription activated: {}", eventData);
                break;
            case "subscription.charge.completed":
                handleFastSpringOrderCompleted(eventData);
                break;
            case "subscription.canceled":
                log.info("FastSpring subscription canceled: {}", eventData);
                break;
            case "subscription.payment.failed":
                log.warn("FastSpring subscription payment failed: {}", eventData);
                break;
            case "return.created":
                log.info("FastSpring return (refund) created: {}", eventData);
                break;
            default:
                log.debug("Unhandled FastSpring webhook event: {}", eventType);
        }

        return ResponseEntity.ok("OK");
    }

    private void handleFastSpringOrderCompleted(JSONObject eventData) {
        if (eventData == null) return;
        // FastSpring passes our tags in the order; orderNo is in tags
        JSONObject tags = eventData.getJSONObject("tags");
        String orderNo = tags != null ? tags.getString("orderNo") : eventData.getString("order");
        if (orderNo == null) {
            log.warn("FastSpring webhook missing orderNo in tags: {}", eventData);
            return;
        }
        paymentService.handlePaymentCallback(orderNo, "FASTSPRING", eventData);

        // Record SUBSCRIBE referral event
        try {
            GridOrder order = orderRepository.findByOrderNo(orderNo).orElse(null);
            if (order != null) {
                referralRecordRepository
                    .findFirstByReferredIdAndEventTypeOrderByCreateTimeDesc(
                        order.getUserId(), "REGISTER")
                    .ifPresent(regRecord -> {
                        referralService.recordEvent(regRecord.getReferralCode(),
                            order.getUserId(), "SUBSCRIBE", order.getOrgId());
                    });
            }
        } catch (Exception e) {
            log.warn("Failed to record SUBSCRIBE referral event for orderNo={}: {}",
                    orderNo, e.getMessage());
        }
    }
}
