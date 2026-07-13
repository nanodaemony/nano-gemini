package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.topic.TopicService;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.modules.app.domain.BizCollectionFolder;
import com.naon.grid.modules.app.domain.BizCollectionItem;
import com.naon.grid.modules.app.rest.vo.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 收藏夹 VO 包装器
 */
public class CollectionWrapper {

    public static CollectionFolderVO toFolderVO(BizCollectionFolder folder, long itemCount) {
        CollectionFolderVO vo = new CollectionFolderVO();
        vo.setId(folder.getId());
        vo.setName(folder.getName());
        vo.setCoverImageId(folder.getCoverImageId());
        vo.setIsDefault(folder.getIsDefault() == 1);
        vo.setIsPinned(folder.getIsPinned() == 1);
        vo.setItemCount(itemCount);
        vo.setCreateTime(folder.getCreateTime());
        return vo;
    }

    public static List<CollectionFolderVO> toFolderVOList(List<BizCollectionFolder> folders,
            Map<Long, Long> itemCountMap) {
        if (folders == null) return Collections.emptyList();
        return folders.stream()
                .map(f -> toFolderVO(f, itemCountMap.getOrDefault(f.getId(), 0L)))
                .collect(Collectors.toList());
    }

    public static CollectionFolderDetailVO toDetailVO(
            BizCollectionFolder folder,
            Map<String, List<BizCollectionItem>> groupedItems,
            CharCharacterService charCharacterService,
            VocabWordService vocabWordService,
            CharRadicalService charRadicalService,
            GrammarPointService grammarPointService,
            GrammarComparisonGroupService grammarComparisonGroupService,
            VocabComparisonGroupService vocabComparisonGroupService,
            TopicService topicService) {

        CollectionFolderDetailVO vo = new CollectionFolderDetailVO();
        vo.setId(folder.getId());
        vo.setName(folder.getName());
        vo.setCoverImageId(folder.getCoverImageId());
        vo.setIsDefault(folder.getIsDefault() == 1);
        vo.setIsPinned(folder.getIsPinned() == 1);
        vo.setCreateTime(folder.getCreateTime());

        List<CollectionGroupVO> groups = new ArrayList<>();
        if (groupedItems != null) {
            for (Map.Entry<String, List<BizCollectionItem>> entry : groupedItems.entrySet()) {
                CollectionGroupVO group = new CollectionGroupVO();
                group.setBizType(entry.getKey());
                group.setItems(toItemVOList(entry.getValue(),
                        charCharacterService, vocabWordService, charRadicalService,
                        grammarPointService, grammarComparisonGroupService, vocabComparisonGroupService,
                        topicService));
                groups.add(group);
            }
        }
        vo.setGroups(groups);
        return vo;
    }

    public static List<CollectionItemVO> toItemVOList(
            List<BizCollectionItem> items,
            CharCharacterService charCharacterService,
            VocabWordService vocabWordService,
            CharRadicalService charRadicalService,
            GrammarPointService grammarPointService,
            GrammarComparisonGroupService grammarComparisonGroupService,
            VocabComparisonGroupService vocabComparisonGroupService,
            TopicService topicService) {

        if (items == null) return Collections.emptyList();
        return items.stream()
                .map(item -> toItemVO(item,
                        charCharacterService, vocabWordService, charRadicalService,
                        grammarPointService, grammarComparisonGroupService, vocabComparisonGroupService,
                        topicService))
                .collect(Collectors.toList());
    }

    public static CollectionItemVO toItemVO(
            BizCollectionItem item,
            CharCharacterService charCharacterService,
            VocabWordService vocabWordService,
            CharRadicalService charRadicalService,
            GrammarPointService grammarPointService,
            GrammarComparisonGroupService grammarComparisonGroupService,
            VocabComparisonGroupService vocabComparisonGroupService,
            TopicService topicService) {

        CollectionItemVO vo = new CollectionItemVO();
        vo.setId(item.getId());
        vo.setContentId(item.getContentId());
        vo.setContentText(item.getContentText());
        vo.setContentName(resolveContentName(item,
                charCharacterService, vocabWordService, charRadicalService,
                grammarPointService, grammarComparisonGroupService, vocabComparisonGroupService,
                topicService));
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    public static CollectionCheckVO toCheckVO(BizCollectionItem item, String folderName) {
        CollectionCheckVO vo = new CollectionCheckVO();
        if (item != null) {
            vo.setCollected(true);
            vo.setItemId(item.getId());
            vo.setFolderName(folderName);
        } else {
            vo.setCollected(false);
            vo.setItemId(null);
            vo.setFolderName(null);
        }
        return vo;
    }

    /**
     * 根据 bizType 动态查询 contentName
     */
    private static String resolveContentName(
            BizCollectionItem item,
            CharCharacterService charCharacterService,
            VocabWordService vocabWordService,
            CharRadicalService charRadicalService,
            GrammarPointService grammarPointService,
            GrammarComparisonGroupService grammarComparisonGroupService,
            VocabComparisonGroupService vocabComparisonGroupService,
            TopicService topicService) {

        // 纯文本类内容直接返回 contentText
        if (item.getContentId() == null && item.getContentText() != null) {
            return item.getContentText();
        }
        if (item.getContentId() == null) {
            return null;
        }

        try {
            switch (item.getBizType()) {
                case "CHARACTER": {
                    CharCharacterDto dto = charCharacterService.findById(
                            item.getContentId().intValue());
                    return dto != null ? dto.getCharacter() : null;
                }
                case "VOCABULARY": {
                    VocabWordDto dto = vocabWordService.findById(
                            item.getContentId().intValue());
                    return dto != null ? dto.getWord() : null;
                }
                case "RADICAL": {
                    CharRadicalDto dto = charRadicalService.findById(
                            item.getContentId());
                    return dto != null ? dto.getRadical() : null;
                }
                case "GRAMMAR": {
                    GrammarPointDto dto = grammarPointService.findById(
                            item.getContentId());
                    return dto != null ? dto.getName() : null;
                }
                case "GRAMMAR_COMPARISON": {
                    GrammarComparisonGroupDto dto = grammarComparisonGroupService.findById(
                            item.getContentId());
                    return dto != null ? dto.getGroupKey() : null;
                }
                case "VOCAB_COMPARISON": {
                    VocabComparisonGroupDto dto = vocabComparisonGroupService.findById(
                            item.getContentId());
                    return dto != null ? dto.getGroupKey() : null;
                }
                case "TOPIC": {
                    TopicDto topicDto = topicService.findById(item.getContentId());
                    return topicDto.getName();
                }
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
