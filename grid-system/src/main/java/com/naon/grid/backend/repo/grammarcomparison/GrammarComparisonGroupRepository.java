package com.naon.grid.backend.repo.grammarcomparison;

import com.naon.grid.backend.domain.grammarcomparison.GrammarComparisonGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GrammarComparisonGroupRepository extends JpaRepository<GrammarComparisonGroup, Long>,
        JpaSpecificationExecutor<GrammarComparisonGroup> {
}
