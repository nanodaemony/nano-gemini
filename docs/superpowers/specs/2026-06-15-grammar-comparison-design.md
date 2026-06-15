# 语法辨析管理后台设计文档

## 概述

实现语法辨析（Grammar Comparison）管理后台功能，支持对语法点进行辨析对比的管理。语法辨析将多个易混淆的语法点放在一个"辨析组"中进行对比说明，每个组包含多个语法条目和情景对话。

## 技术架构

### 分层结构（与词汇辨析对齐）

```
Controller 层: GrammarComparisonController
  ↓ ↑ Request/VO
Wrapper 层:   GrammarComparisonGroupWrapper (request↔dto↔vo)
  ↓ ↑ DTO
Service 层:   GrammarComparisonGroupService → GrammarComparisonGroupServiceImpl
  ↓ ↑ Entity/ID
Repository 层: GrammarComparisonGroupRepository / ItemRepository / ChatRepository
  ↓
Domain 层 (DO): GrammarComparisonGroup / GrammarComparisonItem / GrammarComparisonChat
```

### 包路径

```
grid-system/src/main/java/com/naon/grid/backend/
├── domain/grammarcomparison/
│   ├── GrammarComparisonGroup.java      (DO, 继承 BaseEntity)
│   ├── GrammarComparisonItem.java        (DO)
│   └── GrammarComparisonChat.java        (DO)
├── repo/grammarcomparison/
│   ├── GrammarComparisonGroupRepository.java
│   ├── GrammarComparisonItemRepository.java
│   └── GrammarComparisonChatRepository.java
├── service/grammarcomparison/
│   ├── GrammarComparisonGroupService.java          (接口)
│   ├── dto/
│   │   ├── GrammarComparisonGroupDto.java         (继承 BaseDTO)
│   │   ├── GrammarComparisonItemDto.java
│   │   ├── GrammarComparisonChatDto.java
│   │   └── GrammarComparisonGroupQueryCriteria.java
│   └── impl/
│       └── GrammarComparisonGroupServiceImpl.java
└── rest/
    ├── controller/
    │   └── GrammarComparisonController.java
    ├── request/
    │   ├── GrammarComparisonGroupCreateRequest.java
    │   └── GrammarComparisonGroupQueryRequest.java
    ├── vo/
    │   ├── GrammarComparisonGroupVO.java
    │   ├── GrammarComparisonGroupBaseVO.java
    │   ├── GrammarComparisonGroupCreateVO.java
    │   ├── GrammarComparisonItemVO.java
    │   └── GrammarComparisonChatVO.java
    └── wrapper/
        └── GrammarComparisonGroupWrapper.java
```

## 数据模型

### 数据库表（已有 SQL）

#### grammar_comparison_group（辨析组主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK) | AUTO_INCREMENT |
| group_key | VARCHAR(64) | 辨析组标识，如"会vs能" |
| exercise_question_ids | VARCHAR(256) | 练习题ID列表JSON |
| `order` | INT | 排序权重（大在前） |
| draft_content | TEXT | 草稿内容JSON |
| edit_status | VARCHAR(20) | draft / reviewing |
| publish_status | VARCHAR(20) | unpublished / published |
| create_by / update_by | VARCHAR(255) | 审计字段（继承BaseEntity） |
| create_time / update_time | DATETIME | 审计字段（继承BaseEntity） |
| status | TINYINT | 1-有效 0-无效 |

#### grammar_comparison_item（辨析条目表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK) | AUTO_INCREMENT |
| group_id | BIGINT | 所属辨析组ID |
| grammar_id | BIGINT | 语法点ID |
| grammar_name | VARCHAR(50) | 语法点名称（冗余） |
| usage_comparison | VARCHAR(2048) | 用法对比说明 |
| usage_comparison_translations | TEXT | 用法对比外文翻译（JSON数组） |
| example_sentences | TEXT | 例句（每行一条，含✓✗标记） |
| usage_sentence_id | BIGINT | 用法例句ID（关联example_sentence表） |
| `order` | INT | 组内排序权重（大在前） |
| status | TINYINT | 1-有效 0-无效 |

#### grammar_comparison_chat（情景对话表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (PK) | AUTO_INCREMENT |
| group_id | BIGINT | 所属辨析组ID |
| role | VARCHAR(20) | teacher / student |
| content | VARCHAR(1024) | 中文对话内容 |
| example_sentence_id | BIGINT | 关联example_sentence表 |
| `order` | INT | 排序权重（大在前） |
| status | TINYINT | 1-有效 0-无效 |

## API 接口设计

### 基础路径：`/api/grammar/comparison`

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/grammar/comparison` | 创建辨析组 | GrammarComparisonGroupCreateRequest | GrammarComparisonGroupCreateVO |
| PUT | `/api/grammar/comparison/{id}` | 更新辨析组 | GrammarComparisonGroupCreateRequest | 204 No Content |
| PUT | `/api/grammar/comparison/{id}/review` | 审核（draft→reviewed） | - | 204 No Content |
| PUT | `/api/grammar/comparison/{id}/publish` | 发布（reviewed→published） | - | 204 No Content |
| GET | `/api/grammar/comparison/{id}` | 查询详情 | - | GrammarComparisonGroupVO |
| GET | `/api/grammar/comparison` | 分页列表 | GrammarComparisonGroupQueryRequest + Pageable | PageResult\<GrammarComparisonGroupBaseVO\> |
| DELETE | `/api/grammar/comparison/{id}` | 删除 | - | 204 No Content |
| PUT | `/api/grammar/comparison/{id}/offline` | 下线 | - | 204 No Content |

## 草稿工作流

完全对齐词汇辨析的发布流程：

### 创建/更新
1. 接收请求 → Wrapper 转为 DTO
2. 将完整 DTO（含 items 和 chats）序列化为 JSON 
3. 存入主表的 `draft_content` 列
4. `edit_status` = `draft`，`publish_status` = `unpublished`

### 审核
1. `edit_status` 从 `draft` → `reviewed`（仅更新状态）

### 发布
1. 从 `draft_content` 反序列化为 DTO
2. 将 `group_key`、`exercise_question_ids`、`order` 回写到主表字段
3. 同步 `grammar_comparison_item` 子表：软删除旧行 + 创建新行
4. 同步 `grammar_comparison_chat` 子表：软删除旧行 + 创建新行
5. 关联的句子通过 `ExampleSentenceService.save()` 写入 `example_sentence` 表
6. 清空 `draft_content`，`publish_status` = `published`

### 下线
1. `publish_status` = `unpublished`（保留已发布数据，只是不对外展示）

## 草稿优先查询（与汉字模块对齐）

### 分页列表查询
1. 通过 JPA Specification 正常查询主表（含过滤条件、分页）
2. 对每条结果调用 `toDtoWithDraftOverlay`：
   - 如 `editStatus` 为 `draft`/`reviewed` → 从 `draftContent` JSON 反序列化，覆盖业务字段到 DTO
   - 如 `published` → 使用主表字段
3. 填充每个组的条目数量

### 详情查询
1. 按 ID 查询实体
2. 如 `draft`/`reviewed` → 从 `draftContent` 反序列化完整 DTO（含 items 和 chats）
3. 如 `published` → 加载主表字段 + 子表数据（含 `example_sentence` 关联）

## ExampleSentence 使用规则

- `grammar_comparison_item.usage_sentence_id`：关联 `example_sentence` 表，存储用法例句
- `grammar_comparison_chat.example_sentence_id`：关联 `example_sentence` 表，存储对话例句
- 发布时通过 `ExampleSentenceService.save()` 持久化例句数据（文案、翻译、拼音、音频等）
- 查询时通过 `ExampleSentenceService.findByIds()` 批量加载

## 与词汇辨析的关键差异

| 维度 | 词汇辨析 | 语法辨析 |
|------|---------|---------|
| 条目关联 | word_id + word + part_of_speech | grammar_id + grammar_name |
| 条目字段 | usage_comparison + common_usage | usage_comparison（无 common_usage） |
| 句子字段 | 无独立句子文本 | `example_sentences`（TEXT，含✓✗标记的格式化文本） |
| 句子ID | 无 | `usage_sentence_id`（关联 example_sentence） |
| 分组标识 | group_key（词汇组） | group_key（语法组） |
| 查询过滤 | word / wordId | grammarId / groupKey |

## 注意事项（非功能性）

- 所有接口使用 `@Anonymous*Mapping` 注解（与现有风格一致）
- 软删除使用 `StatusEnum.ENABLED`/`DISABLED`
- 排序默认按 `group_order` 降序
- 请求参数使用 `@Valid` 校验
- 分页使用 Spring Data `Pageable`
- 不使用 JPA `@ManyToOne` 关联，通过 ID 手动查询
- JSON 序列化使用 Fastjson2 的 `JsonUtils`
