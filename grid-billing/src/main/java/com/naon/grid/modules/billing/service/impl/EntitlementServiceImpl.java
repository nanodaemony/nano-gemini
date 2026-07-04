package com.naon.grid.modules.billing.service.impl;

import com.naon.grid.modules.billing.domain.Entitlement;
import com.naon.grid.modules.billing.domain.UserEntitlement;
import com.naon.grid.modules.billing.domain.UserEntitlementRecord;
import com.naon.grid.modules.billing.repository.EntitlementRepository;
import com.naon.grid.modules.billing.repository.UserEntitlementRecordRepository;
import com.naon.grid.modules.billing.repository.UserEntitlementRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import com.naon.grid.modules.billing.service.dto.UserEntitlementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementServiceImpl implements EntitlementService {

    private final UserEntitlementRecordRepository recordRepository;
    private final UserEntitlementRepository userEntitlementRepository;
    private final EntitlementRepository entitlementRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantEntitlements(Long userId, List<Integer> entitlementIds,
                                  String sourceType, String sourceId,
                                  int durationDays, String region) {
        if (entitlementIds == null || entitlementIds.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();

        for (Integer entitlementId : entitlementIds) {
            // 1. Write ledger record
            UserEntitlementRecord record = new UserEntitlementRecord();
            record.setUserId(userId);
            record.setEntitlementId(entitlementId);
            record.setSourceType(sourceType);
            record.setSourceId(sourceId);
            record.setDurationDays(durationDays);
            record.setExpireAt(now.plusDays(durationDays));
            record.setRegion(region);
            record.setCreateTime(now);
            recordRepository.save(record);

            // 2. UPSERT summary with optimistic stacking
            Optional<UserEntitlement> existing =
                    userEntitlementRepository.findByUserIdAndEntitlementId(userId, entitlementId);

            LocalDateTime cursor = now;
            UserEntitlement userEntitlement;
            if (existing.isPresent()) {
                userEntitlement = existing.get();
                if (userEntitlement.getExpireAt() != null && userEntitlement.getExpireAt().isAfter(now)) {
                    cursor = userEntitlement.getExpireAt();
                }
                userEntitlement.setExpireAt(cursor.plusDays(durationDays));
                userEntitlement.setStatus("ACTIVE");
                userEntitlement.setUpdateTime(now);
            } else {
                userEntitlement = new UserEntitlement();
                userEntitlement.setUserId(userId);
                userEntitlement.setEntitlementId(entitlementId);
                userEntitlement.setExpireAt(cursor.plusDays(durationDays));
                userEntitlement.setStatus("ACTIVE");
                userEntitlement.setCreateTime(now);
                userEntitlement.setUpdateTime(now);
            }
            userEntitlementRepository.save(userEntitlement);
        }

        log.info("Granted {} entitlements: userId={}, sourceType={}, sourceId={}, days={}",
                entitlementIds.size(), userId, sourceType, sourceId, durationDays);
    }

    @Override
    public boolean hasModuleAccess(Long userId, String moduleCode) {
        Optional<Entitlement> entitlementOpt = entitlementRepository.findByModuleCode(moduleCode);
        if (!entitlementOpt.isPresent()) {
            return false; // Unknown module, deny
        }

        Integer entitlementId = entitlementOpt.get().getId();
        Optional<UserEntitlement> userEntitlement =
                userEntitlementRepository.findByUserIdAndEntitlementId(userId, entitlementId);

        return userEntitlement.isPresent()
                && "ACTIVE".equals(userEntitlement.get().getStatus())
                && userEntitlement.get().getExpireAt() != null
                && userEntitlement.get().getExpireAt().isAfter(LocalDateTime.now());
    }

    @Override
    public boolean hasReceivedTrial(Long userId) {
        return recordRepository.existsByUserIdAndSourceType(userId, "TRIAL");
    }

    @Override
    public List<UserEntitlementVO> getUserEntitlements(Long userId) {
        List<UserEntitlement> summaryList = userEntitlementRepository.findByUserId(userId);
        if (summaryList.isEmpty()) {
            return new ArrayList<>();
        }

        // Batch load entitlement metadata
        List<Entitlement> entitlements = entitlementRepository.findAllById(
                summaryList.stream().map(UserEntitlement::getEntitlementId)
                        .collect(Collectors.toList()));

        java.util.Map<Integer, Entitlement> entitlementMap = new java.util.HashMap<>();
        for (Entitlement e : entitlements) {
            entitlementMap.put(e.getId(), e);
        }

        LocalDateTime now = LocalDateTime.now();
        return summaryList.stream().map(s -> {
            Entitlement e = entitlementMap.get(s.getEntitlementId());
            return UserEntitlementVO.builder()
                    .entitlementId(s.getEntitlementId())
                    .entitlementCode(e != null ? e.getCode() : null)
                    .entitlementName(e != null ? e.getName() : null)
                    .moduleCode(e != null ? e.getModuleCode() : null)
                    .expireAt(s.getExpireAt())
                    .active("ACTIVE".equals(s.getStatus())
                            && s.getExpireAt() != null
                            && s.getExpireAt().isAfter(now))
                    .build();
        }).collect(Collectors.toList());
    }
}
