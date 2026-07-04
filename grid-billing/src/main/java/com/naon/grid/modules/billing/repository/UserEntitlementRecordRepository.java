package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.UserEntitlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserEntitlementRecordRepository extends JpaRepository<UserEntitlementRecord, Long> {
    List<UserEntitlementRecord> findByUserIdAndEntitlementIdOrderByCreateTimeAsc(Long userId, Integer entitlementId);
    List<UserEntitlementRecord> findByUserIdOrderByCreateTimeAsc(Long userId);
    boolean existsByUserIdAndSourceType(Long userId, String sourceType);
}
