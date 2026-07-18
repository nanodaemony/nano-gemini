# Culture Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement full culture (文化点) management: backend CRUD with draft/review/publish workflow, APP-side search + detail with translation filtering, and collection integration.

**Architecture:** Follows the GrammarPoint module pattern exactly. grid-system holds all domain logic (DO, Repo, Service, DTO, backend Controller). grid-app holds APP-side Controllers and VOs. Backend Service reuse across both modules.

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Fastjson2, Lombok, MapStruct, Java 8

## Global Constraints

- All controller I/O uses dedicated Request/VO classes, never domain entities directly
- All DTO↔VO conversion in static Wrapper classes (`{Controller包}/wrapper/`), not in Controllers
- Draft workflow: CREATE→DRAFT, review→REVIEWED, publish→PUBLISHED (materializes sub-tables), offline→UNPUBLISHED
- List queries use draft overlay: if editStatus is DRAFT/REVIEWED, parse draft_content JSON to overlay DTO fields
- APP-side: translations filtered by `language` param, audio/image IDs converted to URLs via preloaded resource maps
- APP-side: resource lookup failures log error but don't throw, field returns null
- APP-side VOs must not expose createBy/updateBy/createTime/updateTime/editStatus/publishStatus/status
- APP-side VOs defined independently from backend VOs
- Collection uses `biz_type` discriminator, `content_id` stores culture.id, names resolved at read-time
- Follow existing code patterns: package structure, naming, annotations identical to GrammarPoint/Character modules

---

### Task 1: Domain classes — Culture.java and CultureKeyword.java

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/culture/Culture.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/culture/CultureKeyword.java`

**Interfaces:**
- Produces: `Culture` entity (table `culture`, extends `BaseEntity`), `CultureKeyword` entity (table `culture_keyword`, standalone timestamps)

**Reference:** `GrammarPoint.java`, `GrammarMeaning.java`

- [ ] **Step 1: Create Culture.java**

```java
package com.naon.grid.backend.domain.culture;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "culture")
public class Culture extends BaseEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "文化点ID", hidden = true)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "pinyin", length = 256)
    private String pinyin;

    @Column(name = "audio_id")
    private Long audioId;

    @Column(name = "translations", columnDefinition = "text")
    private String translations;

    @Column(name = "cover_image_id")
    private Long coverImageId;

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "project", length = 50)
    private String project;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "one_sentence_intro", length = 1024)
    private String oneSentenceIntro;

    @Column(name = "one_sentence_intro_translations", columnDefinition = "text")
    private String oneSentenceIntroTranslations;

    @Column(name = "one_sentence_intro_audio_id")
    private Long oneSentenceIntroAudioId;

    @Column(name = "one_sentence_intro_image_id")
    private Long oneSentenceIntroImageId;

    @Column(name = "detailed_intro", columnDefinition = "text")
    private String detailedIntro;

    @Column(name = "detailed_intro_translations", columnDefinition = "text")
    private String detailedIntroTranslations;

    @Column(name = "detailed_intro_audio_id")
    private Long detailedIntroAudioId;

    @Column(name = "detailed_intro_image_id")
    private Long detailedIntroImageId;

    @Column(name = "sentence_ids", columnDefinition = "text")
    private String sentenceIds;

    @Column(name = "question_ids", columnDefinition = "text")
    private String questionIds;

    @Column(name = "draft_content", columnDefinition = "text")
    @ApiModelProperty(value = "草稿内容JSON（包含主表和子表）")
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

- [ ] **Step 2: Create CultureKeyword.java**

```java
package com.naon.grid.backend.domain.culture;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "culture_keyword")
public class CultureKeyword implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "关键词ID", hidden = true)
    private Long id;

    @Column(name = "culture_id", nullable = false)
    private Long cultureId;

    @Column(name = "keyword", nullable = false, length = 128)
    private String keyword;

    @Column(name = "keyword_description", columnDefinition = "text")
    private String keywordDescription;

    @Column(name = "keyword_translations", columnDefinition = "text")
    private String keywordTranslations;

    @Column(name = "keyword_description_translations", columnDefinition = "text")
    private String keywordDescriptionTranslations;

    @Column(name = "audio_id")
    private Long audioId;

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer order = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd grid-system && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/culture/Culture.java \
        grid-system/src/main/java/com/naon/grid/backend/domain/culture/CultureKeyword.java
git commit -m "feat: add Culture and CultureKeyword domain entities"
```

---

### Task 2: Repository classes

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/culture/CultureRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/culture/CultureKeywordRepository.java`

**Interfaces:**
- Produces: `CultureRepository extends JpaRepository<Culture, Long>, JpaSpecificationExecutor<Culture>`, `CultureKeywordRepository extends JpaRepository<CultureKeyword, Long>` with custom query methods

**Reference:** `GrammarPointRepository.java`

- [ ] **Step 1: Create CultureRepository.java**

```java
package com.naon.grid.backend.repo.culture;

import com.naon.grid.backend.domain.culture.Culture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CultureRepository extends JpaRepository<Culture, Long>, JpaSpecificationExecutor<Culture> {
}
```

- [ ] **Step 2: Create CultureKeywordRepository.java**

```java
package com.naon.grid.backend.repo.culture;

import com.naon.grid.backend.domain.culture.CultureKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CultureKeywordRepository extends JpaRepository<CultureKeyword, Long> {

    List<CultureKeyword> findByCultureIdAndStatus(Long cultureId, Integer status);

    List<CultureKeyword> findByCultureIdInAndStatus(List<Long> cultureIds, Integer status);

    @Query("SELECT c.cultureId, COUNT(c) FROM CultureKeyword c WHERE c.cultureId IN :cultureIds AND c.status = :status GROUP BY c.cultureId")
    List<Object[]> countByCultureIdInGroupByCultureId(@Param("cultureIds") List<Long> cultureIds, @Param("status") Integer status);

    @Modifying
    @Query("UPDATE CultureKeyword c SET c.status = 0 WHERE c.id = :id")
    void softDeleteById(@Param("id") Long id);
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd grid-system && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/culture/
git commit -m "feat: add CultureRepository and CultureKeywordRepository"
```

---

### Task 3: DTO classes — CultureDto, CultureKeywordDto, CultureQueryCriteria

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/culture/dto/CultureDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/culture/dto/CultureKeywordDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/culture/dto/CultureQueryCriteria.java`

**Interfaces:**
- Produces: `CultureDto` (extends `BaseDTO`), `CultureKeywordDto` (implements `Serializable`), `CultureQueryCriteria` (query filters with `@Query` annotations)

**Reference:** `GrammarPointDto.java`, `GrammarMeaningDto.java`, `GrammarPointQueryCriteria.java`

- [ ] **Step 1: Create CultureKeywordDto.java**

```java
package com.naon.grid.backend.service.culture.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CultureKeywordDto implements Serializable {

    private Long id;
    private Long cultureId;
    private String keyword;
    private String keywordDescription;
    private String keywordTranslations;
    private String keywordDescriptionTranslations;
    private Long audioId;
    private Long imageId;
    private Integer order;
    private Timestamp createTime;
    private Timestamp updateTime;
    private List<String> aiGeneratedFields;
}
```

- [ ] **Step 2: Create CultureDto.java**

```java
package com.naon.grid.backend.service.culture.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CultureDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "文化点ID")
    private Long id;

    @ApiModelProperty(value = "文化点名称")
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "名称音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "名称多语言翻译JSON")
    private String translations;

    @ApiModelProperty(value = "封面图片资源ID")
    private Long coverImageId;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "一级项目")
    private String project;

    @ApiModelProperty(value = "二级项目")
    private String category;

    @ApiModelProperty(value = "一句话介绍")
    private String oneSentenceIntro;

    @ApiModelProperty(value = "一句话介绍翻译JSON")
    private String oneSentenceIntroTranslations;

    @ApiModelProperty(value = "一句话介绍音频ID")
    private Long oneSentenceIntroAudioId;

    @ApiModelProperty(value = "一句话介绍图片ID")
    private Long oneSentenceIntroImageId;

    @ApiModelProperty(value = "详细介绍")
    private String detailedIntro;

    @ApiModelProperty(value = "详细介绍翻译JSON")
    private String detailedIntroTranslations;

    @ApiModelProperty(value = "详细介绍音频ID")
    private Long detailedIntroAudioId;

    @ApiModelProperty(value = "详细介绍图片ID")
    private Long detailedIntroImageId;

    @ApiModelProperty(value = "学一学例句ID列表")
    private List<Long> sentenceIds;

    @ApiModelProperty(value = "练一练习题ID列表")
    private List<Long> questionIds;

    @ApiModelProperty(value = "关键词列表")
    private List<CultureKeywordDto> keywords;

    @ApiModelProperty(value = "学一学例句详情列表（已发布时加载）")
    private List<ExampleSentenceDto> sentences;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    // --- 列表统计字段 ---
    @ApiModelProperty(value = "关键词个数")
    private Integer keywordCount;

    @ApiModelProperty(value = "学一学例句个数")
    private Integer sentenceCount;

    @ApiModelProperty(value = "练一练习题个数")
    private Integer questionCount;
}
```

- [ ] **Step 3: Create CultureQueryCriteria.java**

```java
package com.naon.grid.backend.service.culture.dto;

import com.naon.grid.annotation.Query;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CultureQueryCriteria {

    @Query(blurry = "name")
    private String blurry;

    @Query
    private String publishStatus;

    @Query
    private String editStatus;

    @Query
    private String level;

    @Query
    private String project;

    @Query
    private String category;
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd grid-system && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 5: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/culture/dto/
git commit -m "feat: add CultureDto, CultureKeywordDto, CultureQueryCriteria"
```

---

### Task 4: MapStruct Mapper — CultureMapper.java

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/culture/mapstruct/CultureMapper.java`

**Interfaces:**
- Produces: `CultureMapper` — MapStruct interface mapping `Culture` entity ↔ `CultureDto`

**Reference:** `GrammarPointMapper.java`

- [ ] **Step 1: Create CultureMapper.java**

```java
package com.naon.grid.backend.service.culture.mapstruct;

import com.naon.grid.backend.domain.culture.Culture;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.alibaba.fastjson2.JSON;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CultureMapper {

    @Mapping(target = "sentenceIds", source = "sentenceIds", qualifiedByName = "jsonToLongList")
    @Mapping(target = "questionIds", source = "questionIds", qualifiedByName = "jsonToLongList")
    CultureDto toDto(Culture entity);

    @Named("jsonToLongList")
    default List<Long> jsonToLongList(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        return JSON.parseArray(json, Long.class);
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd grid-system && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/culture/mapstruct/CultureMapper.java
git commit -m "feat: add CultureMapper (MapStruct)"
```

---

### Task 5: CultureService interface

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/culture/CultureService.java`

**Interfaces:**
- Produces: `CultureService` interface with 11 methods matching character/grammar pattern

**Reference:** `GrammarPointService.java`

- [ ] **Step 1: Create CultureService.java**

```java
package com.naon.grid.backend.service.culture;

import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CultureService {

    PageResult<CultureDto> queryAll(CultureQueryCriteria criteria, Pageable pageable);

    CultureDto findById(Long id);

    CultureDto findPublishedById(Long id);

    Long create(CultureDto dto);

    void update(Long id, CultureDto dto);

    void delete(Long id);

    List<CultureDto> searchPublished(String blurry);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd grid-system && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/culture/CultureService.java
git commit -m "feat: add CultureService interface"
```

---

### Task 6: CultureServiceImpl — core business logic

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/culture/impl/CultureServiceImpl.java`

**Interfaces:**
- Consumes: `CultureRepository`, `CultureKeywordRepository`, `CultureMapper`, `ExampleSentenceService`, `AiContentMarkerService` (from Task 2-4)
- Produces: Full draft workflow, draft overlay, sub-table sync, sentence/question hydration

**Reference:** `GrammarPointServiceImpl.java`

- [ ] **Step 1: Create CultureServiceImpl.java — imports and class declaration**

```java
package com.naon.grid.backend.service.culture.impl;

import com.naon.grid.backend.domain.culture.Culture;
import com.naon.grid.backend.domain.culture.CultureKeyword;
import com.naon.grid.backend.repo.culture.CultureKeywordRepository;
import com.naon.grid.backend.repo.culture.CultureRepository;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.culture.CultureService;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureKeywordDto;
import com.naon.grid.backend.service.culture.dto.CultureQueryCriteria;
import com.naon.grid.backend.service.culture.mapstruct.CultureMapper;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CultureServiceImpl implements CultureService {

    private final CultureRepository cultureRepository;
    private final CultureKeywordRepository cultureKeywordRepository;
    private final CultureMapper cultureMapper;
    private final ExampleSentenceService exampleSentenceService;
    private final AiContentMarkerService aiContentMarkerService;
```

- [ ] **Step 2: queryAll — paginated list with draft overlay**

```java
    @Override
    public PageResult<CultureDto> queryAll(CultureQueryCriteria criteria, Pageable pageable) {
        Page<Culture> page = cultureRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        PageResult<CultureDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        populateCultureListStats(pageResult.getContent());
        return pageResult;
    }

    private CultureDto toDtoWithDraftOverlay(Culture entity) {
        CultureDto dto = cultureMapper.toDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    private void applyDraftOverlay(CultureDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        CultureDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, CultureDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }

        if (draft.getName() != null)             dto.setName(draft.getName());
        if (draft.getPinyin() != null)           dto.setPinyin(draft.getPinyin());
        if (draft.getLevel() != null)            dto.setLevel(draft.getLevel());
        if (draft.getProject() != null)          dto.setProject(draft.getProject());
        if (draft.getCategory() != null)         dto.setCategory(draft.getCategory());

        if (draft.getKeywords() != null) {
            dto.setKeywordCount(draft.getKeywords().size());
        }
        if (draft.getSentenceIds() != null) {
            dto.setSentenceCount(draft.getSentenceIds().size());
            dto.setSentenceIds(draft.getSentenceIds());
        }
        if (draft.getQuestionIds() != null) {
            dto.setQuestionCount(draft.getQuestionIds().size());
            dto.setQuestionIds(draft.getQuestionIds());
        }
    }

    private void populateCultureListStats(List<CultureDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<Long> ids = dtos.stream().map(CultureDto::getId).collect(Collectors.toList());

        Map<Long, Long> keywordCountMap = cultureKeywordRepository
                .countByCultureIdInGroupByCultureId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        for (CultureDto dto : dtos) {
            if (dto.getKeywordCount() == null) {
                dto.setKeywordCount(keywordCountMap.getOrDefault(dto.getId(), 0L).intValue());
            }
            if (dto.getSentenceCount() == null && dto.getSentenceIds() != null) {
                dto.setSentenceCount(dto.getSentenceIds().size());
            }
            if (dto.getQuestionCount() == null && dto.getQuestionIds() != null) {
                dto.setQuestionCount(dto.getQuestionIds().size());
            }
        }
    }
```

- [ ] **Step 3: findById — detail with draft overlay**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CultureDto findById(Long id) {
        if (id == null) {
            throw new EntityNotFoundException(Culture.class, "id", String.valueOf(id));
        }
        Culture culture = cultureRepository.findById(id).orElseGet(Culture::new);
        if (culture.getId() == null || StatusEnum.DISABLED.getCode().equals(culture.getStatus())) {
            throw new EntityNotFoundException(Culture.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.DRAFT.getCode().equals(culture.getEditStatus()) ||
            EditStatusEnum.REVIEWED.getCode().equals(culture.getEditStatus())) {
            if (culture.getDraftContent() == null) {
                throw new BadRequestException("Draft content not found");
            }
            CultureDto dto = JsonUtils.fromJson(culture.getDraftContent(), CultureDto.class);
            dto.setId(culture.getId());
            dto.setStatus(culture.getStatus());
            dto.setPublishStatus(culture.getPublishStatus());
            dto.setEditStatus(culture.getEditStatus());
            dto.setCreateBy(culture.getCreateBy());
            dto.setUpdateBy(culture.getUpdateBy());
            dto.setCreateTime(culture.getCreateTime());
            dto.setUpdateTime(culture.getUpdateTime());
            if (dto.getKeywords() != null) {
                dto.getKeywords().sort(Comparator.comparing(CultureKeywordDto::getOrder, Comparator.reverseOrder()));
            }
            return dto;
        }

        return toPublishedDetailDto(culture);
    }

    private CultureDto toPublishedDetailDto(Culture culture) {
        Long id = culture.getId();
        CultureDto dto = cultureMapper.toDto(culture);

        List<CultureKeyword> keywords = cultureKeywordRepository.findByCultureIdAndStatus(id, StatusEnum.ENABLED.getCode());
        dto.setKeywords(convertToKeywordDtos(keywords).stream()
                .sorted(Comparator.comparing(CultureKeywordDto::getOrder, Comparator.reverseOrder()))
                .collect(Collectors.toList()));

        if (dto.getSentenceIds() != null && !dto.getSentenceIds().isEmpty()) {
            Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(dto.getSentenceIds());
            List<ExampleSentenceDto> sentences = new ArrayList<>();
            for (Long sid : dto.getSentenceIds()) {
                ExampleSentenceDto s = sentenceMap.get(sid);
                if (s != null) sentences.add(s);
            }
            dto.setSentences(sentences);
            dto.setSentenceCount(sentences.size());
        }

        if (dto.getQuestionIds() != null && !dto.getQuestionIds().isEmpty()) {
            dto.setQuestionCount(dto.getQuestionIds().size());
        }

        return dto;
    }

    private List<CultureKeywordDto> convertToKeywordDtos(List<CultureKeyword> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(e -> {
            CultureKeywordDto dto = new CultureKeywordDto();
            dto.setId(e.getId());
            dto.setCultureId(e.getCultureId());
            dto.setKeyword(e.getKeyword());
            dto.setKeywordDescription(e.getKeywordDescription());
            dto.setKeywordTranslations(e.getKeywordTranslations());
            dto.setKeywordDescriptionTranslations(e.getKeywordDescriptionTranslations());
            dto.setAudioId(e.getAudioId());
            dto.setImageId(e.getImageId());
            dto.setOrder(e.getOrder());
            dto.setCreateTime(e.getCreateTime());
            dto.setUpdateTime(e.getUpdateTime());
            return dto;
        }).collect(Collectors.toList());
    }
```

- [ ] **Step 4: findPublishedById — published-only detail**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CultureDto findPublishedById(Long id) {
        if (id == null) {
            throw new EntityNotFoundException(Culture.class, "id", String.valueOf(id));
        }
        Culture culture = cultureRepository.findById(id).orElseGet(Culture::new);
        if (culture.getId() == null || StatusEnum.DISABLED.getCode().equals(culture.getStatus())) {
            throw new EntityNotFoundException(Culture.class, "id", String.valueOf(id));
        }
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(culture.getPublishStatus())) {
            throw new BadRequestException("文化点尚未发布");
        }
        return toPublishedDetailDto(culture);
    }
```

- [ ] **Step 5: create — new culture point as draft**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CultureDto dto) {
        Culture culture = new Culture();
        culture.setName(dto.getName());
        culture.setDraftContent(JsonUtils.toJson(dto));
        culture.setStatus(StatusEnum.ENABLED.getCode());
        culture.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        culture.setEditStatus(EditStatusEnum.DRAFT.getCode());
        culture = cultureRepository.save(culture);
        return culture.getId();
    }
```

- [ ] **Step 6: update — overwrite draft content**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CultureDto dto) {
        Culture culture = cultureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Culture.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(culture.getStatus())) {
            throw new BadRequestException("已删除的文化点不能修改");
        }

        if (dto.getName() != null) {
            culture.setName(dto.getName());
        }
        culture.setDraftContent(JsonUtils.toJson(dto));

        if (EditStatusEnum.REVIEWED.getCode().equals(culture.getEditStatus()) ||
            EditStatusEnum.PUBLISHED.getCode().equals(culture.getEditStatus())) {
            culture.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        cultureRepository.save(culture);
    }
```

- [ ] **Step 7: reviewDraft, publishDraft, offline, delete**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        Culture culture = cultureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Culture.class, "id", String.valueOf(id)));
        if (!EditStatusEnum.DRAFT.getCode().equals(culture.getEditStatus())) {
            throw new BadRequestException("只有草稿状态的文化点可以审核");
        }
        if (culture.getDraftContent() == null) {
            throw new BadRequestException("草稿内容为空，无法审核");
        }
        culture.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        cultureRepository.save(culture);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id) {
        Culture culture = cultureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Culture.class, "id", String.valueOf(id)));
        if (!EditStatusEnum.REVIEWED.getCode().equals(culture.getEditStatus())) {
            throw new BadRequestException("只有已审核状态的文化点可以发布");
        }
        if (culture.getDraftContent() == null) {
            throw new BadRequestException("草稿内容为空，无法发布");
        }

        CultureDto draft = JsonUtils.fromJson(culture.getDraftContent(), CultureDto.class);

        // 回写主表字段
        culture.setName(draft.getName());
        culture.setPinyin(draft.getPinyin());
        culture.setAudioId(draft.getAudioId());
        culture.setTranslations(draft.getTranslations());
        culture.setCoverImageId(draft.getCoverImageId());
        culture.setLevel(draft.getLevel());
        culture.setProject(draft.getProject());
        culture.setCategory(draft.getCategory());
        culture.setOneSentenceIntro(draft.getOneSentenceIntro());
        culture.setOneSentenceIntroTranslations(draft.getOneSentenceIntroTranslations());
        culture.setOneSentenceIntroAudioId(draft.getOneSentenceIntroAudioId());
        culture.setOneSentenceIntroImageId(draft.getOneSentenceIntroImageId());
        culture.setDetailedIntro(draft.getDetailedIntro());
        culture.setDetailedIntroTranslations(draft.getDetailedIntroTranslations());
        culture.setDetailedIntroAudioId(draft.getDetailedIntroAudioId());
        culture.setDetailedIntroImageId(draft.getDetailedIntroImageId());
        culture.setSentenceIds(draft.getSentenceIds() != null ? JsonUtils.toJson(draft.getSentenceIds()) : null);
        culture.setQuestionIds(draft.getQuestionIds() != null ? JsonUtils.toJson(draft.getQuestionIds()) : null);

        // 同步子表 culture_keyword
        syncKeywords(id, draft.getKeywords());

        // 材料化 AI content markers
        aiContentMarkerService.batchReplace("culture", id, Collections.emptyMap());

        culture.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        culture.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        culture.setDraftContent(null);
        cultureRepository.save(culture);
    }

    private void syncKeywords(Long cultureId, List<CultureKeywordDto> draftKeywords) {
        List<CultureKeyword> existingKeywords = cultureKeywordRepository
                .findByCultureIdAndStatus(cultureId, StatusEnum.ENABLED.getCode());
        Map<Long, CultureKeyword> existingMap = existingKeywords.stream()
                .collect(Collectors.toMap(CultureKeyword::getId, k -> k));
        Set<Long> submittedIds = new java.util.HashSet<>();

        if (draftKeywords != null) {
            for (CultureKeywordDto dto : draftKeywords) {
                if (dto.getId() != null && dto.getId() > 0 && existingMap.containsKey(dto.getId())) {
                    // Update existing
                    CultureKeyword existing = existingMap.get(dto.getId());
                    existing.setKeyword(dto.getKeyword());
                    existing.setKeywordDescription(dto.getKeywordDescription());
                    existing.setKeywordTranslations(dto.getKeywordTranslations());
                    existing.setKeywordDescriptionTranslations(dto.getKeywordDescriptionTranslations());
                    existing.setAudioId(dto.getAudioId());
                    existing.setImageId(dto.getImageId());
                    existing.setOrder(dto.getOrder() != null ? dto.getOrder() : 0);
                    cultureKeywordRepository.save(existing);
                    submittedIds.add(dto.getId());
                } else {
                    // Insert new
                    CultureKeyword newKeyword = new CultureKeyword();
                    newKeyword.setCultureId(cultureId);
                    newKeyword.setKeyword(dto.getKeyword());
                    newKeyword.setKeywordDescription(dto.getKeywordDescription());
                    newKeyword.setKeywordTranslations(dto.getKeywordTranslations());
                    newKeyword.setKeywordDescriptionTranslations(dto.getKeywordDescriptionTranslations());
                    newKeyword.setAudioId(dto.getAudioId());
                    newKeyword.setImageId(dto.getImageId());
                    newKeyword.setOrder(dto.getOrder() != null ? dto.getOrder() : 0);
                    newKeyword.setStatus(StatusEnum.ENABLED.getCode());
                    cultureKeywordRepository.save(newKeyword);
                }
            }
        }

        // Soft delete keywords not in draft
        for (CultureKeyword existing : existingKeywords) {
            if (!submittedIds.contains(existing.getId())) {
                cultureKeywordRepository.softDeleteById(existing.getId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        Culture culture = cultureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Culture.class, "id", String.valueOf(id)));
        culture.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        cultureRepository.save(culture);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Culture culture = cultureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Culture.class, "id", String.valueOf(id)));
        culture.setStatus(StatusEnum.DISABLED.getCode());
        cultureRepository.save(culture);
    }
```

- [ ] **Step 8: searchPublished**

```java
    @Override
    public List<CultureDto> searchPublished(String blurry) {
        List<Culture> entities = cultureRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode()));
            predicates.add(criteriaBuilder.equal(root.get("publishStatus"), PublishStatusEnum.PUBLISHED.getCode()));
            if (blurry != null && !blurry.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + blurry.trim() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
        return entities.stream().map(cultureMapper::toDto).collect(Collectors.toList());
    }
```

- [ ] **Step 9: Verify compilation**

```bash
cd grid-system && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 10: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/culture/impl/CultureServiceImpl.java
git commit -m "feat: add CultureServiceImpl with full draft/publish workflow"
```

---

### Task 7: Backend Request classes

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CultureCreateRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CultureQueryRequest.java`

**Interfaces:**
- Produces: `CultureCreateRequest` (nested `CultureKeywordRequest`), `CultureQueryRequest`

**Reference:** `GrammarPointCreateRequest.java`, `GrammarPointQueryRequest.java`

- [ ] **Step 1: Create CultureCreateRequest.java**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Getter
@Setter
public class CultureCreateRequest {

    @NotBlank
    @ApiModelProperty(value = "文化点名称", required = true)
    private String name;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "名称音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "名称多语言翻译")
    private List<TextTranslationRequest> translations;

    @ApiModelProperty(value = "封面图片资源ID")
    private Long coverImageId;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "一级项目")
    private String project;

    @ApiModelProperty(value = "二级项目")
    private String category;

    @ApiModelProperty(value = "一句话介绍")
    private String oneSentenceIntro;

    @ApiModelProperty(value = "一句话介绍翻译")
    private List<TextTranslationRequest> oneSentenceIntroTranslations;

    @ApiModelProperty(value = "一句话介绍音频ID")
    private Long oneSentenceIntroAudioId;

    @ApiModelProperty(value = "一句话介绍图片ID")
    private Long oneSentenceIntroImageId;

    @ApiModelProperty(value = "详细介绍")
    private String detailedIntro;

    @ApiModelProperty(value = "详细介绍翻译")
    private List<TextTranslationRequest> detailedIntroTranslations;

    @ApiModelProperty(value = "详细介绍音频ID")
    private Long detailedIntroAudioId;

    @ApiModelProperty(value = "详细介绍图片ID")
    private Long detailedIntroImageId;

    @ApiModelProperty(value = "学一学例句ID列表")
    private List<Long> sentenceIds;

    @ApiModelProperty(value = "练一练习题ID列表")
    private List<Long> questionIds;

    @ApiModelProperty(value = "关键词列表")
    private List<CultureKeywordRequest> keywords;

    @ApiModelProperty(value = "AI生成字段")
    private List<String> aiGeneratedFields;

    @Getter
    @Setter
    public static class CultureKeywordRequest {
        private Long id;
        @NotBlank
        private String keyword;
        private String keywordDescription;
        private List<TextTranslationRequest> keywordTranslations;
        private List<TextTranslationRequest> keywordDescriptionTranslations;
        private Long audioId;
        private Long imageId;
        private Integer order;
        private List<String> aiGeneratedFields;
    }
}
```

- [ ] **Step 2: Create CultureQueryRequest.java**

```java
package com.naon.grid.backend.rest.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CultureQueryRequest {

    private String blurry;
    private String publishStatus;
    private String editStatus;
    private String level;
    private String project;
    private String category;
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd grid-system && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/CultureCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/request/CultureQueryRequest.java
git commit -m "feat: add CultureCreateRequest and CultureQueryRequest"
```

---

### Task 8: Backend VO classes

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CultureCreateVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CultureBaseVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CultureVO.java`

**Interfaces:**
- Produces: `CultureCreateVO` ({id}), `CultureBaseVO` (list row with stats), `CultureVO` (detail with nested keyword VOs and AI markers)

**Reference:** `GrammarPointCreateVO.java`, `GrammarPointBaseVO.java`, `GrammarPointVO.java`

- [ ] **Step 1: Create CultureCreateVO.java**

```java
package com.naon.grid.backend.rest.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class CultureCreateVO implements Serializable {
    private Long id;
}
```

- [ ] **Step 2: Create CultureBaseVO.java**

```java
package com.naon.grid.backend.rest.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CultureBaseVO implements Serializable {

    private Long id;
    private String name;
    private String pinyin;
    private Long audioId;
    private Long coverImageId;
    private String level;
    private String project;
    private String category;
    private String publishStatus;
    private String editStatus;
    private Integer keywordCount;
    private Integer sentenceCount;
    private Integer questionCount;
    private String createBy;
    private String updateBy;
    private Timestamp createTime;
    private Timestamp updateTime;
    private List<TextTranslationVO> translations;
}
```

- [ ] **Step 3: Create CultureVO.java** (detail with nested inner VOs)

```java
package com.naon.grid.backend.rest.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CultureVO implements Serializable {

    private Long id;
    private String name;
    private String pinyin;
    private Long audioId;
    private List<TextTranslationVO> translations;
    private Long coverImageId;
    private String level;
    private String project;
    private String category;

    private String oneSentenceIntro;
    private List<TextTranslationVO> oneSentenceIntroTranslations;
    private Long oneSentenceIntroAudioId;
    private Long oneSentenceIntroImageId;

    private String detailedIntro;
    private List<TextTranslationVO> detailedIntroTranslations;
    private Long detailedIntroAudioId;
    private Long detailedIntroImageId;

    private List<Long> sentenceIds;
    private List<Long> questionIds;

    private List<CultureKeywordVO> keywords;
    private List<ExampleSentenceVO> sentences;

    private String publishStatus;
    private String editStatus;
    private String createBy;
    private String updateBy;
    private Timestamp createTime;
    private Timestamp updateTime;

    @Getter
    @Setter
    public static class CultureKeywordVO implements Serializable {
        private Long id;
        private String keyword;
        private String keywordDescription;
        private List<TextTranslationVO> keywordTranslations;
        private List<TextTranslationVO> keywordDescriptionTranslations;
        private Long audioId;
        private Long imageId;
        private Integer order;
        private Timestamp createTime;
        private Timestamp updateTime;
        private List<String> aiGeneratedFields;
        private List<String> aiReviewedFields;
    }
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd grid-system && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 5: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/CultureCreateVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/CultureBaseVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/CultureVO.java
git commit -m "feat: add CultureCreateVO, CultureBaseVO, CultureVO"
```

---

### Task 9: CultureWrapper (backend)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CultureWrapper.java`

**Interfaces:**
- Consumes: `CultureCreateRequest`, `CultureQueryRequest`, `CultureDto`, `CultureKeywordDto`, `ExampleSentenceDto`
- Produces: Request→Dto/Criteria, Dto→VO conversions

**Reference:** `GrammarPointWrapper.java`

- [ ] **Step 1: Create CultureWrapper.java**

```java
package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.CultureCreateRequest;
import com.naon.grid.backend.rest.request.CultureQueryRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.CultureBaseVO;
import com.naon.grid.backend.rest.vo.CultureCreateVO;
import com.naon.grid.backend.rest.vo.CultureVO;
import com.naon.grid.backend.rest.vo.ExampleSentenceVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureKeywordDto;
import com.naon.grid.backend.service.culture.dto.CultureQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;
import com.alibaba.fastjson2.JSON;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CultureWrapper {

    public static CultureQueryCriteria toCriteria(CultureQueryRequest request) {
        CultureQueryCriteria criteria = new CultureQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        criteria.setLevel(request.getLevel());
        criteria.setProject(request.getProject());
        criteria.setCategory(request.getCategory());
        return criteria;
    }

    public static CultureDto toDto(CultureCreateRequest request) {
        CultureDto dto = new CultureDto();
        dto.setName(request.getName());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTranslations(serializeTranslations(request.getTranslations()));
        dto.setCoverImageId(request.getCoverImageId());
        dto.setLevel(request.getLevel());
        dto.setProject(request.getProject());
        dto.setCategory(request.getCategory());
        dto.setOneSentenceIntro(request.getOneSentenceIntro());
        dto.setOneSentenceIntroTranslations(serializeTranslations(request.getOneSentenceIntroTranslations()));
        dto.setOneSentenceIntroAudioId(request.getOneSentenceIntroAudioId());
        dto.setOneSentenceIntroImageId(request.getOneSentenceIntroImageId());
        dto.setDetailedIntro(request.getDetailedIntro());
        dto.setDetailedIntroTranslations(serializeTranslations(request.getDetailedIntroTranslations()));
        dto.setDetailedIntroAudioId(request.getDetailedIntroAudioId());
        dto.setDetailedIntroImageId(request.getDetailedIntroImageId());
        dto.setSentenceIds(request.getSentenceIds());
        dto.setQuestionIds(request.getQuestionIds());
        dto.setKeywords(toKeywordDtoList(request.getKeywords()));
        return dto;
    }

    private static String serializeTranslations(List<TextTranslationRequest> requests) {
        if (requests == null || requests.isEmpty()) return null;
        List<TextTranslation> list = requests.stream().map(r -> {
            TextTranslation t = new TextTranslation();
            t.setLanguage(r.getLanguage());
            t.setTranslation(r.getTranslation());
            return t;
        }).collect(Collectors.toList());
        return JSON.toJSONString(list);
    }

    private static List<CultureKeywordDto> toKeywordDtoList(
            List<CultureCreateRequest.CultureKeywordRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(r -> {
            CultureKeywordDto dto = new CultureKeywordDto();
            dto.setId(r.getId());
            dto.setKeyword(r.getKeyword());
            dto.setKeywordDescription(r.getKeywordDescription());
            dto.setKeywordTranslations(serializeTranslations(r.getKeywordTranslations()));
            dto.setKeywordDescriptionTranslations(serializeTranslations(r.getKeywordDescriptionTranslations()));
            dto.setAudioId(r.getAudioId());
            dto.setImageId(r.getImageId());
            dto.setOrder(r.getOrder() != null ? r.getOrder() : 0);
            dto.setAiGeneratedFields(r.getAiGeneratedFields());
            return dto;
        }).collect(Collectors.toList());
    }

    public static List<CultureBaseVO> toBaseVOList(List<CultureDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(CultureWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static CultureBaseVO toBaseVO(CultureDto dto) {
        CultureBaseVO vo = new CultureBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setCoverImageId(dto.getCoverImageId());
        vo.setLevel(dto.getLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setKeywordCount(dto.getKeywordCount());
        vo.setSentenceCount(dto.getSentenceCount());
        vo.setQuestionCount(dto.getQuestionCount());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setTranslations(toTextTranslationVOList(parseTranslations(dto.getTranslations())));
        return vo;
    }

    public static CultureVO toVO(CultureDto dto,
            Map<String, com.naon.grid.modules.system.service.AiContentMarkerService.MarkerFields> aiMarkers) {
        CultureVO vo = new CultureVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTranslations(toTextTranslationVOList(parseTranslations(dto.getTranslations())));
        vo.setCoverImageId(dto.getCoverImageId());
        vo.setLevel(dto.getLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setOneSentenceIntro(dto.getOneSentenceIntro());
        vo.setOneSentenceIntroTranslations(toTextTranslationVOList(parseTranslations(dto.getOneSentenceIntroTranslations())));
        vo.setOneSentenceIntroAudioId(dto.getOneSentenceIntroAudioId());
        vo.setOneSentenceIntroImageId(dto.getOneSentenceIntroImageId());
        vo.setDetailedIntro(dto.getDetailedIntro());
        vo.setDetailedIntroTranslations(toTextTranslationVOList(parseTranslations(dto.getDetailedIntroTranslations())));
        vo.setDetailedIntroAudioId(dto.getDetailedIntroAudioId());
        vo.setDetailedIntroImageId(dto.getDetailedIntroImageId());
        vo.setSentenceIds(dto.getSentenceIds());
        vo.setQuestionIds(dto.getQuestionIds());
        vo.setKeywords(toKeywordVOList(dto.getKeywords()));
        vo.setSentences(toSentenceVOList(dto.getSentences()));
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<CultureVO.CultureKeywordVO> toKeywordVOList(List<CultureKeywordDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(CultureWrapper::toKeywordVO).collect(Collectors.toList());
    }

    private static CultureVO.CultureKeywordVO toKeywordVO(CultureKeywordDto dto) {
        CultureVO.CultureKeywordVO vo = new CultureVO.CultureKeywordVO();
        vo.setId(dto.getId());
        vo.setKeyword(dto.getKeyword());
        vo.setKeywordDescription(dto.getKeywordDescription());
        vo.setKeywordTranslations(toTextTranslationVOList(parseTranslations(dto.getKeywordTranslations())));
        vo.setKeywordDescriptionTranslations(toTextTranslationVOList(parseTranslations(dto.getKeywordDescriptionTranslations())));
        vo.setAudioId(dto.getAudioId());
        vo.setImageId(dto.getImageId());
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setAiGeneratedFields(dto.getAiGeneratedFields() != null ? dto.getAiGeneratedFields() : Collections.emptyList());
        vo.setAiReviewedFields(Collections.emptyList());
        return vo;
    }

    private static List<ExampleSentenceVO> toSentenceVOList(List<ExampleSentenceDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(CultureWrapper::toSentenceVO).collect(Collectors.toList());
    }

    private static ExampleSentenceVO toSentenceVO(ExampleSentenceDto dto) {
        ExampleSentenceVO vo = new ExampleSentenceVO();
        vo.setId(dto.getId());
        vo.setSentence(dto.getSentence());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setImageId(dto.getImageId());
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<TextTranslation> parseTranslations(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        return JSON.parseArray(json, TextTranslation.class);
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> translations) {
        if (translations == null) return Collections.emptyList();
        return translations.stream().map(t -> {
            TextTranslationVO vo = new TextTranslationVO();
            vo.setLanguage(t.getLanguage());
            vo.setTranslation(t.getTranslation());
            return vo;
        }).collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd grid-system && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CultureWrapper.java
git commit -m "feat: add CultureWrapper (backend)"
```

---

### Task 10: CultureController (backend)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CultureController.java`

**Interfaces:**
- Consumes: `CultureService`, `AiContentMarkerService`
- Produces: 8 REST endpoints at `/api/culture`

**Reference:** `GrammarPointController.java`

- [ ] **Step 1: Create CultureController.java**

```java
package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.CultureCreateRequest;
import com.naon.grid.backend.rest.request.CultureQueryRequest;
import com.naon.grid.backend.rest.vo.CultureBaseVO;
import com.naon.grid.backend.rest.vo.CultureCreateVO;
import com.naon.grid.backend.rest.vo.CultureVO;
import com.naon.grid.backend.rest.wrapper.CultureWrapper;
import com.naon.grid.backend.service.culture.CultureService;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureKeywordDto;
import com.naon.grid.backend.service.culture.dto.CultureQueryCriteria;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：文化点管理")
@RequestMapping("/api/culture")
public class CultureController {

    private final CultureService cultureService;
    private final AiContentMarkerService aiContentMarkerService;

    @Log("新增文化点")
    @ApiOperation("新增文化点")
    @AnonymousPostMapping
    public ResponseEntity<CultureCreateVO> create(@Valid @RequestBody CultureCreateRequest request) {
        CultureCreateVO vo = new CultureCreateVO();
        vo.setId(cultureService.create(CultureWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("修改文化点内容")
    @ApiOperation("修改文化点内容")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody CultureCreateRequest request) {
        cultureService.update(id, CultureWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("文化点草稿审核通过")
    @ApiOperation("文化点草稿审核通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        cultureService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布文化点")
    @ApiOperation("发布文化点（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        cultureService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线文化点")
    @ApiOperation("下线文化点")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        cultureService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除文化点")
    @ApiOperation("删除文化点")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cultureService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询文化点详情")
    @ApiOperation("根据ID查询文化点详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<CultureVO> findById(@PathVariable Long id) {
        CultureDto dto = cultureService.findById(id);
        List<String> entityKeys = collectCultureEntityKeys(dto);
        Map<String, MarkerFields> aiMarkers = aiContentMarkerService.batchQuery(entityKeys);
        return new ResponseEntity<>(CultureWrapper.toVO(dto, aiMarkers), HttpStatus.OK);
    }

    private List<String> collectCultureEntityKeys(CultureDto dto) {
        List<String> keys = new ArrayList<>();
        if (dto.getKeywords() != null) {
            for (CultureKeywordDto kw : dto.getKeywords()) {
                keys.addAll(AiContentMarkerHelper.collectOne("culture_keyword", kw.getId()));
            }
        }
        if (dto.getSentences() != null) {
            for (var s : dto.getSentences()) {
                keys.addAll(AiContentMarkerHelper.collectOne("example_sentence", s.getId()));
            }
        }
        return keys;
    }

    @Log("查询文化点列表")
    @ApiOperation("分页查询文化点列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<CultureBaseVO>> queryAll(CultureQueryRequest request, Pageable pageable) {
        CultureQueryCriteria criteria = CultureWrapper.toCriteria(request);
        PageResult<CultureDto> pageResult = cultureService.queryAll(criteria, pageable);
        return new ResponseEntity<>(
                new PageResult<>(CultureWrapper.toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()),
                HttpStatus.OK);
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd grid-system && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/CultureController.java
git commit -m "feat: add CultureController with 8 backend endpoints"
```

---

### Task 11: APP-side files — Request and VOs

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppCultureSearchRequest.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCultureBaseVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCultureDetailVO.java`

**Interfaces:**
- Produces: `AppCultureSearchRequest` ({blurry}), `AppCultureBaseVO` (search result), `AppCultureDetailVO` (full detail with nested AudioVO/ImageVO)

**Reference:** `AppCharCharacterSearchRequest.java`, `AppCharCharacterBaseVO.java`, `AppCharCharacterDetailVO.java`

- [ ] **Step 1: Create AppCultureSearchRequest.java**

```java
package com.naon.grid.modules.app.rest.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppCultureSearchRequest {
    private String blurry;
}
```

- [ ] **Step 2: Create AppCultureBaseVO.java**

```java
package com.naon.grid.modules.app.rest.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppCultureBaseVO implements Serializable {
    private Long id;
    private String name;
    private String pinyin;
    private String level;
    private String project;
    private String category;
}
```

- [ ] **Step 3: Create AppCultureDetailVO.java**

```java
package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppCultureDetailVO implements Serializable {

    private Long id;
    private String name;
    private String pinyin;
    private AudioVO audio;
    private TextTranslationVO translation;
    private ImageVO coverImage;
    private String level;
    private String project;
    private String category;

    private String oneSentenceIntro;
    private TextTranslationVO oneSentenceIntroTranslation;
    private AudioVO oneSentenceIntroAudio;
    private ImageVO oneSentenceIntroImage;

    private String detailedIntro;
    private TextTranslationVO detailedIntroTranslation;
    private AudioVO detailedIntroAudio;
    private ImageVO detailedIntroImage;

    private List<CultureKeywordVO> keywords;
    private List<AppExampleSentenceVO> sentences;
    private List<AppExerciseQuestionDetailVO> questions;

    @Getter
    @Setter
    public static class CultureKeywordVO implements Serializable {
        private String keyword;
        private String keywordDescription;
        private TextTranslationVO translation;
        private TextTranslationVO descriptionTranslation;
        private AudioVO audio;
        private ImageVO image;
        private Integer order;
    }

    @Getter
    @Setter
    public static class AudioVO implements Serializable {
        private String audioUrl;
    }

    @Getter
    @Setter
    public static class ImageVO implements Serializable {
        private String imageUrl;
    }
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd grid-app && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 5: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppCultureSearchRequest.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCultureBaseVO.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCultureDetailVO.java
git commit -m "feat: add APP culture Request and VO classes"
```

---

### Task 12: AppCultureWrapper

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppCultureWrapper.java`

**Interfaces:**
- Consumes: `CultureDto`, `CultureKeywordDto`, `ExampleSentenceDto`, `AudioResourceDto`, `AliOssStorageDto`, `ExerciseQuestionDto`
- Produces: `AppCultureBaseVO`, `AppCultureDetailVO` with translation filtering and resource URL wrapping

**Reference:** `AppCharCharacterWrapper.java`

- [ ] **Step 1: Create AppCultureWrapper.java**

```java
package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureKeywordDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppCultureBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCultureDetailVO;
import com.naon.grid.modules.app.rest.vo.AppExampleSentenceVO;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AppCultureWrapper {

    public static List<AppCultureBaseVO> toBaseVOList(List<CultureDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(AppCultureWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static AppCultureBaseVO toBaseVO(CultureDto dto) {
        AppCultureBaseVO vo = new AppCultureBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setLevel(dto.getLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        return vo;
    }

    public static AppCultureDetailVO toDetailVO(
            CultureDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {

        AppCultureDetailVO vo = new AppCultureDetailVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(parseTranslations(dto.getTranslations()), language));
        vo.setLevel(dto.getLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());

        // 封面图
        if (dto.getCoverImageId() != null && imageMap != null) {
            AliOssStorageDto img = imageMap.get(dto.getCoverImageId());
            if (img != null) {
                AppCultureDetailVO.ImageVO imageVO = new AppCultureDetailVO.ImageVO();
                imageVO.setImageUrl(img.getFileUrl());
                vo.setCoverImage(imageVO);
            }
        }

        // 音频
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audio = audioMap.get(dto.getAudioId());
            if (audio != null) {
                AppCultureDetailVO.AudioVO audioVO = new AppCultureDetailVO.AudioVO();
                audioVO.setAudioUrl(audio.getFileUrl());
                vo.setAudio(audioVO);
            }
        }

        // 一句话介绍
        vo.setOneSentenceIntro(dto.getOneSentenceIntro());
        vo.setOneSentenceIntroTranslation(filterByLanguage(parseTranslations(dto.getOneSentenceIntroTranslations()), language));
        setAudio(vo::setOneSentenceIntroAudio, dto.getOneSentenceIntroAudioId(), audioMap);
        setImage(vo::setOneSentenceIntroImage, dto.getOneSentenceIntroImageId(), imageMap);

        // 详细介绍
        vo.setDetailedIntro(dto.getDetailedIntro());
        vo.setDetailedIntroTranslation(filterByLanguage(parseTranslations(dto.getDetailedIntroTranslations()), language));
        setAudio(vo::setDetailedIntroAudio, dto.getDetailedIntroAudioId(), audioMap);
        setImage(vo::setDetailedIntroImage, dto.getDetailedIntroImageId(), imageMap);

        // 关键词
        vo.setKeywords(toKeywordVOList(dto.getKeywords(), audioMap, imageMap, language));

        // 学一学例句
        vo.setSentences(toSentenceVOList(dto.getSentences(), audioMap, imageMap, language));

        // 练一练习题 — passed through as-is from ExerciseQuestionService (pre-resolved in controller)

        return vo;
    }

    // --- Keywords ---

    private static List<AppCultureDetailVO.CultureKeywordVO> toKeywordVOList(
            List<CultureKeywordDto> dtos,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getOrder() != null ? b.getOrder() : 0,
                        a.getOrder() != null ? a.getOrder() : 0))
                .map(dto -> toKeywordVO(dto, audioMap, imageMap, language))
                .collect(Collectors.toList());
    }

    private static AppCultureDetailVO.CultureKeywordVO toKeywordVO(
            CultureKeywordDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppCultureDetailVO.CultureKeywordVO vo = new AppCultureDetailVO.CultureKeywordVO();
        vo.setKeyword(dto.getKeyword());
        vo.setKeywordDescription(dto.getKeywordDescription());
        vo.setTranslation(filterByLanguage(parseTranslations(dto.getKeywordTranslations()), language));
        vo.setDescriptionTranslation(filterByLanguage(parseTranslations(dto.getKeywordDescriptionTranslations()), language));
        vo.setOrder(dto.getOrder());

        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audio = audioMap.get(dto.getAudioId());
            if (audio != null) {
                AppCultureDetailVO.AudioVO audioVO = new AppCultureDetailVO.AudioVO();
                audioVO.setAudioUrl(audio.getFileUrl());
                vo.setAudio(audioVO);
            }
        }
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto img = imageMap.get(dto.getImageId());
            if (img != null) {
                AppCultureDetailVO.ImageVO imageVO = new AppCultureDetailVO.ImageVO();
                imageVO.setImageUrl(img.getFileUrl());
                vo.setImage(imageVO);
            }
        }
        return vo;
    }

    // --- Example Sentences ---

    private static List<AppExampleSentenceVO> toSentenceVOList(
            List<ExampleSentenceDto> dtos,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(dto -> toSentenceVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppExampleSentenceVO toSentenceVO(
            ExampleSentenceDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppExampleSentenceVO vo = new AppExampleSentenceVO();
        vo.setSentence(dto.getSentence());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));
        vo.setOrder(dto.getOrder());

        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audio = audioMap.get(dto.getAudioId());
            if (audio != null) {
                AppCultureDetailVO.AudioVO audioVO = new AppCultureDetailVO.AudioVO();
                audioVO.setAudioUrl(audio.getFileUrl());
                vo.setAudio(audioVO);
            }
        }
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto img = imageMap.get(dto.getImageId());
            if (img != null) {
                AppCultureDetailVO.ImageVO imageVO = new AppCultureDetailVO.ImageVO();
                imageVO.setImageUrl(img.getFileUrl());
                vo.setImage(imageVO);
            }
        }
        return vo;
    }

    // --- Utilities ---

    @FunctionalInterface
    private interface AudioSetter {
        void set(AppCultureDetailVO.AudioVO audio);
    }

    @FunctionalInterface
    private interface ImageSetter {
        void set(AppCultureDetailVO.ImageVO image);
    }

    private static void setAudio(AudioSetter setter, Long audioId, Map<Long, AudioResourceDto> audioMap) {
        if (audioId != null && audioMap != null) {
            AudioResourceDto audio = audioMap.get(audioId);
            if (audio != null) {
                AppCultureDetailVO.AudioVO audioVO = new AppCultureDetailVO.AudioVO();
                audioVO.setAudioUrl(audio.getFileUrl());
                setter.set(audioVO);
            } else {
                log.error("音频资源未找到, audioId={}", audioId);
            }
        }
    }

    private static void setImage(ImageSetter setter, Long imageId, Map<Long, AliOssStorageDto> imageMap) {
        if (imageId != null && imageMap != null) {
            AliOssStorageDto img = imageMap.get(imageId);
            if (img != null) {
                AppCultureDetailVO.ImageVO imageVO = new AppCultureDetailVO.ImageVO();
                imageVO.setImageUrl(img.getFileUrl());
                setter.set(imageVO);
            } else {
                log.error("图片资源未找到, imageId={}", imageId);
            }
        }
    }

    private static List<TextTranslation> parseTranslations(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        return JSON.parseArray(json, TextTranslation.class);
    }

    private static TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) return null;
        return translations.stream()
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst()
                .map(t -> {
                    TextTranslationVO vo = new TextTranslationVO();
                    vo.setLanguage(t.getLanguage());
                    vo.setTranslation(t.getTranslation());
                    return vo;
                })
                .orElse(null);
    }
}
```

**Note:** The `AppExampleSentenceVO` needs an `audio` field (AudioVO) and `image` field (ImageVO). If the existing `AppExampleSentenceVO` does not have these, add them in this task:

```java
// Add to existing AppExampleSentenceVO:
private AppCultureDetailVO.AudioVO audio;
private AppCultureDetailVO.ImageVO image;
```

- [ ] **Step 2: Verify AppExampleSentenceVO has audio/image fields**

Check `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppExampleSentenceVO.java`. If it lacks `audio` (AudioVO) and `image` (ImageVO) fields, add them using the same `AppCultureDetailVO.AudioVO` and `AppCultureDetailVO.ImageVO` types.

- [ ] **Step 3: Verify compilation**

```bash
cd grid-app && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 4: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppCultureWrapper.java
git commit -m "feat: add AppCultureWrapper with translation filtering and resource URL wrapping"
```

---

### Task 13: AppCultureController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCultureController.java`

**Interfaces:**
- Consumes: `CultureService`, `AudioResourceService`, `AliOssStorageService`, `ExerciseQuestionService`
- Produces: 2 REST endpoints at `/api/app/culture`

**Reference:** `AppCharCharacterController.java`

- [ ] **Step 1: Create AppCultureController.java**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.culture.CultureService;
import com.naon.grid.backend.service.culture.dto.CultureDto;
import com.naon.grid.backend.service.culture.dto.CultureKeywordDto;
import com.naon.grid.backend.service.question.ExerciseQuestionService;
import com.naon.grid.backend.service.question.dto.ExerciseQuestionDto;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.modules.app.rest.request.AppCultureSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppCultureBaseVO;
import com.naon.grid.modules.app.rest.vo.AppCultureDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppCultureWrapper;
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

import static com.naon.grid.modules.app.rest.wrapper.AppExerciseQuestionWrapper.toDetailVOList;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/culture")
@Api(tags = "用户：文化点接口")
public class AppCultureController {

    private final CultureService cultureService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;
    private final ExerciseQuestionService exerciseQuestionService;

    @ApiOperation("搜索文化点（仅匹配名称字段）")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppCultureBaseVO>> search(AppCultureSearchRequest request) {
        List<CultureDto> dtos = cultureService.searchPublished(request.getBlurry());
        return new ResponseEntity<>(AppCultureWrapper.toBaseVOList(dtos), HttpStatus.OK);
    }

    @ApiOperation("根据ID查询文化点详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppCultureDetailVO> getDetail(
            @PathVariable Long id,
            @RequestParam String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        CultureDto dto = cultureService.findPublishedById(id);

        // 收集所有 audio IDs
        Set<Long> audioIds = new HashSet<>();
        if (dto.getAudioId() != null) audioIds.add(dto.getAudioId());
        if (dto.getOneSentenceIntroAudioId() != null) audioIds.add(dto.getOneSentenceIntroAudioId());
        if (dto.getDetailedIntroAudioId() != null) audioIds.add(dto.getDetailedIntroAudioId());
        if (dto.getKeywords() != null) {
            dto.getKeywords().stream()
                    .map(CultureKeywordDto::getAudioId)
                    .filter(Objects::nonNull)
                    .forEach(audioIds::add);
        }
        if (dto.getSentences() != null) {
            dto.getSentences().stream()
                    .map(ExampleSentenceDto::getAudioId)
                    .filter(Objects::nonNull)
                    .forEach(audioIds::add);
        }

        // 收集所有 image IDs
        Set<Long> imageIds = new HashSet<>();
        if (dto.getCoverImageId() != null) imageIds.add(dto.getCoverImageId());
        if (dto.getOneSentenceIntroImageId() != null) imageIds.add(dto.getOneSentenceIntroImageId());
        if (dto.getDetailedIntroImageId() != null) imageIds.add(dto.getDetailedIntroImageId());
        if (dto.getKeywords() != null) {
            dto.getKeywords().stream()
                    .map(CultureKeywordDto::getImageId)
                    .filter(Objects::nonNull)
                    .forEach(imageIds::add);
        }
        if (dto.getSentences() != null) {
            dto.getSentences().stream()
                    .map(ExampleSentenceDto::getImageId)
                    .filter(Objects::nonNull)
                    .forEach(imageIds::add);
        }

        // 批量查询资源
        Map<Long, AudioResourceDto> audioMap = new HashMap<>();
        if (!audioIds.isEmpty()) {
            try {
                audioMap = audioResourceService.findByIds(new ArrayList<>(audioIds)).stream()
                        .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
            } catch (Exception e) {
                log.error("批量查询音频资源失败", e);
            }
        }

        Map<Long, AliOssStorageDto> imageMap = new HashMap<>();
        if (!imageIds.isEmpty()) {
            try {
                imageMap = aliOssStorageService.findByIds(new ArrayList<>(imageIds)).stream()
                        .collect(Collectors.toMap(AliOssStorageDto::getId, img -> img, (a, b) -> a));
            } catch (Exception e) {
                log.error("批量查询图片资源失败", e);
            }
        }

        AppCultureDetailVO vo = AppCultureWrapper.toDetailVO(dto, audioMap, imageMap, language);

        AppCultureDetailVO vo = AppCultureWrapper.toDetailVO(dto, audioMap, imageMap, language);

        // 批量查询练一练习题并转换
        if (dto.getQuestionIds() != null && !dto.getQuestionIds().isEmpty()) {
            try {
                List<ExerciseQuestionDto> questionDtos = exerciseQuestionService.findPublishedByIds(dto.getQuestionIds());
                // 收集练习题中的音频和图片ID，追加到已有 maps
                for (ExerciseQuestionDto q : questionDtos) {
                    if (q.getAudioId() != null) audioIds.add(q.getAudioId());
                }
                if (!audioIds.isEmpty()) {
                    audioMap = audioResourceService.findByIds(new ArrayList<>(audioIds)).stream()
                            .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
                }
                if (!imageIds.isEmpty()) {
                    imageMap = aliOssStorageService.findByIds(new ArrayList<>(imageIds)).stream()
                            .collect(Collectors.toMap(AliOssStorageDto::getId, img -> img, (a, b) -> a));
                }
                vo.setQuestions(toDetailVOList(questionDtos, audioMap, imageMap));
            } catch (Exception e) {
                log.error("查询练习题失败", e);
            }
        }

        return new ResponseEntity<>(vo, HttpStatus.OK);
    }
}
```

**Note:** The exercise question conversion needs an existing wrapper. Check `AppExerciseQuestionWrapper` — if `toDetailVO` or similar exists, use it in the lambda above. Otherwise define a minimal conversion inline that maps the published ExerciseQuestionDto fields.

- [ ] **Step 2: Resolve exercise question conversion**

Check existing exercise question APP wrapper: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppExerciseQuestionWrapper.java`. Replace the `// TODO` in getDetail with the actual conversion call.

- [ ] **Step 3: Verify compilation**

```bash
cd grid-app && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 4: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCultureController.java
git commit -m "feat: add AppCultureController with search and detail endpoints"
```

---

### Task 14: Collection integration (modify 3 existing files)

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/enums/CollectionBizTypeEnum.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/CollectionWrapper.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/CollectionServiceImpl.java`

**Interfaces:**
- Consumes: `CultureService` (injected into CollectionWrapper callers and CollectionServiceImpl)

- [ ] **Step 1: Add CULTURE to CollectionBizTypeEnum**

In `CollectionBizTypeEnum.java`, add after `TOPIC`:

```java
    CULTURE("CULTURE", "文化");
```

- [ ] **Step 2: Add CULTURE case to CollectionWrapper.resolveContentName()**

In `CollectionWrapper.java`:

```java
import com.naon.grid.backend.service.culture.CultureService;
```

Add parameter to the method signatures that pass through services (`toDetailVO`, `toItemVOList`, `toItemVO`, `resolveContentName`):

Add `CultureService cultureService` as the last parameter to `resolveContentName()`, `toItemVO()`, `toItemVOList()`, and `toDetailVO()`.

In `resolveContentName()`, add before `default`:

```java
                case "CULTURE": {
                    CultureDto dto = cultureService.findById(item.getContentId());
                    return dto != null ? dto.getName() : null;
                }
```

Add the import:
```java
import com.naon.grid.backend.service.culture.dto.CultureDto;
```

- [ ] **Step 3: Add CULTURE case to CollectionServiceImpl.validateContentExists()**

In `CollectionServiceImpl.java`:

```java
import com.naon.grid.backend.service.culture.CultureService;
```

Inject the service:

```java
    private final CultureService cultureService;
```

In `validateContentExists()`, add after the TOPIC case:

```java
            case "CULTURE":
                cultureService.findPublishedById(contentId);
                break;
```

- [ ] **Step 4: Update AppCollectionController to pass CultureService**

In `AppCollectionController.java`, inject `CultureService`:

```java
    private final CultureService cultureService;
```

Update all calls to `CollectionWrapper.toDetailVO()` and related methods to pass `cultureService` as the new last parameter.

- [ ] **Step 5: Verify compilation**

```bash
cd grid-app && mvn compile -pl . -am -DskipTests -q
```

- [ ] **Step 6: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/enums/CollectionBizTypeEnum.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/CollectionWrapper.java \
        grid-app/src/main/java/com/naon/grid/modules/app/service/impl/CollectionServiceImpl.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCollectionController.java
git commit -m "feat: add CULTURE to collection biz type enum, wrapper, and service"
```

---
