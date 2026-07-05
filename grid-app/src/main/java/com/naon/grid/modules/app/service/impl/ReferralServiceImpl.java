package com.naon.grid.modules.app.service.impl;

import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.ReferralRecord;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.repository.ReferralRecordRepository;
import com.naon.grid.modules.app.service.ReferralService;
import com.naon.grid.modules.billing.repository.EntitlementRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.repository.GridOrganizationRepository;
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
public class ReferralServiceImpl implements ReferralService {

    private final GridUserRepository userRepository;
    private final GridOrganizationRepository organizationRepository;
    private final ReferralRecordRepository referralRecordRepository;
    private final EntitlementService entitlementService;
    private final EntitlementRepository entitlementRepository;

    private static final List<String> ALL_ENTITLEMENT_CODES = Arrays.asList(
            "VOCAB_ACCESS", "GRAMMAR_ACCESS", "CHARACTER_ACCESS",
            "CONFUSING_WORDS_ACCESS", "CULTURE_ACCESS", "TOPIC_ACCESS");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordEvent(String referralCode, Long referredUserId, String eventType) {
        return recordEvent(referralCode, referredUserId, eventType, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordEvent(String referralCode, Long referredUserId,
                            String eventType, Integer referredOrgId) {
        if (referralCode == null || referralCode.isEmpty()) return null;

        GridUser referrer = userRepository.findByReferralCode(referralCode).orElse(null);
        if (referrer == null) return null;
        if (referrer.getId().equals(referredUserId)) return null;

        String referrerType = resolveReferrerType(referrer);
        Integer referrerOrgId = referrer.getOrgId();

        ReferralRecord record = new ReferralRecord();
        record.setReferrerId(referrer.getId());
        record.setReferrerType(referrerType);
        record.setReferrerOrgId(referrerOrgId);
        record.setReferredId(referredUserId);
        record.setReferredOrgId(referredOrgId);
        record.setReferralCode(referralCode);
        record.setEventType(eventType);
        record.setRewardStatus("PENDING");
        record.setCreateTime(LocalDateTime.now());
        referralRecordRepository.save(record);

        log.info("Referral event recorded: referrerId={}, type={}, event={}, referredId={}",
                referrer.getId(), referrerType, eventType, referredUserId);
        return referrer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settlePendingRewards() {
        List<ReferralRecord> pending = referralRecordRepository.findByRewardStatus("PENDING");
        if (pending.isEmpty()) return;

        // Group by composite key
        Map<SettleKey, List<ReferralRecord>> groups = pending.stream()
                .collect(Collectors.groupingBy(SettleKey::from));

        String batchId = "REFERRAL_" + LocalDateTime.now().toLocalDate();
        LocalDateTime now = LocalDateTime.now();
        List<Long> settledIds = new ArrayList<>();

        for (Map.Entry<SettleKey, List<ReferralRecord>> entry : groups.entrySet()) {
            SettleKey key = entry.getKey();
            List<ReferralRecord> records = entry.getValue();
            int count = records.size();

            int daysPerEvent = resolveDaysPerEvent(
                    key.referrerType, key.eventType, key.referredOrgId != null);
            if (daysPerEvent <= 0) continue;

            int totalDays;
            int settledCount;

            if ("INSTITUTION".equals(key.referrerType) && key.referredOrgId == null) {
                // Threshold-based: institution → normal user
                int threshold = "REGISTER".equals(key.eventType) ? 100 : 10;
                settledCount = (count / threshold) * threshold;
                totalDays = count / threshold; // 1 day for all members per threshold
            } else {
                // Per-event: total = count * daysPerEvent
                settledCount = count;
                totalDays = count * daysPerEvent;
            }

            if (totalDays <= 0) continue;

            // Grant entitlements
            List<Integer> entitleIds = resolveAllEntitlementIds();
            if ("INSTITUTION".equals(key.referrerType)) {
                grantToInstitutionMembers(key.referrerOrgId, entitleIds, batchId, totalDays);
            } else {
                entitlementService.grantEntitlements(
                        key.referrerId, entitleIds, "REFERRAL", batchId, totalDays, null);
            }

            // Mark settled
            List<Long> ids = records.subList(0, settledCount).stream()
                    .map(ReferralRecord::getId)
                    .collect(Collectors.toList());
            settledIds.addAll(ids);

            log.info("Referral settled: referrerId={}, type={}, event={}, "
                    + "isInstitution={}, count={}, settledCount={}, totalDays={}",
                    key.referrerId, key.referrerType, key.eventType,
                    key.referredOrgId != null, count, settledCount, totalDays);
        }

        if (!settledIds.isEmpty()) {
            referralRecordRepository.batchMarkSettled(settledIds, now);
        }
    }

    private String resolveReferrerType(GridUser referrer) {
        if (referrer.getOrgId() != null) {
            GridOrganization org = organizationRepository.findById(referrer.getOrgId()).orElse(null);
            if (org != null) {
                if ("AGENT".equals(org.getOrgRole())) return "AGENT";
                return "INSTITUTION";
            }
        }
        return "NORMAL";
    }

    /**
     * 按奖励矩阵返回每个事件的奖励天数
     */
    private int resolveDaysPerEvent(String referrerType, String eventType, boolean isInstitutionReferred) {
        if ("NORMAL".equals(referrerType) || "AGENT".equals(referrerType)) {
            if ("REGISTER".equals(eventType)) return isInstitutionReferred ? 10 : 1;
            if ("SUBSCRIBE".equals(eventType)) return isInstitutionReferred ? 100 : 10;
        }
        if ("INSTITUTION".equals(referrerType)) {
            if (isInstitutionReferred) {
                if ("REGISTER".equals(eventType)) return 1;
                if ("SUBSCRIBE".equals(eventType)) return 10;
            }
            // institution → normal user: handled by threshold logic in settlePendingRewards
            return 1; // value ignored — threshold branch computes totalDays independently
        }
        return 0;
    }

    private List<Integer> resolveAllEntitlementIds() {
        return ALL_ENTITLEMENT_CODES.stream()
                .map(code -> entitlementRepository.findByCode(code)
                        .orElseThrow(() -> new RuntimeException("Entitlement not found: " + code)))
                .map(e -> e.getId())
                .collect(Collectors.toList());
    }

    private void grantToInstitutionMembers(Integer orgId, List<Integer> entitlementIds,
                                           String batchId, int days) {
        if (orgId == null) return;
        List<GridUser> members = userRepository.findByOrgId(orgId);
        for (GridUser member : members) {
            entitlementService.grantEntitlements(
                    member.getId(), entitlementIds, "REFERRAL", batchId, days, null);
        }
        log.info("Granted {} days to {} members of orgId={}", days, members.size(), orgId);
    }

    /**
     * 分组键：用于聚合 PENDING 记录
     */
    private static class SettleKey {
        final Long referrerId;
        final String referrerType;
        final Integer referrerOrgId;
        final String eventType;
        final Integer referredOrgId; // null = 普通用户, non-null = 机构

        SettleKey(Long referrerId, String referrerType, Integer referrerOrgId,
                  String eventType, Integer referredOrgId) {
            this.referrerId = referrerId;
            this.referrerType = referrerType;
            this.referrerOrgId = referrerOrgId;
            this.eventType = eventType;
            this.referredOrgId = referredOrgId;
        }

        static SettleKey from(ReferralRecord r) {
            return new SettleKey(r.getReferrerId(), r.getReferrerType(),
                    r.getReferrerOrgId(), r.getEventType(), r.getReferredOrgId());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SettleKey)) return false;
            SettleKey that = (SettleKey) o;
            return Objects.equals(referrerId, that.referrerId)
                    && Objects.equals(referrerType, that.referrerType)
                    && Objects.equals(referrerOrgId, that.referrerOrgId)
                    && Objects.equals(eventType, that.eventType)
                    && Objects.equals(referredOrgId, that.referredOrgId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(referrerId, referrerType, referrerOrgId, eventType, referredOrgId);
        }
    }
}
