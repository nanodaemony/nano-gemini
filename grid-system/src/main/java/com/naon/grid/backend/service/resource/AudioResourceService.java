package com.naon.grid.backend.service.resource;

import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AudioResourceService {

    Long create(AudioResourceDto resources);

    AudioResourceDto findById(Long id);

    List<AudioResourceDto> findByIds(List<Long> ids);

    PageResult<AudioResourceDto> queryAll(AudioResourceQueryCriteria criteria, Pageable pageable);

    void delete(Long id);
}
