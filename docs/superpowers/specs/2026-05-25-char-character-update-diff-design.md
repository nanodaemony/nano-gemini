# 汉字更新接口 Diff 优化设计

## 背景

当前 `PUT /api/character/{id}` 更新汉字时，会先更新汉字主表，再删除当前汉字下所有辨析和组词子表数据，最后重新插入请求中的全部子项。这种方式能完成全量覆盖，但会导致已有子项 ID 被破坏，也无法区分新增、更新和删除。

后台更新时会提交完整汉字数据，因此更新接口需要在保持全量提交语义的前提下，对子表执行 diff 增改删。

## 范围

本次只优化汉字更新接口：

- 保持 `PUT /api/character/{id}` 路径、HTTP 方法和 `204 NO_CONTENT` 返回不变。
- 主表 `char_character` 仍依托路径中的汉字主键 `{id}` 更新内容字段。
- 子表 `char_discrimination` 和 `char_word` 根据已有数据与提交数据 diff 后分别新增、更新、删除。
- 不调整新增、删除、详情、列表接口。
- 不改表结构，不引入 JPA 级联关系，不增加数据库外键。

## 接口语义

路径中的 `{id}` 是更新操作唯一依托的汉字主键。请求体中的主表字段只用于更新该汉字的内容，不能通过子项 ID 修改其他汉字的数据。

`discriminations` 和 `words` 都表示后台提交的全量子表数据：

- 字段为 `null` 时按空列表处理。
- 子项没有 `id` 时表示新增。
- 子项有 `id` 时表示更新。
- 数据库已有但本次请求未提交的子项表示删除。

为了支持子项更新，`CharCharacterCreateRequest.CharDiscriminationRequest` 和 `CharCharacterCreateRequest.CharWordRequest` 需要增加 `id` 字段，并在 Controller 转 DTO 时带上该字段。

## 子表 Diff 规则

`char_discrimination` 和 `char_word` 分别执行相同的 diff 流程：

1. 查询当前汉字已有子项，按 `id` 建立 `existingMap`。
2. 遍历提交子项：
   - `id == null`：构造新实体，设置 `charId = 当前汉字 id`，加入新增列表。
   - `id != null`：
     - 如果同一个请求内重复出现该 `id`，报错。
     - 如果 `existingMap` 中找不到该 `id`，报错，因为它不属于当前汉字或不存在。
     - 找到则覆盖更新该实体的内容字段，加入更新列表。
3. 遍历已有子项：
   - 数据库已有但提交 ID 集合中没有的子项，加入删除列表。
4. 在同一个事务中保存新增项、保存更新项、删除缺失项；任一步失败整体回滚。

主表不存在时沿用当前 `EntityNotFoundException`。子项 ID 归属错误或请求内重复 ID 使用项目现有业务异常风格报错，确保事务回滚。

## 实现边界

更新逻辑放在 `CharCharacterServiceImpl.update()` 中，Controller 继续只负责路径参数、请求体转换和返回状态码。

实现时优先保持代码直接清晰：辨析和组词分别写私有同步方法，不做跨子表的泛型抽象，因为两个子表字段不同，过度抽象会降低可读性。

`findByCharId` 已能支持归属校验和 diff 对比，repository 优先复用现有方法；只有在实现中确有必要时才增加新的 repository 方法。

## 验证

编译验证：

```bash
mvn -pl grid-system -am compile
```

接口场景验证：

1. 只改主表内容，子表请求保持原有子项，子项 ID 不变。
2. 提交无 `id` 辨析或组词，新增对应子项。
3. 提交已有 `id` 辨析或组词，更新对应子项内容。
4. 数据库已有子项未出现在请求中，删除对应子项。
5. 提交不属于当前汉字的子项 `id`，接口报错并回滚。
6. 同一子表请求内重复提交相同子项 `id`，接口报错并回滚。
7. `discriminations` 或 `words` 为 `null`，按空列表处理并删除对应子表全部数据。
