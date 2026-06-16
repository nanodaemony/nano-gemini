package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.GridUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridUserRoleRepository extends JpaRepository<GridUserRole, Long>, JpaSpecificationExecutor<GridUserRole> {

    List<GridUserRole> findByUserId(Long userId);

    Optional<GridUserRole> findByUserIdAndRoleCode(Long userId, String roleCode);

    List<GridUserRole> findByUserIdAndExpireTimeAfterOrExpireTimeIsNull(Long userId, Date now);

    /**
     * 查询用户当前有效的会员角色编码
     * expire_time 为 NULL（永久有效）或 > 当前时间
     */
    @Query("SELECT r.roleCode FROM GridUserRole r " +
           "WHERE r.userId = :userId " +
           "AND r.roleCode IN ('VIP', 'SVIP') " +
           "AND (r.expireTime IS NULL OR r.expireTime > :now)")
    List<String> findValidSubscriptionRoles(@Param("userId") Long userId,
                                            @Param("now") Date now);
}
