package com.naon.grid.backend.service.culture.mapstruct;

import com.naon.grid.backend.domain.culture.Culture;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.alibaba.fastjson2.JSON;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CultureMapper {

    @Mapping(target = "sentenceIds", source = "sentenceIds", qualifiedByName = "jsonToLongList")
    @Mapping(target = "questionIds", source = "questionIds", qualifiedByName = "jsonToLongList")
    CultureDto toDto(Culture entity);

    @Named("jsonToLongList")
    default List<Long> jsonToLongList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        return JSON.parseArray(json, Long.class);
    }
}
