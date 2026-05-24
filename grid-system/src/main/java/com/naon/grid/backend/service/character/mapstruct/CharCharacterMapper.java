package com.naon.grid.backend.service.character.mapstruct;

import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.base.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CharCharacterMapper extends BaseMapper<CharCharacterDto, CharCharacter> {
}
