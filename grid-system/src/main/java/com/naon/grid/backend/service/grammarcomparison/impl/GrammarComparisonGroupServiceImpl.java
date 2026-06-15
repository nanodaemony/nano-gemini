package com.naon.grid.backend.service.grammarcomparison.impl;

import com.naon.grid.backend.domain.grammarcomparison.GrammarComparisonChat;
import com.naon.grid.backend.domain.grammarcomparison.GrammarComparisonGroup;
import com.naon.grid.backend.domain.grammarcomparison.GrammarComparisonItem;
import com.naon.grid.backend.repo.grammarcomparison.GrammarComparisonChatRepository;
import com.naon.grid.backend.repo.grammarcomparison.GrammarComparisonGroupRepository;
import com.naon.grid.backend.repo.grammarcomparison.GrammarComparisonItemRepository;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonChatDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupQueryCriteria;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonItemDto;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
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
public class GrammarComparisonGroupServiceImpl implements GrammarComparisonGroupService {

    private final GrammarComparisonGroupRepository groupRepository;
    private final GrammarComparisonItemRepository itemRepository;
    private final GrammarComparisonChatRepository chatRepository;
    private final ExampleSentenceService exampleSentenceService;

    @Override
    public PageResult<GrammarComparisonGroupDto> queryAll(GrammarComparisonGroupQueryCriteria criteria, Pageable pageable) {
        // 如果提供了 grammarId 或 grammarName，先查询子表获取匹配的 groupId
        List<Long> groupIds = null;
        if (criteria.getGrammarName() != null && !criteria.getGrammarName().trim().isEmpty()) {
            List<GrammarComparisonItem> items = itemRepository.findByGrammarNameAndStatus(
                    criteria.getGrammarName(), StatusEnum.ENABLED.getCode());
            groupIds = items.stream().map(GrammarComparisonItem::getGroupId).distinct().collect(Collectors.toList());
            if (groupIds.isEmpty()) {
                return PageUtil.toPage(Collections.emptyList(), 0L);
            }
        } else if (criteria.getGrammarId() != null) {
            List<GrammarComparisonItem> items = itemRepository.findByGrammarIdAndStatus(
                    criteria.getGrammarId(), StatusEnum.ENABLED.getCode());
            groupIds = items.stream().map(GrammarComparisonItem::getGroupId).distinct().collect(Collectors.toList());
            if (groupIds.isEmpty()) {
                return PageUtil.toPage(Collections.emptyList(), 0L);
            }
        }

        final List<Long> finalGroupIds = groupIds;
        final String finalPublishStatus = criteria.getPublishStatus();
        final String finalEditStatus = criteria.getEditStatus();
        final String finalGroupKey = criteria.getGroupKey();

        Page<GrammarComparisonGroup> page = groupRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
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
            if (finalGroupKey != null && !finalGroupKey.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("groupKey"), "%" + finalGroupKey + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        PageResult<GrammarComparisonGroupDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        populateItemCounts(pageResult.getContent());
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrammarComparisonGroupDto findById(Long id) {
        GrammarComparisonGroup entity = groupRepository.findById(id).orElse(null);
        if (entity == null || StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(GrammarComparisonGroup.class, "id", String.valueOf(id));
        }

        // 草稿或已审核：从 draftContent JSON 反序列化，覆盖系统字段
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            if (entity.getDraftContent() == null) {
                throw new BadRequestException("草稿内容不存在");
            }
            GrammarComparisonGroupDto dto;
            try {
                dto = JsonUtils.fromJson(entity.getDraftContent(), GrammarComparisonGroupDto.class);
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

        // 已发布：从正式表加载
        GrammarComparisonGroupDto dto = toBaseDto(entity);
        dto.setItems(loadItems(id));
        dto.setChats(loadChats(id));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(GrammarComparisonGroupDto resources) {
        GrammarComparisonGroup entity = new GrammarComparisonGroup();
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
    public void update(Long id, GrammarComparisonGroupDto resources) {
        GrammarComparisonGroup entity = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(GrammarComparisonGroup.class, "id", String.valueOf(id));
        }

        // 如果是已审核或已发布状态，回退到草稿
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
        GrammarComparisonGroup entity = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarComparisonGroup.class, "id", String.valueOf(id)));
        entity.setStatus(StatusEnum.DISABLED.getCode());
        groupRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        GrammarComparisonGroup entity = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(GrammarComparisonGroup.class, "id", String.valueOf(id));
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
        GrammarComparisonGroup entity = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(GrammarComparisonGroup.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅已审核状态可发布");
        }

        // 解析草稿内容 JSON
        GrammarComparisonGroupDto draftDto;
        try {
            draftDto = JsonUtils.fromJson(entity.getDraftContent(), GrammarComparisonGroupDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draftDto == null) {
            throw new BadRequestException("草稿数据解析失败");
        }

        // 更新主表字段
        entity.setGroupKey(draftDto.getGroupKey());
        entity.setGroupOrder(draftDto.getGroupOrder() != null ? draftDto.getGroupOrder() : 0);
        entity.setExerciseQuestionIds(draftDto.getExerciseQuestionIds());

        // 同步子表
        syncItems(id, draftDto.getItems());
        syncChats(id, draftDto.getChats());

        // 更新状态并清除草稿
        entity.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        entity.setDraftContent(null);
        groupRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        GrammarComparisonGroup entity = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(GrammarComparisonGroup.class, "id", String.valueOf(id));
        }
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        groupRepository.save(entity);
    }

    @Override
    public List<GrammarComparisonGroupDto> searchByGrammarName(String grammarName) {
        List<GrammarComparisonItem> items = itemRepository.findByGrammarNameAndStatus(
                grammarName, StatusEnum.ENABLED.getCode());
        return searchPublishedGroups(items);
    }

    @Override
    public List<GrammarComparisonGroupDto> searchByGrammarId(Long grammarId) {
        List<GrammarComparisonItem> items = itemRepository.findByGrammarIdAndStatus(
                grammarId, StatusEnum.ENABLED.getCode());
        return searchPublishedGroups(items);
    }

    // ==================== Private Helper Methods ====================

    /**
     * 映射实体所有字段到 DTO（基础字段，不含子列表）。
     */
    private GrammarComparisonGroupDto toBaseDto(GrammarComparisonGroup entity) {
        GrammarComparisonGroupDto dto = new GrammarComparisonGroupDto();
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
     * 基础 DTO + 草稿覆盖（当实体为草稿或已审核状态时）。
     */
    private GrammarComparisonGroupDto toDtoWithDraftOverlay(GrammarComparisonGroup entity) {
        GrammarComparisonGroupDto dto = toBaseDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    /**
     * 从草稿 JSON 覆盖列表相关字段（groupKey, groupOrder, exerciseQuestionIds）。
     */
    private void applyDraftOverlay(GrammarComparisonGroupDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        GrammarComparisonGroupDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, GrammarComparisonGroupDto.class);
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
    private void populateItemCounts(List<GrammarComparisonGroupDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }

        // 收集需要 DB 查询的已发布实体 ID
        List<Long> groupIds = dtos.stream()
                .filter(d -> !(EditStatusEnum.DRAFT.getCode().equals(d.getEditStatus())
                        || EditStatusEnum.REVIEWED.getCode().equals(d.getEditStatus())))
                .map(GrammarComparisonGroupDto::getId)
                .collect(Collectors.toList());

        if (groupIds.isEmpty()) {
            return; // 全部都是草稿/已审核实体，无需 DB 查询
        }

        List<GrammarComparisonItem> allItems = itemRepository.findByGroupIdInAndStatus(
                groupIds, StatusEnum.ENABLED.getCode());

        Map<Long, Long> countMap = allItems.stream()
                .collect(Collectors.groupingBy(GrammarComparisonItem::getGroupId, Collectors.counting()));

        for (GrammarComparisonGroupDto dto : dtos) {
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
     * 将 GrammarComparisonItem 实体转换为 DTO（含翻译解析）。
     */
    private GrammarComparisonItemDto toItemDto(GrammarComparisonItem entity) {
        if (entity == null) {
            return null;
        }
        GrammarComparisonItemDto dto = new GrammarComparisonItemDto();
        dto.setId(entity.getId());
        dto.setGrammarId(entity.getGrammarId());
        dto.setGrammarName(entity.getGrammarName());
        dto.setUsageComparison(entity.getUsageComparison());
        dto.setUsageComparisonTranslations(JsonUtils.parseTranslationList(entity.getUsageComparisonTranslations()));
        dto.setExampleSentences(entity.getExampleSentences());
        dto.setUsageSentenceId(entity.getUsageSentenceId());
        dto.setOrder(entity.getItemOrder());
        return dto;
    }

    /**
     * 将 GrammarComparisonChat 实体转换为 DTO（基本字段）。
     * 例句派生字段（pinyin, translations, audioId）由 {@link #loadChats} 填充。
     */
    private GrammarComparisonChatDto toChatDto(GrammarComparisonChat entity) {
        if (entity == null) {
            return null;
        }
        GrammarComparisonChatDto dto = new GrammarComparisonChatDto();
        dto.setId(entity.getId());
        dto.setRole(entity.getRole());
        dto.setContent(entity.getContent());
        dto.setOrder(entity.getChatOrder());
        dto.setExampleSentenceId(entity.getExampleSentenceId());
        return dto;
    }

    /**
     * 从正式表加载指定组的条目。
     */
    private List<GrammarComparisonItemDto> loadItems(Long groupId) {
        List<GrammarComparisonItem> items = itemRepository.findByGroupIdAndStatus(
                groupId, StatusEnum.ENABLED.getCode());
        return items.stream().map(this::toItemDto).collect(Collectors.toList());
    }

    /**
     * 从正式表加载指定组的对话，然后通过 exampleSentenceId FK 批量加载 example_sentence。
     */
    private List<GrammarComparisonChatDto> loadChats(Long groupId) {
        List<GrammarComparisonChat> chats = chatRepository.findByGroupIdAndStatus(
                groupId, StatusEnum.ENABLED.getCode());
        if (chats == null || chats.isEmpty()) {
            return Collections.emptyList();
        }

        List<GrammarComparisonChatDto> dtos = chats.stream().map(this::toChatDto).collect(Collectors.toList());

        // 通过 chat.exampleSentenceId 批量加载例句
        List<Long> sentenceIds = dtos.stream()
                .map(GrammarComparisonChatDto::getExampleSentenceId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (!sentenceIds.isEmpty()) {
            Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(sentenceIds);
            for (GrammarComparisonChatDto dto : dtos) {
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
     * 发布时同步条目：软删除旧条目，从草稿创建新条目。
     */
    private void syncItems(Long groupId, List<GrammarComparisonItemDto> submittedDtos) {
        // 软删除旧条目
        List<GrammarComparisonItem> existing = itemRepository.findByGroupIdAndStatus(
                groupId, StatusEnum.ENABLED.getCode());
        if (!existing.isEmpty()) {
            for (GrammarComparisonItem item : existing) {
                item.setStatus(StatusEnum.DISABLED.getCode());
            }
            itemRepository.saveAll(existing);
        }

        if (submittedDtos == null || submittedDtos.isEmpty()) {
            return;
        }

        List<GrammarComparisonItem> toSave = new ArrayList<>();
        for (GrammarComparisonItemDto dto : submittedDtos) {
            GrammarComparisonItem item = new GrammarComparisonItem();
            item.setGroupId(groupId);
            item.setGrammarId(dto.getGrammarId() != null ? dto.getGrammarId() : 0L);
            item.setGrammarName(dto.getGrammarName());
            item.setUsageComparison(dto.getUsageComparison());
            item.setUsageComparisonTranslations(JsonUtils.toTranslationJson(dto.getUsageComparisonTranslations()));
            item.setExampleSentences(dto.getExampleSentences());
            // usageSentenceId 在 items 中不会通过发布流程自动生成，保留为空或使用已有值
            item.setUsageSentenceId(dto.getUsageSentenceId());
            item.setItemOrder(dto.getOrder() != null ? dto.getOrder() : 0);
            item.setStatus(StatusEnum.ENABLED.getCode());
            toSave.add(item);
        }
        if (!toSave.isEmpty()) {
            itemRepository.saveAll(toSave);
        }
    }

    /**
     * 发布时同步对话：
     * 软删除旧对话 + 禁用其例句 → 创建新对话 → 创建 example_sentence → 回填 exampleSentenceId。
     */
    private void syncChats(Long groupId, List<GrammarComparisonChatDto> submittedDtos) {
        // 1. 软删除旧的 chats 及其例句
        List<GrammarComparisonChat> existing = chatRepository.findByGroupIdAndStatus(
                groupId, StatusEnum.ENABLED.getCode());
        List<Long> oldSentenceIds = existing.stream()
                .map(GrammarComparisonChat::getExampleSentenceId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (!existing.isEmpty()) {
            for (GrammarComparisonChat chat : existing) {
                chat.setStatus(StatusEnum.DISABLED.getCode());
            }
            chatRepository.saveAll(existing);
        }
        if (!oldSentenceIds.isEmpty()) {
            exampleSentenceService.disableByIds(oldSentenceIds);
        }

        // 2. 创建新 chats
        if (submittedDtos == null || submittedDtos.isEmpty()) {
            return;
        }

        for (GrammarComparisonChatDto dto : submittedDtos) {
            GrammarComparisonChat chat = new GrammarComparisonChat();
            chat.setGroupId(groupId);
            chat.setRole(dto.getRole());
            chat.setContent(dto.getContent());
            chat.setChatOrder(dto.getOrder() != null ? dto.getOrder() : 0);
            chat.setStatus(StatusEnum.ENABLED.getCode());
            chat = chatRepository.save(chat);

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
        }
    }

    /**
     * 给定 GrammarComparisonItem 实体列表，加载其所属组，
     * 只返回已发布的组（含子列表）。
     */
    private List<GrammarComparisonGroupDto> searchPublishedGroups(List<GrammarComparisonItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> groupIds = items.stream()
                .map(GrammarComparisonItem::getGroupId)
                .distinct()
                .collect(Collectors.toList());

        List<GrammarComparisonGroup> groups = groupRepository.findAllById(groupIds);

        List<GrammarComparisonGroupDto> result = new ArrayList<>();
        for (GrammarComparisonGroup group : groups) {
            if (!PublishStatusEnum.PUBLISHED.getCode().equals(group.getPublishStatus())) {
                continue;
            }
            if (StatusEnum.DISABLED.getCode().equals(group.getStatus())) {
                continue;
            }
            GrammarComparisonGroupDto dto = toBaseDto(group);
            dto.setItems(loadItems(group.getId()));
            dto.setChats(loadChats(group.getId()));
            result.add(dto);
        }
        return result;
    }
}
