package com.naon.grid.backend.service.character;

import com.naon.grid.backend.service.character.dto.CharCharacterDraftDto;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CharCharacterService {

    PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable);

    CharCharacterDto findById(Integer id);

    Integer create(CharCharacterDto resources);

    void update(Integer id, CharCharacterDto resources);

    void delete(Integer id);

    /**
     * 根据汉字模糊搜索（仅匹配character字段）
     * @param blurry 搜索关键词
     * @return 匹配的汉字列表
     */
    List<CharCharacterDto> searchByCharacter(String blurry);

    /**
     * 根据汉字模糊搜索（仅匹配character字段），只返回已发布的
     * @param blurry 搜索关键词
     * @return 匹配的汉字列表
     */
    List<CharCharacterDto> searchPublishedByCharacter(String blurry);

    /**
     * 获取草稿详情
     * @param id 汉字ID
     * @return 草稿DTO
     */
    CharCharacterDraftDto getDraft(Integer id);

    /**
     * 保存草稿（创建或更新）
     * @param id 汉字ID（新建时为null）
     * @param draft 草稿DTO
     */
    void saveDraft(Integer id, CharCharacterDraftDto draft);

    /**
     * 创建草稿
     * @param draft 草稿DTO
     * @return 创建的汉字ID
     */
    Integer createDraft(CharCharacterDraftDto draft);

    /**
     * 审核草稿
     * @param id 汉字ID
     */
    void reviewDraft(Integer id);

    /**
     * 发布草稿（同步到正式字段）
     * @param id 汉字ID
     */
    void publishDraft(Integer id);

    /**
     * 下线词汇（从正式字段逻辑删除）
     * @param id 汉字ID
     */
    void offline(Integer id);

    /**
     * 从已发布内容创建草稿
     * @param id 汉字ID
     */
    void createDraftFromPublished(Integer id);
}
