package com.naon.grid.backend.service.vocabcomparison;

import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VocabComparisonGroupService {

    PageResult<VocabComparisonGroupDto> queryAll(VocabComparisonGroupQueryCriteria criteria, Pageable pageable);

    VocabComparisonGroupDto findById(Long id);

    Long create(VocabComparisonGroupDto resources);

    void update(Long id, VocabComparisonGroupDto resources);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);

    /**
     * 根据词汇文本精确搜索已发布的辨析组
     */
    List<VocabComparisonGroupDto> searchByWord(String word);

    /**
     * 根据词汇ID精确搜索已发布的辨析组
     */
    List<VocabComparisonGroupDto> searchByWordId(Long wordId);
}
