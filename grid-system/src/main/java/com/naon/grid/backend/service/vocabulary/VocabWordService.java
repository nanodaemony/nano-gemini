package com.naon.grid.backend.service.vocabulary;

import com.naon.grid.backend.service.vocabulary.dto.VocabWordDraftDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface VocabWordService {

    PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable);

    VocabWordDto findById(Integer id);

    VocabWordDto findPublishedById(Integer id);

    Integer create(VocabWordDto resources);

    void update(Integer id, VocabWordDto resources);

    void delete(Integer id);

    /**
     * 获取草稿详情
     * @param id 词汇ID
     * @return 草稿DTO
     */
    VocabWordDraftDto getDraft(Integer id);

    /**
     * 保存草稿（创建或更新）
     * @param id 词汇ID（新建时为null）
     * @param draft 草稿DTO
     */
    void saveDraft(Integer id, VocabWordDraftDto draft);

    /**
     * 创建草稿
     * @param draft 草稿DTO
     * @return 创建的词汇ID
     */
    Integer createDraft(VocabWordDraftDto draft);

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
     * 从已发布内容创建草稿
     * @param id 词汇ID
     */
    void createDraftFromPublished(Integer id);
}
