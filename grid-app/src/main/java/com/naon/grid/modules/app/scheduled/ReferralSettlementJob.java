package com.naon.grid.modules.app.scheduled;

import com.naon.grid.modules.app.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReferralSettlementJob {

    private final ReferralService referralService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void settleReferralRewards() {
        log.info("Referral settlement job started");
        try {
            referralService.settlePendingRewards();
            log.info("Referral settlement job completed");
        } catch (Exception e) {
            log.error("Referral settlement job failed", e);
        }
    }
}
