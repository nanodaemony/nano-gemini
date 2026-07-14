# 词汇辨析新增例句字段 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `vocab_comparison_item` 中新增 `example_sentences` 文本字段，贯穿 Entity → DTO → VO → Request → Wrapper → Service 全链路，后台和 APP 端均返回该字段。

**Architecture:** 纯文本字段，`\n` 分隔多条例句。沿用 `grammar_comparison_item.example_sentences` 的已有模式。草稿工作流无需额外处理——字段通过 `VocabComparisonItemDto` 自动序列化进 `draftContent` JSON。

**Tech Stack:** Java 8, Spring Boot 2.7.18, JPA/Hibernate, Lombok, MySQL

## Global Constraints

- 遵循 `grammar_comparison_item.example_sentences` 的字段定义和命名模式
- 纯文本 String 类型，不使用 JSON 解析
- Java 字段名: `exampleSentences` (驼峰)
- 数据库列名: `example_sentences` (snake_case)
- 列类型: `text`
- 无兼容性/迁移要求

---

### Task 1: SQL 建表语句

**Files:**
- Modify: `sql/biz_vocabulary_comparison.sql:29`

**Interfaces:**
- Produces: `vocab_comparison_item.example_sentences text` 列

- [ ] **Step 1: 在 vocab_comparison_item 表中新增 example_sentences 列**

在 `common_usage_translations` 行之后插入:

```sql
`example_sentences` text COMMENT '例句（每行一条，含正误标记如✓✗）',
```

编辑位置为 `sql/biz_vocabulary_comparison.sql` 第 29 行 `common_usage_translations` 之后，新行紧跟其后。

- [ ] **Step 2: 验证 SQL 语法**

检查 `CREATE TABLE vocab_comparison_item` 语句结构完整，`example_sentences` 列在 `common_usage_translations` 之后、`order` 列之前。

---

### Task 2: 领域层 — Entity + DTO

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabcomparison/VocabComparisonItem.java:59-61`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/dto/VocabComparisonItemDto.java:35-37`

**Interfaces:**
- Consumes: `example_sentences` 数据库列 (from Task 1)
- Produces: `VocabComparisonItem.exampleSentences: String` (JPA 映射), `VocabComparisonItemDto.exampleSentences: String`

- [ ] **Step 1: Entity 新增字段**

在 `VocabComparisonItem.java` 的 `commonUsageTranslations` 字段之后插入:

```java
@Column(name = "example_sentences", columnDefinition = "text")
@ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
private String exampleSentences;
```

精确位置: 第 59-61 行 (`commonUsageTranslations` 字段结束的 `}` 之后，`itemOrder` 字段之前)。

- [ ] **Step 2: DTO 新增字段**

在 `VocabComparisonItemDto.java` 的 `commonUsageTranslations` 字段之后插入:

```java
@ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
private String exampleSentences;
```

精确位置: 第 35-37 行 (`commonUsageTranslations` 字段之后，`order` 字段之前)。

- [ ] **Step 3: 编译验证**

```bash
cd grid-system && mvn compile -q
```

预期: BUILD SUCCESS

---

### Task 3: 后台 API 层 — VO + Request

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabComparisonItemVO.java:31-33`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabComparisonGroupCreateRequest.java:49-51`

**Interfaces:**
- Consumes: `VocabComparisonItemDto.exampleSentences` (from Task 2)
- Produces: `VocabComparisonItemVO.exampleSentences: String`, `VocabItemRequest.exampleSentences: String`

- [ ] **Step 1: VO 新增字段**

在 `VocabComparisonItemVO.java` 的 `commonUsageTranslations` 字段之后插入:

```java
@ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
private String exampleSentences;
```

精确位置: `commonUsageTranslations` 字段之后，`order` 字段之前。

- [ ] **Step 2: Request 内部类 VocabItemRequest 新增字段**

在 `VocabComparisonGroupCreateRequest.VocabItemRequest` 的 `commonUsageTranslations` 字段之后插入:

```java
@ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
private String exampleSentences;
```

精确位置: `VocabItemRequest` 内部类中，`commonUsageTranslations` 字段之后，`order` 字段之前。

- [ ] **Step 3: 编译验证**

```bash
cd grid-system && mvn compile -q
```

预期: BUILD SUCCESS

---

### Task 4: 后台映射逻辑 — Wrapper + Service 实现

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabComparisonGroupWrapper.java:59` (toItemDto) 和 `:138` (toItemVO)
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabcomparison/impl/VocabComparisonGroupServiceImpl.java:400` (toItemDto entity→DTO) 和 `:496` (syncItems DTO→entity)

**Interfaces:**
- Consumes: `VocabItemRequest.exampleSentences`, `VocabComparisonItemDto.exampleSentences`, `VocabComparisonItem.exampleSentences` (from Tasks 2-3)
- Produces: 完整的映射链路 Request→DTO→Entity→VO

- [ ] **Step 1: Wrapper.toItemDto() — Request → DTO**

在 `VocabComparisonGroupWrapper.java` 的 `toItemDto()` 方法中，`dto.setCommonUsageTranslations(...)` 行之后添加:

```java
dto.setExampleSentences(req.getExampleSentences());
```

- [ ] **Step 2: Wrapper.toItemVO() — DTO → VO**

在 `VocabComparisonGroupWrapper.java` 的 `toItemVO()` 方法中，`vo.setCommonUsageTranslations(...)` 行之后添加:

```java
vo.setExampleSentences(dto.getExampleSentences());
```

- [ ] **Step 3: ServiceImpl.toItemDto(entity) — Entity → DTO**

在 `VocabComparisonGroupServiceImpl.java` 的 `toItemDto(VocabComparisonItem entity)` 方法中，`dto.setCommonUsageTranslations(...)` 行之后添加:

```java
dto.setExampleSentences(entity.getExampleSentences());
```

- [ ] **Step 4: ServiceImpl.syncItems() — DTO → Entity (发布时)**

在 `VocabComparisonGroupServiceImpl.java` 的 `syncItems()` 方法中，`item.setCommonUsageTranslations(...)` 行之后添加:

```java
item.setExampleSentences(dto.getExampleSentences());
```

- [ ] **Step 5: 编译验证**

```bash
cd grid-system && mvn compile -q
```

预期: BUILD SUCCESS

---

### Task 5: APP 端 — VO + Wrapper

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabComparisonGroupVO.java:43-45`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppVocabComparisonWrapper.java:48`

**Interfaces:**
- Consumes: `VocabComparisonItemDto.exampleSentences` (from Task 2)
- Produces: `AppItemVO.exampleSentences: String`

- [ ] **Step 1: AppItemVO 新增字段**

在 `AppVocabComparisonGroupVO.AppItemVO` 的 `commonUsageTranslations` 字段之后插入:

```java
@ApiModelProperty(value = "例句（每行一条，含正误标记如✓✗）")
private String exampleSentences;
```

- [ ] **Step 2: AppVocabComparisonWrapper.toAppItemVO() — DTO → AppItemVO**

在 `AppVocabComparisonWrapper.java` 的 `toAppItemVO()` 方法中，`vo.setCommonUsageTranslations(...)` 行之后添加:

```java
vo.setExampleSentences(dto.getExampleSentences());
```

- [ ] **Step 3: 全量编译验证**

```bash
mvn compile -q
```

预期: BUILD SUCCESS

---

### 验证清单

全部 Task 完成后:

- [ ] SQL: `example_sentences` 列定义与 `grammar_comparison_item.example_sentences` 一致
- [ ] Entity: 字段注解 `@Column(name = "example_sentences", columnDefinition = "text")` 正确
- [ ] DTO: 字段存在于 `VocabComparisonItemDto`，序列化进草稿 JSON
- [ ] VO: 后台和 APP 端 VO 均包含该字段
- [ ] Request: 创建/更新请求接受该字段
- [ ] Wrapper (后台): `toItemDto` 和 `toItemVO` 均映射该字段
- [ ] Wrapper (APP): `toAppItemVO` 映射该字段
- [ ] Service: `toItemDto(entity)` 读取和 `syncItems` 写入均处理该字段
- [ ] 编译通过
