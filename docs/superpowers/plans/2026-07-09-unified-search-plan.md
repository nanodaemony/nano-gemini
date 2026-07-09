# Unified Search API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a single `GET /api/app/search?q=` endpoint that searches vocab, characters, grammar, and comparisons in one call, grouped by type.

**Architecture:** New `AppSearchController` calls existing services for vocab/character/grammar, and two new fuzzy-search service methods for vocab/grammar comparisons. A new `AppSearchWrapper` assembles results into `AppSearchResultVO`. Bottom-up: repositories → services → VOs → wrapper → controller.

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Java 8, Lombok, Fastjson2

## Global Constraints

- Java 8 (no `var`, no records, no `List.of`)
- Follow existing Wrapper pattern: Controller delegates to Wrapper for all DTO→VO mapping
- Follow `@AnonymousGetMapping` for public endpoints
- Keyword empty → return four empty arrays (HTTP 200)
- Each module max 20 results
- Existing 4 search endpoints remain untouched

---

### Task 1: Add fuzzy query methods to comparison item repositories

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabcomparison/VocabComparisonItemRepository.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/grammarcomparison/GrammarComparisonItemRepository.java`

**Interfaces:**
- Produces: `VocabComparisonItemRepository.findByWordContainingAndStatus(String word, Integer status)` → `List<VocabComparisonItem>`
- Produces: `GrammarComparisonItemRepository.findByGrammarNameContainingAndStatus(String name, Integer status)` → `List<GrammarComparisonItem>`

- [ ] **Step 1: Add `findByWordContainingAndStatus` to VocabComparisonItemRepository**

Open `grid-system/src/main/java/com/naon/grid/backend/repo/vocabcomparison/VocabComparisonItemRepository.java` and add one method after the existing `findByWordAndStatus`:

```java
List<VocabComparisonItem> findByWordContainingAndStatus(String word, Integer status);
```

The file should look like:

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

    List<VocabComparisonItem> findByWordContainingAndStatus(String word, Integer status);
}
```

- [ ] **Step 2: Add `findByGrammarNameContainingAndStatus` to GrammarComparisonItemRepository**

Open `grid-system/src/main/java/com/naon/grid/backend/repo/grammarcomparison/GrammarComparisonItemRepository.java` and add one method after the existing `findByGrammarNameAndStatus`:

```java
List<GrammarComparisonItem> findByGrammarNameContainingAndStatus(String grammarName, Integer status);
```

The file should look like:

```java
package com.naon.grid.backend.repo.grammarcomparison;

import com.naon.grid.backend.domain.grammarcomparison.GrammarComparisonItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrammarComparisonItemRepository extends JpaRepository<GrammarComparisonItem, Long>,
        JpaSpecificationExecutor<GrammarComparisonItem> {

    List<GrammarComparisonItem> findByGroupIdAndStatus(Long groupId, Integer status);

    List<GrammarComparisonItem> findByGroupIdInAndStatus(List<Long> groupIds, Integer status);

    List<GrammarComparisonItem> findByGrammarNameAndStatus(String grammarName, Integer status);

    List<GrammarComparisonItem> findByGrammarIdAndStatus(Long grammarId, Integer status);

    List<GrammarComparisonItem> findByGrammarNameContainingAndStatus(String grammarName, Integer status);
}
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/vocabcomparison/VocabComparisonItemRepository.java
git add grid-system/src/main/java/com/naon/grid/backend/repo/grammarcomparison/GrammarComparisonItemRepository.java
git commit -m "feat: add fuzzy query methods to comparison item repositories"
```

---

### Task 2: Add fuzzy search methods to comparison group services

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/VocabComparisonGroupService.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/impl/VocabComparisonGroupServiceImpl.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammarcomparison/GrammarComparisonGroupService.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammarcomparison/impl/GrammarComparisonGroupServiceImpl.java`

**Interfaces:**
- Consumes: `VocabComparisonItemRepository.findByWordContainingAndStatus` (from Task 1)
- Consumes: `GrammarComparisonItemRepository.findByGrammarNameContainingAndStatus` (from Task 1)
- Produces: `VocabComparisonGroupService.searchByWordFuzzy(String word, int limit)` → `List<VocabComparisonGroupDto>`
- Produces: `GrammarComparisonGroupService.searchByGrammarNameFuzzy(String name, int limit)` → `List<GrammarComparisonGroupDto>`

- [ ] **Step 1: Add interface method to VocabComparisonGroupService**

Open `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/VocabComparisonGroupService.java`. Add after `searchByWordId`:

```java
/**
 * 根据词汇文本模糊搜索已发布的辨析组（LIKE %word%），最多返回 limit 条
 */
List<VocabComparisonGroupDto> searchByWordFuzzy(String word, int limit);
```

- [ ] **Step 2: Implement searchByWordFuzzy in VocabComparisonGroupServiceImpl**

Open `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/impl/VocabComparisonGroupServiceImpl.java`. Add this method after `searchByWordId` (after line 257):

```java
@Override
public List<VocabComparisonGroupDto> searchByWordFuzzy(String word, int limit) {
    List<VocabComparisonItem> items = itemRepository.findByWordContainingAndStatus(
            word, StatusEnum.ENABLED.getCode());
    if (items == null || items.isEmpty()) {
        return Collections.emptyList();
    }
    List<Long> groupIds = items.stream()
            .map(VocabComparisonItem::getGroupId)
            .distinct()
            .collect(Collectors.toList());
    List<VocabComparisonGroup> groups = groupRepository.findAllById(groupIds);
    List<VocabComparisonGroupDto> result = new ArrayList<>();
    for (VocabComparisonGroup group : groups) {
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(group.getPublishStatus())) {
            continue;
        }
        if (StatusEnum.DISABLED.getCode().equals(group.getStatus())) {
            continue;
        }
        VocabComparisonGroupDto dto = toBaseDto(group);
        dto.setItems(loadItems(group.getId()));
        dto.setChats(loadChats(group.getId()));
        result.add(dto);
        if (result.size() >= limit) {
            break;
        }
    }
    return result;
}
```

- [ ] **Step 3: Add interface method to GrammarComparisonGroupService**

Open `grid-system/src/main/java/com/naon/grid/backend/service/grammarcomparison/GrammarComparisonGroupService.java`. Add after `searchByGrammarId`:

```java
/**
 * 根据语法点名称模糊搜索已发布的辨析组（LIKE %name%），最多返回 limit 条
 */
List<GrammarComparisonGroupDto> searchByGrammarNameFuzzy(String name, int limit);
```

- [ ] **Step 4: Implement searchByGrammarNameFuzzy in GrammarComparisonGroupServiceImpl**

Open `grid-system/src/main/java/com/naon/grid/backend/service/grammarcomparison/impl/GrammarComparisonGroupServiceImpl.java`. Add this method after `searchByGrammarId` (after line 261):

```java
@Override
public List<GrammarComparisonGroupDto> searchByGrammarNameFuzzy(String name, int limit) {
    List<GrammarComparisonItem> items = itemRepository.findByGrammarNameContainingAndStatus(
            name, StatusEnum.ENABLED.getCode());
    if (items == null || items.isEmpty()) {
        return Collections.emptyList();
    }
    List<Long> groupIds = items.stream()
            .map(GrammarComparisonItem::getGroupId)
            .distinct()
            .collect(Collectors.toList());
    List<GrammarComparisonGroup> groups = groupRepository.findAllById(groupIds);
    List<GrammarComparisonGroupDto> result = new ArrayList<>();
    for (GrammarComparisonGroup group : groups) {
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(group.getPublishStatus())) {
            continue;
        }
        if (StatusEnum.DISABLED.getCode().equals(group.getStatus())) {
            continue;
        }
        GrammarComparisonGroupDto dto = toBaseDto(group);
        dto.setItems(loadItems(group.getId()));
        dto.setChats(loadChats(group.getId()));
        result.add(dto);
        if (result.size() >= limit) {
            break;
        }
    }
    return result;
}
```

- [ ] **Step 5: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/VocabComparisonGroupService.java
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/impl/VocabComparisonGroupServiceImpl.java
git add grid-system/src/main/java/com/naon/grid/backend/service/grammarcomparison/GrammarComparisonGroupService.java
git add grid-system/src/main/java/com/naon/grid/backend/service/grammarcomparison/impl/GrammarComparisonGroupServiceImpl.java
git commit -m "feat: add fuzzy search methods to comparison group services"
```

---

### Task 3: Create search result VOs

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppSearchResultVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppComparisonGroupVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppComparisonItemVO.java`

**Interfaces:**
- Produces: `AppSearchResultVO` with fields `vocab`, `character`, `grammar`, `comparison`
- Produces: `AppComparisonGroupVO` with fields `groupId`, `groupKey`, `type`, `items`
- Produces: `AppComparisonItemVO` with fields `wordId`, `word`, `grammarId`, `grammarName`

- [ ] **Step 1: Create AppSearchResultVO**

Create `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppSearchResultVO.java`:

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppSearchResultVO implements Serializable {

    @ApiModelProperty(value = "词汇搜索结果")
    private List<AppVocabWordBaseVO> vocab;

    @ApiModelProperty(value = "汉字搜索结果")
    private List<AppCharCharacterBaseVO> character;

    @ApiModelProperty(value = "语法搜索结果")
    private List<AppGrammarPointBaseVO> grammar;

    @ApiModelProperty(value = "辨析搜索结果")
    private List<AppComparisonGroupVO> comparison;
}
```

- [ ] **Step 2: Create AppComparisonGroupVO**

Create `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppComparisonGroupVO.java`:

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppComparisonGroupVO implements Serializable {

    @ApiModelProperty(value = "辨析组ID")
    private Long groupId;

    @ApiModelProperty(value = "辨析组标识")
    private String groupKey;

    @ApiModelProperty(value = "辨析类型：vocab / grammar")
    private String type;

    @ApiModelProperty(value = "条目列表")
    private List<AppComparisonItemVO> items;
}
```

- [ ] **Step 3: Create AppComparisonItemVO**

Create `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppComparisonItemVO.java`:

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppComparisonItemVO implements Serializable {

    @ApiModelProperty(value = "词汇ID（词汇辨析）")
    private Long wordId;

    @ApiModelProperty(value = "词汇（词汇辨析）")
    private String word;

    @ApiModelProperty(value = "语法点ID（语法辨析）")
    private Long grammarId;

    @ApiModelProperty(value = "语法点名称（语法辨析）")
    private String grammarName;
}
```

- [ ] **Step 4: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppSearchResultVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppComparisonGroupVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppComparisonItemVO.java
git commit -m "feat: add unified search result VOs"
```

---

### Task 4: Create AppSearchWrapper

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppSearchWrapper.java`

**Interfaces:**
- Consumes: `AppSearchResultVO`, `AppComparisonGroupVO`, `AppComparisonItemVO` (from Task 3)
- Consumes: `VocabComparisonGroupDto`, `VocabComparisonItemDto` (existing)
- Consumes: `GrammarComparisonGroupDto`, `GrammarComparisonItemDto` (existing)
- Produces: `AppSearchWrapper.toResultVO(...)` → `AppSearchResultVO`
- Produces: `AppSearchWrapper.toComparisonGroupVO(VocabComparisonGroupDto, ...)` → `AppComparisonGroupVO`
- Produces: `AppSearchWrapper.toComparisonGroupVO(GrammarComparisonGroupDto, ...)` → `AppComparisonGroupVO`
- Produces: `AppSearchWrapper.toVocabComparisonItemVOList(...)` → `List<AppComparisonItemVO>`
- Produces: `AppSearchWrapper.toGrammarComparisonItemVOList(...)` → `List<AppComparisonItemVO>`

- [ ] **Step 1: Create AppSearchWrapper**

Create `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppSearchWrapper.java`:

```java
package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonItemDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonItemDto;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterBaseVO;
import com.naon.grid.modules.app.rest.vo.AppComparisonGroupVO;
import com.naon.grid.modules.app.rest.vo.AppComparisonItemVO;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointBaseVO;
import com.naon.grid.modules.app.rest.vo.AppSearchResultVO;
import com.naon.grid.modules.app.rest.vo.AppVocabWordBaseVO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户端统一搜索包装器
 */
public class AppSearchWrapper {

    /**
     * 组装搜索结果为统一 VO
     */
    public static AppSearchResultVO toResultVO(
            List<AppVocabWordBaseVO> vocab,
            List<AppCharCharacterBaseVO> character,
            List<AppGrammarPointBaseVO> grammar,
            List<AppComparisonGroupVO> comparison) {
        AppSearchResultVO vo = new AppSearchResultVO();
        vo.setVocab(vocab != null ? vocab : Collections.emptyList());
        vo.setCharacter(character != null ? character : Collections.emptyList());
        vo.setGrammar(grammar != null ? grammar : Collections.emptyList());
        vo.setComparison(comparison != null ? comparison : Collections.emptyList());
        return vo;
    }

    /**
     * 词汇辨析组 → 精简 VO
     */
    public static AppComparisonGroupVO toComparisonGroupVO(
            VocabComparisonGroupDto dto, String type, List<AppComparisonItemVO> items) {
        AppComparisonGroupVO vo = new AppComparisonGroupVO();
        vo.setGroupId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setType(type);
        vo.setItems(items);
        return vo;
    }

    /**
     * 语法辨析组 → 精简 VO
     */
    public static AppComparisonGroupVO toComparisonGroupVO(
            GrammarComparisonGroupDto dto, String type, List<AppComparisonItemVO> items) {
        AppComparisonGroupVO vo = new AppComparisonGroupVO();
        vo.setGroupId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setType(type);
        vo.setItems(items);
        return vo;
    }

    /**
     * 词汇辨析条目 → 精简 VO 列表（仅 wordId + word）
     */
    public static List<AppComparisonItemVO> toVocabComparisonItemVOList(List<VocabComparisonItemDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(AppSearchWrapper::toVocabComparisonItemVO).collect(Collectors.toList());
    }

    private static AppComparisonItemVO toVocabComparisonItemVO(VocabComparisonItemDto dto) {
        AppComparisonItemVO vo = new AppComparisonItemVO();
        vo.setWordId(dto.getWordId());
        vo.setWord(dto.getWord());
        return vo;
    }

    /**
     * 语法辨析条目 → 精简 VO 列表（仅 grammarId + grammarName）
     */
    public static List<AppComparisonItemVO> toGrammarComparisonItemVOList(List<GrammarComparisonItemDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(AppSearchWrapper::toGrammarComparisonItemVO).collect(Collectors.toList());
    }

    private static AppComparisonItemVO toGrammarComparisonItemVO(GrammarComparisonItemDto dto) {
        AppComparisonItemVO vo = new AppComparisonItemVO();
        vo.setGrammarId(dto.getGrammarId());
        vo.setGrammarName(dto.getGrammarName());
        return vo;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppSearchWrapper.java
git commit -m "feat: add AppSearchWrapper for unified search"
```

---

### Task 5: Create AppSearchController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppSearchController.java`

**Interfaces:**
- Consumes: `VocabWordService.queryAll`, `CharCharacterService.searchPublishedByCharacter`, `GrammarPointService.searchPublished`, `VocabComparisonGroupService.searchByWordFuzzy`, `GrammarComparisonGroupService.searchByGrammarNameFuzzy`, `VocabOutlineRecordService.recordIfNeeded` (all existing, injected via constructor)
- Consumes: `AppSearchWrapper` (from Task 4)
- Consumes: `AppVocabWordWrapper.toBaseVOList`, `AppCharCharacterWrapper.toBaseVOList`, `AppGrammarPointWrapper.toBaseVOList` (existing, static calls)
- Produces: `GET /api/app/search?q=` → `ResponseEntity<AppSearchResultVO>`

- [ ] **Step 1: Create AppSearchController**

Create `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppSearchController.java`:

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.modules.app.rest.vo.AppCharCharacterBaseVO;
import com.naon.grid.modules.app.rest.vo.AppComparisonGroupVO;
import com.naon.grid.modules.app.rest.vo.AppComparisonItemVO;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointBaseVO;
import com.naon.grid.modules.app.rest.vo.AppSearchResultVO;
import com.naon.grid.modules.app.rest.vo.AppVocabWordBaseVO;
import com.naon.grid.modules.app.rest.wrapper.AppCharCharacterWrapper;
import com.naon.grid.modules.app.rest.wrapper.AppGrammarPointWrapper;
import com.naon.grid.modules.app.rest.wrapper.AppSearchWrapper;
import com.naon.grid.modules.app.rest.wrapper.AppVocabWordWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app")
@Api(tags = "用户：统一搜索")
public class AppSearchController {

    private final VocabWordService vocabWordService;
    private final CharCharacterService charCharacterService;
    private final GrammarPointService grammarPointService;
    private final VocabComparisonGroupService vocabComparisonGroupService;
    private final GrammarComparisonGroupService grammarComparisonGroupService;
    private final VocabOutlineRecordService vocabOutlineRecordService;

    @ApiOperation("统一搜索（词汇/汉字/语法/辨析）")
    @AnonymousGetMapping("/search")
    public ResponseEntity<AppSearchResultVO> search(@RequestParam(required = false) String q) {
        if (q == null || q.trim().isEmpty()) {
            return new ResponseEntity<>(
                    AppSearchWrapper.toResultVO(
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList()),
                    HttpStatus.OK);
        }

        String keyword = q.trim();

        List<AppVocabWordBaseVO> vocab = searchVocab(keyword);
        List<AppCharCharacterBaseVO> character = searchCharacter(keyword);
        List<AppGrammarPointBaseVO> grammar = searchGrammar(keyword);
        List<AppComparisonGroupVO> comparison = searchComparison(keyword);

        return new ResponseEntity<>(
                AppSearchWrapper.toResultVO(vocab, character, grammar, comparison),
                HttpStatus.OK);
    }

    private List<AppVocabWordBaseVO> searchVocab(String keyword) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(keyword);
        criteria.setPublishStatus("published");
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"));
        List<VocabWordDto> dtos = vocabWordService.queryAll(criteria, pageable).getContent();
        List<AppVocabWordBaseVO> vos = AppVocabWordWrapper.toBaseVOList(dtos);

        if (vos.isEmpty()) {
            vocabOutlineRecordService.recordIfNeeded(keyword);
        }

        return vos;
    }

    private List<AppCharCharacterBaseVO> searchCharacter(String keyword) {
        List<CharCharacterDto> dtos = charCharacterService.searchPublishedByCharacter(keyword);
        if (dtos.size() > 20) {
            dtos = dtos.subList(0, 20);
        }
        return AppCharCharacterWrapper.toBaseVOList(dtos);
    }

    private List<AppGrammarPointBaseVO> searchGrammar(String keyword) {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id"));
        List<GrammarPointDto> dtos = grammarPointService.searchPublished(keyword, pageable).getContent();
        return AppGrammarPointWrapper.toBaseVOList(dtos);
    }

    private List<AppComparisonGroupVO> searchComparison(String keyword) {
        List<AppComparisonGroupVO> result = new ArrayList<>();

        // 词汇辨析
        List<VocabComparisonGroupDto> vocabGroups = vocabComparisonGroupService.searchByWordFuzzy(keyword, 20);
        for (VocabComparisonGroupDto dto : vocabGroups) {
            List<AppComparisonItemVO> items = AppSearchWrapper.toVocabComparisonItemVOList(dto.getItems());
            result.add(AppSearchWrapper.toComparisonGroupVO(dto, "vocab", items));
        }

        // 语法辨析
        List<GrammarComparisonGroupDto> grammarGroups = grammarComparisonGroupService.searchByGrammarNameFuzzy(keyword, 20);
        for (GrammarComparisonGroupDto dto : grammarGroups) {
            List<AppComparisonItemVO> items = AppSearchWrapper.toGrammarComparisonItemVOList(dto.getItems());
            result.add(AppSearchWrapper.toComparisonGroupVO(dto, "grammar", items));
        }

        if (result.size() > 20) {
            return result.subList(0, 20);
        }
        return result;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppSearchController.java
git commit -m "feat: add unified search controller"
```

---

### Task 6: Build and verify

**Files:**
- No new files — verify phase only

- [ ] **Step 1: Build the project**

```bash
cd grid-bootstrap && mvn compile -DskipTests -q
```

Expected: BUILD SUCCESS with no compilation errors.

- [ ] **Step 2: Start the application and verify the endpoint**

Start the application and test with curl:

```bash
# Test: empty keyword
curl -s http://localhost:8000/api/app/search | python -m json.tool
# Expected: {"vocab":[],"character":[],"grammar":[],"comparison":[]}

# Test: keyword with space
curl -s "http://localhost:8000/api/app/search?q=%20" | python -m json.tool
# Expected: {"vocab":[],"character":[],"grammar":[],"comparison":[]}

# Test: valid search
curl -s "http://localhost:8000/api/app/search?q=花" | python -m json.tool
# Expected: JSON with vocab, character, grammar, comparison arrays populated according to data
```

- [ ] **Step 3: Verify existing endpoints still work**

```bash
curl -s "http://localhost:8000/api/app/vocab/search?blurry=花" | python -m json.tool
curl -s "http://localhost:8000/api/app/character/search?blurry=花" | python -m json.tool
curl -s "http://localhost:8000/api/app/grammar/search?keyword=花" | python -m json.tool
curl -s "http://localhost:8000/api/app/vocab/comparison/search?word=花" | python -m json.tool
```

All four existing endpoints should continue to return results as before.

- [ ] **Step 4: Final commit (if any verification tweaks needed)**

```bash
git status
# If clean, no commit needed. If fixes were applied:
git add -A && git commit -m "chore: verification fixes for unified search"
```
