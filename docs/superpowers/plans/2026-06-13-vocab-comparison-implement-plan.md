# 词汇辨析（Vocab Comparison）功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现词汇辨析功能的完整后台管理+用户端 API，支持草稿/审核/发布工作流

**Architecture:** 完全对齐现有 `VocabWord` 的草稿工作流模式。编辑时所有数据存于 `vocab_comparison_group.draft_content` JSON；发布时同步写入 `vocab_comparison_item`、`vocab_comparison_chat` 和 `example_sentence`（bizType=VOCAB_COMPARISON_CHAT）。后台 API 提供 CRUD + 审核发布，用户端 API 提供按词汇精确查询辨析组。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Lombok, MapStruct, Fastjson2, MySQL

**前置条件：** `vocab_comparison_group`、`vocab_comparison_item`、`vocab_comparison_chat` 三张表已在数据库中创建（SQL 见 `sql/biz_vocabulary_comparison.sql`）

---

## 文件结构总览

```
grid-system/src/main/java/com/naon/grid/
├── backend/
│   ├── domain/vocabcomparison/
│   │   ├── VocabComparisonGroup.java       [DO, 辨析组主表]
│   │   ├── VocabComparisonItem.java        [DO, 辨析条目]
│   │   └── VocabComparisonChat.java        [DO, 情景对话]
│   ├── repo/vocabcomparison/
│   │   ├── VocabComparisonGroupRepository.java
│   │   ├── VocabComparisonItemRepository.java
│   │   └── VocabComparisonChatRepository.java
│   ├── service/vocabcomparison/
│   │   ├── VocabComparisonGroupService.java             [接口]
│   │   ├── impl/VocabComparisonGroupServiceImpl.java    [实现]
│   │   └── dto/
│   │       ├── VocabComparisonGroupDto.java
│   │       ├── VocabComparisonItemDto.java
│   │       ├── VocabComparisonChatDto.java
│   │       └── VocabComparisonGroupQueryCriteria.java
│   └── rest/
│       ├── request/
│       │   ├── VocabComparisonGroupCreateRequest.java
│       │   └── VocabComparisonGroupQueryRequest.java
│       ├── vo/
│       │   ├── VocabComparisonGroupBaseVO.java
│       │   ├── VocabComparisonGroupVO.java
│       │   ├── VocabComparisonItemVO.java
│       │   └── VocabComparisonChatVO.java
│       ├── wrapper/
│       │   └── VocabComparisonGroupWrapper.java
│       └── controller/
│           └── VocabComparisonController.java

grid-common/src/main/java/com/naon/grid/enums/
└── SentenceBizTypeEnum.java                 [修改, 新增 VOCAB_COMPARISON_CHAT]

grid-app/src/main/java/com/naon/grid/modules/app/rest/
├── vo/
│   └── AppVocabComparisonGroupVO.java
└── AppVocabComparisonController.java
```

---

### Task 1: 新增 SentenceBizTypeEnum 枚举值

**Files:**
- Modify: `grid-common/src/main/java/com/naon/grid/enums/SentenceBizTypeEnum.java`

- [ ] **Step 1: 在枚举末尾新增 VOCAB_COMPARISON_CHAT**

在 `GRAMMAR_NOTICE_SENTENCE` 后面、分号之前插入：

```java
    VOCAB_COMPARISON_CHAT("VOCAB_COMPARISON_CHAT", "词汇辨析情景对话, bizId=词汇辨析对话ID"),
```

- [ ] **Step 2: 确认编译通过**

```bash
cd grid-common && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: 提交**

```bash
git add grid-common/src/main/java/com/naon/grid/enums/SentenceBizTypeEnum.java
git commit -m "feat: add VOCAB_COMPARISON_CHAT biz type for comparison chat sentences"
```

---

### Task 2: 创建三个 DO 实体

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabcomparison/VocabComparisonGroup.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabcomparison/VocabComparisonItem.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabcomparison/VocabComparisonChat.java`

之前已创建，确认文件内容正确即可：

- [ ] **Step 1: 确认 VocabComparisonGroup.java 内容**

关键点：
- `extends BaseEntity`（继承 createBy/updateBy/createTime/updateTime）
- `@Table(name = "vocab_comparison_group")`
- `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
- 字段：groupKey(String)、exerciseQuestionIds(String)、groupOrder(Integer)、draftContent(String)、status(Integer, 默认1)、publishStatus(String, 默认"unpublished")、editStatus(String, 默认"draft")

- [ ] **Step 2: 确认 VocabComparisonItem.java 内容**

关键点：
- 不继承 BaseEntity（子表只带时间戳）
- `@Table(name = "vocab_comparison_item")`
- 字段：id(Long)、groupId(Long)、wordId(Long)、word(String)、partOfSpeech(String)、usageComparison(String)、usageComparisonTranslations(String/JSON)、commonUsage(String)、commonUsageTranslations(String/JSON)、itemOrder(Integer)、createTime(Timestamp)、updateTime(Timestamp)、status(Integer, 默认1)
- `@Column(name = "\`order\`")` 因为 `order` 是 SQL 保留字

- [ ] **Step 3: 确认 VocabComparisonChat.java 内容**

关键点：
- 不继承 BaseEntity
- `@Table(name = "vocab_comparison_chat")`
- 字段：id(Long)、groupId(Long)、role(String)、content(String)、exampleSentenceId(Long)、chatOrder(Integer, `@Column(name = "\`order\`")`)、createTime、updateTime、status(Integer, 默认1)

- [ ] **Step 4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabcomparison/
git commit -m "feat: add VocabComparisonGroup/Item/Chat entities"
```

---

### Task 3: 创建三个 Repository

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabcomparison/VocabComparisonGroupRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabcomparison/VocabComparisonItemRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabcomparison/VocabComparisonChatRepository.java`

- [ ] **Step 1: 创建 VocabComparisonGroupRepository**

```java
package com.naon.grid.backend.repo.vocabcomparison;

import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabComparisonGroupRepository extends JpaRepository<VocabComparisonGroup, Long>,
        JpaSpecificationExecutor<VocabComparisonGroup> {
}
```

- [ ] **Step 2: 创建 VocabComparisonItemRepository**

```java
package com.naon.grid.backend.repo.vocabcomparison;

import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabComparisonItemRepository extends JpaRepository<VocabComparisonItem, Long>,
        JpaSpecificationExecutor<VocabComparisonItem> {

    List<VocabComparisonItem> findByGroupIdAndStatus(Long groupId, Integer status);

    List<VocabComparisonItem> findByGroupIdInAndStatus(List<Long> groupIds, Integer status);

    List<VocabComparisonItem> findByWordAndStatus(String word, Integer status);

    List<VocabComparisonItem> findByWordIdAndStatus(Long wordId, Integer status);
}
```

- [ ] **Step 3: 创建 VocabComparisonChatRepository**

```java
package com.naon.grid.backend.repo.vocabcomparison;

import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabComparisonChatRepository extends JpaRepository<VocabComparisonChat, Long>,
        JpaSpecificationExecutor<VocabComparisonChat> {

    List<VocabComparisonChat> findByGroupIdAndStatus(Long groupId, Integer status);

    List<VocabComparisonChat> findByGroupIdInAndStatus(List<Long> groupIds, Integer status);
}
```

- [ ] **Step 4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/vocabcomparison/
git commit -m "feat: add VocabComparison repositories"
```

---

### Task 4: 创建 DTO

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/dto/VocabComparisonGroupDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/dto/VocabComparisonItemDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/dto/VocabComparisonChatDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/dto/VocabComparisonGroupQueryCriteria.java`

- [ ] **Step 1: 创建 VocabComparisonGroupDto**

```java
package com.naon.grid.backend.service.vocabcomparison.dto;

import com.naon.grid.base.BaseDTO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class VocabComparisonGroupDto extends BaseDTO {

    @ApiModelProperty(value = "辨析组ID")
    private Long id;

    @ApiModelProperty(value = "辨析组标识")
    private String groupKey;

    @ApiModelProperty(value = "练习题ID列表JSON")
    private String exerciseQuestionIds;

    @ApiModelProperty(value = "组排序权重")
    private Integer groupOrder;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    @ApiModelProperty(value = "条目列表")
    private List<VocabComparisonItemDto> items;

    @ApiModelProperty(value = "情景对话列表")
    private List<VocabComparisonChatDto> chats;

    @ApiModelProperty(value = "条目数量（列表统计用）")
    private Integer itemCount;
}
```

- [ ] **Step 2: 创建 VocabComparisonItemDto**

```java
package com.naon.grid.backend.service.vocabcomparison.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class VocabComparisonItemDto {

    @ApiModelProperty(value = "条目ID（新增时为null）")
    private Long id;

    @ApiModelProperty(value = "词汇ID")
    private Long wordId;

    @ApiModelProperty(value = "词汇词头")
    private String word;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "用法对比")
    private String usageComparison;

    @ApiModelProperty(value = "用法对比外文翻译")
    private List<TextTranslation> usageComparisonTranslations;

    @ApiModelProperty(value = "通用用法")
    private String commonUsage;

    @ApiModelProperty(value = "通用用法外文翻译")
    private List<TextTranslation> commonUsageTranslations;

    @ApiModelProperty(value = "组内排序权重")
    private Integer order;
}
```

- [ ] **Step 3: 创建 VocabComparisonChatDto**

```java
package com.naon.grid.backend.service.vocabcomparison.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class VocabComparisonChatDto {

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
}
```

- [ ] **Step 4: 创建 VocabComparisonGroupQueryCriteria**

```java
package com.naon.grid.backend.service.vocabcomparison.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class VocabComparisonGroupQueryCriteria implements Serializable {

    @ApiModelProperty(value = "词汇文本（精确匹配）")
    private String word;

    @ApiModelProperty(value = "词汇ID（精确匹配）")
    private Long wordId;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;
}
```

- [ ] **Step 5: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/dto/
git commit -m "feat: add VocabComparison DTOs and query criteria"
```

---

### Task 5: 创建 Service 接口

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/VocabComparisonGroupService.java`

- [ ] **Step 1: 创建接口**

```java
package com.naon.grid.backend.service.vocabcomparison;

import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface VocabComparisonGroupService {

    PageResult<VocabComparisonGroupDto> queryAll(VocabComparisonGroupQueryCriteria criteria, Pageable pageable);

    VocabComparisonGroupDto findById(Long id);

    Long create(VocabComparisonGroupDto resources);

    void update(Long id, VocabComparisonGroupDto resources);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);

    List<VocabComparisonGroupDto> searchByWord(String word);

    List<VocabComparisonGroupDto> searchByWordId(Long wordId);
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/VocabComparisonGroupService.java
git commit -m "feat: add VocabComparisonGroupService interface"
```

---

### Task 6: 实现 Service（核心逻辑）

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/impl/VocabComparisonGroupServiceImpl.java`

这是最复杂的任务，需要实现完整的草稿工作流。

- [ ] **Step 1: 创建实现类骨架**

```java
package com.naon.grid.backend.service.vocabcomparison.impl;

import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonChat;
import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonGroup;
import com.naon.grid.backend.domain.vocabcomparison.VocabComparisonItem;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.backend.repo.vocabcomparison.VocabComparisonChatRepository;
import com.naon.grid.backend.repo.vocabcomparison.VocabComparisonGroupRepository;
import com.naon.grid.backend.repo.vocabcomparison.VocabComparisonItemRepository;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonChatDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupQueryCriteria;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonItemDto;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.SentenceBizTypeEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabComparisonGroupServiceImpl implements VocabComparisonGroupService {

    private final VocabComparisonGroupRepository groupRepository;
    private final VocabComparisonItemRepository itemRepository;
    private final VocabComparisonChatRepository chatRepository;
    private final ExampleSentenceService exampleSentenceService;
    private final ExampleSentenceRepository exampleSentenceRepository;

    private static final String COMPARISON_CHAT_BIZ = SentenceBizTypeEnum.VOCAB_COMPARISON_CHAT.getCode();
}
```

- [ ] **Step 2: 实现 queryAll — 分页查询 + 草稿覆盖 + word/wordId 筛选**

在类中添加：

```java
    @Override
    public PageResult<VocabComparisonGroupDto> queryAll(VocabComparisonGroupQueryCriteria criteria, Pageable pageable) {
        // 如果按 word/wordId 筛选，先从 item 表查出 groupId
        List<Long> filterGroupIds = null;
        if (criteria.getWord() != null && !criteria.getWord().isEmpty()) {
            List<VocabComparisonItem> items = itemRepository.findByWordAndStatus(
                    criteria.getWord(), StatusEnum.ENABLED.getCode());
            filterGroupIds = items.stream().map(VocabComparisonItem::getGroupId)
                    .distinct().collect(Collectors.toList());
            if (filterGroupIds.isEmpty()) {
                return PageResult.empty();
            }
        } else if (criteria.getWordId() != null) {
            List<VocabComparisonItem> items = itemRepository.findByWordIdAndStatus(
                    criteria.getWordId(), StatusEnum.ENABLED.getCode());
            filterGroupIds = items.stream().map(VocabComparisonItem::getGroupId)
                    .distinct().collect(Collectors.toList());
            if (filterGroupIds.isEmpty()) {
                return PageResult.empty();
            }
        }

        final List<Long> finalFilterGroupIds = filterGroupIds;

        Page<VocabComparisonGroup> page = groupRepository.findAll((Specification<VocabComparisonGroup>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), StatusEnum.ENABLED.getCode()));
            if (finalFilterGroupIds != null) {
                predicates.add(root.get("id").in(finalFilterGroupIds));
            }
            if (criteria.getPublishStatus() != null) {
                predicates.add(cb.equal(root.get("publishStatus"), criteria.getPublishStatus()));
            }
            if (criteria.getEditStatus() != null) {
                predicates.add(cb.equal(root.get("editStatus"), criteria.getEditStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        PageResult<VocabComparisonGroupDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        populateItemCounts(pageResult.getContent());
        return pageResult;
    }
```

- [ ] **Step 3: 实现 toDtoWithDraftOverlay + populateItemCounts**

```java
    private VocabComparisonGroupDto toDtoWithDraftOverlay(VocabComparisonGroup entity) {
        VocabComparisonGroupDto dto = toBaseDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

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
        if (draft.getGroupKey() != null)        dto.setGroupKey(draft.getGroupKey());
        if (draft.getGroupOrder() != null)      dto.setGroupOrder(draft.getGroupOrder());
    }

    private void populateItemCounts(List<VocabComparisonGroupDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;
        List<Long> ids = dtos.stream().map(VocabComparisonGroupDto::getId).collect(Collectors.toList());
        List<VocabComparisonItem> allItems = itemRepository.findByGroupIdInAndStatus(ids, StatusEnum.ENABLED.getCode());
        Map<Long, Long> countMap = allItems.stream()
                .collect(Collectors.groupingBy(VocabComparisonItem::getGroupId, Collectors.counting()));
        for (VocabComparisonGroupDto dto : dtos) {
            dto.setItemCount(countMap.getOrDefault(dto.getId(), 0L).intValue());
        }
    }
```

- [ ] **Step 4: 实现 findById — 详情查询**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VocabComparisonGroupDto findById(Long id) {
        VocabComparisonGroup group = groupRepository.findById(id).orElse(null);
        if (group == null || StatusEnum.DISABLED.getCode().equals(group.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }

        // 草稿/已审核状态：返回 draftContent 反序列化的完整 DTO
        if (EditStatusEnum.DRAFT.getCode().equals(group.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(group.getEditStatus())) {
            if (group.getDraftContent() == null) {
                throw new BadRequestException("草稿内容不存在");
            }
            VocabComparisonGroupDto dto;
            try {
                dto = JsonUtils.fromJson(group.getDraftContent(), VocabComparisonGroupDto.class);
            } catch (Exception e) {
                throw new BadRequestException("草稿数据解析失败");
            }
            if (dto == null) {
                throw new BadRequestException("草稿内容不存在");
            }
            dto.setId(group.getId());
            dto.setStatus(group.getStatus());
            dto.setPublishStatus(group.getPublishStatus());
            dto.setEditStatus(group.getEditStatus());
            dto.setCreateTime(group.getCreateTime());
            dto.setUpdateTime(group.getUpdateTime());
            dto.setCreateBy(group.getCreateBy());
            dto.setUpdateBy(group.getUpdateBy());
            return dto;
        }

        // 已发布状态：从正式表组装
        VocabComparisonGroupDto dto = toBaseDto(group);
        dto.setItems(loadItems(group.getId()));
        dto.setChats(loadChats(group.getId()));
        return dto;
    }

    private List<VocabComparisonItemDto> loadItems(Long groupId) {
        List<VocabComparisonItem> items = itemRepository.findByGroupIdAndStatus(groupId, StatusEnum.ENABLED.getCode());
        return items.stream().map(this::toItemDto).collect(Collectors.toList());
    }

    private VocabComparisonItemDto toItemDto(VocabComparisonItem entity) {
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

    private List<VocabComparisonChatDto> loadChats(Long groupId) {
        List<VocabComparisonChat> chats = chatRepository.findByGroupIdAndStatus(groupId, StatusEnum.ENABLED.getCode());
        if (chats.isEmpty()) return Collections.emptyList();

        // 批量加载 example_sentence
        List<Long> chatIds = chats.stream().map(VocabComparisonChat::getId).collect(Collectors.toList());
        Map<Long, com.naon.grid.backend.service.common.dto.ExampleSentenceDto> sentenceMap =
                exampleSentenceService.findByBizIds(COMPARISON_CHAT_BIZ, chatIds);

        List<VocabComparisonChatDto> dtos = new ArrayList<>();
        for (VocabComparisonChat chat : chats) {
            VocabComparisonChatDto dto = toChatDto(chat);
            com.naon.grid.backend.service.common.dto.ExampleSentenceDto sentence =
                    sentenceMap.get(chat.getExampleSentenceId());
            if (sentence != null) {
                dto.setContent(sentence.getSentence());
                dto.setPinyin(sentence.getPinyin());
                dto.setTranslations(JsonUtils.parseTranslationList(sentence.getTranslations()));
                dto.setAudioId(sentence.getAudioId());
            } else {
                dto.setContent(chat.getContent());
            }
            dtos.add(dto);
        }
        return dtos;
    }

    private VocabComparisonChatDto toChatDto(VocabComparisonChat entity) {
        VocabComparisonChatDto dto = new VocabComparisonChatDto();
        dto.setId(entity.getId());
        dto.setRole(entity.getRole());
        dto.setContent(entity.getContent());
        dto.setExampleSentenceId(entity.getExampleSentenceId());
        dto.setOrder(entity.getChatOrder());
        return dto;
    }
```

注意：`loadChats` 中优先从 `example_sentence` 读取 content/pinyin/translations/audioId（因为发布时这些数据写入了 example_sentence），fallback 到 chat.content。

- [ ] **Step 5: 实现 create — 新建（存草稿）**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(VocabComparisonGroupDto resources) {
        VocabComparisonGroup group = new VocabComparisonGroup();
        group.setStatus(StatusEnum.ENABLED.getCode());
        group.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        group.setEditStatus(EditStatusEnum.DRAFT.getCode());
        group.setGroupKey(resources.getGroupKey());
        group.setDraftContent(JsonUtils.toJson(resources));
        group = groupRepository.save(group);
        return group.getId();
    }
```

- [ ] **Step 6: 实现 update — 更新（回退到草稿）**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, VocabComparisonGroupDto resources) {
        VocabComparisonGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(group.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }

        // 如果已审核/已发布，回退到草稿
        if (EditStatusEnum.REVIEWED.getCode().equals(group.getEditStatus())
                || EditStatusEnum.PUBLISHED.getCode().equals(group.getEditStatus())) {
            group.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        group.setDraftContent(JsonUtils.toJson(resources));
        groupRepository.save(group);
    }
```

- [ ] **Step 7: 实现 delete — 软删除**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        VocabComparisonGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(group.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }
        group.setStatus(StatusEnum.DISABLED.getCode());
        groupRepository.save(group);
    }
```

- [ ] **Step 8: 实现 reviewDraft — 审核（draft → reviewed）**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        VocabComparisonGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(group.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }
        if (group.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.DRAFT.getCode().equals(group.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }
        group.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        groupRepository.save(group);
    }
```

- [ ] **Step 9: 实现 publishDraft — 发布（核心逻辑）**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id) {
        VocabComparisonGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(group.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }
        if (group.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.REVIEWED.getCode().equals(group.getEditStatus())) {
            throw new BadRequestException("仅已审核状态可发布");
        }

        // 解析草稿
        VocabComparisonGroupDto draftDto = JsonUtils.fromJson(group.getDraftContent(), VocabComparisonGroupDto.class);
        if (draftDto == null) {
            throw new BadRequestException("草稿数据解析失败");
        }

        // 更新主表
        group.setGroupKey(draftDto.getGroupKey());
        group.setGroupOrder(draftDto.getGroupOrder() != null ? draftDto.getGroupOrder() : 0);
        group.setExerciseQuestionIds(draftDto.getExerciseQuestionIds());

        // 同步子表
        syncItems(id, draftDto.getItems());
        syncChats(id, draftDto.getChats());

        // 更新状态
        group.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        group.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        group.setDraftContent(null);
        groupRepository.save(group);
    }

    private void syncItems(Long groupId, List<VocabComparisonItemDto> submitted) {
        // 软删除旧条目
        List<VocabComparisonItem> existing = itemRepository.findByGroupIdAndStatus(groupId, StatusEnum.ENABLED.getCode());
        for (VocabComparisonItem item : existing) {
            item.setStatus(StatusEnum.DISABLED.getCode());
            itemRepository.save(item);
        }

        if (submitted == null || submitted.isEmpty()) return;

        // 创建新条目
        for (VocabComparisonItemDto dto : submitted) {
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
            itemRepository.save(item);
        }
    }

    private void syncChats(Long groupId, List<VocabComparisonChatDto> submitted) {
        // 查出旧 chats
        List<VocabComparisonChat> existing = chatRepository.findByGroupIdAndStatus(groupId, StatusEnum.ENABLED.getCode());
        List<Long> existingIds = existing.stream().map(VocabComparisonChat::getId).collect(Collectors.toList());

        // 软删除旧 chats
        for (VocabComparisonChat chat : existing) {
            chat.setStatus(StatusEnum.DISABLED.getCode());
            chatRepository.save(chat);
        }

        // 软删除关联的 example_sentence
        if (!existingIds.isEmpty()) {
            exampleSentenceService.disableByBizIds(COMPARISON_CHAT_BIZ, existingIds);
        }

        if (submitted == null || submitted.isEmpty()) return;

        // 创建新 chats
        for (VocabComparisonChatDto dto : submitted) {
            VocabComparisonChat chat = new VocabComparisonChat();
            chat.setGroupId(groupId);
            chat.setRole(dto.getRole());
            chat.setContent(dto.getContent());
            chat.setChatOrder(dto.getOrder() != null ? dto.getOrder() : 0);
            chat.setStatus(StatusEnum.ENABLED.getCode());
            chat = chatRepository.save(chat);

            // 创建对应的 example_sentence
            com.naon.grid.backend.service.common.dto.ExampleSentenceDto sentenceDto =
                    new com.naon.grid.backend.service.common.dto.ExampleSentenceDto();
            sentenceDto.setSentence(dto.getContent());
            sentenceDto.setPinyin(dto.getPinyin());
            sentenceDto.setAudioId(dto.getAudioId());
            sentenceDto.setTranslations(dto.getTranslations());
            sentenceDto.setOrder(dto.getOrder() != null ? dto.getOrder() : 0);

            com.naon.grid.backend.service.common.dto.ExampleSentenceDto saved =
                    exampleSentenceService.syncOne(COMPARISON_CHAT_BIZ, chat.getId(), sentenceDto);

            // 回填 example_sentence_id
            chat.setExampleSentenceId(saved.getId());
            chatRepository.save(chat);
        }
    }
```

- [ ] **Step 10: 实现 offline + search 方法**

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        VocabComparisonGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(group.getStatus())) {
            throw new EntityNotFoundException(VocabComparisonGroup.class, "id", String.valueOf(id));
        }
        group.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        groupRepository.save(group);
    }

    @Override
    public List<VocabComparisonGroupDto> searchByWord(String word) {
        List<VocabComparisonItem> items = itemRepository.findByWordAndStatus(word, StatusEnum.ENABLED.getCode());
        return searchByItems(items);
    }

    @Override
    public List<VocabComparisonGroupDto> searchByWordId(Long wordId) {
        List<VocabComparisonItem> items = itemRepository.findByWordIdAndStatus(wordId, StatusEnum.ENABLED.getCode());
        return searchByItems(items);
    }

    private List<VocabComparisonGroupDto> searchByItems(List<VocabComparisonItem> items) {
        if (items.isEmpty()) return Collections.emptyList();

        Set<Long> groupIds = items.stream().map(VocabComparisonItem::getGroupId).collect(Collectors.toSet());
        List<VocabComparisonGroup> groups = groupRepository.findAllById(groupIds);

        // 只返回已发布的
        List<VocabComparisonGroup> published = groups.stream()
                .filter(g -> StatusEnum.ENABLED.getCode().equals(g.getStatus())
                        && PublishStatusEnum.PUBLISHED.getCode().equals(g.getPublishStatus()))
                .collect(Collectors.toList());

        List<VocabComparisonGroupDto> dtos = new ArrayList<>();
        for (VocabComparisonGroup group : published) {
            VocabComparisonGroupDto dto = toBaseDto(group);
            dto.setItems(loadItems(group.getId()));
            dtos.add(dto);
        }
        return dtos;
    }
```

- [ ] **Step 11: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/
git commit -m "feat: implement VocabComparisonGroupService with draft workflow"
```

---

### Task 7: 创建 Admin 端 Request / VO / Wrapper

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabComparisonGroupCreateRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabComparisonGroupQueryRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparisonGroupBaseVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparisonItemVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparisonChatVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparisonGroupVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabComparisonGroupWrapper.java`

- [ ] **Step 1: 创建 VocabComparisonGroupCreateRequest**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class VocabComparisonGroupCreateRequest {

    @NotBlank
    @ApiModelProperty(value = "辨析组标识", required = true)
    private String groupKey;

    @ApiModelProperty(value = "练习题ID列表JSON")
    private String exerciseQuestionIds;

    @ApiModelProperty(value = "组排序权重")
    private Integer groupOrder;

    @Valid
    @ApiModelProperty(value = "条目列表")
    private List<VocabItemRequest> items;

    @Valid
    @ApiModelProperty(value = "情景对话列表")
    private List<VocabChatRequest> chats;

    @Data
    public static class VocabItemRequest {
        @ApiModelProperty(value = "词汇ID")
        private Long wordId;

        @ApiModelProperty(value = "词汇词头")
        private String word;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "用法对比")
        private String usageComparison;

        @ApiModelProperty(value = "用法对比外文翻译")
        private List<TextTranslationRequest> usageComparisonTranslations;

        @ApiModelProperty(value = "通用用法")
        private String commonUsage;

        @ApiModelProperty(value = "通用用法外文翻译")
        private List<TextTranslationRequest> commonUsageTranslations;

        @ApiModelProperty(value = "组内排序权重")
        private Integer order;
    }

    @Data
    public static class VocabChatRequest {
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
    }
}
```

- [ ] **Step 2: 创建 VocabComparisonGroupQueryRequest**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class VocabComparisonGroupQueryRequest implements Serializable {

    @ApiModelProperty(value = "词汇文本（精确匹配）")
    private String word;

    @ApiModelProperty(value = "词汇ID（精确匹配）")
    private Long wordId;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;
}
```

- [ ] **Step 3: 创建 VocabComparisonGroupBaseVO（列表用）**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.sql.Timestamp;

@Data
public class VocabComparisonGroupBaseVO {

    @ApiModelProperty(value = "辨析组ID")
    private Long id;

    @ApiModelProperty(value = "辨析组标识")
    private String groupKey;

    @ApiModelProperty(value = "组排序权重")
    private Integer groupOrder;

    @ApiModelProperty(value = "条目数量")
    private Integer itemCount;

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

- [ ] **Step 4: 创建 VocabComparisonItemVO**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

@Data
public class VocabComparisonItemVO {

    @ApiModelProperty(value = "条目ID")
    private Long id;

    @ApiModelProperty(value = "词汇ID")
    private Long wordId;

    @ApiModelProperty(value = "词汇词头")
    private String word;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "用法对比")
    private String usageComparison;

    @ApiModelProperty(value = "用法对比外文翻译")
    private List<TextTranslationVO> usageComparisonTranslations;

    @ApiModelProperty(value = "通用用法")
    private String commonUsage;

    @ApiModelProperty(value = "通用用法外文翻译")
    private List<TextTranslationVO> commonUsageTranslations;

    @ApiModelProperty(value = "组内排序权重")
    private Integer order;
}
```

- [ ] **Step 5: 创建 VocabComparisonChatVO**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

@Data
public class VocabComparisonChatVO {

    @ApiModelProperty(value = "对话ID")
    private Long id;

    @ApiModelProperty(value = "角色")
    private String role;

    @ApiModelProperty(value = "中文对话内容")
    private String content;

    @ApiModelProperty(value = "对话例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "对话例句翻译")
    private List<TextTranslationVO> translations;

    @ApiModelProperty(value = "对话例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "组内排序权重")
    private Integer order;
}
```

- [ ] **Step 6: 创建 VocabComparisonGroupVO（详情用）**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

@Data
public class VocabComparisonGroupVO {

    @ApiModelProperty(value = "辨析组ID")
    private Long id;

    @ApiModelProperty(value = "辨析组标识")
    private String groupKey;

    @ApiModelProperty(value = "练习题ID列表JSON")
    private String exerciseQuestionIds;

    @ApiModelProperty(value = "组排序权重")
    private Integer groupOrder;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "条目列表")
    private List<VocabComparisonItemVO> items;

    @ApiModelProperty(value = "情景对话列表")
    private List<VocabComparisonChatVO> chats;

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

- [ ] **Step 7: 创建 VocabComparisonGroupWrapper**

```java
package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.request.VocabComparisonGroupCreateRequest;
import com.naon.grid.backend.rest.request.VocabComparisonGroupQueryRequest;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.vo.VocabComparisonChatVO;
import com.naon.grid.backend.rest.vo.VocabComparisonGroupBaseVO;
import com.naon.grid.backend.rest.vo.VocabComparisonGroupVO;
import com.naon.grid.backend.rest.vo.VocabComparisonItemVO;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonChatDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupQueryCriteria;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonItemDto;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VocabComparisonGroupWrapper {

    public static VocabComparisonGroupQueryCriteria toCriteria(VocabComparisonGroupQueryRequest request) {
        VocabComparisonGroupQueryCriteria criteria = new VocabComparisonGroupQueryCriteria();
        criteria.setWord(request.getWord());
        criteria.setWordId(request.getWordId());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    public static VocabComparisonGroupDto toDto(VocabComparisonGroupCreateRequest request) {
        VocabComparisonGroupDto dto = new VocabComparisonGroupDto();
        dto.setGroupKey(request.getGroupKey());
        dto.setExerciseQuestionIds(request.getExerciseQuestionIds());
        dto.setGroupOrder(request.getGroupOrder());
        dto.setItems(toItemDtoList(request.getItems()));
        dto.setChats(toChatDtoList(request.getChats()));
        return dto;
    }

    private static List<VocabComparisonItemDto> toItemDtoList(
            List<VocabComparisonGroupCreateRequest.VocabItemRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabComparisonGroupWrapper::toItemDto).collect(Collectors.toList());
    }

    private static VocabComparisonItemDto toItemDto(VocabComparisonGroupCreateRequest.VocabItemRequest req) {
        VocabComparisonItemDto dto = new VocabComparisonItemDto();
        dto.setWordId(req.getWordId());
        dto.setWord(req.getWord());
        dto.setPartOfSpeech(req.getPartOfSpeech());
        dto.setUsageComparison(req.getUsageComparison());
        dto.setUsageComparisonTranslations(toTextTranslationList(req.getUsageComparisonTranslations()));
        dto.setCommonUsage(req.getCommonUsage());
        dto.setCommonUsageTranslations(toTextTranslationList(req.getCommonUsageTranslations()));
        dto.setOrder(req.getOrder());
        return dto;
    }

    private static List<VocabComparisonChatDto> toChatDtoList(
            List<VocabComparisonGroupCreateRequest.VocabChatRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabComparisonGroupWrapper::toChatDto).collect(Collectors.toList());
    }

    private static VocabComparisonChatDto toChatDto(VocabComparisonGroupCreateRequest.VocabChatRequest req) {
        VocabComparisonChatDto dto = new VocabComparisonChatDto();
        dto.setRole(req.getRole());
        dto.setContent(req.getContent());
        dto.setPinyin(req.getPinyin());
        dto.setTranslations(toTextTranslationList(req.getTranslations()));
        dto.setAudioId(req.getAudioId());
        dto.setOrder(req.getOrder());
        return dto;
    }

    // === DTO → VO ===

    public static List<VocabComparisonGroupBaseVO> toBaseVOList(List<VocabComparisonGroupDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(VocabComparisonGroupWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static VocabComparisonGroupBaseVO toBaseVO(VocabComparisonGroupDto dto) {
        VocabComparisonGroupBaseVO vo = new VocabComparisonGroupBaseVO();
        vo.setId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setGroupOrder(dto.getGroupOrder());
        vo.setItemCount(dto.getItemCount() != null ? dto.getItemCount() : 0);
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static VocabComparisonGroupVO toVO(VocabComparisonGroupDto dto) {
        VocabComparisonGroupVO vo = new VocabComparisonGroupVO();
        vo.setId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setExerciseQuestionIds(dto.getExerciseQuestionIds());
        vo.setGroupOrder(dto.getGroupOrder());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setItems(toItemVOList(dto.getItems()));
        vo.setChats(toChatVOList(dto.getChats()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<VocabComparisonItemVO> toItemVOList(List<VocabComparisonItemDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(VocabComparisonGroupWrapper::toItemVO).collect(Collectors.toList());
    }

    private static VocabComparisonItemVO toItemVO(VocabComparisonItemDto dto) {
        VocabComparisonItemVO vo = new VocabComparisonItemVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setWord(dto.getWord());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setUsageComparison(dto.getUsageComparison());
        vo.setUsageComparisonTranslations(toTextTranslationVOList(dto.getUsageComparisonTranslations()));
        vo.setCommonUsage(dto.getCommonUsage());
        vo.setCommonUsageTranslations(toTextTranslationVOList(dto.getCommonUsageTranslations()));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    private static List<VocabComparisonChatVO> toChatVOList(List<VocabComparisonChatDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(VocabComparisonGroupWrapper::toChatVO).collect(Collectors.toList());
    }

    private static VocabComparisonChatVO toChatVO(VocabComparisonChatDto dto) {
        VocabComparisonChatVO vo = new VocabComparisonChatVO();
        vo.setId(dto.getId());
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setAudioId(dto.getAudioId());
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // === TextTranslation 转换工具 ===

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabComparisonGroupWrapper::toTextTranslation).collect(Collectors.toList());
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
        return list.stream().map(VocabComparisonGroupWrapper::toTextTranslationVO).collect(Collectors.toList());
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

- [ ] **Step 8: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabComparisonGroup*.java
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparison*.java
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabComparisonGroupWrapper.java
git commit -m "feat: add VocabComparison request/VO/wrapper classes"
```

---

### Task 8: 创建 Admin Controller

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabComparisonController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.VocabComparisonGroupCreateRequest;
import com.naon.grid.backend.rest.request.VocabComparisonGroupQueryRequest;
import com.naon.grid.backend.rest.vo.VocabComparisonGroupBaseVO;
import com.naon.grid.backend.rest.vo.VocabComparisonGroupVO;
import com.naon.grid.backend.rest.vo.VocabComparisonGroupCreateVO;
import com.naon.grid.backend.rest.wrapper.VocabComparisonGroupWrapper;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：词汇-词汇辨析")
@RequestMapping("/api/vocab/comparison")
public class VocabComparisonController {

    private final VocabComparisonGroupService vocabComparisonGroupService;

    @Log("新增辨析组")
    @ApiOperation("新增辨析组")
    @AnonymousPostMapping
    public ResponseEntity<VocabComparisonGroupCreateVO> create(@Valid @RequestBody VocabComparisonGroupCreateRequest request) {
        VocabComparisonGroupCreateVO vo = new VocabComparisonGroupCreateVO();
        vo.setId(vocabComparisonGroupService.create(VocabComparisonGroupWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("更新辨析组")
    @ApiOperation("更新辨析组")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody VocabComparisonGroupCreateRequest request) {
        vocabComparisonGroupService.update(id, VocabComparisonGroupWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("审核辨析组")
    @ApiOperation("辨析组草稿通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        vocabComparisonGroupService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布辨析组")
    @ApiOperation("发布辨析组（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        vocabComparisonGroupService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询辨析组详情")
    @ApiOperation("根据ID查询辨析组详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<VocabComparisonGroupVO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(
                VocabComparisonGroupWrapper.toVO(vocabComparisonGroupService.findById(id)), HttpStatus.OK);
    }

    @Log("查询辨析组列表")
    @ApiOperation("分页查询辨析组列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<VocabComparisonGroupBaseVO>> queryAll(
            VocabComparisonGroupQueryRequest request, Pageable pageable) {
        PageResult<VocabComparisonGroupDto> pageResult =
                vocabComparisonGroupService.queryAll(VocabComparisonGroupWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(VocabComparisonGroupWrapper.toBaseVOList(pageResult.getContent()),
                        pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除辨析组")
    @ApiOperation("删除辨析组")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vocabComparisonGroupService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线辨析组")
    @ApiOperation("下线辨析组")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        vocabComparisonGroupService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
```

- [ ] **Step 2: 创建 VocabComparisonGroupCreateVO（create 返回值）**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class VocabComparisonGroupCreateVO {
    @ApiModelProperty(value = "辨析组ID")
    private Long id;
}
```

- [ ] **Step 3: 编译检查**

```bash
cd grid-bootstrap && mvn compile -q 2>&1 | tail -30
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabComparisonController.java
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparisonGroupCreateVO.java
git commit -m "feat: add VocabComparisonController (admin CRUD)"
```

---

### Task 9: 创建 App 端 VO + Controller

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabComparisonGroupVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabComparisonController.java`

- [ ] **Step 1: 创建 AppVocabComparisonGroupVO**

```java
package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

@Data
public class AppVocabComparisonGroupVO {

    @ApiModelProperty(value = "辨析组ID")
    private Long groupId;

    @ApiModelProperty(value = "辨析组标识")
    private String groupKey;

    @ApiModelProperty(value = "条目列表")
    private List<AppItemVO> items;

    @ApiModelProperty(value = "情景对话列表")
    private List<AppChatVO> chats;

    @Data
    public static class AppItemVO {
        @ApiModelProperty(value = "词汇ID")
        private Long wordId;

        @ApiModelProperty(value = "词汇词头")
        private String word;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "用法对比")
        private String usageComparison;

        @ApiModelProperty(value = "用法对比外文翻译")
        private List<TextTranslationVO> usageComparisonTranslations;

        @ApiModelProperty(value = "通用用法")
        private String commonUsage;

        @ApiModelProperty(value = "通用用法外文翻译")
        private List<TextTranslationVO> commonUsageTranslations;

        @ApiModelProperty(value = "组内排序权重")
        private Integer order;
    }

    @Data
    public static class AppChatVO {
        @ApiModelProperty(value = "角色")
        private String role;

        @ApiModelProperty(value = "中文对话内容")
        private String content;

        @ApiModelProperty(value = "拼音")
        private String pinyin;

        @ApiModelProperty(value = "翻译")
        private List<TextTranslationVO> translations;

        @ApiModelProperty(value = "音频URL")
        private String audioUrl;

        @ApiModelProperty(value = "组内排序权重")
        private Integer order;
    }
}
```

- [ ] **Step 2: 创建 AppVocabComparisonController**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonChatDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonItemDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppVocabComparisonGroupVO;
import com.naon.grid.modules.app.rest.vo.AppVocabComparisonGroupVO.AppChatVO;
import com.naon.grid.modules.app.rest.vo.AppVocabComparisonGroupVO.AppItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/vocab/comparison")
@Api(tags = "用户：词汇辨析")
public class AppVocabComparisonController {

    private final VocabComparisonGroupService vocabComparisonGroupService;
    private final AudioResourceService audioResourceService;

    @ApiOperation("根据词汇查询辨析组列表")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppVocabComparisonGroupVO>> search(@RequestParam String word) {
        List<VocabComparisonGroupDto> dtos = vocabComparisonGroupService.searchByWord(word);
        List<AppVocabComparisonGroupVO> vos = toAppVOList(dtos);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("根据辨析组ID查询详情")
    @AnonymousGetMapping("/{groupId}")
    public ResponseEntity<AppVocabComparisonGroupVO> getDetail(@PathVariable Long groupId) {
        VocabComparisonGroupDto dto = vocabComparisonGroupService.findById(groupId);

        // 只返回已发布的数据
        if (!"published".equals(dto.getPublishStatus())) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        AppVocabComparisonGroupVO vo = toAppVO(dto);

        // 批量加载音频（通过 chat 列表索引一一对应）
        if (dto.getChats() != null) {
            List<Long> audioIds = dto.getChats().stream()
                    .map(VocabComparisonChatDto::getAudioId)
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            if (!audioIds.isEmpty()) {
                Map<Long, AudioResourceDto> audioMap = audioResourceService.findByIds(audioIds).stream()
                        .collect(Collectors.toMap(AudioResourceDto::getId, a -> a));
                List<AppChatVO> chatVos = vo.getChats();
                List<VocabComparisonChatDto> chatDtos = dto.getChats();
                for (int i = 0; i < chatDtos.size() && i < chatVos.size(); i++) {
                    Long audioId = chatDtos.get(i).getAudioId();
                    if (audioId != null && audioMap.containsKey(audioId)) {
                        chatVos.get(i).setAudioUrl(audioMap.get(audioId).getFileUrl());
                    }
                }
            }
        }

        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    private List<AppVocabComparisonGroupVO> toAppVOList(List<VocabComparisonGroupDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(this::toAppVO).collect(Collectors.toList());
    }

    private AppVocabComparisonGroupVO toAppVO(VocabComparisonGroupDto dto) {
        AppVocabComparisonGroupVO vo = new AppVocabComparisonGroupVO();
        vo.setGroupId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setItems(toAppItemVOList(dto.getItems()));
        vo.setChats(toAppChatVOList(dto.getChats()));
        return vo;
    }

    private List<AppItemVO> toAppItemVOList(List<VocabComparisonItemDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(this::toAppItemVO).collect(Collectors.toList());
    }

    private AppItemVO toAppItemVO(VocabComparisonItemDto dto) {
        AppItemVO vo = new AppItemVO();
        vo.setWordId(dto.getWordId());
        vo.setWord(dto.getWord());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setUsageComparison(dto.getUsageComparison());
        vo.setUsageComparisonTranslations(toTextTranslationVOList(dto.getUsageComparisonTranslations()));
        vo.setCommonUsage(dto.getCommonUsage());
        vo.setCommonUsageTranslations(toTextTranslationVOList(dto.getCommonUsageTranslations()));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    private List<AppChatVO> toAppChatVOList(List<VocabComparisonChatDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(this::toAppChatVO).collect(Collectors.toList());
    }

    private AppChatVO toAppChatVO(VocabComparisonChatDto dto) {
        AppChatVO vo = new AppChatVO();
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    private List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> translations) {
        if (translations == null) return Collections.emptyList();
        return translations.stream().map(this::toTextTranslationVO).collect(Collectors.toList());
    }

    private TextTranslationVO toTextTranslationVO(TextTranslation translation) {
        if (translation == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabComparisonGroupVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabComparisonController.java
git commit -m "feat: add AppVocabComparisonController for user-facing comparison API"
```

---

### Task 10: 编译验证

- [ ] **Step 1: 全量编译**

```bash
cd grid-bootstrap && mvn compile 2>&1 | tail -30
```

Expected: `BUILD SUCCESS`

如果报错，逐条修复。

- [ ] **Step 2: 提交所有变更**

```bash
git add -A && git status
git commit -m "feat: complete vocab comparison implementation"
```

---

### Task 11: 自检清单

- [ ] 1. `VocabComparisonGroupController` 的 8 个接口路由与 spec 一致
- [ ] 2. 创建时 `status=1`, `publishStatus=unpublished`, `editStatus=draft`
- [ ] 3. 更新后 `editStatus` 回退到 `draft`
- [ ] 4. 审核时仅 draft 可审；发布时仅 reviewed 可发布
- [ ] 5. 发布后 `draftContent` 清空，子表写入
- [ ] 6. 列表查询支持 `word` 精确匹配、`wordId` 精确匹配
- [ ] 7. 软删除改 `status=0` 不删数据
- [ ] 8. 下线只改 `publishStatus=unpublished`
- [ ] 9. App 端 `search` 只返回已发布组；`detail` 对非已发布组返回 404
- [ ] 10. Chat 的 example_sentence 使用 `SentenceBizTypeEnum.VOCAB_COMPARISON_CHAT`
