# 词汇后台管理接口表重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将词汇后台管理接口从旧的 vocab_example/vocab_exercise/JSON-relation 模式迁移到共享 example_sentence 表 + vocab_relation 关联表

**Architecture:** 保持草稿工作流不变（create→update→review→publish→offline），仅替换子表存储方式：例句改用 ExampleSentenceService（参考汉字模块），关联词汇从 sense 表的 JSON 列迁移到 vocab_relation 关联表，删除练习相关代码。

**Tech Stack:** Spring Boot 2.7.18 + Spring Data JPA + MySQL + Fastjson2 + MapStruct

---

## 文件结构

### 新建
| 文件 | 职责 |
|------|------|
| `domain/vocabulary/VocabRelation.java` | vocab_relation 表实体 |
| `service/vocabulary/dto/VocabRelationDto.java` | 关联词汇 DTO |
| `repo/vocabulary/VocabRelationRepository.java` | 关联词汇 Repository |

### 删除
| 文件 | 理由 |
|------|------|
| `domain/vocabulary/VocabExample.java` | 例句迁到通用 example_sentence 表 |
| `domain/vocabulary/VocabExercise.java` | 暂不实现练习 |
| `service/vocabulary/dto/VocabExampleDto.java` | 同上 |
| `service/vocabulary/dto/VocabExerciseDto.java` | 同上 |
| `repo/vocabulary/VocabExampleRepository.java` | 同上 |
| `repo/vocabulary/VocabExerciseRepository.java` | 同上 |

### 修改
| 文件 | 修改内容 |
|------|---------|
| `domain/vocabulary/VocabSense.java` | 列映射改名/删旧字段 |
| `domain/vocabulary/VocabStructure.java` | `structureOrder` → column `` `order` `` |
| `service/vocabulary/dto/VocabSenseDto.java` | 字段同步实体变化 + 新增关系字段 |
| `service/vocabulary/dto/VocabStructureDto.java` | examples → structureSentences |
| `service/vocabulary/dto/VocabWordDto.java` | 删除 exercises |
| `service/vocabulary/dto/VocabWordDraftDto.java` | 删除 exercises |
| `service/vocabulary/mapstruct/VocabWordMapper.java` | 删除 exercise 相关方法 |
| `repo/vocabulary/VocabSenseRepository.java` | 无变化（可选新增方法） |
| `rest/controller/VocabWordController.java` | 删除 exercise/example 引用 |
| `rest/wrapper/VocabWordWrapper.java` | 全面更新映射逻辑 |
| `service/vocabulary/VocabWordService.java` | 删除 exercise 方法引用 |
| `service/vocabulary/impl/VocabWordServiceImpl.java` | 核心重构 |

### 不修改（已由用户定义好）
| 文件 | 说明 |
|------|------|
| `rest/request/VocabWordCreateRequest.java` | 已含新结构 |
| `rest/vo/VocabWordVO.java` | 已含新结构 |
| `rest/vo/VocabWordBaseVO.java` | 已含新结构 |
| `rest/vo/VocabRelationVO.java` | 已定义 |
| `rest/request/VocabRelationRequest.java` | 已定义 |
| `rest/request/ExampleSentenceRequest.java` | 已定义 |
| `rest/vo/ExampleSentenceVO.java` | 已定义 |

---

### Task 1: 新建 VocabRelation 实体

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabRelation.java`

```java
package com.naon.grid.backend.domain.vocabulary;

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
@Table(name = "vocab_relation")
public class VocabRelation implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "关联词汇ID", hidden = true)
    private Long id;

    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "词汇ID")
    private Integer wordId;

    @Column(name = "sense_id", nullable = false)
    @ApiModelProperty(value = "义项ID")
    private Integer senseId;

    @Column(name = "word", nullable = false, length = 32)
    @ApiModelProperty(value = "当前词汇")
    private String word;

    @Column(name = "relation_type", length = 32)
    @ApiModelProperty(value = "关联类型")
    private String relationType;

    @Column(name = "relation_word_id", nullable = false)
    @ApiModelProperty(value = "关联词汇ID")
    private Long relationWordId;

    @Column(name = "relation_sense_id", nullable = false)
    @ApiModelProperty(value = "关联义项ID")
    private Long relationSenseId;

    @Column(name = "relation_word", nullable = false, length = 32)
    @ApiModelProperty(value = "关联词汇")
    private String relationWord;

    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（大在前）")
    private Integer relationOrder = 0;

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

- [ ] **Step 1: Write file**

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabRelation.java
git commit -m "feat: add VocabRelation entity for vocab_relation table"
```

---

### Task 2: 新建 VocabRelationDto

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabRelationDto.java`

```java
package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class VocabRelationDto implements Serializable {

    @ApiModelProperty(value = "关联ID")
    private Long id;

    @ApiModelProperty(value = "词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "义项ID")
    private Integer senseId;

    @ApiModelProperty(value = "当前词汇")
    private String word;

    @ApiModelProperty(value = "关联类型")
    private String relationType;

    @ApiModelProperty(value = "关联词汇ID")
    private Long relationWordId;

    @ApiModelProperty(value = "关联义项ID")
    private Long relationSenseId;

    @ApiModelProperty(value = "关联词汇")
    private String relationWord;

    @ApiModelProperty(value = "排序权重")
    private Integer order;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 1: Write file**

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabRelationDto.java
git commit -m "feat: add VocabRelationDto"
```

---

### Task 3: 新建 VocabRelationRepository

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabRelationRepository.java`

```java
package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabRelationRepository extends JpaRepository<VocabRelation, Long> {

    List<VocabRelation> findBySenseIdAndStatus(Integer senseId, Integer status);

    List<VocabRelation> findBySenseIdAndStatusAndRelationType(Integer senseId, Integer status, String relationType);
}
```

- [ ] **Step 1: Write file**

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabRelationRepository.java
git commit -m "feat: add VocabRelationRepository"
```

---

### Task 4: 修改 VocabSense 实体

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabSense.java`

变更：
- `translations` → `defTranslations` (column `def_translations`)
- `defImage` → `defImageId` (column `def_image_id` / 或 `defImage` 改 column 为 `def_image_id`)
- `senseOrder` → column `` `order` `` (Java 字段名保持 `senseOrder`，column 改为 `` `order` ``)  
- 删除：`synonyms`, `antonyms`, `relatedForward`, `relatedBackward`, `relatedOther` 整段字段+column

```java
// 修改后的完整文件关键变化：

    @Column(name = "def_audio_id")
    @ApiModelProperty(value = "中文释义音频资源ID")
    private Long defAudioId;

    @Column(name = "def_image_id")         // was: def_image
    @ApiModelProperty(value = "中文释义图片资源ID")
    private Long defImageId;               // was: defImage

    @Column(name = "def_translations", columnDefinition = "json")  // was: translations
    @ApiModelProperty(value = "外文翻译列表")
    private String defTranslations;        // was: translations

    // 删除以下整段字段：
    // private String synonyms;
    // private String antonyms;
    // private String relatedForward;
    // private String relatedBackward;
    // private String relatedOther;

    @NotNull
    @Column(name = "`order`", nullable = false)   // was: sense_order
    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder = 0;
```

- [ ] **Step 1: 修改 VocabSense.java 的字段映射**

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabSense.java
git commit -m "refactor: update VocabSense entity fields to match new schema"
```

---

### Task 5: 修改 VocabStructure 实体

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabStructure.java`

```java
    // 唯一变化：
    @NotNull
    @Column(name = "`order`", nullable = false)   // was: structure_order
    @ApiModelProperty(value = "搭配排序权重")
    private Integer structureOrder = 0;
```

- [ ] **Step 1: 修改 VocabStructure.java 的列映射**

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabStructure.java
git commit -m "refactor: update VocabStructure column mapping for order"
```

---

### Task 6: 删除 VocabExample / VocabExercise 实体

**Files:**
- Delete: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabExample.java`
- Delete: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabExercise.java`

```bash
git rm grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabExample.java \
       grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabExercise.java
git commit -m "refactor: remove VocabExample and VocabExercise entities"
```

- [ ] **Step 1: 删除文件并提交**

---

### Task 7: 修改 VocabSenseDto

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java`

```java
package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabSenseDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "自增ID, 义项ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "中文释义")
    private String chineseDef;

    @ApiModelProperty(value = "中文释义音频资源ID")
    private Long defAudioId;

    @ApiModelProperty(value = "中文释义图片资源ID")
    private Long defImageId;

    @ApiModelProperty(value = "中文释义外文翻译")
    private List<TextTranslation> defTranslations;

    @ApiModelProperty(value = "中文释义图片例句")
    private ExampleSentenceDto defImageSentence;

    @ApiModelProperty(value = "近义词列表")
    private List<VocabRelationDto> synonymWords;

    @ApiModelProperty(value = "反义词列表")
    private List<VocabRelationDto> antonymWords;

    @ApiModelProperty(value = "正序关联词汇")
    private List<VocabRelationDto> sequentialWords;

    @ApiModelProperty(value = "逆序关联词汇")
    private List<VocabRelationDto> reverseSequentialWords;

    @ApiModelProperty(value = "乱序关联词汇")
    private List<VocabRelationDto> jumbledWords;

    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder;

    @ApiModelProperty(value = "搭配列表")
    private List<VocabStructureDto> structures;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 1: 写入新内容**

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java
git commit -m "refactor: update VocabSenseDto with new relation fields"
```

---

### Task 8: 修改 VocabStructureDto

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabStructureDto.java`

```java
package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabStructureDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "自增ID, 结构搭配ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "所属义项ID")
    private Integer senseId;

    @ApiModelProperty(value = "结构搭配文案")
    private String pattern;

    @ApiModelProperty(value = "结构搭配释义")
    private String patternDef;

    @ApiModelProperty(value = "结构搭配释义外文翻译列表")
    private List<TextTranslation> patternDefTranslations;

    @ApiModelProperty(value = "搭配排序权重")
    private Integer structureOrder;

    @ApiModelProperty(value = "结构例句列表（通过通用例句表存储）")
    private List<ExampleSentenceDto> structureSentences;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 1: 写入新内容**

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabStructureDto.java
git commit -m "refactor: update VocabStructureDto to use ExampleSentenceDto"
```

---

### Task 9: 修改 VocabWordDto 和 VocabWordDraftDto（删除 exercises）

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDraftDto.java`

VocabWordDto 中删除：
```java
// 删除
@ApiModelProperty(value = "练习题列表")
private List<VocabExerciseDto> exercises;
```

VocabWordDraftDto 中删除同样字段。

- [ ] **Step 1: 修改两个 DTO 删除 exercises 字段**

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDto.java \
       grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDraftDto.java
git commit -m "refactor: remove exercises from VocabWordDto and VocabWordDraftDto"
```

---

### Task 10: 删除 VocabExampleDto / VocabExerciseDto

**Files:**
- Delete: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExampleDto.java`
- Delete: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExerciseDto.java`

```bash
git rm grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExampleDto.java \
       grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExerciseDto.java
git commit -m "refactor: remove VocabExampleDto and VocabExerciseDto"
```

- [ ] **Step 1: 删除并提交**

---

### Task 11: 删除 VocabExampleRepository / VocabExerciseRepository

**Files:**
- Delete: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabExampleRepository.java`
- Delete: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabExerciseRepository.java`

```bash
git rm grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabExampleRepository.java \
       grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabExerciseRepository.java
git commit -m "refactor: remove VocabExampleRepository and VocabExerciseRepository"
```

- [ ] **Step 1: 删除并提交**

---

### Task 12: 重写 VocabWordWrapper

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java`

完整替换（详见附件代码）。核心变化：删除所有 exercise/example 相关方法，增加 relation/example-sentence 的转换。注意 VocabRelationRequest ↔ VocabRelationDto ↔ VocabRelationVO 的转换。

```java
package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.ExampleSentenceRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.request.VocabRelationRequest;
import com.naon.grid.backend.rest.request.VocabWordCreateRequest;
import com.naon.grid.backend.rest.request.VocabWordQueryRequest;
import com.naon.grid.backend.rest.vo.ExampleSentenceVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.vo.VocabRelationVO;
import com.naon.grid.backend.rest.vo.VocabWordBaseVO;
import com.naon.grid.backend.rest.vo.VocabWordVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabRelationDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VocabWordWrapper {

    public static VocabWordQueryCriteria toCriteria(VocabWordQueryRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    public static VocabWordDto toDto(VocabWordCreateRequest request) {
        VocabWordDto dto = new VocabWordDto();
        dto.setWord(request.getWord());
        dto.setWordTraditional(request.getWordTraditional());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setHskLevel(request.getHskLevel());
        dto.setSenses(toSenseDtoList(request.getSenses()));
        return dto;
    }

    private static List<VocabSenseDto> toSenseDtoList(List<VocabWordCreateRequest.VocabSenseRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabWordWrapper::toSenseDto).collect(Collectors.toList());
    }

    private static VocabSenseDto toSenseDto(VocabWordCreateRequest.VocabSenseRequest request) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(request.getId());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setChineseDef(request.getChineseDef());
        dto.setDefAudioId(request.getDefAudioId());
        dto.setDefImageId(request.getDefImageId());
        dto.setDefTranslations(toTextTranslationList(request.getDefTranslations()));
        dto.setDefImageSentence(toExampleSentenceDto(request.getDefImageSentence()));
        dto.setSynonymWords(toRelationDtoList(request.getSynonymWords()));
        dto.setAntonymWords(toRelationDtoList(request.getAntonymWords()));
        dto.setSequentialWords(toRelationDtoList(request.getSequentialWords()));
        dto.setReverseSequentialWords(toRelationDtoList(request.getReverseSequentialWords()));
        dto.setJumbledWords(toRelationDtoList(request.getJumbledWords()));
        dto.setSenseOrder(request.getOrder() != null ? request.getOrder() : 0);
        dto.setStructures(toStructureDtoList(request.getStructures()));
        return dto;
    }

    private static List<VocabStructureDto> toStructureDtoList(List<VocabWordCreateRequest.VocabStructureRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabWordWrapper::toStructureDto).collect(Collectors.toList());
    }

    private static VocabStructureDto toStructureDto(VocabWordCreateRequest.VocabStructureRequest request) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(request.getId());
        dto.setPattern(request.getPattern());
        dto.setPatternDef(request.getPatternDef());
        dto.setPatternDefTranslations(toTextTranslationList(request.getPatternDefTranslations()));
        dto.setStructureOrder(request.getOrder() != null ? request.getOrder() : 0);
        dto.setStructureSentences(toExampleSentenceDtoList(request.getStructureSentences()));
        return dto;
    }

    private static List<VocabRelationDto> toRelationDtoList(List<VocabRelationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabWordWrapper::toRelationDto).collect(Collectors.toList());
    }

    private static VocabRelationDto toRelationDto(VocabRelationRequest request) {
        if (request == null) return null;
        VocabRelationDto dto = new VocabRelationDto();
        dto.setRelationType(request.getRelationType());
        dto.setRelationWordId(request.getRelationWordId());
        dto.setRelationSenseId(request.getRelationSenseId());
        dto.setRelationWord(request.getRelationWord());
        dto.setOrder(request.getOrder());
        return dto;
    }

    private static ExampleSentenceDto toExampleSentenceDto(ExampleSentenceRequest request) {
        if (request == null) return null;
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

    private static List<ExampleSentenceDto> toExampleSentenceDtoList(List<ExampleSentenceRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabWordWrapper::toExampleSentenceDto).collect(Collectors.toList());
    }

    public static List<VocabWordBaseVO> toBaseVOList(List<VocabWordDto> resources) {
        return resources.stream().map(VocabWordWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static VocabWordBaseVO toBaseVO(VocabWordDto dto) {
        VocabWordBaseVO vo = new VocabWordBaseVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static VocabWordVO toVO(VocabWordDto dto) {
        VocabWordVO vo = new VocabWordVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setHskLevel(dto.getHskLevel());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setSenses(toSenseVOList(dto.getSenses()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<VocabWordVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> resources) {
        if (resources == null) return Collections.emptyList();
        return resources.stream().map(VocabWordWrapper::toSenseVO).collect(Collectors.toList());
    }

    private static VocabWordVO.VocabSenseVO toSenseVO(VocabSenseDto dto) {
        VocabWordVO.VocabSenseVO vo = new VocabWordVO.VocabSenseVO();
        vo.setId(dto.getId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        vo.setDefAudioId(dto.getDefAudioId());
        vo.setDefImageId(dto.getDefImageId());
        vo.setDefTranslations(toTextTranslationVOList(dto.getDefTranslations()));
        vo.setDefImageSentence(toExampleSentenceVO(dto.getDefImageSentence()));
        vo.setSynonymWords(toRelationVOList(dto.getSynonymWords()));
        vo.setAntonymWords(toRelationVOList(dto.getAntonymWords()));
        vo.setSequentialWords(toRelationVOList(dto.getSequentialWords()));
        vo.setReverseSequentialWords(toRelationVOList(dto.getReverseSequentialWords()));
        vo.setJumbledWords(toRelationVOList(dto.getJumbledWords()));
        vo.setOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures()));
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<VocabWordVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> resources) {
        if (resources == null) return Collections.emptyList();
        return resources.stream().map(VocabWordWrapper::toStructureVO).collect(Collectors.toList());
    }

    private static VocabWordVO.VocabStructureVO toStructureVO(VocabStructureDto dto) {
        VocabWordVO.VocabStructureVO vo = new VocabWordVO.VocabStructureVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setSenseId(dto.getSenseId());
        vo.setPattern(dto.getPattern());
        vo.setPatternDef(dto.getPatternDef());
        vo.setPatternDefTranslations(toTextTranslationVOList(dto.getPatternDefTranslations()));
        vo.setOrder(dto.getStructureOrder());
        vo.setStructureExamples(toExampleSentenceVOList(dto.getStructureSentences()));
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<VocabRelationVO> toRelationVOList(List<VocabRelationDto> resources) {
        if (resources == null) return Collections.emptyList();
        return resources.stream().map(VocabWordWrapper::toRelationVO).collect(Collectors.toList());
    }

    private static VocabRelationVO toRelationVO(VocabRelationDto dto) {
        if (dto == null) return null;
        VocabRelationVO vo = new VocabRelationVO();
        vo.setRelationId(dto.getId() != null ? dto.getId() : 0L);
        vo.setRelationType(dto.getRelationType());
        vo.setRelationWordId(dto.getRelationWordId() != null ? dto.getRelationWordId() : 0L);
        vo.setRelationSenseId(dto.getRelationSenseId() != null ? dto.getRelationSenseId() : 0L);
        vo.setRelationWord(dto.getRelationWord());
        vo.setOrder(dto.getOrder() != null ? dto.getOrder() : 0);
        return vo;
    }

    private static ExampleSentenceVO toExampleSentenceVO(ExampleSentenceDto dto) {
        if (dto == null) return null;
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

    private static List<ExampleSentenceVO> toExampleSentenceVOList(List<ExampleSentenceDto> resources) {
        if (resources == null) return Collections.emptyList();
        return resources.stream().map(VocabWordWrapper::toExampleSentenceVO).collect(Collectors.toList());
    }

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(VocabWordWrapper::toTextTranslation).collect(Collectors.toList());
    }

    private static TextTranslation toTextTranslation(TextTranslationRequest request) {
        if (request == null) return null;
        TextTranslation t = new TextTranslation();
        t.setLanguage(request.getLanguage());
        t.setTranslation(request.getTranslation());
        return t;
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(VocabWordWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation t) {
        if (t == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(t.getLanguage());
        vo.setTranslation(t.getTranslation());
        return vo;
    }
}
```

- [ ] **Step 1: 全量替换 VocabWordWrapper.java**

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java
git commit -m "refactor: rewrite VocabWordWrapper with new DTO/VO mappings"
```

---

### Task 13: 修改 VocabWordController（清理引用）

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

删除不再需要的 import：
```java
// 删除这些 import：
import com.naon.grid.backend.rest.request.ExerciseOptionRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.ExerciseOptionVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.vo.VocabOutlineRecordVO;
import com.naon.grid.domain.common.ExerciseOption;
import com.naon.grid.backend.service.vocabulary.dto.VocabExampleDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabExerciseDto;
// ... 和其他已经不用的
```

实际上，由于 VocabWordCreateRequest 已经不再包含 ExerciseRequest，且 Wrapper 也不再需要这些工具类，Controller 可以清理所有旧的 import。但为了最小修改，只需确保编译通过即可。

Controller 的代码逻辑本身没有变化（接口方法签名不变），只需要清理 import 即可。

- [ ] **Step 1: 清理 Controller 中不再使用的 import**

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java
git commit -m "refactor: clean up VocabWordController imports"
```

---

### Task 14: 重写 VocabWordServiceImpl（核心）

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`
- Modify（可选）: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java`

这是最复杂的一步，完整内容较多，按子步骤拆分：

#### 14.1 替换注入和常量

```java
// 删除：
private final VocabExampleRepository vocabExampleRepository;
private final VocabExerciseRepository vocabExerciseRepository;

// 新增：
private final ExampleSentenceService exampleSentenceService;
private final VocabRelationRepository vocabRelationRepository;

// 新增常量：
private static final String DEF_IMAGE_SENTENCE_BIZ = SentenceBizTypeEnum.VOCAB_SENSE_DEF_IMAGE_SENTENCE.getCode();
private static final String STRUCTURE_SENTENCE_BIZ = SentenceBizTypeEnum.VOCAB_SENSE_STRUCTURE_SENTENCE.getCode();
```

#### 14.2 修改 convertToSenseDto

- 用 `defTranslations` 替换 `translations`
- 用 `defImageId` 替换 `defImage`
- 删除 synonmys/antonyms/related* 的 JSON 解析
- 通过 `ExampleSentenceService.findOne()` 加载 `defImageSentence`
- 通过 `VocabRelationRepository` 按 senseId 加载 5 种关联关系并分组
- 结构中的例句通过 `ExampleSentenceService.findByBizIds()` 批量加载

```java
private VocabSenseDto convertToSenseDto(VocabSense sense) {
    VocabSenseDto dto = new VocabSenseDto();
    dto.setId(sense.getId());
    dto.setWordId(sense.getWordId());
    dto.setPartOfSpeech(sense.getPartOfSpeech());
    dto.setChineseDef(sense.getChineseDef());
    dto.setDefAudioId(sense.getDefAudioId());
    dto.setDefImageId(sense.getDefImageId());
    dto.setDefTranslations(JsonUtils.parseTranslationList(sense.getDefTranslations()));
    dto.setSenseOrder(sense.getSenseOrder());
    dto.setCreateTime(sense.getCreateTime());
    dto.setUpdateTime(sense.getUpdateTime());
    dto.setStatus(sense.getStatus());

    // 加载释义图片例句
    dto.setDefImageSentence(exampleSentenceService.findOne(
            DEF_IMAGE_SENTENCE_BIZ, sense.getId().longValue()));

    // 加载关联词汇（按类型分组）
    List<VocabRelation> relations = vocabRelationRepository.findBySenseIdAndStatus(
            sense.getId(), StatusEnum.ENABLED.getCode());
    Map<String, List<VocabRelationDto>> grouped = new HashMap<>();
    for (VocabRelation r : relations) {
        grouped.computeIfAbsent(r.getRelationType(), k -> new ArrayList<>())
               .add(toVocabRelationDto(r));
    }
    dto.setSynonymWords(grouped.getOrDefault(VocabRelationTypeEnum.SYNONYMS_WORDS.getCode(), Collections.emptyList()));
    dto.setAntonymWords(grouped.getOrDefault(VocabRelationTypeEnum.ANTONYMS_WORDS.getCode(), Collections.emptyList()));
    dto.setSequentialWords(grouped.getOrDefault(VocabRelationTypeEnum.SEQUENTIAL_WORDS.getCode(), Collections.emptyList()));
    dto.setReverseSequentialWords(grouped.getOrDefault(VocabRelationTypeEnum.REVERSE_SEQUENTIAL_WORDS.getCode(), Collections.emptyList()));
    dto.setJumbledWords(grouped.getOrDefault(VocabRelationTypeEnum.JUMBLED_WORDS.getCode(), Collections.emptyList()));

    // 加载结构（含例句）
    List<VocabStructureDto> structureDtos = new ArrayList<>();
    List<VocabStructure> structures = vocabStructureRepository.findBySenseIdAndStatus(
            sense.getId(), StatusEnum.ENABLED.getCode());
    for (VocabStructure structure : structures) {
        structureDtos.add(convertToStructureDto(structure));
    }
    dto.setStructures(structureDtos);

    return dto;
}
```

#### 14.3 修改 convertToStructureDto

```java
private VocabStructureDto convertToStructureDto(VocabStructure structure) {
    VocabStructureDto dto = new VocabStructureDto();
    dto.setId(structure.getId());
    dto.setWordId(structure.getWordId());
    dto.setSenseId(structure.getSenseId());
    dto.setPattern(structure.getPattern());
    dto.setPatternDef(structure.getPatternDef());
    dto.setPatternDefTranslations(JsonUtils.parseTranslationList(structure.getPatternDefTranslations()));
    dto.setStructureOrder(structure.getStructureOrder());
    dto.setCreateTime(structure.getCreateTime());
    dto.setUpdateTime(structure.getUpdateTime());
    dto.setStatus(structure.getStatus());

    // 从通用例句表加载结构例句
    ExampleSentenceDto sentence = exampleSentenceService.findOne(
            STRUCTURE_SENTENCE_BIZ, structure.getId().longValue());
    if (sentence != null) {
        dto.setStructureSentences(Collections.singletonList(sentence));
    } else {
        dto.setStructureSentences(Collections.emptyList());
    }

    return dto;
}
```

Wait, actually looking at the spec again - a structure can have MULTIPLE example sentences. So I should use `findByBizIds` for batch loading of ALL structures at once. Let me adjust.

Actually, in the old code, a structure could have multiple examples. But looking at the new API request `VocabStructureRequest.structureSentences` - it's a List, so yes it can have multiple sentences.

Let me adjust the implementation to batch-load sentences for all structures at once:

```java
private List<VocabStructureDto> batchConvertStructureDto(List<VocabStructure> structures) {
    if (structures == null || structures.isEmpty()) return Collections.emptyList();
    
    List<VocabStructureDto> dtos = structures.stream()
        .map(this::convertStructureToDtoWithoutSentences)
        .collect(Collectors.toList());
    
    // Batch load sentences
    List<Long> structureIds = dtos.stream()
        .map(s -> s.getId().longValue())
        .collect(Collectors.toList());
    Map<Long, List<ExampleSentenceDto>> sentenceMap = ... // load all sentences for these structure IDs
    
    for (VocabStructureDto dto : dtos) {
        dto.setStructureSentences(sentenceMap.getOrDefault(dto.getId().longValue(), Collections.emptyList()));
    }
    return dtos;
}
```

Hmm, but ExampleSentenceService doesn't have a method to return grouped by bizId as a list. It has `findByBizIds` which groups by bizId but returns only the first (highest-order) sentence per bizId. That's because it's designed for cases where there's only one sentence per bizId (like char words).

For structures where there can be multiple sentences per structureId, I need a different approach. Let me think...

Looking at ExampleSentenceServiceImpl:
```java
public Map<Long, ExampleSentenceDto> findByBizIds(String bizType, Collection<Long> bizIds) {
    List<ExampleSentence> sentences = exampleSentenceRepository.findByBizTypeAndBizIdInAndStatus(...);
    // Groups by bizId but only keeps the first one (highest order)
    Map<Long, ExampleSentenceDto> result = new LinkedHashMap<>();
    for (ExampleSentence sentence : sentences) {
        if (!result.containsKey(sentence.getBizId())) {
            result.put(sentence.getBizId(), toDto(sentence));
        }
    }
    return result;
}
```

This only returns ONE sentence per bizId! But a structure can have multiple sentences. I need to either:
1. Use the repository directly instead of the service for this
2. Add a new method to ExampleSentenceService that returns all sentences per bizId

Wait, let me check the ExampleSentenceRepository:
```java
List<ExampleSentence> findByBizTypeAndBizIdInAndStatus(String bizType, Collection<Long> bizIds, Integer status);
```

This returns ALL matching sentences. So I can use the repository directly.

Actually, looking at the character code more carefully, each char word has exactly one sentence. But for vocab structures, there can be multiple. I think the cleanest approach is to use `exampleSentenceRepository` directly in `VocabWordServiceImpl` for the batch loading, to get all sentences for all structure IDs.

But wait - that mixes concerns. ExampleSentenceService is supposed to be the abstraction. Let me add a method to it that returns all sentences grouped by bizId as a Map<Long, List<ExampleSentenceDto>>.

Actually, this would be a new method that needs to be added to ExampleSentenceService. But the user said not to change things that aren't part of the scope... However, this is a genuine need. Let me think about if there's a simpler approach.

Alternative: I can load sentences one structure at a time using `exampleSentenceService.findOne()` but that would be N+1 queries. Not ideal but works.

Or I can directly inject `ExampleSentenceRepository` into `VocabWordServiceImpl` and call the findByBizTypeAndBizIdInAndStatus method directly, then group them manually.

Actually, I think the simplest approach is to just inject the ExampleSentenceRepository directly in VocabWordServiceImpl for this specific use case. Or better yet, add a new method findByBizIdsAsList to ExampleSentenceService.

Hmm, but to keep the plan focused, let me just inject ExampleSentenceRepository into VocabWordServiceImpl for the batch-load query. This follows the pattern already used where repos are directly injected into services.

Wait, I just realized something. Looking at the new API:

```java
VocabWordCreateRequest.VocabStructureRequest {
    // ...
    @Valid
    @ApiModelProperty(value = "结构例句")
    private List<ExampleSentenceRequest> structureSentences;
}
```

A structure can have MULTIPLE sentences. And in the old `VocabExample` table, a structure could have multiple examples. So I need to handle multiple sentences per structure.

Let me look at how `ExampleSentenceService.syncOne` works - it only handles ONE sentence per bizId (it disables all other sentences for the same bizId to keep only one active). That matches the "one sentence per bizId" pattern.

So for structures with multiple sentences, I can't use `syncOne`. I'd need to implement a different sync strategy.

Hmm, this is a design issue. Let me think about the options:

Option A: Modify `ExampleSentenceService` to handle multiple sentences per bizId (add a `syncList` method)
Option B: Use `ExampleSentenceRepository` directly in `VocabWordServiceImpl`
Option C: Only support one sentence per structure (simplify the data model)

Actually, looking more carefully at the SQL and the user's design, it seems like a structure CAN have multiple sentences. The ExampleSentenceService as-is supports this if I use the repository directly - the `syncOne` method just needs to be called for each sentence, but it would delete all other sentences for that bizId.

Actually wait, let me re-read `syncOne`:

```java
public ExampleSentenceDto syncOne(String bizType, Long bizId, ExampleSentenceDto sentence) {
    if (sentence == null || StringUtils.isBlank(sentence.getSentence())) {
        disableExisting(bizType, bizId, null);  // disables ALL sentences for this bizId
        return null;
    }
    // ... creates or updates the sentence
    entity = exampleSentenceRepository.save(entity);
    disableExisting(bizType, bizId, entity.getId());  // disables all EXCEPT the one just saved
    return toDto(entity);
}
```

So `syncOne` expects ONE sentence per bizId and disables all others. This doesn't support multiple sentences per structure.

I think the cleanest solution is to:
1. For **defImageSentence**: Use `syncOne` (one sentence per sense)
2. For **structureSentences**: Use `ExampleSentenceRepository` directly with batch operations

Let me revise the approach. I'll inject `ExampleSentenceRepository` for the batch operations on structure sentences, and keep using `ExampleSentenceService` for the single-sentence operations (defImageSentence).

For the structure sentences sync:
1. Delete (soft) all existing sentences for this structureId
2. Save new sentences for each submitted one

OK, let me revise the plan accordingly.

Actually, let me reconsider. For reading:
- For `findById`, I need to load all sentences for a structure
- I can use `exampleSentenceRepository.findByBizTypeAndBizIdInAndStatus()` for batch loading
- Then group by bizId to create Map<Integer, List<ExampleSentenceDto>>

For writing (publish):
- For each structure, delete old sentences and save new ones
- I'll add a helper method that uses `ExampleSentenceRepository` directly

Let me write this into the plan. I think I should keep the plan relatively high-level at this point since the user seems eager to proceed. Let me just make sure the important decisions are documented.

OK, I think for the plan document completeness, I'll outline the changes at a higher level for this task since it's the largest one, with specific code snippets for the critical parts.

Let me write the plan now.<｜end▁of▁thinking｜>

<｜｜DSML｜｜tool_calls>
<｜｜DSML｜｜invoke name="Write">
<｜｜DSML｜｜parameter name="content" string="true"># 词汇后台管理接口表重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将词汇后台管理接口从旧的 vocab_example/vocab_exercise/JSON-relation 模式迁移到共享 example_sentence 表 + vocab_relation 关联表

**Architecture:** 保持草稿工作流不变（create→update→review→publish→offline），仅替换子表存储方式：例句改用 ExampleSentenceService + ExampleSentenceRepository（参考汉字模块），关联词汇从 sense 表的 JSON 列迁移到 vocab_relation 关联表，删除练习相关代码。

**Tech Stack:** Spring Boot 2.7.18 + Spring Data JPA + MySQL + Fastjson2 + MapStruct

---

## 文件结构

### 新建
| 文件 | 职责 |
|------|------|
| `domain/vocabulary/VocabRelation.java` | vocab_relation 表实体 |
| `service/vocabulary/dto/VocabRelationDto.java` | 关联词汇 DTO |
| `repo/vocabulary/VocabRelationRepository.java` | 关联词汇 Repository |

### 删除
| 文件 | 理由 |
|------|------|
| `domain/vocabulary/VocabExample.java` | 例句迁到通用 example_sentence 表 |
| `domain/vocabulary/VocabExercise.java` | 暂不实现练习 |
| `service/vocabulary/dto/VocabExampleDto.java` | 同上 |
| `service/vocabulary/dto/VocabExerciseDto.java` | 同上 |
| `repo/vocabulary/VocabExampleRepository.java` | 同上 |
| `repo/vocabulary/VocabExerciseRepository.java` | 同上 |

### 修改
| 文件 | 修改内容 |
|------|---------|
| `domain/vocabulary/VocabSense.java` | 列映射改名/删旧字段 |
| `domain/vocabulary/VocabStructure.java` | `structureOrder` → column `` `order` `` |
| `service/vocabulary/dto/VocabSenseDto.java` | 字段同步实体变化 + 新增关系/例句字段 |
| `service/vocabulary/dto/VocabStructureDto.java` | examples → structureSentences |
| `service/vocabulary/dto/VocabWordDto.java` | 删除 exercises |
| `service/vocabulary/dto/VocabWordDraftDto.java` | 删除 exercises |
| `service/vocabulary/mapstruct/VocabWordMapper.java` | 无变化（仅删除无用方法） |
| `repo/vocabulary/VocabSenseRepository.java` | 无变化 |
| `rest/controller/VocabWordController.java` | 删除 exercise/example 引用 |
| `rest/wrapper/VocabWordWrapper.java` | 全面更新映射逻辑 |
| `service/vocabulary/impl/VocabWordServiceImpl.java` | 核心重构 |

### 不修改（已由用户定义好）
| 文件 | 说明 |
|------|------|
| `rest/request/VocabWordCreateRequest.java` | 已含新结构（VocabRelationRequest、ExampleSentenceRequest、无 exercises） |
| `rest/request/VocabRelationRequest.java` | 已定义 |
| `rest/request/ExampleSentenceRequest.java` | 已定义 |
| `rest/vo/VocabWordVO.java` | 已含新结构（VocabRelationVO、ExampleSentenceVO） |
| `rest/vo/VocabWordBaseVO.java` | 已更新 |
| `rest/vo/VocabRelationVO.java` | 已定义 |
| `rest/vo/ExampleSentenceVO.java` | 已定义 |

---

### Task 1: 新建 VocabRelation 实体

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabRelation.java`

```java
package com.naon.grid.backend.domain.vocabulary;

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
@Table(name = "vocab_relation")
public class VocabRelation implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "关联词汇ID", hidden = true)
    private Long id;

    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "词汇ID")
    private Integer wordId;

    @Column(name = "sense_id", nullable = false)
    @ApiModelProperty(value = "义项ID")
    private Integer senseId;

    @Column(name = "word", nullable = false, length = 32)
    @ApiModelProperty(value = "当前词汇")
    private String word;

    @Column(name = "relation_type", length = 32)
    @ApiModelProperty(value = "关联类型，参考枚举：VocabRelationTypeEnum")
    private String relationType;

    @Column(name = "relation_word_id", nullable = false)
    @ApiModelProperty(value = "关联词汇ID")
    private Long relationWordId;

    @Column(name = "relation_sense_id", nullable = false)
    @ApiModelProperty(value = "关联义项ID")
    private Long relationSenseId;

    @Column(name = "relation_word", nullable = false, length = 32)
    @ApiModelProperty(value = "关联词汇")
    private String relationWord;

    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（大在前）")
    private Integer relationOrder = 0;

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

- [ ] **Step 1: Write file**
- [ ] **Step 2: Commit** `git add ... && git commit -m "feat: add VocabRelation entity"`

---

### Task 2: 新建 VocabRelationDto

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabRelationDto.java`

```java
package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class VocabRelationDto implements Serializable {

    @ApiModelProperty(value = "关联ID")
    private Long id;
    @ApiModelProperty(value = "词汇ID")
    private Integer wordId;
    @ApiModelProperty(value = "义项ID")
    private Integer senseId;
    @ApiModelProperty(value = "当前词汇")
    private String word;
    @ApiModelProperty(value = "关联类型")
    private String relationType;
    @ApiModelProperty(value = "关联词汇ID")
    private Long relationWordId;
    @ApiModelProperty(value = "关联义项ID")
    private Long relationSenseId;
    @ApiModelProperty(value = "关联词汇")
    private String relationWord;
    @ApiModelProperty(value = "排序权重")
    private Integer order;
    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;
    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 1: Write file**
- [ ] **Step 2: Commit**

---

### Task 3: 新建 VocabRelationRepository

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabRelationRepository.java`

```java
package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabRelationRepository extends JpaRepository<VocabRelation, Long> {
    List<VocabRelation> findBySenseIdAndStatus(Integer senseId, Integer status);
    List<VocabRelation> findBySenseIdInAndStatus(List<Integer> senseIds, Integer status);
}
```

- [ ] **Step 1: Write file**
- [ ] **Step 2: Commit**

---

### Task 4: 修改 VocabSense 实体

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabSense.java`

**变化：**
```
translations → defTranslations (column: def_translations)
defImage     → defImageId (column: def_image_id)
senseOrder   → 保持字段名 senseOrder, column 改为 "order"
删除: synonyms, antonyms, relatedForward, relatedBackward, relatedOther
```

关键代码片段：
```java
    @Column(name = "def_image_id")                    // was: def_image
    @ApiModelProperty(value = "中文释义图片资源ID")
    private Long defImageId;                           // was: defImage

    @Column(name = "def_translations", columnDefinition = "text")  // was: translations
    @ApiModelProperty(value = "外文翻译列表")
    private String defTranslations;                    // was: translations

    // 删除以下字段：synonyms, antonyms, relatedForward, relatedBackward, relatedOther

    @NotNull
    @Column(name = "`order`", nullable = false)       // was: sense_order
    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder = 0;
```

- [ ] **Step 1: 修改字段映射**
- [ ] **Step 2: Commit**

---

### Task 5: 修改 VocabStructure 实体

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabStructure.java`

唯一变化：`structureOrder` 的 column 改为 `` `order` ``。

```java
    @NotNull
    @Column(name = "`order`", nullable = false)   // was: structure_order
    private Integer structureOrder = 0;
```

- [ ] **Step 1: 修改列映射**
- [ ] **Step 2: Commit**

---

### Task 6: 删除 VocabExample / VocabExercise 实体

**Files:**
- Delete: `domain/vocabulary/VocabExample.java`, `domain/vocabulary/VocabExercise.java`
- Delete: `service/vocabulary/dto/VocabExampleDto.java`, `service/vocabulary/dto/VocabExerciseDto.java`
- Delete: `repo/vocabulary/VocabExampleRepository.java`, `repo/vocabulary/VocabExerciseRepository.java`

```bash
git rm grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabExample.java \
       grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabExercise.java \
       grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExampleDto.java \
       grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExerciseDto.java \
       grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabExampleRepository.java \
       grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabExerciseRepository.java
git commit -m "refactor: remove VocabExample and VocabExercise entities, DTOs, repos"
```

- [ ] **Step 1: 删除 6 个文件并提交**

---

### Task 7: 修改 VocabSenseDto

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java`

完整替换：
```java
package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabSenseDto extends BaseDTO implements Serializable {
    private Integer id;
    private Integer wordId;
    private String partOfSpeech;
    private String chineseDef;
    private Long defAudioId;
    private Long defImageId;
    private List<TextTranslation> defTranslations;
    private ExampleSentenceDto defImageSentence;
    private List<VocabRelationDto> synonymWords;
    private List<VocabRelationDto> antonymWords;
    private List<VocabRelationDto> sequentialWords;
    private List<VocabRelationDto> reverseSequentialWords;
    private List<VocabRelationDto> jumbledWords;
    private Integer senseOrder;
    private List<VocabStructureDto> structures;
    private Integer status;
}
```

- [ ] **Step 1: 写入新内容**
- [ ] **Step 2: Commit**

---

### Task 8: 修改 VocabStructureDto

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabStructureDto.java`

```java
package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabStructureDto extends BaseDTO implements Serializable {
    private Integer id;
    private Integer wordId;
    private Integer senseId;
    private String pattern;
    private String patternDef;
    private List<TextTranslation> patternDefTranslations;
    private Integer structureOrder;
    private List<ExampleSentenceDto> structureSentences;   // was: List<VocabExampleDto> examples
    private Integer status;
}
```

- [ ] **Step 1: 写入新内容**
- [ ] **Step 2: Commit**

---

### Task 9: 修改 VocabWordDto 和 VocabWordDraftDto

**Files:**
- Modify: `service/vocabulary/dto/VocabWordDto.java`
- Modify: `service/vocabulary/dto/VocabWordDraftDto.java`

删除 `exercises` 字段（及其 import）。

- [ ] **Step 1: 两个文件删除 exercises**
- [ ] **Step 2: Commit**

---

### Task 10: 重写 VocabWordWrapper

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java`

完整替换，详见最终代码。核心变化：
- 删除所有 exercise/example 相关方法
- 新增 `VocabRelationRequest ↔ VocabRelationDto` 和 `ExampleSentenceRequest ↔ ExampleSentenceDto` 的转换
- `toSenseDto` 中: 设置 `defImageId`、`defTranslations`、`defImageSentence`、5 种 relation 列表、`senseOrder`
- `toStructureDto` 中: 设置 `structureSentences`
- VO 方向同理

注意字段名映射（各层保持一致）：
- Request/VO: `defImageId` → Dto/Entity: `defImageId`
- Request/VO: `order` → Dto: `senseOrder` / `structureOrder` (camelCase 字段名与 column `` `order` `` 区分)
- Dto: `senseOrder` / `structureOrder` → VO: `order`

- [ ] **Step 1: 全量替换 Wrapper 文件**
- [ ] **Step 2: Commit**

---

### Task 11: 清理 VocabWordController 的引用

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

删除以下已不再需要的 import：
```
import com.naon.grid.backend.rest.request.ExerciseOptionRequest;
import com.naon.grid.backend.rest.vo.ExerciseOptionVO;
import com.naon.grid.backend.rest.vo.VocabOutlineRecordVO;
import com.naon.grid.domain.common.ExerciseOption;
import com.naon.grid.backend.service.vocabulary.dto.VocabExampleDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabExerciseDto;
```

- [ ] **Step 1: 清理 import**
- [ ] **Step 2: Commit**

---

### Task 12: 重写 VocabWordServiceImpl（核心）

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

这是最复杂的任务，按子步骤拆解：

#### 12.1 替换注入和常量

```java
// 删除：
private final VocabExampleRepository vocabExampleRepository;
private final VocabExerciseRepository vocabExerciseRepository;

// 新增：
private final ExampleSentenceService exampleSentenceService;
private final ExampleSentenceRepository exampleSentenceRepository;
private final VocabRelationRepository vocabRelationRepository;

// 常量
private static final String DEF_IMAGE_SENTENCE_BIZ = SentenceBizTypeEnum.VOCAB_SENSE_DEF_IMAGE_SENTENCE.getCode();
private static final String STRUCTURE_SENTENCE_BIZ = SentenceBizTypeEnum.VOCAB_SENSE_STRUCTURE_SENTENCE.getCode();
```

#### 12.2 删除旧 convert/update 方法

删除以下方法及其所有调用：
- `convertToExampleDto` / `updateExample` / `convertToExampleEntity`
- `convertToExerciseDto` / `updateExercise` / `convertToExerciseEntity`
- `syncExercises`（及所有调用）
- `saveChildren` / `deleteChildren`

#### 12.3 重写 convertToSenseDto

```java
private VocabSenseDto convertToSenseDto(VocabSense sense) {
    VocabSenseDto dto = new VocabSenseDto();
    dto.setId(sense.getId());
    dto.setWordId(sense.getWordId());
    dto.setPartOfSpeech(sense.getPartOfSpeech());
    dto.setChineseDef(sense.getChineseDef());
    dto.setDefAudioId(sense.getDefAudioId());
    dto.setDefImageId(sense.getDefImageId());
    dto.setDefTranslations(JsonUtils.parseTranslationList(sense.getDefTranslations()));
    dto.setSenseOrder(sense.getSenseOrder());
    dto.setCreateTime(sense.getCreateTime());
    dto.setUpdateTime(sense.getUpdateTime());
    dto.setStatus(sense.getStatus());

    // 释义图片例句
    dto.setDefImageSentence(exampleSentenceService.findOne(
            DEF_IMAGE_SENTENCE_BIZ, sense.getId().longValue()));

    // 关联词汇（按类型分组）
    List<VocabRelation> relations = vocabRelationRepository
            .findBySenseIdAndStatus(sense.getId(), StatusEnum.ENABLED.getCode());
    Map<String, List<VocabRelationDto>> grouped = new HashMap<>();
    for (VocabRelation r : relations) {
        grouped.computeIfAbsent(r.getRelationType(), k -> new ArrayList<>())
               .add(toVocabRelationDto(r));
    }
    dto.setSynonymWords(grouped.getOrDefault("SYNONYMS_WORDS", Collections.emptyList()));
    dto.setAntonymWords(grouped.getOrDefault("ANTONYMS", Collections.emptyList()));
    dto.setSequentialWords(grouped.getOrDefault("SEQUENTIAL_WORDS", Collections.emptyList()));
    dto.setReverseSequentialWords(grouped.getOrDefault("REVERSE_SEQUENTIAL_WORDS", Collections.emptyList()));
    dto.setJumbledWords(grouped.getOrDefault("JUMBLED_WORDS", Collections.emptyList()));

    // 结构（含例句）
    List<VocabStructure> structures = vocabStructureRepository
            .findBySenseIdAndStatus(sense.getId(), StatusEnum.ENABLED.getCode());
    dto.setStructures(batchConvertStructureDto(structures));

    return dto;
}
```

#### 12.4 批量转换结构（含例句批量查询）

```java
private List<VocabStructureDto> batchConvertStructureDto(List<VocabStructure> structures) {
    if (structures == null || structures.isEmpty()) return Collections.emptyList();

    List<VocabStructureDto> dtos = new ArrayList<>();
    for (VocabStructure s : structures) {
        VocabStructureDto d = new VocabStructureDto();
        d.setId(s.getId());
        d.setWordId(s.getWordId());
        d.setSenseId(s.getSenseId());
        d.setPattern(s.getPattern());
        d.setPatternDef(s.getPatternDef());
        d.setPatternDefTranslations(JsonUtils.parseTranslationList(s.getPatternDefTranslations()));
        d.setStructureOrder(s.getStructureOrder());
        d.setCreateTime(s.getCreateTime());
        d.setUpdateTime(s.getUpdateTime());
        d.setStatus(s.getStatus());
        dtos.add(d);
    }

    // 批量加载结构例句
    List<Long> structureIds = dtos.stream().map(s -> s.getId().longValue()).collect(Collectors.toList());
    List<ExampleSentence> allSentences = exampleSentenceRepository
            .findByBizTypeAndBizIdInAndStatus(STRUCTURE_SENTENCE_BIZ, structureIds, StatusEnum.ENABLED.getCode());

    Map<Long, List<ExampleSentenceDto>> sentenceMap = new HashMap<>();
    for (ExampleSentence s : allSentences) {
        sentenceMap.computeIfAbsent(s.getBizId(), k -> new ArrayList<>())
                   .add(toExampleSentenceDto(s));
    }

    for (VocabStructureDto dto : dtos) {
        dto.setStructureSentences(sentenceMap.getOrDefault(dto.getId().longValue(), Collections.emptyList()));
    }

    return dtos;
}
```

#### 12.5 新增 VocabRelation → VocabRelationDto 转换

```java
private VocabRelationDto toVocabRelationDto(VocabRelation entity) {
    VocabRelationDto dto = new VocabRelationDto();
    dto.setId(entity.getId());
    dto.setWordId(entity.getWordId());
    dto.setSenseId(entity.getSenseId());
    dto.setWord(entity.getWord());
    dto.setRelationType(entity.getRelationType());
    dto.setRelationWordId(entity.getRelationWordId());
    dto.setRelationSenseId(entity.getRelationSenseId());
    dto.setRelationWord(entity.getRelationWord());
    dto.setOrder(entity.getRelationOrder());
    dto.setCreateTime(entity.getCreateTime());
    dto.setUpdateTime(entity.getUpdateTime());
    dto.setStatus(entity.getStatus());
    return dto;
}
```

#### 12.6 新增 ExampleSentence → ExampleSentenceDto 转换

```java
private ExampleSentenceDto toExampleSentenceDto(ExampleSentence entity) {
    if (entity == null) return null;
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
```

#### 12.7 重写 syncSenses（去除旧字段，增加 relation 同步 + 例句同步）

```java
private void syncSenses(Integer wordId, String word, List<VocabSenseDto> submittedDtos) {
    List<VocabSenseDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
    List<VocabSense> existing = vocabSenseRepository.findByWordIdAndStatus(wordId, StatusEnum.ENABLED.getCode());
    Map<Integer, VocabSense> existingMap = new HashMap<>();
    for (VocabSense s : existing) existingMap.put(s.getId(), s);

    Set<Integer> submittedIds = new HashSet<>();

    for (VocabSenseDto dto : submitted) {
        if (dto.getId() != null && !submittedIds.add(dto.getId())) {
            throw new BadRequestException("义项ID重复: " + dto.getId());
        }
    }

    for (VocabSenseDto dto : submitted) {
        if (dto.getId() == null) {
            VocabSense sense = convertToSenseEntity(dto, wordId);
            sense = vocabSenseRepository.save(sense);
            syncStructures(wordId, sense.getId(), dto.getStructures());
            syncRelations(wordId, sense.getId(), word, dto);
            syncDefImageSentence(sense.getId(), dto.getDefImageSentence());
        } else {
            VocabSense sense = existingMap.get(dto.getId());
            if (sense == null) throw new BadRequestException("义项ID不属于当前词汇: " + dto.getId());
            updateSense(sense, dto);
            vocabSenseRepository.save(sense);
            syncStructures(wordId, sense.getId(), dto.getStructures());
            syncRelations(wordId, sense.getId(), word, dto);
            syncDefImageSentence(sense.getId(), dto.getDefImageSentence());
        }
    }

    // 软删除被移除的义项
    for (VocabSense sense : existing) {
        if (!submittedIds.contains(sense.getId())) {
            // 先删除关联数据
            disableRelationsBySenseId(sense.getId());
            exampleSentenceService.disableByBizIds(DEF_IMAGE_SENTENCE_BIZ, Collections.singletonList(sense.getId().longValue()));
            // 删除结构及其例句
            List<VocabStructure> structures = vocabStructureRepository.findBySenseId(sense.getId());
            for (VocabStructure s : structures) {
                exampleSentenceService.disableByBizIds(STRUCTURE_SENTENCE_BIZ, Collections.singletonList(s.getId().longValue()));
                s.setStatus(StatusEnum.DISABLED.getCode());
                vocabStructureRepository.save(s);
            }
            sense.setStatus(StatusEnum.DISABLED.getCode());
            vocabSenseRepository.save(sense);
        }
    }
}
```

#### 12.8 重写 syncStructures（使用 ExampleSentenceService）

```java
private void syncStructures(Integer wordId, Integer senseId, List<VocabStructureDto> submittedDtos) {
    List<VocabStructureDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
    List<VocabStructure> existing = vocabStructureRepository.findBySenseIdAndStatus(senseId, StatusEnum.ENABLED.getCode());
    Map<Integer, VocabStructure> existingMap = new HashMap<>();
    for (VocabStructure s : existing) existingMap.put(s.getId(), s);

    Set<Integer> submittedIds = new HashSet<>();
    for (VocabStructureDto dto : submitted) {
        if (dto.getId() != null && !submittedIds.add(dto.getId())) {
            throw new BadRequestException("搭配ID重复: " + dto.getId());
        }
    }

    for (VocabStructureDto dto : submitted) {
        Integer structureId;
        if (dto.getId() == null) {
            VocabStructure structure = convertToStructureEntity(dto, wordId, senseId);
            structure = vocabStructureRepository.save(structure);
            structureId = structure.getId();
        } else {
            VocabStructure structure = existingMap.get(dto.getId());
            if (structure == null) throw new BadRequestException("搭配ID不属于当前义项: " + dto.getId());
            updateStructure(structure, dto);
            vocabStructureRepository.save(structure);
            structureId = structure.getId();
        }
        // 同步结构例句
        syncStructureSentences(structureId.longValue(), dto.getStructureSentences());
    }

    // 软删除被移除的结构及其例句
    for (VocabStructure s : existing) {
        if (!submittedIds.contains(s.getId())) {
            exampleSentenceService.disableByBizIds(STRUCTURE_SENTENCE_BIZ,
                    Collections.singletonList(s.getId().longValue()));
            s.setStatus(StatusEnum.DISABLED.getCode());
            vocabStructureRepository.save(s);
        }
    }
}
```

#### 12.9 新增 syncStructureSentences

```java
/**
 * 同步一个结构的多条例句。
 * 先软删除该结构所有旧例句，再逐个新增/更新。
 */
private void syncStructureSentences(Long structureId, List<ExampleSentenceDto> sentenceDtos) {
    // 先软删除旧的
    exampleSentenceService.disableByBizIds(STRUCTURE_SENTENCE_BIZ, Collections.singletonList(structureId));

    if (sentenceDtos == null || sentenceDtos.isEmpty()) return;

    List<ExampleSentence> toSave = new ArrayList<>();
    for (ExampleSentenceDto dto : sentenceDtos) {
        ExampleSentence entity;
        if (dto.getId() != null) {
            entity = exampleSentenceRepository.findById(dto.getId()).orElse(null);
            if (entity == null) continue;
        } else {
            entity = new ExampleSentence();
        }
        entity.setBizType(STRUCTURE_SENTENCE_BIZ);
        entity.setBizId(structureId);
        entity.setSentence(dto.getSentence());
        entity.setPinyin(dto.getPinyin());
        entity.setAudioId(dto.getAudioId());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setImageId(dto.getImageId());
        entity.setSentenceOrder(dto.getOrder() != null ? dto.getOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        toSave.add(entity);
    }
    exampleSentenceRepository.saveAll(toSave);
}
```

#### 12.10 新增 syncDefImageSentence

```java
private void syncDefImageSentence(Integer senseId, ExampleSentenceDto dto) {
    exampleSentenceService.syncOne(DEF_IMAGE_SENTENCE_BIZ, senseId.longValue(), dto);
}
```

#### 12.11 新增 syncRelations

```java
private void syncRelations(Integer wordId, Integer senseId, String word, VocabSenseDto dto) {
    // 先软删除该义项的所有旧关联
    disableRelationsBySenseId(senseId);

    // 批量新增
    List<VocabRelation> toSave = new ArrayList<>();
    toSave.addAll(buildRelations(wordId, senseId, word, VocabRelationTypeEnum.SYNONYMS_WORDS.getCode(), dto.getSynonymWords()));
    toSave.addAll(buildRelations(wordId, senseId, word, VocabRelationTypeEnum.ANTONYMS_WORDS.getCode(), dto.getAntonymWords()));
    toSave.addAll(buildRelations(wordId, senseId, word, VocabRelationTypeEnum.SEQUENTIAL_WORDS.getCode(), dto.getSequentialWords()));
    toSave.addAll(buildRelations(wordId, senseId, word, VocabRelationTypeEnum.REVERSE_SEQUENTIAL_WORDS.getCode(), dto.getReverseSequentialWords()));
    toSave.addAll(buildRelations(wordId, senseId, word, VocabRelationTypeEnum.JUMBLED_WORDS.getCode(), dto.getJumbledWords()));

    if (!toSave.isEmpty()) {
        vocabRelationRepository.saveAll(toSave);
    }
}

private List<VocabRelation> buildRelations(Integer wordId, Integer senseId, String word, String type, List<VocabRelationDto> dtos) {
    if (dtos == null || dtos.isEmpty()) return Collections.emptyList();
    List<VocabRelation> list = new ArrayList<>();
    for (VocabRelationDto r : dtos) {
        VocabRelation entity = new VocabRelation();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setWord(word);
        entity.setRelationType(type);
        entity.setRelationWordId(r.getRelationWordId());
        entity.setRelationSenseId(r.getRelationSenseId() != null ? r.getRelationSenseId() : 0L);
        entity.setRelationWord(r.getRelationWord());
        entity.setRelationOrder(r.getOrder() != null ? r.getOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        list.add(entity);
    }
    return list;
}

private void disableRelationsBySenseId(Integer senseId) {
    List<VocabRelation> existing = vocabRelationRepository.findBySenseIdAndStatus(senseId, StatusEnum.ENABLED.getCode());
    for (VocabRelation r : existing) {
        r.setStatus(StatusEnum.DISABLED.getCode());
        vocabRelationRepository.save(r);
    }
}
```

#### 12.12 修改 convertToSenseEntity 和 updateSense

```java
private VocabSense convertToSenseEntity(VocabSenseDto dto, Integer wordId) {
    VocabSense entity = new VocabSense();
    entity.setWordId(wordId);
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setChineseDef(dto.getChineseDef());
    entity.setDefAudioId(dto.getDefAudioId());
    entity.setDefImageId(dto.getDefImageId());
    entity.setDefTranslations(JsonUtils.toTranslationJson(dto.getDefTranslations()));
    entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
    entity.setStatus(StatusEnum.ENABLED.getCode());
    return entity;
}

private void updateSense(VocabSense entity, VocabSenseDto dto) {
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setChineseDef(dto.getChineseDef());
    entity.setDefAudioId(dto.getDefAudioId());
    entity.setDefImageId(dto.getDefImageId());
    entity.setDefTranslations(JsonUtils.toTranslationJson(dto.getDefTranslations()));
    entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
}
```

#### 12.13 修改 publishDraft

```java
public void publishDraft(Integer id) {
    // ... 前面校验逻辑不变 ...
    VocabWordDto draftDto = JsonUtils.fromJson(vocabWord.getDraftContent(), VocabWordDto.class);

    // 更新主表
    vocabWord.setWordTraditional(draftDto.getWordTraditional());
    vocabWord.setPinyin(draftDto.getPinyin());
    vocabWord.setAudioId(draftDto.getAudioId());
    vocabWord.setHskLevel(draftDto.getHskLevel());

    // 更新子表（注意：需要传入 word 文本用于 vocab_relation）
    syncSenses(id, draftDto.getWord(), draftDto.getSenses());

    // 更新状态
    vocabWord.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
    vocabWord.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
    vocabWord.setDraftContent(null);
    vocabWordRepository.save(vocabWord);
}
```

#### 12.14 修改 findById 和 findPublishedById

从两个方法中删除以下代码：
```java
// 删除：
List<VocabExerciseDto> exerciseDtos = ...;  // 整个 exercise 读取块
```

其他保持不变（已经由 convertToSenseDto 内部处理了新的数据加载）。

#### 12.15 修改 applyDraftOverlay（列表页草稿覆盖）

当前方法覆盖 `word`、`wordTraditional`、`pinyin`、`audioId`、`hskLevel`，这些保持不变。

- [ ] **Step 1: 替换注入**
- [ ] **Step 2: 删除旧 convert/update 方法**
- [ ] **Step 3: 重写 convertToSenseDto + batchConvertStructureDto**
- [ ] **Step 4: 新增 toVocabRelationDto / toExampleSentenceDto 辅助方法**
- [ ] **Step 5: 重写 syncSenses（含 syncRelations + syncDefImageSentence 调用）**
- [ ] **Step 6: 重写 syncStructures（含 syncStructureSentences 调用）**
- [ ] **Step 7: 新增 syncRelations / syncStructureSentences / syncDefImageSentence 方法**
- [ ] **Step 8: 修改 convertToSenseEntity / updateSense**
- [ ] **Step 9: 修改 publishDraft（删 syncExercises，传 word 参数）**
- [ ] **Step 10: 修改 findById / findPublishedById（删 exercise 加载）**
- [ ] **Step 11: 编译项目验证 `mvn compile -pl grid-system -am`**
- [ ] **Step 12: Commit**

---

### Task 13: 编译验证

```bash
# 编译 grid-system 及其依赖
cd C:\Users\nano\Desktop\nano-gemini
mvn compile -pl grid-system -am -DskipTests
```

- [ ] **Step 1: 运行编译**
- [ ] **Step 2: 修复编译错误（如有）**
- [ ] **Step 3: 最终整体提交**

```
git add -A
git commit -m "refactor: complete vocabulary table refactor

- Migrate examples from vocab_example to shared example_sentence table
- Migrate word relations from VocabSense JSON columns to vocab_relation table
- Remove VocabExercise-related code (postponed)
- Update domain entities, DTOs, Wrapper, and Service layer"
```
