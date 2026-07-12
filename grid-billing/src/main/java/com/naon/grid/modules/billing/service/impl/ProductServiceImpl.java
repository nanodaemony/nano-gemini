package com.naon.grid.modules.billing.service.impl;

import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.domain.RegionPricing;
import com.naon.grid.modules.billing.repository.GridProductRepository;
import com.naon.grid.modules.billing.repository.RegionPricingRepository;
import com.naon.grid.modules.billing.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final GridProductRepository productRepository;
    private final RegionPricingRepository pricingRepository;

    @Override
    public List<GridProduct> findAllActive() {
        return productRepository.findByStatusOrderBySortOrder(1);
    }

    @Override
    public Optional<GridProduct> findByCode(String code) {
        return productRepository.findByCode(code);
    }

    @Override
    public List<RegionPricing> getPricingByProductAndRegion(Integer productId, String region) {
        return pricingRepository.findByProductIdAndRegion(productId, region);
    }

    @Override
    public Optional<RegionPricing> getPricing(Integer productId, String region, String billingCycle) {
        return pricingRepository.findByProductIdAndRegionAndBillingCycle(productId, region, billingCycle);
    }

    public Optional<RegionPricing> getPricing(Integer productId, String region, String billingCycle, String currency) {
        return pricingRepository.findByProductIdAndRegionAndBillingCycleAndCurrency(productId, region, billingCycle, currency);
    }
}
