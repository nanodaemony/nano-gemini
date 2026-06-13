package com.naon.grid.backend.repo.vocabcomparison;

import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabComparisonItemRepository extends JpaRepository<VocabComparisonItem, Long>,
        JpaSpecificationExecutor<VocabComparisonItem> {

    List<VocabComparisonItem> findByGroupIdAndStatus(Long groupId, Integer status);

    List<VocabComparisonItem> findByGroupIdInAndStatus(List<Long> groupIds, Integer status);

    List<VocabComparisonItem> findByWordAndStatus(String word, Integer status);

    List<VocabComparisonItem> findByWordIdAndStatus(Long wordId, Integer status);
}
