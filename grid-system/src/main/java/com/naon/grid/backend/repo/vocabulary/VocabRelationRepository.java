package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabRelationRepository extends JpaRepository<VocabRelation, Long> {
    List<VocabRelation> findBySenseIdAndStatus(Integer senseId, Integer status);
    List<VocabRelation> findBySenseIdInAndStatus(List<Integer> senseIds, Integer status);
}
