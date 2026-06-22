package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.EntitlementSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EntitlementSourceRepository extends JpaRepository<EntitlementSource, Long> {
    List<EntitlementSource> findByUserIdAndProductCodeAndStatusOrderByGrantedAtAsc(
            Long userId, String productCode, String status);
    List<EntitlementSource> findByUserIdAndStatusOrderByGrantedAtAsc(Long userId, String status);
    List<EntitlementSource> findByUserIdOrderByGrantedAtAsc(Long userId);
}
