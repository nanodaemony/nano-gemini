package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabWord;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
