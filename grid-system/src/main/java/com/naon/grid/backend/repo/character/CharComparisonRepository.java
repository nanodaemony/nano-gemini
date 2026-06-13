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
}
