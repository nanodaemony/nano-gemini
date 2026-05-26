# 词汇更新接口 Diff 优化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Change `PUT /api/vocabulary/{id}` so vocabulary child tables are updated by diff instead of full delete-and-reinsert.

**Architecture:** Keep the controller API unchanged and add child `id` fields to update request DTOs. The service updates the main `VocabWord`, then synchronizes each child collection with explicit layer-specific diff methods for senses, structures, examples, and exercises.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Spring Data JPA repositories, JUnit 5, Mockito, Maven.

---

## File Structure

- Modify `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java`
  - Add `Integer id` to `VocabSenseRequest`, `VocabStructureRequest`, `VocabExerciseRequest`, and `VocabExampleRequest`.
- Modify `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`
  - Copy nested request IDs into DTOs in `toSenseDto`, `toStructureDto`, `toExerciseDto`, and `toExampleDto`.
- Modify `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`
  - Replace update-time full child deletion with diff synchronization.
  - Add private sync/update/delete helper methods for each child type.
  - Import `com.naon.grid.exception.BadRequestException`.
- Create `grid-system/src/test/java/com/naon/grid/backend/rest/controller/VocabWordControllerTest.java`
  - Verify update request child IDs are passed from controller request objects to service DTOs.
- Create `grid-system/src/test/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImplTest.java`
  - Verify diff update preserves existing child IDs, inserts new children, deletes missing children, and rejects duplicate or wrong-parent IDs.

---

### Task 1: Add request IDs and controller mapping

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java:34-117`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java:109-170`
- Create: `grid-system/src/test/java/com/naon/grid/backend/rest/controller/VocabWordControllerTest.java`

- [ ] **Step 1: Write the failing controller mapping test**

Create `grid-system/src/test/java/com/naon/grid/backend/rest/controller/VocabWordControllerTest.java` with this content:

```java
package com.naon.grid.backend.rest.controller;

import com.naon.grid.backend.rest.request.VocabWordCreateRequest;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VocabWordControllerTest {

    @Test
    void updatePassesNestedChildIdsToServiceDto() {
        VocabWordService vocabWordService = mock(VocabWordService.class);
        VocabWordController controller = new VocabWordController(vocabWordService);

        VocabWordCreateRequest request = new VocabWordCreateRequest();
        request.setWord("学习");

        VocabWordCreateRequest.VocabExampleRequest example = new VocabWordCreateRequest.VocabExampleRequest();
        example.setId(301);
        example.setSentence("我学习中文。");

        VocabWordCreateRequest.VocabStructureRequest structure = new VocabWordCreateRequest.VocabStructureRequest();
        structure.setId(201);
        structure.setPattern("学习 + 语言");
        structure.setExamples(Collections.singletonList(example));

        VocabWordCreateRequest.VocabSenseRequest sense = new VocabWordCreateRequest.VocabSenseRequest();
        sense.setId(101);
        sense.setChineseDef("获取知识");
        sense.setStructures(Collections.singletonList(structure));

        VocabWordCreateRequest.VocabExerciseRequest exercise = new VocabWordCreateRequest.VocabExerciseRequest();
        exercise.setId(401);
        exercise.setQuestionText("选择正确释义");

        request.setSenses(Collections.singletonList(sense));
        request.setExercises(Collections.singletonList(exercise));

        controller.update(1, request);

        ArgumentCaptor<VocabWordDto> captor = ArgumentCaptor.forClass(VocabWordDto.class);
        verify(vocabWordService).update(eq(1), captor.capture());
        VocabWordDto dto = captor.getValue();

        assertEquals(Integer.valueOf(101), dto.getSenses().get(0).getId());
        assertEquals(Integer.valueOf(201), dto.getSenses().get(0).getStructures().get(0).getId());
        assertEquals(Integer.valueOf(301), dto.getSenses().get(0).getStructures().get(0).getExamples().get(0).getId());
        assertEquals(Integer.valueOf(401), dto.getExercises().get(0).getId());
    }
}
```

- [ ] **Step 2: Run the controller test to verify it fails**

Run:

```bash
mvn -pl grid-system -DskipTests=false -Dtest=VocabWordControllerTest test
```

Expected: compilation fails because `VocabSenseRequest`, `VocabStructureRequest`, `VocabExampleRequest`, and `VocabExerciseRequest` do not have `setId(Integer)` yet.

- [ ] **Step 3: Add IDs to nested request classes**

In `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java`, add the shown `id` field to each nested request class:

```java
    @Getter
    @Setter
    public static class VocabSenseRequest implements Serializable {
        @ApiModelProperty(value = "自增ID, 义项ID")
        private Integer id;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;
```

```java
    @Getter
    @Setter
    public static class VocabStructureRequest implements Serializable {
        @ApiModelProperty(value = "自增ID, 结构搭配ID")
        private Integer id;

        @ApiModelProperty(value = "结构搭配文案")
        private String pattern;
```

```java
    @Getter
    @Setter
    public static class VocabExerciseRequest implements Serializable {
        @ApiModelProperty(value = "练习题目唯一ID")
        private Integer id;

        @ApiModelProperty(value = "题目类型")
        private String questionType;
```

```java
    @Getter
    @Setter
    public static class VocabExampleRequest implements Serializable {
        @ApiModelProperty(value = "例句唯一ID")
        private Integer id;

        @ApiModelProperty(value = "例句中文文案")
        private String sentence;
```

- [ ] **Step 4: Map request IDs into DTOs**

In `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`, update these methods:

```java
    private VocabSenseDto toSenseDto(VocabWordCreateRequest.VocabSenseRequest request) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(request.getId());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setChineseDef(request.getChineseDef());
        dto.setDefAudioId(request.getDefAudioId());
        dto.setTranslations(request.getTranslations());
        dto.setSynonyms(request.getSynonyms());
        dto.setAntonyms(request.getAntonyms());
        dto.setRelatedForward(request.getRelatedForward());
        dto.setRelatedBackward(request.getRelatedBackward());
        dto.setSenseOrder(request.getSenseOrder());
        dto.setStructures(toStructureDtoList(request.getStructures()));
        return dto;
    }
```

```java
    private VocabStructureDto toStructureDto(VocabWordCreateRequest.VocabStructureRequest request) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(request.getId());
        dto.setPattern(request.getPattern());
        dto.setStructureOrder(request.getStructureOrder());
        dto.setExamples(toExampleDtoList(request.getExamples()));
        return dto;
    }
```

```java
    private VocabExerciseDto toExerciseDto(VocabWordCreateRequest.VocabExerciseRequest request) {
        VocabExerciseDto dto = new VocabExerciseDto();
        dto.setId(request.getId());
        dto.setQuestionType(request.getQuestionType());
        dto.setQuestionText(request.getQuestionText());
        dto.setOptions(request.getOptions());
        dto.setAnswers(request.getAnswers());
        dto.setExerciseOrder(request.getExerciseOrder());
        return dto;
    }
```

```java
    private VocabExampleDto toExampleDto(VocabWordCreateRequest.VocabExampleRequest request) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(request.getId());
        dto.setSentence(request.getSentence());
        dto.setAudioId(request.getAudioId());
        dto.setPinyin(request.getPinyin());
        dto.setTranslations(request.getTranslations());
        dto.setExampleOrder(request.getExampleOrder());
        return dto;
    }
```

- [ ] **Step 5: Run the controller test to verify it passes**

Run:

```bash
mvn -pl grid-system -DskipTests=false -Dtest=VocabWordControllerTest test
```

Expected: `VocabWordControllerTest` passes.

- [ ] **Step 6: Commit if explicitly approved by the user**

If the user has explicitly requested commits for this implementation, run:

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java grid-system/src/test/java/com/naon/grid/backend/rest/controller/VocabWordControllerTest.java
git commit -m "feat: map vocabulary child ids on update"
```

---

### Task 2: Add service diff tests

**Files:**
- Create: `grid-system/src/test/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImplTest.java`

- [ ] **Step 1: Write failing tests for diff update behavior**

Create `grid-system/src/test/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImplTest.java` with this content:

```java
package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.vocabulary.VocabExample;
import com.naon.grid.backend.domain.vocabulary.VocabExercise;
import com.naon.grid.backend.domain.vocabulary.VocabSense;
import com.naon.grid.backend.domain.vocabulary.VocabStructure;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import com.naon.grid.backend.repo.vocabulary.VocabExampleRepository;
import com.naon.grid.backend.repo.vocabulary.VocabExerciseRepository;
import com.naon.grid.backend.repo.vocabulary.VocabSenseRepository;
import com.naon.grid.backend.repo.vocabulary.VocabStructureRepository;
import com.naon.grid.backend.repo.vocabulary.VocabWordRepository;
import com.naon.grid.backend.service.vocabulary.dto.VocabExampleDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabExerciseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabWordMapper;
import com.naon.grid.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VocabWordServiceImplTest {

    @Mock
    private VocabWordRepository vocabWordRepository;
    @Mock
    private VocabSenseRepository vocabSenseRepository;
    @Mock
    private VocabStructureRepository vocabStructureRepository;
    @Mock
    private VocabExampleRepository vocabExampleRepository;
    @Mock
    private VocabExerciseRepository vocabExerciseRepository;
    @Mock
    private VocabWordMapper vocabWordMapper;

    private VocabWordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VocabWordServiceImpl(
                vocabWordRepository,
                vocabSenseRepository,
                vocabStructureRepository,
                vocabExampleRepository,
                vocabExerciseRepository,
                vocabWordMapper
        );
    }

    @Test
    void updateDiffsAllVocabularyChildren() {
        VocabWord word = new VocabWord();
        word.setId(1);
        word.setWord("旧词");
        when(vocabWordRepository.findById(1)).thenReturn(Optional.of(word));

        VocabSense existingSense = sense(10, 1, "旧义项");
        VocabSense deletedSense = sense(11, 1, "删除义项");
        when(vocabSenseRepository.findByWordId(1)).thenReturn(Arrays.asList(existingSense, deletedSense));

        VocabStructure existingStructure = structure(20, 1, 10, "旧结构");
        VocabStructure deletedStructure = structure(21, 1, 10, "删除结构");
        when(vocabStructureRepository.findBySenseId(10)).thenReturn(Arrays.asList(existingStructure, deletedStructure));

        VocabExample existingExample = example(30, 1, 10, 20, "旧例句");
        VocabExample deletedExample = example(31, 1, 10, 20, "删除例句");
        when(vocabExampleRepository.findByStructureId(20)).thenReturn(Arrays.asList(existingExample, deletedExample));
        when(vocabExampleRepository.findBySenseId(11)).thenReturn(Collections.singletonList(example(32, 1, 11, 22, "义项删除例句")));
        when(vocabStructureRepository.findBySenseId(11)).thenReturn(Collections.singletonList(structure(22, 1, 11, "义项删除结构")));

        VocabExercise existingExercise = exercise(40, 1, "旧题目");
        VocabExercise deletedExercise = exercise(41, 1, "删除题目");
        when(vocabExerciseRepository.findByWordId(1)).thenReturn(Arrays.asList(existingExercise, deletedExercise));

        when(vocabSenseRepository.save(argThat(sense -> sense.getId() == null && "新增义项".equals(sense.getChineseDef()))))
                .thenAnswer(invocation -> {
                    VocabSense saved = invocation.getArgument(0);
                    saved.setId(12);
                    return saved;
                });
        when(vocabStructureRepository.save(argThat(structure -> structure.getId() == null && Integer.valueOf(12).equals(structure.getSenseId()))))
                .thenAnswer(invocation -> {
                    VocabStructure saved = invocation.getArgument(0);
                    saved.setId(23);
                    return saved;
                });

        VocabWordDto dto = new VocabWordDto();
        dto.setWord("新词");
        dto.setSenses(Arrays.asList(
                senseDto(10, "新义项", Collections.singletonList(
                        structureDto(20, "新结构", Collections.singletonList(exampleDto(30, "新例句")))
                )),
                senseDto(null, "新增义项", Collections.singletonList(
                        structureDto(null, "新增结构", Collections.singletonList(exampleDto(null, "新增例句")))
                ))
        ));
        dto.setExercises(Arrays.asList(
                exerciseDto(40, "新题目"),
                exerciseDto(null, "新增题目")
        ));

        service.update(1, dto);

        verify(vocabWordRepository).save(argThat(saved -> "新词".equals(saved.getWord())));
        verify(vocabSenseRepository).save(argThat(saved -> Integer.valueOf(10).equals(saved.getId()) && "新义项".equals(saved.getChineseDef())));
        verify(vocabSenseRepository).save(argThat(saved -> saved.getId() == null && "新增义项".equals(saved.getChineseDef())));
        verify(vocabSenseRepository).deleteAll(argThat(list -> containsSenseId(list, 11)));

        verify(vocabStructureRepository).save(argThat(saved -> Integer.valueOf(20).equals(saved.getId()) && "新结构".equals(saved.getPattern())));
        verify(vocabStructureRepository).save(argThat(saved -> saved.getId() == null && Integer.valueOf(12).equals(saved.getSenseId()) && "新增结构".equals(saved.getPattern())));
        verify(vocabStructureRepository).deleteAll(argThat(list -> containsStructureId(list, 21)));

        verify(vocabExampleRepository).save(argThat(saved -> Integer.valueOf(30).equals(saved.getId()) && "新例句".equals(saved.getSentence())));
        verify(vocabExampleRepository).save(argThat(saved -> saved.getId() == null && Integer.valueOf(23).equals(saved.getStructureId()) && "新增例句".equals(saved.getSentence())));
        verify(vocabExampleRepository).deleteAll(argThat(list -> containsExampleId(list, 31)));
        verify(vocabExampleRepository).deleteAll(argThat(list -> containsExampleId(list, 32)));

        verify(vocabExerciseRepository).save(argThat(saved -> Integer.valueOf(40).equals(saved.getId()) && "新题目".equals(saved.getQuestionText())));
        verify(vocabExerciseRepository).save(argThat(saved -> saved.getId() == null && "新增题目".equals(saved.getQuestionText())));
        verify(vocabExerciseRepository).deleteAll(argThat(list -> containsExerciseId(list, 41)));
    }

    @Test
    void updateRejectsDuplicateSenseId() {
        VocabWord word = new VocabWord();
        word.setId(1);
        when(vocabWordRepository.findById(1)).thenReturn(Optional.of(word));
        when(vocabSenseRepository.findByWordId(1)).thenReturn(Collections.singletonList(sense(10, 1, "旧义项")));

        VocabWordDto dto = new VocabWordDto();
        dto.setSenses(Arrays.asList(senseDto(10, "义项1", null), senseDto(10, "义项2", null)));
        dto.setExercises(Collections.emptyList());

        assertThrows(BadRequestException.class, () -> service.update(1, dto));
        verify(vocabSenseRepository, never()).save(any(VocabSense.class));
    }

    @Test
    void updateRejectsStructureIdOutsideCurrentSense() {
        VocabWord word = new VocabWord();
        word.setId(1);
        when(vocabWordRepository.findById(1)).thenReturn(Optional.of(word));
        when(vocabSenseRepository.findByWordId(1)).thenReturn(Collections.singletonList(sense(10, 1, "旧义项")));
        when(vocabStructureRepository.findBySenseId(10)).thenReturn(Collections.singletonList(structure(20, 1, 10, "旧结构")));

        VocabWordDto dto = new VocabWordDto();
        dto.setSenses(Collections.singletonList(
                senseDto(10, "新义项", Collections.singletonList(structureDto(99, "错误结构", null)))
        ));
        dto.setExercises(Collections.emptyList());

        assertThrows(BadRequestException.class, () -> service.update(1, dto));
        verify(vocabStructureRepository, never()).save(any(VocabStructure.class));
    }

    private VocabSense sense(Integer id, Integer wordId, String chineseDef) {
        VocabSense sense = new VocabSense();
        sense.setId(id);
        sense.setWordId(wordId);
        sense.setChineseDef(chineseDef);
        return sense;
    }

    private VocabStructure structure(Integer id, Integer wordId, Integer senseId, String pattern) {
        VocabStructure structure = new VocabStructure();
        structure.setId(id);
        structure.setWordId(wordId);
        structure.setSenseId(senseId);
        structure.setPattern(pattern);
        return structure;
    }

    private VocabExample example(Integer id, Integer wordId, Integer senseId, Integer structureId, String sentence) {
        VocabExample example = new VocabExample();
        example.setId(id);
        example.setWordId(wordId);
        example.setSenseId(senseId);
        example.setStructureId(structureId);
        example.setSentence(sentence);
        return example;
    }

    private VocabExercise exercise(Integer id, Integer wordId, String questionText) {
        VocabExercise exercise = new VocabExercise();
        exercise.setId(id);
        exercise.setWordId(wordId);
        exercise.setQuestionText(questionText);
        return exercise;
    }

    private VocabSenseDto senseDto(Integer id, String chineseDef, List<VocabStructureDto> structures) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(id);
        dto.setChineseDef(chineseDef);
        dto.setStructures(structures);
        return dto;
    }

    private VocabStructureDto structureDto(Integer id, String pattern, List<VocabExampleDto> examples) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(id);
        dto.setPattern(pattern);
        dto.setExamples(examples);
        return dto;
    }

    private VocabExampleDto exampleDto(Integer id, String sentence) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(id);
        dto.setSentence(sentence);
        return dto;
    }

    private VocabExerciseDto exerciseDto(Integer id, String questionText) {
        VocabExerciseDto dto = new VocabExerciseDto();
        dto.setId(id);
        dto.setQuestionText(questionText);
        return dto;
    }

    private boolean containsSenseId(List<VocabSense> list, Integer id) {
        return list.stream().anyMatch(item -> id.equals(item.getId()));
    }

    private boolean containsStructureId(List<VocabStructure> list, Integer id) {
        return list.stream().anyMatch(item -> id.equals(item.getId()));
    }

    private boolean containsExampleId(List<VocabExample> list, Integer id) {
        return list.stream().anyMatch(item -> id.equals(item.getId()));
    }

    private boolean containsExerciseId(List<VocabExercise> list, Integer id) {
        return list.stream().anyMatch(item -> id.equals(item.getId()));
    }
}
```

- [ ] **Step 2: Run service tests to verify they fail**

Run:

```bash
mvn -pl grid-system -DskipTests=false -Dtest=VocabWordServiceImplTest test
```

Expected: tests fail because `VocabWordServiceImpl.update()` still calls `deleteChildren(id)` and `saveChildren(resources, id)`, so existing child entities are deleted and recreated instead of updated by ID. If Task 1 has not been completed, compilation also fails because `BadRequestException` is not imported in the service implementation yet.

- [ ] **Step 3: Commit if explicitly approved by the user**

If the user has explicitly requested commits for this implementation, run:

```bash
git add grid-system/src/test/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImplTest.java
git commit -m "test: cover vocabulary diff update behavior"
```

---

### Task 3: Implement service diff update

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java:1-270`
- Test: `grid-system/src/test/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImplTest.java`

- [ ] **Step 1: Add BadRequestException import**

In `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`, change the imports so they include `BadRequestException`:

```java
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
```

- [ ] **Step 2: Replace update child handling**

In `VocabWordServiceImpl.update()`, replace:

```java
        deleteChildren(id);
        saveChildren(resources, id);
```

with:

```java
        syncSenses(id, resources.getSenses());
        syncExercises(id, resources.getExercises());
```

- [ ] **Step 3: Add sync methods after `deleteChildren`**

In `VocabWordServiceImpl.java`, add these methods after `deleteChildren(Integer wordId)`:

```java
    private void syncSenses(Integer wordId, List<VocabSenseDto> submittedDtos) {
        List<VocabSenseDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<VocabSense> existing = vocabSenseRepository.findByWordId(wordId);
        Map<Integer, VocabSense> existingMap = new HashMap<>();
        for (VocabSense sense : existing) {
            existingMap.put(sense.getId(), sense);
        }

        Set<Integer> submittedIds = new HashSet<>();

        for (VocabSenseDto dto : submitted) {
            if (dto.getId() == null) {
                VocabSense sense = convertToSenseEntity(dto, wordId);
                sense = vocabSenseRepository.save(sense);
                syncStructures(wordId, sense.getId(), dto.getStructures());
                continue;
            }
            if (!submittedIds.add(dto.getId())) {
                throw new BadRequestException("义项ID重复: " + dto.getId());
            }
            VocabSense sense = existingMap.get(dto.getId());
            if (sense == null) {
                throw new BadRequestException("义项ID不属于当前词汇: " + dto.getId());
            }
            updateSense(sense, dto);
            vocabSenseRepository.save(sense);
            syncStructures(wordId, sense.getId(), dto.getStructures());
        }

        List<VocabSense> toDelete = new ArrayList<>();
        for (VocabSense sense : existing) {
            if (!submittedIds.contains(sense.getId())) {
                vocabExampleRepository.deleteAll(vocabExampleRepository.findBySenseId(sense.getId()));
                vocabStructureRepository.deleteAll(vocabStructureRepository.findBySenseId(sense.getId()));
                toDelete.add(sense);
            }
        }
        vocabSenseRepository.deleteAll(toDelete);
    }

    private void syncStructures(Integer wordId, Integer senseId, List<VocabStructureDto> submittedDtos) {
        List<VocabStructureDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<VocabStructure> existing = vocabStructureRepository.findBySenseId(senseId);
        Map<Integer, VocabStructure> existingMap = new HashMap<>();
        for (VocabStructure structure : existing) {
            existingMap.put(structure.getId(), structure);
        }

        Set<Integer> submittedIds = new HashSet<>();

        for (VocabStructureDto dto : submitted) {
            if (dto.getId() == null) {
                VocabStructure structure = convertToStructureEntity(dto, wordId, senseId);
                structure = vocabStructureRepository.save(structure);
                syncExamples(wordId, senseId, structure.getId(), dto.getExamples());
                continue;
            }
            if (!submittedIds.add(dto.getId())) {
                throw new BadRequestException("搭配ID重复: " + dto.getId());
            }
            VocabStructure structure = existingMap.get(dto.getId());
            if (structure == null) {
                throw new BadRequestException("搭配ID不属于当前义项: " + dto.getId());
            }
            updateStructure(structure, dto);
            vocabStructureRepository.save(structure);
            syncExamples(wordId, senseId, structure.getId(), dto.getExamples());
        }

        List<VocabStructure> toDelete = new ArrayList<>();
        for (VocabStructure structure : existing) {
            if (!submittedIds.contains(structure.getId())) {
                vocabExampleRepository.deleteAll(vocabExampleRepository.findByStructureId(structure.getId()));
                toDelete.add(structure);
            }
        }
        vocabStructureRepository.deleteAll(toDelete);
    }

    private void syncExamples(Integer wordId, Integer senseId, Integer structureId, List<VocabExampleDto> submittedDtos) {
        List<VocabExampleDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<VocabExample> existing = vocabExampleRepository.findByStructureId(structureId);
        Map<Integer, VocabExample> existingMap = new HashMap<>();
        for (VocabExample example : existing) {
            existingMap.put(example.getId(), example);
        }

        Set<Integer> submittedIds = new HashSet<>();
        List<VocabExample> toSave = new ArrayList<>();

        for (VocabExampleDto dto : submitted) {
            if (dto.getId() == null) {
                toSave.add(convertToExampleEntity(dto, wordId, senseId, structureId));
                continue;
            }
            if (!submittedIds.add(dto.getId())) {
                throw new BadRequestException("例句ID重复: " + dto.getId());
            }
            VocabExample example = existingMap.get(dto.getId());
            if (example == null) {
                throw new BadRequestException("例句ID不属于当前搭配: " + dto.getId());
            }
            updateExample(example, dto);
            toSave.add(example);
        }

        List<VocabExample> toDelete = new ArrayList<>();
        for (VocabExample example : existing) {
            if (!submittedIds.contains(example.getId())) {
                toDelete.add(example);
            }
        }
        vocabExampleRepository.deleteAll(toDelete);
        vocabExampleRepository.saveAll(toSave);
    }

    private void syncExercises(Integer wordId, List<VocabExerciseDto> submittedDtos) {
        List<VocabExerciseDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<VocabExercise> existing = vocabExerciseRepository.findByWordId(wordId);
        Map<Integer, VocabExercise> existingMap = new HashMap<>();
        for (VocabExercise exercise : existing) {
            existingMap.put(exercise.getId(), exercise);
        }

        Set<Integer> submittedIds = new HashSet<>();
        List<VocabExercise> toSave = new ArrayList<>();

        for (VocabExerciseDto dto : submitted) {
            if (dto.getId() == null) {
                toSave.add(convertToExerciseEntity(dto, wordId));
                continue;
            }
            if (!submittedIds.add(dto.getId())) {
                throw new BadRequestException("练习题ID重复: " + dto.getId());
            }
            VocabExercise exercise = existingMap.get(dto.getId());
            if (exercise == null) {
                throw new BadRequestException("练习题ID不属于当前词汇: " + dto.getId());
            }
            updateExercise(exercise, dto);
            toSave.add(exercise);
        }

        List<VocabExercise> toDelete = new ArrayList<>();
        for (VocabExercise exercise : existing) {
            if (!submittedIds.contains(exercise.getId())) {
                toDelete.add(exercise);
            }
        }
        vocabExerciseRepository.deleteAll(toDelete);
        vocabExerciseRepository.saveAll(toSave);
    }
```

- [ ] **Step 4: Add entity update methods before convert-to-entity methods**

In `VocabWordServiceImpl.java`, add these methods before `convertToSenseEntity`:

```java
    private void updateSense(VocabSense entity, VocabSenseDto dto) {
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setChineseDef(dto.getChineseDef());
        entity.setDefAudioId(dto.getDefAudioId());
        entity.setTranslations(dto.getTranslations());
        entity.setSynonyms(dto.getSynonyms());
        entity.setAntonyms(dto.getAntonyms());
        entity.setRelatedForward(dto.getRelatedForward());
        entity.setRelatedBackward(dto.getRelatedBackward());
        entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
    }

    private void updateStructure(VocabStructure entity, VocabStructureDto dto) {
        entity.setPattern(dto.getPattern());
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
    }

    private void updateExample(VocabExample entity, VocabExampleDto dto) {
        entity.setSentence(dto.getSentence());
        entity.setAudioId(dto.getAudioId());
        entity.setPinyin(dto.getPinyin());
        entity.setTranslations(dto.getTranslations());
        entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
    }

    private void updateExercise(VocabExercise entity, VocabExerciseDto dto) {
        entity.setQuestionType(dto.getQuestionType());
        entity.setQuestionText(dto.getQuestionText());
        entity.setOptions(dto.getOptions());
        entity.setAnswers(dto.getAnswers());
        entity.setExerciseOrder(dto.getExerciseOrder() != null ? dto.getExerciseOrder() : 0);
    }
```

- [ ] **Step 5: Run service tests**

Run:

```bash
mvn -pl grid-system -DskipTests=false -Dtest=VocabWordServiceImplTest test
```

Expected: `VocabWordServiceImplTest` passes.

- [ ] **Step 6: Commit if explicitly approved by the user**

If the user has explicitly requested commits for this implementation, run:

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java grid-system/src/test/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImplTest.java
git commit -m "feat: diff update vocabulary children"
```

---

### Task 4: Verify compile and full targeted tests

**Files:**
- Test: `grid-system/src/test/java/com/naon/grid/backend/rest/controller/VocabWordControllerTest.java`
- Test: `grid-system/src/test/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImplTest.java`

- [ ] **Step 1: Run both targeted tests**

Run:

```bash
mvn -pl grid-system -DskipTests=false -Dtest=VocabWordControllerTest,VocabWordServiceImplTest test
```

Expected: both test classes pass.

- [ ] **Step 2: Run module compile with dependencies**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: Maven compile succeeds for `grid-system` and required modules.

- [ ] **Step 3: Check git diff**

Run:

```bash
git diff -- grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java grid-system/src/test/java/com/naon/grid/backend/rest/controller/VocabWordControllerTest.java grid-system/src/test/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImplTest.java
```

Expected: diff only contains request ID fields, controller ID mapping, service diff update logic, and the two targeted tests.

- [ ] **Step 4: Commit if explicitly approved by the user**

If the user has explicitly requested a final commit and previous task commits were not created, run:

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java grid-system/src/test/java/com/naon/grid/backend/rest/controller/VocabWordControllerTest.java grid-system/src/test/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImplTest.java docs/superpowers/specs/2026-05-26-vocab-word-update-diff-design.md docs/superpowers/plans/2026-05-26-vocab-word-update-diff.md
git commit -m "feat: optimize vocabulary update with diff sync"
```

---

## Self-Review

- Spec coverage: request child IDs are covered by Task 1; controller DTO mapping is covered by Task 1; full child diff for senses, structures, examples, and exercises is covered by Tasks 2 and 3; rollback-triggering `BadRequestException` cases are covered by Task 2; compile and targeted verification are covered by Task 4.
- Placeholder scan: no placeholder steps are left; each code-changing step includes concrete code or an exact replacement.
- Type consistency: plan uses existing entity, DTO, repository, controller, and service class names from the current codebase. New methods are private methods in `VocabWordServiceImpl` and are referenced consistently.
