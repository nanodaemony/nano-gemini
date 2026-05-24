package com.naon.grid.backend.service.character;

import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface CharCharacterService {

    PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable);

    CharCharacterDto findById(Integer id);

    Integer create(CharCharacterDto resources);

    void update(Integer id, CharCharacterDto resources);

    void delete(Integer id);
}
