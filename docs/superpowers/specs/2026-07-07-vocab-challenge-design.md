# App 端词汇大挑战接口设计

**日期**: 2026-07-07
**状态**: 设计中

## 概述

为 grid-app 模块新增词汇大挑战接口。用户每次请求获得 10 道图片选择题，根据配图选出正确的中文词汇。出题等级根据用户个人中心设置的 HSK 等级自动适配。

## 设计决策

| 决策 | 结论 |
|------|------|
| 接口路径 | `GET /api/app/vocab/challenge`，放在 `AppVocabWordController` |
| 是否需要登录 | 必须登录 |
| 题目数 | 固定 10 题 |
| 题目图片 | 一期全部有图，随机取词 JOIN 有 `def_image_id` 的义项 |
| 题干形式 | 展示图片，`contentText` 为 `null` |
| 选项形式 | 全文字（词头），暂不配图 |
| 去重 | 不需要 |
| 成绩记录 | 一期不记录 |
| 输出结构 | 复用 `AppExerciseQuestionDetailVO` |

## 接口设计

### GET /api/app/vocab/challenge

- **方法**: `GET`
- **路径**: `/api/app/vocab/challenge`
- **认证**: 需要 App Token
- **返回**: `List<AppExerciseQuestionDetailVO>`（固定 10 条）

### 出题策略

```
用户已登录 → 查询 grid_user.hsk_level
  ├─ hskLevel != "0"
  │   ├─ N < 9  → 当前等级 N 出 5 题 + (N+1) 等级出 5 题
  │   └─ N >= 9 → 10 题全来自等级 9
  └─ hskLevel == "0" → HSK 4 出 5 题 + HSK 5 出 5 题
```

## 数据层

### 随机取词 SQL

```sql
SELECT DISTINCT vw.* FROM vocab_word vw
INNER JOIN vocab_sense vs ON vw.id = vs.word_id
WHERE vw.hsk_level = ?1
  AND vw.status = 1 AND vw.publish_status = 'published'
  AND vs.status = 1 AND vs.def_image_id IS NOT NULL
ORDER BY RAND() LIMIT ?2
```

新增到 `VocabWordRepository`，通过 JOIN `vocab_sense` 确保每个随机结果至少有一个带配图的义项。

### VocabWordRepository

```java
@Query(value = "SELECT DISTINCT vw.* FROM vocab_word vw " +
    "INNER JOIN vocab_sense vs ON vw.id = vs.word_id " +
    "WHERE vw.hsk_level = ?1 AND vw.status = 1 AND vw.publish_status = 'published' " +
    "AND vs.status = 1 AND vs.def_image_id IS NOT NULL " +
    "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
List<VocabWord> findRandomPublishedByHskLevel(String hskLevel, int count);
```

### VocabWordService

```java
List<VocabWordDto> findRandomPublishedWithImage(String hskLevel, int count);
```

实现：调用 Repository 方法 → 返回的实体用 MapStruct 映射为 DTO，不含子表数据。

## 题目组装

每道题 → `AppExerciseQuestionDetailVO`：

| VO 字段 | 取值 |
|---------|------|
| `id` | `-(vocabWord.id)`，负数区分非持久化题目 |
| `questionType` | `"vocab_challenge"` |
| `stem` | `"请根据提示选出对应的词语"` |
| `content.contentText` | `null` |
| `content.image` | 第一个有图的义项 OSS URL |
| `options[4]` | 正确答案 + 3 个同等级干扰项，随机打乱 |
| `answer[1]` | `["A"]` ~ `["D"]` |
| `explanation` | `null` |
| `audio` | `null` |
| `audioText` | `null` |
| `sort` | 1-10 |
| `children` | `null` |

**义项选择**：对每个答案词调用 `vocabSenseRepository.findByWordIdAndStatus()`，取第一个 `def_image_id != null` 的义项。

**干扰项**：对每个答案词，从同 HSK 等级有图词中随机取 3 个（使用同一个 random SQL），排除答案词自身及其已在本题中作为干扰项的词。干扰项的选项字母随机分配。

## 数据流

```
1. Controller 收到 GET /api/app/vocab/challenge
2. AppSecurityUtils.getCurrentUserId() → GridUserRepository.findById() → 获取 hskLevel
3. 确定两个出题等级 (levelA=5题, levelB=5题)
4. 对每个等级：
   a. vocabWordRepository.findRandomPublishedByHskLevel(level, N) → VocabWord 实体列表
   b. 遍历每个答案词：
      - vocabSenseRepository.findByWordIdAndStatus() → 义项列表
      - 选第一个 def_image_id != null 的义项 → 提取 def_image_id
      - 同等级再随机取 3 个干扰词 → 排除答案词自身
   c. 为每道题的答案+干扰项随机分配 A/B/C/D
5. 收集所有 def_image_id → aliOssStorageService.findByIds() → Map<Long, AliOssStorageDto>
6. AppVocabChallengeWrapper.toChallengeVOList() 组装 → List<AppExerciseQuestionDetailVO>
7. 返回
```

## 资源解析

与 `AppExerciseQuestionController` 模式一致：

- Controller 从义项 DTO 收集 `def_image_id` 列表
- 调用 `aliOssStorageService.findByIds(imageIds)` 批量查询
- 返回 `Map<Long, AliOssStorageDto>`，传给 Wrapper
- Wrapper 中取 `fileUrl` 设置到 `ImageVO.imageUrl`
- 资源找不到：**不报错**，仅 `log.error`，对应字段返回 `null`

## 架构

```
grid-app/.../AppVocabWordController (新增方法)
    ├── GridUserRepository → 查用户 hskLevel
    ├── VocabWordService → 随机取词
    ├── VocabSenseRepository → 查义项（取 def_image_id）
    ├── AliOssStorageService → 批量查图片 URL
    └── AppVocabChallengeWrapper → DTO → VO 映射

grid-system/.../VocabWordService (新增方法)
    └── VocabWordRepository → 原生 SQL 随机取词
```

## Wrapper 模式

`AppVocabChallengeWrapper` 遵循项目 Wrapper 规范：

- 纯静态方法
- 只做字段映射
- 图片 URL 等外部资源由 Controller 预加载后通过 `Map` 参数传入
- 不注入任何 Spring Bean

```java
public static List<AppExerciseQuestionDetailVO> toChallengeVOList(
    List<ChallengeItem> items,
    Map<Long, AliOssStorageDto> imageMap)
```

其中 `ChallengeItem` 是 Wrapper 包内的临时 DTO（或直接使用现有 DTO 拼接）。

## 需要改动的文件

### grid-system

| 文件 | 变更 |
|------|------|
| `VocabWordRepository.java` | 新增 `findRandomPublishedByHskLevel` 原生 SQL |
| `VocabWordService.java` | 新增 `findRandomPublishedWithImage` 方法声明 |
| `VocabWordServiceImpl.java` | 实现 `findRandomPublishedWithImage` |

### grid-app

| 文件 | 变更 |
|------|------|
| `AppVocabWordController.java` | 新增 `GET /api/app/vocab/challenge`，注入 `GridUserRepository` |
| `AppVocabChallengeWrapper.java` | 新建，静态 Wrapper |

### 不改动

| 文件 | 说明 |
|------|------|
| `AppExerciseQuestionDetailVO.java` | 直接复用，不做修改 |
| `AppVocabWordBaseVO.java` | 不涉及 |

## 错误处理

- 用户未登录：Spring Security 返回 401
- 用户未找到：`EntityNotFoundException`
- 某等级有图词不足 N 条：返回实际可用的数量（程序健壮，不抛异常）
- 图片资源找不到：`log.error` 记录，`imageUrl` 为 `null`，不中断请求

## 验证方式

1. 使用有 HSK 等级的用户 Token 调用接口，验证返回 10 题，5 题来自当前等级 + 5 题来自 +1 等级
2. 使用无 HSK 等级的用户（hskLevel="0"），验证返回 HSK4 + HSK5 各 5 题
3. hsKLevel≥9 用户，验证 10 题全来自等级 9
4. 验证每道题 `content.image.imageUrl` 有值、`content.contentText` 为 null
5. 验证每道题有 4 个选项，`optionText` 为不同词头，正确答案在选项中
6. 验证 `answer` 数组只含正确选项的字母
