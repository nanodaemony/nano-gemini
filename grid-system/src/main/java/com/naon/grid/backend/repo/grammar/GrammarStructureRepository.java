package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GrammarStructureRepository extends JpaRepository<GrammarStructure, Long> {

    List<GrammarStructure> findByGrammarIdAndStatus(Long grammarId, Integer status);

    @Query("SELECT s.grammarId, COUNT(s) FROM GrammarStructure s WHERE s.grammarId IN :grammarIds AND s.status = :status GROUP BY s.grammarId")
    List<Object[]> countByGrammarIdInGroupByGrammarId(@Param("grammarIds") Collection<Long> grammarIds, @Param("status") Integer status);
}
