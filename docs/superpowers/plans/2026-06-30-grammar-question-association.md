# Grammar-Question Association Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable associating ExerciseQuestion IDs with GrammarPoint through the existing draft/publish workflow.

**Architecture:** Add a `GrammarQuestion` entity + service layer for the `grammar_question` table. Integrate `questionIds` (List<Long>) into the existing `GrammarPointDto` / VOs / Wrapper, and hook sync into `GrammarPointServiceImpl.publishDraft()`.

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Fastjson2, Lombok, MapStruct, Java 8

**Modules affected:** `grid-system` only

## Global Constraints

- Java 8 language level — no `var`, no records, no `List.of()`, no `Map.of()`, no `collect(Collectors.toMap())` with null values
- Follow existing sub-table pattern (GrammarMeaning, GrammarStructure, etc.)
- Soft delete (set status=0), never physically delete
- JSON serialization via Fastjson2 (`com.alibaba.fastjson2.JSON`)
- Use Lombok `@Getter`/`@Setter` on all classes
- `grid-system` module only — no cross-module changes

---

### Task 1: Create GrammarQuestion Entity, Repository, and DTO

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/grammar/GrammarQuestion.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/grammar/GrammarQuestionRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarQuestionDto.java`

**Interfaces:**
- Consumes: nothing — pure data layer
- Produces: `GrammarQuestion` entity, `GrammarQuestionRepository` (JPA), `GrammarQuestionDto`

- [ ] **Step 1: Create `GrammarQuestion.java` entity**

Write to `grid-system/src/main/java/com/naon/grid/backend/domain/grammar/GrammarQuestion.java`:

```java
package com.naon.grid.backend.domain.grammar;

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
@Table(name = "grammar_question")
public class GrammarQuestion implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "语法题目关联ID", hidden = true)
    private Long id;

    @Column(name = "grammar_id", nullable = false)
    private Long grammarId;

    @Column(name = "question_ids", nullable = false, length = 1024)
    private String questionIds;

    @Column(name = "`order`", nullable = false)
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
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 2: Create `GrammarQuestionRepository.java`**

Write to `grid-system/src/main/java/com/naon/grid/backend/repo/grammar/GrammarQuestionRepository.java`:

```java
package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GrammarQuestionRepository extends JpaRepository<GrammarQuestion, Long> {

    List<GrammarQuestion> findByGrammarIdAndStatus(Long grammarId, Integer status);

    List<GrammarQuestion> findByGrammarIdInAndStatus(Collection<Long> grammarIds, Integer status);
}
```

- [ ] **Step 3: Create `GrammarQuestionDto.java`**

Write to `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarQuestionDto.java`:

```java
package com.naon.grid.backend.service.grammar.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class GrammarQuestionDto implements Serializable {

    @ApiModelProperty(value = "语法题目关联ID")
    private Long id;

    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @ApiModelProperty(value = "语法题目ID列表")
    private List<Long> questionIds;

    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer order;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am -q
```
Expected: BUILD SUCCESS (no errors)

- [ ] **Step 5: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/grammar/GrammarQuestion.java \
        grid-system/src/main/java/com/naon/grid/backend/repo/grammar/GrammarQuestionRepository.java \
        grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarQuestionDto.java
git commit -m "feat: add GrammarQuestion entity, repository, and DTO for grammar-question association"
```

---

### Task 2: Create GrammarQuestionService

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/GrammarQuestionService.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/GrammarQuestionServiceImpl.java`

**Interfaces:**
- Consumes: `GrammarQuestionRepository`, `GrammarQuestionDto`
- Produces: `GrammarQuestionService` interface with `findByGrammarId()`, `findByGrammarIds()`, `syncFromDraft()`

- [ ] **Step 1: Create `GrammarQuestionService.java` interface**

Write to `grid-system/src/main/java/com/naon/grid/backend/service/grammar/GrammarQuestionService.java`:

```java
package com.naon.grid.backend.service.grammar;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GrammarQuestionService {

    List<Long> findByGrammarId(Long grammarId);

    Map<Long, List<Long>> findByGrammarIds(Collection<Long> grammarIds);

    void syncFromDraft(Long grammarId, List<Long> questionIds);
}
```

- [ ] **Step 2: Create `GrammarQuestionServiceImpl.java`**

Write to `grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/GrammarQuestionServiceImpl.java`:

```java
package com.naon.grid.backend.service.grammar.impl;

import com.naon.grid.backend.domain.grammar.GrammarQuestion;
import com.naon.grid.backend.repo.grammar.GrammarQuestionRepository;
import com.naon.grid.backend.service.grammar.GrammarQuestionService;
import com.naon.grid.enums.StatusEnum;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrammarQuestionServiceImpl implements GrammarQuestionService {

    private final GrammarQuestionRepository grammarQuestionRepository;

    @Override
    public List<Long> findByGrammarId(Long grammarId) {
        List<GrammarQuestion> records = grammarQuestionRepository
                .findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        return records.stream()
                .map(r -> parseQuestionIds(r.getQuestionIds()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<Long>> findByGrammarIds(Collection<Long> grammarIds) {
        if (grammarIds == null || grammarIds.isEmpty()) return Collections.emptyMap();
        List<GrammarQuestion> records = grammarQuestionRepository
                .findByGrammarIdInAndStatus(grammarIds, StatusEnum.ENABLED.getCode());
        return records.stream()
                .collect(Collectors.toMap(
                        GrammarQuestion::getGrammarId,
                        r -> parseQuestionIds(r.getQuestionIds())
                ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromDraft(Long grammarId, List<Long> questionIds) {
        // Soft-delete existing enabled records
        List<GrammarQuestion> existing = grammarQuestionRepository
                .findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        for (GrammarQuestion record : existing) {
            record.setStatus(StatusEnum.DISABLED.getCode());
            grammarQuestionRepository.save(record);
        }

        // Create new record if questionIds is non-empty
        if (questionIds != null && !questionIds.isEmpty()) {
            GrammarQuestion entity = new GrammarQuestion();
            entity.setGrammarId(grammarId);
            entity.setQuestionIds(JSON.toJSONString(questionIds));
            entity.setOrder(0);
            entity.setStatus(StatusEnum.ENABLED.getCode());
            grammarQuestionRepository.save(entity);
        }
    }

    private List<Long> parseQuestionIds(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return JSON.parseArray(json, Long.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/grammar/GrammarQuestionService.java \
        grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/GrammarQuestionServiceImpl.java
git commit -m "feat: add GrammarQuestionService for grammar-question association management"
```

---

### Task 3: Add questionIds to GrammarPoint DTO and VOs

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarPointDto.java` — add `questionIds` field
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/GrammarPointCreateRequest.java` — add `questionIds` field
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarPointVO.java` — add `questionIds` field
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarPointBaseVO.java` — add `questionIds` field

**Interfaces:**
- Consumes: nothing new
- Produces: `questionIds` field available in all layers

- [ ] **Step 1: Modify `GrammarPointDto.java`**

In `GrammarPointDto.java`, add a `questionIds` field after the existing `errors` field (after line 43):

```java
    @ApiModelProperty(value = "语法题目ID列表")
    private List<Long> questionIds;
```

- [ ] **Step 2: Modify `GrammarPointCreateRequest.java`**

In `GrammarPointCreateRequest.java`, add a `questionIds` field after the existing `errors` field (after line 46):

```java
    @ApiModelProperty(value = "语法题目ID列表")
    private List<Long> questionIds;
```

- [ ] **Step 3: Modify `GrammarPointVO.java`**

In `GrammarPointVO.java`, add a `questionIds` field after the existing `errors` field (after line 64):

```java
    @ApiModelProperty(value = "语法题目ID列表")
    private List<Long> questionIds;
```

- [ ] **Step 4: Modify `GrammarPointBaseVO.java`**

In `GrammarPointBaseVO.java`, add a `questionIds` field after the existing `errorCount` field (after line 63):

```java
    @ApiModelProperty(value = "语法题目ID列表")
    private List<Long> questionIds;
```

- [ ] **Step 5: Verify compilation**

```bash
cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarPointDto.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/request/GrammarPointCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarPointVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarPointBaseVO.java
git commit -m "feat: add questionIds field to GrammarPoint DTO and VOs"
```

---

### Task 4: Add questionIds Mapping to GrammarPointWrapper

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/GrammarPointWrapper.java`

**Interfaces:**
- Consumes: `GrammarPointCreateRequest.questionIds`, `GrammarPointDto.questionIds`
- Produces: Mapped values in `GrammarPointDto`, `GrammarPointVO`, `GrammarPointBaseVO`

- [ ] **Step 1: Add import for `Collections` if not present**

`GrammarPointWrapper.java` already imports `Collections` (line 20).

- [ ] **Step 2: Add questionIds mapping in `toDto(GrammarPointCreateRequest)`**

In `toDto()` method (at line 48, before `return dto;`), add:

```java
        dto.setQuestionIds(request.getQuestionIds());
```

- [ ] **Step 3: Add questionIds mapping in `toVO(GrammarPointDto)`**

In `toVO()` method (at line 71, before `return vo;`), add:

```java
        vo.setQuestionIds(dto.getQuestionIds());
```

- [ ] **Step 4: Add questionIds mapping in `toBaseVO(GrammarPointDto)`**

In `toBaseVO()` method (at line 98, before `return vo;`), add:

```java
        vo.setQuestionIds(dto.getQuestionIds());
```

- [ ] **Step 5: Verify compilation**

```bash
cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/GrammarPointWrapper.java
git commit -m "feat: add questionIds mapping in GrammarPointWrapper"
```

---

### Task 5: Integrate GrammarQuestionService into GrammarPointServiceImpl

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/GrammarPointServiceImpl.java`

**Interfaces:**
- Consumes: `GrammarQuestionService`, `GrammarPointDto.questionIds`
- Produces: Working publish sync + query flow

- [ ] **Step 1: Add `GrammarQuestionService` import and field injection**

Add import:
```java
import com.naon.grid.backend.service.grammar.GrammarQuestionService;
```

Add field (after `exampleSentenceService` at line 61):
```java
    private final GrammarQuestionService grammarQuestionService;
```

- [ ] **Step 2: Add `syncQuestions()` call in `publishDraft()`**

After `syncErrors(grammarPoint.getId(), draftDto.getErrors());` (line 413), add:
```java
        syncQuestions(grammarPoint.getId(), draftDto.getQuestionIds());
```

- [ ] **Step 3: Add `syncQuestions()` private method**

Add this method before the `// ===== Example sentence helpers =====` section (before line 651):

```java
    private void syncQuestions(Long grammarId, List<Long> questionIds) {
        grammarQuestionService.syncFromDraft(grammarId, questionIds);
    }
```

- [ ] **Step 4: Add questionIds loading in `toPublishedDetailDto()`**

In `toPublishedDetailDto()`, after `dto.setErrors(...)` (line 219), add:
```java
        dto.setQuestionIds(grammarQuestionService.findByGrammarId(id));
```

- [ ] **Step 5: Add questionIds batch loading in `populateGrammarListStats()`**

At the end of `populateGrammarListStats()`, after building the errorCountMap (after line 110), add batch loading:

```java
        Map<Long, List<Long>> questionIdsMap = grammarQuestionService.findByGrammarIds(ids);
```

Then in the for-loop (after line 120), add:
```java
            if (dto.getQuestionIds() == null) {
                dto.setQuestionIds(questionIdsMap.getOrDefault(dto.getId(), Collections.emptyList()));
            }
```

- [ ] **Step 6: Add questionIds draft overlay in `applyDraftOverlay()`**

In `applyDraftOverlay()`, after the existing draft overlay fields (after line 171), add:
```java
        if (draft.getQuestionIds() != null) {
            dto.setQuestionIds(draft.getQuestionIds());
        }
```

- [ ] **Step 7: Add `Collections` import if not present**

Line 42 already has `import java.util.Collections;` — verify it's there.

- [ ] **Step 8: Verify compilation**

```bash
cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am -q
```
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/GrammarPointServiceImpl.java
git commit -m "feat: integrate GrammarQuestionService into GrammarPoint publish/query flow"
```

---

### Task 6: Full Build Verification

- [ ] **Step 1: Full project compilation**

```bash
cd /Users/nano/Desktop/nano-gemini && mvn clean compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 2: Verify git status clean**

```bash
git status
```
Expected: working tree clean (no uncommitted changes after the 5 commits)

- [ ] **Step 3: Final commit summary**

```bash
git log --oneline -6
```
Expected: 5 feature commits + the design doc commit
