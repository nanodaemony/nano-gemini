# ExampleSentence 多态关联重构 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `example_sentence` 表的 `biz_type` + `biz_id` 多态关联模式改为业务表直接持有 FK 的规范设计。

**Architecture:** 1:1 关系（char_word→sentence, vocab_sense→def_image_sentence）在业务表新增 FK 列；1:N 关系（vocab_structure→sentences）在 example_sentence 表新增 `structure_id` 列。`VOCAB_COMPARISON_CHAT` 保持已有 `example_sentence_id` 字段，但不再通过 biz_type 查询。

**Tech Stack:** Spring Boot 2.7, Spring Data JPA, MySQL, Fastjson2

**涉及文件总览：**

| 类别 | 修改 | 变更说明 |
|------|------|----------|
| SQL | 3 | biz_common.sql / biz_character.sql / biz_vocabulary.sql |
| Java Entity | 3 | ExampleSentence / CharWord / VocabSense |
| Repository | 1 | ExampleSentenceRepository |
| Service Interface | 1 | ExampleSentenceService |
| Service Impl | 3 | ExampleSentenceServiceImpl / CharCharacterServiceImpl / VocabWordServiceImpl / VocabComparisonGroupServiceImpl（简化） |
| DTO | 2 | ExampleSentenceDto / CharWordDto / VocabSenseDto |
| Enum | 1 | SentenceBizTypeEnum |
| Test | 1 | ExampleSentenceServiceImplTest |
| **合计** | **14+** | |

---

### Task 1: 修改 SQL 表定义

**文件:**
- Modify: `sql/biz_common.sql` — example_sentence 表结构调整
- Modify: `sql/biz_character.sql` — char_word 新增 sentence_id
- Modify: `sql/biz_vocabulary.sql` — vocab_sense 新增 def_image_sentence_id

- [ ] **Step 1: 修改 `sql/biz_common.sql` — example_sentence 去掉 biz_type/biz_id，新增 structure_id**

将原表定义：
```sql
CREATE TABLE `example_sentence`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '例句ID',
  `biz_type` varchar(64) NOT NULL COMMENT '例句业务类型, 参考枚举：SentenceBizTypeEnum',
  `biz_id` bigint(20) NOT NULL COMMENT '例句业务ID',
  `sentence` varchar(1024) NOT NULL COMMENT '例句中文文案',
  `pinyin` varchar(2048) NULL DEFAULT NULL COMMENT '例句拼音',
  `audio_id` bigint NULL DEFAULT NULL COMMENT '例句音频资源ID',
  `translations` text NULL COMMENT '例句外文翻译列表',
  `image_id` bigint NULL DEFAULT NULL COMMENT '例句图片(ID)',
  `order` int NOT NULL DEFAULT 0 COMMENT '例句排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bizType_bizId`(`biz_type`, `biz_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='例句文案表';
```

改为：
```sql
-- 例句文案表
-- 1:1 场景（char_word、vocab_sense、vocab_comparison_chat）由业务表的 FK 列引用；
-- 1:N 场景（vocab_structure）由 structure_id 列指向父表。
CREATE TABLE `example_sentence`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '例句ID',
  `structure_id` bigint DEFAULT NULL COMMENT '所属词汇搭配结构ID（1:N 场景，对应 vocab_structure.id）',
  `sentence` varchar(1024) NOT NULL COMMENT '例句中文文案',
  `pinyin` varchar(2048) NULL DEFAULT NULL COMMENT '例句拼音',
  `audio_id` bigint NULL DEFAULT NULL COMMENT '例句音频资源ID',
  `translations` text NULL COMMENT '例句外文翻译列表',
  `image_id` bigint NULL DEFAULT NULL COMMENT '例句图片(ID)',
  `order` int NOT NULL DEFAULT 0 COMMENT '例句排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_structure_id`(`structure_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='例句文案表';
```

- [ ] **Step 2: 修改 `sql/biz_character.sql` — char_word 加 sentence_id**

在 `char_word` 表的 `word_item_translations` 之后、`order` 之前新增一行：
```sql
  `sentence_id` bigint NULL DEFAULT NULL COMMENT '组词例句ID（对应 example_sentence.id）',
```

更新表的注释（第51行附近）：
```sql
-- 汉字组词表
-- 注：一个汉字可能有多个组词。组词的例句通过 sentence_id 关联 example_sentence 表。
```

- [ ] **Step 3: 修改 `sql/biz_vocabulary.sql` — vocab_sense 加 def_image_sentence_id**

在 `vocab_sense` 表的 `def_image_id` 之后新增一行：
```sql
  `def_image_sentence_id` bigint NULL DEFAULT NULL COMMENT '释义图片例句ID（对应 example_sentence.id）',
```

- [ ] **Step 4: 编译验证 SQL 无语法问题**（仅人工检查字段名、逗号正确性）

- [ ] **Step 5: Commit**

```bash
git add sql/biz_common.sql sql/biz_character.sql sql/biz_vocabulary.sql
git commit -m "refactor: remove biz_type/biz_id from example_sentence, add FK columns to business tables

- example_sentence: drop biz_type, biz_id; add structure_id for 1:N vocab_structure
- char_word: add sentence_id for 1:1 relationship
- vocab_sense: add def_image_sentence_id for 0..1 relationship

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: 清理 SentenceBizTypeEnum

**文件:**
- Modify: `grid-common/src/main/java/com/naon/grid/enums/SentenceBizTypeEnum.java`

- [ ] **Step 1: 删除不再使用的枚举值**

从枚举中删除所有 GRAMMAR_* 和 CHAR_WORD_SENTENCE / VOCAB_SENSE_DEF_IMAGE_SENTENCE / VOCAB_SENSE_STRUCTURE，只保留：
```java
@Getter
public enum SentenceBizTypeEnum {
    VOCAB_COMPARISON_CHAT("VOCAB_COMPARISON_CHAT", "词汇辨析情景对话（保留仅作标记，不再用于查询）"),
    ;
    private final String code;
    private final String description;
    SentenceBizTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-common/src/main/java/com/naon/grid/enums/SentenceBizTypeEnum.java
git commit -m "refactor: clean up SentenceBizTypeEnum, keep only VOCAB_COMPARISON_CHAT

All other biz types are replaced by direct FK columns in business tables.
Grammar-related values removed since not yet implemented — will use new pattern.

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: 修改 ExampleSentence 实体类

**文件:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/common/ExampleSentence.java`

- [ ] **Step 1: 替换字段**

删除以下两个字段及其 getter/setter 和 column 注解：
```java
// 删除
@Column(name = "biz_type", nullable = false, length = 64)
private String bizType;

@Column(name = "biz_id", nullable = false)
private Long bizId;
```

新增：
```java
@Column(name = "structure_id")
@ApiModelProperty(value = "所属词汇搭配结构ID")
private Long structureId;
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/common/ExampleSentence.java
git commit -m "refactor: update ExampleSentence entity - remove bizType/bizId, add structureId

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: 修改 ExampleSentenceDto

**文件:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/common/dto/ExampleSentenceDto.java`

- [ ] **Step 1: 替换字段**

```java
// 删除
@ApiModelProperty(value = "业务类型")
private String bizType;

@ApiModelProperty(value = "业务ID")
private Long bizId;

// 新增
@ApiModelProperty(value = "所属词汇搭配结构ID")
private Long structureId;
```

其他字段（id、sentence、pinyin、audioId、translations、imageId、order、createTime、updateTime、status）保持不变。

**备注：** 已有 draftContent JSON 中可能包含序列化的旧 dto（含 bizType/bizId），Fastjson2 反序列化时遇到不存在的字段默认忽略，不会报错。

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/common/dto/ExampleSentenceDto.java
git commit -m "refactor: update ExampleSentenceDto - remove bizType/bizId, add structureId

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: 修改 ExampleSentenceRepository

**文件:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/common/ExampleSentenceRepository.java`

- [ ] **Step 1: 替换查询方法**

```java
@Repository
public interface ExampleSentenceRepository extends JpaRepository<ExampleSentence, Long> {

    // 删除以下两个旧方法（暂时注释掉，等所有引用处改完再彻底删除）：
    // List<ExampleSentence> findByBizTypeAndBizIdAndStatus(String bizType, Long bizId, Integer status);
    // List<ExampleSentence> findByBizTypeAndBizIdInAndStatus(String bizType, Collection<Long> bizIds, Integer status);

    // --- 新增 ---

    /**
     * 查询某结构的所有启用例句（1:N vocab_structure 场景）
     */
    List<ExampleSentence> findByStructureIdAndStatus(Long structureId, Integer status);

    /**
     * 批量查询多个结构的所有启用例句
     */
    List<ExampleSentence> findByStructureIdInAndStatus(Collection<Long> structureIds, Integer status);
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/common/ExampleSentenceRepository.java
git commit -m "refactor: update ExampleSentenceRepository - replace bizType queries with structureId queries

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: 重写 ExampleSentenceService 接口和实现

**文件:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/common/ExampleSentenceService.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImpl.java`

- [ ] **Step 1: 重写 Service 接口**

```java
package com.naon.grid.backend.service.common;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ExampleSentenceService {

    /** 根据 ID 查询单条例句（仅返回 status=ENABLED 的） */
    ExampleSentenceDto findById(Long id);

    /** 批量查询例句（按 ID），返回 Map<id, Dto>，仅包含 ENABLED */
    Map<Long, ExampleSentenceDto> findByIds(Collection<Long> ids);

    /** 查询某 structure 的所有启用例句，按 order 降序 */
    List<ExampleSentenceDto> findByStructureId(Long structureId);

    /** 批量查询多个 structure 的例句，返回 Map<structureId, List<Dto>> */
    Map<Long, List<ExampleSentenceDto>> findByStructureIds(Collection<Long> structureIds);

    /** 创建或更新一条例句（id=null 新增，id!=null 更新） */
    ExampleSentenceDto save(ExampleSentenceDto dto);

    /** 软删除一条例句 */
    void disableById(Long id);

    /** 批量软删除 */
    void disableByIds(Collection<Long> ids);

    /** 软删除某 structure 的所有例句 */
    void disableByStructureId(Long structureId);

    /** 批量软删除多个 structure 的所有例句 */
    void disableByStructureIds(Collection<Long> structureIds);
}
```

- [ ] **Step 2: 重写 Service 实现**

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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExampleSentenceServiceImpl implements ExampleSentenceService {

    private final ExampleSentenceRepository exampleSentenceRepository;

    @Override
    public ExampleSentenceDto findById(Long id) {
        if (id == null) return null;
        return exampleSentenceRepository.findById(id)
                .filter(e -> StatusEnum.ENABLED.getCode().equals(e.getStatus()))
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public Map<Long, ExampleSentenceDto> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        List<ExampleSentence> all = exampleSentenceRepository.findAllById(ids);
        if (all == null || all.isEmpty()) return Collections.emptyMap();
        Map<Long, ExampleSentenceDto> result = new LinkedHashMap<>();
        for (ExampleSentence e : all) {
            if (StatusEnum.ENABLED.getCode().equals(e.getStatus())) {
                result.put(e.getId(), toDto(e));
            }
        }
        return result;
    }

    @Override
    public List<ExampleSentenceDto> findByStructureId(Long structureId) {
        if (structureId == null) return Collections.emptyList();
        List<ExampleSentence> sentences = exampleSentenceRepository
                .findByStructureIdAndStatus(structureId, StatusEnum.ENABLED.getCode());
        if (sentences == null || sentences.isEmpty()) return Collections.emptyList();
        sentences.sort(activeSentenceComparator());
        return sentences.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<ExampleSentenceDto>> findByStructureIds(Collection<Long> structureIds) {
        if (structureIds == null || structureIds.isEmpty()) return Collections.emptyMap();
        List<ExampleSentence> sentences = exampleSentenceRepository
                .findByStructureIdInAndStatus(structureIds, StatusEnum.ENABLED.getCode());
        if (sentences == null || sentences.isEmpty()) return Collections.emptyMap();
        Map<Long, List<ExampleSentenceDto>> result = new LinkedHashMap<>();
        for (ExampleSentence s : sentences) {
            result.computeIfAbsent(s.getStructureId(), k -> new ArrayList<>())
                  .add(toDto(s));
        }
        for (List<ExampleSentenceDto> list : result.values()) {
            list.sort(Comparator.comparing(ExampleSentenceDto::getOrder,
                    Comparator.nullsLast(Comparator.reverseOrder())));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExampleSentenceDto save(ExampleSentenceDto dto) {
        if (dto == null || StringUtils.isBlank(dto.getSentence())) {
            return null;
        }

        ExampleSentence entity;
        if (dto.getId() == null) {
            entity = new ExampleSentence();
        } else {
            entity = exampleSentenceRepository.findById(dto.getId())
                    .orElseThrow(() -> new BadRequestException("例句不存在: " + dto.getId()));
        }

        apply(entity, dto);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        entity = exampleSentenceRepository.save(entity);
        return toDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableById(Long id) {
        if (id == null) return;
        exampleSentenceRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(StatusEnum.DISABLED.getCode());
            exampleSentenceRepository.save(entity);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<ExampleSentence> existing = exampleSentenceRepository.findAllById(ids);
        if (existing == null || existing.isEmpty()) return;
        for (ExampleSentence e : existing) {
            if (StatusEnum.ENABLED.getCode().equals(e.getStatus())) {
                e.setStatus(StatusEnum.DISABLED.getCode());
            }
        }
        exampleSentenceRepository.saveAll(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableByStructureId(Long structureId) {
        if (structureId == null) return;
        List<ExampleSentence> existing = exampleSentenceRepository
                .findByStructureIdAndStatus(structureId, StatusEnum.ENABLED.getCode());
        if (existing == null || existing.isEmpty()) return;
        for (ExampleSentence e : existing) {
            e.setStatus(StatusEnum.DISABLED.getCode());
        }
        exampleSentenceRepository.saveAll(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableByStructureIds(Collection<Long> structureIds) {
        if (structureIds == null || structureIds.isEmpty()) return;
        List<ExampleSentence> existing = exampleSentenceRepository
                .findByStructureIdInAndStatus(structureIds, StatusEnum.ENABLED.getCode());
        if (existing == null || existing.isEmpty()) return;
        for (ExampleSentence e : existing) {
            e.setStatus(StatusEnum.DISABLED.getCode());
        }
        exampleSentenceRepository.saveAll(existing);
    }

    private void apply(ExampleSentence entity, ExampleSentenceDto dto) {
        entity.setSentence(dto.getSentence());
        entity.setPinyin(dto.getPinyin());
        entity.setAudioId(dto.getAudioId());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setImageId(dto.getImageId());
        entity.setSentenceOrder(dto.getOrder() != null ? dto.getOrder() : 0);
        if (dto.getStructureId() != null) {
            entity.setStructureId(dto.getStructureId());
        }
    }

    private ExampleSentenceDto toDto(ExampleSentence entity) {
        if (entity == null) return null;
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(entity.getId());
        dto.setStructureId(entity.getStructureId());
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

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/common/ExampleSentenceService.java
git add grid-system/src/main/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImpl.java
git commit -m "refactor: rewrite ExampleSentenceService - replace bizType operations with direct FK operations

- findOne(bizType, bizId) → findById(id)
- findByBizIds(bizType, ids) → findByIds(ids)
- syncOne(bizType, bizId, dto) → save(dto)
- disableByBizIds(bizType, ids) → disableByIds(ids)
- Add findByStructureId / findByStructureIds for 1:N vocab_structure case

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 7: 修改 CharWord 实体 + DTO + CharCharacterServiceImpl

**文件:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 1: CharWord 实体加 sentenceId**

在 `wordItemTranslations` 字段之后、`wordOrder` 之前新增：
```java
@Column(name = "sentence_id")
@ApiModelProperty(value = "组词例句ID（对应 example_sentence.id）")
private Long sentenceId;
```

- [ ] **Step 2: CharWordDto 加 sentenceId**

```java
@ApiModelProperty(value = "组词例句ID（对应 example_sentence.id）")
private Long sentenceId;
```

- [ ] **Step 3: CharCharacterServiceImpl 重写**

删除常量：
```java
// 删除整行
private static final String CHAR_WORD_SENTENCE_BIZ_TYPE = SentenceBizTypeEnum.CHAR_WORD_SENTENCE.getCode();
```

删除不再需要的 import：
```java
// 删除
import com.naon.grid.enums.SentenceBizTypeEnum;
```

重写 `hydrateWordSentences` 方法（约第 264-286 行）：
```java
private List<CharWordDto> hydrateWordSentences(List<CharWordDto> words) {
    if (words == null || words.isEmpty()) {
        return words;
    }
    List<Long> sentenceIds = words.stream()
            .map(CharWordDto::getSentenceId)
            .filter(id -> id != null)
            .collect(Collectors.toList());
    if (sentenceIds.isEmpty()) {
        return words;
    }
    Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(sentenceIds);
    if (sentenceMap == null || sentenceMap.isEmpty()) {
        return words;
    }
    for (CharWordDto word : words) {
        if (word.getSentenceId() != null) {
            word.setWordItemSentence(sentenceMap.get(word.getSentenceId()));
        }
    }
    return words;
}
```

重写 `syncWordSentences` 方法（约第 448-465 行）：
```java
private void syncWordSentences(List<CharWord> savedWords, List<CharWordDto> submittedDtos) {
    if (savedWords == null || savedWords.isEmpty() || submittedDtos == null || submittedDtos.isEmpty()) {
        return;
    }
    int pairCount = Math.min(savedWords.size(), submittedDtos.size());
    for (int i = 0; i < pairCount; i++) {
        CharWord savedWord = savedWords.get(i);
        CharWordDto submittedDto = submittedDtos.get(i);
        if (savedWord == null || savedWord.getId() == null || submittedDto == null) {
            continue;
        }
        ExampleSentenceDto sentenceDto = submittedDto.getWordItemSentence();
        if (sentenceDto != null && StringUtils.isNotBlank(sentenceDto.getSentence())) {
            // 如果有旧 sentenceId，用旧 id 去更新
            if (savedWord.getSentenceId() != null) {
                sentenceDto.setId(savedWord.getSentenceId());
            }
            ExampleSentenceDto saved = exampleSentenceService.save(sentenceDto);
            if (saved != null && saved.getId() != null) {
                savedWord.setSentenceId(saved.getId());
            }
        } else {
            // 没有例句：禁用旧的，清空 sentenceId
            if (savedWord.getSentenceId() != null) {
                exampleSentenceService.disableById(savedWord.getSentenceId());
                savedWord.setSentenceId(null);
            }
        }
    }
}
```

重写 `disableWordSentences` 方法（约第 434-446 行）：
```java
private void disableWordSentences(Collection<CharWord> words) {
    if (words == null || words.isEmpty()) {
        return;
    }
    List<Long> sentenceIds = words.stream()
            .map(CharWord::getSentenceId)
            .filter(id -> id != null)
            .collect(Collectors.toList());
    if (!sentenceIds.isEmpty()) {
        exampleSentenceService.disableByIds(sentenceIds);
    }
}
```

在 `convertToWordDto` 方法中（约第 500-514 行），在 `dto.setWordOrder(...)` 之前新增：
```java
dto.setSentenceId(word.getSentenceId());
```

注意：需要新增 import `com.naon.grid.utils.StringUtils`（若尚未导入）。

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java
git add grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java
git commit -m "refactor: update char_word to use direct sentenceId FK instead of biz_type

- CharWord: add sentenceId field
- CharWordDto: add sentenceId field
- CharCharacterServiceImpl: replace bizType-based example_sentence operations
  with direct FK read/write via exampleSentenceService.save()/findByIds()/disableByIds()

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 8: 修改 VocabSense 实体 + DTO + VocabWordServiceImpl

**文件:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabSense.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

- [ ] **Step 1: VocabSense 实体加 defImageSentenceId**

在 `defImageId` 字段之后新增：
```java
@Column(name = "def_image_sentence_id")
@ApiModelProperty(value = "释义图片例句ID（对应 example_sentence.id）")
private Long defImageSentenceId;
```

- [ ] **Step 2: VocabSenseDto 加 defImageSentenceId**

```java
@ApiModelProperty(value = "释义图片例句ID（对应 example_sentence.id）")
private Long defImageSentenceId;
```
`defImageSentence` (ExampleSentenceDto) 字段保留，用于 VO 展示和 draft JSON 兼容。

- [ ] **Step 3: VocabWordServiceImpl 重写**

删除两个常量：
```java
// 删除
private static final String DEF_IMAGE_SENTENCE_BIZ = SentenceBizTypeEnum.VOCAB_SENSE_DEF_IMAGE_SENTENCE.getCode();
private static final String STRUCTURE_SENTENCE_BIZ = SentenceBizTypeEnum.VOCAB_SENSE_STRUCTURE_SENTENCE.getCode();
```

删除 `ExampleSentenceRepository` 的注入（如果不再直接使用）。检查 —— `syncStructureSentences` 中不再直接操作 `exampleSentenceRepository`，可以删除：
```java
// 删除（如果不再使用）
private final ExampleSentenceRepository exampleSentenceRepository;
```
并删除对应的构造参数和 import。

**如果 `ExampleSentenceRepository` 在别处还被使用，则保留。**

重写 `syncDefImageSentence` 方法（约第 438-440 行）：
```java
private void syncDefImageSentence(Integer senseId, ExampleSentenceDto dto) {
    if (senseId == null) return;
    VocabSense sense = vocabSenseRepository.findById(senseId).orElse(null);
    if (sense == null) return;

    if (dto != null && StringUtils.isNotBlank(dto.getSentence())) {
        // 如果有旧 sentence，用旧 id 去更新
        if (sense.getDefImageSentenceId() != null) {
            dto.setId(sense.getDefImageSentenceId());
        }
        ExampleSentenceDto saved = exampleSentenceService.save(dto);
        if (saved != null && saved.getId() != null
                && !saved.getId().equals(sense.getDefImageSentenceId())) {
            sense.setDefImageSentenceId(saved.getId());
            vocabSenseRepository.save(sense);
        }
    } else {
        // 没有例句：禁用旧的，清空
        if (sense.getDefImageSentenceId() != null) {
            exampleSentenceService.disableById(sense.getDefImageSentenceId());
            sense.setDefImageSentenceId(null);
            vocabSenseRepository.save(sense);
        }
    }
}
```

重写 `syncStructureSentences` 方法（约第 407-436 行）：
```java
private void syncStructureSentences(Long structureId, List<ExampleSentenceDto> sentenceDtos) {
    if (structureId == null) return;

    // 先软删除该结构旧例句
    exampleSentenceService.disableByStructureId(structureId);

    if (sentenceDtos == null || sentenceDtos.isEmpty()) return;

    for (ExampleSentenceDto dto : sentenceDtos) {
        dto.setId(null); // 每次都新建
        dto.setStructureId(structureId);
        exampleSentenceService.save(dto);
    }
}
```

重写 `convertToSenseDto` 中的例句加载部分（约第 501-502 行）：
```java
// 原代码：
// dto.setDefImageSentence(exampleSentenceService.findOne(
//         DEF_IMAGE_SENTENCE_BIZ, sense.getId().longValue()));

// 改为：
if (sense.getDefImageSentenceId() != null) {
    dto.setDefImageSentence(exampleSentenceService.findById(sense.getDefImageSentenceId()));
}
```

在 `batchConvertStructureDto` 中重写例句批量加载部分（约第 546-558 行）：
```java
// 原代码：
// List<Long> structureIds = dtos.stream().map(s -> s.getId().longValue()).collect(Collectors.toList());
// List<ExampleSentence> allSentences = exampleSentenceRepository
//         .findByBizTypeAndBizIdInAndStatus(STRUCTURE_SENTENCE_BIZ, structureIds, StatusEnum.ENABLED.getCode());
// Map<Long, List<ExampleSentenceDto>> sentenceMap = new HashMap<>();
// for (ExampleSentence s : allSentences) {
//     sentenceMap.computeIfAbsent(s.getBizId(), k -> new ArrayList<>())
//                .add(toExampleSentenceDto(s));
// }

// 改为：
List<Long> structureIds = dtos.stream().map(s -> s.getId().longValue()).collect(Collectors.toList());
Map<Long, List<ExampleSentenceDto>> sentenceMap = exampleSentenceService.findByStructureIds(structureIds);
```

在 `syncSenses` 中删除被移除义项的处理处（约第 343 行），替换：
```java
// 原代码：
// exampleSentenceService.disableByBizIds(DEF_IMAGE_SENTENCE_BIZ,
//         Collections.singletonList(sense.getId().longValue()));

// 改为：
if (sense.getDefImageSentenceId() != null) {
    exampleSentenceService.disableById(sense.getDefImageSentenceId());
}
```

在 `syncSenses` 中删除结构例句的处理处（约第 348 行），替换：
```java
// 原代码：
// exampleSentenceService.disableByBizIds(STRUCTURE_SENTENCE_BIZ,
//         Collections.singletonList(s.getId().longValue()));

// 改为：
exampleSentenceService.disableByStructureId(s.getId().longValue());
```

在 `syncStructures` 中删除被移除结构的处理处（约第 399 行），替换：
```java
// 原代码：
// exampleSentenceService.disableByBizIds(STRUCTURE_SENTENCE_BIZ,
//         Collections.singletonList(structure.getId().longValue()));

// 改为：
exampleSentenceService.disableByStructureId(structure.getId().longValue());
```

删除 `toExampleSentenceDto` 私有方法（约第 580-596 行）—— 不再需要，因为不再直接使用 `ExampleSentenceRepository`。

删除 import：
```java
// 删除
import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.enums.SentenceBizTypeEnum;
```

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabSense.java
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "refactor: update vocab_sense/structure to use direct FK instead of biz_type

- VocabSense: add defImageSentenceId field
- VocabSenseDto: add defImageSentenceId field
- VocabWordServiceImpl: replace all bizType-based example_sentence operations
  with direct FK read/write for both def_image_sentence and structure_sentences

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 9: 简化 VocabComparisonGroupServiceImpl

**文件:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/impl/VocabComparisonGroupServiceImpl.java`

- [ ] **Step 1: 重写 loadChats**

```java
private List<VocabComparisonChatDto> loadChats(Long groupId) {
    List<VocabComparisonChat> chats = chatRepository.findByGroupIdAndStatus(
            groupId, StatusEnum.ENABLED.getCode());
    if (chats == null || chats.isEmpty()) {
        return Collections.emptyList();
    }

    List<VocabComparisonChatDto> dtos = chats.stream().map(this::toChatDto).collect(Collectors.toList());

    // 通过 chat.exampleSentenceId 直接批量加载例句
    List<Long> sentenceIds = dtos.stream()
            .map(VocabComparisonChatDto::getExampleSentenceId)
            .filter(id -> id != null)
            .collect(Collectors.toList());
    if (!sentenceIds.isEmpty()) {
        Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(sentenceIds);
        for (VocabComparisonChatDto dto : dtos) {
            if (dto.getExampleSentenceId() != null) {
                ExampleSentenceDto sentence = sentenceMap.get(dto.getExampleSentenceId());
                if (sentence != null) {
                    dto.setPinyin(sentence.getPinyin());
                    dto.setTranslations(sentence.getTranslations());
                    dto.setAudioId(sentence.getAudioId());
                }
            }
        }
    }

    return dtos;
}
```

- [ ] **Step 2: 重写 syncChats**

```java
private void syncChats(Long groupId, List<VocabComparisonChatDto> submittedDtos) {
    // 1. 软删除旧的 chats 及其例句
    List<VocabComparisonChat> existing = chatRepository.findByGroupIdAndStatus(
            groupId, StatusEnum.ENABLED.getCode());
    List<Long> oldSentenceIds = existing.stream()
            .map(VocabComparisonChat::getExampleSentenceId)
            .filter(id -> id != null)
            .collect(Collectors.toList());
    if (!existing.isEmpty()) {
        for (VocabComparisonChat chat : existing) {
            chat.setStatus(StatusEnum.DISABLED.getCode());
        }
        chatRepository.saveAll(existing);
    }
    if (!oldSentenceIds.isEmpty()) {
        exampleSentenceService.disableByIds(oldSentenceIds);
    }

    // 2. 创建新 chats
    if (submittedDtos == null || submittedDtos.isEmpty()) {
        return;
    }

    for (VocabComparisonChatDto dto : submittedDtos) {
        VocabComparisonChat chat = new VocabComparisonChat();
        chat.setGroupId(groupId);
        chat.setRole(dto.getRole());
        chat.setContent(dto.getContent());
        chat.setChatOrder(dto.getOrder() != null ? dto.getOrder() : 0);
        chat.setStatus(StatusEnum.ENABLED.getCode());
        chat = chatRepository.save(chat);

        // 创建例句（不设 bizType）
        ExampleSentenceDto sentenceDto = new ExampleSentenceDto();
        sentenceDto.setSentence(dto.getContent());
        sentenceDto.setPinyin(dto.getPinyin());
        sentenceDto.setAudioId(dto.getAudioId());
        sentenceDto.setTranslations(dto.getTranslations());
        sentenceDto.setOrder(dto.getOrder());

        ExampleSentenceDto savedSentence = exampleSentenceService.save(sentenceDto);
        if (savedSentence != null && savedSentence.getId() != null) {
            chat.setExampleSentenceId(savedSentence.getId());
            chatRepository.save(chat);
        }
    }
}
```

- [ ] **Step 3: 删除 COMPARISON_CHAT_BIZ 常量**

```java
// 删除整行
private static final String COMPARISON_CHAT_BIZ = SentenceBizTypeEnum.VOCAB_COMPARISON_CHAT.getCode();
```

删除不再需要的 import：
```java
// 删除
import com.naon.grid.enums.SentenceBizTypeEnum;
```

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/impl/VocabComparisonGroupServiceImpl.java
git commit -m "refactor: simplify VocabComparisonGroupServiceImpl to use direct sentence FK

- loadChats: load via chat.exampleSentenceId instead of bizType lookup
- syncChats: create sentence without bizType, backfill exampleSentenceId
- Remove COMPARISON_CHAT_BIZ constant

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 10: 重写 ExampleSentenceServiceImplTest

**文件:**
- Modify: `grid-system/src/test/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImplTest.java`

- [ ] **Step 1: 完全替换测试类内容**

```java
package com.naon.grid.backend.service.common.impl;

import com.naon.grid.backend.domain.common.ExampleSentence;
import com.naon.grid.backend.repo.common.ExampleSentenceRepository;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
    void saveCreatesNewSentenceWhenNoId() {
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

        when(repository.save(any(ExampleSentence.class))).thenAnswer(invocation -> {
            ExampleSentence e = invocation.getArgument(0);
            e.setId(99L);
            return e;
        });

        ExampleSentenceDto result = service.save(dto);

        assertNotNull(result);
        assertEquals(99L, result.getId().longValue());

        ArgumentCaptor<ExampleSentence> captor = ArgumentCaptor.forClass(ExampleSentence.class);
        verify(repository).save(captor.capture());
        ExampleSentence saved = captor.getValue();
        assertEquals("你好，我叫小明。", saved.getSentence());
        assertEquals("nǐ hǎo, wǒ jiào xiǎo míng.", saved.getPinyin());
        assertEquals(Long.valueOf(21L), saved.getAudioId());
        assertEquals(Long.valueOf(34L), saved.getImageId());
        assertEquals(Integer.valueOf(5), saved.getSentenceOrder());
        assertEquals(StatusEnum.ENABLED.getCode(), saved.getStatus());
        assertEquals("Hello, my name is Xiaoming.",
                JsonUtils.parseTranslationList(saved.getTranslations()).get(0).getTranslation());
    }

    @Test
    void saveUpdatesExistingSentenceWhenIdProvided() {
        ExampleSentence existing = new ExampleSentence();
        existing.setId(88L);
        existing.setSentence("旧例句");
        existing.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(88L);
        dto.setSentence("新例句");

        when(repository.findById(88L)).thenReturn(Optional.of(existing));
        when(repository.save(any(ExampleSentence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExampleSentenceDto result = service.save(dto);

        assertEquals("新例句", existing.getSentence());
        verify(repository).save(existing);
    }

    @Test
    void saveReturnsNullWhenDtoIsNull() {
        assertNull(service.save(null));
    }

    @Test
    void saveReturnsNullWhenSentenceIsBlank() {
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setSentence("   ");
        assertNull(service.save(dto));
    }

    @Test
    void saveThrowsWhenIdNotFound() {
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(999L);
        dto.setSentence("新例句");

        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.save(dto));
    }

    @Test
    void findByIdReturnsDtoWhenFoundAndEnabled() {
        ExampleSentence entity = new ExampleSentence();
        entity.setId(1L);
        entity.setSentence("例句");
        entity.setStatus(StatusEnum.ENABLED.getCode());

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        ExampleSentenceDto result = service.findById(1L);
        assertNotNull(result);
        assertEquals("例句", result.getSentence());
    }

    @Test
    void findByIdReturnsNullWhenDisabled() {
        ExampleSentence entity = new ExampleSentence();
        entity.setId(1L);
        entity.setStatus(StatusEnum.DISABLED.getCode());

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertNull(service.findById(1L));
    }

    @Test
    void findByIdReturnsNullWhenNull() {
        assertNull(service.findById(null));
    }

    @Test
    void findByIdsReturnsMapOfEnabledSentences() {
        ExampleSentence first = new ExampleSentence();
        first.setId(1L);
        first.setSentence("第一句");
        first.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentence second = new ExampleSentence();
        second.setId(2L);
        second.setSentence("第二句");
        second.setStatus(StatusEnum.DISABLED.getCode());

        when(repository.findAllById(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(first, second));

        Map<Long, ExampleSentenceDto> result = service.findByIds(Arrays.asList(1L, 2L));

        assertEquals(1, result.size());
        assertTrue(result.containsKey(1L));
    }

    @Test
    void findByStructureIdReturnsOrderedSentences() {
        ExampleSentence first = new ExampleSentence();
        first.setId(1L);
        first.setStructureId(10L);
        first.setSentence("例句A");
        first.setSentenceOrder(5);
        first.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentence second = new ExampleSentence();
        second.setId(2L);
        second.setStructureId(10L);
        second.setSentence("例句B");
        second.setSentenceOrder(10);
        second.setStatus(StatusEnum.ENABLED.getCode());

        when(repository.findByStructureIdAndStatus(10L, StatusEnum.ENABLED.getCode()))
                .thenReturn(Arrays.asList(first, second));

        List<ExampleSentenceDto> result = service.findByStructureId(10L);

        assertEquals(2, result.size());
        assertEquals("例句B", result.get(0).getSentence());
        assertEquals("例句A", result.get(1).getSentence());
    }

    @Test
    void disableByIdSetsStatusToDisabled() {
        ExampleSentence entity = new ExampleSentence();
        entity.setId(1L);
        entity.setStatus(StatusEnum.ENABLED.getCode());

        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.disableById(1L);

        assertEquals(StatusEnum.DISABLED.getCode(), entity.getStatus());
        verify(repository).save(entity);
    }

    @Test
    void disableByStructureIdDisablesAllActiveSentences() {
        ExampleSentence s1 = new ExampleSentence();
        s1.setId(1L);
        s1.setStructureId(10L);
        s1.setStatus(StatusEnum.ENABLED.getCode());

        ExampleSentence s2 = new ExampleSentence();
        s2.setId(2L);
        s2.setStructureId(10L);
        s2.setStatus(StatusEnum.ENABLED.getCode());

        when(repository.findByStructureIdAndStatus(10L, StatusEnum.ENABLED.getCode()))
                .thenReturn(Arrays.asList(s1, s2));

        service.disableByStructureId(10L);

        assertEquals(StatusEnum.DISABLED.getCode(), s1.getStatus());
        assertEquals(StatusEnum.DISABLED.getCode(), s2.getStatus());
        verify(repository).saveAll(Arrays.asList(s1, s2));
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

```bash
cd grid-system && mvn test -Dtest=ExampleSentenceServiceImplTest -DfailIfNoTests=false
```

预期：所有测试通过（绿色）。

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/test/java/com/naon/grid/backend/service/common/impl/ExampleSentenceServiceImplTest.java
git commit -m "test: rewrite ExampleSentenceServiceImplTest for new service API

Replace old tests based on syncOne/findByBizIds with new tests
for save/findById/findByIds/findByStructureId/disableById/disableByStructureId.

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 11: 完整编译验证

**文件:**
- Run: `mvn clean compile -DskipTests` 和 `mvn test`

- [ ] **Step 1: 编译整个项目**

```bash
mvn clean compile -DskipTests
```

预期：编译成功（BUILD SUCCESS）。

如果遇到编译错误：
1. 检查所有 import 是否正确（特别注意删除的类和枚举）
2. 检查所有方法调用签名是否匹配新 Service 接口
3. 检查 `CharCharacterServiceImpl` 中 `StringUtils` 是否已 import
4. 检查 `VocabWordServiceImpl` 中是否还有对 `ExampleSentenceRepository` 或旧常量/方法的引用
5. 修复后重新编译

- [ ] **Step 2: 运行所有测试**

```bash
mvn test
```

预期：所有测试通过（绿色）。

- [ ] **Step 3: 最终 Commit**

```bash
git add -A
git commit -m "chore: final cleanup after example_sentence polymorphic refactor

Co-Authored-By: Claude <noreply@anthropic.com>"
```
