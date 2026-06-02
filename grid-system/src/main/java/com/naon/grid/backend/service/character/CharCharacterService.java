package com.naon.grid.backend.service.character;

import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CharCharacterService {

    PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable);

    CharCharacterDto findById(Integer id);

    CharCharacterDto findPublishedById(Integer id);

    Integer create(CharCharacterDto resources);

    void update(Integer id, CharCharacterDto resources);

    void delete(Integer id);

    List<CharCharacterDto> searchByCharacter(String blurry);

    List<CharCharacterDto> searchPublishedByCharacter(String blurry);

    void reviewDraft(Integer id);

    void publishDraft(Integer id);

    void offline(Integer id);
}
