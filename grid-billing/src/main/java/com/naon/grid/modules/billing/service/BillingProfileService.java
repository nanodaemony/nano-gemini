package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.domain.BillingProfile;
import java.util.Optional;

public interface BillingProfileService {
    BillingProfile saveOrUpdate(BillingProfile profile);
    Optional<BillingProfile> findByUserAndOrg(Long userId, Integer orgId);
    Optional<BillingProfile> findByUser(Long userId);
    Optional<BillingProfile> findByOrg(Integer orgId);
}
