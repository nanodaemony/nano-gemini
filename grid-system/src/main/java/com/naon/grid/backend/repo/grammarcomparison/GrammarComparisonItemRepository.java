package com.naon.grid.backend.repo.grammarcomparison;

import com.naon.grid.backend.domain.grammarcomparison.GrammarComparisonItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrammarComparisonItemRepository extends JpaRepository<GrammarComparisonItem, Long>,
        JpaSpecificationExecutor<GrammarComparisonItem> {

    List<GrammarComparisonItem> findByGroupIdAndStatus(Long groupId, Integer status);

    List<GrammarComparisonItem> findByGroupIdInAndStatus(List<Long> groupIds, Integer status);

    List<GrammarComparisonItem> findByGrammarNameAndStatus(String grammarName, Integer status);

    List<GrammarComparisonItem> findByGrammarIdAndStatus(Long grammarId, Integer status);
}
