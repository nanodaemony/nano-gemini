# 练习题业务标签 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 `exercise_question` 表和管理后台新增 `tags` 业务标签字段（逗号分隔存储、Java 层 `List<String>`），App 端不透出。

**Architecture:** DB 用逗号分隔字符串 + `FIND_IN_SET` 精确查询；实体存 `String`；DTO/VO/Request 用 `List<String>`；Service 层负责转换；Wrapper 负责映射。

**Tech Stack:** Java 8, Spring Boot 2.7, Spring Data JPA, Lombok, Fastjson2

## Global Constraints

- 标签字段非必填（创建/更新时不传或传空列表均可）
- 无需数据迁移或兼容处理（业务未发布）
- App 端接口（Controller/VO/Wrapper）不透出标签字段
- 参照项目已有代码风格（`QuestionTypeEnum` 枚举风格、Wrapper 静态映射模式）

---

### Task 1: 新建 `QuestionBizTagEnum` 枚举

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/enums/QuestionBizTagEnum.java`

**Interfaces:**
- Produces: `QuestionBizTagEnum` — 7 个枚举值，`@JsonCreator fromCode(String)` 反序列化，`@JsonValue getCode()` 序列化

- [ ] **Step 1: 创建枚举类**

参照 `grid-system/src/main/java/com/naon/grid/backend/enums/QuestionTypeEnum.java` 的风格：

```java
package com.naon.grid.backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 练习题业务标签枚举
 */
@Getter
public enum QuestionBizTagEnum {

    CHARACTER("CHARACTER", "汉字"),
    VOCABULARY("VOCABULARY", "词汇"),
    GRAMMAR("GRAMMAR", "语法"),
    TOPIC("TOPIC", "话题"),
    CULTURE("CULTURE", "文化"),
    CHARACTER_CHALLENGE("CHARACTER_CHALLENGE", "汉字大挑战"),
    VOCABULARY_CHALLENGE("VOCABULARY_CHALLENGE", "词汇大挑战");

    private final String code;
    private final String description;

    QuestionBizTagEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonCreator
    public static QuestionBizTagEnum fromCode(String code) {
        for (QuestionBizTagEnum tag : values()) {
            if (tag.code.equalsIgnoreCase(code)) {
                return tag;
            }
        }
        throw new IllegalArgumentException("Unknown question biz tag: " + code);
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/enums/QuestionBizTagEnum.java
git commit -m "feat: add QuestionBizTagEnum for exercise question business tagging"
```

---

### Task 2: SQL 新增 `tags` 列

**Files:**
- Modify: `sql/biz_common.sql` — `exercise_question` 建表语句

**Interfaces:**
- Produces: `exercise_question.tags VARCHAR(512) DEFAULT NULL`

- [ ] **Step 1: 在 `sort` 列后、`draft_content` 列前新增 `tags` 列**

当前 `sql/biz_common.sql` 中 `exercise_question` 表结构（第 35-37 行）:

```sql
  `sort` int DEFAULT '0' COMMENT '排序号（值越大越靠前）',

  `draft_content` text COMMENT '草稿内容（JSON结构）',
```

在 `sort` 和 `draft_content` 之间插入：

```sql
  `tags` varchar(512) DEFAULT NULL COMMENT '业务标签，逗号分隔多个值（如 GRAMMAR,CULTURE），参考枚举：QuestionBizTagEnum',
```

- [ ] **Step 2: 提交**

```bash
git add sql/biz_common.sql
git commit -m "feat: add tags column to exercise_question table"
```

---

### Task 3: 实体 `ExerciseQuestion` 新增 `tags` 字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/question/ExerciseQuestion.java`

**Interfaces:**
- Produces: `ExerciseQuestion.tags` — `String` 类型，Lombok `@Getter @Setter`

- [ ] **Step 1: 在 `sort` 字段后新增 `tags` 字段**

在 `sort` 字段定义之后添加：

```java
    @Column(name = "tags", length = 512)
    private String tags;
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/question/ExerciseQuestion.java
git commit -m "feat: add tags field to ExerciseQuestion entity"
```

---

### Task 4: DTO `ExerciseQuestionDto` 新增 `tags`

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/question/dto/ExerciseQuestionDto.java`

**Interfaces:**
- Produces: `ExerciseQuestionDto.tags` — `List<String>` 类型

- [ ] **Step 1: 新增字段**

在 `aiGeneratedFields` 字段附近添加：

```java
    private List<String> tags;
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/question/dto/ExerciseQuestionDto.java
git commit -m "feat: add tags field to ExerciseQuestionDto"
```

---

### Task 5: 查询条件 `ExerciseQuestionQueryCriteria` 新增 `tags` 过滤

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/question/dto/ExerciseQuestionQueryCriteria.java`

**Interfaces:**
- Produces: `ExerciseQuestionQueryCriteria.tags` — `String` 类型，`@Query(type = FIND_IN_SET)` 精确匹配

- [ ] **Step 1: 新增字段**

在现有字段后添加：

```java
    @Query(type = Query.Type.FIND_IN_SET)
    private String tags;
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/question/dto/ExerciseQuestionQueryCriteria.java
git commit -m "feat: add tags FIND_IN_SET filter to ExerciseQuestionQueryCriteria"
```

---

### Task 6: 入参 `ExerciseQuestionCreateRequest` 新增 `tags`

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionCreateRequest.java`

**Interfaces:**
- Produces: `ExerciseQuestionCreateRequest.tags` — `List<String>` 类型

- [ ] **Step 1: 新增字段**

在 `aiGeneratedFields` 字段附近添加：

```java
    @ApiModelProperty(value = "业务标签列表，参考枚举：QuestionBizTagEnum")
    private List<String> tags;
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionCreateRequest.java
git commit -m "feat: add tags field to ExerciseQuestionCreateRequest"
```

---

### Task 7: 查询入参 `ExerciseQuestionQueryRequest` 新增 `tags`

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionQueryRequest.java`

**Interfaces:**
- Produces: `ExerciseQuestionQueryRequest.tags` — `String` 类型（单个标签值用于 FIND_IN_SET 过滤）

- [ ] **Step 1: 新增字段**

在 `editStatus` 字段后添加：

```java
    @ApiModelProperty(value = "业务标签过滤，参考枚举：QuestionBizTagEnum")
    private String tags;
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/ExerciseQuestionQueryRequest.java
git commit -m "feat: add tags filter to ExerciseQuestionQueryRequest"
```

---

### Task 8: 列表 VO `ExerciseQuestionBaseVO` 新增 `tags`

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionBaseVO.java`

**Interfaces:**
- Produces: `ExerciseQuestionBaseVO.tags` — `List<String>` 类型

- [ ] **Step 1: 新增字段**

在 `editStatus` 字段附近添加：

```java
    @ApiModelProperty(value = "业务标签列表")
    private List<String> tags;
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionBaseVO.java
git commit -m "feat: add tags field to ExerciseQuestionBaseVO"
```

---

### Task 9: 详情 VO `ExerciseQuestionVO` 新增 `tags`

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionVO.java`

**Interfaces:**
- Produces: `ExerciseQuestionVO.tags` — `List<String>` 类型

- [ ] **Step 1: 新增字段**

在 `aiReviewedFields` 字段附近添加：

```java
    @ApiModelProperty(value = "业务标签列表")
    private List<String> tags;
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/ExerciseQuestionVO.java
git commit -m "feat: add tags field to ExerciseQuestionVO"
```

---

### Task 10: Wrapper `ExerciseQuestionWrapper` 新增 tags 映射

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/ExerciseQuestionWrapper.java`

**Interfaces:**
- Consumes: `ExerciseQuestionQueryRequest.tags` `(String)`, `ExerciseQuestionCreateRequest.tags` `(List<String>)`, `ExerciseQuestionDto.tags` `(List<String>)`
- Produces: 映射到 `ExerciseQuestionQueryCriteria.tags`、`ExerciseQuestionDto.tags`、`ExerciseQuestionBaseVO.tags`、`ExerciseQuestionVO.tags`

- [ ] **Step 1: `toCriteria` 新增 tags 映射**

在 `toCriteria` 方法中，`criteria.setEditStatus(...)` 之后添加：

```java
        criteria.setTags(request.getTags());
```

- [ ] **Step 2: `toDto` 新增 tags 映射**

在 `toDto` 方法中，`dto.setAiGeneratedFields(...)` 之后添加：

```java
        dto.setTags(request.getTags());
```

- [ ] **Step 3: `toBaseVO` 新增 tags 映射**

在 `toBaseVO` 方法中，`vo.setEditStatus(...)` 之后添加：

```java
        vo.setTags(dto.getTags());
```

- [ ] **Step 4: `toVO` 新增 tags 映射**

在 `toVO` 方法中，`vo.setEditStatus(...)` 之后添加：

```java
        vo.setTags(dto.getTags());
```

- [ ] **Step 5: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/ExerciseQuestionWrapper.java
git commit -m "feat: add tags mapping to ExerciseQuestionWrapper"
```

---

### Task 11: ServiceImpl `ExerciseQuestionServiceImpl` 新增 tags 转换逻辑

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/question/impl/ExerciseQuestionServiceImpl.java`

**Interfaces:**
- Consumes: `ExerciseQuestion.tags` `(String)`, `ExerciseQuestionDto.tags` `(List<String>)`
- Produces: entity ↔ dto 转换，草稿覆盖，发布回写，子题同步

- [ ] **Step 1: `toBaseDto` — entity tags 字符串 → DTO List<String>**

在 `toBaseDto` 方法中，`dto.setSort(...)` 之后添加：

```java
        // tags: comma-separated DB string → List<String>
        dto.setTags(entity.getTags() == null ? null
                : java.util.Arrays.asList(entity.getTags().split(",")));
```

- [ ] **Step 2: `applyDraftOverlay` — 草稿 tags 覆盖**

在 `applyDraftOverlay` 方法中，`draft.getSort()` 覆盖逻辑之后添加：

```java
        if (draft.getTags() != null) dto.setTags(draft.getTags());
```

- [ ] **Step 3: `publishDraft` — 发布时 tags 回写到 entity**

在 `publishDraft` 方法中，`entity.setSort(...)` 之后添加：

```java
        // tags: List<String> → comma-separated DB string
        entity.setTags(draftDto.getTags() == null || draftDto.getTags().isEmpty() ? null
                : String.join(",", draftDto.getTags()));
```

- [ ] **Step 4: `syncChildren` — 子题 tags 回写**

在 `syncChildren` 方法中，`childEntity.setSort(...)` 之后添加：

```java
            childEntity.setTags(dto.getTags() == null || dto.getTags().isEmpty() ? null
                    : String.join(",", dto.getTags()));
```

- [ ] **Step 5: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/question/impl/ExerciseQuestionServiceImpl.java
git commit -m "feat: add tags conversion logic to ExerciseQuestionServiceImpl"
```

---

### Task 12: 编译验证

**Files:**
- 无新文件，验证前述所有修改编译通过

- [ ] **Step 1: 编译项目**

```bash
cd grid-bootstrap && mvn compile -q
```

- [ ] **Step 2: 确认无编译错误**

预期：`BUILD SUCCESS`，无任何编译错误。
