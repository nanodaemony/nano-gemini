# 每日一词 — 设计文档

> **版本**: v1.0  
> **日期**: 2026-07-11  
> **需求来源**: 《有路中文 PC Web 端 C 端交互需求汇总》Section 8  
> **需求文档**: `E:\KnowledgeOcean\nano-grid\A-有路中文\待办需求\每日一词-需求说明.md`

---

## 一、产品概述

"每日一词"是首页轻量化内容入口，平台主动推送中文表达给用户。核心交互：查看今日卡片 → 换一换 → 收藏 → 分享 → 历史归档。

**关键设计决策：**

| 决策点 | 结论 |
|--------|------|
| `related_word_id` | 一对一关联 `vocab_word.id` |
| 后台管理方式 | 列表管理，无日历视图 |
| 分享卡片 | 前端调用文生图接口生成，后端保存图片 ID |
| 权限控制 | 简化为只区分登录/未登录 |
| 表命名 | `daily_vocabulary`（无 `biz_` 前缀） |

---

## 二、数据库设计

### 2.1 表：`daily_vocabulary`

SQL 写入 `sql/biz_vocabulary.sql`：

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

### 2.2 枚举：`DailyVocabularyTypeEnum`

位置：`grid-common/src/main/java/com/naon/grid/enums/DailyVocabularyTypeEnum.java`

| Code | 中文 | 说明 |
|------|------|------|
| IDIOM | 成语 | 四字固定成语 |
| PROVERB | 谚语 | 含有哲理的固定语句 |
| COLLOQUIALISM | 惯用语 | 口语化固定表达 |
| XIEHOUYU | 歇后语 | 前喻后解 |
| NEOLOGISM | 新词新语 | 当代流行语 |

---

## 三、grid-system 后台模块设计

### 3.1 文件清单

```
grid-common/src/main/java/com/naon/grid/enums/
├── DailyVocabularyTypeEnum.java          # 类型枚举

grid-system/src/main/java/com/naon/grid/backend/
├── domain/vocabulary/
│   └── DailyVocabulary.java             # JPA 实体
├── repo/vocabulary/
│   └── DailyVocabularyRepository.java    # Spring Data JPA Repository
├── service/vocabulary/
│   ├── DailyVocabularyService.java       # Service 接口
│   ├── impl/
│   │   └── DailyVocabularyServiceImpl.java  # Service 实现
│   └── dto/
│       ├── DailyVocabularyDto.java       # 全量 DTO
│       └── DailyVocabularyQueryCriteria.java  # 查询条件
├── rest/controller/
│   └── DailyVocabularyController.java    # 后台 Controller
├── rest/request/
│   ├── DailyVocabularyCreateRequest.java # 创建/编辑请求
│   └── DailyVocabularyQueryRequest.java  # 列表查询请求
├── rest/vo/
│   ├── DailyVocabularyBaseVO.java        # 列表项 VO
│   ├── DailyVocabularyVO.java            # 详情 VO
│   └── DailyVocabularyCreateVO.java      # 创建返回 VO
└── rest/wrapper/
    └── DailyVocabularyWrapper.java       # Request→DTO / DTO→VO 转换
```

### 3.2 实体 `DailyVocabulary`

继承 `BaseEntity`，`@Table(name = "daily_vocabulary")`。字段一一对应数据表，使用 `@Column` 映射蛇形命名。

- `id`: `Integer`, `@GeneratedValue(IDENTITY)`
- `status`: 默认 `StatusEnum.ENABLED.getCode()` (1)
- `publishStatus`: 默认 `PublishStatusEnum.UNPUBLISHED.getCode()`
- `editStatus`: 默认 `EditStatusEnum.DRAFT.getCode()`
- `draftContent`: `@Column(columnDefinition = "text")`

### 3.3 Repository `DailyVocabularyRepository`

```java
public interface DailyVocabularyRepository
    extends JpaRepository<DailyVocabulary, Integer>,
            JpaSpecificationExecutor<DailyVocabulary> {

    // 查今日已发布内容（按 sort_order 排序）
    List<DailyVocabulary> findByDisplayDateAndPublishStatusAndStatus(
        LocalDate displayDate, String publishStatus, Integer status, Sort sort);

    // 按月份查有内容的日期
    @Query("SELECT DISTINCT d.displayDate FROM DailyVocabulary d WHERE ...")
    List<LocalDate> findDistinctDisplayDatesByMonth(...);
}
```

### 3.4 Service 接口

| 方法 | 返回 | 说明 |
|------|------|------|
| `queryAll(DailyVocabularyQueryCriteria, Pageable)` | `PageResult<DailyVocabularyDto>` | 分页查询列表 |
| `findById(Integer id)` | `DailyVocabularyDto` | 详情（含草稿覆盖） |
| `findPublishedById(Integer id)` | `DailyVocabularyDto` | 已发布详情 |
| `create(DailyVocabularyDto dto)` | `Integer` | 创建，返回 ID |
| `update(Integer id, DailyVocabularyDto dto)` | `void` | 更新草稿 |
| `reviewDraft(Integer id)` | `void` | 审核 draft→reviewed |
| `publishDraft(Integer id)` | `void` | 发布 reviewed→published |
| `offline(Integer id)` | `void` | 下线 |
| `delete(Integer id)` | `void` | 软删除 status=0 |
| `schedule(Integer id, LocalDate date)` | `void` | 设展示日期 |
| `batchSchedule(List<Integer> ids, List<LocalDate> dates)` | `void` | 批量排期 |
| `getTodayMain()` | `DailyVocabularyDto` | 今日主推 |
| `getTodayBackups()` | `List<DailyVocabularyDto>` | 今日备选池 |
| `queryHistory(DailyVocabularyQueryCriteria, Pageable)` | `PageResult<DailyVocabularyDto>` | 历史归档 |
| `getCalendarDates(int year, int month)` | `List<LocalDate>` | 月有内容日期 |

**草稿/Publish 逻辑**（与 VocabWordServiceImpl 一致）：
- **create**: 写入实体基础字段，序列化完整 DTO 到 `draftContent` JSON
- **update**: 更新 `draftContent`，若当前 `reviewed` 则回退到 `draft`
- **publish**: `draftContent` JSON 反序列化后写入实际列（phrase, pinyin, plainExplanation 等），同步 `example_sentence_id`，清除 `draftContent`。需要 published 状态的 `example_sentence` 也一并处理
- **queryAll (draft 态)**: 若非 published，从 `draftContent` JSON 提取字段覆盖到 DTO 返回

### 3.5 Controller 后台 API

`@RequestMapping("/api/daily-vocabulary")`

| 方法 | 路径 | 说明 | 参照 |
|------|------|------|------|
| POST | `/` | 创建 | VocabWordController.create |
| PUT | `/{id}` | 编辑 | VocabWordController.update |
| GET | `/{id}` | 详情 | VocabWordController.findById |
| GET | `/` | 分页列表 | VocabWordController.queryAll |
| PUT | `/{id}/review` | 审核通过 | VocabWordController.reviewDraft |
| PUT | `/{id}/publish` | 发布 | VocabWordController.publishVocab |
| PUT | `/{id}/offline` | 下线 | VocabWordController.offline |
| DELETE | `/{id}` | 删除 | VocabWordController.delete |
| PUT | `/{id}/schedule` | 设置展示日期 | 新增 |
| POST | `/batch-schedule` | 批量排期 | 新增 |

后台 VO 下发全部语言译文，不做语言筛选。

### 3.6 DTO/VO/Request 设计

**`DailyVocabularyDto`** — 全量 DTO：
- 基础字段：id, phrase, phraseType, pinyin, audioId, imageId
- 内容字段：plainExplanation, originStory, displayDate, sortOrder, relatedWordId
- 翻译字段 (JSON 序列化)：phraseTranslations (`List<TextTranslation>`), explanationTranslations (`List<TextTranslation>`)
- 嵌套：exampleSentence (`ExampleSentenceDto`)
- 审计：继承 BaseDTO (createBy, updateBy, createTime, updateTime)
- 状态：publishStatus, editStatus

**`DailyVocabularyQueryCriteria`**：
- blurry (String, `@Query(type = LIKE, prop = "phrase"`)
- phraseType (String)
- publishStatus (String)
- displayDateStart (LocalDate, `@Query(prop = "displayDate", type = GREATER_OR_EQ)`)
- displayDateEnd (LocalDate, `@Query(prop = "displayDate", type = LESS_OR_EQ)`)
- publishedOnly (Boolean) — App 端使用，仅查已发布

**`DailyVocabularyCreateRequest`**：
```
phrase, phraseType, pinyin, phraseTranslations(List<TextTranslationRequest>),
audioId, imageId, plainExplanation, explanationTranslations, originStory,
exampleSentence(ExampleSentenceRequest), displayDate, sortOrder, relatedWordId
```

**`DailyVocabularyQueryRequest`**：
```
blurry, phraseType, publishStatus, displayDateStart, displayDateEnd
```

**`DailyVocabularyBaseVO`**（列表项）：
```
id, phrase, phraseType, pinyin, displayDate, sortOrder,
publishStatus, editStatus, createBy, updateBy, createTime, updateTime
```

**`DailyVocabularyVO`**（详情）：
```
BaseVO 所有字段 + phraseTranslations, audioId, imageId,
plainExplanation, explanationTranslations, originStory,
exampleSentence(ExampleSentenceVO), relatedWordId
```

**`DailyVocabularyCreateVO`**：`id` 单字段

### 3.7 Wrapper `DailyVocabularyWrapper`

静态工具类，`public static` 方法：
- `toCriteria(DailyVocabularyQueryRequest)` → `DailyVocabularyQueryCriteria`
- `toDto(DailyVocabularyCreateRequest)` → `DailyVocabularyDto`
- `toVO(DailyVocabularyDto)` → `DailyVocabularyVO`
- `toBaseVOList(List<DailyVocabularyDto>)` → `List<DailyVocabularyBaseVO>`

---

## 四、grid-app App 模块设计

### 4.1 文件清单

```
grid-app/src/main/java/com/naon/grid/modules/app/
├── enums/
│   └── CollectionBizTypeEnum.java        # 扩展 DAILY_VOCABULARY
├── rest/
│   ├── AppDailyVocabularyController.java # App Controller
│   ├── request/
│   │   ├── AppDailyVocabularyHistoryRequest.java  # 历史查询
│   │   └── AppDailyVocabularyShareImageRequest.java  # 分享图保存
│   ├── vo/
│   │   ├── AppDailyVocabularyTodayVO.java    # 今日（主推+备选）
│   │   ├── AppDailyVocabularyBaseVO.java     # 列表项
│   │   └── AppDailyVocabularyDetailVO.java   # 详情（语言筛选）
│   └── wrapper/
│       └── AppDailyVocabularyWrapper.java    # DTO→VO 转换
```

### 4.2 Controller API

`@RequestMapping("/api/app/daily-vocabulary")`

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/today` | 今日主推+全部备选（一次下发） | 无需登录 |
| GET | `/{id}` | 详情（参数: `language`） | 无需登录 |
| GET | `/history` | 历史归档分页 | 无需登录 |
| GET | `/calendar` | 月有内容日期列表 | 无需登录 |
| POST | `/{id}/share-image` | 保存分享图ID | 需登录 |

**今日接口逻辑**：
1. 调用 `DailyVocabularyService.getTodayMain()` 取主推
2. 调用 `DailyVocabularyService.getTodayBackups()` 取备选池
3. 收集音频/图片 ID，批量查询 `AudioResourceService` / `AliOssStorageService`
4. 传入 Wrapper 组装 `AppDailyVocabularyTodayVO`

**详情接口逻辑**：
1. 调用 `DailyVocabularyService.findPublishedById(id)`
2. 收集音频/图片 ID（词目音频 + 例句音频 + 配图）
3. 批量查询资源
4. 传入 Wrapper + `language` 组装 `AppDailyVocabularyDetailVO`

**历史归档逻辑**：
1. 校验 `publishedOnly` 强制为 true
2. 分页查询，仅返回已发布内容
3. 批量查询图片 URL，组装 `List<AppDailyVocabularyBaseVO>`

### 4.3 App Request 类

**`AppDailyVocabularyHistoryRequest`**：
```java
private String phraseType;
private String keyword;
private String month;  // yyyy-MM
private Integer page = 0;
private Integer size = 20;
```

**`AppDailyVocabularyShareImageRequest`**：
```java
private Long imageId;
```

### 4.4 App VO 类（语言筛选，不含审计/状态字段）

**`AppDailyVocabularyTodayVO`**：
```java
AppDailyVocabularyDetailVO main;           // 主推
List<AppDailyVocabularyDetailVO> backups;   // 备选池
```

**`AppDailyVocabularyBaseVO`**（历史列表）：
```java
Long id;
String phrase;
String phraseType;
String pinyin;
String imageUrl;        // 图片 URL（非 ID）
LocalDate displayDate;
```

**`AppDailyVocabularyDetailVO`**（详情/今日卡片）：
```java
Long id;
String phrase;
String phraseType;
String pinyin;
TextTranslationVO phraseTranslation;    // 按语言筛选，单项
String plainExplanation;
TextTranslationVO explanationTranslation; // 按语言筛选，单项
String originStory;
ExampleSentenceVO exampleSentence;       // 含 sentence, pinyin, translation(筛选), audioUrl
AudioVO audio;                            // 词目发音 URL
ImageVO image;                            // 配图 URL
LocalDate displayDate;
Long relatedWordId;
```

**内嵌 VO**（与 AppVocabWordDetailVO 一致模式）：
```java
// 静态内部类
class TextTranslationVO { String language; String translation; }
class AudioVO { String audioUrl; }
class ImageVO { String imageUrl; }
class ExampleSentenceVO { 
    String sentence; String pinyin; 
    TextTranslationVO translation;  // 筛选
    AudioVO audio; ImageVO image; 
}
```

**设计要点**：
- 不含 `createBy`, `updateBy`, `createTime`, `updateTime`
- 不含 `editStatus`, `publishStatus`, `draftContent`, `status`, `sortOrder`
- 所有翻译从 `List<TextTranslation>` 按 `language` 筛选为单个 `TextTranslationVO`
- 资源字段给 URL 不给 ID（减少 App 端额外请求）
- 使用 `Long` 类型 ID（与表 `bigint` 对齐）

### 4.5 Wrapper `AppDailyVocabularyWrapper`

```java
public class AppDailyVocabularyWrapper {
    public static AppDailyVocabularyTodayVO toTodayVO(
        DailyVocabularyDto main, List<DailyVocabularyDto> backups,
        Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap,
        String language);

    public static AppDailyVocabularyDetailVO toDetailVO(
        DailyVocabularyDto dto,
        Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap,
        String language);

    public static List<AppDailyVocabularyBaseVO> toBaseVOList(
        List<DailyVocabularyDto> dtos, Map<Long, AliOssStorageDto> imageMap);

    // 私有：按语言筛翻译
    private static TextTranslationVO filterByLanguage(
        List<TextTranslation> translations, String language);
}
```

---

## 五、缓存与定时任务

### 5.1 Redis 缓存

| Key | Value | TTL |
|-----|-------|-----|
| `daily_vocabulary:today:main` | 主推 DTO JSON | 到次日 00:00 |
| `daily_vocabulary:today:backups` | 备选池 DTO 列表 JSON | 到次日 00:00 |

### 5.2 定时刷新

```java
@Scheduled(cron = "0 0 0 * * ?")  // 每日 00:00
public void refreshTodayCache() {
    // 清除今日缓存，下次请求时懒加载
    redisUtils.del("daily_vocabulary:today:main");
    redisUtils.del("daily_vocabulary:today:backups");
}
```

### 5.3 缓存失效触发

- 发布新内容（publish）→ 若 `display_date` 是今天 → 清今日缓存
- 下线内容（offline）→ 若 `display_date` 是今天 → 清今日缓存
- 修改 `display_date`（schedule/batchSchedule）→ 清今日缓存

---

## 六、收藏扩展

`CollectionBizTypeEnum` 新增：

```java
DAILY_VOCABULARY("DAILY_VOCABULARY", "每日一词")
```

App 端收藏/取消收藏复用现有 `AppCollectionController.addItem` / `removeItem`，前端传入：
- `bizType`: `"DAILY_VOCABULARY"`
- `bizId`: `daily_vocabulary.id`

---

## 七、App 权限控制

简化为两级：

| 状态 | 今日每日一词 | 换一换 | 历史 | 收藏/分享 |
|------|------------|--------|------|----------|
| 未登录 | ✅ | ✅ | ✅ | ❌ |
| 已登录 | ✅ | ✅ | ✅ | ✅ |

无需 entitlement 检查，仅通过 `AppSecurityUtils.getCurrentUserId()` 判断登录态。

---

## 八、文件变更总览

### 新建文件

| 模块 | 文件 | 说明 |
|------|------|------|
| grid-common | `enums/DailyVocabularyTypeEnum.java` | 类型枚举 |
| grid-system | `domain/vocabulary/DailyVocabulary.java` | 实体 |
| grid-system | `repo/vocabulary/DailyVocabularyRepository.java` | Repository |
| grid-system | `service/vocabulary/DailyVocabularyService.java` | Service 接口 |
| grid-system | `service/vocabulary/impl/DailyVocabularyServiceImpl.java` | Service 实现 |
| grid-system | `service/vocabulary/dto/DailyVocabularyDto.java` | DTO |
| grid-system | `service/vocabulary/dto/DailyVocabularyQueryCriteria.java` | 查询条件 |
| grid-system | `rest/controller/DailyVocabularyController.java` | 后台 Controller |
| grid-system | `rest/request/DailyVocabularyCreateRequest.java` | 创建 Request |
| grid-system | `rest/request/DailyVocabularyQueryRequest.java` | 查询 Request |
| grid-system | `rest/vo/DailyVocabularyBaseVO.java` | 列表 VO |
| grid-system | `rest/vo/DailyVocabularyVO.java` | 详情 VO |
| grid-system | `rest/vo/DailyVocabularyCreateVO.java` | 创建返回 VO |
| grid-system | `rest/wrapper/DailyVocabularyWrapper.java` | 后台 Wrapper |
| grid-app | `rest/AppDailyVocabularyController.java` | App Controller |
| grid-app | `rest/request/AppDailyVocabularyHistoryRequest.java` | App 查询 Request |
| grid-app | `rest/request/AppDailyVocabularyShareImageRequest.java` | App 分享图 Request |
| grid-app | `rest/vo/AppDailyVocabularyTodayVO.java` | 今日 VO |
| grid-app | `rest/vo/AppDailyVocabularyBaseVO.java` | 列表 VO |
| grid-app | `rest/vo/AppDailyVocabularyDetailVO.java` | 详情 VO |
| grid-app | `rest/wrapper/AppDailyVocabularyWrapper.java` | App Wrapper |

### 修改文件

| 模块 | 文件 | 变更 |
|------|------|------|
| grid-app | `enums/CollectionBizTypeEnum.java` | 新增 `DAILY_VOCABULARY` |
| sql | `biz_vocabulary.sql` | 新增 `daily_vocabulary` 建表 DDL |

共 **21 个新建文件 + 2 个修改文件**。

---

## 九、验收标准对照

| 编号 | 验收项 | 实现对应 |
|------|--------|---------|
| AC-1 | 首页卡片 | `GET /api/app/daily-vocabulary/today` |
| AC-2 | 换一换 | today 接口一次返回 main+backups，前端本地切换 |
| AC-3 | 音频播放 | detail 接口返回 audioUrl |
| AC-4 | 收藏 | 复用 AppCollectionController，扩展 DAILY_VOCABULARY |
| AC-5 | 分享 | `POST /{id}/share-image` 保存前端生成的图片 ID |
| AC-6 | 历史归档 | `GET /api/app/daily-vocabulary/history` |
| AC-7 | 日历视图 | `GET /api/app/daily-vocabulary/calendar` |
| AC-8 | 跨板块跳转 | `relatedWordId` 字段，前端跳转词汇详情 |
| AC-9 | 权限控制 | 登录/未登录两级 |
| AC-10 | 后台 CRUD | 完整 8 个管理端接口 |
| AC-11 | 排期逻辑 | `@Scheduled` 凌晨刷新 + 发布/下线清缓存 |
| AC-12 | 缓存 | Redis 今日缓存，TTL 次日 00:00 |

---

*此文档为设计规格（spec），下一步进入 writing-plans 生成实施计划。*
