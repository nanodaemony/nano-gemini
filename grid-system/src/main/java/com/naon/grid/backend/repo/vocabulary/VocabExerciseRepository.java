package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabExerciseRepository extends JpaRepository<VocabExercise, Integer>, JpaSpecificationExecutor<VocabExercise> {
    List<VocabExercise> findByWordId(Integer wordId);
    List<VocabExercise> findByWordIdAndStatus(Integer wordId, Integer status);
}
