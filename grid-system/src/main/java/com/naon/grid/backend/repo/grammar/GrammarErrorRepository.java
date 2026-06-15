package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GrammarErrorRepository extends JpaRepository<GrammarError, Long> {

    List<GrammarError> findByGrammarIdAndStatus(Long grammarId, Integer status);

    @Query("SELECT e.grammarId, COUNT(e) FROM GrammarError e WHERE e.grammarId IN :grammarIds AND e.status = :status GROUP BY e.grammarId")
    List<Object[]> countByGrammarIdInGroupByGrammarId(@Param("grammarIds") Collection<Long> grammarIds, @Param("status") Integer status);
}
