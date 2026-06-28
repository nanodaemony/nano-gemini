package com.naon.grid.modules.billing.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.naon.grid.enums.BillingCycleEnum;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.domain.PaymentRecord;
import com.naon.grid.modules.billing.domain.PaymentSubscription;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.repository.PaymentRecordRepository;
import com.naon.grid.modules.billing.repository.PaymentSubscriptionRepository;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import com.naon.grid.modules.billing.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final GridOrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final EntitlementEngine entitlementEngine;
    private final PaymentSubscriptionRepository subscriptionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentCallback(String orderNo, String paymentMethod, Map<String, Object> callbackData) {
        GridOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderNo));

        if (!"PENDING".equals(order.getStatus())) {
            log.warn("订单 {} 状态已不是 PENDING: {}", orderNo, order.getStatus());
            return false;
        }

        order.setStatus("PAID");
        order.setPaymentMethod(paymentMethod);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setPaymentMethod(paymentMethod);
        record.setTransactionId(
                callbackData != null && callbackData.get("transactionId") != null
                        ? callbackData.get("transactionId").toString()
                        : "TXN_" + IdUtil.fastSimpleUUID().substring(0, 16));
        record.setAmount(order.getAmount());
        record.setCurrency(order.getCurrency());
        record.setStatus("SUCCESS");
        record.setRawCallback(callbackData != null ? JSON.toJSONString(callbackData) : "webhook");
        record.setCreateTime(LocalDateTime.now());
        paymentRecordRepository.save(record);

        BillingCycleEnum cycle = BillingCycleEnum.fromCode(order.getBillingCycle());
        int days = cycle != null ? cycle.getDays() : 365;
        entitlementEngine.grant(
                order.getUserId(),
                "PURCHASE",
                order.getOrderNo(),
                order.getProductCode(),
                days,
                order.getRegion()
        );

        if (order.getChannelSubId() != null) {
            PaymentSubscription sub = subscriptionRepository
                    .findByChannelAndChannelSubId("PHOTONPAY", order.getChannelSubId())
                    .orElseGet(() -> {
                        PaymentSubscription newSub = new PaymentSubscription();
                        newSub.setUserId(order.getUserId());
                        newSub.setOrderId(order.getId());
                        newSub.setProductCode(order.getProductCode());
                        newSub.setBillingCycle(order.getBillingCycle());
                        newSub.setRegion(order.getRegion());
                        newSub.setChannel("PHOTONPAY");
                        newSub.setChannelSubId(order.getChannelSubId());
                        newSub.setCreateTime(LocalDateTime.now());
                        return newSub;
                    });
            sub.setStatus("ACTIVE");
            sub.setLastChargedAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
            log.info("Payment subscription processed: userId={}, product={}, subId={}",
                    order.getUserId(), order.getProductCode(), order.getChannelSubId());
        }

        log.info("Payment processed: orderNo={}, userId={}, product={}, days={}",
                orderNo, order.getUserId(), order.getProductCode(), days);
        return true;
    }
}
