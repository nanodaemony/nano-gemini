package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.RegionPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegionPricingRepository extends JpaRepository<RegionPricing, Integer> {
    Optional<RegionPricing> findByProductIdAndRegionAndBillingCycle(Integer productId, String region, String billingCycle);
    List<RegionPricing> findByProductIdAndRegion(Integer productId, String region);
    List<RegionPricing> findByProductId(Integer productId);
}
