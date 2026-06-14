package com.naon.grid.backend.service.common;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;

import java.util.Collection;
import java.util.Map;

public interface ExampleSentenceService {

    /** 根据 ID 查询单条例句（仅返回 status=ENABLED 的） */
    ExampleSentenceDto findById(Long id);

    /** 批量查询例句（按 ID），返回 Map<id, Dto>，仅包含 ENABLED */
    Map<Long, ExampleSentenceDto> findByIds(Collection<Long> ids);

    /** 创建或更新一条例句（id=null 新增，id!=null 更新） */
    ExampleSentenceDto save(ExampleSentenceDto dto);

    /** 软删除一条例句 */
    void disableById(Long id);

    /** 批量软删除 */
    void disableByIds(Collection<Long> ids);
}
