package com.naon.grid.backend.service.grammar.mapstruct;

import com.naon.grid.backend.domain.grammar.GrammarPoint;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.base.BaseMapper;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.utils.JsonUtils;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GrammarPointMapper extends BaseMapper<GrammarPointDto, GrammarPoint> {

    default String toTranslationJson(List<TextTranslation> list) {
        return JsonUtils.toTranslationJson(list);
    }

    default List<TextTranslation> toTranslationList(String json) {
        return JsonUtils.parseTranslationList(json);
    }
}
