package com.naon.grid.modules.system.repository;

import com.naon.grid.modules.system.domain.GridAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridAgentRepository extends JpaRepository<GridAgent, Integer>,
        JpaSpecificationExecutor<GridAgent> {
    Optional<GridAgent> findByReferralCode(String referralCode);
    List<GridAgent> findByAuditStatus(String auditStatus);
}
