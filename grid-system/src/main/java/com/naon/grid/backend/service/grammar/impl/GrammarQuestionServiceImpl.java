package com.naon.grid.backend.service.grammar.impl;

import com.naon.grid.backend.domain.grammar.GrammarQuestion;
import com.naon.grid.backend.repo.grammar.GrammarQuestionRepository;
import com.naon.grid.backend.service.grammar.GrammarQuestionService;
import com.naon.grid.enums.StatusEnum;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrammarQuestionServiceImpl implements GrammarQuestionService {

    private final GrammarQuestionRepository grammarQuestionRepository;

    @Override
    public List<Long> findByGrammarId(Long grammarId) {
        List<GrammarQuestion> records = grammarQuestionRepository
                .findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        return records.stream()
                .map(r -> parseQuestionIds(r.getQuestionIds()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<Long>> findByGrammarIds(Collection<Long> grammarIds) {
        if (grammarIds == null || grammarIds.isEmpty()) return Collections.emptyMap();
        List<GrammarQuestion> records = grammarQuestionRepository
                .findByGrammarIdInAndStatus(grammarIds, StatusEnum.ENABLED.getCode());
        return records.stream()
                .collect(Collectors.toMap(
                        GrammarQuestion::getGrammarId,
                        r -> parseQuestionIds(r.getQuestionIds()),
                        (a, b) -> a
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromDraft(Long grammarId, List<Long> questionIds) {
        // Soft-delete existing enabled records
        List<GrammarQuestion> existing = grammarQuestionRepository
                .findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        for (GrammarQuestion record : existing) {
            record.setStatus(StatusEnum.DISABLED.getCode());
        }

        // Create new record if questionIds is non-empty
        if (questionIds != null && !questionIds.isEmpty()) {
            GrammarQuestion entity = new GrammarQuestion();
            entity.setGrammarId(grammarId);
            entity.setQuestionIds(JSON.toJSONString(questionIds));
            entity.setOrder(0);
            entity.setStatus(StatusEnum.ENABLED.getCode());
            grammarQuestionRepository.save(entity);
        }
    }

    private List<Long> parseQuestionIds(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return JSON.parseArray(json, Long.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
