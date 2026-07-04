package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.UserEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserEntitlementRepository extends JpaRepository<UserEntitlement, Long> {
    Optional<UserEntitlement> findByUserIdAndEntitlementId(Long userId, Integer entitlementId);
    List<UserEntitlement> findByUserIdAndStatus(Long userId, String status);
    List<UserEntitlement> findByUserId(Long userId);
}
