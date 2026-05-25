# 汉字更新接口 Diff 优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `PUT /api/character/{id}` 从删除重建子表改为基于提交全量数据的 diff 增改删更新。

**Architecture:** Controller 保持接口契约不变，只补齐子项 ID 入参到 DTO 的映射。`CharCharacterServiceImpl.update()` 负责主表更新和两个子表的 diff 同步；子项 ID 重复或不属于当前汉字时抛出 `BadRequestException` 并回滚事务。

**Tech Stack:** Java 8, Spring Boot 2.7.18, Spring Data JPA, Maven, Lombok.

**Important:** 用户明确要求代码不要 commit，完成后等待 review。本计划不包含 git commit 步骤。

---

## File Structure

- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`
  - 给 `CharDiscriminationRequest` 和 `CharWordRequest` 增加 `id` 字段，使更新接口能表达已有子项。
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java`
  - 子项 request 转 DTO 时带上 `id`。
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`
  - 引入 `BadRequestException`、集合工具类型。
  - 将 `update()` 中的 `deleteChildren(id); saveChildren(resources, id);` 替换为辨析和组词的 diff 同步。
  - 保留新增接口的 `saveChildren()` 行为。
- No repository changes expected.
- No database schema changes.

---

### Task 1: Add child item IDs to update request mapping

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java`

- [ ] **Step 1: Add `id` to discrimination request**

In `CharCharacterCreateRequest.CharDiscriminationRequest`, add this field before `discrimChar`:

```java
@ApiModelProperty(value = "辨析唯一ID")
private Integer id;
```

The nested class should start like this:

```java
@Getter
@Setter
public static class CharDiscriminationRequest implements Serializable {
    @ApiModelProperty(value = "辨析唯一ID")
    private Integer id;

    @ApiModelProperty(value = "辨析汉字")
    private String discrimChar;

    @ApiModelProperty(value = "辨析拼音")
    private String discrimPinyin;

    @ApiModelProperty(value = "辨析汉字翻译")
    private String discrimCharTranslations;

    @ApiModelProperty(value = "对比翻译")
    private String comparisonTranslations;
}
```

- [ ] **Step 2: Add `id` to word request**

In `CharCharacterCreateRequest.CharWordRequest`, add this field before `wordItem`:

```java
@ApiModelProperty(value = "组词唯一ID")
private Integer id;
```

The nested class should start like this:

```java
@Getter
@Setter
public static class CharWordRequest implements Serializable {
    @ApiModelProperty(value = "组词唯一ID")
    private Integer id;

    @ApiModelProperty(value = "组词")
    private String wordItem;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "组词翻译")
    private String wordItemTranslations;

    @ApiModelProperty(value = "例句")
    private String exampleSentence;

    @ApiModelProperty(value = "例句拼音")
    private String examplePinyin;

    @ApiModelProperty(value = "例句翻译")
    private String exampleTranslations;

    @ApiModelProperty(value = "例句图片")
    private String exampleImage;
}
```

- [ ] **Step 3: Map discrimination request ID to DTO**

In `CharCharacterController.toDiscriminationDto(...)`, set the ID before other fields:

```java
private CharDiscriminationDto toDiscriminationDto(CharCharacterCreateRequest.CharDiscriminationRequest request) {
    CharDiscriminationDto dto = new CharDiscriminationDto();
    dto.setId(request.getId());
    dto.setDiscrimChar(request.getDiscrimChar());
    dto.setDiscrimPinyin(request.getDiscrimPinyin());
    dto.setDiscrimCharTranslations(request.getDiscrimCharTranslations());
    dto.setComparisonTranslations(request.getComparisonTranslations());
    return dto;
}
```

- [ ] **Step 4: Map word request ID to DTO**

In `CharCharacterController.toWordDto(...)`, set the ID before other fields:

```java
private CharWordDto toWordDto(CharCharacterCreateRequest.CharWordRequest request) {
    CharWordDto dto = new CharWordDto();
    dto.setId(request.getId());
    dto.setWordItem(request.getWordItem());
    dto.setLevel(request.getLevel());
    dto.setPinyin(request.getPinyin());
    dto.setPartOfSpeech(request.getPartOfSpeech());
    dto.setWordItemTranslations(request.getWordItemTranslations());
    dto.setExampleSentence(request.getExampleSentence());
    dto.setExamplePinyin(request.getExamplePinyin());
    dto.setExampleTranslations(request.getExampleTranslations());
    dto.setExampleImage(request.getExampleImage());
    return dto;
}
```

- [ ] **Step 5: Compile to verify request and mapping changes**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: build reaches `BUILD SUCCESS` or fails only on pre-existing environment/config issues unrelated to these type changes. If Java compile fails for missing getters, check Lombok field placement and imports.

---

### Task 2: Implement diff update for discriminations

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Update imports**

Replace current collection imports:

```java
import java.util.ArrayList;
import java.util.List;
```

with:

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
```

Add the exception import:

```java
import com.naon.grid.exception.BadRequestException;
```

The top imports should include both exceptions:

```java
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
```

- [ ] **Step 2: Add `syncDiscriminations` method**

Add this private method after `deleteChildren(Integer charId)`:

```java
private void syncDiscriminations(Integer charId, List<CharDiscriminationDto> submittedDtos) {
    List<CharDiscriminationDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
    List<CharDiscrimination> existing = charDiscriminationRepository.findByCharId(charId);
    Map<Integer, CharDiscrimination> existingMap = new HashMap<>();
    for (CharDiscrimination discrimination : existing) {
        existingMap.put(discrimination.getId(), discrimination);
    }

    Set<Integer> submittedIds = new HashSet<>();
    List<CharDiscrimination> toSave = new ArrayList<>();

    for (CharDiscriminationDto dto : submitted) {
        if (dto.getId() == null) {
            toSave.add(convertToDiscriminationEntity(dto, charId));
            continue;
        }
        if (!submittedIds.add(dto.getId())) {
            throw new BadRequestException("辨析ID重复: " + dto.getId());
        }
        CharDiscrimination discrimination = existingMap.get(dto.getId());
        if (discrimination == null) {
            throw new BadRequestException("辨析ID不属于当前汉字: " + dto.getId());
        }
        updateDiscrimination(discrimination, dto);
        toSave.add(discrimination);
    }

    List<CharDiscrimination> toDelete = new ArrayList<>();
    for (CharDiscrimination discrimination : existing) {
        if (!submittedIds.contains(discrimination.getId())) {
            toDelete.add(discrimination);
        }
    }

    charDiscriminationRepository.deleteAll(toDelete);
    charDiscriminationRepository.saveAll(toSave);
}
```

- [ ] **Step 3: Add `updateDiscrimination` method**

Add this method near `convertToDiscriminationEntity(...)`:

```java
private void updateDiscrimination(CharDiscrimination entity, CharDiscriminationDto dto) {
    entity.setDiscrimChar(dto.getDiscrimChar());
    entity.setDiscrimPinyin(dto.getDiscrimPinyin());
    entity.setDiscrimCharTranslations(dto.getDiscrimCharTranslations());
    entity.setComparisonTranslations(dto.getComparisonTranslations());
}
```

- [ ] **Step 4: Compile to verify discrimination sync code**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: `BUILD SUCCESS`. If `saveAll` or `deleteAll` type inference fails, confirm repository extends `JpaRepository<CharDiscrimination, Integer>`.

---

### Task 3: Implement diff update for words

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Add `syncWords` method**

Add this private method after `syncDiscriminations(...)`:

```java
private void syncWords(Integer charId, List<CharWordDto> submittedDtos) {
    List<CharWordDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
    List<CharWord> existing = charWordRepository.findByCharId(charId);
    Map<Integer, CharWord> existingMap = new HashMap<>();
    for (CharWord word : existing) {
        existingMap.put(word.getId(), word);
    }

    Set<Integer> submittedIds = new HashSet<>();
    List<CharWord> toSave = new ArrayList<>();

    for (CharWordDto dto : submitted) {
        if (dto.getId() == null) {
            toSave.add(convertToWordEntity(dto, charId));
            continue;
        }
        if (!submittedIds.add(dto.getId())) {
            throw new BadRequestException("组词ID重复: " + dto.getId());
        }
        CharWord word = existingMap.get(dto.getId());
        if (word == null) {
            throw new BadRequestException("组词ID不属于当前汉字: " + dto.getId());
        }
        updateWord(word, dto);
        toSave.add(word);
    }

    List<CharWord> toDelete = new ArrayList<>();
    for (CharWord word : existing) {
        if (!submittedIds.contains(word.getId())) {
            toDelete.add(word);
        }
    }

    charWordRepository.deleteAll(toDelete);
    charWordRepository.saveAll(toSave);
}
```

- [ ] **Step 2: Add `updateWord` method**

Add this method near `convertToWordEntity(...)`:

```java
private void updateWord(CharWord entity, CharWordDto dto) {
    entity.setWordItem(dto.getWordItem());
    entity.setLevel(dto.getLevel());
    entity.setPinyin(dto.getPinyin());
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setWordItemTranslations(dto.getWordItemTranslations());
    entity.setExampleSentence(dto.getExampleSentence());
    entity.setExamplePinyin(dto.getExamplePinyin());
    entity.setExampleTranslations(dto.getExampleTranslations());
    entity.setExampleImage(dto.getExampleImage());
}
```

- [ ] **Step 3: Compile to verify word sync code**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: `BUILD SUCCESS`.

---

### Task 4: Wire diff sync into update flow

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Replace delete/reinsert logic in `update`**

In `CharCharacterServiceImpl.update(Integer id, CharCharacterDto resources)`, replace:

```java
charCharacterRepository.save(charCharacter);
deleteChildren(id);
saveChildren(resources, id);
```

with:

```java
charCharacterRepository.save(charCharacter);
syncDiscriminations(id, resources.getDiscriminations());
syncWords(id, resources.getWords());
```

The full method should be:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void update(Integer id, CharCharacterDto resources) {
    CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
    if (charCharacter.getId() == null) {
        throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
    }
    charCharacter.setSequenceNo(resources.getSequenceNo());
    charCharacter.setCharacter(resources.getCharacter());
    charCharacter.setLevel(resources.getLevel());
    charCharacter.setPinyin(resources.getPinyin());
    charCharacter.setAudioId(resources.getAudioId());
    charCharacter.setTraditional(resources.getTraditional());
    charCharacter.setRadical(resources.getRadical());
    charCharacter.setStroke(resources.getStroke());
    charCharacter.setCharDesc(resources.getCharDesc());
    charCharacter.setDescTranslations(resources.getDescTranslations());
    charCharacterRepository.save(charCharacter);
    syncDiscriminations(id, resources.getDiscriminations());
    syncWords(id, resources.getWords());
}
```

- [ ] **Step 2: Compile full module graph**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: `BUILD SUCCESS`.

---

### Task 5: Manual verification scenarios

**Files:**
- No code changes expected.

- [ ] **Step 1: Review generated diff before manual testing**

Run:

```bash
git diff -- grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java docs/superpowers/specs/2026-05-25-char-character-update-diff-design.md docs/superpowers/plans/2026-05-25-char-character-update-diff.md
```

Expected: only planned request, controller mapping, service diff logic, and docs changes appear.

- [ ] **Step 2: Verify update keeps child IDs when unchanged**

Use an existing character with at least one discrimination and one word. Send `PUT /api/character/{id}` with the same child IDs and changed main-table fields.

Expected:

- Response status: `204`.
- `GET /api/character/{id}` returns updated main fields.
- Existing child IDs are unchanged.

- [ ] **Step 3: Verify child insert**

Send `PUT /api/character/{id}` with existing child rows plus one discrimination without `id` and one word without `id`.

Expected:

- Response status: `204`.
- `GET /api/character/{id}` shows newly created child rows with generated IDs.
- Previously submitted existing child IDs remain unchanged.

- [ ] **Step 4: Verify child update**

Send `PUT /api/character/{id}` with an existing discrimination ID but changed `comparisonTranslations`, and an existing word ID but changed `exampleSentence`.

Expected:

- Response status: `204`.
- `GET /api/character/{id}` shows updated child content under the same IDs.

- [ ] **Step 5: Verify child delete**

Send `PUT /api/character/{id}` omitting one existing discrimination ID and one existing word ID.

Expected:

- Response status: `204`.
- `GET /api/character/{id}` no longer contains omitted child rows.

- [ ] **Step 6: Verify invalid child ID rolls back**

Send `PUT /api/character/{id}` with a child ID from another character or a non-existent child ID.

Expected:

- Response status: `400`.
- Error message contains `辨析ID不属于当前汉字` or `组词ID不属于当前汉字`.
- Main table and child tables remain unchanged from before the request.

- [ ] **Step 7: Verify duplicate child ID rolls back**

Send `PUT /api/character/{id}` with the same discrimination ID twice, then repeat with the same word ID twice.

Expected:

- Response status: `400`.
- Error message contains `辨析ID重复` or `组词ID重复`.
- Main table and child tables remain unchanged from before the request.

- [ ] **Step 8: Verify null child lists delete all children**

Send `PUT /api/character/{id}` with `discriminations: null` or without the field, and `words: null` or without the field.

Expected:

- Response status: `204`.
- The corresponding child list is empty in `GET /api/character/{id}`.

---

## Self-Review

- Spec coverage: The plan covers path-ID based update, main-table content update, child insert/update/delete diff, null-as-empty child lists, duplicate ID errors, invalid ownership errors, transaction rollback expectations, and no API contract change except accepting child IDs in request bodies.
- Placeholder scan: No placeholder implementation steps remain.
- Type consistency: Request child IDs are `Integer`, matching entity and DTO IDs. Service methods use `CharDiscriminationDto`, `CharWordDto`, `CharDiscrimination`, and `CharWord` consistently.
- User constraint: No commit steps are included because the user requested review before commit.
