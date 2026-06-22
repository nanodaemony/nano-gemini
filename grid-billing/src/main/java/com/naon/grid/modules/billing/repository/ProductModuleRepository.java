package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.ProductModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductModuleRepository extends JpaRepository<ProductModule, Integer> {
    List<ProductModule> findByProductId(Integer productId);
    List<String> findModuleCodeByProductId(Integer productId);
}
