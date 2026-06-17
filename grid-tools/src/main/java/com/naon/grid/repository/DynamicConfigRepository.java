package com.naon.grid.repository;

import com.naon.grid.domain.DynamicConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DynamicConfigRepository
        extends JpaRepository<DynamicConfig, Long>,
                JpaSpecificationExecutor<DynamicConfig> {

    List<DynamicConfig> findByStatus(Integer status);
}
