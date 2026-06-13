package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabSense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabSenseRepository extends JpaRepository<VocabSense, Integer>, JpaSpecificationExecutor<VocabSense> {
    List<VocabSense> findByWordId(Integer wordId);
    List<VocabSense> findByWordIdAndStatus(Integer wordId, Integer status);
    List<VocabSense> findByWordIdInAndStatus(List<Integer> wordIds, Integer status);

    /**
     * 统计每个词汇的有效义项数
     */
    @Query(value = "select word_id, count(*) from vocab_sense where word_id in ?1 and status = ?2 group by word_id", nativeQuery = true)
    List<Object[]> countByWordIdInGroupByWordId(List<Integer> wordIds, Integer status);
}
