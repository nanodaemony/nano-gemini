package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabExampleRepository extends JpaRepository<VocabExample, Integer>, JpaSpecificationExecutor<VocabExample> {
    List<VocabExample> findByStructureId(Integer structureId);
    List<VocabExample> findBySenseId(Integer senseId);
    List<VocabExample> findByWordId(Integer wordId);
    List<VocabExample> findByStructureIdAndStatus(Integer structureId, Integer status);
}
