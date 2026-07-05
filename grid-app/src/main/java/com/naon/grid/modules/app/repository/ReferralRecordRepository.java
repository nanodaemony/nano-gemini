package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.ReferralRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRecordRepository extends JpaRepository<ReferralRecord, Long> {

    List<ReferralRecord> findByRewardStatus(String rewardStatus);

    List<ReferralRecord> findByReferrerId(Long referrerId);

    Optional<ReferralRecord> findFirstByReferredIdAndEventTypeOrderByCreateTimeDesc(
            Long referredId, String eventType);

    @Modifying
    @Query("UPDATE ReferralRecord r SET r.rewardStatus = 'SETTLED', r.settleTime = :now WHERE r.id IN :ids")
    int batchMarkSettled(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);
}
