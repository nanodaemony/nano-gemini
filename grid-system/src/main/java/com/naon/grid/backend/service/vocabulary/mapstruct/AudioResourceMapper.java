package com.naon.grid.backend.service.vocabulary.mapstruct;

import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.base.BaseMapper;
import com.naon.grid.backend.domain.resource.AudioResource;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AudioResourceMapper extends BaseMapper<AudioResourceDto, AudioResource> {
}
