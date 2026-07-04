package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.Entitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EntitlementRepository extends JpaRepository<Entitlement, Integer> {
    Optional<Entitlement> findByCode(String code);
    Optional<Entitlement> findByModuleCode(String moduleCode);
    List<Entitlement> findByStatusOrderBySortOrder(Integer status);
    List<Entitlement> findByCodeIn(List<String> codes);
}
