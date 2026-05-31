package com.naon.grid.backend.service.vocabulary.mapstruct;

import com.naon.grid.backend.domain.vocabulary.VocabOutlineRecord;
import com.naon.grid.backend.rest.vo.VocabOutlineRecordVO;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordDto;
import com.naon.grid.base.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VocabOutlineRecordMapper extends BaseMapper<VocabOutlineRecordDto, VocabOutlineRecord> {

    VocabOutlineRecordVO toVo(VocabOutlineRecordDto dto);
}
