package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GrammarQuestionRepository extends JpaRepository<GrammarQuestion, Long> {

    List<GrammarQuestion> findByGrammarIdAndStatus(Long grammarId, Integer status);

    List<GrammarQuestion> findByGrammarIdInAndStatus(Collection<Long> grammarIds, Integer status);
}
