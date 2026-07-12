package com.naon.grid.modules.billing.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.naon.grid.enums.BillingCycleEnum;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.domain.PaymentRecord;
import com.naon.grid.modules.billing.domain.PaymentSubscription;
import com.naon.grid.modules.billing.repository.EntitlementRepository;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.repository.GridProductRepository;
import com.naon.grid.modules.billing.repository.PaymentRecordRepository;
import com.naon.grid.modules.billing.repository.PaymentSubscriptionRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import com.naon.grid.modules.billing.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final GridOrderRepository orderRepository;
    private final GridProductRepository productRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final EntitlementService entitlementService;
    private final EntitlementRepository entitlementRepository;
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
        record.setGateway(order.getChannel());
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

        // Grant entitlements from product's entitlementIds
        GridProduct product = productRepository.findByCode(order.getProductCode())
                .orElse(null);
        if (product != null && product.getEntitlementIds() != null
                && !product.getEntitlementIds().isEmpty()) {
            JSONArray arr = JSON.parseArray(product.getEntitlementIds());
            List<Integer> ids = arr.stream()
                    .map(Object::toString)
                    .map(code -> entitlementRepository.findByCode(code))
                    .filter(Optional::isPresent)
                    .map(o -> o.get().getId())
                    .collect(Collectors.toList());
            if (!ids.isEmpty()) {
                entitlementService.grantEntitlements(
                        order.getUserId(), ids,
                        "PURCHASE", order.getOrderNo(), days, order.getRegion());
            } else {
                log.warn("No valid entitlements found for product: {}", order.getProductCode());
            }
        }

        if (order.getChannelSubId() != null) {
            String channel = order.getChannel() != null ? order.getChannel() : "FASTSPRING";
            PaymentSubscription sub = subscriptionRepository
                    .findByChannelAndChannelSubId(channel, order.getChannelSubId())
                    .orElseGet(() -> {
                        PaymentSubscription newSub = new PaymentSubscription();
                        newSub.setUserId(order.getUserId());
                        newSub.setProductCode(order.getProductCode());
                        newSub.setChannel(channel);
                        newSub.setChannelSubId(order.getChannelSubId());
                        newSub.setCreateTime(LocalDateTime.now());
                        return newSub;
                    });
            sub.setStatus("ACTIVE");
            subscriptionRepository.save(sub);
            log.info("Payment subscription processed: userId={}, product={}, channel={}, subId={}",
                    order.getUserId(), order.getProductCode(), channel, order.getChannelSubId());
        }

        log.info("Payment processed: orderNo={}, userId={}, product={}, days={}",
                orderNo, order.getUserId(), order.getProductCode(), days);
        return true;
    }
}
