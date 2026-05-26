package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabStructureRepository extends JpaRepository<VocabStructure, Integer>, JpaSpecificationExecutor<VocabStructure> {
    List<VocabStructure> findBySenseId(Integer senseId);
    List<VocabStructure> findByWordId(Integer wordId);
    List<VocabStructure> findBySenseIdAndStatus(Integer senseId, Integer status);
}
