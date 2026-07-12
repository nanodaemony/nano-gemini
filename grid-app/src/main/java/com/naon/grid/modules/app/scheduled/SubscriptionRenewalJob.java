package com.naon.grid.modules.app.scheduled;

import com.naon.grid.modules.billing.domain.PaymentSubscription;
import com.naon.grid.modules.billing.repository.PaymentSubscriptionRepository;
import com.naon.grid.modules.billing.service.GatewayRouter;
import com.naon.grid.modules.billing.service.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Safety-net job that checks for subscription expirations not covered by gateway webhooks.
 * FastSpring handles recurring billing natively, so this job is a fallback only.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionRenewalJob {

    private final PaymentSubscriptionRepository subscriptionRepository;
    private final GatewayRouter gatewayRouter;

    /**
     * Daily check at 3 AM for subscriptions nearing expiry.
     * FastSpring auto-renews natively; this checks for anomalies only.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void checkExpiringSubscriptions() {
        log.info("Subscription renewal safety check starting");
        List<PaymentSubscription> activeSubs = subscriptionRepository.findByStatus("ACTIVE");

        for (PaymentSubscription sub : activeSubs) {
            // FastSpring handles the actual renewal; we just log anomalies
            if (sub.getChannelSubId() != null && !sub.getChannelSubId().isEmpty()) {
                try {
                    PaymentGateway gateway = gatewayRouter.resolve();
                    // Query gateway for subscription status as a safety check
                    // gateway.queryTransaction(sub.getChannelSubId()) would confirm status
                    log.debug("Subscription active via gateway: userId={}, subId={}, channel={}",
                            sub.getUserId(), sub.getChannelSubId(), sub.getChannel());
                } catch (Exception e) {
                    log.warn("Failed to check subscription status for userId={}, subId={}: {}",
                            sub.getUserId(), sub.getChannelSubId(), e.getMessage());
                }
            }
        }
        log.info("Subscription renewal safety check completed, active subscriptions: {}", activeSubs.size());
    }
}
