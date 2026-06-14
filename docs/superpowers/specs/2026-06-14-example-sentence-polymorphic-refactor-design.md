# ExampleSentence 多态关联重构设计

## 背景

当前 `example_sentence` 表使用 `biz_type` + `biz_id` 的多态关联模式指向不同的业务表。这是一种数据库设计 anti-pattern，导致：

- 无参照完整性（biz_type 是字符串，输入错误即产生脏数据）
- 查询业务实体的例句时需要二次查找（如 `hydrateWordSentences`）
- 每新增一种业务类型都要理解 biz_type 枚举语义

本次重构的目标：**业务实体直接持有 ExampleSentence 的主键引用**，即"业务依赖例句"而非"例句指向业务"。

## 涉及范围

### 1:1 关系 —— 业务表新增 FK 列

| 业务表 | 新增字段 | 意义 |
|--------|----------|------|
| `char_word` | `sentence_id` bigint | 组词-例句 1:1 |
| `vocab_sense` | `def_image_sentence_id` bigint | 义项释义图片-例句 0..1 |
| `vocab_comparison_chat` | 已有 `example_sentence_id` | 保持不动，但不再写 biz_type |

### 1:N 关系 —— ExampleSentence 新增 FK 列

| 业务表 | ExampleSentence 新增字段 | 意义 |
|--------|------------------------|------|
| `vocab_structure` | `structure_id` bigint | 结构 → 多个例句 |

### ExampleSentence 表

| 变更 | 字段 |
|------|------|
| 删除 | `biz_type` varchar(64) NOT NULL |
| 删除 | `biz_id` bigint NOT NULL |
| 新增 | `structure_id` bigint DEFAULT NULL |

**注意**：由于 `biz_type`/`biz_id` 被删除，语法模块（GRAMMAR_*）未来实现时需采用新设计 —— 1:N 场景直接在 `example_sentence` 加对应的 FK 列。

### 枚举值变动

- 删除 `CHAR_WORD_SENTENCE`（不再需要）
- 删除 `VOCAB_SENSE_DEF_IMAGE_SENTENCE`（不再需要）
- 删除 `VOCAB_SENSE_STRUCTURE`（改由 `structure_id` 列替代）
- 删除 `GRAMMAR_MEANING_SENTENCE`、`GRAMMAR_STRUCTURE_SENTENCE`、`GRAMMAR_NOTICE_SENTENCE`（未实现，未来用新设计）
- **保留** `VOCAB_COMPARISON_CHAT`（后端内部不再用 biz_type 查询，但枚举值保留供参考）

## 详细变更

### 1. Service 接口重设计

```java
// 旧接口（删除）
findOne(String bizType, Long bizId)
findByBizIds(String bizType, Collection<Long> bizIds)
syncOne(String bizType, Long bizId, ExampleSentenceDto sentence)
disableByBizIds(String bizType, Collection<Long> bizIds)

// 新接口
findById(Long id)                              // 通用按 ID 查询
findByStructureId(Long structureId)             // 1:N — 查某个结构的所有例句
findByStructureIds(Collection<Long> ids)        // 批量
saveOrUpdateOne(ExampleSentenceDto dto)         // 新建或更新一条例句（不再需要 bizType+bizId）
disableById(Long id)                            // 软删某条
disableByStructureId(Long structureId)          // 软删某结构的所有例句
disableByStructureIds(Collection<Long> ids)
```

### 2. CharCharacterServiceImpl 改动

**当前**：发布时 `syncWordSentences()` 调用 `exampleSentenceService.syncOne(CHAR_WORD_SENTENCE, wordId, dto)`

**改后**：
1. 创建/更新 ExampleSentence → 得到 sentenceId
2. `charWord.setSentenceId(sentenceId)` → 写回 char_word 表
3. 查询时：`charWordRepository.findById` 拿到 `sentenceId`，再查 ExampleSentence

### 3. VocabWordServiceImpl 改动

**def_image_sentence**（1:1）：
- 创建 ExampleSentence → 回填 `vocabSense.setDefImageSentenceId(savedSentenceId)`

**structure_sentences**（1:N）：
- 创建 ExampleSentence 时，设置 `exampleSentence.setStructureId(structureId)`
- 删除时：`exampleSentenceService.disableByStructureId(structureId)`
- 查询时：`exampleSentenceService.findByStructureId(structureId)`

### 4. VocabComparisonGroupServiceImpl 改动

**当前**：`loadChats()` 调用 `exampleSentenceService.findByBizIds(VOCAB_COMPARISON_CHAT, chatIds)` 再用 biz_type 查。
`syncChats()` 先用 `syncOne(COMPARISON_CHAT_BIZ, chat.getId(), sentenceDto)` 写 biz_type，再回填 `chat.exampleSentenceId`。

**改后**：
- `loadChats()`：直接用 `chat.getExampleSentenceId()` 调用 `exampleSentenceService.findByIds(chatIds)` 批量加载。
- `syncChats()`：创建/更新 ExampleSentence（直接 set 字段，不设 biz_type），得到 id 后回填 `chat.setExampleSentenceId()`。
- `VOCAB_COMPARISON_CHAT` 枚举值保留但不再用于 Service 层面的 `findByBizType` 查询。

### 5. Repository 改动

```java
// 删除
findByBizTypeAndBizIdAndStatus(String bizType, Long bizId, Integer status)
findByBizTypeAndBizIdInAndStatus(String bizType, Collection<Long> bizIds, Integer status)

// 新增
findByStructureIdAndStatus(Long structureId, Integer status)
findByStructureIdInAndStatus(Collection<Long> structureIds, Integer status)
```

### 6. SQL 文件

**biz_common.sql**：example_sentence 表结构调整
**biz_character.sql**：char_word 加 sentence_id 列
**biz_vocabulary.sql**：vocab_sense 加 def_image_sentence_id 列

## 不改动的部分

- **前端 API 出入参**：`ExampleSentenceRequest`/`ExampleSentenceVO` 结构不变。前端仍然发送完整的 sentence/pinyin/translations/audioId/imageId/order 信息
- **草稿工作流**：draft content JSON 中例句信息结构不变
- **Controller 层**：不修改
- **Wrapper 层**：不修改（已经是从 DTO 到 VO 的逐字段映射）

## 风险与迁移

- **现有数据**：需要写迁移 SQL 将现有 `example_sentence` 数据按 `biz_type` 回填到业务表的新 FK 列
- **语法模块**：由于未实现，GRAMMAR_* 的枚举值先留在 enum 中标记 `@Deprecated`，example_sentence 表也暂时保留 biz_type/biz_id 的兼容列？—— 不保留，语法模块实现时再用新设计
- **测试**：`ExampleSentenceServiceImplTest` 需要完全重写
