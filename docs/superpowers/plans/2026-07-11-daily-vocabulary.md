# 每日一词 (Daily Vocabulary) 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现"每日一词"完整功能——后台管理 CRUD + 审核/发布草稿工作流 + App 端每日推送和历史归档 API + Redis 缓存。

**Architecture:** DailyVocabulary 作为独立实体，通过 `related_word_id` 一对一关联 `vocab_word`。后台沿用 VocabWord 的 draft→reviewed→published 三状态草稿工作流。App 端独立 VO 层，不含审计字段，翻译按语言参数筛选。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA + JpaSpecificationExecutor, Redis (RedisUtils), Lombok, Fastjson2

**Spec:** `docs/superpowers/specs/2026-07-11-daily-vocabulary-design.md`

## Global Constraints

- Java 8，Spring Boot 2.7.18
- 所有 Controller 方法使用 `@AnonymousGetMapping` / `@AnonymousPostMapping` / `@AnonymousPutMapping` / `@AnonymousDeleteMapping`（项目自定义注解）
- 后台路径前缀 `/api/daily-vocabulary`，App 路径前缀 `/api/app/daily-vocabulary`
- JSON 序列化使用 Fastjson2（`JsonUtils.toJson` / `JsonUtils.fromJson`）
- 实体内翻译字段存 JSON text，Java 类型 `List<TextTranslation>`
- 草稿内容必须覆盖业务字段到 DTO 返回，但 id/status/publishStatus/editStatus/审计字段永远以主表为准
- App VO 不含 createBy, updateBy, createTime, updateTime, editStatus, publishStatus, draftContent, status
- App 端所有多语言翻译字段按 `language` 参数筛选为单项 TextTranslationVO
- 遵循 CLAUDE.md 中 Wrapper 模式：Controller 不含转换逻辑，Wrapper 类为 `public static` 纯映射

---

## 文件结构

```
# --- grid-common (1 new) ---
grid-common/src/main/java/com/naon/grid/enums/
└── DailyVocabularyTypeEnum.java          # 类型枚举 (IDIOM/PROVERB/COLLOQUIALISM/XIEHOUYU/NEOLOGISM)

# --- grid-system (14 new) ---
grid-system/src/main/java/com/naon/grid/backend/
├── domain/vocabulary/
│   └── DailyVocabulary.java              # JPA 实体
├── repo/vocabulary/
│   └── DailyVocabularyRepository.java    # Repository
├── service/vocabulary/
│   ├── DailyVocabularyService.java       # Service 接口
│   ├── impl/
│   │   └── DailyVocabularyServiceImpl.java  # Service 实现 (含缓存逻辑)
│   └── dto/
│       ├── DailyVocabularyDto.java       # 全量 DTO
│       └── DailyVocabularyQueryCriteria.java  # 查询条件
├── rest/controller/
│   └── DailyVocabularyController.java    # 后台 Controller (10 API)
├── rest/request/
│   ├── DailyVocabularyCreateRequest.java # 创建/编辑请求
│   └── DailyVocabularyQueryRequest.java  # 列表查询请求
├── rest/vo/
│   ├── DailyVocabularyBaseVO.java        # 列表项 VO
│   ├── DailyVocabularyVO.java            # 详情 VO
│   └── DailyVocabularyCreateVO.java      # 创建返回 VO (仅 id)
└── rest/wrapper/
    └── DailyVocabularyWrapper.java       # Request→DTO / DTO→VO

# --- grid-app (6 new + 1 modified) ---
grid-app/src/main/java/com/naon/grid/modules/app/
├── enums/
│   └── CollectionBizTypeEnum.java        # [MODIFY] 新增 DAILY_VOCABULARY
├── rest/
│   ├── AppDailyVocabularyController.java # App Controller (5 API)
│   ├── request/
│   │   ├── AppDailyVocabularyHistoryRequest.java  # 历史查询请求
│   │   └── AppDailyVocabularyShareImageRequest.java  # 分享图保存请求
│   ├── vo/
│   │   ├── AppDailyVocabularyTodayVO.java    # 今日 VO (main + backups)
│   │   ├── AppDailyVocabularyBaseVO.java     # 列表项 VO
│   │   └── AppDailyVocabularyDetailVO.java   # 详情 VO (语言筛选)
│   └── wrapper/
│       └── AppDailyVocabularyWrapper.java    # App Wrapper

# --- SQL (1 modified) ---
sql/
└── biz_vocabulary.sql                   # [MODIFY] 追加 daily_vocabulary DDL
```

---

### Task 1: SQL DDL — 新增 daily_vocabulary 表

**Files:**
- Modify: `sql/biz_vocabulary.sql` — 文件末尾追加 DDL

**Interfaces:**
- Produces: `daily_vocabulary` 表结构，供后续 Task 3 实体映射

- [ ] **Step 1: 追加建表 DDL**

在 `sql/biz_vocabulary.sql` 文件末尾追加：

```sql

-- 每日一词表
CREATE TABLE `daily_vocabulary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '每日一词ID',
  `phrase` varchar(100) NOT NULL COMMENT '词目（如：画蛇添足）',
  `phrase_type` varchar(20) NOT NULL COMMENT '类型：IDIOM/PROVERB/COLLOQUIALISM/XIEHOUYU/NEOLOGISM',
  `pinyin` varchar(200) DEFAULT NULL COMMENT '拼音',
  `phrase_translations` text DEFAULT NULL COMMENT '词目翻译列表（JSON，List<TextTranslation>）',
  `audio_id` bigint DEFAULT NULL COMMENT '发音音频ID，关联 audio_resource.id',
  `image_id` bigint DEFAULT NULL COMMENT 'AI配图ID，关联 oss_resource_meta.id',
  `plain_explanation` varchar(1024) DEFAULT NULL COMMENT '通俗中文讲解',
  `explanation_translations` text DEFAULT NULL COMMENT '讲解翻译列表（JSON，List<TextTranslation>）',
  `origin_story` text DEFAULT NULL COMMENT '出处/典故/背景故事',
  `example_sentence_id` bigint DEFAULT NULL COMMENT '例句ID，关联 example_sentence.id',
  `display_date` date DEFAULT NULL COMMENT '计划展示日期',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '同日期排序，最小为主推，其余为备选',
  `related_word_id` bigint DEFAULT NULL COMMENT '关联词汇ID，关联 vocab_word.id',
  `draft_content` text DEFAULT NULL COMMENT '草稿内容JSON',
  `edit_status` varchar(20) NOT NULL DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
  `publish_status` varchar(20) NOT NULL DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_display_date` (`display_date` ASC) USING BTREE,
  INDEX `idx_phrase_type` (`phrase_type` ASC) USING BTREE,
  INDEX `idx_publish_status` (`publish_status` ASC) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 CHARACTER SET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每日一词表';
```

- [ ] **Step 2: Commit**

```bash
git add sql/biz_vocabulary.sql
git commit -m "feat: add daily_vocabulary table DDL

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: DailyVocabularyTypeEnum 枚举

**Files:**
- Create: `grid-common/src/main/java/com/naon/grid/enums/DailyVocabularyTypeEnum.java`

**Interfaces:**
- Produces: `DailyVocabularyTypeEnum` — `getCode()`, `getDescription()`, `fromCode(String)`

- [ ] **Step 1: 创建枚举类**

```java
package com.naon.grid.enums;

import lombok.Getter;

/**
 * 每日一词类型枚举
 */
@Getter
public enum DailyVocabularyTypeEnum {

    IDIOM("IDIOM", "成语"),
    PROVERB("PROVERB", "谚语"),
    COLLOQUIALISM("COLLOQUIALISM", "惯用语"),
    XIEHOUYU("XIEHOUYU", "歇后语"),
    NEOLOGISM("NEOLOGISM", "新词新语");

    private final String code;
    private final String description;

    DailyVocabularyTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DailyVocabularyTypeEnum fromCode(String code) {
        for (DailyVocabularyTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-common/src/main/java/com/naon/grid/enums/DailyVocabularyTypeEnum.java
git commit -m "feat: add DailyVocabularyTypeEnum for daily vocabulary types

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: DailyVocabulary 实体

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/DailyVocabulary.java`

**Interfaces:**
- Produces: `DailyVocabulary` JPA 实体，表 `daily_vocabulary`，继承 `BaseEntity`

- [ ] **Step 1: 创建实体类**

```java
package com.naon.grid.backend.domain.vocabulary;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "daily_vocabulary")
public class DailyVocabulary extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "每日一词唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "phrase", nullable = false, length = 100)
    @ApiModelProperty(value = "词目")
    private String phrase;

    @Column(name = "phrase_type", length = 20)
    @ApiModelProperty(value = "类型枚举")
    private String phraseType;

    @Column(name = "pinyin", length = 200)
    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @Column(name = "phrase_translations", columnDefinition = "text")
    @ApiModelProperty(value = "词目翻译列表JSON")
    private String phraseTranslations;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "发音音频ID")
    private Long audioId;

    @Column(name = "image_id")
    @ApiModelProperty(value = "AI配图ID")
    private Long imageId;

    @Column(name = "plain_explanation", length = 1024)
    @ApiModelProperty(value = "通俗中文讲解")
    private String plainExplanation;

    @Column(name = "explanation_translations", columnDefinition = "text")
    @ApiModelProperty(value = "讲解翻译列表JSON")
    private String explanationTranslations;

    @Column(name = "origin_story", columnDefinition = "text")
    @ApiModelProperty(value = "出处/典故/背景故事")
    private String originStory;

    @Column(name = "example_sentence_id")
    @ApiModelProperty(value = "例句ID")
    private Long exampleSentenceId;

    @Column(name = "display_date")
    @ApiModelProperty(value = "计划展示日期")
    private LocalDate displayDate;

    @Column(name = "sort_order")
    @ApiModelProperty(value = "同日期排序权重")
    private Integer sortOrder;

    @Column(name = "related_word_id")
    @ApiModelProperty(value = "关联词汇ID")
    private Long relatedWordId;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();

    @Column(name = "publish_status", length = 20)
    @ApiModelProperty(value = "发布状态")
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "edit_status", length = 20)
    @ApiModelProperty(value = "编辑状态")
    private String editStatus = EditStatusEnum.DRAFT.getCode();

    @Column(name = "draft_content", columnDefinition = "text")
    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/DailyVocabulary.java
git commit -m "feat: add DailyVocabulary entity with draft/publish workflow

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: DailyVocabularyRepository

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/DailyVocabularyRepository.java`

**Interfaces:**
- Produces: Repository 提供 `findByDisplayDateAndPublishStatusAndStatus` 和 native query `findDistinctDisplayDatesByMonth`

- [ ] **Step 1: 创建 Repository**

```java
package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.DailyVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyVocabularyRepository
        extends JpaRepository<DailyVocabulary, Integer>,
                JpaSpecificationExecutor<DailyVocabulary> {

    /**
     * 按展示日期、发布状态、有效状态查询（按 sort_order 排序）
     */
    List<DailyVocabulary> findByDisplayDateAndPublishStatusAndStatusOrderBySortOrderAsc(
            LocalDate displayDate, String publishStatus, Integer status);

    /**
     * 查询指定月份内有内容的日期列表（仅已发布有效内容）
     */
    @Query(value = "SELECT DISTINCT d.display_date FROM daily_vocabulary d " +
            "WHERE d.display_date BETWEEN ?1 AND ?2 " +
            "AND d.publish_status = 'published' AND d.status = 1 " +
            "ORDER BY d.display_date ASC", nativeQuery = true)
    List<java.sql.Date> findDistinctDisplayDatesByMonth(LocalDate start, LocalDate end);
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/DailyVocabularyRepository.java
git commit -m "feat: add DailyVocabularyRepository with date queries

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: DTO + QueryCriteria

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/DailyVocabularyDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/DailyVocabularyQueryCriteria.java`

**Interfaces:**
- Produces: `DailyVocabularyDto` — 全量 DTO（含 ExampleSentenceDto 嵌套），`DailyVocabularyQueryCriteria` — 查询条件（blurry/phraseType/publishStatus/displayDate 范围/publishedOnly）

- [ ] **Step 1: 创建 DailyVocabularyDto**

```java
package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DailyVocabularyDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "每日一词ID")
    private Integer id;

    @ApiModelProperty(value = "词目")
    private String phrase;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词目翻译列表")
    private List<TextTranslation> phraseTranslations;

    @ApiModelProperty(value = "发音音频ID")
    private Long audioId;

    @ApiModelProperty(value = "AI配图ID")
    private Long imageId;

    @ApiModelProperty(value = "通俗中文讲解")
    private String plainExplanation;

    @ApiModelProperty(value = "讲解翻译列表")
    private List<TextTranslation> explanationTranslations;

    @ApiModelProperty(value = "出处/典故")
    private String originStory;

    @ApiModelProperty(value = "例句")
    private ExampleSentenceDto exampleSentence;

    @ApiModelProperty(value = "例句ID（创建/编辑时直接传 ID）")
    private Long exampleSentenceId;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;

    @ApiModelProperty(value = "排序")
    private Integer sortOrder;

    @ApiModelProperty(value = "关联词汇ID")
    private Long relatedWordId;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;
}
```

- [ ] **Step 2: 创建 DailyVocabularyQueryCriteria**

```java
package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class DailyVocabularyQueryCriteria implements Serializable {

    @ApiModelProperty(value = "词目模糊查询")
    @Query(blurry = "phrase")
    private String blurry;

    @ApiModelProperty(value = "类型")
    @Query
    private String phraseType;

    @ApiModelProperty(value = "发布状态")
    @Query
    private String publishStatus;

    @ApiModelProperty(value = "展示日期起始")
    @Query(propName = "displayDate", type = Query.Type.GREATER_THAN_OR_EQUAL)
    private LocalDate displayDateStart;

    @ApiModelProperty(value = "展示日期截止")
    @Query(propName = "displayDate", type = Query.Type.LESS_THAN_OR_EQUAL)
    private LocalDate displayDateEnd;

    @ApiModelProperty(value = "仅查已发布（App端使用）")
    private Boolean publishedOnly;
}
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/DailyVocabularyDto.java \
        grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/DailyVocabularyQueryCriteria.java
git commit -m "feat: add DailyVocabularyDto and QueryCriteria

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: DailyVocabularyService 接口

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/DailyVocabularyService.java`

**Interfaces:**
- Produces: 完整 Service 接口签名

- [ ] **Step 1: 创建 Service 接口**

```java
package com.naon.grid.backend.service.vocabulary;

import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DailyVocabularyService {

    /** 分页查询 */
    PageResult<DailyVocabularyDto> queryAll(DailyVocabularyQueryCriteria criteria, Pageable pageable);

    /** 查详情（草稿态从 draftContent 覆盖） */
    DailyVocabularyDto findById(Integer id);

    /** 查已发布详情（从实表字段） */
    DailyVocabularyDto findPublishedById(Integer id);

    /** 创建，返回新 ID */
    Integer create(DailyVocabularyDto dto);

    /** 更新草稿 */
    void update(Integer id, DailyVocabularyDto dto);

    /** 软删除 */
    void delete(Integer id);

    /** 审核草稿 draft→reviewed */
    void reviewDraft(Integer id);

    /** 发布草稿 reviewed→published */
    void publishDraft(Integer id);

    /** 下线 published→unpublished */
    void offline(Integer id);

    /** 设置展示日期 */
    void schedule(Integer id, LocalDate date);

    /** 批量排期 */
    void batchSchedule(List<Integer> ids, List<LocalDate> dates);

    /** 获取今日主推（sort_order 最小的一条） */
    DailyVocabularyDto getTodayMain();

    /** 获取今日备选池（除主推外） */
    List<DailyVocabularyDto> getTodayBackups();

    /** 历史归档分页 */
    PageResult<DailyVocabularyDto> queryHistory(DailyVocabularyQueryCriteria criteria, Pageable pageable);

    /** 获取指定月份有内容的日期列表 */
    List<LocalDate> getCalendarDates(int year, int month);

    /** 保存分享图 ID */
    void saveShareImage(Integer id, Long imageId);
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/DailyVocabularyService.java
git commit -m "feat: add DailyVocabularyService interface

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 7: DailyVocabularyServiceImpl

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/DailyVocabularyServiceImpl.java`

**Interfaces:**
- Consumes: `DailyVocabularyRepository`, `DailyVocabularyService` (implements), `RedisUtils`, `ExampleSentenceService`, `JsonUtils`
- Produces: 完整 Service 实现，含草稿覆盖、发布同步、Redis 缓存

- [ ] **Step 1: 创建 Service 实现类**

```java
package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.vocabulary.DailyVocabulary;
import com.naon.grid.backend.repo.vocabulary.DailyVocabularyRepository;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.vocabulary.DailyVocabularyService;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.naon.grid.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyVocabularyServiceImpl implements DailyVocabularyService {

    private static final String CACHE_KEY_TODAY_MAIN = "daily_vocabulary:today:main";
    private static final String CACHE_KEY_TODAY_BACKUPS = "daily_vocabulary:today:backups";

    private final DailyVocabularyRepository dailyVocabularyRepository;
    private final ExampleSentenceService exampleSentenceService;
    private final RedisUtils redisUtils;

    // ==================== 查询 ====================

    @Override
    public PageResult<DailyVocabularyDto> queryAll(DailyVocabularyQueryCriteria criteria, Pageable pageable) {
        Page<DailyVocabulary> page = dailyVocabularyRepository.findAll((root, cq, cb) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, cb);
            Predicate statusPredicate = cb.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return cb.and(basePredicate, statusPredicate);
        }, pageable);
        PageResult<DailyVocabularyDto> result = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyVocabularyDto findById(Integer id) {
        DailyVocabulary entity = getEntity(id);
        if (isDraftOrReviewed(entity)) {
            return buildDtoFromDraft(entity);
        }
        return buildDtoFromEntity(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyVocabularyDto findPublishedById(Integer id) {
        DailyVocabulary entity = getEntity(id);
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(entity.getPublishStatus())) {
            throw new EntityNotFoundException(DailyVocabulary.class, "id", String.valueOf(id));
        }
        return buildDtoFromEntity(entity);
    }

    // ==================== 写入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(DailyVocabularyDto dto) {
        DailyVocabulary entity = new DailyVocabulary();
        entity.setStatus(StatusEnum.ENABLED.getCode());
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        entity.setPhrase(dto.getPhrase());
        entity.setDraftContent(JsonUtils.toJson(dto));
        entity = dailyVocabularyRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, DailyVocabularyDto dto) {
        DailyVocabulary entity = getEntity(id);

        // 已发布不允许改词目
        String newPhrase = dto.getPhrase();
        if (newPhrase != null && !newPhrase.equals(entity.getPhrase())) {
            if (PublishStatusEnum.PUBLISHED.getCode().equals(entity.getPublishStatus())) {
                throw new BadRequestException("已发布的每日一词不允许修改词目");
            }
            entity.setPhrase(newPhrase);
        }

        // 回退到草稿
        if (EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        entity.setDraftContent(JsonUtils.toJson(dto));
        dailyVocabularyRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        DailyVocabulary entity = getEntity(id);
        entity.setStatus(StatusEnum.DISABLED.getCode());
        dailyVocabularyRepository.save(entity);
        evictTodayCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Integer id) {
        DailyVocabulary entity = getEntity(id);
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }
        entity.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        dailyVocabularyRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Integer id) {
        DailyVocabulary entity = getEntity(id);
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅已审核状态可发布");
        }

        DailyVocabularyDto draft = JsonUtils.fromJson(entity.getDraftContent(), DailyVocabularyDto.class);
        if (draft == null) {
            throw new BadRequestException("草稿数据解析失败");
        }

        // 写回主表业务字段
        entity.setPhrase(draft.getPhrase());
        entity.setPhraseType(draft.getPhraseType());
        entity.setPinyin(draft.getPinyin());
        entity.setPhraseTranslations(JsonUtils.toTranslationJson(draft.getPhraseTranslations()));
        entity.setAudioId(draft.getAudioId());
        entity.setImageId(draft.getImageId());
        entity.setPlainExplanation(draft.getPlainExplanation());
        entity.setExplanationTranslations(JsonUtils.toTranslationJson(draft.getExplanationTranslations()));
        entity.setOriginStory(draft.getOriginStory());
        entity.setExampleSentenceId(draft.getExampleSentenceId());
        entity.setDisplayDate(draft.getDisplayDate());
        entity.setSortOrder(draft.getSortOrder() != null ? draft.getSortOrder() : 0);
        entity.setRelatedWordId(draft.getRelatedWordId());

        entity.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        entity.setDraftContent(null);
        dailyVocabularyRepository.save(entity);

        // 清缓存
        if (entity.getDisplayDate() != null && entity.getDisplayDate().equals(LocalDate.now())) {
            evictTodayCache();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Integer id) {
        DailyVocabulary entity = getEntity(id);
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        dailyVocabularyRepository.save(entity);
        evictTodayCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void schedule(Integer id, LocalDate date) {
        DailyVocabulary entity = getEntity(id);
        entity.setDisplayDate(date);
        dailyVocabularyRepository.save(entity);
        if (date != null && date.equals(LocalDate.now())) {
            evictTodayCache();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSchedule(List<Integer> ids, List<LocalDate> dates) {
        if (ids.size() != dates.size()) {
            throw new BadRequestException("ID 列表和日期列表长度不一致");
        }
        boolean todayAffected = false;
        for (int i = 0; i < ids.size(); i++) {
            DailyVocabulary entity = getEntity(ids.get(i));
            entity.setDisplayDate(dates.get(i));
            dailyVocabularyRepository.save(entity);
            if (dates.get(i) != null && dates.get(i).equals(LocalDate.now())) {
                todayAffected = true;
            }
        }
        if (todayAffected) {
            evictTodayCache();
        }
    }

    // ==================== 今日内容 ====================

    @Override
    public DailyVocabularyDto getTodayMain() {
        // 尝试缓存
        DailyVocabularyDto cached = redisUtils.get(CACHE_KEY_TODAY_MAIN, DailyVocabularyDto.class);
        if (cached != null) {
            return cached;
        }

        List<DailyVocabulary> list = dailyVocabularyRepository
                .findByDisplayDateAndPublishStatusAndStatusOrderBySortOrderAsc(
                        LocalDate.now(), PublishStatusEnum.PUBLISHED.getCode(), StatusEnum.ENABLED.getCode());

        if (list.isEmpty()) {
            return null;
        }

        DailyVocabularyDto dto = buildDtoFromEntity(list.get(0));
        redisUtils.set(CACHE_KEY_TODAY_MAIN, dto, secondsUntilMidnight());
        return dto;
    }

    @Override
    public List<DailyVocabularyDto> getTodayBackups() {
        // 尝试缓存
        List<DailyVocabularyDto> cached = redisUtils.getList(CACHE_KEY_TODAY_BACKUPS, DailyVocabularyDto.class);
        if (cached != null) {
            return cached;
        }

        List<DailyVocabulary> list = dailyVocabularyRepository
                .findByDisplayDateAndPublishStatusAndStatusOrderBySortOrderAsc(
                        LocalDate.now(), PublishStatusEnum.PUBLISHED.getCode(), StatusEnum.ENABLED.getCode());

        if (list.size() <= 1) {
            return Collections.emptyList();
        }

        List<DailyVocabularyDto> backups = list.subList(1, list.size()).stream()
                .map(this::buildDtoFromEntity)
                .collect(Collectors.toList());
        redisUtils.set(CACHE_KEY_TODAY_BACKUPS, backups, secondsUntilMidnight());
        return backups;
    }

    // ==================== 历史 & 日历 ====================

    @Override
    public PageResult<DailyVocabularyDto> queryHistory(DailyVocabularyQueryCriteria criteria, Pageable pageable) {
        Page<DailyVocabulary> page = dailyVocabularyRepository.findAll((root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 仅已发布有效
            predicates.add(cb.equal(root.get("status"), StatusEnum.ENABLED.getCode()));
            predicates.add(cb.equal(root.get("publishStatus"), PublishStatusEnum.PUBLISHED.getCode()));

            // 可选过滤
            if (criteria.getPhraseType() != null) {
                predicates.add(cb.equal(root.get("phraseType"), criteria.getPhraseType()));
            }
            if (criteria.getBlurry() != null) {
                predicates.add(cb.like(root.get("phrase"), "%" + criteria.getBlurry() + "%"));
            }
            if (criteria.getDisplayDateStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("displayDate"), criteria.getDisplayDateStart()));
            }
            if (criteria.getDisplayDateEnd() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("displayDate"), criteria.getDisplayDateEnd()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        return PageUtil.toPage(page.map(this::buildDtoFromEntity));
    }

    @Override
    public List<LocalDate> getCalendarDates(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        List<java.sql.Date> sqlDates = dailyVocabularyRepository.findDistinctDisplayDatesByMonth(start, end);
        return sqlDates.stream()
                .map(java.sql.Date::toLocalDate)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveShareImage(Integer id, Long imageId) {
        DailyVocabulary entity = getEntity(id);
        entity.setImageId(imageId);
        dailyVocabularyRepository.save(entity);
    }

    // ==================== 私有辅助 ====================

    private DailyVocabulary getEntity(Integer id) {
        DailyVocabulary entity = dailyVocabularyRepository.findById(id).orElse(null);
        if (entity == null || StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(DailyVocabulary.class, "id", String.valueOf(id));
        }
        return entity;
    }

    private boolean isDraftOrReviewed(DailyVocabulary entity) {
        return EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus());
    }

    /** 从草稿 JSON 构建 DTO，ID/状态/审计以主表为准 */
    private DailyVocabularyDto buildDtoFromDraft(DailyVocabulary entity) {
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        DailyVocabularyDto draft = JsonUtils.fromJson(entity.getDraftContent(), DailyVocabularyDto.class);
        if (draft == null) {
            throw new BadRequestException("草稿数据解析失败");
        }
        draft.setId(entity.getId());
        draft.setStatus(entity.getStatus());
        draft.setPublishStatus(entity.getPublishStatus());
        draft.setEditStatus(entity.getEditStatus());
        draft.setCreateBy(entity.getCreateBy());
        draft.setUpdateBy(entity.getUpdateBy());
        draft.setCreateTime(entity.getCreateTime());
        draft.setUpdateTime(entity.getUpdateTime());
        return draft;
    }

    /** 列表页草稿覆盖：仅覆盖业务字段 */
    private DailyVocabularyDto toDtoWithDraftOverlay(DailyVocabulary entity) {
        DailyVocabularyDto dto = entityToBaseDto(entity);
        if (isDraftOrReviewed(entity)) {
            DailyVocabularyDto draft = JsonUtils.fromJson(entity.getDraftContent(), DailyVocabularyDto.class);
            if (draft != null) {
                if (draft.getPhrase() != null) dto.setPhrase(draft.getPhrase());
                if (draft.getPhraseType() != null) dto.setPhraseType(draft.getPhraseType());
                if (draft.getPinyin() != null) dto.setPinyin(draft.getPinyin());
                if (draft.getDisplayDate() != null) dto.setDisplayDate(draft.getDisplayDate());
                if (draft.getSortOrder() != null) dto.setSortOrder(draft.getSortOrder());
            }
        }
        return dto;
    }

    /** 从实表字段构建完整 DTO（已发布详情） */
    private DailyVocabularyDto buildDtoFromEntity(DailyVocabulary entity) {
        DailyVocabularyDto dto = entityToBaseDto(entity);
        // 加载例句
        if (entity.getExampleSentenceId() != null) {
            try {
                ExampleSentenceDto esDto = exampleSentenceService.findById(entity.getExampleSentenceId());
                dto.setExampleSentence(esDto);
            } catch (Exception e) {
                log.warn("例句加载失败, exampleSentenceId={}", entity.getExampleSentenceId(), e);
            }
        }
        return dto;
    }

    /** 实体→DTO 基础字段映射 */
    private DailyVocabularyDto entityToBaseDto(DailyVocabulary entity) {
        DailyVocabularyDto dto = new DailyVocabularyDto();
        dto.setId(entity.getId());
        dto.setPhrase(entity.getPhrase());
        dto.setPhraseType(entity.getPhraseType());
        dto.setPinyin(entity.getPinyin());
        dto.setPhraseTranslations(JsonUtils.parseTranslationList(entity.getPhraseTranslations()));
        dto.setAudioId(entity.getAudioId());
        dto.setImageId(entity.getImageId());
        dto.setPlainExplanation(entity.getPlainExplanation());
        dto.setExplanationTranslations(JsonUtils.parseTranslationList(entity.getExplanationTranslations()));
        dto.setOriginStory(entity.getOriginStory());
        dto.setExampleSentenceId(entity.getExampleSentenceId());
        dto.setDisplayDate(entity.getDisplayDate());
        dto.setSortOrder(entity.getSortOrder());
        dto.setRelatedWordId(entity.getRelatedWordId());
        dto.setStatus(entity.getStatus());
        dto.setPublishStatus(entity.getPublishStatus());
        dto.setEditStatus(entity.getEditStatus());
        dto.setCreateBy(entity.getCreateBy());
        dto.setUpdateBy(entity.getUpdateBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    /** 计算到次日 00:00 的秒数 */
    private long secondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.plusDays(1).truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        return ChronoUnit.SECONDS.between(now, midnight);
    }

    /** 清除今日缓存 */
    private void evictTodayCache() {
        redisUtils.del(CACHE_KEY_TODAY_MAIN, CACHE_KEY_TODAY_BACKUPS);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/DailyVocabularyServiceImpl.java
git commit -m "feat: add DailyVocabularyServiceImpl with cache and draft workflow

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 8: Backend Request 类

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/DailyVocabularyCreateRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/DailyVocabularyQueryRequest.java`

**Interfaces:**
- Produces: 两个 Request 类供 Controller 接收参数

- [ ] **Step 1: 创建 DailyVocabularyCreateRequest**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DailyVocabularyCreateRequest implements Serializable {

    @NotBlank
    @ApiModelProperty(value = "词目", required = true)
    private String phrase;

    @ApiModelProperty(value = "类型: IDIOM/PROVERB/COLLOQUIALISM/XIEHOUYU/NEOLOGISM")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词目翻译列表")
    private List<TextTranslationRequest> phraseTranslations;

    @ApiModelProperty(value = "发音音频ID")
    private Long audioId;

    @ApiModelProperty(value = "AI配图ID")
    private Long imageId;

    @ApiModelProperty(value = "通俗中文讲解")
    private String plainExplanation;

    @ApiModelProperty(value = "讲解翻译列表")
    private List<TextTranslationRequest> explanationTranslations;

    @ApiModelProperty(value = "出处/典故")
    private String originStory;

    @ApiModelProperty(value = "例句ID")
    private Long exampleSentenceId;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;

    @ApiModelProperty(value = "排序（同日期最小=主推）")
    private Integer sortOrder;

    @ApiModelProperty(value = "关联词汇ID")
    private Long relatedWordId;
}
```

- [ ] **Step 2: 创建 DailyVocabularyQueryRequest**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
public class DailyVocabularyQueryRequest implements Serializable {

    @ApiModelProperty(value = "词目模糊搜索")
    private String blurry;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "展示日期起")
    private LocalDate displayDateStart;

    @ApiModelProperty(value = "展示日期止")
    private LocalDate displayDateEnd;
}
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/DailyVocabularyCreateRequest.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/request/DailyVocabularyQueryRequest.java
git commit -m "feat: add DailyVocabulary request classes for admin API

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 9: Backend VO 类

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/DailyVocabularyBaseVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/DailyVocabularyVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/DailyVocabularyCreateVO.java`

**Interfaces:**
- Produces: 供后台 Controller 返回 JSON 的 VO 类

- [ ] **Step 1: 创建 DailyVocabularyCreateVO**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class DailyVocabularyCreateVO implements Serializable {

    @ApiModelProperty(value = "新增的每日一词ID")
    private Integer id;
}
```

- [ ] **Step 2: 创建 DailyVocabularyBaseVO**

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;

@Getter
@Setter
public class DailyVocabularyBaseVO implements Serializable {

    @ApiModelProperty(value = "ID")
    private Integer id;

    @ApiModelProperty(value = "词目")
    private String phrase;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;

    @ApiModelProperty(value = "排序")
    private Integer sortOrder;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
```

- [ ] **Step 3: 创建 DailyVocabularyVO**

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DailyVocabularyVO implements Serializable {

    @ApiModelProperty(value = "ID")
    private Integer id;

    @ApiModelProperty(value = "词目")
    private String phrase;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词目翻译列表（全部语言）")
    private List<TextTranslationVO> phraseTranslations;

    @ApiModelProperty(value = "发音音频ID")
    private Long audioId;

    @ApiModelProperty(value = "AI配图ID")
    private Long imageId;

    @ApiModelProperty(value = "通俗中文讲解")
    private String plainExplanation;

    @ApiModelProperty(value = "讲解翻译列表（全部语言）")
    private List<TextTranslationVO> explanationTranslations;

    @ApiModelProperty(value = "出处/典故")
    private String originStory;

    @ApiModelProperty(value = "例句")
    private ExampleSentenceVO exampleSentence;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;

    @ApiModelProperty(value = "排序")
    private Integer sortOrder;

    @ApiModelProperty(value = "关联词汇ID")
    private Long relatedWordId;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
```

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/DailyVocabularyCreateVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/DailyVocabularyBaseVO.java \
        grid-system/src/main/java/com/naon/grid/backend/rest/vo/DailyVocabularyVO.java
git commit -m "feat: add DailyVocabulary VO classes for admin API

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 10: DailyVocabularyWrapper (后台)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/DailyVocabularyWrapper.java`

**Interfaces:**
- Consumes: `DailyVocabularyCreateRequest`, `DailyVocabularyQueryRequest`, `DailyVocabularyDto`, `TextTranslationRequest`, `ExampleSentenceRequest`, `TextTranslationVO`, `ExampleSentenceVO`
- Produces: 静态转换方法

- [ ] **Step 1: 创建 Wrapper**

```java
package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.DailyVocabularyCreateRequest;
import com.naon.grid.backend.rest.request.DailyVocabularyQueryRequest;
import com.naon.grid.backend.rest.request.ExampleSentenceRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.DailyVocabularyBaseVO;
import com.naon.grid.backend.rest.vo.DailyVocabularyVO;
import com.naon.grid.backend.rest.vo.ExampleSentenceVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DailyVocabularyWrapper {

    public static DailyVocabularyQueryCriteria toCriteria(DailyVocabularyQueryRequest request) {
        DailyVocabularyQueryCriteria criteria = new DailyVocabularyQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPhraseType(request.getPhraseType());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setDisplayDateStart(request.getDisplayDateStart());
        criteria.setDisplayDateEnd(request.getDisplayDateEnd());
        return criteria;
    }

    public static DailyVocabularyDto toDto(DailyVocabularyCreateRequest request) {
        DailyVocabularyDto dto = new DailyVocabularyDto();
        dto.setPhrase(request.getPhrase());
        dto.setPhraseType(request.getPhraseType());
        dto.setPinyin(request.getPinyin());
        dto.setPhraseTranslations(toTextTranslationList(request.getPhraseTranslations()));
        dto.setAudioId(request.getAudioId());
        dto.setImageId(request.getImageId());
        dto.setPlainExplanation(request.getPlainExplanation());
        dto.setExplanationTranslations(toTextTranslationList(request.getExplanationTranslations()));
        dto.setOriginStory(request.getOriginStory());
        dto.setExampleSentenceId(request.getExampleSentenceId());
        dto.setDisplayDate(request.getDisplayDate());
        dto.setSortOrder(request.getSortOrder());
        dto.setRelatedWordId(request.getRelatedWordId());
        return dto;
    }

    public static List<DailyVocabularyBaseVO> toBaseVOList(List<DailyVocabularyDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(DailyVocabularyWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static DailyVocabularyBaseVO toBaseVO(DailyVocabularyDto dto) {
        DailyVocabularyBaseVO vo = new DailyVocabularyBaseVO();
        vo.setId(dto.getId());
        vo.setPhrase(dto.getPhrase());
        vo.setPhraseType(dto.getPhraseType());
        vo.setPinyin(dto.getPinyin());
        vo.setDisplayDate(dto.getDisplayDate());
        vo.setSortOrder(dto.getSortOrder());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static DailyVocabularyVO toVO(DailyVocabularyDto dto) {
        DailyVocabularyVO vo = new DailyVocabularyVO();
        vo.setId(dto.getId());
        vo.setPhrase(dto.getPhrase());
        vo.setPhraseType(dto.getPhraseType());
        vo.setPinyin(dto.getPinyin());
        vo.setPhraseTranslations(toTextTranslationVOList(dto.getPhraseTranslations()));
        vo.setAudioId(dto.getAudioId());
        vo.setImageId(dto.getImageId());
        vo.setPlainExplanation(dto.getPlainExplanation());
        vo.setExplanationTranslations(toTextTranslationVOList(dto.getExplanationTranslations()));
        vo.setOriginStory(dto.getOriginStory());
        if (dto.getExampleSentence() != null) {
            vo.setExampleSentence(toExampleSentenceVO(dto.getExampleSentence()));
        }
        vo.setDisplayDate(dto.getDisplayDate());
        vo.setSortOrder(dto.getSortOrder());
        vo.setRelatedWordId(dto.getRelatedWordId());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    // ==================== 私有工具方法 ====================

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(r -> {
            TextTranslation t = new TextTranslation();
            t.setLanguage(r.getLanguage());
            t.setTranslation(r.getTranslation());
            return t;
        }).collect(Collectors.toList());
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(t -> {
            TextTranslationVO vo = new TextTranslationVO();
            vo.setLanguage(t.getLanguage());
            vo.setTranslation(t.getTranslation());
            return vo;
        }).collect(Collectors.toList());
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
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/DailyVocabularyWrapper.java
git commit -m "feat: add DailyVocabularyWrapper for admin API conversion

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 11: DailyVocabularyController (后台)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/DailyVocabularyController.java`

**Interfaces:**
- Consumes: `DailyVocabularyService`, `DailyVocabularyWrapper`
- Produces: 10 个后台 API 端点

- [ ] **Step 1: 创建 Controller**

```java
package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.DailyVocabularyCreateRequest;
import com.naon.grid.backend.rest.request.DailyVocabularyQueryRequest;
import com.naon.grid.backend.rest.vo.DailyVocabularyBaseVO;
import com.naon.grid.backend.rest.vo.DailyVocabularyCreateVO;
import com.naon.grid.backend.rest.vo.DailyVocabularyVO;
import com.naon.grid.backend.rest.wrapper.DailyVocabularyWrapper;
import com.naon.grid.backend.service.vocabulary.DailyVocabularyService;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：每日一词管理")
@RequestMapping("/api/daily-vocabulary")
public class DailyVocabularyController {

    private final DailyVocabularyService dailyVocabularyService;

    @Log("新增每日一词")
    @ApiOperation("新增每日一词")
    @AnonymousPostMapping
    public ResponseEntity<DailyVocabularyCreateVO> create(@Valid @RequestBody DailyVocabularyCreateRequest request) {
        DailyVocabularyCreateVO vo = new DailyVocabularyCreateVO();
        vo.setId(dailyVocabularyService.create(DailyVocabularyWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("编辑每日一词")
    @ApiOperation("编辑每日一词内容")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                       @Valid @RequestBody DailyVocabularyCreateRequest request) {
        dailyVocabularyService.update(id, DailyVocabularyWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询每日一词详情")
    @ApiOperation("根据ID查询每日一词详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<DailyVocabularyVO> findById(@PathVariable Integer id) {
        return new ResponseEntity<>(
                DailyVocabularyWrapper.toVO(dailyVocabularyService.findById(id)), HttpStatus.OK);
    }

    @Log("查询每日一词列表")
    @ApiOperation("分页查询每日一词列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<DailyVocabularyBaseVO>> queryAll(
            DailyVocabularyQueryRequest request, Pageable pageable) {
        PageResult<DailyVocabularyDto> pageResult = dailyVocabularyService.queryAll(
                DailyVocabularyWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(DailyVocabularyWrapper.toBaseVOList(pageResult.getContent()),
                        pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("审核每日一词")
    @ApiOperation("审核通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Integer id) {
        dailyVocabularyService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布每日一词")
    @ApiOperation("发布每日一词（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publish(@PathVariable Integer id) {
        dailyVocabularyService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线每日一词")
    @ApiOperation("下线每日一词")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Integer id) {
        dailyVocabularyService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除每日一词")
    @ApiOperation("删除每日一词")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        dailyVocabularyService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("设置展示日期")
    @ApiOperation("设置每日一词展示日期")
    @AnonymousPutMapping("/{id}/schedule")
    public ResponseEntity<Void> schedule(@PathVariable Integer id,
                                         @RequestParam LocalDate date) {
        dailyVocabularyService.schedule(id, date);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("批量排期")
    @ApiOperation("批量设置每日一词展示日期")
    @AnonymousPostMapping("/batch-schedule")
    public ResponseEntity<Void> batchSchedule(@RequestParam List<Integer> ids,
                                              @RequestParam List<LocalDate> dates) {
        dailyVocabularyService.batchSchedule(ids, dates);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/DailyVocabularyController.java
git commit -m "feat: add DailyVocabularyController with 10 admin API endpoints

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 12: CollectionBizTypeEnum 扩展

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/enums/CollectionBizTypeEnum.java`

**Interfaces:**
- Consumes: 现有枚举值列表
- Produces: 新增 `DAILY_VOCABULARY`

- [ ] **Step 1: 追加枚举值**

在 `VOCAB_COMPARISON("VOCAB_COMPARISON", "词汇辨析")` 的分号 `;` 前追加逗号和新枚举：

定位原文：
```java
    VOCAB_COMPARISON("VOCAB_COMPARISON", "词汇辨析");
```

替换为：
```java
    VOCAB_COMPARISON("VOCAB_COMPARISON", "词汇辨析"),
    DAILY_VOCABULARY("DAILY_VOCABULARY", "每日一词");
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/enums/CollectionBizTypeEnum.java
git commit -m "feat: add DAILY_VOCABULARY to CollectionBizTypeEnum

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 13: App Request 类

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppDailyVocabularyHistoryRequest.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppDailyVocabularyShareImageRequest.java`

**Interfaces:**
- Produces: App 端请求参数类

- [ ] **Step 1: 创建 AppDailyVocabularyHistoryRequest**

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppDailyVocabularyHistoryRequest implements Serializable {

    @ApiModelProperty(value = "类型筛选")
    private String phraseType;

    @ApiModelProperty(value = "关键词搜索（词目）")
    private String keyword;

    @ApiModelProperty(value = "月份 yyyy-MM")
    private String month;

    @ApiModelProperty(value = "页码")
    private Integer page = 0;

    @ApiModelProperty(value = "每页条数")
    private Integer size = 20;
}
```

- [ ] **Step 2: 创建 AppDailyVocabularyShareImageRequest**

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppDailyVocabularyShareImageRequest implements Serializable {

    @ApiModelProperty(value = "分享图资源ID", required = true)
    private Long imageId;
}
```

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppDailyVocabularyHistoryRequest.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppDailyVocabularyShareImageRequest.java
git commit -m "feat: add App daily vocabulary request classes

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 14: App VO 类

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppDailyVocabularyTodayVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppDailyVocabularyBaseVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppDailyVocabularyDetailVO.java`

**Interfaces:**
- Produces: App 端 VO，不含审计/状态字段，语言筛选为单项

- [ ] **Step 1: 创建 AppDailyVocabularyBaseVO（历史列表项）**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
public class AppDailyVocabularyBaseVO implements Serializable {

    @ApiModelProperty(value = "ID")
    private Long id;

    @ApiModelProperty(value = "词目")
    private String phrase;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "配图URL")
    private String imageUrl;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;
}
```

- [ ] **Step 2: 创建 AppDailyVocabularyDetailVO（详情/今日卡片）**

```java
package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
public class AppDailyVocabularyDetailVO implements Serializable {

    @ApiModelProperty(value = "ID")
    private Long id;

    @ApiModelProperty(value = "词目")
    private String phrase;

    @ApiModelProperty(value = "类型")
    private String phraseType;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词目翻译（按语言筛选后的单条）")
    private TextTranslationVO phraseTranslation;

    @ApiModelProperty(value = "通俗中文讲解")
    private String plainExplanation;

    @ApiModelProperty(value = "讲解翻译（按语言筛选后的单条）")
    private TextTranslationVO explanationTranslation;

    @ApiModelProperty(value = "出处/典故")
    private String originStory;

    @ApiModelProperty(value = "例句")
    private VocabExampleVO exampleSentence;

    @ApiModelProperty(value = "词目发音")
    private AudioVO audio;

    @ApiModelProperty(value = "配图")
    private ImageVO image;

    @ApiModelProperty(value = "展示日期")
    private LocalDate displayDate;

    @ApiModelProperty(value = "关联词汇ID")
    private Long relatedWordId;

    // === 内嵌 VO ===

    @Getter
    @Setter
    public static class AudioVO implements Serializable {
        @ApiModelProperty(value = "音频URL")
        private String audioUrl;
    }

    @Getter
    @Setter
    public static class ImageVO implements Serializable {
        @ApiModelProperty(value = "图片URL")
        private String imageUrl;
    }

    @Getter
    @Setter
    public static class VocabExampleVO implements Serializable {
        @ApiModelProperty(value = "例句中文文案")
        private String sentence;

        @ApiModelProperty(value = "例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "例句音频")
        private AudioVO audio;

        @ApiModelProperty(value = "例句翻译（按语言筛选后的单条）")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "例句图片")
        private ImageVO image;
    }
}
```

- [ ] **Step 3: 创建 AppDailyVocabularyTodayVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppDailyVocabularyTodayVO implements Serializable {

    @ApiModelProperty(value = "今日主推")
    private AppDailyVocabularyDetailVO main;

    @ApiModelProperty(value = "备选池")
    private List<AppDailyVocabularyDetailVO> backups;
}
```

- [ ] **Step 4: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppDailyVocabularyBaseVO.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppDailyVocabularyDetailVO.java \
        grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppDailyVocabularyTodayVO.java
git commit -m "feat: add App daily vocabulary VO classes with language filtering

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 15: AppDailyVocabularyWrapper

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppDailyVocabularyWrapper.java`

**Interfaces:**
- Consumes: `DailyVocabularyDto`, `AudioResourceDto`, `AliOssStorageDto`, `TextTranslation`, `ExampleSentenceDto`
- Produces: `AppDailyVocabularyTodayVO`, `AppDailyVocabularyDetailVO`, `List<AppDailyVocabularyBaseVO>`

- [ ] **Step 1: 创建 App Wrapper**

```java
package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyBaseVO;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyDetailVO;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyTodayVO;
import com.naon.grid.service.dto.AliOssStorageDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppDailyVocabularyWrapper {

    public static AppDailyVocabularyTodayVO toTodayVO(
            DailyVocabularyDto main, List<DailyVocabularyDto> backups,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppDailyVocabularyTodayVO vo = new AppDailyVocabularyTodayVO();
        vo.setMain(toDetailVO(main, audioMap, imageMap, language));
        if (backups != null) {
            vo.setBackups(backups.stream()
                    .map(d -> toDetailVO(d, audioMap, imageMap, language))
                    .collect(Collectors.toList()));
        } else {
            vo.setBackups(Collections.emptyList());
        }
        return vo;
    }

    public static AppDailyVocabularyDetailVO toDetailVO(
            DailyVocabularyDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppDailyVocabularyDetailVO vo = new AppDailyVocabularyDetailVO();
        vo.setId(dto.getId() != null ? dto.getId().longValue() : null);
        vo.setPhrase(dto.getPhrase());
        vo.setPhraseType(dto.getPhraseType());
        vo.setPinyin(dto.getPinyin());
        vo.setPhraseTranslation(filterByLanguage(dto.getPhraseTranslations(), language));
        vo.setPlainExplanation(dto.getPlainExplanation());
        vo.setExplanationTranslation(filterByLanguage(dto.getExplanationTranslations(), language));
        vo.setOriginStory(dto.getOriginStory());
        vo.setDisplayDate(dto.getDisplayDate());
        vo.setRelatedWordId(dto.getRelatedWordId());

        // 音频
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppDailyVocabularyDetailVO.AudioVO audioVO = new AppDailyVocabularyDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            }
        }

        // 配图
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                AppDailyVocabularyDetailVO.ImageVO imageVO = new AppDailyVocabularyDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setImage(imageVO);
            }
        }

        // 例句
        if (dto.getExampleSentence() != null) {
            vo.setExampleSentence(toExampleVO(dto.getExampleSentence(), audioMap, imageMap, language));
        }

        return vo;
    }

    public static List<AppDailyVocabularyBaseVO> toBaseVOList(
            List<DailyVocabularyDto> dtos, Map<Long, AliOssStorageDto> imageMap) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(d -> toBaseVO(d, imageMap)).collect(Collectors.toList());
    }

    public static AppDailyVocabularyBaseVO toBaseVO(
            DailyVocabularyDto dto, Map<Long, AliOssStorageDto> imageMap) {
        AppDailyVocabularyBaseVO vo = new AppDailyVocabularyBaseVO();
        vo.setId(dto.getId() != null ? dto.getId().longValue() : null);
        vo.setPhrase(dto.getPhrase());
        vo.setPhraseType(dto.getPhraseType());
        vo.setPinyin(dto.getPinyin());
        vo.setDisplayDate(dto.getDisplayDate());
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                vo.setImageUrl(imgDto.getFileUrl());
            }
        }
        return vo;
    }

    // ==================== 私有 ====================

    private static AppDailyVocabularyDetailVO.VocabExampleVO toExampleVO(
            ExampleSentenceDto dto,
            Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap,
            String language) {
        AppDailyVocabularyDetailVO.VocabExampleVO vo = new AppDailyVocabularyDetailVO.VocabExampleVO();
        vo.setSentence(dto.getSentence());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));

        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppDailyVocabularyDetailVO.AudioVO audioVO = new AppDailyVocabularyDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            }
        }

        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                AppDailyVocabularyDetailVO.ImageVO imageVO = new AppDailyVocabularyDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setImage(imageVO);
            }
        }
        return vo;
    }

    private static TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) return null;
        return translations.stream()
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst()
                .map(t -> {
                    TextTranslationVO vo = new TextTranslationVO();
                    vo.setLanguage(t.getLanguage());
                    vo.setTranslation(t.getTranslation());
                    return vo;
                })
                .orElse(null);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppDailyVocabularyWrapper.java
git commit -m "feat: add AppDailyVocabularyWrapper with language filtering

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 16: AppDailyVocabularyController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppDailyVocabularyController.java`

**Interfaces:**
- Consumes: `DailyVocabularyService` (from grid-system), `AudioResourceService`, `AliOssStorageService`, `AppDailyVocabularyWrapper`
- Produces: 5 个 App API 端点

- [ ] **Step 1: 创建 App Controller**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.vocabulary.DailyVocabularyService;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyDto;
import com.naon.grid.backend.service.vocabulary.dto.DailyVocabularyQueryCriteria;
import com.naon.grid.modules.app.rest.request.AppDailyVocabularyHistoryRequest;
import com.naon.grid.modules.app.rest.request.AppDailyVocabularyShareImageRequest;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyBaseVO;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyDetailVO;
import com.naon.grid.modules.app.rest.vo.AppDailyVocabularyTodayVO;
import com.naon.grid.modules.app.rest.wrapper.AppDailyVocabularyWrapper;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/daily-vocabulary")
@Api(tags = "用户：每日一词接口")
public class AppDailyVocabularyController {

    private final DailyVocabularyService dailyVocabularyService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;

    @ApiOperation("今日每日一词（主推+备选池）")
    @AnonymousGetMapping("/today")
    public ResponseEntity<AppDailyVocabularyTodayVO> today(@RequestParam(defaultValue = "zh") String language) {
        DailyVocabularyDto main = dailyVocabularyService.getTodayMain();
        List<DailyVocabularyDto> backups = dailyVocabularyService.getTodayBackups();

        // 收集音频/图片 ID
        List<Long> audioIds = collectAudioIds(main, backups);
        List<Long> imageIds = collectImageIds(main, backups);

        Map<Long, AudioResourceDto> audioMap = batchQueryAudios(audioIds);
        Map<Long, AliOssStorageDto> imageMap = batchQueryImages(imageIds);

        AppDailyVocabularyTodayVO vo = AppDailyVocabularyWrapper.toTodayVO(main, backups, audioMap, imageMap, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    @ApiOperation("每日一词详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppDailyVocabularyDetailVO> detail(@PathVariable Integer id,
                                                              @RequestParam String language) {
        DailyVocabularyDto dto = dailyVocabularyService.findPublishedById(id);
        Map<Long, AudioResourceDto> audioMap = batchQueryAudios(collectAudioIds(dto, null));
        Map<Long, AliOssStorageDto> imageMap = batchQueryImages(collectImageIds(dto, null));
        AppDailyVocabularyDetailVO vo = AppDailyVocabularyWrapper.toDetailVO(dto, audioMap, imageMap, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    @ApiOperation("历史归档")
    @AnonymousGetMapping("/history")
    public ResponseEntity<PageResult<AppDailyVocabularyBaseVO>> history(
            AppDailyVocabularyHistoryRequest request) {
        DailyVocabularyQueryCriteria criteria = new DailyVocabularyQueryCriteria();
        criteria.setPhraseType(request.getPhraseType());
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            criteria.setBlurry(request.getKeyword());
        }
        if (request.getMonth() != null && !request.getMonth().isEmpty()) {
            YearMonth ym = YearMonth.parse(request.getMonth());
            criteria.setDisplayDateStart(ym.atDay(1));
            criteria.setDisplayDateEnd(ym.atEndOfMonth());
        }
        criteria.setPublishedOnly(true);

        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "displayDate"));

        PageResult<DailyVocabularyDto> pageResult = dailyVocabularyService.queryHistory(criteria, pageable);

        // 批量查询图片
        List<Long> imageIds = pageResult.getContent().stream()
                .map(DailyVocabularyDto::getImageId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, AliOssStorageDto> imageMap = batchQueryImages(imageIds);

        List<AppDailyVocabularyBaseVO> vos = AppDailyVocabularyWrapper.toBaseVOList(
                pageResult.getContent(), imageMap);
        return new ResponseEntity<>(
                new PageResult<>(vos, pageResult.getTotalElements()), HttpStatus.OK);
    }

    @ApiOperation("日历视图（月有内容日期）")
    @AnonymousGetMapping("/calendar")
    public ResponseEntity<List<LocalDate>> calendar(@RequestParam int year, @RequestParam int month) {
        List<LocalDate> dates = dailyVocabularyService.getCalendarDates(year, month);
        return new ResponseEntity<>(dates, HttpStatus.OK);
    }

    @ApiOperation("保存分享图")
    @AnonymousPostMapping("/{id}/share-image")
    public ResponseEntity<Void> saveShareImage(@PathVariable Integer id,
                                                @RequestBody AppDailyVocabularyShareImageRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        if (userId == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        dailyVocabularyService.saveShareImage(id, request.getImageId());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ==================== 私有辅助 ====================

    private List<Long> collectAudioIds(DailyVocabularyDto main, List<DailyVocabularyDto> backups) {
        List<Long> ids = new ArrayList<>();
        if (main != null && main.getAudioId() != null) ids.add(main.getAudioId());
        if (main != null && main.getExampleSentence() != null
                && main.getExampleSentence().getAudioId() != null) {
            ids.add(main.getExampleSentence().getAudioId());
        }
        if (backups != null) {
            for (DailyVocabularyDto d : backups) {
                if (d.getAudioId() != null) ids.add(d.getAudioId());
                if (d.getExampleSentence() != null && d.getExampleSentence().getAudioId() != null) {
                    ids.add(d.getExampleSentence().getAudioId());
                }
            }
        }
        return ids;
    }

    private List<Long> collectImageIds(DailyVocabularyDto main, List<DailyVocabularyDto> backups) {
        List<Long> ids = new ArrayList<>();
        if (main != null && main.getImageId() != null) ids.add(main.getImageId());
        if (main != null && main.getExampleSentence() != null
                && main.getExampleSentence().getImageId() != null) {
            ids.add(main.getExampleSentence().getImageId());
        }
        if (backups != null) {
            for (DailyVocabularyDto d : backups) {
                if (d.getImageId() != null) ids.add(d.getImageId());
                if (d.getExampleSentence() != null && d.getExampleSentence().getImageId() != null) {
                    ids.add(d.getExampleSentence().getImageId());
                }
            }
        }
        return ids;
    }

    private Map<Long, AudioResourceDto> batchQueryAudios(List<Long> audioIds) {
        if (audioIds.isEmpty()) return Collections.emptyMap();
        List<AudioResourceDto> dtos = audioResourceService.findByIds(audioIds);
        return dtos.stream()
                .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
    }

    private Map<Long, AliOssStorageDto> batchQueryImages(List<Long> imageIds) {
        if (imageIds.isEmpty()) return Collections.emptyMap();
        List<AliOssStorageDto> dtos = aliOssStorageService.findByIds(imageIds);
        return dtos.stream()
                .collect(Collectors.toMap(AliOssStorageDto::getId, i -> i, (a, b) -> a));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppDailyVocabularyController.java
git commit -m "feat: add AppDailyVocabularyController with 5 app API endpoints

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 17: 定时任务 — 每日 00:00 刷新缓存

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/DailyVocabularyServiceImpl.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/scheduled/DailyVocabularyScheduledTask.java`

**Interfaces:**
- Consumes: `RedisUtils` (已在 DailyVocabularyServiceImpl 中注入)
- Produces: `@Scheduled` 定时清缓存

- [ ] **Step 1: 创建定时任务类**

在 `grid-system/src/main/java/com/naon/grid/backend/scheduled/` 下创建（如目录不存在则建）：

```java
package com.naon.grid.backend.scheduled;

import com.naon.grid.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyVocabularyScheduledTask {

    private final RedisUtils redisUtils;

    private static final String CACHE_KEY_TODAY_MAIN = "daily_vocabulary:today:main";
    private static final String CACHE_KEY_TODAY_BACKUPS = "daily_vocabulary:today:backups";

    @Scheduled(cron = "0 0 0 * * ?")
    public void refreshTodayCache() {
        log.info("每日一词缓存刷新开始");
        redisUtils.del(CACHE_KEY_TODAY_MAIN, CACHE_KEY_TODAY_BACKUPS);
        log.info("每日一词缓存已清除，等待懒加载");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/scheduled/DailyVocabularyScheduledTask.java
git commit -m "feat: add DailyVocabularyScheduledTask for midnight cache refresh

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Spec 覆盖检查清单

| 规格要求 | 对应任务 |
|---------|---------|
| daily_vocabulary 表 DDL | Task 1 |
| DailyVocabularyTypeEnum 枚举 | Task 2 |
| 实体 + Repository | Task 3, 4 |
| Service 接口 + 实现（草稿/发布/Cache） | Task 5, 6, 7 |
| 后台 CRUD + 审核/发布/下线/排期 API | Task 8, 9, 10, 11 |
| CollectionBizTypeEnum 扩展 | Task 12 |
| App 端历史/今日/详情/日历/分享 API | Task 13, 14, 15, 16 |
| Redis 缓存 + 定时刷新 | Task 7, 17 |
| App VO 不含审计字段 + 语言筛选 | Task 14, 15 |
| 后台 VO 全量译文 | Task 9, 10 |

---

## 执行顺序

```
Task 1 ──> Task 2 ──> Task 3 ──> Task 4 ──> Task 5 ──> Task 6 ──> Task 7
                                                                        │
              ┌─────────────────────────────────────────────────────────┘
              ▼
      Task 8 ──> Task 9 ──> Task 10 ──> Task 11
      (后台 Controller 链)

      Task 12 ──> Task 13 ──> Task 14 ──> Task 15 ──> Task 16
      (App 端链，可独立于后台链执行)

      Task 17 (定时任务，依赖 Task 7 中的 RedisUtils 注入)
```
