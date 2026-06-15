package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GrammarNoticeRepository extends JpaRepository<GrammarNotice, Long> {

    List<GrammarNotice> findByGrammarIdAndStatus(Long grammarId, Integer status);

    @Query("SELECT n.grammarId, COUNT(n) FROM GrammarNotice n WHERE n.grammarId IN :grammarIds AND n.status = :status GROUP BY n.grammarId")
    List<Object[]> countByGrammarIdInGroupByGrammarId(@Param("grammarIds") Collection<Long> grammarIds, @Param("status") Integer status);
}
