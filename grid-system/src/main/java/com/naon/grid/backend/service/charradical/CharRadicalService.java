package com.naon.grid.backend.service.charradical;

import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.backend.service.charradical.dto.CharRadicalQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface CharRadicalService {

    PageResult<CharRadicalDto> queryAll(CharRadicalQueryCriteria criteria, Pageable pageable);

    CharRadicalDto findById(Long id);

    void update(Long id, CharRadicalDto resources);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);
}
