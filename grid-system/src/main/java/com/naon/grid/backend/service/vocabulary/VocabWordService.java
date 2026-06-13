package com.naon.grid.backend.service.vocabulary;

import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VocabWordService {

    PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable);

    VocabWordDto findById(Integer id);

    VocabWordDto findPublishedById(Integer id);

    Integer create(VocabWordDto resources);

    void update(Integer id, VocabWordDto resources);

    void delete(Integer id);

    /**
     * 审核草稿
     * @param id 词汇ID
     */
    void reviewDraft(Integer id);

    /**
     * 发布草稿（同步到正式字段）
     * @param id 词汇ID
     */
    void publishDraft(Integer id);

    /**
     * 下线词汇（从正式字段逻辑删除）
     * @param id 词汇ID
     */
    void offline(Integer id);

    /**
     * 根据词汇文本精确搜索已发布的词汇及其义项
     * @param word 词汇文本（精确匹配）
     * @return 匹配的已发布词汇列表，无匹配返回空列表
     */
    List<VocabWordDto> searchByWord(String word);
}
