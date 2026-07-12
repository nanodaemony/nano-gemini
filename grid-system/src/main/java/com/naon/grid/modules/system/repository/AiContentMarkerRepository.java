package com.naon.grid.modules.system.repository;

import com.naon.grid.modules.system.domain.AiContentMarker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiContentMarkerRepository extends JpaRepository<AiContentMarker, Long> {

    List<AiContentMarker> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Modifying
    @Query("DELETE FROM AiContentMarker m WHERE m.entityType = :entityType AND m.entityId = :entityId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") String entityType,
                                       @Param("entityId") Long entityId);

    @Query("SELECT m FROM AiContentMarker m " +
           "WHERE CONCAT(m.entityType, ':', CAST(m.entityId AS string)) IN :keys")
    List<AiContentMarker> findByEntityKeys(@Param("keys") List<String> keys);
}
