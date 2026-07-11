# 汉字大挑战 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement three anonymous GET endpoints for the "汉字大挑战" game module that each return 10 quiz questions with answers and single-language explanations.

**Architecture:** Follows existing project layers — `grid-common` enum → `grid-system` repository/DTO/service → `grid-app` VO/wrapper/controller. Separates DTO (multi-language raw data) from VO (single-language filtered output) with a static Wrapper doing the language filtering, consistent with `AppCharCharacterWrapper` pattern.

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Java 8, Lombok, Fastjson2

## Global Constraints

- Java 8 — no `var`, no records, no text blocks, no `List.of()`, no Stream.toList()
- All game endpoints use `@AnonymousGetMapping` (anonymous access)
- `language` parameter is required, validated non-empty
- Each endpoint returns exactly 10 questions; fewer if data insufficient (no error thrown)
- Service DTO holds `List<TextTranslation>` (all languages); Wrapper produces VO with single `TextTranslationVO`
- No user state tracking, no database writes from game APIs
- Follow existing naming conventions: `Char*` prefix for character domain, `App*` prefix for app-layer classes
- MySQL native queries use backtick-quoted identifiers for reserved words

---

## File Structure

| File | Module | Create/Modify | Responsibility |
|------|--------|---------------|----------------|
| `grid-common/.../enums/HskLevelRange.java` | grid-common | Create | Map semantic level keys to HSK level code lists |
| `grid-system/.../repo/character/CharCharacterRepository.java` | grid-system | Modify | Add 2 native random-sampling queries |
| `grid-system/.../repo/character/CharComparisonRepository.java` | grid-system | Modify | Add 2 queries (random enabled + batch by charIds) |
| `grid-system/.../repo/character/CharWordRepository.java` | grid-system | Modify | Add 2 queries (batch by charIds + single charId) |
| `grid-system/.../repo/charradical/CharRadicalRepository.java` | grid-system | Modify | Add 1 native query (random excluding IDs) |
| `grid-system/.../repo/vocabulary/VocabWordRepository.java` | grid-system | Modify | Add 1 native query (random excluding char) |
| `grid-system/.../service/game/dto/GameQuestionDTO.java` | grid-system | Create | Multi-language question DTO with nested option/explanation classes |
| `grid-system/.../service/game/GameCharacterService.java` | grid-system | Create | Service interface (3 methods) |
| `grid-system/.../service/game/impl/GameCharacterServiceImpl.java` | grid-system | Create | Service implementation with 3 question-generation algorithms |
| `grid-app/.../rest/vo/AppGameQuestionVO.java` | grid-app | Create | Single-language question VO with nested option/explanation classes |
| `grid-app/.../rest/wrapper/AppGameWrapper.java` | grid-app | Create | Static DTO→VO conversion + language filtering |
| `grid-app/.../rest/AppGameController.java` | grid-app | Create | REST controller with 3 anonymous endpoints |

---

### Task 1: Create HskLevelRange enum (grid-common)

**Files:**
- Create: `grid-common/src/main/java/com/naon/grid/enums/HskLevelRange.java`

**Interfaces:**
- Produces: `HskLevelRange.fromKey(String)` → `List<String>` — maps `"elementary"`→`["1","2"]`, `"intermediate"`→`["3","4"]`, `"advanced"`→`["5","6"]`, throws `IllegalArgumentException` for invalid keys

- [ ] **Step 1: Create the enum file**

```java
package com.naon.grid.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 汉字大挑战 — 难度分层枚举
 * <p>
 * 将前端语义 key（elementary/intermediate/advanced）映射为 HSK 等级代码列表。
 */
public enum HskLevelRange {

    ELEMENTARY("elementary", Arrays.asList("1", "2")),
    INTERMEDIATE("intermediate", Arrays.asList("3", "4")),
    ADVANCED("advanced", Arrays.asList("5", "6"));

    private final String key;
    private final List<String> levels;

    HskLevelRange(String key, List<String> levels) {
        this.key = key;
        this.levels = levels;
    }

    public String getKey() {
        return key;
    }

    public List<String> getLevels() {
        return levels;
    }

    /**
     * 根据前端语义 key 获取对应的 HSK 等级代码列表。
     *
     * @param key 前端语义 key，如 "elementary"、"intermediate"、"advanced"
     * @return HSK 等级代码列表，如 ["1", "2"]
     * @throws IllegalArgumentException 如果 key 不合法
     */
    public static List<String> fromKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("level 参数不能为空，有效值: elementary, intermediate, advanced");
        }
        for (HskLevelRange r : values()) {
            if (r.key.equals(key.trim())) {
                return r.levels;
            }
        }
        throw new IllegalArgumentException("无效的 level: " + key + "，有效值: elementary, intermediate, advanced");
    }
}
```

- [ ] **Step 2: Build grid-common to verify compilation**

```bash
mvn clean compile -pl grid-common -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-common/src/main/java/com/naon/grid/enums/HskLevelRange.java
git commit -m "feat: add HskLevelRange enum for game difficulty mapping

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: Add repository query methods (grid-system)

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharCharacterRepository.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharComparisonRepository.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharWordRepository.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/charradical/CharRadicalRepository.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabWordRepository.java`

**Interfaces:**
- Produces: `CharCharacterRepository.findRandomPublishedByHskLevels(List<String>, int)` → `List<CharCharacter>`
- Produces: `CharCharacterRepository.findRandomPublishedWithMinWords(List<String>, int)` → `List<CharCharacter>`
- Produces: `CharComparisonRepository.findRandomEnabled(int)` → `List<CharComparison>`
- Produces: `CharComparisonRepository.findByCharIdInAndStatus(List<Integer>, Integer)` → `List<CharComparison>`
- Produces: `CharWordRepository.findByCharIdInAndStatus(List<Integer>, Integer)` → `List<CharWord>` (already exists)
- Produces: `CharWordRepository.findByCharIdAndStatus(Integer, Integer)` → `List<CharWord>` (already exists)
- Produces: `CharRadicalRepository.findRandomPublishedExcluding(List<Long>, int)` → `List<CharRadical>`
- Produces: `VocabWordRepository.findRandomPublishedExcludingChar(List<String>, String, int)` → `List<VocabWord>`

- [ ] **Step 1: Add methods to CharCharacterRepository**

Open `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharCharacterRepository.java`. After the existing `findByRadicalIdAndStatusAndPublishStatus` methods (before the closing `}`), add:

```java
    /**
     * 按 HSK 等级列表随机取已发布汉字。
     *
     * @param levels HSK 等级代码列表
     * @param limit  返回数量上限
     * @return 随机汉字列表
     */
    @Query(value = "SELECT * FROM char_character WHERE hsk_level IN ?1 " +
        "AND status = 1 AND publish_status = 'published' " +
        "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<CharCharacter> findRandomPublishedByHskLevels(List<String> levels, int limit);

    /**
     * 按 HSK 等级列表随机取已发布且关联至少 2 条组词的汉字。
     *
     * @param levels HSK 等级代码列表
     * @param limit  返回数量上限
     * @return 随机汉字列表
     */
    @Query(value = "SELECT cc.* FROM char_character cc " +
        "INNER JOIN char_word cw ON cc.id = cw.char_id " +
        "WHERE cc.hsk_level IN ?1 AND cc.status = 1 AND cc.publish_status = 'published' " +
        "AND cw.status = 1 GROUP BY cc.id HAVING COUNT(cw.id) >= 2 " +
        "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<CharCharacter> findRandomPublishedWithMinWords(List<String> levels, int limit);
```

Also add the missing import at the top (alongside existing `@Query` import):
(No new import needed — `List`, `@Query`, `@Param` already imported)

- [ ] **Step 2: Add methods to CharComparisonRepository**

Open `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharComparisonRepository.java`. After the existing `countByCharIdInGroupByCharId` method, add:

```java
    /**
     * 随机取已启用（status=1）的辨析记录。
     *
     * @param limit 返回数量上限
     * @return 随机辨析记录列表
     */
    @Query(value = "SELECT * FROM char_comparison WHERE status = 1 " +
        "ORDER BY RAND() LIMIT ?1", nativeQuery = true)
    List<CharComparison> findRandomEnabled(int limit);

    /**
     * 按 charId 列表批量查询已启用的辨析记录（用于干扰项生成）。
     *
     * @param charIds 汉字ID列表
     * @param status  状态
     * @return 辨析记录列表
     */
    List<CharComparison> findByCharIdInAndStatus(List<Integer> charIds, Integer status);
```

Add the missing import (alongside the existing `@Query` import):
(no new import needed)

- [ ] **Step 3: Add methods to CharWordRepository**

Open `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharWordRepository.java`. After the existing `countByCharIdInGroupByCharId` method, add:

```java
    /**
     * 按 charId 列表批量查询已启用的组词。
     *
     * @param charIds 汉字ID列表
     * @param status  状态
     * @return 组词列表
     */
    List<CharWord> findByCharIdInAndStatus(List<Integer> charIds, Integer status);
```

Add the missing import:
(no new import needed — `List` already imported)

- [ ] **Step 4: Add method to CharRadicalRepository**

Open `grid-system/src/main/java/com/naon/grid/backend/repo/charradical/CharRadicalRepository.java`. After the existing `findByStatusAndPublishStatusOrderByIdAsc` method, add:

```java
    /**
     * 随机取已发布部首（排除指定 ID 列表）。
     *
     * @param excludeIds 要排除的部首ID列表
     * @param limit      返回数量上限
     * @return 随机部首列表
     */
    @Query(value = "SELECT * FROM char_radical WHERE status = 1 " +
        "AND publish_status = 'published' AND id NOT IN ?1 " +
        "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<CharRadical> findRandomPublishedExcluding(List<Long> excludeIds, int limit);
```

- [ ] **Step 5: Add method to VocabWordRepository**

Open `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabWordRepository.java`. After the existing `findRandomPublishedByHskLevel` method, add:

```java
    /**
     * 随机取已发布词汇，排除包含指定汉字的词（用于组词游戏干扰项）。
     *
     * @param levels      HSK等级列表
     * @param excludeChar 要排除的汉字
     * @param limit       返回数量上限
     * @return 随机词汇列表
     */
    @Query(value = "SELECT * FROM vocab_word WHERE hsk_level IN ?1 " +
        "AND status = 1 AND publish_status = 'published' " +
        "AND word NOT LIKE CONCAT('%', ?2, '%') " +
        "ORDER BY RAND() LIMIT ?3", nativeQuery = true)
    List<VocabWord> findRandomPublishedExcludingChar(List<String> levels, String excludeChar, int limit);
```

- [ ] **Step 6: Build grid-system to verify all repositories compile**

```bash
mvn clean compile -pl grid-system -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/character/CharCharacterRepository.java
git add grid-system/src/main/java/com/naon/grid/backend/repo/character/CharComparisonRepository.java
git add grid-system/src/main/java/com/naon/grid/backend/repo/character/CharWordRepository.java
git add grid-system/src/main/java/com/naon/grid/backend/repo/charradical/CharRadicalRepository.java
git add grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabWordRepository.java
git commit -m "feat: add random-sampling repository queries for game module

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: Create GameQuestionDTO with nested classes (grid-system)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/game/dto/GameQuestionDTO.java`

**Interfaces:**
- Produces: `GameQuestionDTO` — container with `gameType`, `questionIndex`, `stem`, `character`, `pinyin`, `options` (List\<GameOptionDTO\>), `correctKey`, `explanation` (GameExplanationDTO)
- Produces: `GameOptionDTO` (inner static) — `key`, `text`, `isCorrect`
- Produces: `GameExplanationDTO` (inner static) — multi-language fields for all three game types, each game fills its relevant subset

- [ ] **Step 1: Create the DTO file and directory**

```bash
mkdir -p grid-system/src/main/java/com/naon/grid/backend/service/game/dto
```

- [ ] **Step 2: Write the DTO file**

Create `grid-system/src/main/java/com/naon/grid/backend/service/game/dto/GameQuestionDTO.java`:

```java
package com.naon.grid.backend.service.game.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 游戏题目 DTO（多语言原始数据）。
 * <p>
 * 包含所有可用语言的翻译列表，由 AppGameWrapper 在 app 层筛选为单语言 VO。
 */
@Getter
@Setter
public class GameQuestionDTO implements Serializable {

    @ApiModelProperty(value = "游戏类型: radical / comparison / word_formation")
    private String gameType;

    @ApiModelProperty(value = "题号 1-10")
    private Integer questionIndex;

    @ApiModelProperty(value = "题干（展示用的汉字或句子语境）")
    private String stem;

    @ApiModelProperty(value = "目标汉字")
    private String character;

    @ApiModelProperty(value = "目标汉字拼音")
    private String pinyin;

    @ApiModelProperty(value = "选项列表（4个，顺序已打乱）")
    private List<GameOptionDTO> options;

    @ApiModelProperty(value = "正确答案的 key: A/B/C/D")
    private String correctKey;

    @ApiModelProperty(value = "解析信息（多语言原始列表）")
    private GameExplanationDTO explanation;

    // --- inner classes ---

    @Getter
    @Setter
    public static class GameOptionDTO implements Serializable {

        @ApiModelProperty(value = "选项标识: A/B/C/D")
        private String key;

        @ApiModelProperty(value = "选项文字")
        private String text;

        @ApiModelProperty(value = "是否正确答案")
        private Boolean isCorrect;
    }

    @Getter
    @Setter
    public static class GameExplanationDTO implements Serializable {

        // --- 部首游戏 ---

        @ApiModelProperty(value = "部首（部首游戏）")
        private String radical;

        @ApiModelProperty(value = "部首名称（部首游戏）")
        private String radicalName;

        @ApiModelProperty(value = "部首含义多语言（部首游戏）")
        private List<TextTranslation> radicalMeaning;

        // --- 形近字辨析游戏 ---

        @ApiModelProperty(value = "对比字（形近字游戏）")
        private String comparisonChar;

        @ApiModelProperty(value = "对比字拼音（形近字游戏）")
        private String comparisonPinyin;

        @ApiModelProperty(value = "对比说明多语言（形近字游戏）")
        private List<TextTranslation> comparisonDesc;

        // --- 组词游戏 ---

        @ApiModelProperty(value = "正确组词（组词游戏）")
        private String correctWord;

        @ApiModelProperty(value = "正确组词拼音（组词游戏）")
        private String correctWordPinyin;

        @ApiModelProperty(value = "正确组词词性（组词游戏）")
        private String correctWordPos;

        @ApiModelProperty(value = "正确组词释义多语言（组词游戏）")
        private List<TextTranslation> correctWordMeaning;
    }
}
```

- [ ] **Step 3: Build grid-system to verify compilation**

```bash
mvn clean compile -pl grid-system -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/game/
git commit -m "feat: add GameQuestionDTO with nested option and explanation classes

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: Create GameCharacterService interface (grid-system)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/game/GameCharacterService.java`

**Interfaces:**
- Produces: `GameCharacterService.generateRadicalQuestions(List<String>)` → `List<GameQuestionDTO>`
- Produces: `GameCharacterService.generateComparisonQuestions()` → `List<GameQuestionDTO>`
- Produces: `GameCharacterService.generateWordFormationQuestions(List<String>)` → `List<GameQuestionDTO>`

- [ ] **Step 1: Write the interface**

Create `grid-system/src/main/java/com/naon/grid/backend/service/game/GameCharacterService.java`:

```java
package com.naon.grid.backend.service.game;

import com.naon.grid.backend.service.game.dto.GameQuestionDTO;

import java.util.List;

/**
 * 汉字大挑战 — 出题服务接口。
 * <p>
 * 三种游戏各返回 10 道题目；数据不足时返回实际可用数量，不抛异常。
 */
public interface GameCharacterService {

    /**
     * 部首识记 — 生成 10 道部首选择题。
     *
     * @param hskLevels HSK 等级代码列表，如 ["1", "2"]
     * @return 题目列表
     */
    List<GameQuestionDTO> generateRadicalQuestions(List<String> hskLevels);

    /**
     * 形近字辨析 — 生成 10 道形近字选择题。
     *
     * @return 题目列表
     */
    List<GameQuestionDTO> generateComparisonQuestions();

    /**
     * 汉字组词 — 生成 10 道组词选择题。
     *
     * @param hskLevels HSK 等级代码列表，如 ["1", "2"]
     * @return 题目列表
     */
    List<GameQuestionDTO> generateWordFormationQuestions(List<String> hskLevels);
}
```

- [ ] **Step 2: Build to verify**

```bash
mvn clean compile -pl grid-system -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/game/GameCharacterService.java
git commit -m "feat: add GameCharacterService interface for quiz generation

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: Create GameCharacterServiceImpl (grid-system)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/game/impl/GameCharacterServiceImpl.java`

**Interfaces:**
- Consumes: `CharCharacterRepository` (5 methods), `CharComparisonRepository` (3 methods), `CharWordRepository` (2 methods), `CharRadicalRepository` (1 method), `VocabWordRepository` (1 method), `ExampleSentenceRepository.findById(Long)` (existing)
- Consumes: `com.naon.grid.utils.JsonUtils.parseArray(String, Class<T>)` — existing utility
- Produces: three `generate*` methods from `GameCharacterService` interface

- [ ] **Step 1: Write the full implementation**

Create `grid-system/src/main/java/com/naon/grid/backend/service/game/impl/GameCharacterServiceImpl.java`:

```java
package com.naon.grid.backend.service.game.impl;

import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.domain.character.CharComparison;
import com.naon.grid.backend.domain.character.CharWord;
import com.naon.grid.backend.domain.charradical.CharRadical;
import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.backend.repo.character.CharCharacterRepository;
import com.naon.grid.backend.repo.character.CharComparisonRepository;
import com.naon.grid.backend.repo.character.CharWordRepository;
import com.naon.grid.backend.repo.charradical.CharRadicalRepository;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.backend.repo.vocabulary.VocabWordRepository;
import com.naon.grid.backend.service.game.GameCharacterService;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO.GameExplanationDTO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO.GameOptionDTO;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 汉字大挑战 — 出题服务实现。
 * <p>
 * 每个方法内部处理数据不足的降级逻辑（返回实际可用数量，不抛异常）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameCharacterServiceImpl implements GameCharacterService {

    private static final int QUESTION_COUNT = 10;
    private static final int DISTRACTOR_COUNT = 3;
    private static final List<String> OPTION_KEYS = Arrays.asList("A", "B", "C", "D");

    private final CharCharacterRepository charCharacterRepository;
    private final CharComparisonRepository charComparisonRepository;
    private final CharWordRepository charWordRepository;
    private final CharRadicalRepository charRadicalRepository;
    private final VocabWordRepository vocabWordRepository;
    private final ExampleSentenceRepository exampleSentenceRepository;

    // ==================== 部首识记 ====================

    @Override
    @Transactional(readOnly = true)
    public List<GameQuestionDTO> generateRadicalQuestions(List<String> hskLevels) {
        List<CharCharacter> chars = charCharacterRepository
                .findRandomPublishedByHskLevels(hskLevels, QUESTION_COUNT);
        if (chars.isEmpty()) {
            log.warn("部首识记: 等级 {} 无可用汉字", hskLevels);
            return Collections.emptyList();
        }

        List<GameQuestionDTO> questions = new ArrayList<>();
        for (int i = 0; i < chars.size(); i++) {
            CharCharacter ch = chars.get(i);
            GameQuestionDTO q = buildRadicalQuestion(ch, i + 1);
            questions.add(q);
        }
        return questions;
    }

    private GameQuestionDTO buildRadicalQuestion(CharCharacter ch, int index) {
        GameQuestionDTO q = new GameQuestionDTO();
        q.setGameType("radical");
        q.setQuestionIndex(index);
        q.setStem(ch.getCharacter());
        q.setCharacter(ch.getCharacter());
        q.setPinyin(ch.getPinyin());

        // 正确答案: 该字所属部首
        String correctRadical = ch.getRadical();
        Long correctRadicalId = ch.getRadicalId();

        // 干扰项: 随机取其他部首
        List<Long> excludeIds = new ArrayList<>();
        if (correctRadicalId != null) {
            excludeIds.add(correctRadicalId);
        }
        List<CharRadical> distractorRadicals = charRadicalRepository
                .findRandomPublishedExcluding(excludeIds, DISTRACTOR_COUNT);

        // 构建选项列表
        List<GameOptionDTO> options = new ArrayList<>();

        GameOptionDTO correctOption = new GameOptionDTO();
        correctOption.setText(correctRadical);
        correctOption.setIsCorrect(true);
        options.add(correctOption);

        for (CharRadical dr : distractorRadicals) {
            GameOptionDTO opt = new GameOptionDTO();
            opt.setText(dr.getRadical());
            opt.setIsCorrect(false);
            options.add(opt);
        }

        // 打乱并分配 key
        Collections.shuffle(options);
        assignKeys(options, q);

        // 解析
        GameExplanationDTO exp = new GameExplanationDTO();
        exp.setRadical(correctRadical);
        if (correctRadicalId != null) {
            List<CharRadical> rads = charRadicalRepository.findAllById(Collections.singletonList(correctRadicalId));
            if (!rads.isEmpty()) {
                CharRadical rad = rads.get(0);
                exp.setRadicalName(rad.getRadicalName());
                exp.setRadicalMeaning(parseTranslations(rad.getEvolutionDescTranslations()));
            }
        }
        q.setExplanation(exp);

        return q;
    }

    // ==================== 形近字辨析 ====================

    @Override
    @Transactional(readOnly = true)
    public List<GameQuestionDTO> generateComparisonQuestions() {
        List<CharComparison> comparisons = charComparisonRepository.findRandomEnabled(QUESTION_COUNT);
        if (comparisons.isEmpty()) {
            log.warn("形近字辨析: 无可用辨析数据");
            return Collections.emptyList();
        }

        // 批量预加载所有涉及的 char_character
        List<Integer> charIds = comparisons.stream()
                .map(CharComparison::getCharId)
                .distinct()
                .collect(Collectors.toList());
        List<CharCharacter> chars = charCharacterRepository.findByIdIn(charIds);

        List<GameQuestionDTO> questions = new ArrayList<>();
        Set<Integer> usedComparisonIds = new HashSet<>();
        for (int i = 0; i < comparisons.size(); i++) {
            CharComparison comp = comparisons.get(i);
            if (!usedComparisonIds.add(comp.getId())) {
                continue; // 去重
            }
            CharCharacter target = findCharById(chars, comp.getCharId());
            if (target == null) {
                log.warn("形近字辨析: comparison id={} 关联的 charId={} 不存在", comp.getId(), comp.getCharId());
                continue;
            }
            GameQuestionDTO q = buildComparisonQuestion(comp, target, i + 1, chars);
            questions.add(q);
        }
        return questions;
    }

    private GameQuestionDTO buildComparisonQuestion(CharComparison comp, CharCharacter target,
                                                     int index, List<CharCharacter> allChars) {
        GameQuestionDTO q = new GameQuestionDTO();
        q.setGameType("comparison");
        q.setQuestionIndex(index);
        q.setCharacter(target.getCharacter());
        q.setPinyin(target.getPinyin());

        // 题干: 优先使用例句，将目标字替换为 ____
        String stem = buildComparisonStem(comp.getCharId(), target.getCharacter());
        q.setStem(stem);

        // 正确答案
        String correctChar = target.getCharacter();

        // 干扰项: 同 charId 下的其他 comparison_char，或同 radical 下的字
        List<GameOptionDTO> options = new ArrayList<>();

        GameOptionDTO correctOption = new GameOptionDTO();
        correctOption.setText(correctChar);
        correctOption.setIsCorrect(true);
        options.add(correctOption);

        List<String> distractors = findComparisonDistractors(comp, target, allChars, DISTRACTOR_COUNT);
        for (String d : distractors) {
            GameOptionDTO opt = new GameOptionDTO();
            opt.setText(d);
            opt.setIsCorrect(false);
            options.add(opt);
        }

        Collections.shuffle(options);
        assignKeys(options, q);

        // 解析
        GameExplanationDTO exp = new GameExplanationDTO();
        exp.setComparisonChar(comp.getComparisonChar());
        exp.setComparisonPinyin(comp.getComparisonPinyin());
        exp.setComparisonDesc(parseTranslations(comp.getComparisonDescTranslations()));
        q.setExplanation(exp);

        return q;
    }

    private String buildComparisonStem(Integer charId, String targetChar) {
        List<CharWord> words = charWordRepository.findByCharIdAndStatus(charId, 1);
        for (CharWord w : words) {
            if (w.getSentenceId() != null) {
                try {
                    ExampleSentence es = exampleSentenceRepository.findById(w.getSentenceId()).orElse(null);
                    if (es != null && es.getSentence() != null && es.getSentence().contains(targetChar)) {
                        return es.getSentence().replace(targetChar, "____");
                    }
                } catch (Exception e) {
                    // 忽略例句加载失败，继续尝试下一个
                }
            }
        }
        // 无合适例句时: 用释义提示作为题干
        return "____ (" + targetChar + ")";
    }

    private List<String> findComparisonDistractors(CharComparison comp, CharCharacter target,
                                                    List<CharCharacter> allChars, int count) {
        List<String> result = new ArrayList<>();
        // 优先: 同一 char_id 下的其他 comparison_char
        List<CharComparison> siblingComparisons = charComparisonRepository
                .findByCharIdAndStatus(comp.getCharId(), 1);
        for (CharComparison sc : siblingComparisons) {
            if (result.size() >= count) break;
            if (!sc.getComparisonChar().equals(target.getCharacter())
                    && !result.contains(sc.getComparisonChar())) {
                result.add(sc.getComparisonChar());
            }
        }
        // 不足时: 同 radical 下的其他字
        if (result.size() < count && target.getRadical() != null) {
            for (CharCharacter ch : allChars) {
                if (result.size() >= count) break;
                if (!ch.getCharacter().equals(target.getCharacter())
                        && target.getRadical().equals(ch.getRadical())
                        && !result.contains(ch.getCharacter())) {
                    result.add(ch.getCharacter());
                }
            }
        }
        // 再不足: 同等级随机字
        if (result.size() < count && target.getLevel() != null) {
            List<CharCharacter> randomPool = charCharacterRepository
                    .findByLevelAndStatusAndPublishStatus(target.getLevel(), 1, "published");
            for (CharCharacter ch : randomPool) {
                if (result.size() >= count) break;
                if (!ch.getCharacter().equals(target.getCharacter())
                        && !result.contains(ch.getCharacter())) {
                    result.add(ch.getCharacter());
                }
            }
        }
        return result;
    }

    // ==================== 汉字组词 ====================

    @Override
    @Transactional(readOnly = true)
    public List<GameQuestionDTO> generateWordFormationQuestions(List<String> hskLevels) {
        List<CharCharacter> chars = charCharacterRepository
                .findRandomPublishedWithMinWords(hskLevels, QUESTION_COUNT);
        if (chars.isEmpty()) {
            log.warn("汉字组词: 等级 {} 无含足够组词的汉字", hskLevels);
            return Collections.emptyList();
        }

        List<GameQuestionDTO> questions = new ArrayList<>();
        Set<Integer> usedCharIds = new HashSet<>();
        for (int i = 0; i < chars.size(); i++) {
            CharCharacter ch = chars.get(i);
            if (!usedCharIds.add(ch.getId())) {
                continue;
            }
            List<CharWord> words = charWordRepository.findByCharIdAndStatus(ch.getId(), 1);
            if (words.size() < 2) {
                continue;
            }
            GameQuestionDTO q = buildWordFormationQuestion(ch, words, i + 1, hskLevels);
            questions.add(q);
        }
        return questions;
    }

    private GameQuestionDTO buildWordFormationQuestion(CharCharacter ch, List<CharWord> words,
                                                        int index, List<String> hskLevels) {
        GameQuestionDTO q = new GameQuestionDTO();
        q.setGameType("word_formation");
        q.setQuestionIndex(index);
        q.setStem(ch.getCharacter());
        q.setCharacter(ch.getCharacter());
        q.setPinyin(ch.getPinyin());

        // 正确答案: 随机取一条组词
        Collections.shuffle(words);
        CharWord correctWord = words.get(0);

        // 干扰项: 从 vocab_word 取同等级词（排除含目标字的）
        List<VocabWord> distractorVocabs = vocabWordRepository
                .findRandomPublishedExcludingChar(hskLevels, ch.getCharacter(), DISTRACTOR_COUNT + 3);
        // 过滤掉与正确答案相同的词
        List<VocabWord> filtered = new ArrayList<>();
        for (VocabWord vw : distractorVocabs) {
            if (filtered.size() >= DISTRACTOR_COUNT) break;
            if (!vw.getWord().equals(correctWord.getWordItem())) {
                filtered.add(vw);
            }
        }
        // 如果同等级不足，放宽等级重试
        if (filtered.size() < DISTRACTOR_COUNT) {
            List<String> allLevels = Arrays.asList("1", "2", "3", "4", "5", "6");
            List<VocabWord> fallback = vocabWordRepository
                    .findRandomPublishedExcludingChar(allLevels, ch.getCharacter(), DISTRACTOR_COUNT + 3);
            for (VocabWord vw : fallback) {
                if (filtered.size() >= DISTRACTOR_COUNT) break;
                if (!vw.getWord().equals(correctWord.getWordItem())
                        && !containsVocabWord(filtered, vw)) {
                    filtered.add(vw);
                }
            }
        }

        // 构建选项
        List<GameOptionDTO> options = new ArrayList<>();

        GameOptionDTO correctOption = new GameOptionDTO();
        correctOption.setText(correctWord.getWordItem());
        correctOption.setIsCorrect(true);
        options.add(correctOption);

        for (VocabWord vw : filtered) {
            GameOptionDTO opt = new GameOptionDTO();
            opt.setText(vw.getWord());
            opt.setIsCorrect(false);
            options.add(opt);
        }

        Collections.shuffle(options);
        assignKeys(options, q);

        // 解析
        GameExplanationDTO exp = new GameExplanationDTO();
        exp.setCorrectWord(correctWord.getWordItem());
        exp.setCorrectWordPinyin(correctWord.getPinyin());
        exp.setCorrectWordPos(correctWord.getPartOfSpeech());
        exp.setCorrectWordMeaning(parseTranslations(correctWord.getWordItemTranslations()));
        q.setExplanation(exp);

        return q;
    }

    // ==================== 通用工具方法 ====================

    /**
     * 为打乱后的选项列表分配 A/B/C/D key，并将正确答案的 key 记录到 question 上。
     */
    private void assignKeys(List<GameOptionDTO> options, GameQuestionDTO question) {
        for (int i = 0; i < options.size() && i < OPTION_KEYS.size(); i++) {
            GameOptionDTO opt = options.get(i);
            opt.setKey(OPTION_KEYS.get(i));
            if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                question.setCorrectKey(opt.getKey());
            }
        }
        question.setOptions(options);
    }

    /**
     * 解析 JSON 翻译字段为 TextTranslation 列表。
     */
    private List<TextTranslation> parseTranslations(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JsonUtils.parseTranslationList(json);
        } catch (Exception e) {
            log.warn("解析翻译 JSON 失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    private CharCharacter findCharById(List<CharCharacter> chars, Integer id) {
        for (CharCharacter ch : chars) {
            if (ch.getId().equals(id)) {
                return ch;
            }
        }
        return null;
    }

    private boolean containsVocabWord(List<VocabWord> list, VocabWord word) {
        for (VocabWord vw : list) {
            if (vw.getId().equals(word.getId())) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 2: Build grid-system to verify compilation**

```bash
mvn clean compile -pl grid-system -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/game/impl/GameCharacterServiceImpl.java
git commit -m "feat: add GameCharacterServiceImpl with three quiz generation algorithms

- Radical quiz: random chars → 4 radical options with shuffle
- Comparison quiz: random comparison pairs → fill-in-blank stems
- Word formation quiz: random chars with ≥2 words → vocab distractors
- All methods handle data-insufficient edge cases gracefully

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: Create AppGameQuestionVO (grid-app)

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppGameQuestionVO.java`

**Interfaces:**
- Produces: `AppGameQuestionVO` — single-language question VO with `GameOptionVO` and `GameExplanationVO` inner classes
- Note: `GameExplanationVO` uses single `TextTranslationVO` per field (not List), matching `AppCharCharacterDetailVO` pattern

- [ ] **Step 1: Write the VO file**

Create `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppGameQuestionVO.java`:

```java
package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 用户端游戏题目 VO（单语言筛选后）。
 */
@Getter
@Setter
public class AppGameQuestionVO implements Serializable {

    @ApiModelProperty(value = "游戏类型: radical / comparison / word_formation")
    private String gameType;

    @ApiModelProperty(value = "题号 1-10")
    private Integer questionIndex;

    @ApiModelProperty(value = "题干（展示用的汉字或句子语境）")
    private String stem;

    @ApiModelProperty(value = "目标汉字")
    private String character;

    @ApiModelProperty(value = "目标汉字拼音")
    private String pinyin;

    @ApiModelProperty(value = "选项列表（4个，已打乱）")
    private List<GameOptionVO> options;

    @ApiModelProperty(value = "正确答案 key")
    private String correctKey;

    @ApiModelProperty(value = "解析信息（单语言）")
    private GameExplanationVO explanation;

    // --- inner classes ---

    @Getter
    @Setter
    public static class GameOptionVO implements Serializable {

        @ApiModelProperty(value = "选项标识: A/B/C/D")
        private String key;

        @ApiModelProperty(value = "选项文字")
        private String text;

        @ApiModelProperty(value = "是否正确答案")
        private Boolean isCorrect;
    }

    @Getter
    @Setter
    public static class GameExplanationVO implements Serializable {

        // --- 部首游戏 ---
        @ApiModelProperty(value = "部首")
        private String radical;

        @ApiModelProperty(value = "部首名称")
        private String radicalName;

        @ApiModelProperty(value = "部首含义（单语言）")
        private TextTranslationVO radicalMeaning;

        // --- 形近字辨析游戏 ---
        @ApiModelProperty(value = "对比字")
        private String comparisonChar;

        @ApiModelProperty(value = "对比字拼音")
        private String comparisonPinyin;

        @ApiModelProperty(value = "对比说明（单语言）")
        private TextTranslationVO comparisonDesc;

        // --- 组词游戏 ---
        @ApiModelProperty(value = "正确组词")
        private String correctWord;

        @ApiModelProperty(value = "正确组词拼音")
        private String correctWordPinyin;

        @ApiModelProperty(value = "正确组词词性")
        private String correctWordPos;

        @ApiModelProperty(value = "正确组词释义（单语言）")
        private TextTranslationVO correctWordMeaning;
    }
}
```

- [ ] **Step 2: Build grid-app to verify compilation**

```bash
mvn clean compile -pl grid-app -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppGameQuestionVO.java
git commit -m "feat: add AppGameQuestionVO for single-language game question response

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 7: Create AppGameWrapper (grid-app)

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppGameWrapper.java`

**Interfaces:**
- Consumes: `GameQuestionDTO`, `GameQuestionDTO.GameOptionDTO`, `GameQuestionDTO.GameExplanationDTO` from Task 3
- Consumes: `AppGameQuestionVO`, `AppGameQuestionVO.GameOptionVO`, `AppGameQuestionVO.GameExplanationVO` from Task 6
- Consumes: `TextTranslationVO` (existing) from `com.naon.grid.backend.rest.vo`
- Consumes: `TextTranslation` (existing) from `com.naon.grid.domain.common`
- Produces: `AppGameWrapper.toQuestionVOList(List<GameQuestionDTO>, String language)` → `List<AppGameQuestionVO>`

- [ ] **Step 1: Write the Wrapper**

Create `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppGameWrapper.java`:

```java
package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO.GameExplanationDTO;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO.GameOptionDTO;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppGameQuestionVO;
import com.naon.grid.modules.app.rest.vo.AppGameQuestionVO.GameExplanationVO;
import com.naon.grid.modules.app.rest.vo.AppGameQuestionVO.GameOptionVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户端游戏包装器 — DTO → VO 转换 + 单语言筛选。
 * <p>
 * 遵循 AppCharCharacterWrapper 的静态工具类模式。
 */
public class AppGameWrapper {

    /**
     * 批量转换题目 DTO 列表为单语言 VO 列表。
     *
     * @param dtos     多语言题目 DTO 列表
     * @param language 目标语言，如 "zh"、"en"
     * @return 单语言 VO 列表
     */
    public static List<AppGameQuestionVO> toQuestionVOList(List<GameQuestionDTO> dtos, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        List<AppGameQuestionVO> vos = new ArrayList<>(dtos.size());
        for (GameQuestionDTO dto : dtos) {
            vos.add(toQuestionVO(dto, language));
        }
        return vos;
    }

    /**
     * 转换单个题目 DTO 为单语言 VO。
     */
    public static AppGameQuestionVO toQuestionVO(GameQuestionDTO dto, String language) {
        AppGameQuestionVO vo = new AppGameQuestionVO();
        vo.setGameType(dto.getGameType());
        vo.setQuestionIndex(dto.getQuestionIndex());
        vo.setStem(dto.getStem());
        vo.setCharacter(dto.getCharacter());
        vo.setPinyin(dto.getPinyin());
        vo.setCorrectKey(dto.getCorrectKey());
        vo.setOptions(toOptionVOList(dto.getOptions()));
        vo.setExplanation(toExplanationVO(dto.getExplanation(), language));
        return vo;
    }

    private static List<GameOptionVO> toOptionVOList(List<GameOptionDTO> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        List<GameOptionVO> vos = new ArrayList<>(dtos.size());
        for (GameOptionDTO dto : dtos) {
            GameOptionVO vo = new GameOptionVO();
            vo.setKey(dto.getKey());
            vo.setText(dto.getText());
            vo.setIsCorrect(dto.getIsCorrect());
            vos.add(vo);
        }
        return vos;
    }

    private static GameExplanationVO toExplanationVO(GameExplanationDTO dto, String language) {
        if (dto == null) {
            return null;
        }
        GameExplanationVO vo = new GameExplanationVO();

        // 部首游戏解析
        vo.setRadical(dto.getRadical());
        vo.setRadicalName(dto.getRadicalName());
        vo.setRadicalMeaning(filterByLanguage(dto.getRadicalMeaning(), language));

        // 形近字辨析解析
        vo.setComparisonChar(dto.getComparisonChar());
        vo.setComparisonPinyin(dto.getComparisonPinyin());
        vo.setComparisonDesc(filterByLanguage(dto.getComparisonDesc(), language));

        // 组词游戏解析
        vo.setCorrectWord(dto.getCorrectWord());
        vo.setCorrectWordPinyin(dto.getCorrectWordPinyin());
        vo.setCorrectWordPos(dto.getCorrectWordPos());
        vo.setCorrectWordMeaning(filterByLanguage(dto.getCorrectWordMeaning(), language));

        return vo;
    }

    /**
     * 从多语言翻译列表中筛选匹配目标语言的单个翻译 VO。
     * <p>
     * 复刻 AppCharCharacterWrapper.filterByLanguage 的实现逻辑。
     */
    private static TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) {
            return null;
        }
        for (TextTranslation t : translations) {
            if (language.equals(t.getLanguage())) {
                TextTranslationVO vo = new TextTranslationVO();
                vo.setLanguage(t.getLanguage());
                vo.setTranslation(t.getTranslation());
                return vo;
            }
        }
        return null;
    }
}
```

- [ ] **Step 2: Build grid-app to verify compilation**

```bash
mvn clean compile -pl grid-app -am -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppGameWrapper.java
git commit -m "feat: add AppGameWrapper for DTO→VO conversion with language filtering

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 8: Create AppGameController (grid-app)

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppGameController.java`

**Interfaces:**
- Consumes: `GameCharacterService.generateRadicalQuestions(List<String>)` → `List<GameQuestionDTO>` from Task 4
- Consumes: `GameCharacterService.generateComparisonQuestions()` → `List<GameQuestionDTO>` from Task 4
- Consumes: `GameCharacterService.generateWordFormationQuestions(List<String>)` → `List<GameQuestionDTO>` from Task 4
- Consumes: `HskLevelRange.fromKey(String)` → `List<String>` from Task 1
- Consumes: `AppGameWrapper.toQuestionVOList(List<GameQuestionDTO>, String)` → `List<AppGameQuestionVO>` from Task 7
- Produces: 3 REST endpoints returning `ResponseEntity<List<AppGameQuestionVO>>`

- [ ] **Step 1: Write the Controller**

Create `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppGameController.java`:

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.game.GameCharacterService;
import com.naon.grid.backend.service.game.dto.GameQuestionDTO;
import com.naon.grid.enums.HskLevelRange;
import com.naon.grid.modules.app.rest.vo.AppGameQuestionVO;
import com.naon.grid.modules.app.rest.wrapper.AppGameWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端汉字游戏接口。
 * <p>
 * 三种游戏各返回 10 道题目，匿名访问。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/character/game")
@Api(tags = "用户：汉字游戏接口")
public class AppGameController {

    private final GameCharacterService gameCharacterService;

    @AnonymousGetMapping("/radical")
    @ApiOperation("获取部首识记题目（10题）")
    public ResponseEntity<List<AppGameQuestionVO>> getRadicalQuestions(
            @RequestParam @ApiParam(value = "难度: elementary|intermediate|advanced", required = true)
            String level,
            @RequestParam @ApiParam(value = "语言: zh|en", required = true)
            String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        List<String> hskLevels = HskLevelRange.fromKey(level);
        List<GameQuestionDTO> dtos = gameCharacterService.generateRadicalQuestions(hskLevels);
        List<AppGameQuestionVO> vos = AppGameWrapper.toQuestionVOList(dtos, language);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @AnonymousGetMapping("/comparison")
    @ApiOperation("获取形近字辨析题目（10题）")
    public ResponseEntity<List<AppGameQuestionVO>> getComparisonQuestions(
            @RequestParam @ApiParam(value = "语言: zh|en", required = true)
            String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        List<GameQuestionDTO> dtos = gameCharacterService.generateComparisonQuestions();
        List<AppGameQuestionVO> vos = AppGameWrapper.toQuestionVOList(dtos, language);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @AnonymousGetMapping("/word-formation")
    @ApiOperation("获取组词游戏题目（10题）")
    public ResponseEntity<List<AppGameQuestionVO>> getWordFormationQuestions(
            @RequestParam @ApiParam(value = "难度: elementary|intermediate|advanced", required = true)
            String level,
            @RequestParam @ApiParam(value = "语言: zh|en", required = true)
            String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        List<String> hskLevels = HskLevelRange.fromKey(level);
        List<GameQuestionDTO> dtos = gameCharacterService.generateWordFormationQuestions(hskLevels);
        List<AppGameQuestionVO> vos = AppGameWrapper.toQuestionVOList(dtos, language);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }
}
```

- [ ] **Step 2: Full project build to verify all modules compile together**

```bash
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS across all modules

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppGameController.java
git commit -m "feat: add AppGameController with three anonymous game endpoints

- GET /api/app/character/game/radical
- GET /api/app/character/game/comparison
- GET /api/app/character/game/word-formation

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 9: Verify endpoints with curl smoke test

**Files:**
- None (manual verification)

- [ ] **Step 1: Start the application with dev profile**

```bash
cd grid-bootstrap
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Wait for `Started GridApplication in X seconds`.

- [ ] **Step 2: Test radical endpoint**

```bash
curl -s "http://localhost:8000/api/app/character/game/radical?level=elementary&language=zh" | head -c 500
```

Expected: JSON array of game questions (may be empty if no data in char_character table).

- [ ] **Step 3: Test comparison endpoint**

```bash
curl -s "http://localhost:8000/api/app/character/game/comparison?language=zh" | head -c 500
```

Expected: JSON array of game questions.

- [ ] **Step 4: Test word-formation endpoint**

```bash
curl -s "http://localhost:8000/api/app/character/game/word-formation?level=intermediate&language=zh" | head -c 500
```

Expected: JSON array of game questions.

- [ ] **Step 5: Test invalid level parameter**

```bash
curl -s "http://localhost:8000/api/app/character/game/radical?level=invalid&language=zh"
```

Expected: 400 error with message containing "无效的 level"

- [ ] **Step 6: Test missing language parameter**

```bash
curl -s "http://localhost:8000/api/app/character/game/radical?level=elementary&language="
```

Expected: 400 error with message "language 参数不能为空"
