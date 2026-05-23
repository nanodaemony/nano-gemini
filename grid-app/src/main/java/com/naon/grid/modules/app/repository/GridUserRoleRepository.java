package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.GridUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridUserRoleRepository extends JpaRepository<GridUserRole, Long>, JpaSpecificationExecutor<GridUserRole> {

    List<GridUserRole> findByUserId(Long userId);

    Optional<GridUserRole> findByUserIdAndRoleCode(Long userId, String roleCode);

    List<GridUserRole> findByUserIdAndExpireTimeAfterOrExpireTimeIsNull(Long userId, Date now);
}
