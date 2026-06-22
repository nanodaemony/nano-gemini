package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.GridOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridOrderRepository extends JpaRepository<GridOrder, Long> {
    Optional<GridOrder> findByOrderNo(String orderNo);
    List<GridOrder> findByUserIdOrderByCreateTimeDesc(Long userId);
    List<GridOrder> findByOrgIdOrderByCreateTimeDesc(Integer orgId);
}
