package com.naon.grid.backend.service.grammar;

import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface GrammarPointService {

    PageResult<GrammarPointDto> queryAll(GrammarPointQueryCriteria criteria, Pageable pageable);

    GrammarPointDto findById(Long id);

    /**
     * 查询已发布的语法点详情（不走草稿覆盖逻辑，仅返回发布态数据）
     */
    GrammarPointDto findPublishedById(Long id);

    /**
     * 按关键词搜索已发布的语法点
     */
    PageResult<GrammarPointDto> searchPublished(String keyword, Pageable pageable);

    Long create(GrammarPointDto resources);

    void update(Long id, GrammarPointDto resources);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);
}
