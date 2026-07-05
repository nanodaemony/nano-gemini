package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.modules.app.domain.BizCollectionFolder;
import com.naon.grid.modules.app.domain.BizCollectionItem;
import com.naon.grid.modules.app.rest.request.*;
import com.naon.grid.modules.app.rest.vo.*;
import com.naon.grid.modules.app.rest.wrapper.CollectionWrapper;
import com.naon.grid.modules.app.service.CollectionService;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/collection")
@Api(tags = "用户：收藏夹接口")
public class AppCollectionController {

    private final CollectionService collectionService;
    private final CharCharacterService charCharacterService;
    private final VocabWordService vocabWordService;
    private final CharRadicalService charRadicalService;
    private final GrammarPointService grammarPointService;
    private final GrammarComparisonGroupService grammarComparisonGroupService;
    private final VocabComparisonGroupService vocabComparisonGroupService;

    /**
     * 获取当前用户ID，未登录时回退为请求参数中的 userId（测试用）
     */
    private Long resolveUserId() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        if (userId != null) {
            return userId;
        }
        // 测试阶段回退：未认证时默认使用 userId=1
        return 1L;
    }

    @Log("新建收藏夹")
    @ApiOperation("新建收藏夹")
    @AnonymousPostMapping("/folder")
    public ResponseEntity<CollectionFolderVO> createFolder(
            @Validated @RequestBody CreateFolderRequest request) {
        Long userId = resolveUserId();
        BizCollectionFolder folder = collectionService.createFolder(
                userId, request.getName(), request.getCoverImageId());
        return new ResponseEntity<>(
                CollectionWrapper.toFolderVO(folder, 0), HttpStatus.OK);
    }

    @ApiOperation("查询我的收藏夹列表")
    @AnonymousGetMapping("/folder/list")
    public ResponseEntity<List<CollectionFolderVO>> listFolders() {
        Long userId = resolveUserId();
        List<BizCollectionFolder> folders = collectionService.listFolders(userId);
        Map<Long, Long> itemCountMap = new java.util.HashMap<>();
        for (BizCollectionFolder f : folders) {
            itemCountMap.put(f.getId(), collectionService.countActiveItems(f.getId()));
        }
        return ResponseEntity.ok(CollectionWrapper.toFolderVOList(folders, itemCountMap));
    }

    @ApiOperation("查询收藏夹详情")
    @AnonymousGetMapping("/folder/{folderId}")
    public ResponseEntity<CollectionFolderDetailVO> getFolderDetail(
            @PathVariable Long folderId) {
        Long userId = resolveUserId();
        BizCollectionFolder folder = collectionService.getFolder(folderId, userId);
        Map<String, List<BizCollectionItem>> groupedItems =
                collectionService.getFolderItemsGrouped(folderId);
        return ResponseEntity.ok(CollectionWrapper.toDetailVO(
                folder, groupedItems,
                charCharacterService, vocabWordService, charRadicalService,
                grammarPointService, grammarComparisonGroupService, vocabComparisonGroupService));
    }

    @Log("修改收藏夹名称")
    @ApiOperation("修改收藏夹名称")
    @AnonymousPutMapping("/folder/{folderId}/name")
    public ResponseEntity<Void> updateFolderName(
            @PathVariable Long folderId,
            @Validated @RequestBody UpdateFolderNameRequest request) {
        Long userId = resolveUserId();
        collectionService.updateFolderName(folderId, userId, request.getName());
        return ResponseEntity.ok().build();
    }

    @Log("修改收藏夹封面图")
    @ApiOperation("修改收藏夹封面图")
    @AnonymousPutMapping("/folder/{folderId}/cover")
    public ResponseEntity<Void> updateFolderCover(
            @PathVariable Long folderId,
            @Validated @RequestBody UpdateFolderCoverRequest request) {
        Long userId = resolveUserId();
        collectionService.updateFolderCover(folderId, userId, request.getCoverImageId());
        return ResponseEntity.ok().build();
    }

    @Log("删除收藏夹")
    @ApiOperation("删除收藏夹")
    @AnonymousDeleteMapping("/folder/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long folderId) {
        Long userId = resolveUserId();
        collectionService.deleteFolder(folderId, userId);
        return ResponseEntity.ok().build();
    }

    @Log("置顶收藏夹")
    @ApiOperation("置顶收藏夹")
    @AnonymousPutMapping("/folder/{folderId}/pin")
    public ResponseEntity<Void> pinFolder(@PathVariable Long folderId) {
        Long userId = resolveUserId();
        collectionService.pinFolder(folderId, userId);
        return ResponseEntity.ok().build();
    }

    @Log("取消置顶收藏夹")
    @ApiOperation("取消置顶收藏夹")
    @AnonymousPutMapping("/folder/{folderId}/unpin")
    public ResponseEntity<Void> unpinFolder(@PathVariable Long folderId) {
        Long userId = resolveUserId();
        collectionService.unpinFolder(folderId, userId);
        return ResponseEntity.ok().build();
    }

    @Log("添加收藏")
    @ApiOperation("添加内容到收藏夹")
    @AnonymousPostMapping("/item")
    public ResponseEntity<Void> addItem(@Validated @RequestBody AddItemRequest request) {
        Long userId = resolveUserId();
        collectionService.addItem(userId, request.getFolderId(),
                request.getBizType(), request.getContentId(), request.getContentText());
        return ResponseEntity.ok().build();
    }

    @Log("取消收藏")
    @ApiOperation("取消收藏")
    @AnonymousDeleteMapping("/item/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long itemId) {
        Long userId = resolveUserId();
        collectionService.removeItem(itemId, userId);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("检查内容是否已收藏")
    @AnonymousGetMapping("/item/check")
    public ResponseEntity<CollectionCheckVO> checkCollected(
            @RequestParam String bizType,
            @RequestParam Long contentId) {
        Long userId = resolveUserId();
        BizCollectionItem item = collectionService.checkCollected(userId, bizType, contentId);
        String folderName = null;
        if (item != null) {
            try {
                BizCollectionFolder folder = collectionService.getFolder(
                        item.getFolderId(), userId);
                folderName = folder.getName();
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(CollectionWrapper.toCheckVO(item, folderName));
    }
}
