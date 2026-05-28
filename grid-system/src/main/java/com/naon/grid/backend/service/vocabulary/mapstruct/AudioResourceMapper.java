package com.naon.grid.backend.service.vocabulary.mapstruct;

import com.naon.grid.backend.enums.AudioFileFormatEnum;
import com.naon.grid.backend.enums.AudioSourceTypeEnum;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.base.BaseMapper;
import com.naon.grid.backend.domain.resource.AudioResource;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AudioResourceMapper extends BaseMapper<AudioResourceDto, AudioResource> {

    default String sourceTypeToString(AudioSourceTypeEnum sourceType) {
        return sourceType != null ? sourceType.getCode() : null;
    }

    default AudioSourceTypeEnum stringToSourceType(String sourceType) {
        return sourceType != null ? AudioSourceTypeEnum.fromCode(sourceType) : null;
    }

    default String fileFormatToString(AudioFileFormatEnum fileFormat) {
        return fileFormat != null ? fileFormat.getCode() : null;
    }

    default AudioFileFormatEnum stringToFileFormat(String fileFormat) {
        return fileFormat != null ? AudioFileFormatEnum.fromCode(fileFormat) : null;
    }
}
