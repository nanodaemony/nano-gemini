package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharCharacterRepository extends JpaRepository<CharCharacter, Integer>, JpaSpecificationExecutor<CharCharacter> {

    /**
     * 根据汉字模糊搜索（仅匹配character字段）
     * @param blurry 搜索关键词
     * @param status 状态
     * @return 匹配的汉字列表
     */
    @Query("SELECT c FROM CharCharacter c WHERE c.character LIKE %:blurry% AND c.status = :status")
    List<CharCharacter> findByCharacterContainingAndStatus(@Param("blurry") String blurry, @Param("status") Integer status);

    /**
     * 根据汉字模糊搜索（仅匹配character字段），只返回已发布的
     * @param blurry 搜索关键词
     * @param status 状态
     * @param publishStatus 发布状态
     * @return 匹配的汉字列表
     */
    @Query("SELECT c FROM CharCharacter c WHERE c.character LIKE %:blurry% AND c.status = :status AND c.publishStatus = :publishStatus")
    List<CharCharacter> findByCharacterContainingAndStatusAndPublishStatus(@Param("blurry") String blurry, @Param("status") Integer status, @Param("publishStatus") String publishStatus);

    /**
     * 根据 HSK 等级查询已发布的汉字
     *
     * @param level         HSK等级（对应实体字段 level，数据库列 hsk_level）
     * @param status        有效状态
     * @param publishStatus 发布状态
     * @return 匹配的汉字列表
     */
    List<CharCharacter> findByLevelAndStatusAndPublishStatus(String level, Integer status, String publishStatus);

    /**
     * 根据 ID 列表批量查询汉字（结果不保证顺序，需调用方自行排序）
     *
     * @param ids 汉字ID列表
     * @return 匹配的汉字列表
     */
    List<CharCharacter> findByIdIn(List<Integer> ids);

    /**
     * 根据 radicalId 分页查询已发布的汉字
     *
     * @param radicalId     部首ID
     * @param status        有效状态
     * @param publishStatus 发布状态
     * @param pageable      分页参数
     * @return 分页的汉字列表
     */
    Page<CharCharacter> findByRadicalIdAndStatusAndPublishStatus(Long radicalId, Integer status, String publishStatus, Pageable pageable);

    /**
     * 根据 radicalId 查询所有已发布的汉字（不分页）
     *
     * @param radicalId     部首ID
     * @param status        有效状态
     * @param publishStatus 发布状态
     * @return 汉字列表
     */
    List<CharCharacter> findByRadicalIdAndStatusAndPublishStatus(Long radicalId, Integer status, String publishStatus);

    /**
     * 按 HSK 等级列表随机取已发布汉字。
     *
     * @param levels HSK 等级代码列表
     * @param limit  返回数量上限
     * @return 随机汉字列表
     */
    @Query(value = "SELECT * FROM char_character WHERE hsk_level IN ?1 " +
        "AND status = 1 AND publish_status = 'published' " +
        "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<CharCharacter> findRandomPublishedByHskLevels(List<String> levels, int limit);

    /**
     * 按 HSK 等级列表随机取已发布且关联至少 2 条组词的汉字。
     *
     * @param levels HSK 等级代码列表
     * @param limit  返回数量上限
     * @return 随机汉字列表
     */
    @Query(value = "SELECT cc.* FROM char_character cc " +
        "INNER JOIN char_word cw ON cc.id = cw.char_id " +
        "WHERE cc.hsk_level IN ?1 AND cc.status = 1 AND cc.publish_status = 'published' " +
        "AND cw.status = 1 GROUP BY cc.id HAVING COUNT(cw.id) >= 2 " +
        "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<CharCharacter> findRandomPublishedWithMinWords(List<String> levels, int limit);
}
