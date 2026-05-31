package com.naon.grid.backend.service.vocabulary;

import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface VocabOutlineRecordService {

    /**
     * 记录纲外词（如果符合条件）
     * @param searchWord 用户原始搜索词
     */
    void recordIfNeeded(String searchWord);

    /**
     * 分页查询纲外词
     */
    PageResult<VocabOutlineRecordDto> queryAll(VocabOutlineRecordQueryCriteria criteria, Pageable pageable);

    /**
     * 标记为已处理
     */
    void markAsCompleted(Integer id);
}
