package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.BillingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BillingProfileRepository extends JpaRepository<BillingProfile, Integer> {
    Optional<BillingProfile> findByUserIdAndOrgId(Long userId, Integer orgId);
    Optional<BillingProfile> findByUserIdAndOrgIdIsNull(Long userId);
    Optional<BillingProfile> findByOrgId(Integer orgId);
}
