# Exercise Question Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the exercise question (exercise_question) admin management module with draft/review/publish workflow, parent-child question structure, and CRUD APIs.

**Architecture:** Follows the existing CharCharacter management pattern — DO in `domain/question/`, Repository in `repo/question/`, Service with DTO in `service/question/`, Controller/Wrapper/Request/VO in `rest/`. Draft workflow stores full state in `draft_content` JSON, publishes by writing parent fields to main row and syncing children rows in the same table.

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MapStruct, Fastjson2, Lombok

## Global Constraints

- Use Long for exercise_question ID type (bigint in DB)
- JSON fields in DB stored as String, converted to structured types in DTO via JsonUtils
- Parent-child: parent_id=0 for parent questions, children are rows in same table with parent_id=parent.id
- Max 2 levels: children must not have their own children
- Soft delete via status=0, no physical delete
- All mappings in static Wrapper class (Request→Dto, Dto→VO), not in Controller
- Follow existing conventions: @Log, @AnonymousPostMapping etc., @Api/@ApiOperation, @RequiredArgsConstructor
- ExerciseQuestion extends BaseEntity for audit fields
- No MapStruct for ExerciseQuestionDto/Entity — manual conversion via Wrapper + JsonUtils

---
### Task 1: DO (ExerciseQuestion) + Repository

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/question/ExerciseQuestion.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/question/ExerciseQuestionRepository.java`

**Interfaces:**
- Produces: `ExerciseQuestion` entity, `ExerciseQuestionRepository` with basic CRUD + findByParentIdAndStatus + countByParentIdAndStatus

- [ ] **Step 1: Create ExerciseQuestion DO**

`grid-system/src/main/java/com/naon/grid/backend/domain/question/ExerciseQuestion.java`:

```java
package com.naon.grid.backend.domain.question;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "exercise_question")
public class ExerciseQuestion extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column(name = "question_type", length = 32, nullable = false)
    private String questionType;

    @Column(name = "stem", length = 512)
    private String stem;

    @Column(name = "content", length = 4096)
    private String content;

    @Column(name = "options", length = 2048)
    private String options;

    @Column(name = "answer", length = 512)
    private String answer;

    @Column(name = "explanation", length = 1024)
    private String explanation;

    @Column(name = "audio_id")
    private Long audioId;

    @Column(name = "sort")
    private Integer sort = 0;

    @Column(name = "draft_content", columnDefinition = "text")
    private String draftContent;

    @Column(name = "edit_status", length = 20)
    private String editStatus = EditStatusEnum.DRAFT.getCode();

    @Column(name = "publish_status", length = 20)
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "status")
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 2: Create Repository**

`grid-system/src/main/java/com/naon/grid/backend/repo/question/ExerciseQuestionRepository.java`:

```java
package com.naon.grid.backend.repo.question;

import com.naon.grid.backend.domain.question.ExerciseQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseQuestionRepository extends JpaRepository<ExerciseQuestion, Long>, JpaSpecificationExecutor<ExerciseQuestion> {

    List<ExerciseQuestion> findByParentIdAndStatus(Long parentId, Integer status);

    long countByParentIdAndStatus(Long parentId, Integer status);
}
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/question/ExerciseQuestion.java
git add grid-system/src/main/java/com/naon/grid/backend/repo/question/ExerciseQuestionRepository.java
git commit -m "feat: add ExerciseQuestion DO and Repository"
```

---
### Task 2: DTO + QueryCriteria

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/question/dto/ExerciseQuestionDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/question/dto/ExerciseQuestionQueryCriteria.java`

**Interfaces:**
- Produces: `ExerciseQuestionDto` (core data carrier with children), `ExerciseQuestionQueryCriteria` (query filter, consumed by Service)

- [ ] **Step 1: Create ExerciseQuestionDto**

`grid-system/src/main/java/com/naon/grid/backend/service/question/dto/ExerciseQuestionDto.java`:

```java
package com.naon.grid.backend.service.question.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.QuestionContent;
import com.naon.grid.domain.common.QuestionOption;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExerciseQuestionDto extends BaseDTO {

    private Long id;
    private Long parentId;
    private String questionType;
    private String stem;
    private QuestionContent content;
    private List<QuestionOption> options;
    private List<String> answer;
    private String explanation;
    private Long audioId;
    private Integer sort;

    private String editStatus;
    private String publishStatus;
    private Integer status;

    // 子题列表（详情时需要）
    private List<ExerciseQuestionDto> children;

    // 列表统计
    private Integer childCount;
}
```

- [ ] **Step 2: Create QueryCriteria**

`grid-system/src/main/java/com/naon/grid/backend/service/question/dto/ExerciseQuestionQueryCriteria.java`:

```java
package com.naon.grid.backend.service.question.dto;

import com.naon.grid.annotation.Query;
import lombok.Data;

import java.io.Serializable;

@Data
public class ExerciseQuestionQueryCriteria implements Serializable {

    @Query(blurry = "stem")
    private String blurry;

    @Query
    private String questionType;

    @Query
    private String publishStatus;

    @Query
    private String editStatus;
}
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/question/dto/ExerciseQuestionDto.java
git add grid-system/src/main/java/com/naon/grid/backend/service/question/dto/ExerciseQuestionQueryCriteria.java
git commit -m "feat: add ExerciseQuestionDto and QueryCriteria"
```

---
### Task 3: Service Interface

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/question/ExerciseQuestionService.java`

**Interfaces:**
- Consumes: `ExerciseQuestionDto`, `ExerciseQuestionQueryCriteria`
- Produces: Service interface consumable by Controller
- Produces: `PageResult<ExerciseQuestionDto>`, `ExerciseQuestionDto`, `Long`

- [ ] **Step 1: Create Service interface**

`grid-system/src/main/java/com/naon/grid/backend/service/question/ExerciseQuestionService.java`:

```java
package com.naon.grid.backend.service.question;

import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface ExerciseQuestionService {

    PageResult<ExerciseQuestionDto> queryAll(ExerciseQuestionQueryCriteria criteria, Pageable pageable);

    ExerciseQuestionDto findById(Long id);

    Long create(ExerciseQuestionDto dto);

    void update(Long id, ExerciseQuestionDto dto);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/question/ExerciseQuestionService.java
git commit -m "feat: add ExerciseQuestionService interface"
```

---
### Task 4: Service Implementation (Core Logic)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/question/impl/ExerciseQuestionServiceImpl.java`

**Interfaces:**
- Consumes: `ExerciseQuestionRepository`, `ExerciseQuestionDto`, `ExerciseQuestionQueryCriteria`
- Produces: All service methods implemented

- [ ] **Step 1: Create implementation class**

`grid-system/src/main/java/com/naon/grid/backend/service/question/impl/ExerciseQuestionServiceImpl.java`:

```java
package com.naon.grid.backend.service.question.impl;

import com.naon.grid.backend.domain.question.ExerciseQuestion;
import com.naon.grid.backend.repo.question.ExerciseQuestionRepository;
import com.naon.grid.backend.service.question.ExerciseQuestionService;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionQueryCriteria;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
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

    @Override
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

        // 填充 childCount
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
        if (draft.getSort() != null) dto.setSort(draft.getSort());

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

        // 已发布状态下不允许修改 questionType
        if (PublishStatusEnum.PUBLISHED.getCode().equals(entity.getPublishStatus())) {
            String newType = dto.getQuestionType();
            if (newType != null && !newType.equals(entity.getQuestionType())) {
                throw new BadRequestException("已发布的题目不允许修改题目类型");
            }
        }

        // REVERSED or PUBLISHED → 回退到 DRAFT
        if (EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.PUBLISHED.getCode().equals(entity.getEditStatus())) {
            entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        entity.setDraftContent(JsonUtils.toJson(dto));
        exerciseQuestionRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ExerciseQuestion entity = exerciseQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id)));
        if (entity.getId() == null) {
            throw new EntityNotFoundException(ExerciseQuestion.class, "id", String.valueOf(id));
        }
        entity.setStatus(StatusEnum.DISABLED.getCode());
        exerciseQuestionRepository.save(entity);

        // 同时软删除子题
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

        // 回写父题业务字段
        entity.setQuestionType(draftDto.getQuestionType());
        entity.setStem(draftDto.getStem());
        entity.setContent(JsonUtils.toJson(draftDto.getContent()));
        entity.setOptions(JsonUtils.toExerciseOptionListJson(draftDto.getOptions()));
        entity.setAnswer(JsonUtils.toStringListJson(draftDto.getAnswer()));
        entity.setExplanation(draftDto.getExplanation());
        entity.setAudioId(draftDto.getAudioId());
        entity.setSort(draftDto.getSort() != null ? draftDto.getSort() : 0);

        // 同步子题
        syncChildren(id, draftDto.getChildren());

        // 更新状态
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
            childEntity.setQuestionType(dto.getQuestionType());
            childEntity.setStem(dto.getStem());
            childEntity.setContent(JsonUtils.toJson(dto.getContent()));
            childEntity.setOptions(JsonUtils.toExerciseOptionListJson(dto.getOptions()));
            childEntity.setAnswer(JsonUtils.toStringListJson(dto.getAnswer()));
            childEntity.setExplanation(dto.getExplanation());
            childEntity.setAudioId(dto.getAudioId());
            childEntity.setSort(dto.getSort() != null ? dto.getSort() : 0);
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

    // 需要新增的 Repository 方法：findByParentIdInAndStatus
    // 将添加到 ExerciseQuestionRepository 中

    private ExerciseQuestionDto toBaseDto(ExerciseQuestion entity) {
        ExerciseQuestionDto dto = new ExerciseQuestionDto();
        dto.setId(entity.getId());
        dto.setParentId(entity.getParentId());
        dto.setQuestionType(entity.getQuestionType());
        dto.setStem(entity.getStem());
        dto.setContent(JsonUtils.fromJson(entity.getContent(), QuestionContent.class)); // need import
        dto.setOptions(JsonUtils.parseExerciseOptionList(entity.getOptions()));
        dto.setAnswer(JsonUtils.parseStringList(entity.getAnswer()));
        dto.setExplanation(entity.getExplanation());
        dto.setAudioId(entity.getAudioId());
        dto.setSort(entity.getSort());
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
```

Note: `toBaseDto` needs these imports added:
```java
import com.naon.grid.domain.common.QuestionContent;
```

- [ ] **Step 2: Add `findByParentIdInAndStatus` to Repository**

Edit `ExerciseQuestionRepository.java` to add:
```java
List<ExerciseQuestion> findByParentIdInAndStatus(List<Long> parentIds, Integer status);
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/question/impl/ExerciseQuestionServiceImpl.java
git add grid-system/src/main/java/com/naon/grid/backend/repo/question/ExerciseQuestionRepository.java
git commit -m "feat: add ExerciseQuestionService implementation"
```

---
### Task 5: Request + VO Classes

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionCreateRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionQueryRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionBaseVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionCreateVO.java`

**Interfaces:**
- Consumes: None (pure data classes)
- Produces: Request classes (consumed by Controller), VO classes (consumed by Wrapper)

- [ ] **Step 1: Create CreateRequest**

`grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionCreateRequest.java`:

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class ExerciseQuestionCreateRequest implements Serializable {

    @ApiModelProperty(value = "题目类型，参考枚举：QuestionTypeEnum", required = true)
    private String questionType;

    @ApiModelProperty(value = "题干")
    private String stem;

    @ApiModelProperty(value = "题目内容材料")
    private QuestionContentRequest content;

    @ApiModelProperty(value = "选项列表")
    @Valid
    private List<QuestionOptionRequest> options;

    @ApiModelProperty(value = "答案列表")
    private List<String> answer;

    @ApiModelProperty(value = "解析")
    private String explanation;

    @ApiModelProperty(value = "听力音频ID")
    private Long audioId;

    @ApiModelProperty(value = "排序号（值越大越靠前）")
    private Integer sort;

    @Valid
    @ApiModelProperty(value = "子题列表")
    private List<ExerciseQuestionCreateRequest> children;

    @Getter
    @Setter
    public static class QuestionContentRequest implements Serializable {
        @ApiModelProperty(value = "内容文案")
        private String contentText;

        @ApiModelProperty(value = "内容图片ID")
        private String contentImageId;
    }

    @Getter
    @Setter
    public static class QuestionOptionRequest implements Serializable {
        @ApiModelProperty(value = "选项标识，如 A、B、C、D")
        private String option;

        @ApiModelProperty(value = "选项文案")
        private String optionText;

        @ApiModelProperty(value = "选项图片ID")
        private String optionImageId;
    }
}
```

- [ ] **Step 2: Create QueryRequest**

`grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionQueryRequest.java`:

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ExerciseQuestionQueryRequest implements Serializable {

    @ApiModelProperty(value = "题干模糊搜索")
    private String blurry;

    @ApiModelProperty(value = "题目类型")
    private String questionType;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;
}
```

- [ ] **Step 3: Create VO (detail)**

`grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionVO.java`:

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class ExerciseQuestionVO implements Serializable {

    @ApiModelProperty(value = "题目ID")
    private Long id;

    @ApiModelProperty(value = "题目类型，参考枚举：QuestionTypeEnum")
    private String questionType;

    @ApiModelProperty(value = "题干")
    private String stem;

    @ApiModelProperty(value = "题目内容材料")
    private QuestionContentVO content;

    @ApiModelProperty(value = "选项列表")
    private List<QuestionOptionVO> options;

    @ApiModelProperty(value = "答案列表")
    private List<String> answer;

    @ApiModelProperty(value = "解析")
    private String explanation;

    @ApiModelProperty(value = "听力音频ID")
    private Long audioId;

    @ApiModelProperty(value = "排序号（值越大越靠前）")
    private Integer sort;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @ApiModelProperty(value = "子题列表")
    private List<ExerciseQuestionVO> children;

    @Getter
    @Setter
    public static class QuestionContentVO implements Serializable {
        @ApiModelProperty(value = "内容文案")
        private String contentText;

        @ApiModelProperty(value = "内容图片ID")
        private String contentImageId;
    }

    @Getter
    @Setter
    public static class QuestionOptionVO implements Serializable {
        @ApiModelProperty(value = "选项标识，如 A、B、C、D")
        private String option;

        @ApiModelProperty(value = "选项文案")
        private String optionText;

        @ApiModelProperty(value = "选项图片ID")
        private String optionImageId;
    }
}
```

- [ ] **Step 4: Create BaseVO (list)**

`grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionBaseVO.java`:

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class ExerciseQuestionBaseVO implements Serializable {

    @ApiModelProperty(value = "题目ID")
    private Long id;

    @ApiModelProperty(value = "题目类型，参考枚举：QuestionTypeEnum")
    private String questionType;

    @ApiModelProperty(value = "题干")
    private String stem;

    @ApiModelProperty(value = "听力音频ID")
    private Long audioId;

    @ApiModelProperty(value = "排序号（值越大越靠前）")
    private Integer sort;

    @ApiModelProperty(value = "子题数量")
    private Integer childCount;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
```

- [ ] **Step 5: Create CreateVO**

`grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionCreateVO.java`:

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ExerciseQuestionCreateVO implements Serializable {

    @ApiModelProperty(value = "新建题目ID")
    private Long id;
}
```

- [ ] **Step 6: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionCreateRequest.java
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionQueryRequest.java
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionVO.java
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionBaseVO.java
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionCreateVO.java
git commit -m "feat: add ExerciseQuestion request and VO classes"
```

---
### Task 6: Wrapper

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/ExerciseQuestionWrapper.java`

**Interfaces:**
- Consumes: `ExerciseQuestionCreateRequest`, `ExerciseQuestionQueryRequest`, `ExerciseQuestionDto`, `ExerciseQuestionVO`, `ExerciseQuestionBaseVO`
- Produces: Static mapping methods consumable by Controller

- [ ] **Step 1: Create Wrapper**

`grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/ExerciseQuestionWrapper.java`:

```java
package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.ExerciseQuestionCreateRequest;
import com.naon.grid.backend.rest.request.ExerciseQuestionQueryRequest;
import com.naon.grid.backend.rest.vo.ExerciseQuestionBaseVO;
import com.naon.grid.backend.rest.vo.ExerciseQuestionVO;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionQueryCriteria;
import com.naon.grid.domain.common.QuestionContent;
import com.naon.grid.domain.common.QuestionOption;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExerciseQuestionWrapper {

    public static ExerciseQuestionQueryCriteria toCriteria(ExerciseQuestionQueryRequest request) {
        if (request == null) return null;
        ExerciseQuestionQueryCriteria criteria = new ExerciseQuestionQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setQuestionType(request.getQuestionType());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    public static ExerciseQuestionDto toDto(ExerciseQuestionCreateRequest request) {
        if (request == null) return null;
        ExerciseQuestionDto dto = new ExerciseQuestionDto();
        dto.setQuestionType(request.getQuestionType());
        dto.setStem(request.getStem());
        dto.setContent(toQuestionContent(request.getContent()));
        dto.setOptions(toQuestionOptionList(request.getOptions()));
        dto.setAnswer(request.getAnswer());
        dto.setExplanation(request.getExplanation());
        dto.setAudioId(request.getAudioId());
        dto.setSort(request.getSort());
        dto.setChildren(toDtoList(request.getChildren()));
        return dto;
    }

    private static List<ExerciseQuestionDto> toDtoList(List<ExerciseQuestionCreateRequest> children) {
        if (children == null) return Collections.emptyList();
        return children.stream().map(ExerciseQuestionWrapper::toDto).collect(Collectors.toList());
    }

    private static QuestionContent toQuestionContent(ExerciseQuestionCreateRequest.QuestionContentRequest req) {
        if (req == null) return null;
        QuestionContent content = new QuestionContent();
        content.setContentText(req.getContentText());
        content.setContentImageId(req.getContentImageId());
        return content;
    }

    private static List<QuestionOption> toQuestionOptionList(List<ExerciseQuestionCreateRequest.QuestionOptionRequest> reqs) {
        if (reqs == null) return Collections.emptyList();
        return reqs.stream().map(ExerciseQuestionWrapper::toQuestionOption).collect(Collectors.toList());
    }

    private static QuestionOption toQuestionOption(ExerciseQuestionCreateRequest.QuestionOptionRequest req) {
        if (req == null) return null;
        QuestionOption option = new QuestionOption();
        option.setOption(req.getOption());
        option.setOptionText(req.getOptionText());
        option.setOptionImageId(req.getOptionImageId());
        return option;
    }

    public static List<ExerciseQuestionBaseVO> toBaseVOList(List<ExerciseQuestionDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(ExerciseQuestionWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static ExerciseQuestionBaseVO toBaseVO(ExerciseQuestionDto dto) {
        if (dto == null) return null;
        ExerciseQuestionBaseVO vo = new ExerciseQuestionBaseVO();
        vo.setId(dto.getId());
        vo.setQuestionType(dto.getQuestionType());
        vo.setStem(dto.getStem());
        vo.setAudioId(dto.getAudioId());
        vo.setSort(dto.getSort());
        vo.setChildCount(dto.getChildCount());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static ExerciseQuestionVO toVO(ExerciseQuestionDto dto) {
        if (dto == null) return null;
        ExerciseQuestionVO vo = new ExerciseQuestionVO();
        vo.setId(dto.getId());
        vo.setQuestionType(dto.getQuestionType());
        vo.setStem(dto.getStem());
        vo.setContent(toQuestionContentVO(dto.getContent()));
        vo.setOptions(toQuestionOptionVOList(dto.getOptions()));
        vo.setAnswer(dto.getAnswer());
        vo.setExplanation(dto.getExplanation());
        vo.setAudioId(dto.getAudioId());
        vo.setSort(dto.getSort());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setChildren(toVOList(dto.getChildren()));
        return vo;
    }

    private static List<ExerciseQuestionVO> toVOList(List<ExerciseQuestionDto> children) {
        if (children == null) return Collections.emptyList();
        return children.stream().map(ExerciseQuestionWrapper::toVO).collect(Collectors.toList());
    }

    private static ExerciseQuestionVO.QuestionContentVO toQuestionContentVO(QuestionContent content) {
        if (content == null) return null;
        ExerciseQuestionVO.QuestionContentVO vo = new ExerciseQuestionVO.QuestionContentVO();
        vo.setContentText(content.getContentText());
        vo.setContentImageId(content.getContentImageId());
        return vo;
    }

    private static List<ExerciseQuestionVO.QuestionOptionVO> toQuestionOptionVOList(List<QuestionOption> options) {
        if (options == null) return Collections.emptyList();
        return options.stream().map(ExerciseQuestionWrapper::toQuestionOptionVO).collect(Collectors.toList());
    }

    private static ExerciseQuestionVO.QuestionOptionVO toQuestionOptionVO(QuestionOption option) {
        if (option == null) return null;
        ExerciseQuestionVO.QuestionOptionVO vo = new ExerciseQuestionVO.QuestionOptionVO();
        vo.setOption(option.getOption());
        vo.setOptionText(option.getOptionText());
        vo.setOptionImageId(option.getOptionImageId());
        return vo;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/ExerciseQuestionWrapper.java
git commit -m "feat: add ExerciseQuestionWrapper"
```

---
### Task 7: Controller

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/ExerciseQuestionController.java`

**Interfaces:**
- Consumes: `ExerciseQuestionService`, `ExerciseQuestionWrapper`, Request/VO classes
- Produces: REST API endpoints

- [ ] **Step 1: Create Controller**

`grid-system/src/main/java/com/naon/grid/backend/rest/controller/ExerciseQuestionController.java`:

```java
package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.ExerciseQuestionCreateRequest;
import com.naon.grid.backend.rest.request.ExerciseQuestionQueryRequest;
import com.naon.grid.backend.rest.vo.ExerciseQuestionBaseVO;
import com.naon.grid.backend.rest.vo.ExerciseQuestionCreateVO;
import com.naon.grid.backend.rest.vo.ExerciseQuestionVO;
import com.naon.grid.backend.rest.wrapper.ExerciseQuestionWrapper;
import com.naon.grid.backend.service.question.ExerciseQuestionService;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：练习题目管理")
@RequestMapping("/api/exercise-question")
public class ExerciseQuestionController {

    private final ExerciseQuestionService exerciseQuestionService;

    @Log("新增题目")
    @ApiOperation("新增题目")
    @AnonymousPostMapping
    public ResponseEntity<ExerciseQuestionCreateVO> create(@Valid @RequestBody ExerciseQuestionCreateRequest request) {
        ExerciseQuestionCreateVO vo = new ExerciseQuestionCreateVO();
        vo.setId(exerciseQuestionService.create(ExerciseQuestionWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("修改题目内容")
    @ApiOperation("修改题目内容")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody ExerciseQuestionCreateRequest request) {
        exerciseQuestionService.update(id, ExerciseQuestionWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("题目草稿审核通过")
    @ApiOperation("题目草稿审核通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        exerciseQuestionService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布题目")
    @ApiOperation("发布题目（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        exerciseQuestionService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询题目详情")
    @ApiOperation("根据ID查询题目详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<ExerciseQuestionVO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(ExerciseQuestionWrapper.toVO(exerciseQuestionService.findById(id)), HttpStatus.OK);
    }

    @Log("查询题目列表")
    @ApiOperation("分页查询题目列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<ExerciseQuestionBaseVO>> queryAll(ExerciseQuestionQueryRequest request, Pageable pageable) {
        PageResult<ExerciseQuestionDto> pageResult = exerciseQuestionService.queryAll(
                ExerciseQuestionWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(ExerciseQuestionWrapper.toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()),
                HttpStatus.OK);
    }

    @Log("删除题目")
    @ApiOperation("删除题目")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        exerciseQuestionService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线题目")
    @ApiOperation("下线题目")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        exerciseQuestionService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/ExerciseQuestionController.java
git commit -m "feat: add ExerciseQuestionController with full CRUD and draft workflow"
```

---
### Task 8: Final Verification

**Files:** None (verification only)

**Interfaces:** N/A

- [ ] **Step 1: Verify build compiles**

```bash
cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am -DskipTests -q
```

Expected: BUILD SUCCESS (no errors)

- [ ] **Step 2: Verify no missing imports or references**

Check that all `ExerciseQuestionWrapper.toDto` / `Wrapper.toCriteria` etc. calls match the method signatures defined in the Wrapper.

Check that PageResult constructor with + (list, totalElements)(list, totalElements) matches existing usage elsewhere:
```java
new PageResult<>(list, totalElements)
```
This is how `CharCharacterController` and `VocabWordController` use it.

- [ ] **Step 3: Commit any final fixes**

```bash
git add -A
git commit -m "fix: compilation fixes for exercise question module"
```

- [ ] **Step 4: Done — verify all files exist**

```bash
find grid-system/src/main/java/com/naon/grid/backend -name "*ExerciseQuestion*" -o -name "*ExerciseQuestion*Repository*" | sort
```

Expected output shows all 10 new files.
