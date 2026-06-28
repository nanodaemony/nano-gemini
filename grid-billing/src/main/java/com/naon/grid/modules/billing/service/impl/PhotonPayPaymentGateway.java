package com.naon.grid.modules.billing.service.impl;

import cn.hutool.core.util.IdUtil;
import com.naon.grid.modules.billing.config.PhotonPayConfig;
import com.naon.grid.modules.billing.service.PaymentGateway;
import com.naon.grid.modules.billing.service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotonPayPaymentGateway implements PaymentGateway {

    private final PhotonPayConfig config;

    @Override
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        // TODO: 替换为真实 PhotonPay /checkout API 调用
        log.info("Creating PhotonPay payment: orderNo={}, amount={} {}", request.getOrderNo(),
                request.getAmount(), request.getCurrency());
        String transactionId = "PP_" + IdUtil.fastSimpleUUID().toUpperCase();
        String paymentUrl = config.getBaseUrl() + "/checkout?order_no=" + request.getOrderNo();
        return PaymentCreateResponse.builder()
                .transactionId(transactionId)
                .paymentUrl(paymentUrl)
                .status("PENDING")
                .build();
    }

    @Override
    public SubscriptionCreateResponse createSubscription(SubscriptionCreateRequest request) {
        // TODO: 替换为真实 PhotonPay /subscription API 调用
        log.info("Creating PhotonPay subscription: orderNo={}, amount={} {}, cycle={}",
                request.getOrderNo(), request.getAmount(), request.getCurrency(), request.getBillingCycle());
        String subId = "SUBS_" + IdUtil.fastSimpleUUID().toUpperCase();
        String txnId = "PP_" + IdUtil.fastSimpleUUID().toUpperCase();
        String paymentUrl = config.getBaseUrl() + "/subscription?order_no=" + request.getOrderNo();
        return SubscriptionCreateResponse.builder()
                .subscriptionId(subId)
                .initialOrderId(txnId)
                .paymentUrl(paymentUrl)
                .status("PENDING")
                .build();
    }

    @Override
    public void cancelSubscription(String channelSubId) {
        // TODO: 调用 PhotonPay 取消订阅 API
        log.info("Cancelling PhotonPay subscription: subId={}", channelSubId);
    }

    @Override
    public TransactionQueryResponse queryTransaction(String channelOrderId) {
        // TODO: 调用 PhotonPay 交易查询 API
        log.info("Querying PhotonPay transaction: txnId={}", channelOrderId);
        return TransactionQueryResponse.builder()
                .transactionId(channelOrderId)
                .status("UNKNOWN")
                .build();
    }

    @Override
    public RefundResponse refund(String channelOrderId, BigDecimal amount) {
        // TODO: 调用 PhotonPay 退款 API
        log.info("Refunding PhotonPay transaction: txnId={}, amount={}", channelOrderId, amount);
        return RefundResponse.builder()
                .refundId("REF_" + IdUtil.fastSimpleUUID().toUpperCase())
                .status("PENDING")
                .build();
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        // TODO: 替换为真实 HMAC-SHA256 签名验证
        if (payload == null || payload.isEmpty()) {
            log.warn("Webhook signature verification failed: empty payload");
            return false;
        }
        log.info("Webhook signature verified (Phase 1 simplified check)");
        return true;
    }
}
