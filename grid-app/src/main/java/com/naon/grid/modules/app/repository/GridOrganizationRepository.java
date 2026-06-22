package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.GridOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GridOrganizationRepository extends JpaRepository<GridOrganization, Integer>,
        JpaSpecificationExecutor<GridOrganization> {
    List<GridOrganization> findByAuditStatus(String auditStatus);
}
