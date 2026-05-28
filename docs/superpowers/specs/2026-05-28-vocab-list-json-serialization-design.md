# 词汇模块列表字段 JSON 序列化设计文档

**日期**：2026-05-28

**目标**：将词汇模块的列表字段从 String JSON 格式改为对象类型，在 Service 层进行 JSON 序列化/反序列化转换

---

## 1. 背景

当前 VocabSense 和 VocabExercise 中的列表字段在各层都使用 String 类型存储 JSON 格式数据，导致 API 层与前端交互时也需要手动处理 JSON 字符串，体验不佳。需要将这些字段改为真正的对象类型，仅在 Entity 层保持 String 类型与数据库交互。

---

## 2. 涉及的字段

### 2.1 VocabSense 字段

| 字段 | 数据库类型 | JSON 格式示例 | 说明 |
|------|-----------|--------------|------|
| `synonyms` | text | `["吃饭", "干饭"]` | 近义词列表 |
| `antonyms` | text | `["饿肚子"]` | 反义词列表 |
| `relatedForward` | text | `["餐桌"]` | 正序关联词汇 |
| `relatedBackward` | text | `["做饭"]` | 逆序关联词汇 |

### 2.2 VocabExercise 字段

| 字段 | 数据库类型 | JSON 格式示例 | 说明 |
|------|-----------|--------------|------|
| `options` | text | `[{"option":"A","text":"选项A"},{"option":"B","text":"选项B"}]` | 选项列表 |
| `answers` | text | `["A", "C"]` | 答案列表 |

---

## 3. 分层设计

| 层级 | 字段类型 | 说明 |
|------|---------|------|
| **Entity (Repo)** | `String` | 保持不变，直接与数据库交互 |
| **DTO (Service)** | 对象类型 | 转换为对象，Service 层使用 |
| **Request/VO (API)** | 对象类型 | 与前端交互使用对象 |

---

## 4. ExerciseOption 类

**位置**：`com.naon.grid.backend.domain.vocabulary.ExerciseOption`

```java
@Getter
@Setter
public class ExerciseOption {
    private String option;  // "A", "B", "C", "D"
    private String text;    // 选项文案
}
```

这是一个简单的 POJO 类，不需要 JPA 注解，不对应数据库表。

---

## 5. JsonUtils 扩展

在 `JsonUtils.java` 中新增以下方法：

### 5.1 字符串列表处理

```java
/**
 * 将字符串列表序列化为 JSON 字符串
 *
 * @param list 字符串列表
 * @return JSON 字符串，null 或空列表返回 null
 */
public static String toStringListJson(List<String> list)

/**
 * 将 JSON 字符串反序列化为字符串列表
 *
 * @param json JSON 字符串
 * @return 字符串列表，null 或空白字符串返回空列表，会过滤掉空白字符串
 */
public static List<String> parseStringList(String json)
```

### 5.2 ExerciseOption 列表处理

```java
/**
 * 将 ExerciseOption 列表序列化为 JSON 字符串
 *
 * @param list 选项列表
 * @return JSON 字符串，null 或空列表返回 null
 */
public static String toExerciseOptionListJson(List<ExerciseOption> list)

/**
 * 将 JSON 字符串反序列化为 ExerciseOption 列表
 *
 * @param json JSON 字符串
 * @return 选项列表，null 或空白字符串返回空列表，会过滤掉空对象
 */
public static List<ExerciseOption> parseExerciseOptionList(String json)
```

---

## 6. 空值处理规则

与 translations 字段保持一致：

- **写入时**：null 或空列表 → 数据库存 `null`
- **读取时**：数据库 `null` 或空白字符串 → 返回空列表
- **过滤规则**：
  - 字符串列表：过滤掉 `null` 和空白字符串（`""`, `"  "`）
  - ExerciseOption 列表：过滤掉 `null` 对象，以及 `option` 和 `text` 都为 null/空白的对象

---

## 7. 需要修改的文件清单

| 文件 | 改动内容 |
|------|---------|
| `JsonUtils.java` | 新增 JSON 转换方法 |
| `ExerciseOption.java` | 新建选项类 |
| `VocabSenseDto.java` | synonyms/antonyms/relatedForward/relatedBackward 改为 `List<String>` |
| `VocabExerciseDto.java` | options 改为 `List<ExerciseOption>`，answers 改为 `List<String>` |
| `VocabWordVO.java` | 内部类 `VocabSenseVO` 和 `VocabExerciseVO` 对应字段类型修改 |
| `VocabWordCreateRequest.java` | 内部类对应字段类型修改 |
| `VocabWordController.java` | 转换方法同步修改 |
| `VocabWordServiceImpl.java` | convertToXxx 和 updateXxx 方法中添加 JSON 转换逻辑 |

