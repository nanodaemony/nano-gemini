package com.naon.grid.backend.service.grammarcomparison;

import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GrammarComparisonGroupService {

    PageResult<GrammarComparisonGroupDto> queryAll(GrammarComparisonGroupQueryCriteria criteria, Pageable pageable);

    GrammarComparisonGroupDto findById(Long id);

    Long create(GrammarComparisonGroupDto resources);

    void update(Long id, GrammarComparisonGroupDto resources);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);

    /**
     * 根据语法点名称搜索已发布的辨析组
     */
    List<GrammarComparisonGroupDto> searchByGrammarName(String grammarName);

    /**
     * 根据语法点ID搜索已发布的辨析组
     */
    List<GrammarComparisonGroupDto> searchByGrammarId(Long grammarId);

    /**
     * 根据语法点名称模糊搜索已发布的辨析组（LIKE %name%），最多返回 limit 条
     */
    List<GrammarComparisonGroupDto> searchByGrammarNameFuzzy(String name, int limit);
}
