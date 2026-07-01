package com.naon.grid.modules.system.repository;

import com.naon.grid.modules.system.domain.GridOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridOrganizationRepository extends JpaRepository<GridOrganization, Integer>,
        JpaSpecificationExecutor<GridOrganization> {
    List<GridOrganization> findByAuditStatus(String auditStatus);

    Optional<GridOrganization> findByContactEmail(String contactEmail);
}
