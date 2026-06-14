# App 词汇详情接口结构调整设计

## 概述

对 `AppVocabWordController#getDetail` 接口的响应 VO `AppVocabWordDetailVO` 进行结构调整，使其与后台管理端的 `VocabWordVO` 结构对齐，同时：
- 剔除管理端专属字段（审核人、创建人、时间、草稿态、发布态）
- 新增语言参数筛选翻译内容
- 补充缺失的 `defImageSentence`（释义图片例句）
- 改进资源查找错误处理（打 ERROR 日志而非静默跳过）

## 设计

### 1. AppVocabWordDetailVO 结构调整

#### 顶部层（不变）

```java
public class AppVocabWordDetailVO {
    private Integer id;
    private String word;
    private String wordTraditional;
    private String pinyin;
    private AudioVO audio;           // 已包装为 VO ✓
    private String hskLevel;
    private List<VocabSenseVO> senses;
}
```

#### VocabSenseVO（调整翻译字段类型 + 新增 defImageSentence）

| 字段 | 调整前 | 调整后 |
|------|--------|--------|
| `translations` | `List<TextTranslationVO>` | `TextTranslationVO translation` — 按语言筛选后的单条 |
| `defImageSentence` | 无 | `VocabExampleVO defImageSentence` — 新增，复用例句VO结构 |

其余字段（`id`, `partOfSpeech`, `chineseDef`, `defAudio`, `defImage`, `synonyms`, `antonyms`, `relatedForward`, `relatedBackward`, `relatedOther`, `senseOrder`, `structures`）保持不动。

#### VocabStructureVO（调整翻译字段类型）

| 字段 | 调整前 | 调整后 |
|------|--------|--------|
| `patternDefTranslations` | `List<TextTranslationVO>` | `TextTranslationVO patternDefTranslation` — 按语言筛选后的单条 |

#### VocabExampleVO（调整翻译字段类型）

| 字段 | 调整前 | 调整后 |
|------|--------|--------|
| `translations` | `List<TextTranslationVO>` | `TextTranslationVO translation` — 按语言筛选后的单条 |

### 2. Controller 接口变更

```java
@AnonymousGetMapping("/{id}")
public ResponseEntity<AppVocabWordDetailVO> getDetail(
        @PathVariable Integer id,
        @RequestParam String language)
```

加 language 参数非空校验，参考 `AppCharCharacterController#getDetail`。

### 3. 语言筛选逻辑

复用 `AppCharCharacterController.filterByLanguage` 模式：

```java
private TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
    if (translations == null || language == null) return null;
    return translations.stream()
            .filter(t -> language.equals(t.getLanguage()))
            .findFirst()
            .map(this::toTextTranslationVO)
            .orElse(null);
}
```

应用位置：
- `toSenseVO` 中 `vo.setTranslation(filterByLanguage(dto.getDefTranslations(), language))`
- `toStructureVO` 中 `vo.setPatternDefTranslation(filterByLanguage(dto.getPatternDefTranslations(), language))`
- `toExampleVO` 中 `vo.setTranslation(filterByLanguage(dto.getTranslations(), language))`

### 4. defImageSentence 处理

在 `toSenseVO` 中补充：

```java
if (dto.getDefImageSentence() != null) {
    vo.setDefImageSentence(toExampleVO(dto.getDefImageSentence(), audioMap, imageMap));
}
```

同时在 `collectAndBatchQueryAudios` 和 `collectAndBatchQueryImages` 中收集 `defImageSentence` 的 `audioId` 和 `imageId`。

### 5. 资源查找错误处理

将资源查找失败从静默跳过改为打 ERROR 日志：

```java
if (dto.getAudioId() != null) {
    AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
    if (audioDto != null) {
        audioVO.setAudioUrl(audioDto.getFileUrl());
    } else {
        log.error("词汇音频资源未找到, audioId={}", dto.getAudioId());
    }
}
```

Controller 类需加 `@Slf4j` 注解（若已有则复用）。

### 6. 涉及修改的文件

| 文件 | 改动 |
|---|---|
| `grid-app/.../vo/AppVocabWordDetailVO.java` | 字段类型变更 + 新增 `defImageSentence` |
| `grid-app/.../rest/AppVocabWordController.java` | 加 language 参数、filterByLanguage 方法、defImageSentence 处理、资源日志 |

Service/Repository/Wrapper 层不需要调整。

## 不变的部分

- Service 层 `vocabWordService.findPublishedById()` 不变
- `collectAndBatchQueryAudios()` 和 `collectAndBatchQueryImages()` 的批量查询逻辑不变（仅新增收集 defImageSentence 的资源 ID）
- 搜索接口 `/search` 不变
- `AppVocabWordBaseVO` 不变
