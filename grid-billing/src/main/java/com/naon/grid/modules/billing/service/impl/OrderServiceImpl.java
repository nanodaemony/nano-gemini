package com.naon.grid.modules.billing.service.impl;

import cn.hutool.core.util.IdUtil;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.domain.RegionPricing;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.repository.RegionPricingRepository;
import com.naon.grid.modules.billing.service.OrderService;
import com.naon.grid.modules.billing.service.GatewayRouter;
import com.naon.grid.modules.billing.service.PaymentGateway;
import com.naon.grid.modules.billing.service.ProductService;
import com.naon.grid.modules.billing.service.dto.*;
import com.naon.grid.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final GridOrderRepository orderRepository;
    private final RegionPricingRepository pricingRepository;
    private final ProductService productService;
    private final GatewayRouter gatewayRouter;

    private static final Set<String> SUBSCRIPTION_CYCLES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("MONTHLY", "QUARTERLY", "YEARLY")));

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request, String region) {
        request.setRegion(region);

        GridProduct product = productService.findByCode(request.getProductCode())
                .orElseThrow(() -> new BadRequestException("产品不存在: " + request.getProductCode()));

        String currency = request.getCurrency();
        if (currency == null || currency.isEmpty()) {
            currency = "C".equals(region) ? "CNY" : "USD";
        }
        java.util.Optional<RegionPricing> pricingOpt = pricingRepository
                .findByProductIdAndRegionAndBillingCycleAndCurrency(
                        product.getId(), region, request.getBillingCycle(), currency);
        if (!pricingOpt.isPresent()) {
            pricingOpt = pricingRepository.findByProductIdAndRegionAndBillingCycle(
                    product.getId(), region, request.getBillingCycle());
        }
        RegionPricing pricing = pricingOpt
                .orElseThrow(() -> new BadRequestException(
                        "该产品在" + region + "区没有" + request.getBillingCycle() + "定价"));

        GridOrder order = new GridOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setOrgId(request.getOrgId());
        order.setProductCode(request.getProductCode());
        order.setRegion(region);
        order.setBillingCycle(request.getBillingCycle());
        order.setAmount(pricing.getPrice());
        order.setCurrency(pricing.getCurrency());
        order.setSubtotal(pricing.getPrice());
        order.setChannel("FASTSPRING");
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());
        orderRepository.save(order);

        PaymentGateway gateway = gatewayRouter.resolve();
        String redirectUrl;
        if (isSubscriptionBilling(request.getBillingCycle())) {
            SubscriptionCreateResponse subResp = gateway.createSubscription(
                    SubscriptionCreateRequest.builder()
                            .orderNo(order.getOrderNo())
                            .productCode(order.getProductCode())
                            .amount(order.getAmount())
                            .currency(order.getCurrency())
                            .billingCycle(order.getBillingCycle())
                            .description(product.getName())
                            .build()
            );
            order.setChannelSubId(subResp.getSubscriptionId());
            order.setChannelOrderId(subResp.getInitialOrderId());
            redirectUrl = subResp.getPaymentUrl();
        } else {
            PaymentCreateResponse payResp = gateway.createPayment(
                    PaymentCreateRequest.builder()
                            .orderNo(order.getOrderNo())
                            .productCode(order.getProductCode())
                            .amount(order.getAmount())
                            .currency(order.getCurrency())
                            .description(product.getName())
                            .build()
            );
            order.setChannelOrderId(payResp.getTransactionId());
            redirectUrl = payResp.getPaymentUrl();
        }

        orderRepository.save(order);
        log.info("Order created: orderNo={}, userId={}, product={}, amount={} {}",
                order.getOrderNo(), userId, request.getProductCode(),
                order.getAmount(), order.getCurrency());

        return OrderCreateResponse.builder()
                .orderNo(order.getOrderNo())
                .productCode(order.getProductCode())
                .billingCycle(order.getBillingCycle())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .redirectUrl(redirectUrl)
                .build();
    }

    private boolean isSubscriptionBilling(String billingCycle) {
        return billingCycle != null && SUBSCRIPTION_CYCLES.contains(billingCycle.toUpperCase());
    }

    @Override
    public Optional<GridOrder> findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo);
    }

    @Override
    public GridOrder save(GridOrder order) {
        return orderRepository.save(order);
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
    }
}
