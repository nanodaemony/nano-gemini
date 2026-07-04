package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.UserEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserEntitlementRepository extends JpaRepository<UserEntitlement, Long> {
    Optional<UserEntitlement> findByUserIdAndEntitlementId(Long userId, Integer entitlementId);
    List<UserEntitlement> findByUserIdAndStatus(Long userId, String status);
    List<UserEntitlement> findByUserId(Long userId);

    @Modifying
    @Query(value = "INSERT INTO user_entitlement (user_id, entitlement_id, expire_at, status, create_time, update_time) " +
            "VALUES (:userId, :entitlementId, :expireAt, 'ACTIVE', :now, :now) " +
            "ON DUPLICATE KEY UPDATE " +
            "expire_at = GREATEST(COALESCE(expire_at, :now), :now) + INTERVAL :durationDays DAY, " +
            "status = 'ACTIVE', update_time = :now",
            nativeQuery = true)
    void upsertUserEntitlement(@Param("userId") Long userId,
                               @Param("entitlementId") Integer entitlementId,
                               @Param("expireAt") LocalDateTime expireAt,
                               @Param("durationDays") int durationDays,
                               @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserEntitlement u SET u.status = 'EXPIRED', u.updateTime = :now " +
            "WHERE u.status = 'ACTIVE' AND u.expireAt IS NOT NULL AND u.expireAt <= :now")
    int expirePastDue(@Param("now") LocalDateTime now);
}
