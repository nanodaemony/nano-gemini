# Character SQL API Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align the backend character management code with the updated character SQL schema and request/response models, including a reusable `example_sentence` foundation used only by character word sentences in this change.

**Architecture:** Keep the existing character draft/review/publish workflow intact: create/update write only `draft_content`, review changes only status, publish synchronizes main table, comparison table, word table, and one `example_sentence` per character word. Add a focused reusable `ExampleSentenceService`, but do not connect vocabulary publish/query flows in this plan.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Spring Data JPA, Lombok, Fastjson2 via `JsonUtils`, JUnit 5, Mockito, Maven.

---

## Scope Guardrails

- Do not modify `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`.
- Do not modify vocabulary create/update/review/publish/query behavior.
- Do not change `CharCharacterController` endpoint paths or method semantics.
- Do not introduce JPA relationships such as `@ManyToOne`; use IDs and repositories.
- Preserve the character draft workflow from `docs/汉字管理接口使用流程.md`.
- Treat one `CharWord` as having at most one active `ExampleSentence` with `bizType = SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode()`.

## File Structure

### Create

- `grid-system/src/main/java/com/naon/grid/backend/domain/common/ExampleSentence.java`
  JPA entity for `example_sentence`.
- `grid-system/src/main/java/com/naon/grid/backend/repo/common/ExampleSentenceRepository.java`
  Repository queries by `bizType`, `bizId`, and `status`.
- `grid-system/src/main/java/com/naon/grid/backend/service/common/dto/ExampleSentenceDto.java`
  Service DTO for example sentence records.
- `grid-system/src/main/java/com/naon/grid/backend/service/common/ExampleSentenceService.java`
  Reusable service boundary.
- `grid-system/src/main/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImpl.java`
  Implements one-active-sentence synchronization.
- `grid-system/src/test/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImplTest.java`
  Unit tests for reusable example sentence behavior.
- `grid-system/src/test/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapperTest.java`
  Unit tests for new character request/response mapping.

### Rename / Replace

- Rename `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharDiscrimination.java` to `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharComparison.java`.
- Rename `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharDiscriminationRepository.java` to `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharComparisonRepository.java`.
- Rename `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java` to `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharComparisonDto.java`.

### Modify

- `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java`
  Align column names and add `radicalId`, `componentCombination`.
- `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java`
  Align `hsk_level`, ``order`` and remove old inline sentence columns.
- `grid-system/src/main/java/com/naon/grid/backend/rest/request/ExampleSentenceRequest.java`
  Change `id` from `long` to `Long`.
- `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`
  Fix `CharWordRequest.id` validation and ensure `sentenceContent` is singular.
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExampleSentenceVO.java`
  Prefer `Long id` and `List<TextTranslationVO>` for response semantics.
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`
  Keep `wordItemSentence` singular and map new main fields.
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`
  Add list-visible new main fields.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java`
  Add `radicalId`, `componentCombination`, and use comparison DTO list.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java`
  Remove old inline example fields and add `ExampleSentenceDto wordItemSentence`.
- `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java`
  Map request/DTO/VO fields, including singular example sentence.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`
  Synchronize new schema, comparison records, word records, and example sentences.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java`
  Keep translation helpers compatible with renamed columns.

---

### Task 1: Add wrapper tests for the confirmed request/response contract

**Files:**
- Create: `grid-system/src/test/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapperTest.java`
- Modify later in Task 2/5: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java`
- Modify later in Task 2: `grid-system/src/main/java/com/naon/grid/backend/rest/request/ExampleSentenceRequest.java`
- Modify later in Task 2: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`
- Modify later in Task 5: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`

- [ ] **Step 1: Create the failing wrapper contract test**

Create `grid-system/src/test/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapperTest.java` with this content:

```java
package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.CharCharacterCreateRequest;
import com.naon.grid.backend.rest.request.ExampleSentenceRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.CharCharacterVO;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CharCharacterWrapperTest {

    @Test
    void toDtoMapsNewCharacterFieldsAndSingleWordSentence() {
        CharCharacterCreateRequest request = new CharCharacterCreateRequest();
        request.setCharacter("你");
        request.setHskLevel("1");
        request.setPinyin("nǐ");
        request.setAudioId(12L);
        request.setTraditional("你");
        request.setRadicalId(3L);
        request.setRadical("亻");
        request.setComponentCombination("亻 + 尔");
        request.setCharDesc("第二人称代词");
        request.setStroke("stroke-json");

        TextTranslationRequest descTranslation = new TextTranslationRequest();
        descTranslation.setLanguage("en");
        descTranslation.setTranslation("you");
        request.setCharDescTranslations(Collections.singletonList(descTranslation));

        CharCharacterCreateRequest.CharWordRequest wordRequest = new CharCharacterCreateRequest.CharWordRequest();
        wordRequest.setId(9);
        wordRequest.setWordItem("你好");
        wordRequest.setHskLevel("1");
        wordRequest.setPinyin("nǐ hǎo");
        wordRequest.setPartOfSpeech("interj.");
        wordRequest.setWordItemTranslations(Collections.singletonList(descTranslation));
        wordRequest.setOrder(7);

        ExampleSentenceRequest sentenceRequest = new ExampleSentenceRequest();
        sentenceRequest.setId(88L);
        sentenceRequest.setSentence("你好，我叫小明。");
        sentenceRequest.setPinyin("nǐ hǎo, wǒ jiào xiǎo míng.");
        sentenceRequest.setAudioId(21L);
        sentenceRequest.setImageId(34L);
        sentenceRequest.setOrder(5);
        sentenceRequest.setTranslations(Collections.singletonList(descTranslation));
        wordRequest.setSentenceContent(sentenceRequest);
        request.setWords(Collections.singletonList(wordRequest));

        CharCharacterDto dto = CharCharacterWrapper.toDto(request);

        assertEquals("你", dto.getCharacter());
        assertEquals("1", dto.getLevel());
        assertEquals("nǐ", dto.getPinyin());
        assertEquals(Long.valueOf(12L), dto.getAudioId());
        assertEquals(Long.valueOf(3L), dto.getRadicalId());
        assertEquals("亻 + 尔", dto.getComponentCombination());
        assertEquals("第二人称代词", dto.getCharDesc());
        assertEquals("stroke-json", dto.getStroke());
        assertEquals(1, dto.getDescTranslations().size());

        assertEquals(1, dto.getWords().size());
        CharWordDto wordDto = dto.getWords().get(0);
        assertEquals(Integer.valueOf(9), wordDto.getId());
        assertEquals("你好", wordDto.getWordItem());
        assertEquals("1", wordDto.getLevel());
        assertEquals(Integer.valueOf(7), wordDto.getWordOrder());
        assertNotNull(wordDto.getWordItemSentence());
        assertEquals(Long.valueOf(88L), wordDto.getWordItemSentence().getId());
        assertEquals("你好，我叫小明。", wordDto.getWordItemSentence().getSentence());
    }

    @Test
    void toVoMapsSingleWordSentence() {
        CharCharacterDto dto = new CharCharacterDto();
        dto.setId(1);
        dto.setCharacter("你");
        dto.setLevel("1");
        dto.setRadicalId(3L);
        dto.setComponentCombination("亻 + 尔");

        CharWordDto wordDto = new CharWordDto();
        wordDto.setId(9);
        wordDto.setCharId(1);
        wordDto.setWordItem("你好");
        wordDto.setLevel("1");
        wordDto.setWordOrder(7);

        ExampleSentenceDto sentenceDto = new ExampleSentenceDto();
        sentenceDto.setId(88L);
        sentenceDto.setSentence("你好，我叫小明。");
        sentenceDto.setPinyin("nǐ hǎo, wǒ jiào xiǎo míng.");
        sentenceDto.setAudioId(21L);
        sentenceDto.setImageId(34L);
        sentenceDto.setOrder(5);
        wordDto.setWordItemSentence(sentenceDto);
        dto.setWords(Collections.singletonList(wordDto));

        CharCharacterVO vo = CharCharacterWrapper.toVO(dto);

        assertEquals(Long.valueOf(3L), vo.getRadicalId());
        assertEquals("亻 + 尔", vo.getComponentCombination());
        assertEquals(1, vo.getWords().size());
        assertNotNull(vo.getWords().get(0).getWordItemSentence());
        assertEquals(Long.valueOf(88L), vo.getWords().get(0).getWordItemSentence().getId());
        assertEquals("你好，我叫小明。", vo.getWords().get(0).getWordItemSentence().getSentence());
    }
}
```

- [ ] **Step 2: Run the wrapper contract test and verify it fails**

Run:

```bash
mvn -pl grid-system -Dtest=CharCharacterWrapperTest test
```

Expected: compilation fails because `ExampleSentenceDto`, `getRadicalId`, `getComponentCombination`, `getWordItemSentence`, and `setSentenceContent` support is not implemented yet.

- [ ] **Step 3: Commit the failing test**

```bash
git add grid-system/src/test/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapperTest.java
git commit -m "test: define character wrapper SQL alignment contract"
```

---

### Task 2: Add the reusable example sentence foundation

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/common/ExampleSentence.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/common/ExampleSentenceRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/common/dto/ExampleSentenceDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/common/ExampleSentenceService.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImpl.java`
- Create: `grid-system/src/test/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImplTest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/ExampleSentenceRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExampleSentenceVO.java`

- [ ] **Step 1: Write the failing service tests**

Create `grid-system/src/test/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImplTest.java`:

```java
package com.naon.grid.backend.service.common.impl;

import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.enums.SentenceBizTypeEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExampleSentenceServiceImplTest {

    private ExampleSentenceRepository repository;
    private ExampleSentenceServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ExampleSentenceRepository.class);
        service = new ExampleSentenceServiceImpl(repository);
    }

    @Test
    void syncOneCreatesSentenceWhenNoIdIsProvided() {
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setSentence("你好，我叫小明。");
        dto.setPinyin("nǐ hǎo, wǒ jiào xiǎo míng.");
        dto.setAudioId(21L);
        dto.setImageId(34L);
        dto.setOrder(5);

        TextTranslation translation = new TextTranslation();
        translation.setLanguage("en");
        translation.setTranslation("Hello, my name is Xiaoming.");
        dto.setTranslations(Collections.singletonList(translation));

        when(repository.findByBizTypeAndBizIdAndStatus(
                SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, StatusEnum.ENABLED.getCode()))
                .thenReturn(Collections.emptyList());
        when(repository.save(any(ExampleSentence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncOne(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, dto);

        ArgumentCaptor<ExampleSentence> captor = ArgumentCaptor.forClass(ExampleSentence.class);
        verify(repository).save(captor.capture());
        ExampleSentence saved = captor.getValue();
        assertEquals(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), saved.getBizType());
        assertEquals(Long.valueOf(9L), saved.getBizId());
        assertEquals("你好，我叫小明。", saved.getSentence());
        assertEquals(Long.valueOf(21L), saved.getAudioId());
        assertEquals(Long.valueOf(34L), saved.getImageId());
        assertEquals(Integer.valueOf(5), saved.getSentenceOrder());
        assertEquals(StatusEnum.ENABLED.getCode(), saved.getStatus());
        assertEquals("Hello, my name is Xiaoming.", JsonUtils.parseTranslationList(saved.getTranslations()).get(0).getTranslation());
    }

    @Test
    void syncOneUpdatesOwnedSentenceAndDisablesOtherActiveRows() {
        ExampleSentence existing = new ExampleSentence();
        existing.setId(88L);
        existing.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        existing.setBizId(9L);
        existing.setSentence("旧例句");
        existing.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentence duplicate = new ExampleSentence();
        duplicate.setId(89L);
        duplicate.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        duplicate.setBizId(9L);
        duplicate.setSentence("重复例句");
        duplicate.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(88L);
        dto.setSentence("新例句");

        when(repository.findById(88L)).thenReturn(Optional.of(existing));
        when(repository.findByBizTypeAndBizIdAndStatus(
                SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, StatusEnum.ENABLED.getCode()))
                .thenReturn(Arrays.asList(existing, duplicate));
        when(repository.save(any(ExampleSentence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncOne(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, dto);

        assertEquals("新例句", existing.getSentence());
        assertEquals(StatusEnum.DISABLED.getCode(), duplicate.getStatus());
        verify(repository).save(existing);
        verify(repository).save(duplicate);
    }

    @Test
    void syncOneRejectsSentenceIdOwnedByAnotherBizObject() {
        ExampleSentence existing = new ExampleSentence();
        existing.setId(88L);
        existing.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        existing.setBizId(10L);
        existing.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(88L);
        dto.setSentence("新例句");

        when(repository.findById(88L)).thenReturn(Optional.of(existing));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.syncOne(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, dto));

        assertTrue(exception.getMessage().contains("例句ID不属于当前业务对象"));
        verify(repository, never()).save(any(ExampleSentence.class));
    }

    @Test
    void syncOneDisablesExistingSentencesWhenRequestIsEmpty() {
        ExampleSentence existing = new ExampleSentence();
        existing.setId(88L);
        existing.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        existing.setBizId(9L);
        existing.setSentence("旧例句");
        existing.setStatus(StatusEnum.ENABLED.getCode());

        when(repository.findByBizTypeAndBizIdAndStatus(
                SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, StatusEnum.ENABLED.getCode()))
                .thenReturn(Collections.singletonList(existing));

        service.syncOne(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), 9L, null);

        assertEquals(StatusEnum.DISABLED.getCode(), existing.getStatus());
        verify(repository).save(existing);
    }

    @Test
    void findByBizIdsReturnsOneSentencePerBizId() {
        ExampleSentence first = new ExampleSentence();
        first.setId(88L);
        first.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        first.setBizId(9L);
        first.setSentence("第一条例句");
        first.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentence second = new ExampleSentence();
        second.setId(90L);
        second.setBizType(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode());
        second.setBizId(10L);
        second.setSentence("第二条例句");
        second.setStatus(StatusEnum.ENABLED.getCode());

        when(repository.findByBizTypeAndBizIdInAndStatus(
                eq(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode()), eq(Arrays.asList(9L, 10L)), eq(StatusEnum.ENABLED.getCode())))
                .thenReturn(Arrays.asList(first, second));

        Map<Long, ExampleSentenceDto> result = service.findByBizIds(
                SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), Arrays.asList(9L, 10L));

        assertEquals(2, result.size());
        assertEquals("第一条例句", result.get(9L).getSentence());
        assertEquals("第二条例句", result.get(10L).getSentence());
    }
}
```

- [ ] **Step 2: Run the service tests and verify they fail**

Run:

```bash
mvn -pl grid-system -Dtest=ExampleSentenceServiceImplTest test
```

Expected: compilation fails because the common example sentence classes do not exist.

- [ ] **Step 3: Implement `ExampleSentence` entity**

Create `grid-system/src/main/java/com/naon/grid/backend/domain/common/ExampleSentence.java`:

```java
package com.naon.grid.backend.domain.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "example_sentence")
public class ExampleSentence implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "例句ID", hidden = true)
    private Long id;

    @Column(name = "biz_type", nullable = false, length = 64)
    private String bizType;

    @Column(name = "biz_id", nullable = false)
    private Long bizId;

    @Column(name = "sentence", nullable = false, length = 1024)
    private String sentence;

    @Column(name = "pinyin", length = 2048)
    private String pinyin;

    @Column(name = "audio_id")
    private Long audioId;

    @Column(name = "translations", columnDefinition = "text")
    private String translations;

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "`order`", nullable = false)
    private Integer sentenceOrder = 0;

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

- [ ] **Step 4: Implement `ExampleSentenceRepository`**

Create `grid-system/src/main/java/com/naon/grid/backend/repo/common/ExampleSentenceRepository.java`:

```java
package com.naon.grid.backend.repo.common;

import com.naon.grid.backend.domain.common.ExampleSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ExampleSentenceRepository extends JpaRepository<ExampleSentence, Long> {

    List<ExampleSentence> findByBizTypeAndBizIdAndStatus(String bizType, Long bizId, Integer status);

    List<ExampleSentence> findByBizTypeAndBizIdInAndStatus(String bizType, Collection<Long> bizIds, Integer status);
}
```

- [ ] **Step 5: Implement `ExampleSentenceDto`**

Create `grid-system/src/main/java/com/naon/grid/backend/service/common/dto/ExampleSentenceDto.java`:

```java
package com.naon.grid.backend.service.common.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class ExampleSentenceDto implements Serializable {

    @ApiModelProperty(value = "例句ID")
    private Long id;

    @ApiModelProperty(value = "业务类型")
    private String bizType;

    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "例句外文翻译")
    private List<TextTranslation> translations;

    @ApiModelProperty(value = "例句图片ID")
    private Long imageId;

    @ApiModelProperty(value = "排序权重")
    private Integer order;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "有效状态")
    private Integer status;
}
```

- [ ] **Step 6: Implement `ExampleSentenceService`**

Create `grid-system/src/main/java/com/naon/grid/backend/service/common/ExampleSentenceService.java`:

```java
package com.naon.grid.backend.service.common;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;

import java.util.Collection;
import java.util.Map;

public interface ExampleSentenceService {

    ExampleSentenceDto findOne(String bizType, Long bizId);

    Map<Long, ExampleSentenceDto> findByBizIds(String bizType, Collection<Long> bizIds);

    ExampleSentenceDto syncOne(String bizType, Long bizId, ExampleSentenceDto sentence);

    void disableByBizIds(String bizType, Collection<Long> bizIds);
}
```

- [ ] **Step 7: Implement `ExampleSentenceServiceImpl`**

Create `grid-system/src/main/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImpl.java`:

```java
package com.naon.grid.backend.service.common.impl;

import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExampleSentenceServiceImpl implements ExampleSentenceService {

    private final ExampleSentenceRepository exampleSentenceRepository;

    @Override
    public ExampleSentenceDto findOne(String bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            return null;
        }
        List<ExampleSentence> sentences = exampleSentenceRepository.findByBizTypeAndBizIdAndStatus(
                bizType, bizId, StatusEnum.ENABLED.getCode());
        if (sentences == null || sentences.isEmpty()) {
            return null;
        }
        sentences.sort(activeSentenceComparator());
        return toDto(sentences.get(0));
    }

    @Override
    public Map<Long, ExampleSentenceDto> findByBizIds(String bizType, Collection<Long> bizIds) {
        if (bizType == null || bizIds == null || bizIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ExampleSentence> sentences = exampleSentenceRepository.findByBizTypeAndBizIdInAndStatus(
                bizType, bizIds, StatusEnum.ENABLED.getCode());
        if (sentences == null || sentences.isEmpty()) {
            return Collections.emptyMap();
        }
        sentences.sort(activeSentenceComparator());
        Map<Long, ExampleSentenceDto> result = new LinkedHashMap<>();
        for (ExampleSentence sentence : sentences) {
            if (!result.containsKey(sentence.getBizId())) {
                result.put(sentence.getBizId(), toDto(sentence));
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExampleSentenceDto syncOne(String bizType, Long bizId, ExampleSentenceDto sentence) {
        if (bizType == null || bizId == null) {
            throw new BadRequestException("例句业务类型或业务ID不能为空");
        }
        if (sentence == null || StringUtils.isBlank(sentence.getSentence())) {
            disableExisting(bizType, bizId, null);
            return null;
        }

        ExampleSentence entity;
        if (sentence.getId() == null) {
            entity = new ExampleSentence();
            entity.setBizType(bizType);
            entity.setBizId(bizId);
        } else {
            entity = exampleSentenceRepository.findById(sentence.getId())
                    .orElseThrow(() -> new BadRequestException("例句不存在: " + sentence.getId()));
            if (!bizType.equals(entity.getBizType()) || !bizId.equals(entity.getBizId())
                    || StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
                throw new BadRequestException("例句ID不属于当前业务对象: " + sentence.getId());
            }
        }

        apply(entity, sentence);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        entity = exampleSentenceRepository.save(entity);
        disableExisting(bizType, bizId, entity.getId());
        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableByBizIds(String bizType, Collection<Long> bizIds) {
        if (bizType == null || bizIds == null || bizIds.isEmpty()) {
            return;
        }
        List<ExampleSentence> existing = exampleSentenceRepository.findByBizTypeAndBizIdInAndStatus(
                bizType, bizIds, StatusEnum.ENABLED.getCode());
        if (existing == null || existing.isEmpty()) {
            return;
        }
        for (ExampleSentence sentence : existing) {
            sentence.setStatus(StatusEnum.DISABLED.getCode());
            exampleSentenceRepository.save(sentence);
        }
    }

    private void disableExisting(String bizType, Long bizId, Long keepId) {
        List<ExampleSentence> existing = exampleSentenceRepository.findByBizTypeAndBizIdAndStatus(
                bizType, bizId, StatusEnum.ENABLED.getCode());
        if (existing == null || existing.isEmpty()) {
            return;
        }
        for (ExampleSentence sentence : existing) {
            if (keepId != null && keepId.equals(sentence.getId())) {
                continue;
            }
            sentence.setStatus(StatusEnum.DISABLED.getCode());
            exampleSentenceRepository.save(sentence);
        }
    }

    private void apply(ExampleSentence entity, ExampleSentenceDto dto) {
        entity.setSentence(dto.getSentence());
        entity.setPinyin(dto.getPinyin());
        entity.setAudioId(dto.getAudioId());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setImageId(dto.getImageId());
        entity.setSentenceOrder(dto.getOrder() != null ? dto.getOrder() : 0);
    }

    private ExampleSentenceDto toDto(ExampleSentence entity) {
        if (entity == null) {
            return null;
        }
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(entity.getId());
        dto.setBizType(entity.getBizType());
        dto.setBizId(entity.getBizId());
        dto.setSentence(entity.getSentence());
        dto.setPinyin(entity.getPinyin());
        dto.setAudioId(entity.getAudioId());
        dto.setTranslations(JsonUtils.parseTranslationList(entity.getTranslations()));
        dto.setImageId(entity.getImageId());
        dto.setOrder(entity.getSentenceOrder());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private Comparator<ExampleSentence> activeSentenceComparator() {
        return Comparator.comparing(ExampleSentence::getSentenceOrder,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExampleSentence::getUpdateTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExampleSentence::getId,
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
```

- [ ] **Step 8: Update `ExampleSentenceRequest`**

Modify `grid-system/src/main/java/com/naon/grid/backend/rest/request/ExampleSentenceRequest.java` so the `id` field is `Long`:

```java
@ApiModelProperty("例句ID, 新增时不传, 更新时传")
private Long id;
```

Keep the remaining fields unchanged in this task.

- [ ] **Step 9: Update `ExampleSentenceVO` response types**

Modify `grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExampleSentenceVO.java` imports and fields:

```java
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class ExampleSentenceVO implements Serializable {

    @ApiModelProperty(value = "例句ID")
    private Long id;

    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "例句外文翻译")
    private List<TextTranslationVO> translations;

    @ApiModelProperty(value = "例句图片ID")
    private Long imageId;

    @ApiModelProperty(value = "排序权重")
    private Integer order;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

Also replace the existing `TextTranslationRequest` import with `TextTranslationVO`.

- [ ] **Step 10: Run example sentence service tests**

Run:

```bash
mvn -pl grid-system -Dtest=ExampleSentenceServiceImplTest test
```

Expected: all tests in `ExampleSentenceServiceImplTest` pass.

- [ ] **Step 11: Run wrapper test again and verify it still fails only on character mapping work**

Run:

```bash
mvn -pl grid-system -Dtest=CharCharacterWrapperTest test
```

Expected: compilation still fails for missing character DTO/Wrapper fields, not for missing `ExampleSentenceDto` or `ExampleSentenceRequest.id`.

- [ ] **Step 12: Commit common example sentence foundation**

```bash
git add \
  grid-system/src/main/java/com/naon/grid/backend/domain/common/ExampleSentence.java \
  grid-system/src/main/java/com/naon/grid/backend/repo/common/ExampleSentenceRepository.java \
  grid-system/src/main/java/com/naon/grid/backend/service/common/dto/ExampleSentenceDto.java \
  grid-system/src/main/java/com/naon/grid/backend/service/common/ExampleSentenceService.java \
  grid-system/src/main/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImpl.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/request/ExampleSentenceRequest.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExampleSentenceVO.java \
  grid-system/src/test/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImplTest.java
git commit -m "feat: add reusable example sentence service"
```

---

### Task 3: Align character entities and repositories with the new SQL schema

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java`
- Rename/Create: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharComparison.java`
- Rename/Create: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharComparisonRepository.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharWordRepository.java`
- Modify later references in: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Replace `CharCharacter` field mappings**

Modify `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java` fields so the class contains these business mappings:

```java
@Id
@Column(name = "id")
@GeneratedValue(strategy = GenerationType.IDENTITY)
@ApiModelProperty(value = "汉字唯一ID", hidden = true)
private Integer id;

@NotBlank
@Column(name = "`character`", nullable = false, length = 16)
private String character;

@Column(name = "hsk_level", length = 20)
@ApiModelProperty(value = "HSK等级")
private String level;

@Column(name = "pinyin", length = 32)
private String pinyin;

@Column(name = "traditional", length = 16)
private String traditional;

@Column(name = "audio_id")
private Long audioId;

@Column(name = "radical", length = 16)
private String radical;

@Column(name = "radical_id")
private Long radicalId;

@Column(name = "component_combination", length = 64)
private String componentCombination;

@Column(name = "char_desc", length = 1024)
private String charDesc;

@Column(name = "char_desc_translations", columnDefinition = "text")
private String descTranslations;

@Column(name = "stroke", length = 4096)
private String stroke;

@Column(name = "status")
@ApiModelProperty(value = "状态: 1=可用, 0=不可用")
private Integer status = StatusEnum.ENABLED.getCode();

@Column(name = "publish_status", length = 20)
@ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

@Column(name = "edit_status", length = 20)
@ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
private String editStatus = EditStatusEnum.DRAFT.getCode();

@Column(name = "draft_content", columnDefinition = "text")
@ApiModelProperty(value = "草稿内容JSON（包含主表和子表）")
private String draftContent;
```

Remove the old `sequenceNo` mapping because `sql/biz_character.sql` no longer defines `sequence_no`.

- [ ] **Step 2: Replace `CharWord` field mappings**

Modify `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java` so the class no longer has inline example fields and uses SQL names:

```java
@Id
@Column(name = "id")
@GeneratedValue(strategy = GenerationType.IDENTITY)
@ApiModelProperty(value = "组词唯一ID", hidden = true)
private Integer id;

@NotNull
@Column(name = "char_id", nullable = false)
private Integer charId;

@NotBlank
@Column(name = "word_item", nullable = false, length = 50)
private String wordItem;

@Column(name = "hsk_level", length = 20)
@ApiModelProperty(value = "HSK等级")
private String level;

@Column(name = "pinyin", length = 100)
private String pinyin;

@Column(name = "part_of_speech", length = 50)
private String partOfSpeech;

@Column(name = "word_item_translations", columnDefinition = "text")
private String wordItemTranslations;

@NotNull
@Column(name = "`order`", nullable = false)
@ApiModelProperty(value = "组词排序权重（值大的排前面）")
private Integer wordOrder = 0;
```

Keep existing `createTime`, `updateTime`, and `status` fields.

- [ ] **Step 3: Rename `CharDiscrimination` to `CharComparison`**

Use git rename:

```bash
git mv grid-system/src/main/java/com/naon/grid/backend/domain/character/CharDiscrimination.java grid-system/src/main/java/com/naon/grid/backend/domain/character/CharComparison.java
```

Replace the file content with:

```java
package com.naon.grid.backend.domain.character;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "char_comparison")
public class CharComparison implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "辨析记录ID", hidden = true)
    private Integer id;

    @NotNull
    @Column(name = "char_id", nullable = false)
    private Integer charId;

    @NotBlank
    @Column(name = "comparison_char", nullable = false, length = 10)
    private String comparisonChar;

    @Column(name = "comparison_pinyin", length = 100)
    private String comparisonPinyin;

    @Column(name = "comparison_char_translations", columnDefinition = "text")
    private String comparisonCharTranslations;

    @Column(name = "comparison_desc_translations", columnDefinition = "text")
    private String comparisonDescTranslations;

    @NotNull
    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer comparisonOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 4: Rename comparison repository**

Use git rename:

```bash
git mv grid-system/src/main/java/com/naon/grid/backend/repo/character/CharDiscriminationRepository.java grid-system/src/main/java/com/naon/grid/backend/repo/character/CharComparisonRepository.java
```

Replace the file content with:

```java
package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharComparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharComparisonRepository extends JpaRepository<CharComparison, Integer> {

    List<CharComparison> findByCharId(Integer charId);

    List<CharComparison> findByCharIdAndStatus(Integer charId, Integer status);
}
```

- [ ] **Step 5: Keep `CharWordRepository` focused**

Ensure `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharWordRepository.java` remains:

```java
package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharWordRepository extends JpaRepository<CharWord, Integer> {

    List<CharWord> findByCharId(Integer charId);

    List<CharWord> findByCharIdAndStatus(Integer charId, Integer status);
}
```

- [ ] **Step 6: Run compile and capture expected reference failures**

Run:

```bash
mvn -pl grid-system -DskipTests compile
```

Expected: compilation fails in character service/DTO imports that still refer to `CharDiscrimination` names and old fields. Those failures are resolved in Tasks 4 and 6.

- [ ] **Step 7: Commit entity and repository alignment**

```bash
git add \
  grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java \
  grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java \
  grid-system/src/main/java/com/naon/grid/backend/domain/character/CharComparison.java \
  grid-system/src/main/java/com/naon/grid/backend/repo/character/CharComparisonRepository.java \
  grid-system/src/main/java/com/naon/grid/backend/repo/character/CharWordRepository.java
git add -u grid-system/src/main/java/com/naon/grid/backend/domain/character grid-system/src/main/java/com/naon/grid/backend/repo/character
git commit -m "feat: align character entities with SQL schema"
```

---

### Task 4: Update character DTOs for new fields and singular example sentences

**Files:**
- Rename/Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharComparisonDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDraftDto.java`

- [ ] **Step 1: Rename comparison DTO**

Run:

```bash
git mv grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharComparisonDto.java
```

Replace the content with:

```java
package com.naon.grid.backend.service.character.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CharComparisonDto implements Serializable {

    @ApiModelProperty(value = "辨析ID")
    private Integer id;

    @ApiModelProperty(value = "汉字ID")
    private Integer charId;

    @ApiModelProperty(value = "辨析汉字")
    private String comparisonChar;

    @ApiModelProperty(value = "辨析拼音")
    private String comparisonPinyin;

    @ApiModelProperty(value = "辨析汉字翻译")
    private List<TextTranslation> comparisonCharTranslations;

    @ApiModelProperty(value = "对比辨析说明外文翻译")
    private List<TextTranslation> comparisonDescTranslations;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer order;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 2: Update `CharCharacterDto`**

Modify `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java` so business fields are:

```java
@ApiModelProperty(value = "汉字唯一ID")
private Integer id;

@ApiModelProperty(value = "汉字")
private String character;

@ApiModelProperty(value = "HSK等级")
private String level;

@ApiModelProperty(value = "拼音")
private String pinyin;

@ApiModelProperty(value = "读音音频资源ID")
private Long audioId;

@ApiModelProperty(value = "繁体字")
private String traditional;

@ApiModelProperty(value = "部首ID")
private Long radicalId;

@ApiModelProperty(value = "部首")
private String radical;

@ApiModelProperty(value = "部件组合")
private String componentCombination;

@ApiModelProperty(value = "笔画")
private String stroke;

@ApiModelProperty(value = "汉字说明")
private String charDesc;

@ApiModelProperty(value = "说明翻译")
private List<TextTranslation> descTranslations;

@ApiModelProperty(value = "辨析列表")
private List<CharComparisonDto> comparisons;

@ApiModelProperty(value = "组词列表")
private List<CharWordDto> words;

@ApiModelProperty(value = "状态: 1=可用, 0=不可用")
private Integer status;

@ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
private String publishStatus;

@ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
private String editStatus;

@ApiModelProperty(value = "草稿内容JSON")
private String draftContent;
```

Remove `sequenceNo` from the DTO because the new SQL does not define `sequence_no`.

- [ ] **Step 3: Update `CharWordDto`**

Modify `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java` so it imports `ExampleSentenceDto` and uses these fields:

```java
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
```

Business fields:

```java
@ApiModelProperty(value = "组词唯一ID")
private Integer id;

@ApiModelProperty(value = "汉字ID")
private Integer charId;

@ApiModelProperty(value = "组词")
private String wordItem;

@ApiModelProperty(value = "HSK等级")
private String level;

@ApiModelProperty(value = "拼音")
private String pinyin;

@ApiModelProperty(value = "词性")
private String partOfSpeech;

@ApiModelProperty(value = "组词翻译")
private List<TextTranslation> wordItemTranslations;

@ApiModelProperty(value = "组词例句")
private ExampleSentenceDto wordItemSentence;

@ApiModelProperty(value = "创建时间")
private Timestamp createTime;

@ApiModelProperty(value = "更新时间")
private Timestamp updateTime;

@ApiModelProperty(value = "组词排序权重（值大的排前面）")
private Integer wordOrder;

@ApiModelProperty(value = "状态: 1=可用, 0=不可用")
private Integer status;
```

Remove old fields: `exampleSentence`, `examplePinyin`, `exampleTranslations`, `exampleImage`.

- [ ] **Step 4: Update `CharCharacterDraftDto`**

Replace `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDraftDto.java` content with:

```java
package com.naon.grid.backend.service.character.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CharCharacterDraftDto implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "HSK等级")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首ID")
    private Long radicalId;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "部件组合")
    private String componentCombination;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "说明翻译")
    private List<TextTranslation> descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharComparisonDto> comparisons;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordDto> words;
}
```

- [ ] **Step 5: Run compile and verify remaining failures are service/wrapper references**

Run:

```bash
mvn -pl grid-system -DskipTests compile
```

Expected: failures remain in `CharCharacterWrapper` and `CharCharacterServiceImpl`, because they still refer to old DTO names or old fields.

- [ ] **Step 6: Commit DTO alignment**

```bash
git add \
  grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharComparisonDto.java \
  grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java \
  grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDraftDto.java \
  grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java
git add -u grid-system/src/main/java/com/naon/grid/backend/service/character/dto
git commit -m "feat: update character DTOs for SQL alignment"
```

---

### Task 5: Update request, VO, and wrapper mapping

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java`
- Test: `grid-system/src/test/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapperTest.java`

- [ ] **Step 1: Fix `CharCharacterCreateRequest.CharWordRequest.id`**

In `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`, change the `id` field block to:

```java
@ApiModelProperty(value = "组词ID, 新增时不传 更新时传")
private Integer id;
```

Ensure the field has no `@NotBlank` annotation.

Also ensure the singular sentence field is:

```java
@ApiModelProperty(value = "组词例句")
private ExampleSentenceRequest sentenceContent;
```

- [ ] **Step 2: Update `CharCharacterVO` new field types**

In `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`, set these main fields:

```java
@ApiModelProperty(value = "HSK等级")
private String level;

@ApiModelProperty(value = "读音音频ID")
private Long audioId;

@ApiModelProperty(value = "部首ID")
private Long radicalId;

@ApiModelProperty(value = "部件组合")
private String componentCombination;
```

Keep nested `CharWordVO` singular field:

```java
@ApiModelProperty(value = "组词例句")
private ExampleSentenceVO wordItemSentence;
```

- [ ] **Step 3: Update `CharCharacterBaseVO` fields**

In `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`, remove `sequenceNo` and add these list-visible fields if missing:

```java
@ApiModelProperty(value = "部首ID")
private Long radicalId;

@ApiModelProperty(value = "部件组合")
private String componentCombination;
```

Keep `descTranslations` as the list response name already used by this VO.

- [ ] **Step 4: Replace wrapper imports**

In `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java`, imports must include:

```java
import com.naon.grid.backend.rest.request.ExampleSentenceRequest;
import com.naon.grid.backend.rest.vo.ExampleSentenceVO;
import com.naon.grid.backend.service.character.dto.CharComparisonDto;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
```

Remove imports for `CharDiscriminationDto`.

- [ ] **Step 5: Update `toDto` main field mapping**

In `CharCharacterWrapper.toDto`, use this body:

```java
public static CharCharacterDto toDto(CharCharacterCreateRequest request) {
    CharCharacterDto dto = new CharCharacterDto();
    dto.setCharacter(request.getCharacter());
    dto.setLevel(request.getHskLevel());
    dto.setPinyin(request.getPinyin());
    dto.setAudioId(request.getAudioId());
    dto.setTraditional(request.getTraditional());
    dto.setRadicalId(request.getRadicalId());
    dto.setRadical(request.getRadical());
    dto.setComponentCombination(request.getComponentCombination());
    dto.setStroke(request.getStroke());
    dto.setCharDesc(request.getCharDesc());
    dto.setDescTranslations(toTextTranslationList(request.getCharDescTranslations()));
    dto.setComparisons(toComparisonDtoList(request.getComparisons()));
    dto.setWords(toWordDtoList(request.getWords()));
    return dto;
}
```

- [ ] **Step 6: Replace comparison mapping methods**

Replace old discrimination mapping methods with:

```java
private static List<CharComparisonDto> toComparisonDtoList(List<CharCharacterCreateRequest.CharComparisonRequest> requests) {
    if (requests == null) {
        return Collections.emptyList();
    }
    return requests.stream().map(CharCharacterWrapper::toComparisonDto).collect(Collectors.toList());
}

private static CharComparisonDto toComparisonDto(CharCharacterCreateRequest.CharComparisonRequest request) {
    CharComparisonDto dto = new CharComparisonDto();
    dto.setId(request.getId());
    dto.setComparisonChar(request.getComparisonChar());
    dto.setComparisonPinyin(request.getComparisonPinyin());
    dto.setComparisonCharTranslations(toTextTranslationList(request.getComparisonCharTranslations()));
    dto.setComparisonDescTranslations(toTextTranslationList(request.getComparisonDescTranslations()));
    dto.setOrder(request.getOrder());
    return dto;
}
```

- [ ] **Step 7: Update word request mapping**

Replace `toWordDto` with:

```java
private static CharWordDto toWordDto(CharCharacterCreateRequest.CharWordRequest request) {
    CharWordDto dto = new CharWordDto();
    dto.setId(request.getId());
    dto.setWordItem(request.getWordItem());
    dto.setLevel(request.getHskLevel());
    dto.setPinyin(request.getPinyin());
    dto.setPartOfSpeech(request.getPartOfSpeech());
    dto.setWordItemTranslations(toTextTranslationList(request.getWordItemTranslations()));
    dto.setWordItemSentence(toExampleSentenceDto(request.getSentenceContent()));
    dto.setWordOrder(request.getOrder() != null ? request.getOrder() : 0);
    return dto;
}
```

Add example sentence request mapping:

```java
private static ExampleSentenceDto toExampleSentenceDto(ExampleSentenceRequest request) {
    if (request == null) {
        return null;
    }
    ExampleSentenceDto dto = new ExampleSentenceDto();
    dto.setId(request.getId());
    dto.setSentence(request.getSentence());
    dto.setPinyin(request.getPinyin());
    dto.setAudioId(request.getAudioId());
    dto.setTranslations(toTextTranslationList(request.getTranslations()));
    dto.setImageId(request.getImageId());
    dto.setOrder(request.getOrder());
    return dto;
}
```

- [ ] **Step 8: Update base VO mapping**

Replace `toBaseVO` body with:

```java
private static CharCharacterBaseVO toBaseVO(CharCharacterDto dto) {
    CharCharacterBaseVO vo = new CharCharacterBaseVO();
    vo.setId(dto.getId());
    vo.setCharacter(dto.getCharacter());
    vo.setLevel(dto.getLevel());
    vo.setPinyin(dto.getPinyin());
    vo.setAudioId(dto.getAudioId());
    vo.setTraditional(dto.getTraditional());
    vo.setRadicalId(dto.getRadicalId());
    vo.setRadical(dto.getRadical());
    vo.setComponentCombination(dto.getComponentCombination());
    vo.setStroke(dto.getStroke());
    vo.setCharDesc(dto.getCharDesc());
    vo.setDescTranslations(toTextTranslationVOList(dto.getDescTranslations()));
    vo.setPublishStatus(dto.getPublishStatus());
    vo.setEditStatus(dto.getEditStatus());
    vo.setCreateBy(dto.getCreateBy());
    vo.setUpdateBy(dto.getUpdateBy());
    vo.setCreateTime(dto.getCreateTime());
    vo.setUpdateTime(dto.getUpdateTime());
    return vo;
}
```

- [ ] **Step 9: Update detail VO mapping**

In `toVO`, add the new fields:

```java
vo.setRadicalId(dto.getRadicalId());
vo.setComponentCombination(dto.getComponentCombination());
```

Use:

```java
vo.setComparisons(toComparisonVOList(dto.getComparisons()));
```

- [ ] **Step 10: Replace comparison VO methods**

Replace old discrimination VO methods with:

```java
private static List<CharCharacterVO.CharComparisonVO> toComparisonVOList(List<CharComparisonDto> resources) {
    if (resources == null) {
        return Collections.emptyList();
    }
    return resources.stream().map(CharCharacterWrapper::toComparisonVO).collect(Collectors.toList());
}

private static CharCharacterVO.CharComparisonVO toComparisonVO(CharComparisonDto dto) {
    CharCharacterVO.CharComparisonVO vo = new CharCharacterVO.CharComparisonVO();
    vo.setId(dto.getId());
    vo.setComparisonChar(dto.getComparisonChar());
    vo.setComparisonPinyin(dto.getComparisonPinyin());
    vo.setComparisonCharTranslations(toTextTranslationVOList(dto.getComparisonCharTranslations()));
    vo.setComparisonDescTranslations(toTextTranslationVOList(dto.getComparisonDescTranslations()));
    vo.setOrder(dto.getOrder());
    vo.setCreateTime(dto.getCreateTime());
    vo.setUpdateTime(dto.getUpdateTime());
    return vo;
}
```

- [ ] **Step 11: Update word VO mapping**

Replace the sentence mapping in `toWordVO` with:

```java
vo.setWordItemSentence(toExampleSentenceVO(dto.getWordItemSentence()));
```

Add:

```java
private static ExampleSentenceVO toExampleSentenceVO(ExampleSentenceDto dto) {
    if (dto == null) {
        return null;
    }
    ExampleSentenceVO vo = new ExampleSentenceVO();
    vo.setId(dto.getId());
    vo.setSentence(dto.getSentence());
    vo.setPinyin(dto.getPinyin());
    vo.setAudioId(dto.getAudioId());
    vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
    vo.setImageId(dto.getImageId());
    vo.setOrder(dto.getOrder());
    vo.setCreateTime(dto.getCreateTime());
    vo.setUpdateTime(dto.getUpdateTime());
    return vo;
}
```

Remove calls to `setExampleSentence`, `setExamplePinyin`, `setExampleTranslations`, and `setExampleImage`.

- [ ] **Step 12: Run wrapper test**

Run:

```bash
mvn -pl grid-system -Dtest=CharCharacterWrapperTest test
```

Expected: `CharCharacterWrapperTest` passes.

- [ ] **Step 13: Commit request/VO/wrapper alignment**

```bash
git add \
  grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java \
  grid-system/src/test/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapperTest.java
git commit -m "feat: map character API models to new schema"
```

---

### Task 6: Update character service publish/query logic for comparisons, words, and example sentences

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`
- Inspect/Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java` to confirm translation helpers still map renamed text columns
- Test: existing tests from Tasks 1 and 2

- [ ] **Step 1: Replace service imports and constructor fields**

In `CharCharacterServiceImpl`, replace comparison and example imports with:

```java
import com.naon.grid.backend.domain.character.CharComparison;
import com.naon.grid.backend.domain.character.CharWord;
import com.naon.grid.backend.repo.character.CharComparisonRepository;
import com.naon.grid.backend.repo.character.CharWordRepository;
import com.naon.grid.backend.service.character.dto.CharComparisonDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.enums.SentenceBizTypeEnum;
```

Replace injected fields with:

```java
private final CharCharacterRepository charCharacterRepository;
private final CharComparisonRepository charComparisonRepository;
private final CharWordRepository charWordRepository;
private final CharCharacterMapper charCharacterMapper;
private final ExampleSentenceService exampleSentenceService;
```

- [ ] **Step 2: Update draft overlay for new main fields**

In `applyDraftOverlay`, remove `sequenceNo` handling and add:

```java
if (draft.getLevel() != null)                dto.setLevel(draft.getLevel());
if (draft.getPinyin() != null)               dto.setPinyin(draft.getPinyin());
if (draft.getAudioId() != null)              dto.setAudioId(draft.getAudioId());
if (draft.getTraditional() != null)          dto.setTraditional(draft.getTraditional());
if (draft.getRadicalId() != null)            dto.setRadicalId(draft.getRadicalId());
if (draft.getRadical() != null)              dto.setRadical(draft.getRadical());
if (draft.getComponentCombination() != null) dto.setComponentCombination(draft.getComponentCombination());
if (draft.getStroke() != null)               dto.setStroke(draft.getStroke());
if (draft.getCharDesc() != null)             dto.setCharDesc(draft.getCharDesc());
if (draft.getDescTranslations() != null)     dto.setDescTranslations(draft.getDescTranslations());
```

- [ ] **Step 3: Update published detail query**

In both `findById` published branch and `findPublishedById`, replace child population with:

```java
CharCharacterDto charCharacterDto = charCharacterMapper.toDto(charCharacter);
charCharacterDto.setComparisons(sortComparisonsDesc(convertToComparisonDtos(
        charComparisonRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
charCharacterDto.setWords(sortWordsDesc(withWordSentences(convertToWordDtos(
        charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())))));
return charCharacterDto;
```

- [ ] **Step 4: Replace comparison conversion methods**

Replace old discrimination conversion methods with:

```java
private List<CharComparisonDto> convertToComparisonDtos(List<CharComparison> comparisons) {
    List<CharComparisonDto> comparisonDtos = new ArrayList<>();
    for (CharComparison comparison : comparisons) {
        comparisonDtos.add(convertToComparisonDto(comparison));
    }
    return comparisonDtos;
}

private CharComparisonDto convertToComparisonDto(CharComparison comparison) {
    CharComparisonDto dto = new CharComparisonDto();
    dto.setId(comparison.getId());
    dto.setCharId(comparison.getCharId());
    dto.setComparisonChar(comparison.getComparisonChar());
    dto.setComparisonPinyin(comparison.getComparisonPinyin());
    dto.setComparisonCharTranslations(JsonUtils.parseTranslationList(comparison.getComparisonCharTranslations()));
    dto.setComparisonDescTranslations(JsonUtils.parseTranslationList(comparison.getComparisonDescTranslations()));
    dto.setOrder(comparison.getComparisonOrder());
    dto.setCreateTime(comparison.getCreateTime());
    dto.setUpdateTime(comparison.getUpdateTime());
    dto.setStatus(comparison.getStatus());
    return dto;
}
```

- [ ] **Step 5: Update word conversion to exclude old inline examples**

Replace `convertToWordDto` with:

```java
private CharWordDto convertToWordDto(CharWord word) {
    CharWordDto dto = new CharWordDto();
    dto.setId(word.getId());
    dto.setCharId(word.getCharId());
    dto.setWordItem(word.getWordItem());
    dto.setLevel(word.getLevel());
    dto.setPinyin(word.getPinyin());
    dto.setPartOfSpeech(word.getPartOfSpeech());
    dto.setWordItemTranslations(JsonUtils.parseTranslationList(word.getWordItemTranslations()));
    dto.setWordOrder(word.getWordOrder());
    dto.setCreateTime(word.getCreateTime());
    dto.setUpdateTime(word.getUpdateTime());
    dto.setStatus(word.getStatus());
    return dto;
}
```

- [ ] **Step 6: Add published word sentence hydration**

Add this helper:

```java
private List<CharWordDto> withWordSentences(List<CharWordDto> words) {
    if (words == null || words.isEmpty()) {
        return words;
    }
    List<Long> wordIds = words.stream()
            .filter(word -> word.getId() != null)
            .map(word -> word.getId().longValue())
            .collect(java.util.stream.Collectors.toList());
    Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByBizIds(
            SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), wordIds);
    for (CharWordDto word : words) {
        if (word.getId() != null) {
            word.setWordItemSentence(sentenceMap.get(word.getId().longValue()));
        }
    }
    return words;
}
```

Ensure `java.util.Map` and stream collectors are imported or already available.

- [ ] **Step 7: Replace comparison sync method**

Replace `syncDiscriminations` with `syncComparisons`:

```java
private void syncComparisons(Integer charId, List<CharComparisonDto> submittedDtos) {
    List<CharComparisonDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
    List<CharComparison> existing = charComparisonRepository.findByCharIdAndStatus(charId, StatusEnum.ENABLED.getCode());
    Map<Integer, CharComparison> existingMap = new HashMap<>();
    for (CharComparison comparison : existing) {
        existingMap.put(comparison.getId(), comparison);
    }

    Set<Integer> submittedIds = new HashSet<>();
    List<CharComparison> toSave = new ArrayList<>();

    for (CharComparisonDto dto : submitted) {
        if (dto.getId() == null) {
            toSave.add(convertToComparisonEntity(dto, charId));
            continue;
        }
        if (!submittedIds.add(dto.getId())) {
            throw new BadRequestException("辨析ID重复: " + dto.getId());
        }
        CharComparison comparison = existingMap.get(dto.getId());
        if (comparison == null) {
            throw new BadRequestException("辨析ID不属于当前汉字: " + dto.getId());
        }
        updateComparison(comparison, dto);
        toSave.add(comparison);
    }

    for (CharComparison comparison : existing) {
        if (!submittedIds.contains(comparison.getId())) {
            comparison.setStatus(StatusEnum.DISABLED.getCode());
            charComparisonRepository.save(comparison);
        }
    }
    charComparisonRepository.saveAll(toSave);
}
```

- [ ] **Step 8: Add comparison entity update/creation helpers**

Replace old discrimination helpers with:

```java
private void updateComparison(CharComparison entity, CharComparisonDto dto) {
    entity.setComparisonChar(dto.getComparisonChar());
    entity.setComparisonPinyin(dto.getComparisonPinyin());
    entity.setComparisonCharTranslations(JsonUtils.toTranslationJson(dto.getComparisonCharTranslations()));
    entity.setComparisonDescTranslations(JsonUtils.toTranslationJson(dto.getComparisonDescTranslations()));
    entity.setComparisonOrder(dto.getOrder() != null ? dto.getOrder() : 0);
}

private CharComparison convertToComparisonEntity(CharComparisonDto dto, Integer charId) {
    CharComparison entity = new CharComparison();
    entity.setCharId(charId);
    entity.setComparisonChar(dto.getComparisonChar());
    entity.setComparisonPinyin(dto.getComparisonPinyin());
    entity.setComparisonCharTranslations(JsonUtils.toTranslationJson(dto.getComparisonCharTranslations()));
    entity.setComparisonDescTranslations(JsonUtils.toTranslationJson(dto.getComparisonDescTranslations()));
    entity.setComparisonOrder(dto.getOrder() != null ? dto.getOrder() : 0);
    entity.setStatus(StatusEnum.ENABLED.getCode());
    return entity;
}
```

- [ ] **Step 9: Update word sync to return saved/active words and disable removed sentences**

Replace `syncWords` signature and body with:

```java
private List<CharWord> syncWords(Integer charId, List<CharWordDto> submittedDtos) {
    List<CharWordDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
    List<CharWord> existing = charWordRepository.findByCharIdAndStatus(charId, StatusEnum.ENABLED.getCode());
    Map<Integer, CharWord> existingMap = new HashMap<>();
    for (CharWord word : existing) {
        existingMap.put(word.getId(), word);
    }

    Set<Integer> submittedIds = new HashSet<>();
    List<CharWord> toSave = new ArrayList<>();
    List<Long> removedWordIds = new ArrayList<>();

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

    for (CharWord word : existing) {
        if (!submittedIds.contains(word.getId())) {
            word.setStatus(StatusEnum.DISABLED.getCode());
            charWordRepository.save(word);
            removedWordIds.add(word.getId().longValue());
        }
    }
    if (!removedWordIds.isEmpty()) {
        exampleSentenceService.disableByBizIds(SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(), removedWordIds);
    }
    return charWordRepository.saveAll(toSave);
}
```

- [ ] **Step 10: Update word helpers to exclude inline examples**

Replace `updateWord` and `convertToWordEntity` with:

```java
private void updateWord(CharWord entity, CharWordDto dto) {
    entity.setWordItem(dto.getWordItem());
    entity.setLevel(dto.getLevel());
    entity.setPinyin(dto.getPinyin());
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setWordItemTranslations(JsonUtils.toTranslationJson(dto.getWordItemTranslations()));
    entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);
}

private CharWord convertToWordEntity(CharWordDto dto, Integer charId) {
    CharWord entity = new CharWord();
    entity.setCharId(charId);
    entity.setWordItem(dto.getWordItem());
    entity.setLevel(dto.getLevel());
    entity.setPinyin(dto.getPinyin());
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setWordItemTranslations(JsonUtils.toTranslationJson(dto.getWordItemTranslations()));
    entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);
    entity.setStatus(StatusEnum.ENABLED.getCode());
    return entity;
}
```

- [ ] **Step 11: Add word sentence sync helper**

Add:

```java
private void syncWordSentences(List<CharWord> savedWords, List<CharWordDto> submittedDtos) {
    List<CharWordDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
    if (savedWords == null || savedWords.isEmpty()) {
        return;
    }
    for (int i = 0; i < savedWords.size(); i++) {
        CharWord savedWord = savedWords.get(i);
        CharWordDto submittedDto = submitted.get(i);
        exampleSentenceService.syncOne(
                SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode(),
                savedWord.getId().longValue(),
                submittedDto.getWordItemSentence());
    }
}
```

This relies on `charWordRepository.saveAll(toSave)` preserving iteration order for the submitted list, which Spring Data JPA does for `saveAll(Iterable)` by returning saved entities in input iteration order.

- [ ] **Step 12: Update sorting helpers**

Replace comparison sorting helper with:

```java
private List<CharComparisonDto> sortComparisonsDesc(List<CharComparisonDto> list) {
    if (list == null || list.isEmpty()) {
        return list;
    }
    list.sort(Comparator.comparing(
            CharComparisonDto::getOrder,
            Comparator.nullsLast(Comparator.reverseOrder())
    ));
    return list;
}
```

Keep word sorting but ensure it uses `CharWordDto::getWordOrder`.

- [ ] **Step 13: Update `publishDraft` main table mapping and child sync**

In `publishDraft`, replace main field updates with:

```java
charCharacter.setLevel(draftDto.getLevel());
charCharacter.setPinyin(draftDto.getPinyin());
charCharacter.setAudioId(draftDto.getAudioId());
charCharacter.setTraditional(draftDto.getTraditional());
charCharacter.setRadicalId(draftDto.getRadicalId());
charCharacter.setRadical(draftDto.getRadical());
charCharacter.setComponentCombination(draftDto.getComponentCombination());
charCharacter.setStroke(draftDto.getStroke());
charCharacter.setCharDesc(draftDto.getCharDesc());
charCharacter.setDescTranslations(JsonUtils.toTranslationJson(draftDto.getDescTranslations()));
```

Replace child sync calls with:

```java
syncComparisons(id, draftDto.getComparisons());
List<CharWord> savedWords = syncWords(id, draftDto.getWords());
syncWordSentences(savedWords, draftDto.getWords());
```

- [ ] **Step 14: Remove old child methods**

Delete the unused old methods if they remain:

- `saveChildren`
- `deleteChildren`
- `syncDiscriminations`
- `convertToDiscriminationDtos`
- `convertToDiscriminationDto`
- `updateDiscrimination`
- `convertToDiscriminationEntity`

- [ ] **Step 15: Run focused tests**

Run:

```bash
mvn -pl grid-system -Dtest=ExampleSentenceServiceImplTest,CharCharacterWrapperTest test
```

Expected: both test classes pass.

- [ ] **Step 16: Run module compile**

Run:

```bash
mvn -pl grid-system -am -DskipTests compile
```

Expected: build succeeds.

- [ ] **Step 17: Confirm mapper helper compatibility**

Open `grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java` and ensure it still contains these helper methods:

```java
default String toTranslationJson(List<TextTranslation> list) {
    return JsonUtils.toTranslationJson(list);
}

default List<TextTranslation> toTranslationList(String json) {
    return JsonUtils.parseTranslationList(json);
}
```

No field-specific changes are needed because `CharCharacter.descTranslations` remains the Java property name while its entity column is changed to `char_desc_translations`.

- [ ] **Step 18: Commit service alignment**

```bash
git add \
  grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java \
  grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java
git commit -m "feat: sync character publish flow with example sentences"
```

---

### Task 7: Verify no vocabulary logic was changed

**Files:**
- Inspect only: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`
- Inspect only: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java`
- Inspect only: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java`

- [ ] **Step 1: Check git diff for vocabulary service files**

Run:

```bash
git diff -- grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java
```

Expected: no diff from this implementation. If there is a diff caused by this implementation, revert only those vocabulary changes before continuing.

- [ ] **Step 2: Check references to `ExampleSentenceService`**

Run:

```bash
rg "ExampleSentenceService" grid-system/src/main/java
```

Expected output includes only:

```text
grid-system/src/main/java/com/naon/grid/backend/service/common/ExampleSentenceService.java
grid-system/src/main/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImpl.java
grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java
```

- [ ] **Step 3: Commit guardrail verification if any accidental vocabulary change was reverted**

If Step 1 required a revert, commit that revert:

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java
git commit -m "chore: keep vocabulary flow unchanged"
```

If Step 1 had no diff, skip this commit.

---

### Task 8: Final verification and manual API checklist

**Files:**
- Read: `docs/汉字管理接口使用流程.md`
- Read: `sql/biz_character.sql`
- Read: `sql/biz_common.sql`

- [ ] **Step 1: Run all focused unit tests**

Run:

```bash
mvn -pl grid-system -Dtest=ExampleSentenceServiceImplTest,CharCharacterWrapperTest test
```

Expected: tests pass.

- [ ] **Step 2: Run compile for dependent modules**

Run:

```bash
mvn -pl grid-system -am -DskipTests compile
```

Expected: build succeeds.

- [ ] **Step 3: Confirm no old character inline example fields remain in production Java code**

Run:

```bash
rg "exampleSentence|examplePinyin|exampleTranslations|exampleImage" grid-system/src/main/java/com/naon/grid/backend
```

Expected: no matches in `domain/character`, `service/character`, or `rest/wrapper/CharCharacterWrapper.java`. Matches in old docs or unrelated worktrees are not relevant.

- [ ] **Step 4: Confirm no old discrimination names remain in production Java code**

Run:

```bash
rg "CharDiscrimination|Discrimination|discrimination" grid-system/src/main/java/com/naon/grid/backend
```

Expected: no matches in character production code. If a public API description still uses the Chinese word “辨析”, that is acceptable; Java class and method names should use `Comparison`.

- [ ] **Step 5: Manual API create payload**

Use this payload with `POST /api/character` in Swagger/Knife4j or an API client:

```json
{
  "character": "你",
  "hskLevel": "1",
  "pinyin": "nǐ",
  "traditional": "你",
  "audioId": 12,
  "radicalId": 3,
  "radical": "亻",
  "componentCombination": "亻 + 尔",
  "charDesc": "第二人称代词",
  "charDescTranslations": [
    { "language": "en", "translation": "you" }
  ],
  "stroke": "stroke-json",
  "comparisons": [
    {
      "comparisonChar": "您",
      "comparisonPinyin": "nín",
      "comparisonCharTranslations": [
        { "language": "en", "translation": "you, polite" }
      ],
      "comparisonDescTranslations": [
        { "language": "en", "translation": "您 is more polite than 你." }
      ],
      "order": 10
    }
  ],
  "words": [
    {
      "wordItem": "你好",
      "hskLevel": "1",
      "pinyin": "nǐ hǎo",
      "partOfSpeech": "interj.",
      "wordItemTranslations": [
        { "language": "en", "translation": "hello" }
      ],
      "sentenceContent": {
        "sentence": "你好，我叫小明。",
        "pinyin": "nǐ hǎo, wǒ jiào xiǎo míng.",
        "audioId": 21,
        "imageId": 34,
        "translations": [
          { "language": "en", "translation": "Hello, my name is Xiaoming." }
        ],
        "order": 5
      },
      "order": 7
    }
  ]
}
```

Expected: response `201 Created` with an `id`.

- [ ] **Step 6: Manual draft detail check**

Call `GET /api/character/{id}` for the ID from Step 5.

Expected response includes:

```json
{
  "character": "你",
  "level": "1",
  "radicalId": 3,
  "componentCombination": "亻 + 尔",
  "words": [
    {
      "wordItem": "你好",
      "wordItemSentence": {
        "sentence": "你好，我叫小明。"
      }
    }
  ],
  "editStatus": "draft"
}
```

- [ ] **Step 7: Manual publish flow**

Run these requests:

```text
PUT /api/character/{id}/review
PUT /api/character/{id}/publish
GET /api/character/{id}
```

Expected after publish:

- `publishStatus = published`
- `editStatus = published`
- `words[0].wordItemSentence.id` is not null
- database table `example_sentence` has one enabled row with `biz_type = CHAR_WORD_SENTENCE` and `biz_id = char_word.id`

- [ ] **Step 8: Manual update existing sentence check**

Use `PUT /api/character/{id}` with the same payload, now including existing `word.id` and `sentenceContent.id`, and change only the sentence text:

```json
{
  "character": "你",
  "hskLevel": "1",
  "pinyin": "nǐ",
  "traditional": "你",
  "audioId": 12,
  "radicalId": 3,
  "radical": "亻",
  "componentCombination": "亻 + 尔",
  "charDesc": "第二人称代词",
  "charDescTranslations": [
    { "language": "en", "translation": "you" }
  ],
  "stroke": "stroke-json",
  "comparisons": [],
  "words": [
    {
      "id": 9,
      "wordItem": "你好",
      "hskLevel": "1",
      "pinyin": "nǐ hǎo",
      "partOfSpeech": "interj.",
      "wordItemTranslations": [
        { "language": "en", "translation": "hello" }
      ],
      "sentenceContent": {
        "id": 88,
        "sentence": "你好，很高兴认识你。",
        "pinyin": "nǐ hǎo, hěn gāo xìng rèn shi nǐ.",
        "audioId": 21,
        "imageId": 34,
        "translations": [
          { "language": "en", "translation": "Hello, nice to meet you." }
        ],
        "order": 5
      },
      "order": 7
    }
  ]
}
```

Use the real IDs returned by the previous `GET /api/character/{id}` instead of `9` and `88`.

Then run:

```text
PUT /api/character/{id}/review
PUT /api/character/{id}/publish
GET /api/character/{id}
```

Expected: the existing example sentence row is updated rather than duplicated; only one enabled `example_sentence` row remains for that `char_word.id`.

- [ ] **Step 9: Final git status**

Run:

```bash
git status --short
```

Expected: only intentionally uncommitted files remain. If every implementation task committed successfully, there should be no uncommitted production or test changes except pre-existing user changes outside this plan.
