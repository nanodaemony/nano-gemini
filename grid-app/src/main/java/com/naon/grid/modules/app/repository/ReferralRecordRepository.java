package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.ReferralRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRecordRepository extends JpaRepository<ReferralRecord, Long> {
    Optional<ReferralRecord> findByReferralCodeAndReferredId(String referralCode, Long referredId);
    List<ReferralRecord> findByReferrerId(Long referrerId);
    List<ReferralRecord> findByRewardStatus(String rewardStatus);
}
