package com.naon.grid.backend.service.character.mapstruct;

import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.base.BaseMapper;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.utils.JsonUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CharCharacterMapper extends BaseMapper<CharCharacterDto, CharCharacter> {

    @Override
    @Mapping(source = "level", target = "hskLevel")
    CharCharacterDto toDto(CharCharacter entity);

    default String toTranslationJson(List<TextTranslation> list) {
        return JsonUtils.toTranslationJson(list);
    }

    default List<TextTranslation> toTranslationList(String json) {
        return JsonUtils.parseTranslationList(json);
    }
}
