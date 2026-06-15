package com.naon.grid.backend.service.grammar;

import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface GrammarPointService {

    PageResult<GrammarPointDto> queryAll(GrammarPointQueryCriteria criteria, Pageable pageable);

    GrammarPointDto findById(Long id);

    Long create(GrammarPointDto resources);

    void update(Long id, GrammarPointDto resources);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);
}
