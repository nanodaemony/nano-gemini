package com.naon.grid.modules.billing.service.impl;

import cn.hutool.core.util.IdUtil;
import com.naon.grid.enums.BillingCycleEnum;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.domain.PaymentRecord;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.repository.PaymentRecordRepository;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentCallback(String orderNo, String paymentMethod, Map<String, Object> callbackData) {
        GridOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderNo));

        if (!"PENDING".equals(order.getStatus())) {
            log.warn("订单 {} 状态已不是 PENDING: {}", orderNo, order.getStatus());
            return false;
        }

        // Phase 1: Always succeed (mock)
        order.setStatus("PAID");
        order.setPaymentMethod(paymentMethod);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // Record payment
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setPaymentMethod(paymentMethod);
        record.setTransactionId("MOCK_" + IdUtil.fastSimpleUUID().substring(0, 16));
        record.setAmount(order.getAmount());
        record.setCurrency(order.getCurrency());
        record.setStatus("SUCCESS");
        record.setRawCallback(callbackData != null ? callbackData.toString() : "mock");
        record.setCreateTime(LocalDateTime.now());
        paymentRecordRepository.save(record);

        // Grant entitlement
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

        log.info("Payment callback processed: orderNo={}, userId={}, product={}, days={}",
                orderNo, order.getUserId(), order.getProductCode(), days);
        return true;
    }
}
