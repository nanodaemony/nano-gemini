package com.naon.grid.backend.service.topic.impl;

import com.naon.grid.backend.domain.topic.Topic;
import com.naon.grid.backend.domain.topic.TopicChat;
import com.naon.grid.backend.domain.topic.TopicPattern;
import com.naon.grid.backend.repo.topic.TopicChatRepository;
import com.naon.grid.backend.repo.topic.TopicPatternRepository;
import com.naon.grid.backend.repo.topic.TopicRepository;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.topic.TopicService;
import com.naon.grid.backend.service.topic.dto.TopicChatDto;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicPatternDto;
import com.naon.grid.backend.service.topic.dto.TopicQueryCriteria;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final TopicPatternRepository patternRepository;
    private final TopicChatRepository chatRepository;
    private final ExampleSentenceService exampleSentenceService;
    private final AiContentMarkerService aiContentMarkerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TopicDto resources) {
        Topic entity = new Topic();
        entity.setStatus(StatusEnum.ENABLED.getCode());
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        entity.setName(resources.getName());
        entity.setDraftContent(JsonUtils.toJson(resources));
        entity = topicRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TopicDto resources) {
        Topic entity = topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Topic.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(Topic.class, "id", String.valueOf(id));
        }
        if (EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.PUBLISHED.getCode().equals(entity.getEditStatus())) {
            entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }
        entity.setDraftContent(JsonUtils.toJson(resources));
        topicRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Topic entity = topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Topic.class, "id", String.valueOf(id)));
        entity.setStatus(StatusEnum.DISABLED.getCode());
        topicRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        Topic entity = topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Topic.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(Topic.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }
        entity.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        topicRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        Topic entity = topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Topic.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(Topic.class, "id", String.valueOf(id));
        }
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        topicRepository.save(entity);
    }

    @Override
    public PageResult<TopicDto> queryAll(TopicQueryCriteria criteria, Pageable pageable) {
        final String finalPublishStatus = criteria.getPublishStatus();
        final String finalEditStatus = criteria.getEditStatus();

        Page<Topic> page = topicRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), StatusEnum.ENABLED.getCode()));
            if (finalPublishStatus != null && !finalPublishStatus.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("publishStatus"), finalPublishStatus));
            }
            if (finalEditStatus != null && !finalEditStatus.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("editStatus"), finalEditStatus));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        PageResult<TopicDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        populatePatternCounts(pageResult.getContent());
        return pageResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TopicDto findById(Long id) {
        Topic entity = topicRepository.findById(id).orElse(null);
        if (entity == null || StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(Topic.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            if (entity.getDraftContent() == null) {
                throw new BadRequestException("草稿内容不存在");
            }
            TopicDto dto;
            try {
                dto = JsonUtils.fromJson(entity.getDraftContent(), TopicDto.class);
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

        TopicDto dto = toBaseDto(entity);
        dto.setPatterns(loadPatterns(id));
        return dto;
    }

    @Override
    public TopicDto findPublishedById(Long id) {
        Topic entity = topicRepository.findById(id).orElse(null);
        if (entity == null || StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(Topic.class, "id", String.valueOf(id));
        }
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(entity.getPublishStatus())) {
            throw new EntityNotFoundException(Topic.class, "id", String.valueOf(id));
        }
        TopicDto dto = toBaseDto(entity);
        dto.setPatterns(loadPatterns(id));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id) {
        Topic entity = topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Topic.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(Topic.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅已审核状态可发布");
        }

        TopicDto draftDto;
        try {
            draftDto = JsonUtils.fromJson(entity.getDraftContent(), TopicDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draftDto == null) {
            throw new BadRequestException("草稿数据解析失败");
        }

        // Update main table fields from draft
        entity.setName(draftDto.getName());
        entity.setPinyin(draftDto.getPinyin());
        entity.setAudioId(draftDto.getAudioId());
        entity.setCoverImageId(draftDto.getCoverImageId());
        entity.setTranslations(JsonUtils.toTranslationJson(draftDto.getTranslations()));

        // Sync patterns and chats
        List<TopicPattern> savedPatterns = syncPatterns(id, draftDto.getPatterns());

        // Collect AI markers
        List<AiContentMarkerService.MarkerEntry> markerEntries = new ArrayList<>();
        collectTopicMarkers(draftDto.getPatterns(), savedPatterns, markerEntries);
        aiContentMarkerService.batchReplace(markerEntries);

        entity.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        entity.setDraftContent(null);
        topicRepository.save(entity);
    }

    @Override
    public List<TopicDto> searchPublished(String blurry) {
        List<Topic> topics = topicRepository.findByNameContainingAndStatusAndPublishStatus(
                blurry, StatusEnum.ENABLED.getCode(), PublishStatusEnum.PUBLISHED.getCode());
        return topics.stream().map(entity -> {
            TopicDto dto = toBaseDto(entity);
            dto.setPatterns(loadPatterns(entity.getId()));
            return dto;
        }).collect(Collectors.toList());
    }

    // ==================== Private Helpers ====================

    private TopicDto toBaseDto(Topic entity) {
        TopicDto dto = new TopicDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPinyin(entity.getPinyin());
        dto.setAudioId(entity.getAudioId());
        dto.setCoverImageId(entity.getCoverImageId());
        dto.setTranslations(JsonUtils.parseTranslationList(entity.getTranslations()));
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

    private TopicDto toDtoWithDraftOverlay(Topic entity) {
        TopicDto dto = toBaseDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    private void applyDraftOverlay(TopicDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        TopicDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, TopicDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        if (draft.getName() != null) {
            dto.setName(draft.getName());
        }
        if (draft.getPinyin() != null) {
            dto.setPinyin(draft.getPinyin());
        }
        if (draft.getAudioId() != null) {
            dto.setAudioId(draft.getAudioId());
        }
        if (draft.getCoverImageId() != null) {
            dto.setCoverImageId(draft.getCoverImageId());
        }
        if (draft.getTranslations() != null) {
            dto.setTranslations(draft.getTranslations());
        }
        if (draft.getPatterns() != null) {
            dto.setPatternCount(draft.getPatterns().size());
        }
    }

    private void populatePatternCounts(List<TopicDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<Long> topicIds = dtos.stream()
                .filter(d -> !(EditStatusEnum.DRAFT.getCode().equals(d.getEditStatus())
                        || EditStatusEnum.REVIEWED.getCode().equals(d.getEditStatus())))
                .map(TopicDto::getId)
                .collect(Collectors.toList());

        if (topicIds.isEmpty()) return;

        List<TopicPattern> allPatterns = patternRepository.findByTopicIdInAndStatus(
                topicIds, StatusEnum.ENABLED.getCode());
        Map<Long, Long> countMap = allPatterns.stream()
                .collect(Collectors.groupingBy(TopicPattern::getTopicId, Collectors.counting()));

        for (TopicDto dto : dtos) {
            if (EditStatusEnum.DRAFT.getCode().equals(dto.getEditStatus())
                    || EditStatusEnum.REVIEWED.getCode().equals(dto.getEditStatus())) {
                if (dto.getPatternCount() == null) {
                    dto.setPatternCount(countMap.getOrDefault(dto.getId(), 0L).intValue());
                }
                continue;
            }
            dto.setPatternCount(countMap.getOrDefault(dto.getId(), 0L).intValue());
        }
    }

    private List<TopicPatternDto> loadPatterns(Long topicId) {
        List<TopicPattern> patterns = patternRepository.findByTopicIdAndStatus(
                topicId, StatusEnum.ENABLED.getCode());
        if (patterns == null || patterns.isEmpty()) {
            return Collections.emptyList();
        }
        // Sort by order descending
        patterns.sort(Comparator.comparing(TopicPattern::getPatternOrder).reversed());

        // Collect all pattern IDs to batch-load chats
        List<Long> patternIds = patterns.stream().map(TopicPattern::getId).collect(Collectors.toList());
        List<TopicChat> allChats = chatRepository.findByPatternIdInAndStatus(patternIds, StatusEnum.ENABLED.getCode());
        Map<Long, List<TopicChat>> chatsByPatternId = allChats.stream()
                .collect(Collectors.groupingBy(TopicChat::getPatternId));

        // Batch-load example_sentences via chat FK
        List<Long> sentenceIds = allChats.stream()
                .map(TopicChat::getExampleSentenceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, ExampleSentenceDto> sentenceMap = Collections.emptyMap();
        if (!sentenceIds.isEmpty()) {
            sentenceMap = exampleSentenceService.findByIds(sentenceIds);
        }

        List<TopicPatternDto> result = new ArrayList<>();
        for (TopicPattern pattern : patterns) {
            TopicPatternDto patternDto = new TopicPatternDto();
            patternDto.setId(pattern.getId());
            patternDto.setPattern(pattern.getPattern());
            patternDto.setImageId(pattern.getImageId());
            patternDto.setOrder(pattern.getPatternOrder());

            List<TopicChat> patternChats = chatsByPatternId.getOrDefault(pattern.getId(), Collections.emptyList());
            patternChats.sort(Comparator.comparing(TopicChat::getChatOrder).reversed());

            List<TopicChatDto> chatDtos = new ArrayList<>();
            for (TopicChat chat : patternChats) {
                TopicChatDto chatDto = new TopicChatDto();
                chatDto.setId(chat.getId());
                chatDto.setRole(chat.getRole());
                chatDto.setContent(chat.getContent());
                chatDto.setOrder(chat.getChatOrder());
                chatDto.setExampleSentenceId(chat.getExampleSentenceId());
                if (chat.getExampleSentenceId() != null) {
                    ExampleSentenceDto sentence = sentenceMap.get(chat.getExampleSentenceId());
                    if (sentence != null) {
                        chatDto.setPinyin(sentence.getPinyin());
                        chatDto.setTranslations(sentence.getTranslations());
                        chatDto.setAudioId(sentence.getAudioId());
                    }
                }
                chatDtos.add(chatDto);
            }
            patternDto.setChats(chatDtos);
            result.add(patternDto);
        }
        return result;
    }

    private List<TopicPattern> syncPatterns(Long topicId, List<TopicPatternDto> submittedDtos) {
        // Soft-delete old patterns
        List<TopicPattern> existing = patternRepository.findByTopicIdAndStatus(topicId, StatusEnum.ENABLED.getCode());
        if (!existing.isEmpty()) {
            for (TopicPattern p : existing) {
                p.setStatus(StatusEnum.DISABLED.getCode());
            }
            patternRepository.saveAll(existing);

            // Soft-delete old chats for these patterns
            List<Long> oldPatternIds = existing.stream().map(TopicPattern::getId).collect(Collectors.toList());
            List<TopicChat> oldChats = chatRepository.findByPatternIdInAndStatus(oldPatternIds, StatusEnum.ENABLED.getCode());
            if (!oldChats.isEmpty()) {
                List<Long> oldSentenceIds = oldChats.stream()
                        .map(TopicChat::getExampleSentenceId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                for (TopicChat chat : oldChats) {
                    chat.setStatus(StatusEnum.DISABLED.getCode());
                }
                chatRepository.saveAll(oldChats);
                if (!oldSentenceIds.isEmpty()) {
                    exampleSentenceService.disableByIds(oldSentenceIds);
                }
            }
        }

        // Create new patterns and chats
        if (submittedDtos == null || submittedDtos.isEmpty()) {
            return Collections.emptyList();
        }

        List<TopicPattern> savedPatterns = new ArrayList<>();
        for (TopicPatternDto patternDto : submittedDtos) {
            TopicPattern pattern = new TopicPattern();
            pattern.setTopicId(topicId);
            pattern.setPattern(patternDto.getPattern());
            pattern.setImageId(patternDto.getImageId());
            pattern.setPatternOrder(patternDto.getOrder() != null ? patternDto.getOrder() : 0);
            pattern.setStatus(StatusEnum.ENABLED.getCode());
            pattern = patternRepository.save(pattern);
            patternDto.setId(pattern.getId());

            // Sync chats for this pattern
            syncChats(topicId, pattern.getId(), patternDto.getChats());
            savedPatterns.add(pattern);
        }
        return savedPatterns;
    }

    private void syncChats(Long topicId, Long patternId, List<TopicChatDto> submittedDtos) {
        if (submittedDtos == null || submittedDtos.isEmpty()) {
            return;
        }

        for (TopicChatDto dto : submittedDtos) {
            TopicChat chat = new TopicChat();
            chat.setTopicId(topicId);
            chat.setPatternId(patternId);
            chat.setRole(dto.getRole());
            chat.setContent(dto.getContent());
            chat.setChatOrder(dto.getOrder() != null ? dto.getOrder() : 0);
            chat.setStatus(StatusEnum.ENABLED.getCode());
            chat = chatRepository.save(chat);
            dto.setId(chat.getId());

            // Create example_sentence for the chat
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

    private void collectTopicMarkers(
            List<TopicPatternDto> patternDtos, List<TopicPattern> savedPatterns,
            List<AiContentMarkerService.MarkerEntry> entries) {
        if (patternDtos != null && savedPatterns != null) {
            int size = Math.min(patternDtos.size(), savedPatterns.size());
            for (int i = 0; i < size; i++) {
                TopicPatternDto patternDto = patternDtos.get(i);
                if (patternDto.getAiGeneratedFields() != null && !patternDto.getAiGeneratedFields().isEmpty()) {
                    entries.add(new AiContentMarkerService.MarkerEntry(
                            "topic_pattern", savedPatterns.get(i).getId(),
                            patternDto.getAiGeneratedFields()));
                }
                // Collect chat markers
                if (patternDto.getChats() != null) {
                    for (TopicChatDto chatDto : patternDto.getChats()) {
                        if (chatDto.getAiGeneratedFields() != null && !chatDto.getAiGeneratedFields().isEmpty()) {
                            entries.add(new AiContentMarkerService.MarkerEntry(
                                    "topic_chat", chatDto.getId(),
                                    chatDto.getAiGeneratedFields()));
                        }
                    }
                }
            }
        }
    }
}
