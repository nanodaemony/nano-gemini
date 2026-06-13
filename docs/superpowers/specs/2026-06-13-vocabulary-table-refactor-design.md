# 词汇后台管理接口表重构设计

## 概述

对词汇后台管理（VocabWordController）涉及的表结构进行重构：
1. 字段命名规范化、字段增删
2. 例句从独立的 `vocab_example` 表抽到共享 `example_sentence` 表
3. 关联词汇从 `vocab_sense` 的 JSON 列迁移到 `vocab_relation` 关联表
4. 词汇练习题目相关逻辑暂不实现

## 变更清单

### 领域实体

| 操作 | 类 | 说明 |
|------|------|------|
| 修改 | `VocabSense.java` | `translations`→`defTranslations`(column `def_translations`)、`defImage`→`defImageId`(column `def_image_id`)、`senseOrder`→column `` `order` ``；删除 `synonyms`/`antonyms`/`relatedForward`/`relatedBackward`/`relatedOther` |
| 修改 | `VocabStructure.java` | `structureOrder`→column `` `order` `` |
| 删除 | `VocabExample.java` | 不再需要独立例句表实体 |
| 删除 | `VocabExercise.java` | 暂不实现练习逻辑 |
| 新建 | `VocabRelation.java` | 映射 `vocab_relation` 表 |

### DTO

| 操作 | 类 | 说明 |
|------|------|------|
| 修改 | `VocabSenseDto` | 字段与实体同步；增加 `defImageSentence`(ExampleSentenceDto)、5 种 `VocabRelationDto` 列表 |
| 修改 | `VocabStructureDto` | `examples`(List\<VocabExampleDto\>) → `structureSentences`(List\<ExampleSentenceDto\>) |
| 修改 | `VocabWordDto` | 删除 `exercises` |
| 删除 | `VocabExampleDto` | |
| 删除 | `VocabExerciseDto` | |
| 新建 | `VocabRelationDto` | |

### Repository

| 操作 | 类 |
|------|------|
| 删除 | `VocabExampleRepository` |
| 删除 | `VocabExerciseRepository` |
| 新建 | `VocabRelationRepository` |

### Service

| 操作 | 类 | 说明 |
|------|------|------|
| 修改 | `VocabWordServiceImpl` | 注入 `ExampleSentenceService`、`VocabRelationRepository`；移除 VocabExample/VocabExercise 相关代码；发布时通过 `ExampleSentenceService.syncOne()` 同步例句、通过新方法同步 `vocab_relation` |
| 新建 | `syncRelations()` 方法（在 VocabWordServiceImpl 内部） |

### Wrapper

`VocabWordWrapper` 删除 exercise/example 转换方法，增加 relation/example_sentence 的 DTO↔VO 转换。

### API 出入参

VocabWordCreateRequest 和 VocabWordVO 的出入参已由用户定义完成，保持不动。

## 核心逻辑

### 草稿工作流（保持不变）

create → update（存草稿） → review → publish（同步子表） → offline

### 例句同步（发布时）

- 结构例句：`SentenceBizTypeEnum.VOCAB_SENSE_STRUCTURE_SENTENCE` + `structureId`
- 释义图片例句：`SentenceBizTypeEnum.VOCAB_SENSE_DEF_IMAGE_SENTENCE` + `senseId`
- 方法：`ExampleSentenceService.syncOne()` 单条同步，`disableByBizIds()` 清理删除的

### 关联词汇同步（发布时）

对每个义项的 5 种关联类型，先软删除该 senseId+type 的旧记录，再批量插入新记录。
