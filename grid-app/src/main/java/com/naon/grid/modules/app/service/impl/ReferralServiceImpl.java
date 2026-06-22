package com.naon.grid.modules.app.service.impl;

import com.naon.grid.modules.app.domain.GridAgent;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.ReferralRecord;
import com.naon.grid.modules.app.repository.GridAgentRepository;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.repository.ReferralRecordRepository;
import com.naon.grid.modules.app.service.ReferralService;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {
    private final GridUserRepository userRepository;
    private final GridAgentRepository agentRepository;
    private final ReferralRecordRepository referralRecordRepository;
    private final GridOrderRepository orderRepository;
    private final EntitlementEngine entitlementEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long processReferral(String referralCode, Long referredUserId) {
        if (referralCode == null || referralCode.isEmpty()) return null;

        // Check if it belongs to an agent
        Optional<GridAgent> agentOpt = agentRepository.findByReferralCode(referralCode);
        if (agentOpt.isPresent() && "APPROVED".equals(agentOpt.get().getAuditStatus())) {
            GridAgent agent = agentOpt.get();
            ReferralRecord record = new ReferralRecord();
            record.setReferrerId(agent.getId().longValue());
            record.setReferrerType("AGENT");
            record.setReferredId(referredUserId);
            record.setReferralCode(referralCode);
            record.setRewardStatus("PENDING");
            record.setRewardType("CASH");
            record.setCreateTime(LocalDateTime.now());
            referralRecordRepository.save(record);
            return agent.getId().longValue();
        }

        // Check if it belongs to a user
        Optional<GridUser> userOpt = userRepository.findByReferralCode(referralCode);
        if (userOpt.isPresent() && !userOpt.get().getId().equals(referredUserId)) {
            GridUser referrer = userOpt.get();
            ReferralRecord record = new ReferralRecord();
            record.setReferrerId(referrer.getId());
            record.setReferrerType("NORMAL");
            record.setReferredId(referredUserId);
            record.setReferralCode(referralCode);
            record.setRewardStatus("PENDING");
            record.setRewardType("EXTEND_DAYS");
            record.setCreateTime(LocalDateTime.now());
            referralRecordRepository.save(record);
            return referrer.getId();
        }

        return null;
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
                // Grant 30 days PLUS
                entitlementEngine.grant(record.getReferrerId(), "REFERRAL",
                        String.valueOf(record.getId()), "PLUS", 30, null);
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
