# Vocab Challenge App API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GET /api/app/vocab/challenge` endpoint returning 10 image-based vocabulary quiz questions, with HSK level auto-adaptation from user profile.

**Architecture:** New native SQL query on `VocabWordRepository` for random word selection; new service method on `VocabWordService` returning `VocabWordDto` list; new static `AppVocabChallengeWrapper` for DTO→VO mapping; new endpoint method on existing `AppVocabWordController`. Reuses `AppExerciseQuestionDetailVO` unchanged. Controller calls service (not repository directly).

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MySQL (native query with RAND()), Java 8

## Global Constraints

- Java 8 — no `var`, no `List.of()`, no `Stream.toList()`
- Lombok — `@RequiredArgsConstructor`, `@Slf4j`, `@Getter`, `@Setter`
- Wrapper pattern — pure static methods, no Spring beans, external resources (audio/image) passed as `Map` params
- Controller — no conversion logic; all mapping in Wrapper
- App auth — `AppSecurityUtils.getCurrentUserId()` for logged-in user (NOT anonymous)
- 10 questions fixed, all with images (JOIN vocab_sense WHERE def_image_id IS NOT NULL)
- No duplicate avoidance across requests, no score tracking

---

### Task 1: Add random vocab query to VocabWordRepository

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabWordRepository.java`

**Interfaces:**
- Produces: `List<VocabWord> findRandomPublishedByHskLevel(String hskLevel, int count)`

- [ ] **Step 1: Add native query method**

Add the new method and import inside the existing interface:

```java
import org.springframework.data.jpa.repository.Query;

// Inside the interface, add:
/**
 * 随机获取指定HSK等级的已发布且有配图的词汇
 *
 * @param hskLevel HSK等级
 * @param count    返回数量
 * @return 随机词汇列表
 */
@Query(value = "SELECT DISTINCT vw.* FROM vocab_word vw " +
    "INNER JOIN vocab_sense vs ON vw.id = vs.word_id " +
    "WHERE vw.hsk_level = ?1 AND vw.status = 1 AND vw.publish_status = 'published' " +
    "AND vs.status = 1 AND vs.def_image_id IS NOT NULL " +
    "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
List<VocabWord> findRandomPublishedByHskLevel(String hskLevel, int count);
```

- [ ] **Step 2: Run compile to verify**

```bash
cd grid-bootstrap && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabWordRepository.java
git commit -m "feat: add findRandomPublishedByHskLevel native query for vocab challenge"
```

---

### Task 2: Add findRandomPublishedWithImage to VocabWordService

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

**Interfaces:**
- Produces: `List<VocabWordDto> VocabWordService.findRandomPublishedWithImage(String hskLevel, int count)`

- [ ] **Step 1: Add method declaration to interface**

In `VocabWordService.java`, add after `searchByWord`:

```java
/**
 * 随机获取指定HSK等级的已发布且有配图的词汇
 *
 * @param hskLevel HSK等级
 * @param count    返回数量
 * @return 词汇DTO列表（仅主表字段，不含子表）
 */
List<VocabWordDto> findRandomPublishedWithImage(String hskLevel, int count);
```

- [ ] **Step 2: Implement in VocabWordServiceImpl**

In `VocabWordServiceImpl.java`, add after `searchByWord`:

```java
@Override
public List<VocabWordDto> findRandomPublishedWithImage(String hskLevel, int count) {
    List<VocabWord> words = vocabWordRepository.findRandomPublishedByHskLevel(hskLevel, count);
    return words.stream()
            .map(vocabWordMapper::toDto)
            .collect(Collectors.toList());
}
```

- [ ] **Step 3: Run compile to verify**

```bash
cd grid-bootstrap && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java \
        grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "feat: add findRandomPublishedWithImage to VocabWordService"
```

---

### Task 3: Create AppVocabChallengeWrapper

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppVocabChallengeWrapper.java`

**Interfaces:**
- Produces: `AppVocabChallengeWrapper.toChallengeVOList(List<ChallengeItem>, Map<Long, AliOssStorageDto>) → List<AppExerciseQuestionDetailVO>`

- [ ] **Step 1: Create the wrapper**

```java
package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.modules.app.rest.vo.AppExerciseQuestionDetailVO;
import com.naon.grid.service.dto.AliOssStorageDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class AppVocabChallengeWrapper {

    /**
     * 将挑战题目数据转换为 AppExerciseQuestionDetailVO 列表
     */
    public static List<AppExerciseQuestionDetailVO> toChallengeVOList(
            List<ChallengeItem> items,
            Map<Long, AliOssStorageDto> imageMap) {
        if (items == null) {
            return Collections.emptyList();
        }
        List<AppExerciseQuestionDetailVO> vos = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            vos.add(toChallengeVO(items.get(i), i + 1, imageMap));
        }
        return vos;
    }

    private static AppExerciseQuestionDetailVO toChallengeVO(
            ChallengeItem item, int sort, Map<Long, AliOssStorageDto> imageMap) {

        VocabWordDto correct = item.getCorrectWord();

        AppExerciseQuestionDetailVO vo = new AppExerciseQuestionDetailVO();
        vo.setId((long) -correct.getId());
        vo.setQuestionType("vocab_challenge");
        vo.setStem("请根据提示选出对应的词语");
        vo.setContent(buildContent(item.getDefImageId(), imageMap));
        vo.setOptions(buildOptions(correct, item.getDistractors()));
        vo.setAnswer(Collections.singletonList(
                findCorrectOption(vo.getOptions(), correct.getWord())));
        vo.setExplanation(null);
        vo.setAudio(null);
        vo.setAudioText(null);
        vo.setSort(sort);
        vo.setChildren(null);
        return vo;
    }

    private static AppExerciseQuestionDetailVO.QuestionContentVO buildContent(
            Long defImageId, Map<Long, AliOssStorageDto> imageMap) {
        AppExerciseQuestionDetailVO.QuestionContentVO content =
                new AppExerciseQuestionDetailVO.QuestionContentVO();
        content.setContentText(null);
        if (defImageId != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(defImageId);
            if (imgDto != null) {
                AppExerciseQuestionDetailVO.ImageVO imageVO =
                        new AppExerciseQuestionDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                content.setImage(imageVO);
            } else {
                log.error("挑战题目图片资源未找到, imageId={}", defImageId);
            }
        }
        return content;
    }

    private static List<AppExerciseQuestionDetailVO.QuestionOptionVO> buildOptions(
            VocabWordDto correctWord, List<VocabWordDto> distractors) {
        List<VocabWordDto> all = new ArrayList<>();
        all.add(correctWord);
        if (distractors != null) {
            all.addAll(distractors);
        }
        Collections.shuffle(all);
        String[] letters = {"A", "B", "C", "D"};
        return IntStream.range(0, Math.min(all.size(), 4))
                .mapToObj(i -> {
                    AppExerciseQuestionDetailVO.QuestionOptionVO opt =
                            new AppExerciseQuestionDetailVO.QuestionOptionVO();
                    opt.setOption(letters[i]);
                    opt.setOptionText(all.get(i).getWord());
                    opt.setImage(null);
                    return opt;
                })
                .collect(Collectors.toList());
    }

    private static String findCorrectOption(
            List<AppExerciseQuestionDetailVO.QuestionOptionVO> options,
            String correctWord) {
        for (AppExerciseQuestionDetailVO.QuestionOptionVO opt : options) {
            if (correctWord.equals(opt.getOptionText())) {
                return opt.getOption();
            }
        }
        return "A";
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ChallengeItem {
        private VocabWordDto correctWord;
        private Long defImageId;
        private List<VocabWordDto> distractors;
    }
}
```

- [ ] **Step 2: Run compile**

```bash
cd grid-bootstrap && mvn compile -q
```

Expected: BUILD SUCCESS (will fail until Task 4 adds the import for ChallengeItem, but verify the wrapper itself compiles clean)

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppVocabChallengeWrapper.java
git commit -m "feat: add AppVocabChallengeWrapper for vocab challenge DTO mapping"
```

---

### Task 4: Add challenge endpoint to AppVocabWordController

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java`

**Interfaces:**
- Consumes: `VocabWordService.findRandomPublishedWithImage()`, `VocabSenseRepository.findByWordIdAndStatus()`, `GridUserRepository.findById()`, `AliOssStorageService.findByIds()`, `AppVocabChallengeWrapper.toChallengeVOList()`
- Produces: `GET /api/app/vocab/challenge → ResponseEntity<List<AppExerciseQuestionDetailVO>>`

- [ ] **Step 1: Add imports**

Add these imports to the existing imports in `AppVocabWordController.java`:

```java
import com.naon.grid.backend.repo.vocabulary.VocabSenseRepository;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.modules.app.rest.vo.AppExerciseQuestionDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppVocabChallengeWrapper;
import com.naon.grid.modules.app.rest.wrapper.AppVocabChallengeWrapper.ChallengeItem;
import com.naon.grid.exception.EntityNotFoundException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
```

- [ ] **Step 2: Add injected dependencies**

Add two new `final` fields to the class:

```java
private final GridUserRepository gridUserRepository;
private final VocabSenseRepository vocabSenseRepository;
```

- [ ] **Step 3: Add challenge endpoint and helper methods**

Insert before the first existing private method (`collectAndBatchQueryAudios`):

```java
    @ApiOperation("词汇大挑战（10道图片选择题）")
    @GetMapping("/challenge")
    public ResponseEntity<List<AppExerciseQuestionDetailVO>> challenge() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        GridUser user = gridUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(GridUser.class, "id", userId.toString()));
        String hskLevel = user.getHskLevel();

        // 确定出题等级
        String levelA, levelB;
        if (hskLevel == null || "0".equals(hskLevel)) {
            levelA = "4";
            levelB = "5";
        } else {
            int n = Integer.parseInt(hskLevel);
            if (n >= 9) {
                levelA = "9";
                levelB = "9";
            } else {
                levelA = hskLevel;
                levelB = String.valueOf(n + 1);
            }
        }

        List<VocabWordDto> answerWords = new ArrayList<>();
        answerWords.addAll(vocabWordService.findRandomPublishedWithImage(levelA, 5));
        answerWords.addAll(vocabWordService.findRandomPublishedWithImage(levelB, 5));

        // 同一等级时去重并补足
        if (levelA.equals(levelB)) {
            List<VocabWordDto> deduped = new ArrayList<>();
            Set<Integer> seen = new HashSet<>();
            for (VocabWordDto w : answerWords) {
                if (seen.add(w.getId())) {
                    deduped.add(w);
                }
            }
            answerWords = deduped;
            if (answerWords.size() < 10) {
                int need = 10 - answerWords.size();
                List<VocabWordDto> extra = vocabWordService.findRandomPublishedWithImage(levelA, need + 5);
                for (VocabWordDto w : extra) {
                    if (answerWords.size() >= 10) break;
                    if (seen.add(w.getId())) {
                        answerWords.add(w);
                    }
                }
            }
        }

        if (answerWords.size() > 10) {
            answerWords = answerWords.subList(0, 10);
        }

        // 组装每道题
        List<ChallengeItem> items = new ArrayList<>();
        List<Long> imageIds = new ArrayList<>();

        for (VocabWordDto answerWord : answerWords) {
            Long defImageId = findFirstImageId(answerWord.getId());
            if (defImageId != null) {
                imageIds.add(defImageId);
            }
            String wordLevel = answerWord.getHskLevel() != null ? answerWord.getHskLevel() : levelA;
            List<VocabWordDto> distractors = getDistractors(wordLevel, 3,
                    Collections.singletonList(answerWord.getId()));
            items.add(new ChallengeItem(answerWord, defImageId, distractors));
        }

        Map<Long, AliOssStorageDto> imageMap = batchQueryImages(imageIds);
        List<AppExerciseQuestionDetailVO> vos = AppVocabChallengeWrapper.toChallengeVOList(items, imageMap);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    private Long findFirstImageId(Integer wordId) {
        List<com.naon.grid.backend.domain.vocabulary.VocabSense> senses =
                vocabSenseRepository.findByWordIdAndStatus(wordId, 1);
        for (com.naon.grid.backend.domain.vocabulary.VocabSense sense : senses) {
            if (sense.getDefImageId() != null) {
                return sense.getDefImageId();
            }
        }
        return null;
    }

    private List<VocabWordDto> getDistractors(String hskLevel, int count, List<Integer> excludeIds) {
        int fetchCount = count + (excludeIds != null ? excludeIds.size() : 0) + 5;
        List<VocabWordDto> candidates = vocabWordService.findRandomPublishedWithImage(
                hskLevel, Math.min(fetchCount, 50));
        List<VocabWordDto> result = new ArrayList<>();
        for (VocabWordDto w : candidates) {
            if (result.size() >= count) break;
            boolean excluded = false;
            if (excludeIds != null) {
                for (Integer eid : excludeIds) {
                    if (eid.equals(w.getId())) {
                        excluded = true;
                        break;
                    }
                }
            }
            if (!excluded) {
                result.add(w);
            }
        }
        return result;
    }

    private Map<Long, AliOssStorageDto> batchQueryImages(List<Long> imageIds) {
        if (imageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AliOssStorageDto> imageDtos = aliOssStorageService.findByIds(imageIds);
        return imageDtos.stream()
                .collect(Collectors.toMap(AliOssStorageDto::getId, i -> i, (a, b) -> a));
    }
```

- [ ] **Step 4: Run full compile**

```bash
cd grid-bootstrap && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java
git commit -m "feat: add GET /api/app/vocab/challenge endpoint"
```

---

### Task 5: Verify

- [ ] **Step 1: Full project compile**

```bash
cd grid-bootstrap && mvn compile
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Manual endpoint test** (requires running app)

1. Start the application: `cd grid-bootstrap && mvn spring-boot:run`
2. Call endpoint: `GET /api/app/vocab/challenge` with valid App Bearer Token
3. Verify JSON response:
   - Exactly 10 items
   - Each: `id` negative, `questionType` = `"vocab_challenge"`, `content.contentText` = null, `content.image.imageUrl` not null, `options` = 4 items (A/B/C/D with word text), `answer` = single letter matching correct option
4. Test hskLevel="0" user → questions from levels 4 and 5
5. Test hskLevel="3" user → questions from levels 3 and 4
6. Test hskLevel="9" user → all 10 from level 9

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "verify: vocab challenge endpoint working correctly"
```
