package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharComparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CharComparisonRepository extends JpaRepository<CharComparison, Integer> {

    List<CharComparison> findByCharId(Integer charId);

    List<CharComparison> findByCharIdAndStatus(Integer charId, Integer status);

    @Query("SELECT c.charId, COUNT(c) FROM CharComparison c WHERE c.charId IN :charIds AND c.status = :status GROUP BY c.charId")
    List<Object[]> countByCharIdInGroupByCharId(@Param("charIds") Collection<Integer> charIds, @Param("status") Integer status);

    /**
     * 随机取已启用（status=1）的辨析记录。
     *
     * @param limit 返回数量上限
     * @return 随机辨析记录列表
     */
    @Query(value = "SELECT * FROM char_comparison WHERE status = 1 " +
        "ORDER BY RAND() LIMIT ?1", nativeQuery = true)
    List<CharComparison> findRandomEnabled(int limit);

    /**
     * 按 charId 列表批量查询已启用的辨析记录（用于干扰项生成）。
     *
     * @param charIds 汉字ID列表
     * @param status  状态
     * @return 辨析记录列表
     */
    List<CharComparison> findByCharIdInAndStatus(List<Integer> charIds, Integer status);
}
