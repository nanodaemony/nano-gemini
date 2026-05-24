package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabSense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabSenseRepository extends JpaRepository<VocabSense, Integer>, JpaSpecificationExecutor<VocabSense> {
    List<VocabSense> findByWordId(Integer wordId);
}
