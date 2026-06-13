# 词汇辨析（Vocab Comparison）功能设计

> 日期: 2026-06-13
> 作者: nano
> 状态: 草案

## 1. 概述

词汇辨析功能允许将多个易混淆的词汇组合成一个辨析组（Comparison Group），对组内每个词汇进行用法对比说明，并可选配情景对话，帮助学习者理解词汇间的差异。

整体工作流与现有词汇管理（`VocabWord`）保持一致：**草稿 → 审核 → 发布**。

## 2. 数据表设计

### 2.1 vocab_comparison_group（辨析组主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 辨析组 ID |
| group_key | varchar(64) | 辨析组标识（如"标准vs尺度"），对外展示作为对比头 |
| exercise_question_ids | varchar(256) | 练习题 ID 列表 JSON |
| group_order | int | 排序权重（大在前） |
| draft_content | text | 草稿内容 JSON |
| edit_status | varchar(20) | `draft` / `reviewed` |
| publish_status | varchar(20) | `unpublished` / `published` |
| status | tinyint | `1`=有效 / `0`=无效（软删除） |
| BaseEntity 审计字段 | — | create_by, update_by, create_time, update_time |

### 2.2 vocab_comparison_item（辨析条目表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 条目 ID |
| group_id | bigint FK | 所属辨析组 ID |
| word_id | bigint | 词汇 ID（关联 `vocab_word.id`） |
| word | varchar(50) | 词头（冗余，方便查询和显示） |
| part_of_speech | varchar(50) | 词性 |
| usage_comparison | varchar(512) | 用法对比 |
| usage_comparison_translations | text | 用法对比外文翻译（JSON） |
| common_usage | varchar(512) | 通用用法 |
| common_usage_translations | text | 通用用法外文翻译（JSON） |
| order | int | 组内排序权重 |
| status | tinyint | `1`=有效 / `0`=无效 |
| create_time / update_time | datetime | — |

索引：`idx_group_id`、`idx_word_id`、`idx_word`

### 2.3 vocab_comparison_chat（情景对话表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 对话 ID |
| group_id | bigint FK | 所属辨析组 ID |
| role | varchar(20) | `teacher`=老师 / `student`=学生 |
| content | varchar(1024) | 中文对话内容 |
| example_sentence_id | bigint | 关联 `example_sentence` 表，存储翻译/拼音/音频等富媒体内容 |
| order | int | 组内排序权重 |
| status | tinyint | `1`=有效 / `0`=无效 |
| create_time / update_time | datetime | — |

索引：`idx_group_id`

### 2.4 example_sentence（已有，新增 biz_type 值）

新增 `SentenceBizTypeEnum` 枚举值：`VOCAB_COMPARISON_CHAT("VOCAB_COMPARISON_CHAT", "词汇辨析情景对话, bizId=词汇辨析对话ID")`

每个 chat 记录发布时对应一条 `example_sentence` 记录：
- `biz_type` = `"VOCAB_COMPARISON_CHAT"`
- `biz_id` = `chat.id`
- 存储该句对话的 content、pinyin、translations、audioId

## 3. 工作流与草稿模式

完全对齐 `VocabWord` 的草稿工作流：

### 状态机

```
创建 → [draft] → review → [reviewed] → publish → [published]
                 ↕ (更新则回退到 draft)
[任意状态] → 软删除 (status=0)
[published] → offline → [unpublished]（保留 published 子表数据，仅改发布状态）
```

### 编辑与发布

- **编辑阶段**：所有 item 和 chat 数据存于 `vocab_comparison_group.draft_content`（JSON）
- **审核**：`edit_status` → `reviewed`，业务含义为"内容已确认，可发布"
- **发布**：
  1. 解析 `draft_content`
  2. 更新 group 主表字段（group_key, group_order, exercise_question_ids）
  3. 同步 `vocab_comparison_item`：软删除旧 → 批量新建
  4. 同步 `vocab_comparison_chat` + `example_sentence`：
     - 软删除旧 chats 及关联 example_sentence
     - 逐个创建新 chat 记录，同时创建对应的 example_sentence（含 pinyin、translations、audioId）
     - 回填 `chat.example_sentence_id`
  5. `edit_status` → `published`，`publish_status` → `published`，清空 `draft_content`

### 查询覆盖

- **列表页**：当 `edit_status` 为 `draft` 或 `reviewed` 时，从 `draft_content` 覆盖 `group_key`、`group_order` 等业务字段到 DTO
- **详情页**：当 `edit_status` 为 `draft` 或 `reviewed` 时，直接返回 `draft_content` 反序列化的完整 DTO（含 items/chats）
- **已发布**：从正式表拼接 items + chats（chats 通过 example_sentence 加载翻译/音频）

## 4. 模块结构

### 4.1 grid-system（后台管理模块）

新增以下包路径（与 vocabulary 同级）：

```
com.naon.grid.backend.domain.vocab.comparison/
├── VocabComparisonGroup.java     (DO, extends BaseEntity)
├── VocabComparisonItem.java      (DO)
└── VocabComparisonChat.java      (DO)

com.naon.grid.backend.repo.vocab.comparison/
├── VocabComparisonGroupRepository.java
├── VocabComparisonItemRepository.java
└── VocabComparisonChatRepository.java

com.naon.grid.backend.service.vocab.comparison/
├── VocabComparisonGroupService.java                (接口)
├── impl/VocabComparisonGroupServiceImpl.java       (实现)
└── dto/
    ├── VocabComparisonGroupDto.java
    ├── VocabComparisonItemDto.java
    ├── VocabComparisonChatDto.java
    └── VocabComparisonGroupQueryCriteria.java

com.naon.grid.backend.rest.controller/
├── VocabComparisonController.java                   (后台 API)

com.naon.grid.backend.rest.request/
├── VocabComparisonGroupCreateRequest.java
└── VocabComparisonGroupQueryRequest.java

com.naon.grid.backend.rest.vo/
├── VocabComparisonGroupBaseVO.java
├── VocabComparisonGroupVO.java
├── VocabComparisonItemVO.java
└── VocabComparisonChatVO.java

com.naon.grid.backend.rest.wrapper/
└── VocabComparisonGroupWrapper.java
```

### 4.2 grid-app（用户端模块）

```
com.naon.grid.modules.app.rest/
├── AppVocabComparisonController.java

com.naon.grid.modules.app.rest.vo/
├── AppVocabComparisonGroupVO.java
└── AppVocabComparisonItemVO.java
```

### 4.3 修改已有文件

```
grid-common/src/main/java/com/naon/grid/enums/SentenceBizTypeEnum.java
  → 新增: VOCAB_COMPARISON_CHAT("VOCAB_COMPARISON_CHAT", "词汇辨析情景对话, bizId=词汇辨析对话ID")
```

## 5. API 设计

### 5.1 后台 API (`/api/vocab/comparison`)

| 方法 | 路径 | 说明 | 请求体/参数 | 响应 |
|------|------|------|------------|------|
| POST | `/` | 新增辨析组 | `VocabComparisonGroupCreateRequest` | 201 + id |
| PUT | `/{id}` | 更新辨析组 | `VocabComparisonGroupCreateRequest` | 204 |
| PUT | `/{id}/review` | 审核 | — | 204 |
| PUT | `/{id}/publish` | 发布 | — | 204 |
| PUT | `/{id}/offline` | 下线 | — | 204 |
| GET | `/{id}` | 详情 | — | `VocabComparisonGroupVO` |
| GET | `/` | 分页列表 | `VocabComparisonGroupQueryRequest` | `PageResult<VocabComparisonGroupBaseVO>` |
| DELETE | `/{id}` | 软删除 | — | 204 |

列表查询支持以下筛选条件：
- `word` — 精确匹配 `vocab_comparison_item.word`
- `wordId` — 精确匹配 `vocab_comparison_item.word_id`
- `publishStatus` / `editStatus` — 状态过滤

查询逻辑：先通过 item 表根据 word/wordId 查出 group_id 列表，再在 group 主表做分页查询。

### 5.2 用户端 API (`/api/app/vocab/comparison`)

| 方法 | 路径 | 说明 | 响应 |
|------|------|------|------|
| GET | `/search?word=标准` | 根据词汇精确匹配已发布的辨析组列表 | `List<AppVocabComparisonGroupVO>`（含 items） |
| GET | `/{groupId}` | 查询已发布辨析组详情 | `AppVocabComparisonGroupVO`（含 items + chats） |

用户端 VO 组装：
- items 字段：word_id、word、part_of_speech、usage_comparison、usage_comparison_translations、common_usage、common_usage_translations、order
- chats 字段：role、content、pinyin、translations、audio（含 url）、order

## 6. 关键实现细节

### 6.1 草稿覆盖（列表页）

在 `queryAll` 中，参考 `VocabWordServiceImpl.toDtoWithDraftOverlay`：
- 当 `editStatus` 为 `draft` 或 `reviewed`，从 `draftContent` JSON 覆盖 `groupKey`、`groupOrder` 到 DTO
- 不覆盖：id、status、publishStatus、editStatus、审计字段

### 6.2 发布时的子表同步

```
syncItems(groupId, items):
  1. 软删除 VocabComparisonItem 中该 group_id 的旧记录（status=0）
  2. 遍历 items 逐个创建新记录

syncChats(groupId, chats):
  1. 查出旧 chats（含 id 列表）
  2. 软删除旧 chats（status=0）
  3. 通过 exampleSentenceService.disableByBizIds 软删除旧 example_sentence
  4. 遍历 chats 逐个创建新 chat 记录
  5. 对每个 chat 创建 example_sentence：
     - biz_type = "VOCAB_COMPARISON_CHAT" (SentenceBizTypeEnum.VOCAB_COMPARISON_CHAT.getCode())
     - biz_id = chat.id
     - content = chat.content
     - pinyin = chat.pinyin
     - translations = chat.translations (JSON)
     - audioId = chat.audioId
  6. 将 example_sentence.id 回填到 chat.example_sentence_id
```

### 6.3 查询列表时按 word/wordId 筛选

使用 JPA Specification 或原生查询实现：
1. 若 `word` 或 `wordId` 参数不为空，先从 `vocab_comparison_item` 查 `group_id`
2. 在 Group 主表上包装 `IN (group_ids)` 条件 + 分页

## 7. 未覆盖事项

- 暂无练习题模块的对接（`exercise_question_ids` 字段预留但暂不实现交互逻辑）
- 暂无 Excel 导入功能（后续可基于阿里云 OSS + POI 实现）
