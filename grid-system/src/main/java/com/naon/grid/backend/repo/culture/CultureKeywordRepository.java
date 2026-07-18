package com.naon.grid.backend.repo.culture;

import com.naon.grid.backend.domain.culture.CultureKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CultureKeywordRepository extends JpaRepository<CultureKeyword, Long> {

    List<CultureKeyword> findByCultureIdAndStatus(Long cultureId, Integer status);

    List<CultureKeyword> findByCultureIdInAndStatus(List<Long> cultureIds, Integer status);

    @Query("SELECT c.cultureId, COUNT(c) FROM CultureKeyword c WHERE c.cultureId IN :cultureIds AND c.status = :status GROUP BY c.cultureId")
    List<Object[]> countByCultureIdInGroupByCultureId(@Param("cultureIds") List<Long> cultureIds, @Param("status") Integer status);

    @Modifying
    @Query("UPDATE CultureKeyword c SET c.status = 0 WHERE c.id = :id")
    void softDeleteById(@Param("id") Long id);
}
