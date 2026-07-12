package com.naon.grid.backend.service.vocabcomparison.impl;

import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonChat;
import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonGroup;
import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonItem;
import com.naon.grid.backend.repo.vocabcomparison.VocabComparisonChatRepository;
import com.naon.grid.backend.repo.vocabcomparison.VocabComparisonGroupRepository;
import com.naon.grid.backend.repo.vocabcomparison.VocabComparisonItemRepository;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonChatDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupQueryCriteria;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonItemDto;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.modules.system.service.AiContentMarkerService;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabComparisonGroupServiceImpl implements VocabComparisonGroupService {

    private final VocabComparisonGroupRepository groupRepository;
    private final VocabComparisonItemRepository itemRepository;
    private final VocabComparisonChatRepository chatRepository;
    private final ExampleSentenceService exampleSentenceService;
    private final AiContentMarkerService aiContentMarkerService;

    @Override
    public PageResult<VocabComparisonGroupDto> queryAll(VocabComparisonGroupQueryCriteria criteria, Pageable pageable) {
        // If word or wordId is provided, first query VocabComparisonItem to get matching group IDs
        List<Long> groupIds = null;
        if (criteria.getWord() != null && !criteria.getWord().trim().isEmpty()) {
            List<VocabComparisonItem> items = itemRepository.findByWordAndStatus(
                    criteria.getWord(), StatusEnum.ENABLED.getCode());
            groupIds = items.stream().map(VocabComparisonItem::getGroupId).distinct().collect(Collectors.toList());
            if (groupIds.isEmpty()) {
                return PageUtil.toPage(Collections.emptyList(), 0L);
            }
        } else if (criteria.getWordId() != null) {
            List<VocabComparisonItem> items = itemRepository.findByWordIdAndStatus(
                    criteria.getWordId(), StatusEnum.ENABLED.getCode());
            groupIds = items.stream().map(VocabComparisonItem::getGroupId).distinct().collect(Collectors.toList());
            if (groupIds.isEmpty()) {
                return PageUtil.toPage(Collections.emptyList(), 0L);
            }
        }

        final List<Long> finalGroupIds = groupIds;
        final String finalPublishStatus = criteria.getPublishStatus();
        final String finalEditStatus = criteria.getEditStatus();

        Page<VocabComparisonGroup> page = groupRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode()));

            if (finalGroupIds != null && !finalGroupIds.isEmpty()) {
                predicates.add(root.get("id").in(finalGroupIds));
            }
            if (finalPublishStatus != null && !finalPublishStatus.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("publishStatus"), finalPublishStatus));
            }
            if (finalEditStatus != null && !finalEditStatus.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("editStatus"), finalEditStatus));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        PageResult<VocabComparisonGroupDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        populateItemCounts(pageResult.getContent());
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VocabComparisonGroupDto findById(Long id) {
        VocabComparisonGroup entity = groupRepository.findById(id).orElse(null);
        if (entity == null || StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }

        // Draft or reviewed: deserialize draftContent JSON, overlay entity fields
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            if (entity.getDraftContent() == null) {
                throw new BadRequestException("草稿内容不存在");
            }
            VocabComparisonGroupDto dto;
            try {
                dto = JsonUtils.fromJson(entity.getDraftContent(), VocabComparisonGroupDto.class);
            } catch (Exception e) {
                throw new BadRequestException("草稿数据解析失败");
            }
            if (dto == null) {
                throw new BadRequestException("草稿内容不存在");
            }
            dto.setId(entity.getId());
            dto.setStatus(entity.getStatus());
            dto.setPublishStatus(entity.getPublishStatus());
            dto.setEditStatus(entity.getEditStatus());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setCreateBy(entity.getCreateBy());
            dto.setUpdateBy(entity.getUpdateBy());
            return dto;
        }

        // Published: load from formal tables
        VocabComparisonGroupDto dto = toBaseDto(entity);
        dto.setItems(loadItems(id));
        dto.setChats(loadChats(id));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(VocabComparisonGroupDto resources) {
        VocabComparisonGroup entity = new VocabComparisonGroup();
        entity.setStatus(StatusEnum.ENABLED.getCode());
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        entity.setGroupKey(resources.getGroupKey());
        entity.setGroupOrder(resources.getGroupOrder() != null ? resources.getGroupOrder() : 0);
        entity.setDraftContent(JsonUtils.toJson(resources));
        entity = groupRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, VocabComparisonGroupDto resources) {
        VocabComparisonGroup entity = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }

        // If reviewed or published, roll back to draft
        if (EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.PUBLISHED.getCode().equals(entity.getEditStatus())) {
            entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        entity.setDraftContent(JsonUtils.toJson(resources));
        groupRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        VocabComparisonGroup entity = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id)));
        entity.setStatus(StatusEnum.DISABLED.getCode());
        groupRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        VocabComparisonGroup entity = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }
        entity.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        groupRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id) {
        VocabComparisonGroup entity = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅已审核状态可发布");
        }

        // Parse draft content JSON
        VocabComparisonGroupDto draftDto;
        try {
            draftDto = JsonUtils.fromJson(entity.getDraftContent(), VocabComparisonGroupDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draftDto == null) {
            throw new BadRequestException("草稿数据解析失败");
        }

        // Update group main table fields
        entity.setGroupKey(draftDto.getGroupKey());
        entity.setGroupOrder(draftDto.getGroupOrder() != null ? draftDto.getGroupOrder() : 0);
        entity.setExerciseQuestionIds(draftDto.getExerciseQuestionIds());

        // Sync items: soft delete old → create new, capture saved items for marker collection
        List<VocabComparisonItem> savedItems = syncItems(id, draftDto.getItems());

        // Sync chats: soft delete old + disable example_sentences → create new → sync
        List<VocabComparisonChat> savedChats = syncChats(id, draftDto.getChats());

        // 物化 AI 内容标记
        List<AiContentMarkerService.MarkerEntry> markerEntries = new ArrayList<>();
        collectComparisonMarkers(draftDto.getItems(), savedItems,
                draftDto.getChats(), savedChats, markerEntries);
        aiContentMarkerService.batchReplace(markerEntries);

        // Update status and clear draft content
        entity.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        entity.setDraftContent(null);
        groupRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        VocabComparisonGroup entity = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        groupRepository.save(entity);
    }

    @Override
    public List<VocabComparisonGroupDto> searchByWord(String word) {
        List<VocabComparisonItem> items = itemRepository.findByWordAndStatus(word, StatusEnum.ENABLED.getCode());
        return searchPublishedGroups(items);
    }

    @Override
    public List<VocabComparisonGroupDto> searchByWordId(Long wordId) {
        List<VocabComparisonItem> items = itemRepository.findByWordIdAndStatus(wordId, StatusEnum.ENABLED.getCode());
        return searchPublishedGroups(items);
    }

    @Override
    public List<VocabComparisonGroupDto> searchByWordFuzzy(String word, int limit) {
        List<VocabComparisonItem> items = itemRepository.findByWordContainingAndStatus(
                word, StatusEnum.ENABLED.getCode());
        List<VocabComparisonGroupDto> groups = searchPublishedGroups(items);
        if (groups.size() > limit) {
            return groups.subList(0, limit);
        }
        return groups;
    }

    // ==================== Private Helper Methods ====================

    /**
     * Map entity all fields to DTO (base fields only, no sub-lists).
     */
    private VocabComparisonGroupDto toBaseDto(VocabComparisonGroup entity) {
        VocabComparisonGroupDto dto = new VocabComparisonGroupDto();
        dto.setId(entity.getId());
        dto.setGroupKey(entity.getGroupKey());
        dto.setExerciseQuestionIds(entity.getExerciseQuestionIds());
        dto.setGroupOrder(entity.getGroupOrder());
        dto.setStatus(entity.getStatus());
        dto.setPublishStatus(entity.getPublishStatus());
        dto.setEditStatus(entity.getEditStatus());
        dto.setDraftContent(entity.getDraftContent());
        dto.setCreateBy(entity.getCreateBy());
        dto.setUpdateBy(entity.getUpdateBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    /**
     * Base DTO + draft overlay when entity is in draft or reviewed status.
     */
    private VocabComparisonGroupDto toDtoWithDraftOverlay(VocabComparisonGroup entity) {
        VocabComparisonGroupDto dto = toBaseDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    /**
     * Overlay list-relevant fields (groupKey, groupOrder, exerciseQuestionIds) from draft JSON.
     */
    private void applyDraftOverlay(VocabComparisonGroupDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        VocabComparisonGroupDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, VocabComparisonGroupDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        if (draft.getGroupKey() != null) {
            dto.setGroupKey(draft.getGroupKey());
        }
        if (draft.getGroupOrder() != null) {
            dto.setGroupOrder(draft.getGroupOrder());
        }
        if (draft.getExerciseQuestionIds() != null) {
            dto.setExerciseQuestionIds(draft.getExerciseQuestionIds());
        }

        // 从草稿计算列表统计字段：条目数量
        if (draft.getItems() != null) {
            dto.setItemCount(draft.getItems().size());
        }
    }

    /**
     * 批量填充条目数量。
     * 草稿/已审核实体的 itemCount 已在 {@link #applyDraftOverlay} 中从草稿数据计算，跳过 DB 查询。
     */
    private void populateItemCounts(List<VocabComparisonGroupDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        List<Long> groupIds = dtos.stream()
                .filter(d -> !(EditStatusEnum.DRAFT.getCode().equals(d.getEditStatus())
                        || EditStatusEnum.REVIEWED.getCode().equals(d.getEditStatus())))
                .map(VocabComparisonGroupDto::getId)
                .collect(Collectors.toList());

        if (groupIds.isEmpty()) {
            // 全部都是草稿/已审核实体，无需 DB 查询
            return;
        }

        List<VocabComparisonItem> allItems = itemRepository.findByGroupIdInAndStatus(
                groupIds, StatusEnum.ENABLED.getCode());

        Map<Long, Long> countMap = allItems.stream()
                .collect(Collectors.groupingBy(VocabComparisonItem::getGroupId, Collectors.counting()));

        for (VocabComparisonGroupDto dto : dtos) {
            if (EditStatusEnum.DRAFT.getCode().equals(dto.getEditStatus())
                    || EditStatusEnum.REVIEWED.getCode().equals(dto.getEditStatus())) {
                // 草稿实体已在 applyDraftOverlay 中设置 itemCount
                // 如果草稿没有 items 数据（null），则从 DB 兜底
                if (dto.getItemCount() == null) {
                    dto.setItemCount(countMap.getOrDefault(dto.getId(), 0L).intValue());
                }
                continue;
            }
            dto.setItemCount(countMap.getOrDefault(dto.getId(), 0L).intValue());
        }
    }

    /**
     * Convert VocabComparisonItem entity to DTO with translation parsing.
     */
    private VocabComparisonItemDto toItemDto(VocabComparisonItem entity) {
        if (entity == null) {
            return null;
        }
        VocabComparisonItemDto dto = new VocabComparisonItemDto();
        dto.setId(entity.getId());
        dto.setWordId(entity.getWordId());
        dto.setWord(entity.getWord());
        dto.setPartOfSpeech(entity.getPartOfSpeech());
        dto.setUsageComparison(entity.getUsageComparison());
        dto.setUsageComparisonTranslations(JsonUtils.parseTranslationList(entity.getUsageComparisonTranslations()));
        dto.setCommonUsage(entity.getCommonUsage());
        dto.setCommonUsageTranslations(JsonUtils.parseTranslationList(entity.getCommonUsageTranslations()));
        dto.setOrder(entity.getItemOrder());
        return dto;
    }

    /**
     * Convert VocabComparisonChat entity to DTO (basic fields only).
     * Example-sentence derived fields (pinyin, translations, audioId) are filled by {@link #loadChats}.
     */
    private VocabComparisonChatDto toChatDto(VocabComparisonChat entity) {
        if (entity == null) {
            return null;
        }
        VocabComparisonChatDto dto = new VocabComparisonChatDto();
        dto.setId(entity.getId());
        dto.setRole(entity.getRole());
        dto.setContent(entity.getContent());
        dto.setOrder(entity.getChatOrder());
        dto.setExampleSentenceId(entity.getExampleSentenceId());
        return dto;
    }

    /**
     * Load items from the formal table for a given group.
     */
    private List<VocabComparisonItemDto> loadItems(Long groupId) {
        List<VocabComparisonItem> items = itemRepository.findByGroupIdAndStatus(
                groupId, StatusEnum.ENABLED.getCode());
        return items.stream().map(this::toItemDto).collect(Collectors.toList());
    }

    /**
     * Load chats from the formal table for a given group,
     * then batch-load example_sentences by exampleSentenceId FK.
     */
    private List<VocabComparisonChatDto> loadChats(Long groupId) {
        List<VocabComparisonChat> chats = chatRepository.findByGroupIdAndStatus(
                groupId, StatusEnum.ENABLED.getCode());
        if (chats == null || chats.isEmpty()) {
            return Collections.emptyList();
        }

        List<VocabComparisonChatDto> dtos = chats.stream().map(this::toChatDto).collect(Collectors.toList());

        // 通过 chat.exampleSentenceId 直接批量加载例句
        List<Long> sentenceIds = dtos.stream()
                .map(VocabComparisonChatDto::getExampleSentenceId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (!sentenceIds.isEmpty()) {
            Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(sentenceIds);
            for (VocabComparisonChatDto dto : dtos) {
                if (dto.getExampleSentenceId() != null) {
                    ExampleSentenceDto sentence = sentenceMap.get(dto.getExampleSentenceId());
                    if (sentence != null) {
                        dto.setPinyin(sentence.getPinyin());
                        dto.setTranslations(sentence.getTranslations());
                        dto.setAudioId(sentence.getAudioId());
                    }
                }
            }
        }

        return dtos;
    }

    /**
     * Sync items during publish: soft delete old items, create new items from draft.
     * @return the list of saved items (with IDs assigned by the database)
     */
    private List<VocabComparisonItem> syncItems(Long groupId, List<VocabComparisonItemDto> submittedDtos) {
        // Soft delete old items
        List<VocabComparisonItem> existing = itemRepository.findByGroupIdAndStatus(
                groupId, StatusEnum.ENABLED.getCode());
        if (!existing.isEmpty()) {
            for (VocabComparisonItem item : existing) {
                item.setStatus(StatusEnum.DISABLED.getCode());
            }
            itemRepository.saveAll(existing);
        }

        // Create new items
        if (submittedDtos == null || submittedDtos.isEmpty()) {
            return Collections.emptyList();
        }

        List<VocabComparisonItem> toSave = new ArrayList<>();
        for (VocabComparisonItemDto dto : submittedDtos) {
            VocabComparisonItem item = new VocabComparisonItem();
            item.setGroupId(groupId);
            item.setWordId(dto.getWordId() != null ? dto.getWordId() : 0L);
            item.setWord(dto.getWord());
            item.setPartOfSpeech(dto.getPartOfSpeech());
            item.setUsageComparison(dto.getUsageComparison());
            item.setUsageComparisonTranslations(JsonUtils.toTranslationJson(dto.getUsageComparisonTranslations()));
            item.setCommonUsage(dto.getCommonUsage());
            item.setCommonUsageTranslations(JsonUtils.toTranslationJson(dto.getCommonUsageTranslations()));
            item.setItemOrder(dto.getOrder() != null ? dto.getOrder() : 0);
            item.setStatus(StatusEnum.ENABLED.getCode());
            toSave.add(item);
        }
        if (!toSave.isEmpty()) {
            itemRepository.saveAll(toSave);
        }

        // Set DTO IDs from saved entities for AI marker collection
        for (int i = 0; i < submittedDtos.size(); i++) {
            submittedDtos.get(i).setId(toSave.get(i).getId());
        }

        return toSave;
    }

    /**
     * Sync chats during publish:
     * soft delete old chats + disable their example_sentences
     * → create new chats
     * → create example_sentences (without bizType)
     * → backfill chat.exampleSentenceId.
     * @return the list of saved chats (with IDs assigned by the database)
     */
    private List<VocabComparisonChat> syncChats(Long groupId, List<VocabComparisonChatDto> submittedDtos) {
        // 1. 软删除旧的 chats 及其例句
        List<VocabComparisonChat> existing = chatRepository.findByGroupIdAndStatus(
                groupId, StatusEnum.ENABLED.getCode());
        List<Long> oldSentenceIds = existing.stream()
                .map(VocabComparisonChat::getExampleSentenceId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (!existing.isEmpty()) {
            for (VocabComparisonChat chat : existing) {
                chat.setStatus(StatusEnum.DISABLED.getCode());
            }
            chatRepository.saveAll(existing);
        }
        if (!oldSentenceIds.isEmpty()) {
            exampleSentenceService.disableByIds(oldSentenceIds);
        }

        // 2. 创建新 chats
        if (submittedDtos == null || submittedDtos.isEmpty()) {
            return Collections.emptyList();
        }

        List<VocabComparisonChat> savedChats = new ArrayList<>();
        for (VocabComparisonChatDto dto : submittedDtos) {
            VocabComparisonChat chat = new VocabComparisonChat();
            chat.setGroupId(groupId);
            chat.setRole(dto.getRole());
            chat.setContent(dto.getContent());
            chat.setChatOrder(dto.getOrder() != null ? dto.getOrder() : 0);
            chat.setStatus(StatusEnum.ENABLED.getCode());
            chat = chatRepository.save(chat);
            dto.setId(chat.getId());

            // 创建例句（不设 bizType）
            ExampleSentenceDto sentenceDto = new ExampleSentenceDto();
            sentenceDto.setSentence(dto.getContent());
            sentenceDto.setPinyin(dto.getPinyin());
            sentenceDto.setAudioId(dto.getAudioId());
            sentenceDto.setTranslations(dto.getTranslations());
            sentenceDto.setOrder(dto.getOrder());

            ExampleSentenceDto savedSentence = exampleSentenceService.save(sentenceDto);
            if (savedSentence != null && savedSentence.getId() != null) {
                chat.setExampleSentenceId(savedSentence.getId());
                chatRepository.save(chat);
            }
            savedChats.add(chat);
        }
        return savedChats;
    }

    /**
     * 从草稿 DTO 和已保存的实体中收集 AI 内容标记。
     * DTO 列表与已保存实体列表按索引一一对应（按提交顺序创建）。
     */
    private void collectComparisonMarkers(
            List<VocabComparisonItemDto> itemDtos, List<VocabComparisonItem> savedItems,
            List<VocabComparisonChatDto> chatDtos, List<VocabComparisonChat> savedChats,
            List<AiContentMarkerService.MarkerEntry> entries) {
        // 收集条目标记
        if (itemDtos != null && savedItems != null) {
            int size = Math.min(itemDtos.size(), savedItems.size());
            for (int i = 0; i < size; i++) {
                VocabComparisonItemDto dto = itemDtos.get(i);
                if (dto.getAiGeneratedFields() != null && !dto.getAiGeneratedFields().isEmpty()) {
                    entries.add(new AiContentMarkerService.MarkerEntry(
                            "vocab_comparison_item", savedItems.get(i).getId(),
                            dto.getAiGeneratedFields()));
                }
            }
        }
        // 收集对话标记
        if (chatDtos != null && savedChats != null) {
            int size = Math.min(chatDtos.size(), savedChats.size());
            for (int i = 0; i < size; i++) {
                VocabComparisonChatDto dto = chatDtos.get(i);
                if (dto.getAiGeneratedFields() != null && !dto.getAiGeneratedFields().isEmpty()) {
                    entries.add(new AiContentMarkerService.MarkerEntry(
                            "vocab_comparison_chat", savedChats.get(i).getId(),
                            dto.getAiGeneratedFields()));
                }
            }
        }
    }

    /**
     * Given a list of VocabComparisonItem entities, load their groups and
     * return only published groups with items loaded.
     */
    private List<VocabComparisonGroupDto> searchPublishedGroups(List<VocabComparisonItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> groupIds = items.stream()
                .map(VocabComparisonItem::getGroupId)
                .distinct()
                .collect(Collectors.toList());

        List<VocabComparisonGroup> groups = groupRepository.findAllById(groupIds);

        List<VocabComparisonGroupDto> result = new ArrayList<>();
        for (VocabComparisonGroup group : groups) {
            if (!PublishStatusEnum.PUBLISHED.getCode().equals(group.getPublishStatus())) {
                continue;
            }
            if (StatusEnum.DISABLED.getCode().equals(group.getStatus())) {
                continue;
            }
            VocabComparisonGroupDto dto = toBaseDto(group);
            dto.setItems(loadItems(group.getId()));
            dto.setChats(loadChats(group.getId()));
            result.add(dto);
        }
        return result;
    }
}
