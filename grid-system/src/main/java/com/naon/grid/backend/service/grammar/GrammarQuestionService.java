package com.naon.grid.backend.service.grammar;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GrammarQuestionService {

    List<Long> findByGrammarId(Long grammarId);

    Map<Long, List<Long>> findByGrammarIds(Collection<Long> grammarIds);

    void syncFromDraft(Long grammarId, List<Long> questionIds);
}
