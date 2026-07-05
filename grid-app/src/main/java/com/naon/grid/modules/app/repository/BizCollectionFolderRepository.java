package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.BizCollectionFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BizCollectionFolderRepository extends JpaRepository<BizCollectionFolder, Long> {

    List<BizCollectionFolder> findByUserIdAndStatusOrderByIsPinnedDescCreateTimeDesc(
            Long userId, Integer status);

    Optional<BizCollectionFolder> findByUserIdAndIsDefaultAndStatus(
            Long userId, Integer isDefault, Integer status);

    Optional<BizCollectionFolder> findByIdAndUserIdAndStatus(
            Long id, Long userId, Integer status);

    @Modifying
    @Query("UPDATE BizCollectionFolder f SET f.status = 0 WHERE f.id = :folderId")
    int softDeleteById(@Param("folderId") Long folderId);
}
