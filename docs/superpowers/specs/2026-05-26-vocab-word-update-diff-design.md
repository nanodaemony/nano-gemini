# 词汇更新接口 Diff 优化设计

## 背景

当前 `PUT /api/vocabulary/{id}` 更新词汇时，会先更新词汇主表，再删除该词汇下所有义项、搭配、例句和练习题，最后按请求体重新插入全部子项。这种全删重建方式会破坏已有子项 ID，也无法区分新增、更新和删除。

汉字更新接口已经改为 diff 更新：主表正常覆盖字段，子表按提交数据和数据库已有数据执行新增、更新、删除。词汇和汉字都是主表加子表的管理方式，因此词汇更新也采用相同语义。

## 范围

本次只优化词汇更新接口：

- 保持 `PUT /api/vocabulary/{id}` 路径、HTTP 方法和 `204 NO_CONTENT` 返回不变。
- 主表 `vocab_word` 仍依托路径中的词汇主键 `{id}` 更新内容字段。
- 子表 `vocab_sense`、`vocab_structure`、`vocab_example`、`vocab_exercise` 根据已有数据与提交数据 diff 后分别新增、更新、删除。
- 不调整新增、删除、详情、列表接口。
- 不改表结构，不引入 JPA 级联关系，不增加数据库外键。

## 接口语义

路径中的 `{id}` 是更新操作唯一依托的词汇主键。请求体中的主表字段只用于更新该词汇内容，不能通过子项 ID 修改其他词汇或其他父级的数据。

所有子集合都表示后台提交的全量子表数据：

- 字段为 `null` 时按空列表处理。
- 子项没有 `id` 时表示新增。
- 子项有 `id` 时表示更新。
- 数据库已有但本次请求未提交的子项表示删除。

为了支持子项更新，`VocabWordCreateRequest` 的嵌套请求类需要增加 `id` 字段：

- `VocabSenseRequest.id`
- `VocabStructureRequest.id`
- `VocabExampleRequest.id`
- `VocabExerciseRequest.id`

Controller 转 DTO 时需要带上这些 ID。现有 `VocabSenseDto`、`VocabStructureDto`、`VocabExampleDto`、`VocabExerciseDto` 已有 `id` 字段，不需要新增 DTO 字段。

## 子表 Diff 规则

### 义项 `vocab_sense`

`vocab_sense` 按当前词汇 `wordId` 执行 diff：

1. 查询当前词汇已有义项，按 `id` 建立 `existingMap`。
2. 遍历提交义项：
   - `id == null`：新增义项，设置 `wordId = 当前词汇 id`，保存后拿到新义项 ID，再同步其 `structures`。
   - `id != null`：
     - 如果同一个请求内重复出现该 `id`，报错。
     - 如果 `existingMap` 中找不到该 `id`，报错，因为它不属于当前词汇或不存在。
     - 找到则覆盖更新该义项内容字段，再同步其 `structures`。
3. 数据库已有但提交 ID 集合中没有的义项需要删除。删除义项前，先删除该义项下所有 `examples` 和 `structures`。

### 搭配 `vocab_structure`

`vocab_structure` 按当前义项 `senseId` 执行 diff：

1. 查询当前义项已有搭配，按 `id` 建立 `existingMap`。
2. 遍历提交搭配：
   - `id == null`：新增搭配，设置 `wordId` 和 `senseId`，保存后拿到新搭配 ID，再同步其 `examples`。
   - `id != null`：
     - 如果同一个义项请求内重复出现该 `id`，报错。
     - 如果 `existingMap` 中找不到该 `id`，报错，因为它不属于当前义项或不存在。
     - 找到则覆盖更新该搭配内容字段，再同步其 `examples`。
3. 数据库已有但提交 ID 集合中没有的搭配需要删除。删除搭配前，先删除该搭配下所有 `examples`。

### 例句 `vocab_example`

`vocab_example` 按当前搭配 `structureId` 执行 diff：

1. 查询当前搭配已有例句，按 `id` 建立 `existingMap`。
2. 遍历提交例句：
   - `id == null`：新增例句，设置 `wordId`、`senseId`、`structureId`。
   - `id != null`：
     - 如果同一个搭配请求内重复出现该 `id`，报错。
     - 如果 `existingMap` 中找不到该 `id`，报错，因为它不属于当前搭配或不存在。
     - 找到则覆盖更新该例句内容字段。
3. 数据库已有但提交 ID 集合中没有的例句需要删除。

### 练习题 `vocab_exercise`

`vocab_exercise` 按当前词汇 `wordId` 执行 diff：

1. 查询当前词汇已有练习题，按 `id` 建立 `existingMap`。
2. 遍历提交练习题：
   - `id == null`：新增练习题，设置 `wordId = 当前词汇 id`。
   - `id != null`：
     - 如果同一个请求内重复出现该 `id`，报错。
     - 如果 `existingMap` 中找不到该 `id`，报错，因为它不属于当前词汇或不存在。
     - 找到则覆盖更新该练习题内容字段。
3. 数据库已有但提交 ID 集合中没有的练习题需要删除。

## 实现边界

更新逻辑放在 `VocabWordServiceImpl.update()` 中，Controller 继续只负责路径参数、请求体转换和返回状态码。

实现时优先保持代码直接清晰：义项、搭配、例句、练习题分别写私有同步方法，不做跨子表的泛型抽象。词汇子表存在嵌套父子关系，强行抽象会降低可读性。

Repository 优先复用现有查询方法：

- `VocabSenseRepository.findByWordId`
- `VocabStructureRepository.findBySenseId`
- `VocabStructureRepository.findByWordId`
- `VocabExampleRepository.findByStructureId`
- `VocabExampleRepository.findBySenseId`
- `VocabExampleRepository.findByWordId`
- `VocabExerciseRepository.findByWordId`

只有实现中确有必要时才增加新的 repository 方法。

## 异常和事务

`VocabWordServiceImpl.update()` 继续使用 `@Transactional(rollbackFor = Exception.class)`，任一子表同步失败整体回滚。

主表不存在时沿用当前 `EntityNotFoundException`。子项 ID 归属错误或请求内重复 ID 使用项目现有业务异常风格 `BadRequestException` 报错，并确保事务回滚。

## 验证

编译验证：

```bash
mvn -pl grid-system -am compile
```

接口场景验证：

1. 只改主表内容，子表请求保持原有子项，子项 ID 不变。
2. 提交无 `id` 的义项、搭配、例句或练习题，新增对应子项。
3. 提交已有 `id` 的义项、搭配、例句或练习题，更新对应子项内容。
4. 数据库已有子项未出现在请求中，删除对应子项。
5. 删除义项时，其下搭配和例句一并删除。
6. 删除搭配时，其下例句一并删除。
7. 提交不属于当前词汇或当前父级的子项 `id`，接口报错并回滚。
8. 同一层级请求内重复提交相同子项 `id`，接口报错并回滚。
9. `senses`、`structures`、`examples` 或 `exercises` 为 `null`，按空列表处理并删除对应层级已有数据。
