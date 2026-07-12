# AI 内容标记 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为所有后台管理模块实现 AI 内容来源标记，使 AI 生成的内容在后台详情页可被标识和验收。

**Architecture:** 新增独立的 `ai_content_marker` 表集中存储所有 AI 标记字段。标记随业务 Request 一起提交（嵌入式 `aiGeneratedFields` 字段），查询时通过 Wrapper 批量注入到 VO。草稿实体标记随 draft_content 流转，发布时物化到标记表；无草稿实体直接写入标记表。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MySQL, Lombok, Fastjson2

## Global Constraints

- Java 1.8，Lombok `@Getter/@Setter`，不使用 `@Data`
- Request 内嵌类使用 `implements Serializable`
- Wrapper 为 `public static` 纯映射方法，Controller 负责编排
- 字段名使用 Java 驼峰命名（与 Entity/Request/VO 的 Java 字段名一致）
- `ai_content_marker` 只存 AI 生成字段（`ai_generated=1`），不存即为人工
- 更新语义：每个实体全量替换标记（DELETE + INSERT）

---

### Task 1: 基础设施 — 数据库表、JPA 实体、Repository、Service

**Files:**
- Create: `sql/biz_common.sql` (追加)
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/domain/AiContentMarker.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/repository/AiContentMarkerRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/service/AiContentMarkerService.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/service/impl/AiContentMarkerServiceImpl.java`

**Interfaces:**
- Produces: `AiContentMarkerService.replaceFields(entityType, entityId, aiFields)` — 全量替换单个实体标记
- Produces: `AiContentMarkerService.batchQuery(entityKeys)` — 批量查询，返回 `Map<String, List<String>>`
- Produces: `AiContentMarkerService.batchReplace(entries)` — 批量替换

- [ ] **Step 1: 在 biz_common.sql 末尾追加建表 DDL**

```sql
-- AI内容来源标记表
CREATE TABLE `ai_content_marker` (
    `id`              BIGINT AUTO_INCREMENT,
    `entity_type`     VARCHAR(50)  NOT NULL COMMENT '实体表名',
    `entity_id`       BIGINT       NOT NULL COMMENT '实体记录ID',
    `field_name`      VARCHAR(255) NOT NULL COMMENT 'Java字段名(驼峰)',
    `ai_generated`    TINYINT      NOT NULL DEFAULT 1 COMMENT '1=AI生成 0=人工',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_entity_field` (`entity_type`, `entity_id`, `field_name`),
    KEY `idx_entity` (`entity_type`, `entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI内容来源标记表';
```

- [ ] **Step 2: 执行建表 SQL**

```bash
# 在 MySQL 中执行 sql/biz_common.sql 末尾新增的 DDL
```

- [ ] **Step 3: 创建 JPA 实体 AiContentMarker**

```java
package com.naon.grid.modules.system.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "ai_content_marker")
public class AiContentMarker implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    @ApiModelProperty(value = "实体表名")
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    @ApiModelProperty(value = "实体记录ID")
    private Long entityId;

    @Column(name = "field_name", nullable = false, length = 255)
    @ApiModelProperty(value = "Java字段名(驼峰)")
    private String fieldName;

    @Column(name = "ai_generated", nullable = false)
    @ApiModelProperty(value = "1=AI生成 0=人工")
    private Integer aiGenerated = 1;
}
```

- [ ] **Step 4: 创建 Repository**

```java
package com.naon.grid.modules.system.repository;

import com.naon.grid.modules.system.domain.AiContentMarker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AiContentMarkerRepository extends JpaRepository<AiContentMarker, Long> {

    List<AiContentMarker> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Modifying
    @Query("DELETE FROM AiContentMarker m WHERE m.entityType = :entityType AND m.entityId = :entityId")
    void deleteByEntityTypeAndEntityId(@Param("entityType") String entityType,
                                       @Param("entityId") Long entityId);

    @Query("SELECT m FROM AiContentMarker m " +
           "WHERE CONCAT(m.entityType, ':', CAST(m.entityId AS string)) IN :keys")
    List<AiContentMarker> findByEntityKeys(@Param("keys") List<String> keys);
}
```

- [ ] **Step 5: 创建 Service 接口**

```java
package com.naon.grid.modules.system.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

public interface AiContentMarkerService {

    /**
     * 全量替换指定实体的 AI 标记字段。
     */
    void replaceFields(String entityType, Long entityId, List<String> aiFields);

    /**
     * 批量替换。
     */
    void batchReplace(List<MarkerEntry> entries);

    /**
     * 批量查询 AI 标记。
     * @param entityKeys 格式 ["vocab_sense:88", "example_sentence:42"]
     * @return key="entity_type:entity_id", value=AI生成的字段名列表
     */
    Map<String, List<String>> batchQuery(List<String> entityKeys);

    @Data
    @AllArgsConstructor
    class MarkerEntry {
        private String entityType;
        private Long entityId;
        private List<String> aiFields;
    }
}
```

- [ ] **Step 6: 创建 Service 实现**

```java
package com.naon.grid.modules.system.service.impl;

import com.naon.grid.modules.system.domain.AiContentMarker;
import com.naon.grid.modules.system.repository.AiContentMarkerRepository;
import com.naon.grid.modules.system.service.AiContentMarkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiContentMarkerServiceImpl implements AiContentMarkerService {

    private final AiContentMarkerRepository repository;

    @Override
    @Transactional
    public void replaceFields(String entityType, Long entityId, List<String> aiFields) {
        repository.deleteByEntityTypeAndEntityId(entityType, entityId);
        if (aiFields != null && !aiFields.isEmpty()) {
            List<AiContentMarker> markers = aiFields.stream().map(field -> {
                AiContentMarker m = new AiContentMarker();
                m.setEntityType(entityType);
                m.setEntityId(entityId);
                m.setFieldName(field);
                m.setAiGenerated(1);
                return m;
            }).collect(Collectors.toList());
            repository.saveAll(markers);
        }
    }

    @Override
    @Transactional
    public void batchReplace(List<MarkerEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        for (MarkerEntry entry : entries) {
            repository.deleteByEntityTypeAndEntityId(
                    entry.getEntityType(), entry.getEntityId());
        }
        List<AiContentMarker> all = entries.stream()
                .filter(e -> e.getAiFields() != null && !e.getAiFields().isEmpty())
                .flatMap(e -> e.getAiFields().stream().map(field -> {
                    AiContentMarker m = new AiContentMarker();
                    m.setEntityType(e.getEntityType());
                    m.setEntityId(e.getEntityId());
                    m.setFieldName(field);
                    m.setAiGenerated(1);
                    return m;
                })).collect(Collectors.toList());
        if (!all.isEmpty()) {
            repository.saveAll(all);
        }
    }

    @Override
    public Map<String, List<String>> batchQuery(List<String> entityKeys) {
        if (entityKeys == null || entityKeys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AiContentMarker> markers = repository.findByEntityKeys(entityKeys);
        return markers.stream().collect(Collectors.groupingBy(
                m -> m.getEntityType() + ":" + m.getEntityId(),
                Collectors.mapping(AiContentMarker::getFieldName, Collectors.toList())
        ));
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add sql/biz_common.sql \
        grid-system/src/main/java/com/naon/grid/modules/system/domain/AiContentMarker.java \
        grid-system/src/main/java/com/naon/grid/modules/system/repository/AiContentMarkerRepository.java \
        grid-system/src/main/java/com/naon/grid/modules/system/service/AiContentMarkerService.java \
        grid-system/src/main/java/com/naon/grid/modules/system/service/impl/AiContentMarkerServiceImpl.java
git commit -m "feat: add ai_content_marker table, entity, repository and service

Foundation for AI content source tracking across all backend modules.

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: 通用组件 — EntityKeyCollector 辅助工具

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/service/AiContentMarkerHelper.java`

**Interfaces:**
- Produces: `AiContentMarkerHelper.collect(vocabSenseId, vocabStructureIds, exampleSentenceIds)` — 收集实体键列表
- Produces: `AiContentMarkerHelper.key(entityType, entityId)` — 构建单个实体键

- [ ] **Step 1: 创建 AiContentMarkerHelper**

```java
package com.naon.grid.modules.system.service;

import java.util.*;

/**
 * AI 标记辅助工具 — 构建 entity_key 和收集实体 ID。
 */
public class AiContentMarkerHelper {

    private AiContentMarkerHelper() {}

    /** 构建 entity_key: "entity_type:entity_id" */
    public static String key(String entityType, Object entityId) {
        if (entityId == null) return null;
        return entityType + ":" + entityId;
    }

    /** 收集 entity_key 列表 */
    public static List<String> collect(String entityType, Collection<?> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) return Collections.emptyList();
        List<String> keys = new ArrayList<>();
        for (Object id : entityIds) {
            if (id != null) keys.add(key(entityType, id));
        }
        return keys;
    }

    /** 收集单个 entity_key */
    public static List<String> collectOne(String entityType, Object entityId) {
        String key = key(entityType, entityId);
        return key == null ? Collections.emptyList() : Collections.singletonList(key);
    }

    /** 合并多个列表 */
    @SafeVarargs
    public static List<String> merge(List<String>... lists) {
        List<String> result = new ArrayList<>();
        for (List<String> list : lists) {
            if (list != null) result.addAll(list);
        }
        return result;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/service/AiContentMarkerHelper.java
git commit -m "feat: add AiContentMarkerHelper utility for entity key collection

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: 例句模块 — ExampleSentenceRequest/VO 加 aiGeneratedFields

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/ExampleSentenceRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExampleSentenceVO.java`

**Interfaces:**
- Consumes: nothing (独立变更)
- Produces: `ExampleSentenceRequest.aiGeneratedFields`, `ExampleSentenceVO.aiGeneratedFields`

- [ ] **Step 1: ExampleSentenceRequest 追加 aiGeneratedFields**

在 `ExampleSentenceRequest.java` 末尾（`private Integer order;` 之后）追加：

```java
    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;
```

- [ ] **Step 2: ExampleSentenceVO 追加 aiGeneratedFields**

在 `ExampleSentenceVO.java` 末尾（`private Timestamp updateTime;` 之后）追加：

```java
    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/ExampleSentenceRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExampleSentenceVO.java
git commit -m "feat: add aiGeneratedFields to ExampleSentenceRequest and ExampleSentenceVO

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: 词汇模块 — Request/VO 加 aiGeneratedFields

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java`

- [ ] **Step 1: VocabSenseRequest 追加 aiGeneratedFields**

在 `VocabWordCreateRequest.VocabSenseRequest` 的 `private Integer order;` 之前追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
```

- [ ] **Step 2: VocabStructureRequest 追加 aiGeneratedFields**

在 `VocabWordCreateRequest.VocabStructureRequest` 的 `private Integer order;` 之前追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
```

- [ ] **Step 3: VocabSenseVO 追加 aiGeneratedFields**

在 `VocabWordVO.VocabSenseVO` 的 `private Timestamp updateTime;` 之前追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表")
        private List<String> aiGeneratedFields;
```

- [ ] **Step 4: VocabStructureVO 追加 aiGeneratedFields**

在 `VocabWordVO.VocabStructureVO` 的 `private Timestamp updateTime;` 之前追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表")
        private List<String> aiGeneratedFields;
```

- [ ] **Step 5: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java
git commit -m "feat: add aiGeneratedFields to VocabWord Request/VO inner classes

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: 语法模块 — Request/VO 加 aiGeneratedFields

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/GrammarPointCreateRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarPointVO.java`

- [ ] **Step 1: GrammarMeaningRequest、GrammarStructureRequest、GrammarNoticeRequest、GrammarErrorRequest 各追加 aiGeneratedFields**

在 `GrammarPointCreateRequest` 的四个内嵌类中，各在 `private Integer order;` 之前追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
```

- [ ] **Step 2: GrammarMeaningVO、GrammarStructureVO、GrammarNoticeVO、GrammarErrorVO 各追加 aiGeneratedFields**

在 `GrammarPointVO` 的四个内嵌类中，各在 `private Timestamp updateTime;` 之前追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表")
        private List<String> aiGeneratedFields;
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/GrammarPointCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarPointVO.java
git commit -m "feat: add aiGeneratedFields to GrammarPoint Request/VO inner classes

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: 每日一语模块 — Request/VO 加 aiGeneratedFields

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/DailyVocabularyCreateRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/DailyVocabularyVO.java`

- [ ] **Step 1: DailyVocabularyCreateRequest 追加 aiGeneratedFields**

在 `DailyVocabularyCreateRequest` 末尾（`private Long relatedWordId;` 之后）追加：

```java
    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;
```

- [ ] **Step 2: DailyVocabularyVO 追加 aiGeneratedFields**

在 `DailyVocabularyVO` 末尾（`private Timestamp updateTime;` 之后）追加：

```java
    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/DailyVocabularyCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/DailyVocabularyVO.java
git commit -m "feat: add aiGeneratedFields to DailyVocabulary Request and VO

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 7: 汉字模块 — Request/VO 加 aiGeneratedFields

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`

- [ ] **Step 1: CharCharacterCreateRequest 本身及 CharComparisonRequest、CharWordRequest 追加 aiGeneratedFields**

`CharCharacterCreateRequest` 末尾（`private String createBy;` 之前）追加：

```java
    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;
```

`CharComparisonRequest` 的 `private int order;` 之前追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
```

`CharWordRequest` 的 `private Integer order;` 之前追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
```

- [ ] **Step 2: CharCharacterVO 的子 VO 追加 aiGeneratedFields**

`CharComparisonVO` 的 `private Timestamp updateTime;` 之前追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表")
        private List<String> aiGeneratedFields;
```

`CharWordVO` 的 `private Timestamp updateTime;` 之前追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表")
        private List<String> aiGeneratedFields;
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java
git commit -m "feat: add aiGeneratedFields to CharCharacter Request/VO inner classes

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 8: 词汇辨析/语法辨析/练习题 — Request/VO 加 aiGeneratedFields

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabComparisonGroupCreateRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparisonItemVO.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparisonChatVO.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/GrammarComparisonGroupCreateRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarComparisonItemVO.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarComparisonChatVO.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionCreateRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionVO.java`

- [ ] **Step 1: VocabComparisonGroupCreateRequest 的内嵌类 VocabItemRequest、VocabChatRequest 各追加 aiGeneratedFields**

每个内嵌类末尾追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
```

- [ ] **Step 2: VocabComparisonItemVO、VocabComparisonChatVO 各追加 aiGeneratedFields**

每个类末尾追加：

```java
    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;
```

- [ ] **Step 3: GrammarComparisonGroupCreateRequest 的内嵌类 GrammarItemRequest、GrammarChatRequest 各追加 aiGeneratedFields**

每个内嵌类末尾追加：

```java
        @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
        private List<String> aiGeneratedFields;
```

- [ ] **Step 4: GrammarComparisonItemVO、GrammarComparisonChatVO 各追加 aiGeneratedFields**

每个类末尾追加：

```java
    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;
```

- [ ] **Step 5: ExerciseQuestionCreateRequest 追加 aiGeneratedFields**

在 `ExerciseQuestionCreateRequest` 末尾（`private List<ExerciseQuestionCreateRequest> children;` 之后）追加：

```java
    @ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
    private List<String> aiGeneratedFields;
```

- [ ] **Step 6: ExerciseQuestionVO 追加 aiGeneratedFields**

在 `ExerciseQuestionVO` 末尾（`private List<ExerciseQuestionVO> children;` 之后）追加：

```java
    @ApiModelProperty(value = "AI生成的字段名列表")
    private List<String> aiGeneratedFields;
```

- [ ] **Step 7: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabComparisonGroupCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparisonItemVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparisonChatVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/request/GrammarComparisonGroupCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarComparisonItemVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarComparisonChatVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionVO.java
git commit -m "feat: add aiGeneratedFields to comparison and exercise Request/VO classes

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 9: 词汇模块 — Wrapper 注入 AI 标记到 VO

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java`

**Interfaces:**
- Consumes: `AiContentMarkerHelper.key()`, `AiContentMarkerService.batchQuery()`
- Produces: `VocabWordWrapper.toVO(VocabWordDto, Map<String, List<String>>)` — 新签名

- [ ] **Step 1: 修改 toVO 方法签名，增加 aiMarkers 参数**

将 `toVO(VocabWordDto dto)` 改为 `toVO(VocabWordDto dto, Map<String, List<String>> aiMarkers)`。内部调用子方法时透传 `aiMarkers`。

- [ ] **Step 2: 修改 toSenseVO — 注入 sense 的 aiGeneratedFields**

在 `toSenseVO` 方法末尾（`vo.setUpdateTime(dto.getUpdateTime())` 之后）追加：

```java
        String key = AiContentMarkerHelper.key("vocab_sense", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
```

- [ ] **Step 3: 修改 toStructureVO — 注入 structure 的 aiGeneratedFields**

在 `toStructureVO` 方法末尾追加：

```java
        String key = AiContentMarkerHelper.key("vocab_structure", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
```

- [ ] **Step 4: 修改 toExampleSentenceVO — 注入 example_sentence 的 aiGeneratedFields**

在现有的 `toExampleSentenceVO` 方法末尾追加：

```java
        String key = AiContentMarkerHelper.key("example_sentence", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
```

- [ ] **Step 5: 修改所有调用 toVO 的内部方法，传递 aiMarkers 参数**

所有 `toSenseVO(dto)` → `toSenseVO(dto, aiMarkers)`
所有 `toStructureVO(dto)` → `toStructureVO(dto, aiMarkers)`
所有 `toExampleSentenceVO(dto)` → `toExampleSentenceVO(dto, aiMarkers)`

- [ ] **Step 6: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java
git commit -m "feat: inject aiGeneratedFields into VocabWord Wrapper toVO methods

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 10: 词汇模块 — Controller 查询时收集 entity keys 并查询 AI 标记

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

**Interfaces:**
- Consumes: `AiContentMarkerService.batchQuery()`, `AiContentMarkerHelper`, `VocabWordWrapper.toVO(dto, aiMarkers)`

- [ ] **Step 1: 注入 AiContentMarkerService**

```java
    private final AiContentMarkerService aiContentMarkerService;
```

添加 import：

```java
import com.naon.grid.modules.system.service.AiContentMarkerService;
import com.naon.grid.modules.system.service.AiContentMarkerHelper;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
```

- [ ] **Step 2: 修改 findById 方法 — 收集 entity keys → 批量查询 → 传入 Wrapper**

```java
    @Log("查询词汇详情")
    @ApiOperation("根据ID查询词汇详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<VocabWordVO> findById(@PathVariable Integer id) {
        VocabWordDto dto = vocabWordService.findById(id);
        List<String> entityKeys = collectVocabEntityKeys(dto);
        Map<String, List<String>> aiMarkers = aiContentMarkerService.batchQuery(entityKeys);
        return new ResponseEntity<>(VocabWordWrapper.toVO(dto, aiMarkers), HttpStatus.OK);
    }

    /** 从 VocabWordDto 树中收集所有子实体的 entity key */
    private List<String> collectVocabEntityKeys(VocabWordDto dto) {
        List<String> keys = new ArrayList<>();
        if (dto.getSenses() != null) {
            for (com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto s : dto.getSenses()) {
                keys.addAll(AiContentMarkerHelper.collectOne("vocab_sense", s.getId()));
                if (s.getDefImageSentence() != null) {
                    keys.addAll(AiContentMarkerHelper.collectOne("example_sentence",
                            s.getDefImageSentence().getId()));
                }
                if (s.getStructures() != null) {
                    for (com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto st : s.getStructures()) {
                        keys.addAll(AiContentMarkerHelper.collectOne("vocab_structure", st.getId()));
                        if (st.getStructureSentences() != null) {
                            keys.addAll(AiContentMarkerHelper.collect("example_sentence",
                                    st.getStructureSentences().stream()
                                            .map(com.naon.grid.backend.service.common.dto.ExampleSentenceDto::getId)
                                            .collect(java.util.stream.Collectors.toList())));
                        }
                    }
                }
            }
        }
        return keys;
    }
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java
git commit -m "feat: batch query AI markers in VocabWordController detail endpoint

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 11: 词汇模块 — 发布时物化 AI 标记到 ai_content_marker

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

**Interfaces:**
- Consumes: `AiContentMarkerService.batchReplace()`

- [ ] **Step 1: 注入 AiContentMarkerService**

```java
    private final AiContentMarkerService aiContentMarkerService;
```

```java
import com.naon.grid.modules.system.service.AiContentMarkerService;
import java.util.ArrayList;
```

- [ ] **Step 2: 在 publishDraft 中，syncSenses 完毕后，从 draftDto 提取 AI 标记并写入**

在 `publishDraft` 方法的 `syncSenses(id, draftDto.getWord(), draftDto.getSenses());` 之后追加：

```java
        // 物化 AI 内容标记
        List<AiContentMarkerService.MarkerEntry> markerEntries = new ArrayList<>();
        collectSenseMarkers(draftDto.getSenses(), markerEntries);
        aiContentMarkerService.batchReplace(markerEntries);
```

添加私有辅助方法：

```java
    private void collectSenseMarkers(List<com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto> senses,
                                     List<AiContentMarkerService.MarkerEntry> entries) {
        if (senses == null) return;
        for (com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto s : senses) {
            if (s.getId() != null && s.getAiGeneratedFields() != null) {
                entries.add(new AiContentMarkerService.MarkerEntry(
                        "vocab_sense", s.getId().longValue(), s.getAiGeneratedFields()));
            }
            // 例句
            if (s.getDefImageSentence() != null && s.getDefImageSentence().getId() != null
                    && s.getDefImageSentence().getAiGeneratedFields() != null) {
                entries.add(new AiContentMarkerService.MarkerEntry(
                        "example_sentence", s.getDefImageSentence().getId(),
                        s.getDefImageSentence().getAiGeneratedFields()));
            }
            // 结构及其例句
            if (s.getStructures() != null) {
                for (com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto st : s.getStructures()) {
                    if (st.getId() != null && st.getAiGeneratedFields() != null) {
                        entries.add(new AiContentMarkerService.MarkerEntry(
                                "vocab_structure", st.getId().longValue(), st.getAiGeneratedFields()));
                    }
                    if (st.getStructureSentences() != null) {
                        for (com.naon.grid.backend.service.common.dto.ExampleSentenceDto es : st.getStructureSentences()) {
                            if (es.getId() != null && es.getAiGeneratedFields() != null) {
                                entries.add(new AiContentMarkerService.MarkerEntry(
                                        "example_sentence", es.getId(), es.getAiGeneratedFields()));
                            }
                        }
                    }
                }
            }
        }
    }
```

- [ ] **Step 3: 同时在 VocabSenseDto 中增加 aiGeneratedFields 字段**

```java
    // VocabSenseDto 中追加
    private List<String> aiGeneratedFields;
```

- [ ] **Step 4: 在 VocabStructureDto 中增加 aiGeneratedFields 字段**

```java
    // VocabStructureDto 中追加
    private List<String> aiGeneratedFields;
```

- [ ] **Step 5: 在 ExampleSentenceDto 中增加 aiGeneratedFields 字段**

```java
    // ExampleSentenceDto 中追加
    private List<String> aiGeneratedFields;
```

- [ ] **Step 6: Wrapper 中 Request→Dto 转换时传递 aiGeneratedFields**

在 `VocabWordWrapper.toSenseDto()` 中追加：

```java
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
```

在 `VocabWordWrapper.toStructureDto()` 中追加：

```java
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
```

在 `VocabWordWrapper.toExampleSentenceDto()` 中追加：

```java
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
```

- [ ] **Step 7: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java \
        grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java \
        grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabStructureDto.java \
        grid-system/src/main/java/com/naon/grid/backend/service/common/dto/ExampleSentenceDto.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java
git commit -m "feat: materialize AI markers on vocab publish, wire DTO and Wrapper

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 12: 语法模块 — Wrapper + Controller + Publish 注入 AI 标记

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/GrammarPointWrapper.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/GrammarPointController.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/GrammarPointServiceImpl.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarMeaningDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarStructureDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarNoticeDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarErrorDto.java`

**Interfaces:**
- Consumes: `AiContentMarkerService`, `AiContentMarkerHelper`
- Produces: `GrammarPointWrapper.toVO(dto, aiMarkers)`, Controller 收集 keys 并查询, Service publish 物化标记

- [ ] **Step 1: 修改 GrammarPointWrapper.toVO 签名，增加 aiMarkers 参数，子 VO 注入标记**

`toVO(GrammarPointDto dto)` → `toVO(GrammarPointDto dto, Map<String, List<String>> aiMarkers)`。

各子 VO 构建方法末尾按模式追加：

```java
        String key = AiContentMarkerHelper.key("grammar_meaning", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
```

对 grammar_meaning、grammar_structure、grammar_notice、grammar_error 四个子类型各做一次。对内部 example_sentence 也做注入（复用已改好的 toExampleSentenceVO）。

- [ ] **Step 2: Wrapper 中 Request→Dto 转换时传递 aiGeneratedFields**

在 `toMeaningDto`、`toStructureDto`、`toNoticeDto`、`toErrorDto` 中各追加：

```java
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
```

- [ ] **Step 3: 四个 Grammar Dto 追加 aiGeneratedFields 字段**

```java
    private List<String> aiGeneratedFields;
```

- [ ] **Step 4: GrammarPointController 注入 AiContentMarkerService，findById 收集 entity keys**

```java
    private final AiContentMarkerService aiContentMarkerService;

    @AnonymousGetMapping("/{id}")
    public ResponseEntity<GrammarPointVO> findById(@PathVariable Long id) {
        GrammarPointDto dto = grammarPointService.findById(id);
        List<String> keys = collectGrammarEntityKeys(dto);
        Map<String, List<String>> aiMarkers = aiContentMarkerService.batchQuery(keys);
        return new ResponseEntity<>(GrammarPointWrapper.toVO(dto, aiMarkers), HttpStatus.OK);
    }
```

`collectGrammarEntityKeys` 方法遍历 meanings/structures/notices/errors 及其 example sentences。

- [ ] **Step 5: GrammarPointServiceImpl.publishDraft 物化 AI 标记**

同 Task 11 模式，发布时从 DraftDto 的各子列表中提取 aiGeneratedFields，写入 ai_content_marker。

- [ ] **Step 6: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/GrammarPointWrapper.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/controller/GrammarPointController.java \
        grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/GrammarPointServiceImpl.java \
        grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarMeaningDto.java \
        grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarStructureDto.java \
        grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarNoticeDto.java \
        grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarErrorDto.java
git commit -m "feat: integrate AI marker query and publish for GrammarPoint module

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 13: 每日一语模块 — Wrapper + Controller + Publish 注入 AI 标记

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/DailyVocabularyWrapper.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/DailyVocabularyController.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/DailyVocabularyServiceImpl.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/DailyVocabularyDto.java`

**Interfaces:**
- Consumes: `AiContentMarkerService`, `AiContentMarkerHelper`
- Produces: `DailyVocabularyWrapper.toVO(dto, aiMarkers)`

- [ ] **Step 1: DailyVocabularyDto 追加 aiGeneratedFields 字段**

```java
    private List<String> aiGeneratedFields;
```

- [ ] **Step 2: DailyVocabularyWrapper 修改**

`toVO(DailyVocabularyDto dto)` → `toVO(DailyVocabularyDto dto, Map<String, List<String>> aiMarkers)`。

在 toVO 末尾注入：

```java
        String key = AiContentMarkerHelper.key("daily_vocabulary", dto.getId());
        if (key != null && aiMarkers != null) {
            vo.setAiGeneratedFields(aiMarkers.getOrDefault(key, Collections.emptyList()));
        }
```

Wrapper 中 toDto 方法追加：

```java
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
```

- [ ] **Step 3: DailyVocabularyController 注入 AiContentMarkerService，findById 收集 keys**

```java
    private final AiContentMarkerService aiContentMarkerService;

    @AnonymousGetMapping("/{id}")
    public ResponseEntity<DailyVocabularyVO> findById(@PathVariable Integer id) {
        DailyVocabularyDto dto = dailyVocabularyService.findById(id);
        List<String> keys = AiContentMarkerHelper.collectOne("daily_vocabulary", dto.getId());
        Map<String, List<String>> aiMarkers = aiContentMarkerService.batchQuery(keys);
        return new ResponseEntity<>(DailyVocabularyWrapper.toVO(dto, aiMarkers), HttpStatus.OK);
    }
```

- [ ] **Step 4: DailyVocabularyServiceImpl.publishDraft 物化 AI 标记**

从 draft_content 解析出 Dto，调用 replaceFields 写入标记。

- [ ] **Step 5: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/DailyVocabularyWrapper.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/controller/DailyVocabularyController.java \
        grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/DailyVocabularyServiceImpl.java \
        grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/DailyVocabularyDto.java
git commit -m "feat: integrate AI marker query and publish for DailyVocabulary module

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 14: 汉字模块 — Wrapper + Controller + Publish 注入 AI 标记

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharComparisonDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java`

**entity_type 映射：** `char_comparison` (CharComparisonVO/Dto), `char_word` (CharWordVO/Dto), `example_sentence` (其中的例句)

- [ ] **Step 1: CharComparisonDto、CharWordDto 追加 aiGeneratedFields**

```java
    private List<String> aiGeneratedFields;
```

- [ ] **Step 2: CharCharacterWrapper 修改**

`toVO(CharCharacterDto dto)` → `toVO(CharCharacterDto dto, Map<String, List<String>> aiMarkers)`。

`toComparisonVO` 末尾注入 entity_type=`char_comparison` 的标记。
`toWordVO` 末尾注入 entity_type=`char_word` 的标记。
其中的 ExampleSentenceVO 注入 entity_type=`example_sentence` 的标记。

`toDto` 方法中传递 `request.getAiGeneratedFields()` 到 Dto。

- [ ] **Step 3: CharCharacterController 注入 AiContentMarkerService，findById 收集 entity keys**

注入 `AiContentMarkerService`。在 `findById` 方法中，遍历 Dto 的 comparisons/words，收集 `char_comparison:{id}`、`char_word:{id}`、`example_sentence:{id}` 键，调用 `batchQuery` 传入 Wrapper。

- [ ] **Step 4: CharCharacterServiceImpl.publishDraft 物化 AI 标记**

从 draft_content 解析 Dto，提取 comparisons 和 words 列表中的 aiGeneratedFields，按 entity_type 写入 ai_content_marker。

- [ ] **Step 5: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java \
        grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java \
        grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharComparisonDto.java \
        grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java
git commit -m "feat: integrate AI marker query and publish for CharCharacter module

Co-Authored-By: Claude <noreply@anthropic.com>"
```

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java \
        grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java \
        grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharComparisonDto.java \
        grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java
git commit -m "feat: integrate AI marker query and publish for CharCharacter module

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 15: 词汇辨析模块 — Wrapper + Controller + Publish 注入 AI 标记

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabComparisonGroupWrapper.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabComparisonController.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/impl/VocabComparisonGroupServiceImpl.java`
- 词汇辨析相关 Dto（Item 和 Chat）

**entity_type 映射：** `vocab_comparison_item`, `vocab_comparison_chat`

- [ ] **Step 1: 相关 Dto 追加 aiGeneratedFields**
- [ ] **Step 2: VocabComparisonGroupWrapper.toVO 签名加 aiMarkers 参数，子 VO 注入标记**

`toVO(dto, aiMarkers)` — VocabComparisonItemVO 注入 entity_type=`vocab_comparison_item`，VocabComparisonChatVO 注入 entity_type=`vocab_comparison_chat`。

- [ ] **Step 3: Wrapper.toDto 传递 aiGeneratedFields**
- [ ] **Step 4: VocabComparisonController 注入 AiContentMarkerService，findById 收集 keys**
- [ ] **Step 5: VocabComparisonGroupServiceImpl.publishDraft 物化标记**

从 draft_content 解析 Dto，提取 items/chats 列表中的 aiGeneratedFields 写入 ai_content_marker。

- [ ] **Step 6: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabComparisonGroupWrapper.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabComparisonController.java \
        grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/impl/VocabComparisonGroupServiceImpl.java
git commit -m "feat: integrate AI marker query and publish for VocabComparison module

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 16: 语法辨析 + 练习题模块 — Wrapper + Controller + Service 注入 AI 标记

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/GrammarComparisonGroupWrapper.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/GrammarComparisonController.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammarcomparison/impl/GrammarComparisonGroupServiceImpl.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/ExerciseQuestionWrapper.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/ExerciseQuestionController.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/question/impl/ExerciseQuestionServiceImpl.java`
- 相关 Dto 文件

**entity_type 映射：** `grammar_comparison_item`, `grammar_comparison_chat`, `exercise_question`

- [ ] **Step 1: 语法辨析相关 Dto 追加 aiGeneratedFields，Wrapper/Controller 注入标记（同 Task 15 模式）**

entity_type: `grammar_comparison_item`, `grammar_comparison_chat`

- [ ] **Step 2: GrammarComparisonGroupServiceImpl.publishDraft 物化标记**
- [ ] **Step 3: ExerciseQuestion 相关 Dto 追加 aiGeneratedFields**

```java
    private List<String> aiGeneratedFields;
```

- [ ] **Step 4: ExerciseQuestionWrapper.toVO 签名加 aiMarkers，注入 entity_type=`exercise_question` 标记**

练习题有递归 children，对每个子题也注入 `exercise_question` 标记。

- [ ] **Step 5: ExerciseQuestionWrapper.toDto 传递 aiGeneratedFields**

```java
        dto.setAiGeneratedFields(request.getAiGeneratedFields());
```

- [ ] **Step 6: ExerciseQuestionController 注入 AiContentMarkerService，findById 收集 keys（含递归 children）**
- [ ] **Step 7: ExerciseQuestionServiceImpl.create/update 调用 AiContentMarkerService.replaceFields**

练习题无草稿流程，直接在 create 和 update 方法中调用：

```java
        aiContentMarkerService.replaceFields("exercise_question",
                savedQuestion.getId(), dto.getAiGeneratedFields());
        // 递归处理子题
        if (dto.getChildren() != null) {
            for (ExerciseQuestionDto child : dto.getChildren()) {
                aiContentMarkerService.replaceFields("exercise_question",
                        child.getId(), child.getAiGeneratedFields());
            }
        }
```

- [ ] **Step 8: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/GrammarComparisonGroupWrapper.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/controller/GrammarComparisonController.java \
        grid-system/src/main/java/com/naon/grid/backend/service/grammarcomparison/impl/GrammarComparisonGroupServiceImpl.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/ExerciseQuestionWrapper.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/controller/ExerciseQuestionController.java \
        grid-system/src/main/java/com/naon/grid/backend/service/question/impl/ExerciseQuestionServiceImpl.java
git commit -m "feat: integrate AI marker for GrammarComparison and ExerciseQuestion modules

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 17: 验证 & 测试

- [ ] **Step 1: 构建项目确认编译通过**

```bash
cd grid-system && mvn compile -DskipTests
```

- [ ] **Step 2: 手动测试 — 创建词汇草稿，带 AI 标记**

POST /api/vocab-words，senses/chineseDef 带 `aiGeneratedFields: ["chineseDef"]`。发布后检查 `ai_content_marker` 表有对应记录。

- [ ] **Step 3: 手动测试 — 查询详情，检查 VO 返回 aiGeneratedFields**

GET /api/vocab-words/{id}，确认 senses[0].aiGeneratedFields 包含 ["chineseDef"]。

- [ ] **Step 4: 手动测试 — 更新后全量替换**

PUT 同一个词汇，aiGeneratedFields 传空数组。检查详情返回空列表，数据库无记录。

- [ ] **Step 5: 手动测试 — 无草稿实体 (exercise_question)**

POST 创建练习题，带 aiGeneratedFields，查询详情确认返回。

- [ ] **Step 6: 手动测试 — 各模块详情接口均能正常返回**

语法、汉字、每日一语、辨析等所有模块的详情接口验证 AI 标记正常工作。

- [ ] **Step 7: Commit 测试相关变更**
