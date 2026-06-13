package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabStructureRepository extends JpaRepository<VocabStructure, Integer>, JpaSpecificationExecutor<VocabStructure> {
    List<VocabStructure> findBySenseId(Integer senseId);
    List<VocabStructure> findByWordId(Integer wordId);
    List<VocabStructure> findBySenseIdAndStatus(Integer senseId, Integer status);

    /**
     * 统计每个词汇的有效结构数
     */
    @Query(value = "select word_id, count(*) from vocab_structure where word_id in ?1 and status = ?2 group by word_id", nativeQuery = true)
    List<Object[]> countByWordIdInGroupByWordId(List<Integer> wordIds, Integer status);
}
