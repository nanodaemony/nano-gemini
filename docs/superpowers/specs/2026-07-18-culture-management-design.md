# 文化点管理功能设计

## 概述

新增"文化点"业务模块，提供文化点（如春节、饺子）的后台管理和 APP 端查询功能。一个文化点关联：文化关键词、学一学例句（example_sentence）、练一练习题（exercise_question）。支持草稿/审核/发布工作流。

## 数据库表

已写入 `sql/biz_culture.sql`，两张表：

### culture — 文化点主表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 文化点ID |
| name | VARCHAR(128) NOT NULL | 名称（饺子、春节） |
| pinyin | VARCHAR(256) | 拼音 |
| audio_id | BIGINT | 名称音频资源ID |
| translations | TEXT | 名称多语言翻译 JSON |
| cover_image_id | BIGINT | 封面图片资源ID |
| level | VARCHAR(20) | 等级：初等/中等/高等 |
| project | VARCHAR(50) | 一级项目（社会交往） |
| category | VARCHAR(50) | 二级项目（饮食、居住、节日） |
| one_sentence_intro | VARCHAR(1024) | 一句话介绍 |
| one_sentence_intro_translations | TEXT | 一句话介绍翻译 JSON |
| one_sentence_intro_audio_id | BIGINT | 一句话介绍音频 |
| one_sentence_intro_image_id | BIGINT | 一句话介绍图片 |
| detailed_intro | TEXT | 详细介绍 |
| detailed_intro_translations | TEXT | 详细介绍翻译 JSON |
| detailed_intro_audio_id | BIGINT | 详细介绍音频 |
| detailed_intro_image_id | BIGINT | 详细介绍图片 |
| sentence_ids | TEXT | 学一学例句ID列表 JSON 数组 |
| question_ids | TEXT | 练一练习题ID列表 JSON 数组 |
| draft_content | TEXT | 草稿 JSON |
| edit_status | VARCHAR(20) DEFAULT 'draft' | draft/reviewed/published |
| publish_status | VARCHAR(20) DEFAULT 'unpublished' | unpublished/published |
| status | TINYINT DEFAULT 1 | 1=有效 0=软删除 |
| create_by/update_by/create_time/update_time | | 审计字段 |

### culture_keyword — 文化关键词表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT PK | 关键词ID |
| culture_id | BIGINT NOT NULL | 所属文化点ID |
| keyword | VARCHAR(128) NOT NULL | 关键词名称（团圆、辞旧迎新） |
| keyword_description | TEXT | 关键词详细说明 |
| keyword_translations | TEXT | 关键词名称翻译 JSON |
| keyword_description_translations | TEXT | 关键词说明翻译 JSON |
| audio_id | BIGINT | 音频资源ID |
| image_id | BIGINT | 图片资源ID |
| order | INT DEFAULT 0 | 排序权重 |
| status | TINYINT DEFAULT 1 | 1=有效 0=软删除 |

### 关联关系

- `sentence_ids` → `example_sentence.id`（学一学例句）
- `question_ids` → `exercise_question.id`（练一练习题）
- 音频引用 `audio_resource.id`，图片引用 `oss_resource_meta.id`

## 管理后台（grid-system）

### CultureController 端点

Base: `/api/culture`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | / | 新增文化点（草稿） |
| PUT | /{id} | 修改文化点内容 |
| PUT | /{id}/review | 审核通过（draft→reviewed） |
| PUT | /{id}/publish | 发布（reviewed→published） |
| PUT | /{id}/offline | 下线 |
| DELETE | /{id} | 软删除 |
| GET | /{id} | 查询详情（草稿覆盖） |
| GET | / | 分页列表（草稿覆盖） |

### 草稿工作流

完全对标 GrammarPoint 的实现模式：

- **CREATE**：status=ENABLED, publishStatus=UNPUBLISHED, editStatus=DRAFT。整个请求体序列化为 `draft_content` JSON。同时写入 `name` 字段（方便搜索）。
- **UPDATE**：覆写 `draft_content`。如果当前 editStatus 为 REVIEWED/PUBLISHED，回退为 DRAFT。
- **READ 列表**：对每条记录判断 editStatus。如果 DRAFT/REVIEWED，解析 `draft_content` 覆盖 DTO 的业务字段（name/level/project/category），计算 keywordCount。如果 PUBLISHED，从 DB 字段取值。
- **READ 详情**：DRAFT/REVIEWED 时从 `draft_content` 直接反序列化 CultureDto（含嵌套 keywords 列表），只从主表实体覆盖 id/status/editStatus/publishStatus/createBy/updateBy/时间。PUBLISHED 时查主表+子表组装。
- **REVIEW**：editStatus DRAFT→REVIEWED，校验 draft_content 非空。
- **PUBLISH**：editStatus REVIEWED→PUBLISHED。解析 draft_content，回写主表字段（name/pinyin/translations/level/project/category/coverImageId/oneSentenceIntro/detailedIntro/sentenceIds/questionIds 等），同步子表 culture_keyword（增/改/删），材料化 AI content markers，清空 draft_content。
- **OFFLINE**：publishStatus→UNPUBLISHED，不删子表。
- **DELETE**：status→DISABLED，不删子表。

### 子表同步说明

- **culture_keyword**：发布时走 sync 模式。查现有 enabled 行 → draft 中有 id 则 UPDATE，无 id 则 INSERT，DB 中有但 draft 中无则 soft DELETE。
- **sentence_ids / question_ids**：这些 ID 在前端提交时已存在（先调 example_sentence/exercise_question 保存接口拿 ID），发布时直接写入主表字段，不需要额外 sync。

### 文件清单

```
grid-system/src/main/java/com/naon/grid/backend/
├── domain/culture/
│   ├── Culture.java
│   └── CultureKeyword.java
├── repo/culture/
│   ├── CultureRepository.java
│   └── CultureKeywordRepository.java
├── service/culture/
│   ├── CultureService.java                 (接口)
│   ├── impl/CultureServiceImpl.java        (实现)
│   ├── dto/CultureDto.java
│   ├── dto/CultureKeywordDto.java
│   ├── dto/CultureQueryCriteria.java
│   └── mapstruct/CultureMapper.java
└── rest/
    ├── controller/CultureController.java
    ├── request/CultureCreateRequest.java
    ├── request/CultureQueryRequest.java
    ├── vo/CultureCreateVO.java
    ├── vo/CultureBaseVO.java
    ├── vo/CultureVO.java
    └── wrapper/CultureWrapper.java
```

## APP 端（grid-app）

### AppCultureController 端点

Base: `/api/app/culture`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /search?blurry= | 模糊搜索已发布文化点 |
| GET | /{id}?language= | 查询文化详情（翻译过滤+资源URL） |

### 详情接口流程

1. `CultureService.findPublishedById(id)` → CultureDto
2. Controller 预加载资源：收集所有 audio_id/image_id（主表 + keywords + sentences 的 audio/image），批量查询 `AudioResourceService.findByIds()` 和 `AliOssStorageService.findByIds()`
3. `AppCultureWrapper.toDetailVO(dto, audioMap, imageMap, language)` → AppCultureDetailVO

### AppCultureDetailVO 结构

```
AppCultureDetailVO {
    id, name, pinyin, audio{audioUrl}, translation,
    level, project, category,
    coverImage{imageUrl},
    oneSentenceIntro, oneSentenceIntroTranslation, oneSentenceIntroAudio{audioUrl}, oneSentenceIntroImage{imageUrl},
    detailedIntro, detailedIntroTranslation, detailedIntroAudio{audioUrl}, detailedIntroImage{imageUrl},
    keywords: [{keyword, keywordDescription, translation, descriptionTranslation, audio{audioUrl}, image{imageUrl}, order}],
    sentences: [{sentence, pinyin, audio{audioUrl}, translation, image{imageUrl}, order}],
    questions: [{...AppExerciseQuestionDetailVO}]
}
```

### 关键规则

- **翻译过滤**：所有 translations 字段只保留匹配 `language` 参数的那一条
- **资源 URL**：audio_id/image_id 全部转为嵌套 AudioVO/ImageVO（只有 URL），不暴露内部 ID
- **错误处理**：资源查不到 → log.error → 字段 null，不抛异常
- **管理字段隔离**：不暴露 createBy/updateBy/createTime/updateTime/editStatus/publishStatus/status
- **VO 独立定义**：APP 侧 VO 不复用后台 VO，字段精简

### 文件清单

```
grid-app/src/main/java/com/naon/grid/modules/app/rest/
├── AppCultureController.java
├── request/AppCultureSearchRequest.java
├── vo/AppCultureBaseVO.java
├── vo/AppCultureDetailVO.java
└── wrapper/AppCultureWrapper.java
```

## 收藏夹扩展

修改 3 个现有文件：

- **CollectionBizTypeEnum** — 新增 `CULTURE("CULTURE", "文化")`
- **CollectionWrapper.resolveContentName()** — 新增 `case "CULTURE": cultureService.findById() → dto.getName()`
- **CollectionServiceImpl.validateContentExists()** — 新增 `case "CULTURE": cultureService.findPublishedById()`

## 关键设计决策

| 决策 | 结论 | 原因 |
|---|---|---|
| 复用 example_sentence 表 | ✅ | 已有 sentence/pinyin/audio/translations/image 字段，完全匹配 |
| 复用 exercise_question 表 | ✅ | 已有完整题型结构，CultureService.findPublishedById 用 ExerciseQuestionService.findPublishedByIds 批量查 |
| sentence_ids/question_ids 存主表 | ✅ | 非子表关联，直接用 JSON 数组字段更简单，对标 vocab_structure.sentence_ids |
| 关键词独立子表 | ✅ | 对标 topic_pattern，有排序，发布时同步 |
| name 不加唯一约束 | ✅ | 和 topic 一致，文化点名称允许重复 |
| 封面图 | ✅ cover_image_id | 对标 topic.cover_image_id |
