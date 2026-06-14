package com.naon.grid.backend.service.common;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ExampleSentenceService {

    /** 根据 ID 查询单条例句（仅返回 status=ENABLED 的） */
    ExampleSentenceDto findById(Long id);

    /** 批量查询例句（按 ID），返回 Map<id, Dto>，仅包含 ENABLED */
    Map<Long, ExampleSentenceDto> findByIds(Collection<Long> ids);

    /** 查询某 structure 的所有启用例句，按 order 降序 */
    List<ExampleSentenceDto> findByStructureId(Long structureId);

    /** 批量查询多个 structure 的例句，返回 Map<structureId, List<Dto>> */
    Map<Long, List<ExampleSentenceDto>> findByStructureIds(Collection<Long> structureIds);

    /** 创建或更新一条例句（id=null 新增，id!=null 更新） */
    ExampleSentenceDto save(ExampleSentenceDto dto);

    /** 软删除一条例句 */
    void disableById(Long id);

    /** 批量软删除 */
    void disableByIds(Collection<Long> ids);

    /** 软删除某 structure 的所有例句 */
    void disableByStructureId(Long structureId);

    /** 批量软删除多个 structure 的所有例句 */
    void disableByStructureIds(Collection<Long> structureIds);
}
