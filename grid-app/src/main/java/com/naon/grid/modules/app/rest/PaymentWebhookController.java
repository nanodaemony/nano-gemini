package com.naon.grid.modules.app.rest;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
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

    private final PaymentGateway paymentGateway;
    private final PaymentService paymentService;

    @ApiOperation("PhotonPay 支付回调 Webhook")
    @AnonymousPostMapping("/webhook/photonpay")
    public ResponseEntity<String> handlePhotonPayWebhook(
            HttpServletRequest request,
            @RequestBody String payload) {

        // TODO: 确认 PhotonPay Webhook 签名的 Header 名称
        String signature = request.getHeader("X-PhotonPay-Signature");
        if (!paymentGateway.verifyWebhookSignature(payload, signature)) {
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
}
