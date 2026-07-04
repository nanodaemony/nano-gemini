package com.naon.grid.modules.app.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.ReferralRecord;
import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.repository.GridOrganizationRepository;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.repository.ReferralRecordRepository;
import com.naon.grid.modules.app.service.ReferralService;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.repository.EntitlementRepository;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.repository.GridProductRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {
    private final GridUserRepository userRepository;
    private final GridOrganizationRepository organizationRepository;
    private final ReferralRecordRepository referralRecordRepository;
    private final GridOrderRepository orderRepository;
    private final GridProductRepository productRepository;
    private final EntitlementRepository entitlementRepository;
    private final EntitlementService entitlementService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long processReferral(String referralCode, Long referredUserId) {
        if (referralCode == null || referralCode.isEmpty()) return null;

        // 按推荐码查找推荐人用户
        GridUser referrer = userRepository.findByReferralCode(referralCode).orElse(null);
        if (referrer == null) return null;

        // 不能自己推荐自己
        if (referrer.getId().equals(referredUserId)) return null;

        // 判断推荐人是否为代理机构成员
        boolean isAgent = false;
        if (referrer.getOrgId() != null) {
            isAgent = organizationRepository.findById(referrer.getOrgId())
                    .map(org -> "AGENT".equals(org.getOrgRole()))
                    .orElse(false);
        }

        String referrerType = isAgent ? "AGENT" : "NORMAL";
        String rewardType = isAgent ? "CASH" : "EXTEND_DAYS";

        ReferralRecord record = new ReferralRecord();
        record.setReferrerId(referrer.getId());
        record.setReferrerType(referrerType);
        record.setReferredId(referredUserId);
        record.setReferralCode(referralCode);
        record.setRewardStatus("PENDING");
        record.setRewardType(rewardType);
        record.setCreateTime(LocalDateTime.now());
        referralRecordRepository.save(record);

        return referrer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleReferralReward(Long referredUserId, String orderNo) {
        // Find the most recent referral record for this referred user
        Optional<ReferralRecord> recordOpt = referralRecordRepository
                .findFirstByReferredIdOrderByCreateTimeDesc(referredUserId);
        recordOpt.ifPresent(record -> {
            GridOrder order = orderRepository.findByOrderNo(orderNo).orElse(null);
            if (order == null || !"PENDING".equals(record.getRewardStatus())) return;

            if ("NORMAL".equals(record.getReferrerType())) {
                // Grant 30 days via product entitlements
                GridProduct product = productRepository.findByCode("PLUS").orElse(null);
                if (product != null && product.getEntitlementIds() != null
                        && !product.getEntitlementIds().isEmpty()) {
                    JSONArray arr = JSON.parseArray(product.getEntitlementIds());
                    List<Integer> ids = arr.stream()
                            .map(o -> o.toString())
                            .map(code -> entitlementRepository.findByCode(code))
                            .filter(Optional::isPresent)
                            .map(o -> o.get().getId())
                            .collect(Collectors.toList());
                    if (!ids.isEmpty()) {
                        entitlementService.grantEntitlements(
                                record.getReferrerId(), ids,
                                "REFERRAL", String.valueOf(record.getId()), 30, null);
                    }
                }
                record.setRewardAmount(BigDecimal.valueOf(30));
            } else if ("AGENT".equals(record.getReferrerType())) {
                // Calculate commission
                // Phase 1: simplified, actual commission calc later
                record.setRewardAmount(BigDecimal.ZERO);
            }
            record.setOrderId(order.getId());
            record.setRewardStatus("SETTLED");
            record.setSettleTime(LocalDateTime.now());
            referralRecordRepository.save(record);
            log.info("Referral reward settled: recordId={}, referrerId={}, type={}",
                    record.getId(), record.getReferrerId(), record.getReferrerType());
        });
    }
}
