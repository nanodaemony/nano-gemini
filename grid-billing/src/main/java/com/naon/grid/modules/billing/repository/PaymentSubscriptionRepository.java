package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.PaymentSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentSubscriptionRepository extends JpaRepository<PaymentSubscription, Long> {
    List<PaymentSubscription> findByUserIdAndStatus(Long userId, String status);
    Optional<PaymentSubscription> findByChannelAndChannelSubId(String channel, String channelSubId);
    Optional<PaymentSubscription> findByUserIdAndProductCodeAndStatus(Long userId, String productCode, String status);
}
