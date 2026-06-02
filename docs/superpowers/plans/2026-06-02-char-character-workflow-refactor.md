# 汉字管理工作流重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构汉字管理工作流程，移除重复接口，统一使用 draftContent，实现正确的状态流转

**Architecture:** 
- 更新枚举添加 PUBLISHED 状态
- 重构 Service 层实现新的工作流逻辑
- 重构 Controller 层移除重复接口
- 保持用户端接口不变

**Tech Stack:** Java 8, Spring Boot 2.7, Spring Data JPA, Maven

---

## Files Overview

| File | Action | Purpose |
|------|--------|---------|
| `grid-common/src/main/java/com/naon/grid/enums/EditStatusEnum.java` | Modify | Add PUBLISHED enum |
| `grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java` | Modify | Remove draft methods |
| `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java` | Modify | Refactor all business logic |
| `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java` | Modify | Remove draft endpoints, refactor remaining |

---

### Task 1: Add PUBLISHED to EditStatusEnum

**Files:**
- Modify: `grid-common/src/main/java/com/naon/grid/enums/EditStatusEnum.java`

- [ ] **Step 1: Read the current enum file**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && cat "grid-common/src/main/java/com/naon/grid/enums/EditStatusEnum.java"
```

- [ ] **Step 2: Add PUBLISHED enum value**

Edit the file to add the new enum:

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum EditStatusEnum {
    DRAFT("draft", "草稿"),
    REVIEWED("reviewed", "已审核"),
    PUBLISHED("published", "已发布");

    private final String code;
    private final String description;

    EditStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 3: Commit the change**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "grid-common/src/main/java/com/naon/grid/enums/EditStatusEnum.java" && git commit -m "feat: add PUBLISHED to EditStatusEnum"
```

---

### Task 2: Update CharCharacterService interface

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java`

- [ ] **Step 1: Remove draft-related methods from interface**

Update the file to remove these methods:
- `getDraft(Integer id)`
- `createDraft(CharCharacterDraftDto draft)`
- `saveDraft(Integer id, CharCharacterDraftDto draft)`
- `createDraftFromPublished(Integer id)`

Update the JavaDoc for remaining methods:

```java
package com.naon.grid.backend.service.character;

import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CharCharacterService {

    PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable);

    CharCharacterDto findById(Integer id);

    CharCharacterDto findPublishedById(Integer id);

    Integer create(CharCharacterDto resources);

    void update(Integer id, CharCharacterDto resources);

    void delete(Integer id);

    List<CharCharacterDto> searchByCharacter(String blurry);

    List<CharCharacterDto> searchPublishedByCharacter(String blurry);

    void reviewDraft(Integer id);

    void publishDraft(Integer id);

    void offline(Integer id);
}
```

- [ ] **Step 2: Commit the change**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java" && git commit -m "refactor: remove draft-related methods from CharCharacterService"
```

---

### Task 3: Refactor CharCharacterServiceImpl - create method

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Refactor the create method**

Update the `create` method to only save draftContent:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Integer create(CharCharacterDto resources) {
    CharCharacter charCharacter = new CharCharacter();
    charCharacter.setStatus(StatusEnum.ENABLED.getCode());
    charCharacter.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
    charCharacter.setEditStatus(EditStatusEnum.DRAFT.getCode());
    charCharacter.setDraftContent(JsonUtils.toJson(resources));
    charCharacter = charCharacterRepository.save(charCharacter);
    return charCharacter.getId();
}
```

- [ ] **Step 2: Commit the change**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java" && git commit -m "refactor: update create method to only save draftContent"
```

---

### Task 4: Refactor CharCharacterServiceImpl - update method

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Refactor the update method**

Update the `update` method to only update draftContent with state handling:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void update(Integer id, CharCharacterDto resources) {
    CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
    if (charCharacter.getId() == null || StatusEnum.DISABLED.getCode().equals(charCharacter.getStatus())) {
        throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
    }
    
    // If current status is REVIEWED or PUBLISHED, revert to DRAFT
    if (EditStatusEnum.REVIEWED.getCode().equals(charCharacter.getEditStatus()) || 
        EditStatusEnum.PUBLISHED.getCode().equals(charCharacter.getEditStatus())) {
        charCharacter.setEditStatus(EditStatusEnum.DRAFT.getCode());
    }
    
    charCharacter.setDraftContent(JsonUtils.toJson(resources));
    charCharacterRepository.save(charCharacter);
}
```

- [ ] **Step 2: Commit the change**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java" && git commit -m "refactor: update update method to use draftContent with state handling"
```

---

### Task 5: Refactor CharCharacterServiceImpl - findById method

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Refactor the findById method**

Update the `findById` method to return draftContent when appropriate:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public CharCharacterDto findById(Integer id) {
    if (id == null) {
        throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
    }
    CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
    if (charCharacter.getId() == null || StatusEnum.DISABLED.getCode().equals(charCharacter.getStatus())) {
        throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
    }
    
    // If in DRAFT or REVIEWED status, return draftContent
    if (EditStatusEnum.DRAFT.getCode().equals(charCharacter.getEditStatus()) || 
        EditStatusEnum.REVIEWED.getCode().equals(charCharacter.getEditStatus())) {
        if (charCharacter.getDraftContent() == null) {
            throw new BadRequestException("Draft content not found");
        }
        CharCharacterDto dto = JsonUtils.fromJson(charCharacter.getDraftContent(), CharCharacterDto.class);
        dto.setId(charCharacter.getId());
        dto.setStatus(charCharacter.getStatus());
        dto.setPublishStatus(charCharacter.getPublishStatus());
        dto.setEditStatus(charCharacter.getEditStatus());
        dto.setCreateBy(charCharacter.getCreateBy());
        dto.setUpdateBy(charCharacter.getUpdateBy());
        dto.setCreateTime(charCharacter.getCreateTime());
        dto.setUpdateTime(charCharacter.getUpdateTime());
        return dto;
    }
    
    // If in PUBLISHED status, return main table + child tables
    CharCharacterDto charCharacterDto = charCharacterMapper.toDto(charCharacter);
    charCharacterDto.setDiscriminations(convertToDiscriminationDtos(charDiscriminationRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())));
    charCharacterDto.setWords(convertToWordDtos(charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())));
    return charCharacterDto;
}
```

- [ ] **Step 2: Commit the change**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java" && git commit -m "refactor: update findById to return draftContent when appropriate"
```

---

### Task 6: Refactor CharCharacterServiceImpl - publishDraft method

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Refactor the publishDraft method**

Update the `publishDraft` method to set editStatus=PUBLISHED:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void publishDraft(Integer id) {
    CharCharacter charCharacter = charCharacterRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id)));

    if (StatusEnum.DISABLED.getCode().equals(charCharacter.getStatus())) {
        throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
    }

    if (charCharacter.getDraftContent() == null) {
        throw new BadRequestException("Draft content not found");
    }

    if (!EditStatusEnum.REVIEWED.getCode().equals(charCharacter.getEditStatus())) {
        throw new BadRequestException("Only reviewed drafts can be published");
    }

    // Parse draft content
    CharCharacterDto draftDto = JsonUtils.fromJson(charCharacter.getDraftContent(), CharCharacterDto.class);

    // Update main table fields
    charCharacter.setSequenceNo(draftDto.getSequenceNo());
    charCharacter.setCharacter(draftDto.getCharacter());
    charCharacter.setLevel(draftDto.getLevel());
    charCharacter.setPinyin(draftDto.getPinyin());
    charCharacter.setAudioId(draftDto.getAudioId());
    charCharacter.setTraditional(draftDto.getTraditional());
    charCharacter.setRadical(draftDto.getRadical());
    charCharacter.setStroke(draftDto.getStroke());
    charCharacter.setCharDesc(draftDto.getCharDesc());
    charCharacter.setDescTranslations(JsonUtils.toTranslationJson(draftDto.getDescTranslations()));

    // Update child tables
    syncDiscriminations(id, draftDto.getDiscriminations());
    syncWords(id, draftDto.getWords());

    // Update status
    charCharacter.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
    charCharacter.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
    charCharacter.setDraftContent(null);
    charCharacterRepository.save(charCharacter);
}
```

- [ ] **Step 2: Commit the change**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java" && git commit -m "refactor: update publishDraft to set editStatus=PUBLISHED"
```

---

### Task 7: Refactor CharCharacterServiceImpl - offline method

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Refactor the offline method**

Update the `offline` method to only set publishStatus=UNPUBLISHED:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void offline(Integer id) {
    CharCharacter charCharacter = charCharacterRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id)));

    if (StatusEnum.DISABLED.getCode().equals(charCharacter.getStatus())) {
        throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
    }

    // Only update publish status, don't change child tables
    charCharacter.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
    charCharacterRepository.save(charCharacter);
}
```

- [ ] **Step 2: Commit the change**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java" && git commit -m "refactor: update offline to only set publishStatus=UNPUBLISHED"
```

---

### Task 8: Refactor CharCharacterServiceImpl - delete method

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Refactor the delete method**

Update the `delete` method to only set status=DISABLED:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void delete(Integer id) {
    CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
    if (charCharacter.getId() == null) {
        throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
    }
    // Only set status to DISABLED, don't change child tables or publishStatus
    charCharacter.setStatus(StatusEnum.DISABLED.getCode());
    charCharacterRepository.save(charCharacter);
}
```

- [ ] **Step 2: Commit the change**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java" && git commit -m "refactor: update delete to only set status=DISABLED"
```

---

### Task 9: Remove draft methods from CharCharacterServiceImpl

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Remove these methods from the implementation:**
  - `getDraft(Integer id)`
  - `createDraft(CharCharacterDraftDto draft)`
  - `saveDraft(Integer id, CharCharacterDraftDto draft)`
  - `createDraftFromPublished(Integer id)`

- [ ] **Step 2: Commit the change**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java" && git commit -m "refactor: remove draft-related methods from CharCharacterServiceImpl"
```

---

### Task 10: Refactor CharCharacterController - remove draft endpoints

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java`

- [ ] **Step 1: Remove these endpoints from the controller:**
  - `GET /api/character/{id}/draft` - `getDraft` method
  - `POST /api/character/draft` - `createDraft` method
  - `PUT /api/character/{id}/draft` - `updateDraft` method
  - `POST /api/character/{id}/draft/from-published` - `createDraftFromPublished` method

- [ ] **Step 2: Also remove unused imports**

Remove these imports if they're no longer used:
- `com.naon.grid.backend.service.character.dto.CharCharacterDraftDto`

- [ ] **Step 3: Commit the change**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java" && git commit -m "refactor: remove draft endpoints from CharCharacterController"
```

---

### Task 11: Add design doc and plan to git

**Files:**
- Add: `docs/superpowers/specs/2026-06-02-char-character-workflow-refactor-design.md`
- Add: `docs/superpowers/plans/2026-06-02-char-character-workflow-refactor.md`

- [ ] **Step 1: Commit the docs**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && git add "docs/superpowers/specs/2026-06-02-char-character-workflow-refactor-design.md" "docs/superpowers/plans/2026-06-02-char-character-workflow-refactor.md" && git commit -m "docs: add character workflow refactor design and plan"
```

---

### Task 12: Verify compilation

**Files:** (no changes)

- [ ] **Step 1: Compile the project**

```bash
cd "C:\Users\nano\Desktop\nano-gemini" && mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Verify success and commit if any fixes needed**

(Only if compilation fixes were needed)

---

## Self-Review Checklist

**1. Spec coverage:**
- ✅ Add PUBLISHED to EditStatusEnum - Task 1
- ✅ Remove draft methods from Service interface - Task 2
- ✅ Refactor create to only save draftContent - Task 3
- ✅ Refactor update to save draftContent with state handling - Task 4
- ✅ Refactor findById to return draftContent when appropriate - Task 5
- ✅ Refactor publishDraft to set editStatus=PUBLISHED - Task 6
- ✅ Refactor offline to only set publishStatus=UNPUBLISHED - Task 7
- ✅ Refactor delete to only set status=DISABLED - Task 8
- ✅ Remove draft methods from Service impl - Task 9
- ✅ Remove draft endpoints from Controller - Task 10
- ✅ Commit design docs - Task 11
- ✅ Verify compilation - Task 12

**2. Placeholder scan:** No placeholders found - all steps have complete code and commands

**3. Type consistency:**
- All method signatures match
- Enum names are consistent (PUBLISHED, DRAFT, REVIEWED)
- Using CharCharacterDto consistently (not CharCharacterDraftDto)
