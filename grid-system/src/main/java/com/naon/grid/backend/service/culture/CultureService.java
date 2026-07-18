package com.naon.grid.backend.service.culture;

import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CultureService {

    PageResult<CultureDto> queryAll(CultureQueryCriteria criteria, Pageable pageable);

    CultureDto findById(Long id);

    CultureDto findPublishedById(Long id);

    Long create(CultureDto dto);

    void update(Long id, CultureDto dto);

    void delete(Long id);

    List<CultureDto> searchPublished(String blurry);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);
}
