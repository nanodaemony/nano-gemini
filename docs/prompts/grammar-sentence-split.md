# 语法管理：例句拆分为正确例句 / 错误例句

## 背景

当前"语法注意"(`grammar_notice`)和"语法偏误"(`grammar_error`)板块中，例句是一个平铺列表，无法区分"正确例句"和"错误例句"（偏误的例子）。

需求：将例句拆分为两类：
- **正确例句 (`correctSentences`)**：需要展示拼音、翻译、音频、图片等完整富内容
- **错误例句 (`incorrectSentences`)**：只需展示例句文本，不需要拼音/翻译/音频/图片等

在 App 端 VO 中，错误例句直接用 `List<String>` 表示（只传 `sentence` 文本字段），不用 `ExampleVO`。

---

## 一、数据库变更

### 1.1 `grammar_notice` 表

```sql
-- 新增两个字段
ALTER TABLE `grammar_notice`
  ADD COLUMN `correct_sentence_ids` VARCHAR(128) DEFAULT NULL 
    COMMENT '正确例句ID列表, JSON格式, e.g. [1,2,3]',
  ADD COLUMN `incorrect_sentence_ids` VARCHAR(128) DEFAULT NULL 
    COMMENT '错误例句ID列表, JSON格式, e.g. [4,5]';

-- 数据迁移：将原 notice_sentence_ids 全部当作正确例句
UPDATE `grammar_notice` 
SET `correct_sentence_ids` = `notice_sentence_ids`
WHERE `notice_sentence_ids` IS NOT NULL;

-- 废弃旧字段（先保留观察，确认无误后再删）
-- ALTER TABLE `grammar_notice` DROP COLUMN `notice_sentence_ids`;
```

### 1.2 `grammar_error` 表

```sql
-- 新增两个例句字段
ALTER TABLE `grammar_error`
  ADD COLUMN `correct_sentence_ids` VARCHAR(128) DEFAULT NULL 
    COMMENT '正确例句ID列表, JSON格式',
  ADD COLUMN `incorrect_sentence_ids` VARCHAR(128) DEFAULT NULL 
    COMMENT '错误例句ID列表, JSON格式';
```

> `example_sentence` 表无需任何改动 — 正确和错误例句共用同一张表，区别仅在于引用的字段不同。展示时正确例句走富内容渲染，错误例句只取 `sentence` 文本。

---

## 二、Domain Entity 变更

### 2.1 `GrammarNotice.java`

```
路径: grid-system/src/main/java/.../domain/grammar/GrammarNotice.java
```

- 废弃字段 `noticeSentenceIds` → 新增字段 `correctSentenceIds` + `incorrectSentenceIds`
- getter/setter 对应新字段

```java
// 旧
@Column(name = "notice_sentence_ids", length = 128)
private String noticeSentenceIds;

// 新
@Column(name = "correct_sentence_ids", length = 128)
private String correctSentenceIds;

@Column(name = "incorrect_sentence_ids", length = 128)
private String incorrectSentenceIds;
```

### 2.2 `GrammarError.java`

```
路径: grid-system/src/main/java/.../domain/grammar/GrammarError.java
```

- 新增字段 `correctSentenceIds` + `incorrectSentenceIds`

```java
@Column(name = "correct_sentence_ids", length = 128)
private String correctSentenceIds;

@Column(name = "incorrect_sentence_ids", length = 128)
private String incorrectSentenceIds;
```

---

## 三、DTO 变更

### 3.1 `GrammarNoticeDto.java`

```
路径: grid-system/src/main/java/.../service/grammar/dto/GrammarNoticeDto.java
```

```java
// 旧
@ApiModelProperty(value = "例句列表")
private List<ExampleSentenceDto> sentences;

// 新
@ApiModelProperty(value = "正确例句列表")
private List<ExampleSentenceDto> correctSentences;

@ApiModelProperty(value = "错误例句列表")
private List<ExampleSentenceDto> incorrectSentences;
```

> 注意：DTO 中错误例句仍然用 `List<ExampleSentenceDto>`（统一存储），但在 **App 端 Wrapper** 中映射为 `List<String>`。

### 3.2 `GrammarErrorDto.java`

```
路径: grid-system/src/main/java/.../service/grammar/dto/GrammarErrorDto.java
```

```java
// 新增
@ApiModelProperty(value = "正确例句列表")
private List<ExampleSentenceDto> correctSentences;

@ApiModelProperty(value = "错误例句列表")
private List<ExampleSentenceDto> incorrectSentences;
```

---

## 四、Admin Request 变更

### 4.1 `GrammarNoticeRequest` (内嵌于 `GrammarPointCreateRequest.java`)

```
路径: grid-system/src/main/java/.../rest/request/GrammarPointCreateRequest.java
```

```java
// 旧
@Valid
@ApiModelProperty(value = "例句列表")
private List<ExampleSentenceRequest> sentences;

// 新
@Valid
@ApiModelProperty(value = "正确例句列表")
private List<ExampleSentenceRequest> correctSentences;

@Valid
@ApiModelProperty(value = "错误例句列表")
private List<ExampleSentenceRequest> incorrectSentences;
```

### 4.2 `GrammarErrorRequest` (内嵌于 `GrammarPointCreateRequest.java`)

```java
// 新增
@Valid
@ApiModelProperty(value = "正确例句列表")
private List<ExampleSentenceRequest> correctSentences;

@Valid
@ApiModelProperty(value = "错误例句列表")
private List<ExampleSentenceRequest> incorrectSentences;
```

---

## 五、Admin VO 变更

### 5.1 `GrammarNoticeVO` (内嵌于 `GrammarPointVO.java`)

```
路径: grid-system/src/main/java/.../rest/vo/GrammarPointVO.java
```

```java
// 旧
@ApiModelProperty(value = "例句列表")
private List<ExampleSentenceVO> sentences;

// 新
@ApiModelProperty(value = "正确例句列表")
private List<ExampleSentenceVO> correctSentences;

@ApiModelProperty(value = "错误例句列表")
private List<ExampleSentenceVO> incorrectSentences;
```

### 5.2 `GrammarErrorVO` (内嵌于 `GrammarPointVO.java`)

```java
// 新增
@ApiModelProperty(value = "正确例句列表")
private List<ExampleSentenceVO> correctSentences;

@ApiModelProperty(value = "错误例句列表")
private List<ExampleSentenceVO> incorrectSentences;
```

---

## 六、App VO 变更

### 6.1 `AppGrammarPointDetailVO.GrammarNoticeVO`

```
路径: grid-app/src/main/java/.../app/rest/vo/AppGrammarPointDetailVO.java
```

```java
// 旧
@ApiModelProperty(value = "例句列表")
private List<ExampleVO> sentences;

// 新
@ApiModelProperty(value = "正确例句列表（含拼音、翻译、音频、图片等）")
private List<ExampleVO> correctSentences;

@ApiModelProperty(value = "错误例句文本列表（仅例句文案，无富内容）")
private List<String> incorrectSentences;
```

### 6.2 `AppGrammarPointDetailVO.GrammarErrorVO`

```java
// 新增
@ApiModelProperty(value = "正确例句列表（含拼音、翻译、音频、图片等）")
private List<ExampleVO> correctSentences;

@ApiModelProperty(value = "错误例句文本列表（仅例句文案，无富内容）")
private List<String> incorrectSentences;
```

---

## 七、Service 实现变更

### 7.1 `GrammarPointServiceImpl.java`

```
路径: grid-system/src/main/java/.../service/grammar/impl/GrammarPointServiceImpl.java
```

以下按方法逐一说明：

#### `syncNotices()` (行 615-674)

当前逻辑是 `saveSentencesAndCollectIds(dto.getSentences())` 存入 `noticeSentenceIds`。改为：

```java
// 分别保存正确例句和错误例句
List<Long> correctIds = saveSentencesAndCollectIds(dto.getCorrectSentences());
entity.setCorrectSentenceIds(JsonUtils.toJson(correctIds));

List<Long> incorrectIds = saveSentencesAndCollectIds(dto.getIncorrectSentences());
entity.setIncorrectSentenceIds(JsonUtils.toJson(incorrectIds));
```

#### `syncErrors()` (行 676-732)

当前只存 `errorContent`、`errorAnalysis` 等，没有例句。新增：

```java
// 新增：保存正确例句和错误例句
List<Long> correctIds = saveSentencesAndCollectIds(dto.getCorrectSentences());
entity.setCorrectSentenceIds(JsonUtils.toJson(correctIds));

List<Long> incorrectIds = saveSentencesAndCollectIds(dto.getIncorrectSentences());
entity.setIncorrectSentenceIds(JsonUtils.toJson(incorrectIds));
```

#### `disableChildSentences()` 调用点 (行 542, 602, 663)

`syncErrors()` 的删除分支中也需要添加 `disableChildSentences()` 调用：

```java
// 在 syncErrors 的删除循环后
disableChildSentences(toDelete, GrammarError::getCorrectSentenceIds);
disableChildSentences(toDelete, GrammarError::getIncorrectSentenceIds);
```

同样 `syncNotices()` 中原来是：
```java
disableChildSentences(toDelete, GrammarNotice::getNoticeSentenceIds);
```
改为：
```java
disableChildSentences(toDelete, GrammarNotice::getCorrectSentenceIds);
disableChildSentences(toDelete, GrammarNotice::getIncorrectSentenceIds);
```

> 注意 `disableChildSentences` 的 lambda 签名需要匹配新字段名 getter。

#### `convertToNoticeDtos()` (行 846-877)

原逻辑解析 `noticeSentenceIds` → `sentences`。改为解析两个字段：

```java
// 解析正确例句
String correctIdsJson = entity.getCorrectSentenceIds();
if (correctIdsJson != null) {
    List<Long> ids = JSON.parseArray(correctIdsJson, Long.class);
    if (ids != null) {
        dto.setCorrectSentences(ids.stream().map(id -> {
            ExampleSentenceDto s = new ExampleSentenceDto();
            s.setId(id);
            return s;
        }).collect(Collectors.toList()));
    }
}

// 解析错误例句
String incorrectIdsJson = entity.getIncorrectSentenceIds();
if (incorrectIdsJson != null) {
    List<Long> ids = JSON.parseArray(incorrectIdsJson, Long.class);
    if (ids != null) {
        dto.setIncorrectSentences(ids.stream().map(id -> {
            ExampleSentenceDto s = new ExampleSentenceDto();
            s.setId(id);
            return s;
        }).collect(Collectors.toList()));
    }
}
```

#### `convertToErrorDtos()` (行 879-896)

新增正确/错误例句ID的解析（逻辑同上，从 entity 的 `correctSentenceIds` / `incorrectSentenceIds` 解析并设入 DTO）。

#### `hydrateNoticeSentences()` (行 325-351)

当前只 hydrate 一个 `sentences` 列表。改为分别 hydrate `correctSentences` 和 `incorrectSentences`：

```java
private List<GrammarNoticeDto> hydrateNoticeSentences(List<GrammarNoticeDto> notices) {
    if (notices == null || notices.isEmpty()) return notices;
    
    // 收集所有例句ID
    List<Long> allSentenceIds = new ArrayList<>();
    for (GrammarNoticeDto n : notices) {
        collectIds(n.getCorrectSentences(), allSentenceIds);
        collectIds(n.getIncorrectSentences(), allSentenceIds);
    }
    if (allSentenceIds.isEmpty()) return notices;
    
    Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(allSentenceIds);
    
    for (GrammarNoticeDto n : notices) {
        hydrateList(n.getCorrectSentences(), sentenceMap);
        hydrateList(n.getIncorrectSentences(), sentenceMap);
    }
    return notices;
}
```

#### `hydrateErrorSentences()` — 新增方法

语法偏误之前没有例句，现在需要新增 hydrate 方法。逻辑与 `hydrateNoticeSentences` 一致：

```java
private List<GrammarErrorDto> hydrateErrorSentences(List<GrammarErrorDto> errors) {
    if (errors == null || errors.isEmpty()) return errors;
    
    List<Long> allSentenceIds = new ArrayList<>();
    for (GrammarErrorDto e : errors) {
        collectIds(e.getCorrectSentences(), allSentenceIds);
        collectIds(e.getIncorrectSentences(), allSentenceIds);
    }
    if (allSentenceIds.isEmpty()) return errors;
    
    Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(allSentenceIds);
    
    for (GrammarErrorDto e : errors) {
        hydrateList(e.getCorrectSentences(), sentenceMap);
        hydrateList(e.getIncorrectSentences(), sentenceMap);
    }
    return errors;
}
```

#### `toPublishedDetailDto()` (行 252-265)

当前 errors 只走 `convertToErrorDtos()` 无 hydrate。改为：

```java
dto.setErrors(sortErrorsDesc(hydrateErrorSentences(convertToErrorDtos(
    grammarErrorRepository.findByGrammarIdAndStatus(id, StatusEnum.ENABLED.getCode())))));
```

#### `collectGrammarMarkers()` (行 900-961)

在 error 循环中新增例句的 marker 收集（与 notice/meaning/structure 一致）：

```java
if (errors != null) {
    for (GrammarErrorDto e : errors) {
        if (e.getId() != null && e.getAiGeneratedFields() != null) {
            entries.add(new AiContentMarkerService.MarkerEntry(
                    "grammar_error", e.getId(), e.getAiGeneratedFields()));
        }
        // 新增：例句 markers
        if (e.getCorrectSentences() != null) {
            for (ExampleSentenceDto es : e.getCorrectSentences()) {
                if (es.getId() != null && es.getAiGeneratedFields() != null) {
                    entries.add(new AiContentMarkerService.MarkerEntry(
                            "example_sentence", es.getId(), es.getAiGeneratedFields()));
                }
            }
        }
        if (e.getIncorrectSentences() != null) {
            for (ExampleSentenceDto es : e.getIncorrectSentences()) {
                if (es.getId() != null && es.getAiGeneratedFields() != null) {
                    entries.add(new AiContentMarkerService.MarkerEntry(
                            "example_sentence", es.getId(), es.getAiGeneratedFields()));
                }
            }
        }
    }
}
```

同时别忘了在 notices 循环中也改为分别处理 correct/incorrect（当前是遍历 `sentences`）。

---

## 八、Wrapper 变更

### 8.1 `GrammarPointWrapper` (Admin 端)

```
路径: grid-system/src/main/java/.../rest/wrapper/GrammarPointWrapper.java
```

#### `toNoticeDto()` (行 146-155)

```java
// 旧
dto.setSentences(toExampleSentenceDtoList(request.getSentences()));

// 新
dto.setCorrectSentences(toExampleSentenceDtoList(request.getCorrectSentences()));
dto.setIncorrectSentences(toExampleSentenceDtoList(request.getIncorrectSentences()));
```

#### `toErrorDto()` (行 162-171)

新增例句转换：

```java
dto.setCorrectSentences(toExampleSentenceDtoList(request.getCorrectSentences()));
dto.setIncorrectSentences(toExampleSentenceDtoList(request.getIncorrectSentences()));
```

#### `toNoticeVO()` (行 242-265)

```java
// 旧
vo.setSentences(toExampleSentenceVOList(dto.getSentences(), aiMarkers));

// 新
vo.setCorrectSentences(toExampleSentenceVOList(dto.getCorrectSentences(), aiMarkers));
vo.setIncorrectSentences(toExampleSentenceVOList(dto.getIncorrectSentences(), aiMarkers));
```

#### `toErrorVO()` (行 273-296)

新增例句转换：

```java
vo.setCorrectSentences(toExampleSentenceVOList(dto.getCorrectSentences(), aiMarkers));
vo.setIncorrectSentences(toExampleSentenceVOList(dto.getIncorrectSentences(), aiMarkers));
```

### 8.2 `AppGrammarPointWrapper` (App 端)

```
路径: grid-app/src/main/java/.../app/rest/wrapper/AppGrammarPointWrapper.java
```

关键区别：错误例句只输出文本 `List<String>`，不走 `ExampleVO`。

#### `toNoticeVO()` (行 138-148)

```java
// 旧
vo.setSentences(toExampleVOList(dto.getSentences(), audioMap, imageMap, language));

// 新
// 正确例句 — 走完整富内容
vo.setCorrectSentences(toExampleVOList(dto.getCorrectSentences(), audioMap, imageMap, language));
// 错误例句 — 只取 sentence 文本
vo.setIncorrectSentences(toSentenceTextList(dto.getIncorrectSentences()));
```

#### `toErrorVO()` (行 160-168)

```java
// 新增
vo.setCorrectSentences(toExampleVOList(dto.getCorrectSentences(), audioMap, imageMap, language));
vo.setIncorrectSentences(toSentenceTextList(dto.getIncorrectSentences()));
```

#### 新增工具方法

```java
/**
 * 从 ExampleSentenceDto 列表中提取纯文本列表（用于错误例句）
 */
private static List<String> toSentenceTextList(List<ExampleSentenceDto> dtos) {
    if (dtos == null) {
        return Collections.emptyList();
    }
    return dtos.stream()
            .map(ExampleSentenceDto::getSentence)
            .filter(s -> s != null && !s.trim().isEmpty())
            .collect(Collectors.toList());
}
```

---

## 九、影响范围总结

| 文件 | 模块 | 变更类型 |
|------|------|---------|
| `sql/biz_grammer.sql` | SQL | DDL 变更 |
| `GrammarNotice.java` | grid-system domain | 字段替换 |
| `GrammarError.java` | grid-system domain | 字段新增 |
| `GrammarNoticeDto.java` | grid-system dto | 字段替换 |
| `GrammarErrorDto.java` | grid-system dto | 字段新增 |
| `GrammarPointCreateRequest.java` | grid-system request | 内嵌类字段变更 |
| `GrammarPointVO.java` | grid-system vo | 内嵌类字段变更 |
| `GrammarPointServiceImpl.java` | grid-system service | 多处逻辑变更 |
| `GrammarPointWrapper.java` | grid-system wrapper | 映射逻辑变更 |
| `AppGrammarPointDetailVO.java` | grid-app vo | 内嵌类字段变更 |
| `AppGrammarPointWrapper.java` | grid-app wrapper | 映射逻辑变更 |

> 所有 `sentences` → `correctSentences` + `incorrectSentences` 的变更覆盖了完整的链路：DB → Entity → DTO → Request/VO → Wrapper。草稿 JSON（`draft_content`）存储在 `grammar_point` 表的 text 列中，其序列化对象是 `GrammarPointDto`，所以 DTO 字段变更会自动映射到草稿的 JSON 结构，无需额外处理 `GrammarPoint` 实体本身。
