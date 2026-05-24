package com.naon.grid.backend.service.vocabulary.mapstruct;

import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.base.BaseMapper;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VocabWordMapper extends BaseMapper<VocabWordDto, VocabWord> {
}
