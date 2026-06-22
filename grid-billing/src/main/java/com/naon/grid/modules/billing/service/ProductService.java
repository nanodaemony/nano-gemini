package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.domain.RegionPricing;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<GridProduct> findAllActive();
    Optional<GridProduct> findByCode(String code);
    List<RegionPricing> getPricingByProductAndRegion(Integer productId, String region);
    Optional<RegionPricing> getPricing(Integer productId, String region, String billingCycle);
}
