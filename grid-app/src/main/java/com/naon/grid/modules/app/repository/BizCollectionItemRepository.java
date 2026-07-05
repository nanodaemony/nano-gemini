package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.BizCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BizCollectionItemRepository extends JpaRepository<BizCollectionItem, Long> {

    List<BizCollectionItem> findByFolderIdAndStatusOrderByCreateTimeDesc(
            Long folderId, Integer status);

    long countByFolderIdAndStatus(Long folderId, Integer status);

    Optional<BizCollectionItem> findByFolderIdAndUserIdAndBizTypeAndContentIdAndStatus(
            Long folderId, Long userId, String bizType, Long contentId, Integer status);

    Optional<BizCollectionItem> findFirstByUserIdAndBizTypeAndContentIdAndStatusOrderByCreateTimeDesc(
            Long userId, String bizType, Long contentId, Integer status);

    @Modifying
    @Query("UPDATE BizCollectionItem i SET i.status = 0 WHERE i.id = :itemId")
    int softDeleteById(@Param("itemId") Long itemId);

    @Modifying
    @Query("UPDATE BizCollectionItem i SET i.status = 0 WHERE i.folderId = :folderId")
    int softDeleteByFolderId(@Param("folderId") Long folderId);
}
