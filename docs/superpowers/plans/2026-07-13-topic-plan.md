# Topic Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete Topic (话题) feature with admin draft/review/publish workflow and APP-side query/search, integrated with the existing favorites system.

**Architecture:** Three-layer entity hierarchy (Topic → TopicPattern → TopicChat) following the existing VocabComparison draft-content-json pattern. Admin services and domain logic live in grid-system; APP controllers/VOs/wrappers live in grid-app. The publish flow serializes the entire DTO tree into Topic.draftContent as JSON, and materializes child tables (topic_pattern, topic_chat, example_sentence) only on publish.

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Fastjson2, Lombok, MySQL

## Global Constraints

- Java 8 compatibility (no var, no records, no text blocks)
- All admin table IDs use Long type
- Draft workflow: create/update writes draft_content JSON only; publish materializes to child tables
- "Latest data" query: if editStatus is draft/reviewed, return draft JSON content; else return published DB content
- Admin controllers use @Anonymous*Mapping annotations (session-based auth)
- APP controllers use @AnonymousGetMapping or @GetMapping with AppSecurityUtils
- All Request/VO classes are separate from Domain classes — no direct entity exposure
- APP-side translations filter by single language parameter, admin-side returns all translations
- APP-side resources resolve IDs to URLs, log ERROR on missing resources, never throw
- Wrapper classes are public static utility classes in their respective wrapper packages
- Follow existing naming conventions: snake_case DB columns, camelCase Java fields

---

### Task 1: Domain Entities (DO)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/topic/Topic.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/topic/TopicPattern.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/topic/TopicChat.java`

**Produces:**
- `Topic` entity with Long id, name, pinyin, audioId, coverImageId, translations (text JSON), draftContent (text), status, publishStatus, editStatus + BaseEntity inherited fields
- `TopicPattern` entity with Long id, topicId, pattern, imageId, order, status, createTime, updateTime
- `TopicChat` entity with Long id, topicId, patternId, role, content, exampleSentenceId, order, status, createTime, updateTime

- [ ] **Step 1: Create Topic.java**

```java
package com.naon.grid.backend.domain.topic;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "topic")
public class Topic extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "话题ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    @ApiModelProperty(value = "话题名称（如"希望"）")
    private String name;

    @Column(name = "pinyin", length = 256)
    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @Column(name = "cover_image_id")
    @ApiModelProperty(value = "封面图片资源ID")
    private Long coverImageId;

    @Column(name = "translations", columnDefinition = "text")
    @ApiModelProperty(value = "话题多语言翻译（JSON）")
    private String translations;

    @Column(name = "draft_content", columnDefinition = "text")
    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();

    @Column(name = "publish_status", length = 20)
    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "edit_status", length = 20)
    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus = EditStatusEnum.DRAFT.getCode();
}
```

- [ ] **Step 2: Create TopicPattern.java**

```java
package com.naon.grid.backend.domain.topic;

import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "topic_pattern")
public class TopicPattern implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "句式ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    @ApiModelProperty(value = "所属话题ID")
    private Long topicId;

    @Column(name = "pattern", nullable = false, length = 512)
    @ApiModelProperty(value = "句式文本（如"（某人）+希望……"）")
    private String pattern;

    @Column(name = "image_id")
    @ApiModelProperty(value = "句式示意图资源ID")
    private Long imageId;

    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "组内排序权重（大的在前）")
    private Integer patternOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "status")
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 3: Create TopicChat.java**

```java
package com.naon.grid.backend.domain.topic;

import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "topic_chat")
public class TopicChat implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "对话ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    @ApiModelProperty(value = "所属话题ID（冗余）")
    private Long topicId;

    @Column(name = "pattern_id", nullable = false)
    @ApiModelProperty(value = "所属句式ID")
    private Long patternId;

    @Column(name = "role", nullable = false, length = 20)
    @ApiModelProperty(value = "角色: teacher=老师, student=学生")
    private String role;

    @Column(name = "content", nullable = false, length = 1024)
    @ApiModelProperty(value = "中文对话内容")
    private String content;

    @Column(name = "example_sentence_id")
    @ApiModelProperty(value = "对话例句ID（对应example_sentence表，发布时回填）")
    private Long exampleSentenceId;

    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "组内排序权重（大的在前）")
    private Integer chatOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "status")
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/topic/
git commit -m "feat: add Topic, TopicPattern, TopicChat domain entities"
```

---

### Task 2: Repositories

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/topic/TopicRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/topic/TopicPatternRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/topic/TopicChatRepository.java`

**Interfaces:**
- Consumes: `Topic`, `TopicPattern`, `TopicChat` entities from Task 1
- Produces: `TopicRepository extends JpaRepository<Topic, Long> + JpaSpecificationExecutor<Topic>`, `TopicPatternRepository` with `findByTopicIdAndStatus`, `TopicChatRepository` with `findByPatternIdInAndStatus`

- [ ] **Step 1: Create TopicRepository.java**

```java
package com.naon.grid.backend.repo.topic;

import com.naon.grid.backend.domain.topic.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long>, JpaSpecificationExecutor<Topic> {

    List<Topic> findByNameContainingAndStatus(String name, Integer status);

    List<Topic> findByNameContainingAndStatusAndPublishStatus(String name, Integer status, String publishStatus);
}
```

- [ ] **Step 2: Create TopicPatternRepository.java**

```java
package com.naon.grid.backend.repo.topic;

import com.naon.grid.backend.domain.topic.TopicPattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicPatternRepository extends JpaRepository<TopicPattern, Long> {

    List<TopicPattern> findByTopicIdAndStatus(Long topicId, Integer status);

    List<TopicPattern> findByTopicIdInAndStatus(List<Long> topicIds, Integer status);
}
```

- [ ] **Step 3: Create TopicChatRepository.java**

```java
package com.naon.grid.backend.repo.topic;

import com.naon.grid.backend.domain.topic.TopicChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicChatRepository extends JpaRepository<TopicChat, Long> {

    List<TopicChat> findByPatternIdAndStatus(Long patternId, Integer status);

    List<TopicChat> findByPatternIdInAndStatus(List<Long> patternIds, Integer status);
}
```

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/topic/
git commit -m "feat: add Topic repositories"
```

---

### Task 3: DTOs

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/topic/dto/TopicDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/topic/dto/TopicPatternDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/topic/dto/TopicChatDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/topic/dto/TopicQueryCriteria.java`

**Produces:**
- `TopicDto` extends BaseDTO with all topic fields + `List<TopicPatternDto> patterns` + `Integer patternCount`
- `TopicPatternDto` with id, pattern, imageId, order, `List<TopicChatDto> chats`, aiGeneratedFields
- `TopicChatDto` matching `VocabComparisonChatDto` structure: id, role, content, pinyin, translations (List<TextTranslation>), audioId, exampleSentenceId, order, aiGeneratedFields
- `TopicQueryCriteria` with blurry, publishStatus, editStatus

- [ ] **Step 1: Create TopicChatDto.java**

```java
package com.naon.grid.backend.service.topic.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TopicChatDto {

    @ApiModelProperty(value = "对话ID（新增时为null）")
    private Long id;

    @ApiModelProperty(value = "角色: teacher=老师, student=学生")
    private String role;

    @ApiModelProperty(value = "中文对话内容")
    private String content;

    @ApiModelProperty(value = "对话例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "对话例句翻译")
    private List<TextTranslation> translations;

    @ApiModelProperty(value = "对话例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "关联的example_sentence_id")
    private Long exampleSentenceId;

    @ApiModelProperty(value = "组内排序权重")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;
}
```

- [ ] **Step 2: Create TopicPatternDto.java**

```java
package com.naon.grid.backend.service.topic.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TopicPatternDto {

    @ApiModelProperty(value = "句式ID（新增时为null）")
    private Long id;

    @ApiModelProperty(value = "句式文本")
    private String pattern;

    @ApiModelProperty(value = "句式示意图资源ID")
    private Long imageId;

    @ApiModelProperty(value = "排序权重")
    private Integer order;

    @ApiModelProperty(value = "情景对话列表")
    private List<TopicChatDto> chats;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;
}
```

- [ ] **Step 3: Create TopicDto.java**

```java
package com.naon.grid.backend.service.topic.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TopicDto extends BaseDTO {

    @ApiModelProperty(value = "话题ID")
    private Long id;

    @ApiModelProperty(value = "话题名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "封面图片资源ID")
    private Long coverImageId;

    @ApiModelProperty(value = "多语言翻译")
    private List<TextTranslation> translations;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    @ApiModelProperty(value = "句式列表")
    private List<TopicPatternDto> patterns;

    @ApiModelProperty(value = "句式数量")
    private Integer patternCount;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;
}
```

- [ ] **Step 4: Create TopicQueryCriteria.java**

```java
package com.naon.grid.backend.service.topic.dto;

import com.naon.grid.annotation.Query;
import lombok.Data;

@Data
public class TopicQueryCriteria {

    @Query(blurry = "name")
    private String blurry;

    @Query
    private String publishStatus;

    @Query
    private String editStatus;
}
```

- [ ] **Step 5: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/topic/dto/
git commit -m "feat: add Topic DTOs and query criteria"
```

---

### Task 4: TopicService Interface

**File:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/topic/TopicService.java`

**Produces:**
- `TopicService` interface with: queryAll, findById, findPublishedById, create, update, delete, reviewDraft, publishDraft, offline, searchPublished

- [ ] **Step 1: Create TopicService.java**

```java
package com.naon.grid.backend.service.topic;

import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TopicService {

    PageResult<TopicDto> queryAll(TopicQueryCriteria criteria, Pageable pageable);

    TopicDto findById(Long id);

    TopicDto findPublishedById(Long id);

    Long create(TopicDto resources);

    void update(Long id, TopicDto resources);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);

    List<TopicDto> searchPublished(String blurry);
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/topic/TopicService.java
git commit -m "feat: add TopicService interface"
```

---

### Task 5: TopicServiceImpl — CRUD + Status Transitions

**File:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/topic/impl/TopicServiceImpl.java`

**Interfaces:**
- Consumes: `TopicService` interface from Task 4, all entities and repos from Tasks 1-2, DTOs from Task 3
- Produces: `create`, `update`, `delete`, `reviewDraft`, `offline` implementations

- [ ] **Step 1: Create TopicServiceImpl skeleton with create/update/delete/reviewDraft/offline**

```java
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
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/topic/impl/TopicServiceImpl.java
git commit -m "feat: add TopicServiceImpl with create/update/delete/review/offline"
```

---

### Task 6: TopicServiceImpl — Query + Publish + Search

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/topic/impl/TopicServiceImpl.java`

**Interfaces:**
- Consumes: `TopicServiceImpl` from Task 5
- Produces: `queryAll`, `findById`, `findPublishedById`, `publishDraft`, `searchPublished` implementations

- [ ] **Step 1: Add queryAll method**

Insert after the `offline` method, before the closing `}`:

```java
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
            // blurry search on name handled by @Query annotation on criteria via QueryHelp
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        PageResult<TopicDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        populatePatternCounts(pageResult.getContent());
        return pageResult;
    }
```

- [ ] **Step 2: Add findById method**

```java
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
```

- [ ] **Step 3: Add findPublishedById method**

```java
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
```

- [ ] **Step 4: Add publishDraft method**

```java
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
```

- [ ] **Step 5: Add searchPublished method**

```java
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
```

- [ ] **Step 6: Add all private helper methods**

Insert all private helpers before the closing `}`:

```java
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
```

- [ ] **Step 3: Verify the file compiles**

Run: `mvn compile -pl grid-system -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/topic/impl/TopicServiceImpl.java
git commit -m "feat: add TopicServiceImpl query, publish, and search methods"
```

---

### Task 7: Admin Request, VO, and Wrapper Classes

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/TopicCreateRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/TopicQueryRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/TopicCreateVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/TopicBaseVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/TopicVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/TopicPatternVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/TopicChatVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/TopicWrapper.java`

**Interfaces:**
- Consumes: DTOs from Task 3
- Produces: All admin Request/VO/Wrapper types for TopicController

- [ ] **Step 1: Create TopicQueryRequest.java**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TopicQueryRequest {

    @ApiModelProperty(value = "模糊搜索（话题名称）")
    private String blurry;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;
}
```

- [ ] **Step 2: Create TopicCreateRequest.java**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class TopicCreateRequest {

    @NotBlank
    @ApiModelProperty(value = "话题名称", required = true)
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "封面图片资源ID")
    private Long coverImageId;

    @ApiModelProperty(value = "多语言翻译")
    private List<TextTranslationRequest> translations;

    @Valid
    @ApiModelProperty(value = "句式列表")
    private List<TopicPatternRequest> patterns;

    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;

    @Data
    public static class TopicPatternRequest {

        @NotBlank
        @ApiModelProperty(value = "句式文本", required = true)
        private String pattern;

        @ApiModelProperty(value = "句式示意图资源ID")
        private Long imageId;

        @ApiModelProperty(value = "排序权重")
        private Integer order;

        @Valid
        @ApiModelProperty(value = "情景对话列表")
        private List<TopicChatRequest> chats;

        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
    }

    @Data
    public static class TopicChatRequest {

        @NotBlank
        @ApiModelProperty(value = "角色: teacher=老师, student=学生", required = true)
        private String role;

        @NotBlank
        @ApiModelProperty(value = "中文对话内容", required = true)
        private String content;

        @ApiModelProperty(value = "对话例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "对话例句翻译")
        private List<TextTranslationRequest> translations;

        @ApiModelProperty(value = "对话例句音频资源ID")
        private Long audioId;

        @ApiModelProperty(value = "组内排序权重")
        private Integer order;

        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
    }
}
```

- [ ] **Step 3: Create TopicCreateVO.java**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TopicCreateVO {

    @ApiModelProperty(value = "话题ID")
    private Long id;
}
```

- [ ] **Step 4: Create TopicBaseVO.java**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class TopicBaseVO {

    @ApiModelProperty(value = "话题ID")
    private Long id;

    @ApiModelProperty(value = "话题名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "句式数量")
    private Integer patternCount;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

- [ ] **Step 5: Create TopicChatVO.java**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class TopicChatVO {

    @ApiModelProperty(value = "对话ID")
    private Long id;

    @ApiModelProperty(value = "角色")
    private String role;

    @ApiModelProperty(value = "对话内容")
    private String content;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "翻译列表")
    private List<TextTranslationVO> translations;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "排序")
    private Integer order;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "AI已审核的字段名列表")
    private List<String> aiReviewedFields;
}
```

- [ ] **Step 6: Create TopicPatternVO.java**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class TopicPatternVO {

    @ApiModelProperty(value = "句式ID")
    private Long id;

    @ApiModelProperty(value = "句式文本")
    private String pattern;

    @ApiModelProperty(value = "示意图资源ID")
    private Long imageId;

    @ApiModelProperty(value = "排序")
    private Integer order;

    @ApiModelProperty(value = "情景对话列表")
    private List<TopicChatVO> chats;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "AI已审核的字段名列表")
    private List<String> aiReviewedFields;
}
```

- [ ] **Step 7: Create TopicVO.java**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class TopicVO {

    @ApiModelProperty(value = "话题ID")
    private Long id;

    @ApiModelProperty(value = "话题名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "封面图资源ID")
    private Long coverImageId;

    @ApiModelProperty(value = "翻译列表")
    private List<TextTranslationVO> translations;

    @ApiModelProperty(value = "句式列表")
    private List<TopicPatternVO> patterns;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;

    @ApiModelProperty(value = "AI已审核的字段名列表")
    private List<String> aiReviewedFields;
}
```

- [ ] **Step 8: Create TopicWrapper.java**

```java
package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.request.TopicCreateRequest;
import com.naon.grid.backend.rest.request.TopicQueryRequest;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.vo.TopicBaseVO;
import com.naon.grid.backend.rest.vo.TopicChatVO;
import com.naon.grid.backend.rest.vo.TopicPatternVO;
import com.naon.grid.backend.rest.vo.TopicVO;
import com.naon.grid.backend.service.topic.dto.TopicChatDto;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicPatternDto;
import com.naon.grid.backend.service.topic.dto.TopicQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;
import com.naon.grid.modules.system.service.AiContentMarkerService.MarkerFields;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopicWrapper {

    public static TopicQueryCriteria toCriteria(TopicQueryRequest request) {
        TopicQueryCriteria criteria = new TopicQueryCriteria();
        if (request != null) {
            criteria.setBlurry(request.getBlurry());
            criteria.setPublishStatus(request.getPublishStatus());
            criteria.setEditStatus(request.getEditStatus());
        }
        return criteria;
    }

    public static TopicDto toDto(TopicCreateRequest request) {
        TopicDto dto = new TopicDto();
        dto.setName(request.getName());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setCoverImageId(request.getCoverImageId());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setPatterns(toPatternDtoList(request.getPatterns()));
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
        return dto;
    }

    private static List<TopicPatternDto> toPatternDtoList(
            List<TopicCreateRequest.TopicPatternRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(TopicWrapper::toPatternDto).collect(Collectors.toList());
    }

    private static TopicPatternDto toPatternDto(TopicCreateRequest.TopicPatternRequest req) {
        TopicPatternDto dto = new TopicPatternDto();
        dto.setPattern(req.getPattern());
        dto.setImageId(req.getImageId());
        dto.setOrder(req.getOrder());
        dto.setChats(toChatDtoList(req.getChats()));
        dto.setAiGeneratedFields(req.getAiGeneratedFields());
        return dto;
    }

    private static List<TopicChatDto> toChatDtoList(
            List<TopicCreateRequest.TopicChatRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(TopicWrapper::toChatDto).collect(Collectors.toList());
    }

    private static TopicChatDto toChatDto(TopicCreateRequest.TopicChatRequest req) {
        TopicChatDto dto = new TopicChatDto();
        dto.setRole(req.getRole());
        dto.setContent(req.getContent());
        dto.setPinyin(req.getPinyin());
        dto.setTranslations(toTextTranslationList(req.getTranslations()));
        dto.setAudioId(req.getAudioId());
        dto.setOrder(req.getOrder());
        dto.setAiGeneratedFields(req.getAiGeneratedFields());
        return dto;
    }

    // === DTO → VO ===

    public static List<TopicBaseVO> toBaseVOList(List<TopicDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(TopicWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static TopicBaseVO toBaseVO(TopicDto dto) {
        TopicBaseVO vo = new TopicBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setPatternCount(dto.getPatternCount() != null ? dto.getPatternCount() : 0);
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static TopicVO toVO(TopicDto dto, Map<String, MarkerFields> aiMarkers) {
        TopicVO vo = new TopicVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setCoverImageId(dto.getCoverImageId());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setPatterns(toPatternVOList(dto.getPatterns(), aiMarkers));
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());

        vo.setAiGeneratedFields(dto.getAiGeneratedFields() != null ? dto.getAiGeneratedFields() : Collections.emptyList());
        vo.setAiReviewedFields(Collections.emptyList());

        String key = AiContentMarkerHelper.key("topic", dto.getId());
        if (key != null && aiMarkers != null && aiMarkers.containsKey(key)) {
            MarkerFields fields = aiMarkers.get(key);
            vo.setAiGeneratedFields(fields.getGenerated());
            vo.setAiReviewedFields(fields.getReviewed());
        }
        return vo;
    }

    private static List<TopicPatternVO> toPatternVOList(List<TopicPatternDto> dtos,
            Map<String, MarkerFields> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toPatternVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static TopicPatternVO toPatternVO(TopicPatternDto dto, Map<String, MarkerFields> aiMarkers) {
        TopicPatternVO vo = new TopicPatternVO();
        vo.setId(dto.getId());
        vo.setPattern(dto.getPattern());
        vo.setImageId(dto.getImageId());
        vo.setOrder(dto.getOrder());
        vo.setChats(toChatVOList(dto.getChats(), aiMarkers));

        vo.setAiGeneratedFields(dto.getAiGeneratedFields() != null ? dto.getAiGeneratedFields() : Collections.emptyList());
        vo.setAiReviewedFields(Collections.emptyList());

        String key = AiContentMarkerHelper.key("topic_pattern", dto.getId());
        if (key != null && aiMarkers != null && aiMarkers.containsKey(key)) {
            MarkerFields fields = aiMarkers.get(key);
            vo.setAiGeneratedFields(fields.getGenerated());
            vo.setAiReviewedFields(fields.getReviewed());
        }
        return vo;
    }

    private static List<TopicChatVO> toChatVOList(List<TopicChatDto> dtos,
            Map<String, MarkerFields> aiMarkers) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toChatVO(dto, aiMarkers)).collect(Collectors.toList());
    }

    private static TopicChatVO toChatVO(TopicChatDto dto, Map<String, MarkerFields> aiMarkers) {
        TopicChatVO vo = new TopicChatVO();
        vo.setId(dto.getId());
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setAudioId(dto.getAudioId());
        vo.setOrder(dto.getOrder());

        vo.setAiGeneratedFields(dto.getAiGeneratedFields() != null ? dto.getAiGeneratedFields() : Collections.emptyList());
        vo.setAiReviewedFields(Collections.emptyList());

        String key = AiContentMarkerHelper.key("topic_chat", dto.getId());
        if (key != null && aiMarkers != null && aiMarkers.containsKey(key)) {
            MarkerFields fields = aiMarkers.get(key);
            vo.setAiGeneratedFields(fields.getGenerated());
            vo.setAiReviewedFields(fields.getReviewed());
        }
        return vo;
    }

    // === TextTranslation helpers ===

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(TopicWrapper::toTextTranslation).collect(Collectors.toList());
    }

    private static TextTranslation toTextTranslation(TextTranslationRequest request) {
        if (request == null) return null;
        TextTranslation t = new TextTranslation();
        t.setLanguage(request.getLanguage());
        t.setTranslation(request.getTranslation());
        return t;
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(TopicWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation t) {
        if (t == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(t.getLanguage());
        vo.setTranslation(t.getTranslation());
        return vo;
    }
}
```

- [ ] **Step 9: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/TopicCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/request/TopicQueryRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/TopicCreateVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/TopicBaseVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/TopicVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/TopicPatternVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/TopicChatVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/TopicWrapper.java
git commit -m "feat: add admin Topic request, VO, and wrapper classes"
```

---

### Task 8: TopicController (Admin)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/TopicController.java`

**Interfaces:**
- Consumes: `TopicService`, `AiContentMarkerService`, `TopicWrapper` from Tasks 4-7
- Produces: Full admin CRUD + workflow controller

- [ ] **Step 1: Create TopicController.java**

```java
package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.TopicCreateRequest;
import com.naon.grid.backend.rest.request.TopicQueryRequest;
import com.naon.grid.backend.rest.vo.TopicBaseVO;
import com.naon.grid.backend.rest.vo.TopicCreateVO;
import com.naon.grid.backend.rest.vo.TopicVO;
import com.naon.grid.backend.rest.wrapper.TopicWrapper;
import com.naon.grid.backend.service.topic.TopicService;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicChatDto;
import com.naon.grid.backend.service.topic.dto.TopicPatternDto;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;
import com.naon.grid.modules.system.service.AiContentMarkerService;
import com.naon.grid.modules.system.service.AiContentMarkerService.MarkerFields;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：话题管理")
@RequestMapping("/api/topic")
public class TopicController {

    private final TopicService topicService;
    private final AiContentMarkerService aiContentMarkerService;

    @Log("新增话题")
    @ApiOperation("新增话题")
    @AnonymousPostMapping
    public ResponseEntity<TopicCreateVO> create(@Valid @RequestBody TopicCreateRequest request) {
        TopicCreateVO vo = new TopicCreateVO();
        vo.setId(topicService.create(TopicWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("更新话题")
    @ApiOperation("更新话题")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody TopicCreateRequest request) {
        topicService.update(id, TopicWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("审核话题")
    @ApiOperation("话题草稿通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        topicService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布话题")
    @ApiOperation("发布话题（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        topicService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询话题详情")
    @ApiOperation("根据ID查询话题详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<TopicVO> findById(@PathVariable Long id) {
        TopicDto dto = topicService.findById(id);
        List<String> entityKeys = collectTopicEntityKeys(dto);
        Map<String, MarkerFields> aiMarkers = aiContentMarkerService.batchQuery(entityKeys);
        return new ResponseEntity<>(TopicWrapper.toVO(dto, aiMarkers), HttpStatus.OK);
    }

    private List<String> collectTopicEntityKeys(TopicDto dto) {
        List<String> keys = new ArrayList<>();
        keys.addAll(AiContentMarkerHelper.collectOne("topic", dto.getId()));
        if (dto.getPatterns() != null) {
            for (TopicPatternDto pattern : dto.getPatterns()) {
                keys.addAll(AiContentMarkerHelper.collectOne("topic_pattern", pattern.getId()));
                if (pattern.getChats() != null) {
                    for (TopicChatDto chat : pattern.getChats()) {
                        keys.addAll(AiContentMarkerHelper.collectOne("topic_chat", chat.getId()));
                    }
                }
            }
        }
        return keys;
    }

    @Log("查询话题列表")
    @ApiOperation("分页查询话题列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<TopicBaseVO>> queryAll(TopicQueryRequest request, Pageable pageable) {
        PageResult<TopicDto> pageResult = topicService.queryAll(TopicWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(TopicWrapper.toBaseVOList(pageResult.getContent()),
                        pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除话题")
    @ApiOperation("删除话题")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        topicService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线话题")
    @ApiOperation("下线话题")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        topicService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl grid-system -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/TopicController.java
git commit -m "feat: add TopicController admin CRUD and workflow endpoints"
```

---

### Task 9: APP VO and Request Classes

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppTopicSearchRequest.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppTopicBaseVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppTopicDetailVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppTopicPatternVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppTopicChatVO.java`

**Produces:** APP-specific VO and Request types with ImageVO/AudioVO inner classes, single-language TextTranslationVO

- [ ] **Step 1: Create AppTopicSearchRequest.java**

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AppTopicSearchRequest {

    @ApiModelProperty(value = "搜索关键词（模糊匹配话题名称）")
    private String blurry;
}
```

- [ ] **Step 2: Create AppTopicBaseVO.java**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AppTopicBaseVO {

    @ApiModelProperty(value = "话题ID")
    private Long id;

    @ApiModelProperty(value = "话题名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "翻译")
    private com.naon.grid.backend.rest.vo.TextTranslationVO translation;

    @ApiModelProperty(value = "封面图")
    private ImageVO coverImage;

    @ApiModelProperty(value = "音频")
    private AudioVO audio;

    @ApiModelProperty(value = "句式数量")
    private Integer patternCount;

    @Data
    public static class ImageVO {
        @ApiModelProperty(value = "图片URL")
        private String imageUrl;
    }

    @Data
    public static class AudioVO {
        @ApiModelProperty(value = "音频URL")
        private String audioUrl;
    }
}
```

- [ ] **Step 3: Create AppTopicChatVO.java**

```java
package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AppTopicChatVO {

    @ApiModelProperty(value = "角色")
    private String role;

    @ApiModelProperty(value = "对话内容")
    private String content;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "翻译")
    private TextTranslationVO translation;

    @ApiModelProperty(value = "音频")
    private AppTopicBaseVO.AudioVO audio;
}
```

- [ ] **Step 4: Create AppTopicPatternVO.java**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class AppTopicPatternVO {

    @ApiModelProperty(value = "句式文本")
    private String pattern;

    @ApiModelProperty(value = "示意图")
    private AppTopicBaseVO.ImageVO image;

    @ApiModelProperty(value = "排序")
    private Integer order;

    @ApiModelProperty(value = "情景对话列表")
    private List<AppTopicChatVO> chats;
}
```

- [ ] **Step 5: Create AppTopicDetailVO.java**

```java
package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class AppTopicDetailVO {

    @ApiModelProperty(value = "话题ID")
    private Long id;

    @ApiModelProperty(value = "话题名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "翻译")
    private TextTranslationVO translation;

    @ApiModelProperty(value = "封面图")
    private AppTopicBaseVO.ImageVO coverImage;

    @ApiModelProperty(value = "音频")
    private AppTopicBaseVO.AudioVO audio;

    @ApiModelProperty(value = "句式列表")
    private List<AppTopicPatternVO> patterns;
}
```

- [ ] **Step 6: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppTopicSearchRequest.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppTopicBaseVO.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppTopicDetailVO.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppTopicPatternVO.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppTopicChatVO.java
git commit -m "feat: add APP Topic VO and request classes"
```

---

### Task 10: AppTopicWrapper

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppTopicWrapper.java`

**Interfaces:**
- Consumes: DTOs from Task 3, VO types from Task 9, `AudioResourceDto`, `AliOssStorageDto`
- Produces: `AppTopicWrapper` with `toBaseVOList`, `toDetailVO` methods containing single-language filter and URL resolution

- [ ] **Step 1: Create AppTopicWrapper.java**

```java
package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.topic.dto.TopicChatDto;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicPatternDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppTopicBaseVO;
import com.naon.grid.modules.app.rest.vo.AppTopicChatVO;
import com.naon.grid.modules.app.rest.vo.AppTopicDetailVO;
import com.naon.grid.modules.app.rest.vo.AppTopicPatternVO;
import com.naon.grid.service.dto.AliOssStorageDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppTopicWrapper {

    public static List<AppTopicBaseVO> toBaseVOList(List<TopicDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(AppTopicWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static AppTopicBaseVO toBaseVO(TopicDto dto) {
        AppTopicBaseVO vo = new AppTopicBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setPatternCount(dto.getPatternCount());
        return vo;
    }

    public static AppTopicDetailVO toDetailVO(
            TopicDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppTopicDetailVO vo = new AppTopicDetailVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));

        // Cover image
        if (dto.getCoverImageId() != null && imageMap != null) {
            AliOssStorageDto ossDto = imageMap.get(dto.getCoverImageId());
            if (ossDto != null) {
                AppTopicBaseVO.ImageVO imageVO = new AppTopicBaseVO.ImageVO();
                imageVO.setImageUrl(ossDto.getFileUrl());
                vo.setCoverImage(imageVO);
            }
        }

        // Audio
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppTopicBaseVO.AudioVO audioVO = new AppTopicBaseVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            }
        }

        // Patterns
        vo.setPatterns(toPatternVOList(dto.getPatterns(), audioMap, imageMap, language));
        return vo;
    }

    private static List<AppTopicPatternVO> toPatternVOList(
            List<TopicPatternDto> dtos,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toPatternVO(dto, audioMap, imageMap, language))
                .collect(Collectors.toList());
    }

    private static AppTopicPatternVO toPatternVO(
            TopicPatternDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppTopicPatternVO vo = new AppTopicPatternVO();
        vo.setPattern(dto.getPattern());
        vo.setOrder(dto.getOrder());

        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto ossDto = imageMap.get(dto.getImageId());
            if (ossDto != null) {
                AppTopicBaseVO.ImageVO imageVO = new AppTopicBaseVO.ImageVO();
                imageVO.setImageUrl(ossDto.getFileUrl());
                vo.setImage(imageVO);
            }
        }

        vo.setChats(toChatVOList(dto.getChats(), audioMap, language));
        return vo;
    }

    private static List<AppTopicChatVO> toChatVOList(
            List<TopicChatDto> dtos,
            Map<Long, AudioResourceDto> audioMap,
            String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toChatVO(dto, audioMap, language))
                .collect(Collectors.toList());
    }

    private static AppTopicChatVO toChatVO(
            TopicChatDto dto,
            Map<Long, AudioResourceDto> audioMap,
            String language) {
        AppTopicChatVO vo = new AppTopicChatVO();
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));

        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppTopicBaseVO.AudioVO audioVO = new AppTopicBaseVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            }
        }

        return vo;
    }

    private static TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) {
            return null;
        }
        return translations.stream()
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst()
                .map(AppTopicWrapper::toTextTranslationVO)
                .orElse(null);
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation translation) {
        if (translation == null) {
            return null;
        }
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppTopicWrapper.java
git commit -m "feat: add AppTopicWrapper with language filter and URL resolution"
```

---

### Task 11: AppTopicController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppTopicController.java`

**Interfaces:**
- Consumes: `TopicService`, `AudioResourceService`, `AliOssStorageService`, `AppTopicWrapper`
- Produces: `AppTopicController` with `/search` and `/{id}` endpoints

- [ ] **Step 1: Create AppTopicController.java**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.topic.TopicService;
import com.naon.grid.backend.service.topic.dto.TopicChatDto;
import com.naon.grid.backend.service.topic.dto.TopicDto;
import com.naon.grid.backend.service.topic.dto.TopicPatternDto;
import com.naon.grid.modules.app.rest.request.AppTopicSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppTopicBaseVO;
import com.naon.grid.modules.app.rest.vo.AppTopicDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppTopicWrapper;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/topic")
@Api(tags = "用户：话题接口")
public class AppTopicController {

    private final TopicService topicService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;

    @ApiOperation("搜索话题")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppTopicBaseVO>> search(AppTopicSearchRequest request) {
        List<TopicDto> dtos = topicService.searchPublished(request.getBlurry());
        return new ResponseEntity<>(AppTopicWrapper.toBaseVOList(dtos), HttpStatus.OK);
    }

    @ApiOperation("话题详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppTopicDetailVO> getDetail(
            @PathVariable Long id,
            @RequestParam String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        TopicDto dto = topicService.findPublishedById(id);

        // Preload audio resources
        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);

        // Preload image resources
        Map<Long, AliOssStorageDto> imageMap = collectAndBatchQueryImages(dto);

        AppTopicDetailVO vo = AppTopicWrapper.toDetailVO(dto, audioMap, imageMap, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    private Map<Long, AudioResourceDto> collectAndBatchQueryAudios(TopicDto dto) {
        List<Long> audioIds = new ArrayList<>();
        if (dto.getAudioId() != null) {
            audioIds.add(dto.getAudioId());
        }
        if (dto.getPatterns() != null) {
            for (TopicPatternDto pattern : dto.getPatterns()) {
                if (pattern.getChats() != null) {
                    for (TopicChatDto chat : pattern.getChats()) {
                        if (chat.getAudioId() != null) {
                            audioIds.add(chat.getAudioId());
                        }
                    }
                }
            }
        }
        if (audioIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<AudioResourceDto> audioDtos = audioResourceService.findByIds(audioIds);
            return audioDtos.stream()
                    .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
        } catch (Exception e) {
            log.error("音频资源批量查询失败, audioIds={}", audioIds, e);
            return Collections.emptyMap();
        }
    }

    private Map<Long, AliOssStorageDto> collectAndBatchQueryImages(TopicDto dto) {
        List<Long> imageIds = new ArrayList<>();
        if (dto.getCoverImageId() != null) {
            imageIds.add(dto.getCoverImageId());
        }
        if (dto.getPatterns() != null) {
            for (TopicPatternDto pattern : dto.getPatterns()) {
                if (pattern.getImageId() != null) {
                    imageIds.add(pattern.getImageId());
                }
            }
        }
        if (imageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<AliOssStorageDto> imageDtos = aliOssStorageService.findByIds(imageIds);
            return imageDtos.stream()
                    .collect(Collectors.toMap(AliOssStorageDto::getId, i -> i, (a, b) -> a));
        } catch (Exception e) {
            log.error("图片资源批量查询失败, imageIds={}", imageIds, e);
            return Collections.emptyMap();
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn compile -pl grid-system,grid-app -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppTopicController.java
git commit -m "feat: add AppTopicController with search and detail endpoints"
```

---

### Task 12: Collection Integration

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/enums/CollectionBizTypeEnum.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/CollectionWrapper.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/CollectionServiceImpl.java`

**Interfaces:**
- Consumes: `TopicService` (for resolveContentName and validateContentExists)
- Produces: TOPIC bizType integration with existing favorites system

- [ ] **Step 1: Add TOPIC to CollectionBizTypeEnum**

Read the existing file, then add the new enum value:

In `CollectionBizTypeEnum.java`, add after `DAILY_VOCABULARY`:
```java
    TOPIC("TOPIC", "话题");
```

- [ ] **Step 2: Add TOPIC case in CollectionWrapper.resolveContentName()**

Read the existing `CollectionWrapper.java`, find the `resolveContentName` method, add after the last case:
```java
            case "TOPIC":
                TopicDto topicDto = topicService.findById(item.getContentId());
                return topicDto.getName();
```

Also add the `TopicService` import and constructor parameter:
```java
import com.naon.grid.backend.service.topic.TopicService;
import com.naon.grid.backend.service.topic.dto.TopicDto;
```

- [ ] **Step 3: Add TOPIC case in CollectionServiceImpl.validateContentExists()**

Read the existing `CollectionServiceImpl.java`, find the `validateContentExists` method, add after the last case:
```java
            case TOPIC:
                topicService.findPublishedById(contentId);
                break;
```

Note: `TOPIC` is already imported as the enum constant from `CollectionBizTypeEnum.TOPIC` — just add the case to the switch.

Also add the `TopicService` field injection:
```java
    private final TopicService topicService;
```

- [ ] **Step 4: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/enums/CollectionBizTypeEnum.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/CollectionWrapper.java \
        grid-app/src/main/java/com/naon/grid/modules/app/service/impl/CollectionServiceImpl.java
git commit -m "feat: integrate TOPIC type into favorites collection system"
```

---

### Task 13: End-to-End Verification

- [ ] **Step 1: Full project compilation**

```bash
mvn clean compile -DskipTests
```
Expected: BUILD SUCCESS across all modules.

- [ ] **Step 2: Check for any missing imports or compilation errors**

Fix any import issues. Common checks:
- `TopicWrapper` properly imports `AiContentMarkerHelper` and `MarkerFields`
- `AppTopicController` properly imports `AudioResourceService` and `AliOssStorageService`
- `CollectionWrapper` properly imports `TopicService` and `TopicDto`
- `TopicServiceImpl` properly imports `ExampleSentenceService` and `ExampleSentenceDto`

- [ ] **Step 3: Run project tests**

```bash
mvn test -pl grid-system -am
```
Expected: Existing tests pass (no regression).

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: final verification and import fixes for topic feature"
```
