package com.naon.grid.backend.service.vocabulary.mapstruct;

import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.base.BaseMapper;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.utils.JsonUtils;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VocabWordMapper extends BaseMapper<VocabWordDto, VocabWord> {

    default String toTranslationJson(List<TextTranslation> list) {
        return JsonUtils.toTranslationJson(list);
    }

    default List<TextTranslation> toTranslationList(String json) {
        return JsonUtils.parseTranslationList(json);
    }
}
