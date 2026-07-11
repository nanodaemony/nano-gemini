package com.naon.grid.backend.service.vocabulary;

import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DailyVocabularyService {

    /** 分页查询 */
    PageResult<DailyVocabularyDto> queryAll(DailyVocabularyQueryCriteria criteria, Pageable pageable);

    /** 查详情（草稿态从 draftContent 覆盖） */
    DailyVocabularyDto findById(Integer id);

    /** 查已发布详情（从实表字段） */
    DailyVocabularyDto findPublishedById(Integer id);

    /** 创建，返回新 ID */
    Integer create(DailyVocabularyDto dto);

    /** 更新草稿 */
    void update(Integer id, DailyVocabularyDto dto);

    /** 软删除 */
    void delete(Integer id);

    /** 审核草稿 draft→reviewed */
    void reviewDraft(Integer id);

    /** 发布草稿 reviewed→published */
    void publishDraft(Integer id);

    /** 下线 published→unpublished */
    void offline(Integer id);

    /** 设置展示日期 */
    void schedule(Integer id, LocalDate date);

    /** 批量排期 */
    void batchSchedule(List<Integer> ids, List<LocalDate> dates);

    /** 获取今日主推（sort_order 最小的一条） */
    DailyVocabularyDto getTodayMain();

    /** 获取今日备选池（除主推外） */
    List<DailyVocabularyDto> getTodayBackups();

    /** 历史归档分页 */
    PageResult<DailyVocabularyDto> queryHistory(DailyVocabularyQueryCriteria criteria, Pageable pageable);

    /** 获取指定月份有内容的日期列表 */
    List<LocalDate> getCalendarDates(int year, int month);

    /** 保存分享图 ID */
    void saveShareImage(Integer id, Long imageId);
}
