package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarMeaning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GrammarMeaningRepository extends JpaRepository<GrammarMeaning, Long> {

    List<GrammarMeaning> findByGrammarIdAndStatus(Long grammarId, Integer status);

    @Query("SELECT m.grammarId, COUNT(m) FROM GrammarMeaning m WHERE m.grammarId IN :grammarIds AND m.status = :status GROUP BY m.grammarId")
    List<Object[]> countByGrammarIdInGroupByGrammarId(@Param("grammarIds") Collection<Long> grammarIds, @Param("status") Integer status);
}
