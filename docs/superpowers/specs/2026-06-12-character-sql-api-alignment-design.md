# 汉字后台接口 SQL 与出入参对齐设计

## 背景

本次改造针对 `CharCharacterController` 下的后台汉字管理接口。数据库结构已按 `sql/biz_character.sql` 和 `sql/biz_common.sql` 调整：部分字段重命名，汉字组词例句从 `char_word` 拆出到通用 `example_sentence` 表，接口出入参也已重新定义。

本设计遵循两个边界：

1. 不根据旧逻辑反向修改新的汉字接口出入参；如果出入参存在明确问题，只做已确认的修正。
2. 本次不改词汇新建、更新、审核、发布、查询等大逻辑。只创建通用例句基础层，并仅在汉字组词例句中接入。

## 已确认的接口约束

- 一个汉字组词只有一个例句。
- `CharWordRequest.sentenceContent` 使用单个 `ExampleSentenceRequest`。
- `CharWordVO.wordItemSentence` 使用单个 `ExampleSentenceVO`。
- `CharWordRequest.id` 新增时不传、更新时传；移除不适用于 `Integer` 的 `@NotBlank`，Swagger `required=false`。
- `ExampleSentenceRequest.id` 从基本类型 `long` 改为包装类型 `Long`，用于区分新增时未传和更新时传 ID。
- `ExampleSentenceRequest.audioId`、`imageId` 本次暂不改，仍保持当前定义。

## 方案选择

采用“通用例句基础层 + 仅汉字接入”的方案。

### 采用该方案的原因

- 符合 `example_sentence` 作为通用表的设计，需要建立可复用的 Repository 和 Service 层。
- 汉字发布流程可以立即使用通用例句表，满足本次业务目标。
- 词汇服务暂不接入，避免改动词汇新建、更新、发布、查询大逻辑。

### 不采用的方案

- 不把例句逻辑直接写死在 `CharCharacterServiceImpl` 中，因为后续词汇、语法接入会重复逻辑。
- 不在本次同时接入词汇例句，因为这会扩大范围并触碰词汇发布链路。

## 架构与组件

### 汉字接口主链路

`CharCharacterController` 的接口数量、路径和语义保持不变：

- `POST /api/character`：创建汉字草稿。
- `PUT /api/character/{id}`：更新汉字草稿。
- `PUT /api/character/{id}/review`：草稿审核通过。
- `PUT /api/character/{id}/publish`：发布草稿。
- `GET /api/character/{id}`：查询详情。
- `GET /api/character`：分页查询列表。
- `DELETE /api/character/{id}`：软删除。
- `PUT /api/character/{id}/offline`：下线。

新增和更新仍只写 `char_character.draft_content`，审核只改状态，发布时再把草稿内容回写主表、子表和通用例句表。

### 汉字实体映射

实体层需要按 `sql/biz_character.sql` 对齐。

#### `CharCharacter`

- `level` 字段映射到 `hsk_level`。
- `descTranslations` 字段映射到 `char_desc_translations`。
- 新增 `radicalId`，映射 `radical_id`。
- 新增 `componentCombination`，映射 `component_combination`。
- `draft_content` 按 SQL 使用 `text`，不依赖 JSON 列定义。

#### `CharComparison`

建议用 `CharComparison` 替代旧的 `CharDiscrimination` 命名，映射新表 `char_comparison`。

字段对齐：

- `comparison_char`
- `comparison_pinyin`
- `comparison_char_translations`
- `comparison_desc_translations`
- ``order``
- `char_id`
- `status`
- `create_time`
- `update_time`

对外接口仍使用 `comparisons`。

#### `CharWord`

- `level` 字段映射到 `hsk_level`。
- 排序字段映射到 SQL 字段 ``order``。
- 移除旧例句字段：`example_sentence`、`example_pinyin`、`example_translations`、`example_image`。
- 例句改由 `example_sentence` 表承载。

### 通用例句基础层

新增通用基础层，本次只由汉字服务使用。

#### `ExampleSentence`

映射 `example_sentence` 表：

- `id`
- `bizType`
- `bizId`
- `sentence`
- `pinyin`
- `audioId`
- `translations`
- `imageId`
- `order`
- `createTime`
- `updateTime`
- `status`

#### `ExampleSentenceRepository`

需要支持：

- 按 `bizType + bizId + status` 查询。
- 按 `bizType + bizIds + status` 批量查询。
- 保存和批量保存。

#### `ExampleSentenceService`

需要提供面向业务的通用方法：

- `findOne(bizType, bizId)`：查询某个业务对象的一条例句。
- `findByBizIds(bizType, bizIds)`：批量查询业务对象例句，并按 `bizId` 组织。
- `syncOne(bizType, bizId, sentence)`：维护“一个业务对象一条例句”的约束。
- `disableByBizIds(bizType, bizIds)`：软删除一批业务对象下的例句。

汉字组词使用 `SentenceBizTypeEnum.CHAR_WORD_SENTENCE`。

## DTO 与 Wrapper 设计

### DTO 承载字段

`CharCharacterDto` 需要补齐：

- `hskLevel` 或继续内部使用 `level` 但明确映射到 `hsk_level`。
- `radicalId`
- `componentCombination`
- `charDescTranslations` 或继续内部使用 `descTranslations` 但明确映射到 `char_desc_translations`。

`CharWordDto` 需要补齐：

- `hskLevel` 或继续内部使用 `level` 但明确映射到 `hsk_level`。
- `wordItemSentence` 或 `sentenceContent`，用于承载单条 `ExampleSentence`。

`CharComparisonDto` 建议替代旧 `CharDiscriminationDto`，字段名与接口和 SQL 保持一致。

### Wrapper 映射

`CharCharacterWrapper` 负责严格对齐新的接口出入参：

- `CharCharacterCreateRequest.hskLevel` → DTO → `char_character.hsk_level`。
- `radicalId`、`componentCombination` 要从 Request 映射到 DTO，并从 DTO 映射到 VO。
- `charDescTranslations` 要映射到 DTO，并最终存入 `char_desc_translations`。
- `comparisons` 映射到比较字 DTO。
- `words[].sentenceContent` 映射到组词 DTO 的单条例句。
- 详情出参中，组词 DTO 的单条例句映射到 `wordItemSentence`。

## 数据流

### 新增与更新

新增和更新阶段仍只写草稿：

1. Controller 接收 `CharCharacterCreateRequest`。
2. Wrapper 转换为 `CharCharacterDto`。
3. DTO 序列化为 `draft_content`。
4. 新增时主表保存 `character` 和三状态字段。
5. 更新时覆盖 `draft_content`，如当前 `edit_status` 为 `reviewed` 或 `published`，改回 `draft`。

该阶段不写 `char_comparison`、`char_word`、`example_sentence`。

### 审核

审核只做状态转换：

- 校验汉字存在且未软删除。
- 校验 `draft_content` 存在。
- 校验当前 `edit_status = draft`。
- 设置 `edit_status = reviewed`。

### 发布

发布是本次主要改造点：

1. 校验汉字存在且未软删除。
2. 校验 `draft_content` 存在。
3. 校验当前 `edit_status = reviewed`。
4. 解析 `draft_content` 为 `CharCharacterDto`。
5. 回写 `char_character` 主表字段。
6. 同步 `char_comparison`。
7. 同步 `char_word`。
8. 对每个有效 `char_word` 同步 `example_sentence` 中的单条例句。
9. 对本次删除或遗漏的组词，软删除对应例句。
10. 设置 `publish_status = published`、`edit_status = published`、`draft_content = null`。

### 详情查询

详情查询保持状态分流。

#### 草稿或已审核

直接从 `draft_content` 返回业务内容，并覆盖主表状态字段。

草稿里的 `words[].sentenceContent` 映射为详情出参的 `wordItemSentence`。如果草稿来自已发布内容再编辑，前端带回的 `sentenceContent.id` 会保留在草稿 JSON 中。

#### 已发布

从数据库组装：

1. 查询 `char_character`。
2. 查询有效 `char_comparison`。
3. 查询有效 `char_word`。
4. 根据所有 `char_word.id` 批量查询 `example_sentence`：
   - `biz_type = CHAR_WORD_SENTENCE`
   - `biz_id in char_word ids`
   - `status = 1`
5. 将例句按 `biz_id` 填充到对应组词。
6. Wrapper 转换为 `CharCharacterVO`。

排序规则保持“值大的排前面”：

- `comparisons` 按 ``order`` 降序。
- `words` 按 ``order`` 降序。
- 例句每个组词只一条，不需要列表排序。

如果历史数据中某个组词存在多条有效例句，详情查询只返回排序最高或最新的一条；后续发布通过 `syncOne` 收敛为一条有效记录。

### 列表查询

列表不查询子表和例句表。

列表草稿覆盖逻辑需要补齐新主表字段，确保草稿和已审核状态下列表展示最新草稿内容。

## 校验与错误处理

### ID 归属校验

发布时对所有带 ID 的子资源做归属校验：

- `comparison.id` 必须属于当前汉字。
- `word.id` 必须属于当前汉字。
- `sentenceContent.id` 必须属于当前组词，条件为：
  - `biz_type = CHAR_WORD_SENTENCE`
  - `biz_id = 当前 char_word.id`
  - `status = 1`

不满足时抛 `BadRequestException`。

### 重复 ID 校验

同一次提交中：

- 重复 `comparison.id` 抛 `辨析ID重复: {id}`。
- 重复 `word.id` 抛 `组词ID重复: {id}`。

单个组词只有一个 `sentenceContent`，不需要列表重复校验。

### 空例句处理

- `sentenceContent == null`：表示没有例句，发布时软删除该组词已有例句。
- `sentenceContent.sentence` 为空：按没有例句处理，不保存空记录。
- `sentenceContent.sentence` 非空：保存或更新例句。

这样可以满足 `example_sentence.sentence` 的 `NOT NULL` 约束。

### 单条例句约束

`ExampleSentenceService.syncOne` 维护“一个业务对象一条例句”的约束：

- 保存或更新目标例句后，软删除同一 `biz_type + biz_id` 下其他有效例句。
- 如果请求无例句内容，软删除同一 `biz_type + biz_id` 下所有有效例句。

### 状态流

沿用现有状态流：

- 只有 `draft` 可审核。
- 只有 `reviewed` 可发布。
- 删除只软删主表。
- 下线只改 `publish_status`。
- 更新已审核或已发布内容时自动回到 `draft`。

## 改动边界

### 允许改动

- 汉字 Request、VO、Wrapper、DTO、Entity、Repository、Service。
- 通用 `example_sentence` Entity、Repository、Service、DTO。
- `ExampleSentenceRequest.id` 的类型修正。
- 与汉字 SQL 字段对齐相关的命名和映射。

### 不改动

- 不接入词汇 `VOCAB_SENSE_*` 例句。
- 不改 `VocabWordServiceImpl` 新建、更新、审核、发布、查询逻辑。
- 不改变 `CharCharacterController` 的接口路径和整体语义。
- 不引入 JPA `@ManyToOne` 或数据库外键。

## 测试与验证

### 编译验证

运行：

```bash
mvn -pl grid-system -am -DskipTests compile
```

验证目标：

- 实体、Repository、Service、Wrapper 可编译。
- `ExampleSentenceRequest.id` 改为 `Long` 后引用一致。
- 汉字 SQL 字段映射无明显类型错误。

### 手工接口流程验证

按 `docs/汉字管理接口使用流程.md` 验证：

1. 新增汉字草稿，包含主字段、`comparisons`、`words[].sentenceContent`。
2. 查询草稿详情，确认 `wordItemSentence` 来自草稿内容。
3. 审核并发布。
4. 查询已发布详情，确认主表、比较字、组词、例句分别来自对应数据库表。
5. 更新已发布汉字，带已有 `word.id` 和 `sentenceContent.id`，发布后更新原例句而非重复新增。
6. 删除某个组词或移除 `sentenceContent`，发布后对应组词或例句软删除。

### 数据库验证

发布后检查：

- `char_character` 使用新字段名。
- `char_comparison` 中有效记录符合提交内容。
- `char_word` 不再写旧例句字段。
- `example_sentence` 中汉字组词例句满足：
  - `biz_type = CHAR_WORD_SENTENCE`
  - `biz_id = char_word.id`
  - 一个组词最多一条有效记录
  - 移除例句或组词后 `status = 0`

## 风险与后续

- 旧代码中 `CharDiscrimination` 与新表 `char_comparison` 命名不一致。建议实现时一次性替换为 `CharComparison`，减少长期混乱。
- `ExampleSentenceVO.translations` 当前使用 `TextTranslationRequest`，语义上更适合 `TextTranslationVO`。本次可以不扩大范围；如果实现时发现转换混乱，可在不影响词汇逻辑的前提下修正。
- 词汇接口已有 `ExampleSentenceRequest` 相关字段，但本次不接入词汇服务。后续词汇改造可复用 `ExampleSentenceService`。
