package com.naon.grid.modules.billing.service.impl;

import cn.hutool.core.util.IdUtil;
import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.domain.RegionPricing;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.repository.RegionPricingRepository;
import com.naon.grid.modules.billing.service.OrderService;
import com.naon.grid.modules.billing.service.ProductService;
import com.naon.grid.modules.billing.service.dto.OrderCreateRequest;
import com.naon.grid.modules.billing.service.dto.OrderCreateResponse;
import com.naon.grid.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final GridOrderRepository orderRepository;
    private final RegionPricingRepository pricingRepository;
    private final ProductService productService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request, String region) {
        request.setRegion(region);

        // Find product and pricing
        GridProduct product = productService.findByCode(request.getProductCode())
                .orElseThrow(() -> new BadRequestException("产品不存在: " + request.getProductCode()));

        RegionPricing pricing = pricingRepository
                .findByProductIdAndRegionAndBillingCycle(product.getId(), region, request.getBillingCycle())
                .orElseThrow(() -> new BadRequestException(
                        "该产品在" + region + "区没有" + request.getBillingCycle() + "定价"));

        // Create order
        GridOrder order = new GridOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setOrgId(request.getOrgId());
        order.setProductCode(request.getProductCode());
        order.setRegion(region);
        order.setBillingCycle(request.getBillingCycle());
        order.setAmount(pricing.getPrice());
        order.setCurrency(pricing.getCurrency());
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());
        orderRepository.save(order);

        return OrderCreateResponse.builder()
                .orderNo(order.getOrderNo())
                .productCode(order.getProductCode())
                .billingCycle(order.getBillingCycle())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .redirectUrl(null) // Phase 1: no real payment
                .build();
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
