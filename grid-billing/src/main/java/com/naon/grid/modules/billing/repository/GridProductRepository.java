package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.GridProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridProductRepository extends JpaRepository<GridProduct, Integer>, JpaSpecificationExecutor<GridProduct> {
    Optional<GridProduct> findByCode(String code);
    List<GridProduct> findByStatusOrderBySortOrder(Integer status);
}
