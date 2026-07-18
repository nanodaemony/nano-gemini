package com.naon.grid.backend.service.question.impl;

import com.naon.grid.backend.domain.question.ExerciseQuestion;
import com.naon.grid.backend.repo.question.ExerciseQuestionRepository;
import com.naon.grid.backend.service.question.ExerciseQuestionService;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionQueryCriteria;
import com.naon.grid.domain.common.QuestionContent;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.modules.system.service.AiContentMarkerService;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseQuestionServiceImpl implements ExerciseQuestionService {

    private final ExerciseQuestionRepository exerciseQuestionRepository;

    private final AiContentMarkerService aiContentMarkerService;

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PageResult<ExerciseQuestionDto> queryAll(ExerciseQuestionQueryCriteria criteria, Pageable pageable) {
        Page<ExerciseQuestion> page = exerciseQuestionRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            Predicate parentPredicate = criteriaBuilder.equal(root.get("parentId"), 0L);
            return criteriaBuilder.and(basePredicate, statusPredicate, parentPredicate);
        }, pageable);
        List<ExerciseQuestionDto> dtos = page.getContent().stream()
                .map(this::toDtoWithDraftOverlay)
                .collect(Collectors.toList());

        // fill childCount
        fillChildCount(dtos);

        return PageUtil.toPage(dtos, page.getTotalElements());
    }

    private void fillChildCount(List<ExerciseQuestionDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<Long> parentIds = dtos.stream().map(ExerciseQuestionDto::getId).collect(Collectors.toList());

        // Group children count from DB
        List<ExerciseQuestion> allChildren = exerciseQuestionRepository.findByParentIdInAndStatus(parentIds, StatusEnum.ENABLED.getCode());
        Map<Long, Long> childCountMap = allChildren.stream()
                .collect(Collectors.groupingBy(ExerciseQuestion::getParentId, Collectors.counting()));

        for (ExerciseQuestionDto dto : dtos) {
            if (dto.getChildCount() == null) {
                dto.setChildCount(childCountMap.getOrDefault(dto.getId(), 0L).intValue());
            }
        }
    }

    private ExerciseQuestionDto toDtoWithDraftOverlay(ExerciseQuestion entity) {
        ExerciseQuestionDto dto = toBaseDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    private void applyDraftOverlay(ExerciseQuestionDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        ExerciseQuestionDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, ExerciseQuestionDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        if (draft.getQuestionType() != null) dto.setQuestionType(draft.getQuestionType());
        if (draft.getStem() != null) dto.setStem(draft.getStem());
        if (draft.getContent() != null) dto.setContent(draft.getContent());
        if (draft.getOptions() != null) dto.setOptions(draft.getOptions());
        if (draft.getAnswer() != null) dto.setAnswer(draft.getAnswer());
        if (draft.getExplanation() != null) dto.setExplanation(draft.getExplanation());
        if (draft.getAudioId() != null) dto.setAudioId(draft.getAudioId());
        if (draft.getAudioText() != null) dto.setAudioText(draft.getAudioText());
        if (draft.getSort() != null) dto.setSort(draft.getSort());
        if (draft.getTags() != null) dto.setTags(draft.getTags());

        // childCount from draft children size
        if (draft.getChildren() != null) {
            dto.setChildCount(draft.getChildren().size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExerciseQuestionDto findById(Long id) {
        if (id == null) {
            throw new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id));
        }
        ExerciseQuestion entity = exerciseQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            return buildDetailFromDraft(entity);
        }

        return buildPublishedDetail(entity);
    }

    private ExerciseQuestionDto buildDetailFromDraft(ExerciseQuestion entity) {
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        ExerciseQuestionDto dto = JsonUtils.fromJson(entity.getDraftContent(), ExerciseQuestionDto.class);
        if (dto == null) {
            throw new BadRequestException("草稿内容解析失败");
        }
        dto.setId(entity.getId());
        dto.setParentId(entity.getParentId());
        dto.setStatus(entity.getStatus());
        dto.setPublishStatus(entity.getPublishStatus());
        dto.setEditStatus(entity.getEditStatus());
        dto.setCreateBy(entity.getCreateBy());
        dto.setUpdateBy(entity.getUpdateBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    private ExerciseQuestionDto buildPublishedDetail(ExerciseQuestion entity) {
        ExerciseQuestionDto dto = toBaseDto(entity);
        dto.setChildren(findChildren(entity.getId()));
        return dto;
    }

    private List<ExerciseQuestionDto> findChildren(Long parentId) {
        List<ExerciseQuestion> children = exerciseQuestionRepository
                .findByParentIdAndStatus(parentId, StatusEnum.ENABLED.getCode());
        return children.stream().map(this::toChildDto).collect(Collectors.toList());
    }

    private ExerciseQuestionDto toChildDto(ExerciseQuestion entity) {
        ExerciseQuestionDto dto = toBaseDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ExerciseQuestionDto dto) {
        ExerciseQuestion entity = new ExerciseQuestion();
        entity.setStatus(StatusEnum.ENABLED.getCode());
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        entity.setQuestionType(dto.getQuestionType());
        entity.setStem(dto.getStem());
        entity.setDraftContent(JsonUtils.toJson(dto));
        entity = exerciseQuestionRepository.save(entity);

        // AI 标记：练习题无草稿流程，直接保存标记
        aiContentMarkerService.replaceFields("exercise_question",
                entity.getId(), dto.getAiGeneratedFields());
        // 递归处理子题
        if (dto.getChildren() != null) {
            for (ExerciseQuestionDto child : dto.getChildren()) {
                if (child.getId() != null) {
                    aiContentMarkerService.replaceFields("exercise_question",
                            child.getId(), child.getAiGeneratedFields());
                }
            }
        }

        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ExerciseQuestionDto dto) {
        ExerciseQuestion entity = exerciseQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id));
        }

        // published state cannot modify questionType
        if (PublishStatusEnum.PUBLISHED.getCode().equals(entity.getPublishStatus())) {
            String newType = dto.getQuestionType();
            if (newType != null && !newType.equals(entity.getQuestionType())) {
                throw new BadRequestException("已发布的题目不允许修改题目类型");
            }
        }

        // REVIEWED or PUBLISHED -> rollback to DRAFT
        if (EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.PUBLISHED.getCode().equals(entity.getEditStatus())) {
            entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        entity.setDraftContent(JsonUtils.toJson(dto));
        exerciseQuestionRepository.save(entity);

        // AI 标记：练习题无草稿流程，直接保存标记
        aiContentMarkerService.replaceFields("exercise_question",
                id, dto.getAiGeneratedFields());
        // 递归处理子题
        if (dto.getChildren() != null) {
            for (ExerciseQuestionDto child : dto.getChildren()) {
                if (child.getId() != null) {
                    aiContentMarkerService.replaceFields("exercise_question",
                            child.getId(), child.getAiGeneratedFields());
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ExerciseQuestion entity = exerciseQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id));
        }
        entity.setStatus(StatusEnum.DISABLED.getCode());
        exerciseQuestionRepository.save(entity);

        // also soft delete children
        List<ExerciseQuestion> children = exerciseQuestionRepository
                .findByParentIdAndStatus(id, StatusEnum.ENABLED.getCode());
        for (ExerciseQuestion child : children) {
            child.setStatus(StatusEnum.DISABLED.getCode());
            exerciseQuestionRepository.save(child);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        ExerciseQuestion entity = exerciseQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }
        entity.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        exerciseQuestionRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id) {
        ExerciseQuestion entity = exerciseQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        if (!EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅已审核的草稿可发布");
        }

        ExerciseQuestionDto draftDto = JsonUtils.fromJson(entity.getDraftContent(), ExerciseQuestionDto.class);

        // Write back parent business fields
        if (draftDto.getQuestionType() == null) {
            throw new BadRequestException("题目类型不能为空");
        }
        entity.setQuestionType(draftDto.getQuestionType());
        entity.setStem(draftDto.getStem());
        entity.setContent(JsonUtils.toJson(draftDto.getContent()));
        entity.setOptions(JsonUtils.toExerciseOptionListJson(draftDto.getOptions()));
        entity.setAnswer(JsonUtils.toStringListJson(draftDto.getAnswer()));
        entity.setExplanation(draftDto.getExplanation());
        entity.setAudioId(draftDto.getAudioId());
        entity.setAudioText(draftDto.getAudioText());
        entity.setSort(draftDto.getSort());
        // tags: List<String> → comma-separated DB string
        entity.setTags(draftDto.getTags() == null || draftDto.getTags().isEmpty() ? null
                : String.join(",", draftDto.getTags()));

        // Sync children
        syncChildren(id, draftDto.getChildren());

        // Update status
        entity.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        entity.setDraftContent(null);
        exerciseQuestionRepository.save(entity);
    }

    private void syncChildren(Long parentId, List<ExerciseQuestionDto> submittedChildren) {
        List<ExerciseQuestionDto> submitted = submittedChildren == null
                ? Collections.emptyList() : submittedChildren;

        List<ExerciseQuestion> existing = exerciseQuestionRepository
                .findByParentIdAndStatus(parentId, StatusEnum.ENABLED.getCode());

        Map<Long, ExerciseQuestion> existingMap = new HashMap<>();
        for (ExerciseQuestion child : existing) {
            existingMap.put(child.getId(), child);
        }

        Set<Long> submittedIds = new HashSet<>();
        List<ExerciseQuestion> toSave = new ArrayList<>();

        for (ExerciseQuestionDto dto : submitted) {
            // Validate max 2 levels: children must not have their own children
            if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
                throw new BadRequestException("子题不能包含自身的子题");
            }
            ExerciseQuestion childEntity;
            if (dto.getId() != null && dto.getId() > 0) {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("子题ID重复: " + dto.getId());
                }
                childEntity = existingMap.get(dto.getId());
                if (childEntity == null) {
                    throw new BadRequestException("子题ID不属于当前题目: " + dto.getId());
                }
            } else {
                childEntity = new ExerciseQuestion();
                childEntity.setParentId(parentId);
                childEntity.setStatus(StatusEnum.ENABLED.getCode());
                childEntity.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
                childEntity.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
            }
            // Copy fields
            if (dto.getQuestionType() == null) {
                throw new BadRequestException("子题题目类型不能为空");
            }
            childEntity.setQuestionType(dto.getQuestionType());
            childEntity.setStem(dto.getStem());
            childEntity.setContent(JsonUtils.toJson(dto.getContent()));
            childEntity.setOptions(JsonUtils.toExerciseOptionListJson(dto.getOptions()));
            childEntity.setAnswer(JsonUtils.toStringListJson(dto.getAnswer()));
            childEntity.setExplanation(dto.getExplanation());
            childEntity.setAudioId(dto.getAudioId());
            childEntity.setAudioText(dto.getAudioText());
            childEntity.setSort(dto.getSort());
            childEntity.setTags(dto.getTags() == null || dto.getTags().isEmpty() ? null
                    : String.join(",", dto.getTags()));
            toSave.add(childEntity);
        }

        // Soft delete removed children
        for (ExerciseQuestion child : existing) {
            if (!submittedIds.contains(child.getId())) {
                child.setStatus(StatusEnum.DISABLED.getCode());
                toSave.add(child);
            }
        }

        exerciseQuestionRepository.saveAll(toSave);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ExerciseQuestionDto findPublishedById(Long id) {
        if (id == null) {
            throw new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id));
        }
        ExerciseQuestion entity = exerciseQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())
                || !PublishStatusEnum.PUBLISHED.getCode().equals(entity.getPublishStatus())) {
            throw new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id));
        }
        return buildPublishedDetail(entity);
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<ExerciseQuestionDto> findPublishedByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExerciseQuestion> entities = exerciseQuestionRepository.findAllById(ids);
        return entities.stream()
                .filter(e -> StatusEnum.ENABLED.getCode().equals(e.getStatus())
                        && PublishStatusEnum.PUBLISHED.getCode().equals(e.getPublishStatus()))
                .map(this::buildPublishedDetail)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        ExerciseQuestion entity = exerciseQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id));
        }
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        exerciseQuestionRepository.save(entity);
    }

    private ExerciseQuestionDto toBaseDto(ExerciseQuestion entity) {
        ExerciseQuestionDto dto = new ExerciseQuestionDto();
        dto.setId(entity.getId());
        dto.setParentId(entity.getParentId());
        dto.setQuestionType(entity.getQuestionType());
        dto.setStem(entity.getStem());
        dto.setContent(JsonUtils.fromJson(entity.getContent(), QuestionContent.class));
        dto.setOptions(JsonUtils.parseExerciseOptionList(entity.getOptions()));
        dto.setAnswer(JsonUtils.parseStringList(entity.getAnswer()));
        dto.setExplanation(entity.getExplanation());
        dto.setAudioId(entity.getAudioId());
        dto.setAudioText(entity.getAudioText());
        dto.setSort(entity.getSort());
        // tags: comma-separated DB string → List<String>
        dto.setTags(entity.getTags() == null ? null
                : java.util.Arrays.asList(entity.getTags().split(",")));
        dto.setEditStatus(entity.getEditStatus());
        dto.setPublishStatus(entity.getPublishStatus());
        dto.setStatus(entity.getStatus());
        dto.setCreateBy(entity.getCreateBy());
        dto.setUpdateBy(entity.getUpdateBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }
}
