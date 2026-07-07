package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabWordRepository extends JpaRepository<VocabWord, Integer>, JpaSpecificationExecutor<VocabWord> {

    List<VocabWord> findByWordAndStatus(String word, Integer status);

    /**
     * 根据 HSK 等级查询已发布的词汇
     *
     * @param hskLevel      HSK等级
     * @param status        有效状态
     * @param publishStatus 发布状态
     * @return 匹配的词汇列表
     */
    List<VocabWord> findByHskLevelAndStatusAndPublishStatus(String hskLevel, Integer status, String publishStatus);

    /**
     * 根据 ID 列表批量查询词汇（结果不保证顺序，需调用方自行排序）
     *
     * @param ids 词汇ID列表
     * @return 匹配的词汇列表
     */
    List<VocabWord> findByIdIn(List<Integer> ids);

    /**
     * 随机获取指定HSK等级的已发布且有配图的词汇
     *
     * @param hskLevel HSK等级
     * @param count    返回数量
     * @return 随机词汇列表
     */
    @Query(value = "SELECT DISTINCT vw.* FROM vocab_word vw " +
        "INNER JOIN vocab_sense vs ON vw.id = vs.word_id " +
        "WHERE vw.hsk_level = ?1 AND vw.status = 1 AND vw.publish_status = 'published' " +
        "AND vs.status = 1 AND vs.def_image_id IS NOT NULL " +
        "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<VocabWord> findRandomPublishedByHskLevel(String hskLevel, int count);
}
