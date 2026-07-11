package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CharWordRepository extends JpaRepository<CharWord, Integer> {
    List<CharWord> findByCharId(Integer charId);
    List<CharWord> findByCharIdAndStatus(Integer charId, Integer status);

    @Query("SELECT c.charId, COUNT(c) FROM CharWord c WHERE c.charId IN :charIds AND c.status = :status GROUP BY c.charId")
    List<Object[]> countByCharIdInGroupByCharId(@Param("charIds") Collection<Integer> charIds, @Param("status") Integer status);

    /**
     * 按 charId 列表批量查询已启用的组词。
     *
     * @param charIds 汉字ID列表
     * @param status  状态
     * @return 组词列表
     */
    List<CharWord> findByCharIdInAndStatus(List<Integer> charIds, Integer status);
}
