package com.naon.grid.modules.app.service.impl;

import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.BizCollectionFolder;
import com.naon.grid.modules.app.domain.BizCollectionItem;
import com.naon.grid.modules.app.enums.CollectionBizTypeEnum;
import com.naon.grid.modules.app.repository.BizCollectionFolderRepository;
import com.naon.grid.modules.app.repository.BizCollectionItemRepository;
import com.naon.grid.modules.app.service.CollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private static final int MAX_ITEMS_PER_FOLDER = 500;

    private final BizCollectionFolderRepository folderRepository;
    private final BizCollectionItemRepository itemRepository;
    private final CharCharacterService charCharacterService;
    private final VocabWordService vocabWordService;
    private final CharRadicalService charRadicalService;
    private final GrammarPointService grammarPointService;
    private final GrammarComparisonGroupService grammarComparisonGroupService;
    private final VocabComparisonGroupService vocabComparisonGroupService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizCollectionFolder createDefaultFolder(Long userId) {
        BizCollectionFolder folder = new BizCollectionFolder();
        folder.setUserId(userId);
        folder.setName("默认收藏夹");
        folder.setIsDefault(1);
        folder.setIsPinned(0);
        folder.setSortOrder(0);
        folder.setStatus(1);
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());
        return folderRepository.save(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizCollectionFolder createFolder(Long userId, String name, Long coverImageId) {
        BizCollectionFolder folder = new BizCollectionFolder();
        folder.setUserId(userId);
        folder.setName(name);
        folder.setCoverImageId(coverImageId);
        folder.setIsDefault(0);
        folder.setIsPinned(0);
        folder.setSortOrder(0);
        folder.setStatus(1);
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());
        return folderRepository.save(folder);
    }

    @Override
    public List<BizCollectionFolder> listFolders(Long userId) {
        return folderRepository.findByUserIdAndStatusOrderByIsPinnedDescCreateTimeDesc(userId, 1);
    }

    @Override
    public BizCollectionFolder getFolder(Long folderId, Long userId) {
        return folderRepository.findByIdAndUserIdAndStatus(folderId, userId, 1)
                .orElseThrow(() -> new BadRequestException("收藏夹不存在"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFolderName(Long folderId, Long userId, String name) {
        BizCollectionFolder folder = getFolder(folderId, userId);
        folder.setName(name);
        folder.setUpdateTime(LocalDateTime.now());
        folderRepository.save(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFolderCover(Long folderId, Long userId, Long coverImageId) {
        BizCollectionFolder folder = getFolder(folderId, userId);
        folder.setCoverImageId(coverImageId);
        folder.setUpdateTime(LocalDateTime.now());
        folderRepository.save(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long folderId, Long userId) {
        BizCollectionFolder folder = getFolder(folderId, userId);
        if (folder.getIsDefault() == 1) {
            throw new BadRequestException("默认收藏夹不可删除");
        }
        folderRepository.softDeleteById(folderId);
        itemRepository.softDeleteByFolderId(folderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pinFolder(Long folderId, Long userId) {
        BizCollectionFolder folder = getFolder(folderId, userId);
        folder.setIsPinned(1);
        folder.setUpdateTime(LocalDateTime.now());
        folderRepository.save(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpinFolder(Long folderId, Long userId) {
        BizCollectionFolder folder = getFolder(folderId, userId);
        folder.setIsPinned(0);
        folder.setUpdateTime(LocalDateTime.now());
        folderRepository.save(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(Long userId, Long folderId, String bizType, Long contentId, String contentText) {
        // 1. 校验 contentId 和 contentText 至少有一个
        if (contentId == null && (contentText == null || contentText.trim().isEmpty())) {
            throw new BadRequestException("contentId和contentText至少需要提供一个");
        }

        // 2. 确定目标收藏夹
        BizCollectionFolder folder;
        if (folderId != null) {
            folder = getFolder(folderId, userId);
        } else {
            folder = folderRepository.findByUserIdAndIsDefaultAndStatus(userId, 1, 1)
                    .orElseThrow(() -> new BadRequestException("未指定收藏夹且默认收藏夹不存在"));
        }

        // 3. 校验内容 ID 存在性（如果有 contentId）
        if (contentId != null) {
            validateContentExists(bizType, contentId);
        }

        // 4. 去重检查
        if (contentId != null) {
            Optional<BizCollectionItem> existing = itemRepository
                    .findByFolderIdAndUserIdAndBizTypeAndContentIdAndStatus(
                            folder.getId(), userId, bizType, contentId, 1);
            if (existing.isPresent()) {
                return; // 幂等忽略
            }
        }

        // 5. 检查 500 条上限
        long activeCount = itemRepository.countByFolderIdAndStatus(folder.getId(), 1);
        if (activeCount >= MAX_ITEMS_PER_FOLDER) {
            throw new BadRequestException("收藏夹已满，最多收藏" + MAX_ITEMS_PER_FOLDER + "条内容");
        }

        // 6. 创建收藏记录
        BizCollectionItem item = new BizCollectionItem();
        item.setFolderId(folder.getId());
        item.setUserId(userId);
        item.setBizType(bizType);
        item.setContentId(contentId);
        item.setContentText(contentText);
        item.setStatus(1);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        itemRepository.save(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId, Long userId) {
        BizCollectionItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BadRequestException("收藏记录不存在"));
        if (!item.getUserId().equals(userId)) {
            throw new BadRequestException("无权操作此收藏记录");
        }
        itemRepository.softDeleteById(itemId);
    }

    @Override
    public Map<String, List<BizCollectionItem>> getFolderItemsGrouped(Long folderId) {
        List<BizCollectionItem> items = itemRepository
                .findByFolderIdAndStatusOrderByCreateTimeDesc(folderId, 1);
        return items.stream()
                .collect(Collectors.groupingBy(BizCollectionItem::getBizType));
    }

    @Override
    public BizCollectionItem checkCollected(Long userId, String bizType, Long contentId) {
        return itemRepository
                .findFirstByUserIdAndBizTypeAndContentIdAndStatusOrderByCreateTimeDesc(
                        userId, bizType, contentId, 1)
                .orElse(null);
    }

    @Override
    public long countActiveItems(Long folderId) {
        return itemRepository.countByFolderIdAndStatus(folderId, 1);
    }

    /**
     * 根据业务类型分派校验 contentId 是否存在
     */
    private void validateContentExists(String bizType, Long contentId) {
        CollectionBizTypeEnum type = CollectionBizTypeEnum.fromCode(bizType);
        if (type == null) {
            // 未知类型，不做校验（兼容将来扩展）
            return;
        }
        switch (type) {
            case CHARACTER: {
                CharCharacterDto dto = charCharacterService.findById(contentId.intValue());
                if (dto == null || dto.getCharacter() == null) {
                    throw new BadRequestException("收藏的" + type.getDescription() + "不存在");
                }
                break;
            }
            case VOCABULARY: {
                VocabWordDto dto = vocabWordService.findById(contentId.intValue());
                if (dto == null || dto.getWord() == null) {
                    throw new BadRequestException("收藏的" + type.getDescription() + "不存在");
                }
                break;
            }
            case RADICAL:
                try {
                    charRadicalService.findById(contentId);
                } catch (Exception e) {
                    throw new BadRequestException("收藏的" + type.getDescription() + "不存在");
                }
                break;
            case GRAMMAR: {
                GrammarPointDto dto = grammarPointService.findById(contentId);
                if (dto == null || dto.getName() == null) {
                    throw new BadRequestException("收藏的" + type.getDescription() + "不存在");
                }
                break;
            }
            case GRAMMAR_COMPARISON:
                try {
                    grammarComparisonGroupService.findById(contentId);
                } catch (Exception e) {
                    throw new BadRequestException("收藏的" + type.getDescription() + "不存在");
                }
                break;
            case VOCAB_COMPARISON:
                try {
                    vocabComparisonGroupService.findById(contentId);
                } catch (Exception e) {
                    throw new BadRequestException("收藏的" + type.getDescription() + "不存在");
                }
                break;
            default:
                return; // 未知类型不校验
        }
    }
}
