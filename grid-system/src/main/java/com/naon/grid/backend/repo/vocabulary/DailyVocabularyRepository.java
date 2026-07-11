package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.DailyVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyVocabularyRepository
        extends JpaRepository<DailyVocabulary, Integer>,
                JpaSpecificationExecutor<DailyVocabulary> {

    /**
     * 按展示日期、发布状态、有效状态查询（按 order 排序）
     */
    List<DailyVocabulary> findByDisplayDateAndPublishStatusAndStatusOrderByOrderAsc(
            LocalDate displayDate, String publishStatus, Integer status);

    /**
     * 查询指定月份内有内容的日期列表（仅已发布有效内容）
     */
    @Query(value = "SELECT DISTINCT d.display_date FROM daily_vocabulary d " +
            "WHERE d.display_date BETWEEN ?1 AND ?2 " +
            "AND d.publish_status = 'published' AND d.status = 1 " +
            "ORDER BY d.display_date ASC", nativeQuery = true)
    List<java.sql.Date> findDistinctDisplayDatesByMonth(LocalDate start, LocalDate end);
}
