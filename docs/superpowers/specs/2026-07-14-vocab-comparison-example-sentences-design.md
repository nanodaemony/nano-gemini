# 词汇辨析用法对比 — 新增例句字段

**日期**: 2026-07-14  
**状态**: 待审核

## 背景

`vocab_comparison_item` 表目前存储了"用法对比"（usage_comparison）和"通用用法"（common_usage），但没有例句字段。需要在每个词汇辨析条目中新增例句文本字段，用于展示该词汇在特定用法下的例句。

`grammar_comparison_item` 表已实现相同功能（`example_sentences text`），本设计沿用其模式。

## 数据格式

- **存储**: `example_sentences text` — 纯文本字符串
- **分隔**: 多条例句使用换行符（`\n`）分隔
- **示例内容**:
  ```
  这些都不符合标准，是不合格产品。
  要按规定收费，不能擅自提高标准。
  这工作要求严，标准高，一般人做不了。
  问题就出在标准太宽，处理不严上。（×）
  ```
- **无 JSON 解析**: 不同于 translations 字段，该字段仅为纯文本，不需要序列化/反序列化逻辑

## 涉及文件

### 1. SQL 建表语句
**文件**: `sql/biz_vocabulary_comparison.sql`

在 `vocab_comparison_item` 表中 `common_usage_translations` 之后新增列:
```sql
`example_sentences` text COMMENT '例句（每行一条，含正误标记如✓✗）',
```

### 2. 实体类
**文件**: `grid-system/.../domain/vocabcomparison/VocabComparisonItem.java`

新增字段（参照 `GrammarComparisonItem.exampleSentences`）:
```java
@Column(name = "example_sentences", columnDefinition = "text")
@ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
private String exampleSentences;
```

### 3. DTO
**文件**: `grid-system/.../service/vocabcomparison/dto/VocabComparisonItemDto.java`

新增字段:
```java
@ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
private String exampleSentences;
```

### 4. VO（后台）
**文件**: `grid-system/.../rest/vo/VocabComparisonItemVO.java`

新增字段:
```java
@ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
private String exampleSentences;
```

### 5. 请求体
**文件**: `grid-system/.../rest/request/VocabComparisonGroupCreateRequest.VocabItemRequest`

新增字段:
```java
@ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
private String exampleSentences;
```

### 6. 后台 Wrapper
**文件**: `grid-system/.../rest/wrapper/VocabComparisonGroupWrapper.java`

- `toItemDto()`: 添加 `dto.setExampleSentences(req.getExampleSentences());`
- `toItemVO()`: 添加 `vo.setExampleSentences(dto.getExampleSentences());`

### 7. Service 实现
**文件**: `grid-system/.../service/vocabcomparison/impl/VocabComparisonGroupServiceImpl.java`

- `toItemDto(entity)` (entity→DTO): 添加 `dto.setExampleSentences(entity.getExampleSentences());`
- `syncItems()` (DTO→entity, publish 时): 添加 `item.setExampleSentences(dto.getExampleSentences());`

### 8. APP VO
**文件**: `grid-app/.../rest/vo/AppVocabComparisonGroupVO.AppItemVO`

新增字段:
```java
@ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
private String exampleSentences;
```

### 9. APP Wrapper
**文件**: `grid-app/.../rest/wrapper/AppVocabComparisonWrapper.java`

- `toAppItemVO()`: 添加 `vo.setExampleSentences(dto.getExampleSentences());`

## 草稿工作流

`exampleSentences` 字段属于 `VocabComparisonItemDto`，该 DTO 在创建/更新时被序列化为 `vocab_comparison_group.draft_content` JSON。发布时从草稿 JSON 反序列化，写入正式表 `vocab_comparison_item.example_sentences` 列。

**无需额外草稿处理逻辑** — 现有的 `JsonUtils.toJson()` / `JsonUtils.fromJson()` 自动包含新字段。

## 兼容性

服务尚未上线，无兼容性问题。直接修改建表语句，无数据迁移需求。

## 不涉及的范围

- 无 AI 内容标记变更（`exampleSentences` 属于人工录入内容）
- 无数据库迁移脚本
- 无 example_sentence 关联表变更（与 grammar 不同，vocab 的 `exampleSentences` 仅为纯文本字段，不关联 `example_sentence` 资源表）
