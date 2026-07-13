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
import com.naon.grid.domain.common.TextTranslation;
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

    // queryAll, findById, findPublishedById, publishDraft, searchPublished
    // will be added in Task 6

    @Override
    public PageResult<TopicDto> queryAll(TopicQueryCriteria criteria, Pageable pageable) {
        throw new UnsupportedOperationException("queryAll not yet implemented — will be added in Task 6");
    }

    @Override
    public TopicDto findById(Long id) {
        throw new UnsupportedOperationException("findById not yet implemented — will be added in Task 6");
    }

    @Override
    public TopicDto findPublishedById(Long id) {
        throw new UnsupportedOperationException("findPublishedById not yet implemented — will be added in Task 6");
    }

    @Override
    public void publishDraft(Long id) {
        throw new UnsupportedOperationException("publishDraft not yet implemented — will be added in Task 6");
    }

    @Override
    public List<TopicDto> searchPublished(String blurry) {
        throw new UnsupportedOperationException("searchPublished not yet implemented — will be added in Task 6");
    }
}
