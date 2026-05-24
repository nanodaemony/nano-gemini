# Character Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an anonymous CRUD backend for Chinese character teaching data, matching the existing vocabulary management style.

**Architecture:** Add a new `character` backend module beside the existing `vocabulary` module. The API exposes aggregate CRUD for `char_character` with nested `char_discrimination` and `char_word` data; updates replace child rows, matching `VocabWordServiceImpl` behavior.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Spring Data JPA, MapStruct, Lombok, Swagger annotations, Maven.

---

## File Structure

Create focused files that mirror the vocabulary module:

- `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java` — JPA entity for `char_character`.
- `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharDiscrimination.java` — JPA entity for `char_discrimination`.
- `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java` — JPA entity for `char_word`.
- `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharCharacterRepository.java` — main table repository with specification support.
- `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharDiscriminationRepository.java` — child repository with `findByCharId`.
- `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharWordRepository.java` — child repository with `findByCharId`.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java` — aggregate DTO.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java` — discrimination DTO.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java` — word DTO.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterQueryCriteria.java` — query criteria for blurry search.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java` — MapStruct mapper for main entity.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java` — service contract.
- `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java` — aggregate CRUD implementation.
- `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterQueryRequest.java` — list query request.
- `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java` — create/update aggregate request.
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java` — list response item.
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterCreateVO.java` — create response.
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java` — detail response with nested children.
- `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java` — anonymous REST controller.

## Task 1: Domain Entities and Repositories

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharDiscrimination.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharCharacterRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharDiscriminationRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/character/CharWordRepository.java`

- [ ] **Step 1: Run compile before adding files to capture current baseline**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: PASS if the current workspace is healthy. If it fails before changes, capture the existing error and continue only if the error is unrelated to character files.

- [ ] **Step 2: Create `CharCharacter.java`**

```java
package com.naon.grid.backend.domain.character;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "char_character")
public class CharCharacter implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "汉字唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sequence_no")
    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @NotBlank
    @Column(name = "character", nullable = false, length = 10)
    @ApiModelProperty(value = "汉字")
    private String character;

    @Column(name = "level", length = 20)
    @ApiModelProperty(value = "等级")
    private String level;

    @NotBlank
    @Column(name = "pinyin", nullable = false, length = 100)
    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @Column(name = "traditional", length = 10)
    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @Column(name = "radical", length = 10)
    @ApiModelProperty(value = "部首")
    private String radical;

    @Column(name = "stroke", length = 4096)
    @ApiModelProperty(value = "笔顺")
    private String stroke;

    @Column(name = "char_desc", length = 1024)
    @ApiModelProperty(value = "部件组合中文说明")
    private String charDesc;

    @Column(name = "desc_translations", columnDefinition = "text")
    @ApiModelProperty(value = "汉字说明的多语种翻译")
    private String descTranslations;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;
}
```

- [ ] **Step 3: Create `CharDiscrimination.java`**

```java
package com.naon.grid.backend.domain.character;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

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
@Table(name = "char_discrimination")
public class CharDiscrimination implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "辨析记录ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "char_id", nullable = false)
    @ApiModelProperty(value = "所属汉字ID")
    private Integer charId;

    @NotBlank
    @Column(name = "discrim_char", nullable = false, length = 10)
    @ApiModelProperty(value = "辨析汉字")
    private String discrimChar;

    @Column(name = "discrim_pinyin", length = 100)
    @ApiModelProperty(value = "辨析字拼音")
    private String discrimPinyin;

    @Column(name = "discrim_char_translations", columnDefinition = "text")
    @ApiModelProperty(value = "辨析字说明的多语种翻译")
    private String discrimCharTranslations;

    @Column(name = "comparison_translations", columnDefinition = "text")
    @ApiModelProperty(value = "对比辨析说明的多语种翻译")
    private String comparisonTranslations;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;
}
```

- [ ] **Step 4: Create `CharWord.java`**

```java
package com.naon.grid.backend.domain.character;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

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
@Table(name = "char_word")
public class CharWord implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "组词记录ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "char_id", nullable = false)
    @ApiModelProperty(value = "所属汉字ID")
    private Integer charId;

    @NotBlank
    @Column(name = "word_item", nullable = false, length = 50)
    @ApiModelProperty(value = "组词")
    private String wordItem;

    @Column(name = "level", length = 20)
    @ApiModelProperty(value = "组词HSK等级")
    private String level;

    @Column(name = "pinyin", length = 100)
    @ApiModelProperty(value = "组词拼音")
    private String pinyin;

    @Column(name = "part_of_speech", length = 50)
    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @Column(name = "word_item_translations", columnDefinition = "text")
    @ApiModelProperty(value = "组词说明的多语种翻译")
    private String wordItemTranslations;

    @Column(name = "example_sentence", columnDefinition = "text")
    @ApiModelProperty(value = "中文例句")
    private String exampleSentence;

    @Column(name = "example_pinyin", length = 500)
    @ApiModelProperty(value = "例句拼音")
    private String examplePinyin;

    @Column(name = "example_translations", columnDefinition = "text")
    @ApiModelProperty(value = "例句的多语种翻译")
    private String exampleTranslations;

    @Column(name = "example_image", length = 255)
    @ApiModelProperty(value = "例句图片路径")
    private String exampleImage;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;
}
```

- [ ] **Step 5: Create repositories**

`CharCharacterRepository.java`:

```java
package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CharCharacterRepository extends JpaRepository<CharCharacter, Integer>, JpaSpecificationExecutor<CharCharacter> {
}
```

`CharDiscriminationRepository.java`:

```java
package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharDiscrimination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharDiscriminationRepository extends JpaRepository<CharDiscrimination, Integer> {

    List<CharDiscrimination> findByCharId(Integer charId);
}
```

`CharWordRepository.java`:

```java
package com.naon.grid.backend.repo.character;

import com.naon.grid.backend.domain.character.CharWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharWordRepository extends JpaRepository<CharWord, Integer> {

    List<CharWord> findByCharId(Integer charId);
}
```

- [ ] **Step 6: Run compile to verify entities and repositories**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: PASS. MapStruct should not generate character mapper yet because it is not created in this task.

- [ ] **Step 7: Commit Task 1**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/character/CharCharacter.java \
  grid-system/src/main/java/com/naon/grid/backend/domain/character/CharDiscrimination.java \
  grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java \
  grid-system/src/main/java/com/naon/grid/backend/repo/character/CharCharacterRepository.java \
  grid-system/src/main/java/com/naon/grid/backend/repo/character/CharDiscriminationRepository.java \
  grid-system/src/main/java/com/naon/grid/backend/repo/character/CharWordRepository.java
git commit -m "新增汉字实体与仓储"
```

## Task 2: DTOs, Query Criteria, Mapper, and Service Contract

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterQueryCriteria.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java`

- [ ] **Step 1: Create `CharDiscriminationDto.java`**

```java
package com.naon.grid.backend.service.character.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class CharDiscriminationDto implements Serializable {

    @ApiModelProperty(value = "辨析记录ID")
    private Integer id;

    @ApiModelProperty(value = "所属汉字ID")
    private Integer charId;

    @ApiModelProperty(value = "辨析汉字")
    private String discrimChar;

    @ApiModelProperty(value = "辨析字拼音")
    private String discrimPinyin;

    @ApiModelProperty(value = "辨析字说明的多语种翻译")
    private String discrimCharTranslations;

    @ApiModelProperty(value = "对比辨析说明的多语种翻译")
    private String comparisonTranslations;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

- [ ] **Step 2: Create `CharWordDto.java`**

```java
package com.naon.grid.backend.service.character.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class CharWordDto implements Serializable {

    @ApiModelProperty(value = "组词记录ID")
    private Integer id;

    @ApiModelProperty(value = "所属汉字ID")
    private Integer charId;

    @ApiModelProperty(value = "组词")
    private String wordItem;

    @ApiModelProperty(value = "组词HSK等级")
    private String level;

    @ApiModelProperty(value = "组词拼音")
    private String pinyin;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "组词说明的多语种翻译")
    private String wordItemTranslations;

    @ApiModelProperty(value = "中文例句")
    private String exampleSentence;

    @ApiModelProperty(value = "例句拼音")
    private String examplePinyin;

    @ApiModelProperty(value = "例句的多语种翻译")
    private String exampleTranslations;

    @ApiModelProperty(value = "例句图片路径")
    private String exampleImage;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

- [ ] **Step 3: Create `CharCharacterDto.java`**

```java
package com.naon.grid.backend.service.character.dto;

import com.naon.grid.base.BaseDTO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CharCharacterDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔顺")
    private String stroke;

    @ApiModelProperty(value = "部件组合中文说明")
    private String charDesc;

    @ApiModelProperty(value = "汉字说明的多语种翻译")
    private String descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationDto> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordDto> words;
}
```

- [ ] **Step 4: Create `CharCharacterQueryCriteria.java`**

```java
package com.naon.grid.backend.service.character.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharCharacterQueryCriteria implements Serializable {

    @ApiModelProperty(value = "汉字或拼音模糊查询")
    @Query(blurry = "character,pinyin")
    private String blurry;
}
```

- [ ] **Step 5: Create mapper and service contract**

`CharCharacterMapper.java`:

```java
package com.naon.grid.backend.service.character.mapstruct;

import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.base.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CharCharacterMapper extends BaseMapper<CharCharacterDto, CharCharacter> {
}
```

`CharCharacterService.java`:

```java
package com.naon.grid.backend.service.character;

import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface CharCharacterService {

    PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable);

    CharCharacterDto findById(Integer id);

    Integer create(CharCharacterDto resources);

    void update(Integer id, CharCharacterDto resources);

    void delete(Integer id);
}
```

- [ ] **Step 6: Run compile to verify DTOs and MapStruct generation**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: PASS and generated source includes `CharCharacterMapperImpl` under `grid-system/target/generated-sources/annotations`.

- [ ] **Step 7: Commit Task 2**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java \
  grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java \
  grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java \
  grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterQueryCriteria.java \
  grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java \
  grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java
git commit -m "新增汉字服务DTO与接口"
```

## Task 3: Service Implementation

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: Create `CharCharacterServiceImpl.java`**

```java
package com.naon.grid.backend.service.character.impl;

import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.domain.character.CharDiscrimination;
import com.naon.grid.backend.domain.character.CharWord;
import com.naon.grid.backend.repo.character.CharCharacterRepository;
import com.naon.grid.backend.repo.character.CharDiscriminationRepository;
import com.naon.grid.backend.repo.character.CharWordRepository;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.backend.service.character.dto.CharDiscriminationDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.backend.service.character.mapstruct.CharCharacterMapper;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CharCharacterServiceImpl implements CharCharacterService {

    private final CharCharacterRepository charCharacterRepository;
    private final CharDiscriminationRepository charDiscriminationRepository;
    private final CharWordRepository charWordRepository;
    private final CharCharacterMapper charCharacterMapper;

    @Override
    public PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable) {
        Page<CharCharacter> page = charCharacterRepository.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder), pageable);
        return PageUtil.toPage(page.map(charCharacterMapper::toDto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CharCharacterDto findById(Integer id) {
        CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
        if (charCharacter.getId() == null) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }

        CharCharacterDto dto = charCharacterMapper.toDto(charCharacter);
        dto.setDiscriminations(convertToDiscriminationDtos(charDiscriminationRepository.findByCharId(id)));
        dto.setWords(convertToWordDtos(charWordRepository.findByCharId(id)));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(CharCharacterDto resources) {
        CharCharacter charCharacter = charCharacterMapper.toEntity(resources);
        charCharacter = charCharacterRepository.save(charCharacter);
        saveChildren(resources, charCharacter.getId());
        return charCharacter.getId();
    }

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
        charCharacter.setCharType(resources.getCharType());
        charCharacterRepository.save(charCharacter);

        deleteChildren(id);
        saveChildren(resources, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        CharCharacter charCharacter = charCharacterRepository.findById(id).orElseGet(CharCharacter::new);
        if (charCharacter.getId() == null) {
            throw new EntityNotFoundException(CharCharacter.class, "id", String.valueOf(id));
        }

        deleteChildren(id);
        charCharacterRepository.delete(charCharacter);
    }

    private void saveChildren(CharCharacterDto resources, Integer charId) {
        if (resources.getDiscriminations() != null) {
            for (CharDiscriminationDto discriminationDto : resources.getDiscriminations()) {
                charDiscriminationRepository.save(convertToDiscriminationEntity(discriminationDto, charId));
            }
        }
        if (resources.getWords() != null) {
            for (CharWordDto wordDto : resources.getWords()) {
                charWordRepository.save(convertToWordEntity(wordDto, charId));
            }
        }
    }

    private void deleteChildren(Integer charId) {
        charDiscriminationRepository.deleteAll(charDiscriminationRepository.findByCharId(charId));
        charWordRepository.deleteAll(charWordRepository.findByCharId(charId));
    }

    private List<CharDiscriminationDto> convertToDiscriminationDtos(List<CharDiscrimination> entities) {
        List<CharDiscriminationDto> resources = new ArrayList<>();
        for (CharDiscrimination entity : entities) {
            resources.add(convertToDiscriminationDto(entity));
        }
        return resources;
    }

    private CharDiscriminationDto convertToDiscriminationDto(CharDiscrimination entity) {
        CharDiscriminationDto dto = new CharDiscriminationDto();
        dto.setId(entity.getId());
        dto.setCharId(entity.getCharId());
        dto.setDiscrimChar(entity.getDiscrimChar());
        dto.setDiscrimPinyin(entity.getDiscrimPinyin());
        dto.setDiscrimCharTranslations(entity.getDiscrimCharTranslations());
        dto.setComparisonTranslations(entity.getComparisonTranslations());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    private List<CharWordDto> convertToWordDtos(List<CharWord> entities) {
        List<CharWordDto> resources = new ArrayList<>();
        for (CharWord entity : entities) {
            resources.add(convertToWordDto(entity));
        }
        return resources;
    }

    private CharWordDto convertToWordDto(CharWord entity) {
        CharWordDto dto = new CharWordDto();
        dto.setId(entity.getId());
        dto.setCharId(entity.getCharId());
        dto.setWordItem(entity.getWordItem());
        dto.setLevel(entity.getLevel());
        dto.setPinyin(entity.getPinyin());
        dto.setPartOfSpeech(entity.getPartOfSpeech());
        dto.setWordItemTranslations(entity.getWordItemTranslations());
        dto.setExampleSentence(entity.getExampleSentence());
        dto.setExamplePinyin(entity.getExamplePinyin());
        dto.setExampleTranslations(entity.getExampleTranslations());
        dto.setExampleImage(entity.getExampleImage());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    private CharDiscrimination convertToDiscriminationEntity(CharDiscriminationDto dto, Integer charId) {
        CharDiscrimination entity = new CharDiscrimination();
        entity.setCharId(charId);
        entity.setDiscrimChar(dto.getDiscrimChar());
        entity.setDiscrimPinyin(dto.getDiscrimPinyin());
        entity.setDiscrimCharTranslations(dto.getDiscrimCharTranslations());
        entity.setComparisonTranslations(dto.getComparisonTranslations());
        return entity;
    }

    private CharWord convertToWordEntity(CharWordDto dto, Integer charId) {
        CharWord entity = new CharWord();
        entity.setCharId(charId);
        entity.setWordItem(dto.getWordItem());
        entity.setLevel(dto.getLevel());
        entity.setPinyin(dto.getPinyin());
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setWordItemTranslations(dto.getWordItemTranslations());
        entity.setExampleSentence(dto.getExampleSentence());
        entity.setExamplePinyin(dto.getExamplePinyin());
        entity.setExampleTranslations(dto.getExampleTranslations());
        entity.setExampleImage(dto.getExampleImage());
        return entity;
    }
}
```

- [ ] **Step 2: Run compile to verify service wiring**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: PASS. If it fails with missing mapper implementation, rerun the same command once after confirming `CharCharacterMapper.java` exists.

- [ ] **Step 3: Commit Task 3**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java
git commit -m "新增汉字聚合服务实现"
```

## Task 4: REST Requests, VOs, and Controller

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterQueryRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterCreateVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java`

- [ ] **Step 1: Create request classes**

`CharCharacterQueryRequest.java`:

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharCharacterQueryRequest implements Serializable {

    @ApiModelProperty(value = "汉字或拼音模糊查询")
    private String blurry;
}
```

`CharCharacterCreateRequest.java`:

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CharCharacterCreateRequest implements Serializable {

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔顺")
    private String stroke;

    @ApiModelProperty(value = "部件组合中文说明")
    private String charDesc;

    @ApiModelProperty(value = "汉字说明的多语种翻译")
    private String descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationRequest> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordRequest> words;

    @Getter
    @Setter
    public static class CharDiscriminationRequest implements Serializable {

        @ApiModelProperty(value = "辨析汉字")
        private String discrimChar;

        @ApiModelProperty(value = "辨析字拼音")
        private String discrimPinyin;

        @ApiModelProperty(value = "辨析字说明的多语种翻译")
        private String discrimCharTranslations;

        @ApiModelProperty(value = "对比辨析说明的多语种翻译")
        private String comparisonTranslations;
    }

    @Getter
    @Setter
    public static class CharWordRequest implements Serializable {

        @ApiModelProperty(value = "组词")
        private String wordItem;

        @ApiModelProperty(value = "组词HSK等级")
        private String level;

        @ApiModelProperty(value = "组词拼音")
        private String pinyin;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "组词说明的多语种翻译")
        private String wordItemTranslations;

        @ApiModelProperty(value = "中文例句")
        private String exampleSentence;

        @ApiModelProperty(value = "例句拼音")
        private String examplePinyin;

        @ApiModelProperty(value = "例句的多语种翻译")
        private String exampleTranslations;

        @ApiModelProperty(value = "例句图片路径")
        private String exampleImage;
    }
}
```

- [ ] **Step 2: Create list and create response VOs**

`CharCharacterBaseVO.java`:

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class CharCharacterBaseVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
```

`CharCharacterCreateVO.java`:

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class CharCharacterCreateVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;
}
```

- [ ] **Step 3: Create detail VO**

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class CharCharacterVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "等级")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔顺")
    private String stroke;

    @ApiModelProperty(value = "部件组合中文说明")
    private String charDesc;

    @ApiModelProperty(value = "汉字说明的多语种翻译")
    private String descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationVO> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordVO> words;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Getter
    @Setter
    public static class CharDiscriminationVO implements Serializable {

        @ApiModelProperty(value = "辨析记录ID")
        private Integer id;

        @ApiModelProperty(value = "所属汉字ID")
        private Integer charId;

        @ApiModelProperty(value = "辨析汉字")
        private String discrimChar;

        @ApiModelProperty(value = "辨析字拼音")
        private String discrimPinyin;

        @ApiModelProperty(value = "辨析字说明的多语种翻译")
        private String discrimCharTranslations;

        @ApiModelProperty(value = "对比辨析说明的多语种翻译")
        private String comparisonTranslations;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class CharWordVO implements Serializable {

        @ApiModelProperty(value = "组词记录ID")
        private Integer id;

        @ApiModelProperty(value = "所属汉字ID")
        private Integer charId;

        @ApiModelProperty(value = "组词")
        private String wordItem;

        @ApiModelProperty(value = "组词HSK等级")
        private String level;

        @ApiModelProperty(value = "组词拼音")
        private String pinyin;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "组词说明的多语种翻译")
        private String wordItemTranslations;

        @ApiModelProperty(value = "中文例句")
        private String exampleSentence;

        @ApiModelProperty(value = "例句拼音")
        private String examplePinyin;

        @ApiModelProperty(value = "例句的多语种翻译")
        private String exampleTranslations;

        @ApiModelProperty(value = "例句图片路径")
        private String exampleImage;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }
}
```

- [ ] **Step 4: Create controller**

```java
package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.CharCharacterCreateRequest;
import com.naon.grid.backend.rest.request.CharCharacterQueryRequest;
import com.naon.grid.backend.rest.vo.CharCharacterBaseVO;
import com.naon.grid.backend.rest.vo.CharCharacterCreateVO;
import com.naon.grid.backend.rest.vo.CharCharacterVO;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.character.dto.CharCharacterQueryCriteria;
import com.naon.grid.backend.service.character.dto.CharDiscriminationDto;
import com.naon.grid.backend.service.character.dto.CharWordDto;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Api(tags = "汉字：汉字管理")
@RequestMapping("/api/character")
public class CharCharacterController {

    private final CharCharacterService charCharacterService;

    @Log("查询汉字详情")
    @ApiOperation("根据ID查询汉字详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<CharCharacterVO> findById(@PathVariable Integer id) {
        return new ResponseEntity<>(toVO(charCharacterService.findById(id)), HttpStatus.OK);
    }

    @Log("查询汉字列表")
    @ApiOperation("分页查询汉字列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<CharCharacterBaseVO>> queryAll(CharCharacterQueryRequest request, Pageable pageable) {
        PageResult<CharCharacterDto> pageResult = charCharacterService.queryAll(toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("新增汉字")
    @ApiOperation("新增汉字")
    @AnonymousPostMapping
    public ResponseEntity<CharCharacterCreateVO> create(@RequestBody CharCharacterCreateRequest request) {
        CharCharacterCreateVO vo = new CharCharacterCreateVO();
        vo.setId(charCharacterService.create(toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("更新汉字")
    @ApiOperation("更新汉字")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Integer id, @RequestBody CharCharacterCreateRequest request) {
        charCharacterService.update(id, toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除汉字")
    @ApiOperation("删除汉字")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Integer id) {
        charCharacterService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private CharCharacterQueryCriteria toCriteria(CharCharacterQueryRequest request) {
        CharCharacterQueryCriteria criteria = new CharCharacterQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        return criteria;
    }

    private CharCharacterDto toDto(CharCharacterCreateRequest request) {
        CharCharacterDto dto = new CharCharacterDto();
        dto.setSequenceNo(request.getSequenceNo());
        dto.setCharacter(request.getCharacter());
        dto.setLevel(request.getLevel());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTraditional(request.getTraditional());
        dto.setRadical(request.getRadical());
        dto.setStroke(request.getStroke());
        dto.setCharDesc(request.getCharDesc());
        dto.setDescTranslations(request.getDescTranslations());
        dto.setCharType(request.getCharType());
        dto.setDiscriminations(toDiscriminationDtoList(request.getDiscriminations()));
        dto.setWords(toWordDtoList(request.getWords()));
        return dto;
    }

    private List<CharDiscriminationDto> toDiscriminationDtoList(List<CharCharacterCreateRequest.CharDiscriminationRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toDiscriminationDto).collect(Collectors.toList());
    }

    private CharDiscriminationDto toDiscriminationDto(CharCharacterCreateRequest.CharDiscriminationRequest request) {
        CharDiscriminationDto dto = new CharDiscriminationDto();
        dto.setDiscrimChar(request.getDiscrimChar());
        dto.setDiscrimPinyin(request.getDiscrimPinyin());
        dto.setDiscrimCharTranslations(request.getDiscrimCharTranslations());
        dto.setComparisonTranslations(request.getComparisonTranslations());
        return dto;
    }

    private List<CharWordDto> toWordDtoList(List<CharCharacterCreateRequest.CharWordRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toWordDto).collect(Collectors.toList());
    }

    private CharWordDto toWordDto(CharCharacterCreateRequest.CharWordRequest request) {
        CharWordDto dto = new CharWordDto();
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

    private List<CharCharacterBaseVO> toBaseVOList(List<CharCharacterDto> resources) {
        return resources.stream().map(this::toBaseVO).collect(Collectors.toList());
    }

    private CharCharacterBaseVO toBaseVO(CharCharacterDto dto) {
        CharCharacterBaseVO vo = new CharCharacterBaseVO();
        vo.setId(dto.getId());
        vo.setSequenceNo(dto.getSequenceNo());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setCharType(dto.getCharType());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private CharCharacterVO toVO(CharCharacterDto dto) {
        CharCharacterVO vo = new CharCharacterVO();
        vo.setId(dto.getId());
        vo.setSequenceNo(dto.getSequenceNo());
        vo.setCharacter(dto.getCharacter());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTraditional(dto.getTraditional());
        vo.setRadical(dto.getRadical());
        vo.setStroke(dto.getStroke());
        vo.setCharDesc(dto.getCharDesc());
        vo.setDescTranslations(dto.getDescTranslations());
        vo.setCharType(dto.getCharType());
        vo.setDiscriminations(toDiscriminationVOList(dto.getDiscriminations()));
        vo.setWords(toWordVOList(dto.getWords()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<CharCharacterVO.CharDiscriminationVO> toDiscriminationVOList(List<CharDiscriminationDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toDiscriminationVO).collect(Collectors.toList());
    }

    private CharCharacterVO.CharDiscriminationVO toDiscriminationVO(CharDiscriminationDto dto) {
        CharCharacterVO.CharDiscriminationVO vo = new CharCharacterVO.CharDiscriminationVO();
        vo.setId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setDiscrimChar(dto.getDiscrimChar());
        vo.setDiscrimPinyin(dto.getDiscrimPinyin());
        vo.setDiscrimCharTranslations(dto.getDiscrimCharTranslations());
        vo.setComparisonTranslations(dto.getComparisonTranslations());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<CharCharacterVO.CharWordVO> toWordVOList(List<CharWordDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toWordVO).collect(Collectors.toList());
    }

    private CharCharacterVO.CharWordVO toWordVO(CharWordDto dto) {
        CharCharacterVO.CharWordVO vo = new CharCharacterVO.CharWordVO();
        vo.setId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setWordItem(dto.getWordItem());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setWordItemTranslations(dto.getWordItemTranslations());
        vo.setExampleSentence(dto.getExampleSentence());
        vo.setExamplePinyin(dto.getExamplePinyin());
        vo.setExampleTranslations(dto.getExampleTranslations());
        vo.setExampleImage(dto.getExampleImage());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
}
```

- [ ] **Step 5: Run compile to verify controller and REST models**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: PASS.

- [ ] **Step 6: Commit Task 4**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterQueryRequest.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterCreateVO.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharCharacterController.java
git commit -m "新增汉字管理匿名接口"
```

## Task 5: Final Verification

**Files:**
- Verify: all files created in Tasks 1-4

- [ ] **Step 1: Run final compile**

Run:

```bash
mvn -pl grid-system -am compile
```

Expected: PASS.

- [ ] **Step 2: Verify API shape in source**

Run:

```bash
git diff --name-only HEAD~4..HEAD
```

Expected output includes the created `domain/character`, `repo/character`, `service/character`, `rest/request/CharCharacter*`, `rest/vo/CharCharacter*`, and `rest/controller/CharCharacterController.java` files.

- [ ] **Step 3: Manual CRUD smoke test after database tables exist**

Start the application using the repository's normal Spring Boot run configuration after creating the three SQL tables from the design spec. Then verify these requests with Swagger or an API client:

Create request:

```http
POST /api/character
Content-Type: application/json

{
  "sequenceNo": 1,
  "character": "你",
  "level": "1",
  "pinyin": "nǐ",
  "audioId": 1001,
  "traditional": "你",
  "radical": "亻",
  "stroke": "撇,竖,撇,横钩,竖钩,撇,点",
  "charDesc": "亻表示人，尔表示读音。",
  "descTranslations": "{\"en\":\"person radical with phonetic component\"}",
  "discriminations": [
    {
      "discrimChar": "您",
      "discrimPinyin": "nín",
      "discrimCharTranslations": "{\"en\":\"polite you\"}",
      "comparisonTranslations": "{\"en\":\"你 is common; 您 is polite.\"}"
    }
  ],
  "words": [
    {
      "wordItem": "你好",
      "level": "1",
      "pinyin": "nǐ hǎo",
      "partOfSpeech": "短语",
      "wordItemTranslations": "{\"en\":\"hello\"}",
      "exampleSentence": "你好，我叫小明。",
      "examplePinyin": "nǐ hǎo, wǒ jiào xiǎo míng.",
      "exampleTranslations": "{\"en\":\"Hello, my name is Xiaoming.\"}",
      "exampleImage": "/images/examples/nihao.png"
    }
  ]
}
```

Expected: HTTP 201 with body containing a numeric `id`.

List request:

```http
GET /api/character?blurry=ni&page=0&size=10
```

Expected: HTTP 200 with a paged response whose `content` items include main-table fields only and do not include `discriminations` or `words`.

Detail request:

```http
GET /api/character/{id}
```

Expected: HTTP 200 with main fields, `discriminations`, and `words`.

Update request:

```http
PUT /api/character/{id}
Content-Type: application/json

{
  "sequenceNo": 1,
  "character": "你",
  "level": "1",
  "pinyin": "nǐ",
  "audioId": 1002,
  "traditional": "你",
  "radical": "亻",
  "stroke": "撇,竖,撇,横钩,竖钩,撇,点",
  "charDesc": "更新后的说明。",
  "descTranslations": "{\"en\":\"updated description\"}",
  "discriminations": [],
  "words": []
}
```

Expected: HTTP 204. A following detail request returns empty `discriminations` and `words`.

Delete request:

```http
DELETE /api/character/{id}
```

Expected: HTTP 204. A following detail request returns the project's standard not-found error response.

- [ ] **Step 4: Commit any verification-only fixes**

If final verification required code fixes, commit only the changed source files:

```bash
git status --short
git add <changed-source-file-paths>
git commit -m "修复汉字管理编译问题"
```

If no fixes were required, do not create an empty commit.

## Self-Review

- Spec coverage: Tasks 1-4 implement the data model, aggregate service, anonymous CRUD endpoints, nested detail response, blurry query, and text translation fields from `docs/superpowers/specs/2026-05-24-character-management-design.md`.
- Placeholder scan: This plan intentionally contains no placeholder sections or deferred implementation steps.
- Type consistency: SQL `bigint` maps to Java `Long`; all translation fields map to `String`; IDs use `Integer`; request, DTO, entity, and VO field names match across tasks.
