# 练习题业务标签设计

## 概述

为 `exercise_question` 表新增业务标签（`tags`）字段，支持给题目打上一个或多个业务分类标签（如语法、文化、词汇对比等）。标签仅用于后台管理分类，**不暴露给客户端 App API**。

## 枚举：QuestionBizTagEnum

新建 `grid-system/src/main/java/com/naon/grid/backend/enums/QuestionBizTagEnum.java`，参照 `QuestionTypeEnum` 风格：

| 枚举值 | 中文含义 |
|---|---|
| `CHARACTER` | 汉字 |
| `VOCABULARY` | 词汇 |
| `GRAMMAR` | 语法 |
| `TOPIC` | 话题 |
| `CULTURE` | 文化 |
| `CHARACTER_CHALLENGE` | 汉字大挑战 |
| `VOCABULARY_CHALLENGE` | 词汇大挑战 |

- `@JsonCreator fromCode(String)` + `@JsonValue getCode()` 用于 JSON 序列化
- 字段非必填，一个题可有零个、一个或多个标签

## 存储方案

- **DB 列**：`tags VARCHAR(512) DEFAULT NULL`，存储逗号分隔字符串（如 `GRAMMAR,CULTURE`）
- **Java 实体**：`String tags`
- **Java DTO/VO**：`List<String> tags`
- **查询**：使用 `@Query(type = Query.Type.FIND_IN_SET)` 精确匹配单个标签值
- **转换**：Service 层负责 `String` ↔ `List<String>`（逗号 split/join）

选择逗号分隔而非 JSON 数组的原因：MySQL `FIND_IN_SET` 函数可精确查询，避免 LIKE 误匹配（如搜 `GRAMMAR` 不会误匹配 `GRAMMAR_COMPARISON`）。

## 涉及文件

### 新建

| 文件 | 说明 |
|---|---|
| `grid-system/.../enums/QuestionBizTagEnum.java` | 业务标签枚举 |

### 修改

| # | 文件 | 改动 |
|---|---|---|
| 1 | `sql/biz_common.sql` | `exercise_question` 表新增 `tags` 列 |
| 2 | `grid-system/.../domain/question/ExerciseQuestion.java` | 新增 `@Column(name = "tags") private String tags` |
| 3 | `grid-system/.../service/question/dto/ExerciseQuestionDto.java` | 新增 `private List<String> tags` |
| 4 | `grid-system/.../service/question/dto/ExerciseQuestionQueryCriteria.java` | 新增 `@Query(type = FIND_IN_SET) private String tags` |
| 5 | `grid-system/.../rest/request/ExerciseQuestionCreateRequest.java` | 新增 `private List<String> tags` |
| 6 | `grid-system/.../rest/request/ExerciseQuestionQueryRequest.java` | 新增 `private String tags` |
| 7 | `grid-system/.../rest/vo/ExerciseQuestionBaseVO.java` | 新增 `private List<String> tags` |
| 8 | `grid-system/.../rest/vo/ExerciseQuestionVO.java` | 新增 `private List<String> tags` |
| 9 | `grid-system/.../rest/wrapper/ExerciseQuestionWrapper.java` | `toCriteria`/`toDto`/`toBaseVO`/`toVO` 各加 tags 映射 |
| 10 | `grid-system/.../service/question/impl/ExerciseQuestionServiceImpl.java` | `toBaseDto` 解析、`applyDraftOverlay` 覆盖、`publishDraft` 回写、`syncChildren` 回写 |

### 不改动（App 端不透出）

- `AppExerciseQuestionController`
- `AppExerciseQuestionWrapper`
- `AppExerciseQuestionDetailVO`
- `AppExerciseQuestionBatchRequest`

## Service 层转换细节

### toBaseDto（entity → dto）
```java
// entity.getTags() = "GRAMMAR,CULTURE"
dto.setTags(entity.getTags() == null ? null
    : Arrays.asList(entity.getTags().split(",")));
```

### applyDraftOverlay（草稿覆盖）
```java
if (draft.getTags() != null) dto.setTags(draft.getTags());
```

### publishDraft（发布回写）
```java
// dto.getTags() = ["GRAMMAR", "CULTURE"]
entity.setTags(dto.getTags() == null || dto.getTags().isEmpty() ? null
    : String.join(",", dto.getTags()));
```

### syncChildren（子题回写）
```java
childEntity.setTags(dto.getTags() == null || dto.getTags().isEmpty() ? null
    : String.join(",", dto.getTags()));
```

## 数据流

```
Create/Update Request (List<String> tags)
  → Wrapper.toDto → ExerciseQuestionDto.tags (List<String>)
  → Service 存入 draft_content JSON（保持 List<String> 格式）

Query Request (String tags = "GRAMMAR")
  → Wrapper.toCriteria → QueryCriteria.tags
  → FIND_IN_SET SQL: WHERE FIND_IN_SET('GRAMMAR', tags) > 0

Publish（草稿 → 发布）
  → DTO List<String> → String.join(",") → entity.tags VARCHAR

Read（详情/列表）
  → entity.tags VARCHAR → String.split(",") → DTO List<String>
  → Wrapper → VO List<String>
```

## 非功能约束

- 标签字段**非必填**（创建/更新时不传或传空列表均可）
- 无需数据迁移或兼容处理（业务未发布）
- App 端接口不透出标签字段
