package com.naon.grid.backend.service.vocabulary;

import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface VocabWordService {

    PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable);

    VocabWordDto findById(Integer id);

    Integer create(VocabWordDto resources);

    void update(Integer id, VocabWordDto resources);

    void delete(Integer id);
}
