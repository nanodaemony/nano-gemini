package com.naon.grid.backend.service.resource;

import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface AudioResourceService {

    PageResult<AudioResourceDto> queryAll(AudioResourceQueryCriteria criteria, Pageable pageable);

    AudioResourceDto findById(Long id);
}
