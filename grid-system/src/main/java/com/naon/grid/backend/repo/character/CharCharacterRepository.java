package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
