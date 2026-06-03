# 词汇管理工作流重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构词汇管理工作流，移除重复接口，统一使用 draftContent 进行草稿编辑，参照汉字管理的实现方式。

**Architecture:** 修改 Service 层和 Controller 层，移除 VocabWordDraftDto 的使用，统一使用 VocabWordDto，调整状态流转逻辑。

**Tech Stack:** Spring Boot 2.7, Spring Data JPA, Lombok

---

## File Mapping

| File | Responsibility |
|------|----------------|
| `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java` | Service 接口定义 |
| `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java` | Service 实现 |
| `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java` | Controller 接口 |

---

### Task 1: 修改 VocabWordService 接口

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java`

- [ ] **Step 1: 移除草稿相关方法**

从接口中移除以下方法：
```java
VocabWordDraftDto getDraft(Integer id);
void saveDraft(Integer id, VocabWordDraftDto draft);
Integer createDraft(VocabWordDraftDto draft);
void createDraftFromPublished(Integer id);
```

保留的方法：
```java
PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable);
VocabWordDto findById(Integer id);
VocabWordDto findPublishedById(Integer id);
Integer create(VocabWordDto resources);
void update(Integer id, VocabWordDto resources);
void delete(Integer id);
void reviewDraft(Integer id);
void publishDraft(Integer id);
void offline(Integer id);
```

- [ ] **Step 2: 提交修改**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java
git commit -m "refactor: remove draft-related methods from VocabWordService"
```

---

### Task 2: 重构 VocabWordServiceImpl 的 create 和 update 方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java:106-135`

- [ ] **Step 1: 重构 create 方法**

替换 create 方法为：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public Integer create(VocabWordDto resources) {
    VocabWord vocabWord = new VocabWord();
    vocabWord.setStatus(StatusEnum.ENABLED.getCode());
    vocabWord.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
    vocabWord.setEditStatus(EditStatusEnum.DRAFT.getCode());
    vocabWord.setWord(resources.getWord());
    vocabWord.setDraftContent(JsonUtils.toJson(resources));
    vocabWord = vocabWordRepository.save(vocabWord);
    return vocabWord.getId();
}
```

- [ ] **Step 2: 重构 update 方法**

替换 update 方法为：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void update(Integer id, VocabWordDto resources) {
    VocabWord vocabWord = vocabWordRepository.findById(id).orElseGet(VocabWord::new);
    if (vocabWord.getId() == null || StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }
    if (EditStatusEnum.REVIEWED.getCode().equals(vocabWord.getEditStatus()) ||
        EditStatusEnum.PUBLISHED.getCode().equals(vocabWord.getEditStatus())) {
        vocabWord.setEditStatus(EditStatusEnum.DRAFT.getCode());
    }
    vocabWord.setDraftContent(JsonUtils.toJson(resources));
    vocabWordRepository.save(vocabWord);
}
```

- [ ] **Step 3: 提交修改**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "refactor: rewrite create and update for draft workflow"
```

---

### Task 3: 重构 VocabWordServiceImpl 的 findById 方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java:48-73`

- [ ] **Step 1: 重构 findById 方法**

替换 findById 方法为：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public VocabWordDto findById(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id).orElseGet(VocabWord::new);
    if (vocabWord.getId() == null || StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }

    if (EditStatusEnum.DRAFT.getCode().equals(vocabWord.getEditStatus()) ||
        EditStatusEnum.REVIEWED.getCode().equals(vocabWord.getEditStatus())) {
        VocabWordDto dto = JsonUtils.fromJson(vocabWord.getDraftContent(), VocabWordDto.class);
        dto.setId(vocabWord.getId());
        dto.setStatus(vocabWord.getStatus());
        dto.setPublishStatus(vocabWord.getPublishStatus());
        dto.setEditStatus(vocabWord.getEditStatus());
        dto.setCreateTime(vocabWord.getCreateTime());
        dto.setUpdateTime(vocabWord.getUpdateTime());
        dto.setCreateBy(vocabWord.getCreateBy());
        dto.setUpdateBy(vocabWord.getUpdateBy());
        return dto;
    }

    VocabWordDto vocabWordDto = vocabWordMapper.toDto(vocabWord);

    List<VocabSenseDto> senseDtos = new ArrayList<>();
    List<VocabSense> senses = vocabSenseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode());
    for (VocabSense sense : senses) {
        VocabSenseDto senseDto = convertToSenseDto(sense);
        senseDtos.add(senseDto);
    }
    vocabWordDto.setSenses(senseDtos);

    List<VocabExerciseDto> exerciseDtos = new ArrayList<>();
    List<VocabExercise> exercises = vocabExerciseRepository.findByWordIdAndStatus(id, StatusEnum.ENABLED.getCode());
    for (VocabExercise exercise : exercises) {
        VocabExerciseDto exerciseDto = convertToExerciseDto(exercise);
        exerciseDtos.add(exerciseDto);
    }
    vocabWordDto.setExercises(exerciseDtos);

    return vocabWordDto;
}
```

- [ ] **Step 2: 提交修改**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "refactor: rewrite findById to return draftContent when appropriate"
```

---

### Task 4: 重构 VocabWordServiceImpl 的 publishDraft、offline 和 delete 方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java:660-713`

- [ ] **Step 1: 重构 publishDraft 方法**

替换 publishDraft 方法为：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void publishDraft(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

    if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }

    if (vocabWord.getDraftContent() == null) {
        throw new BadRequestException("草稿不存在");
    }

    if (!EditStatusEnum.REVIEWED.getCode().equals(vocabWord.getEditStatus())) {
        throw new BadRequestException("仅已审核状态可发布");
    }

    VocabWordDto draftDto = JsonUtils.fromJson(vocabWord.getDraftContent(), VocabWordDto.class);

    vocabWord.setWordTraditional(draftDto.getWordTraditional());
    vocabWord.setPinyin(draftDto.getPinyin());
    vocabWord.setAudioId(draftDto.getAudioId());
    vocabWord.setHskLevel(draftDto.getHskLevel());

    syncSenses(id, draftDto.getSenses());
    syncExercises(id, draftDto.getExercises());

    vocabWord.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
    vocabWord.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
    vocabWord.setDraftContent(null);
    vocabWordRepository.save(vocabWord);
}
```

- [ ] **Step 2: 重构 offline 方法**

替换 offline 方法为：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void offline(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id)));

    if (StatusEnum.DISABLED.getCode().equals(vocabWord.getStatus())) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }

    vocabWord.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
    vocabWordRepository.save(vocabWord);
}
```

- [ ] **Step 3: 重构 delete 方法**

替换 delete 方法为：
```java
@Override
@Transactional(rollbackFor = Exception.class)
public void delete(Integer id) {
    VocabWord vocabWord = vocabWordRepository.findById(id).orElseGet(VocabWord::new);
    if (vocabWord.getId() == null) {
        throw new EntityNotFoundException(VocabWord.class, "id", String.valueOf(id));
    }

    vocabWord.setStatus(StatusEnum.DISABLED.getCode());
    vocabWordRepository.save(vocabWord);
}
```

- [ ] **Step 4: 提交修改**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "refactor: rewrite publishDraft, offline, delete for new workflow"
```

---

### Task 5: 移除 VocabWordServiceImpl 中的草稿相关方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

- [ ] **Step 1: 删除草稿相关方法**

删除以下方法：
- `getDraft(Integer id)`
- `createDraft(VocabWordDraftDto draft)`
- `saveDraft(Integer id, VocabWordDraftDto draft)`
- `createDraftFromPublished(Integer id)`

- [ ] **Step 2: 提交修改**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "refactor: remove draft-related service implementations"
```

---

### Task 6: 修改 VocabWordController

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

- [ ] **Step 1: 移除草稿相关接口**

删除以下接口方法：
- `getDraft(Integer id)`
- `createDraft(VocabWordCreateRequest request)`
- `updateDraft(Integer id, VocabWordCreateRequest request)`
- `createDraftFromPublished(Integer id)`

同时删除不再需要的 `convertToDraftDto` 方法。

- [ ] **Step 2: 确认保留的接口**

确认保留以下接口：
- `findById(Integer id)`
- `queryAll(VocabWordQueryRequest request, Pageable pageable)`
- `create(VocabWordCreateRequest request)`
- `update(Integer id, VocabWordCreateRequest request)`
- `delete(Integer id)`
- `reviewDraft(Integer id)`
- `publishDraft(Integer id)`
- `offline(Integer id)`

- [ ] **Step 3: 提交修改**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java
git commit -m "refactor: remove draft-related endpoints from VocabWordController"
```

---

### Task 7: 编译验证

**Files:**
- (build files)

- [ ] **Step 1: 运行 Maven 编译**

```bash
mvn clean compile -DskipTests
```

预期结果：编译成功，无错误

- [ ] **Step 2: 提交（如有必要）**

如果有任何修复需要提交：
```bash
git add .
git commit -m "fix: compilation fixes after refactor"
```

---

## Plan Complete

Plan complete and saved to `docs/superpowers/plans/2026-06-02-vocab-word-workflow-refactor.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
