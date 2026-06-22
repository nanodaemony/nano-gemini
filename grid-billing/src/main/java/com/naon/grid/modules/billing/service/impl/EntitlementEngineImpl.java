package com.naon.grid.modules.billing.service.impl;

import com.naon.grid.modules.billing.domain.EntitlementSource;
import com.naon.grid.modules.billing.repository.EntitlementSourceRepository;
import com.naon.grid.modules.billing.repository.ProductModuleRepository;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import com.naon.grid.modules.billing.service.dto.EntitlementResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementEngineImpl implements EntitlementEngine {

    private static final String PLUS_PRODUCT_CODE = "PLUS";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final EntitlementSourceRepository sourceRepository;
    private final ProductModuleRepository productModuleRepository;

    @Override
    public EntitlementResult compute(Long userId) {
        List<EntitlementSource> sources = sourceRepository
                .findByUserIdAndStatusOrderByGrantedAtAsc(userId, STATUS_ACTIVE);

        // Group by productCode
        Map<String, List<EntitlementSource>> grouped = sources.stream()
                .collect(Collectors.groupingBy(EntitlementSource::getProductCode));

        List<EntitlementResult.ProductEntitlement> entitlements = new ArrayList<>();
        LocalDateTime overallExpireAt = null;

        for (Map.Entry<String, List<EntitlementSource>> entry : grouped.entrySet()) {
            LocalDateTime expireAt = computeStackedExpiry(entry.getValue());
            entitlements.add(EntitlementResult.ProductEntitlement.builder()
                    .productCode(entry.getKey())
                    .expireAt(expireAt)
                    .active(expireAt != null && expireAt.isAfter(LocalDateTime.now()))
                    .build());
            if (expireAt != null && (overallExpireAt == null || expireAt.isAfter(overallExpireAt))) {
                overallExpireAt = expireAt;
            }
        }

        return EntitlementResult.builder()
                .userId(userId)
                .productEntitlements(entitlements)
                .overallExpireAt(overallExpireAt)
                .build();
    }

    /**
     * 堆叠算法：对同一产品的多条来源，按 granted_at 排序后堆叠
     * cursor = max(cursor, source.granted_at) + source.duration_days
     */
    private LocalDateTime computeStackedExpiry(List<EntitlementSource> sources) {
        if (sources == null || sources.isEmpty()) return null;

        // Sort by grantedAt ascending
        sources.sort(Comparator.comparing(EntitlementSource::getGrantedAt));

        LocalDateTime cursor = LocalDateTime.now();

        for (EntitlementSource source : sources) {
            LocalDateTime grantTime = source.getGrantedAt();
            if (cursor.isBefore(grantTime)) {
                cursor = grantTime;
            }
            cursor = cursor.plusDays(source.getDurationDays());
        }

        return cursor;
    }

    @Override
    public boolean hasAccess(Long userId, String productCode) {
        EntitlementResult result = compute(userId);
        if (result.getProductEntitlements() == null) return false;

        // Check if user has PLUS (which includes all)
        boolean hasPlus = result.getProductEntitlements().stream()
                .anyMatch(e -> PLUS_PRODUCT_CODE.equals(e.getProductCode()) && e.isActive());

        if (hasPlus) return true;

        // Check specific product
        return result.getProductEntitlements().stream()
                .anyMatch(e -> productCode.equals(e.getProductCode()) && e.isActive());
    }

    @Override
    public boolean hasModuleAccess(Long userId, String moduleCode) {
        EntitlementResult result = compute(userId);
        if (result.getProductEntitlements() == null) return false;

        // Check PLUS first
        boolean hasPlus = result.getProductEntitlements().stream()
                .anyMatch(e -> PLUS_PRODUCT_CODE.equals(e.getProductCode()) && e.isActive());
        if (hasPlus) return true;

        // Check if user has a product that covers this module
        return result.getProductEntitlements().stream()
                .filter(EntitlementResult.ProductEntitlement::isActive)
                .anyMatch(e -> e.getProductCode().equals(moduleCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LocalDateTime grant(Long userId, String sourceType, String sourceId,
                               String productCode, int durationDays, String region) {
        // Compute current expiry for this product
        EntitlementResult result = compute(userId);
        LocalDateTime currentExpiry = null;
        if (result.getProductEntitlements() != null) {
            currentExpiry = result.getProductEntitlements().stream()
                    .filter(e -> productCode.equals(e.getProductCode()))
                    .findFirst()
                    .map(EntitlementResult.ProductEntitlement::getExpireAt)
                    .orElse(null);
        }

        // Stack: new expiry = max(currentExpiry, now) + durationDays
        LocalDateTime cursor = LocalDateTime.now();
        if (currentExpiry != null && currentExpiry.isAfter(cursor)) {
            cursor = currentExpiry;
        }
        LocalDateTime newExpireAt = cursor.plusDays(durationDays);

        EntitlementSource source = new EntitlementSource();
        source.setUserId(userId);
        source.setSourceType(sourceType);
        source.setSourceId(sourceId);
        source.setProductCode(productCode);
        source.setGrantedAt(LocalDateTime.now());
        source.setDurationDays(durationDays);
        source.setExpireAt(newExpireAt);
        source.setRegion(region);
        source.setStatus(STATUS_ACTIVE);
        source.setCreateTime(LocalDateTime.now());
        sourceRepository.save(source);

        log.info("Entitlement granted: userId={}, product={}, days={}, expireAt={}, source={}",
                userId, productCode, durationDays, newExpireAt, sourceType);

        return newExpireAt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long sourceId) {
        Optional<EntitlementSource> opt = sourceRepository.findById(sourceId);
        if (opt.isPresent()) {
            EntitlementSource source = opt.get();
            source.setStatus("REVOKED");
            sourceRepository.save(source);
            log.info("Entitlement revoked: sourceId={}, userId={}, product={}",
                    sourceId, source.getUserId(), source.getProductCode());
        }
    }

    @Override
    public boolean isValidForRegion(Long userId, String currentRegion) {
        List<EntitlementSource> sources = sourceRepository
                .findByUserIdAndStatusOrderByGrantedAtAsc(userId, STATUS_ACTIVE);

        boolean regionMatched = sources.stream()
                .anyMatch(s -> currentRegion.equals(s.getRegion()));

        if (!regionMatched) {
            log.warn("Region mismatch: userId={}, currentRegion={}, purchasedRegions={}",
                    userId, currentRegion,
                    sources.stream().map(EntitlementSource::getRegion).collect(Collectors.toSet()));
            // Phase 1: warn only, do not block
        }
        return true; // Phase 1: always pass
    }
}
