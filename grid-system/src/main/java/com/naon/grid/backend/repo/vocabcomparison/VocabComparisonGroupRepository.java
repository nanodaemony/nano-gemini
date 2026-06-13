package com.naon.grid.backend.repo.vocabcomparison;

import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabComparisonGroupRepository extends JpaRepository<VocabComparisonGroup, Long>,
        JpaSpecificationExecutor<VocabComparisonGroup> {
}
