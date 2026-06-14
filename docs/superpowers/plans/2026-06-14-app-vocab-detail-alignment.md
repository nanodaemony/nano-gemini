# App Vocab 详情接口结构调整实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 调整 AppVocabWordDetailVO 结构使其与后台 VocabWordVO 对齐，加语言筛选、补 defImageSentence、改进资源错误处理

**Architecture:** 只修改 App 端 VO 类和 Controller 类两个文件。Service/Wrapper 层不动。语言筛选复用 AppCharCharacterController.filterByLanguage 的相同模式。资源查找从静默跳过改为打 ERROR 日志

**Tech Stack:** Java 8, Spring Boot, Lombok, Fastjson2

---

### Task 1: AppVocabWordDetailVO 字段结构调整

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabWordDetailVO.java`

- [ ] **Step 1: 调整 VocabSenseVO 内部类 — translation 改为单条 + 新增 defImageSentence**

在 VocabSenseVO 中：
- 将 `private List<TextTranslationVO> translations;` 改为 `private TextTranslationVO translation;`
- 新增 `private VocabExampleVO defImageSentence;`

```java
// VocabSenseVO 内部，替换 translations 字段
@ApiModelProperty(value = "中文释义外文翻译（按语言筛选后的单条）")
private TextTranslationVO translation;

// VocabSenseVO 内部，新增
@ApiModelProperty(value = "释义图片例句")
private VocabExampleVO defImageSentence;
```

- [ ] **Step 2: 调整 VocabStructureVO 内部类 — patternDefTranslations 改为单条**

将 `private List<TextTranslationVO> patternDefTranslations;` 改为 `private TextTranslationVO patternDefTranslation;`

```java
// VocabStructureVO 内部，替换 patternDefTranslations 字段
@ApiModelProperty(value = "搭配释义外文翻译（按语言筛选后的单条）")
private TextTranslationVO patternDefTranslation;
```

- [ ] **Step 3: 调整 VocabExampleVO 内部类 — translations 改为单条**

将 `private List<TextTranslationVO> translations;` 改为 `private TextTranslationVO translation;`

```java
// VocabExampleVO 内部，替换 translations 字段
@ApiModelProperty(value = "例句外文翻译（按语言筛选后的单条）")
private TextTranslationVO translation;
```

- [ ] **Step 4: 验证编译通过**

```bash
cd /c/Users/nano/Desktop/nano-gemini
mvn compile -pl grid-app -am -DskipTests -q 2>&1 | tail -20
```
Expected: BUILD SUCCESS（会因 Controller 引用了旧字段而报错，这是预期的，下一个 Task 修复）

---

### Task 2: AppVocabWordController 逻辑补齐

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java`

- [ ] **Step 1: 加 @Slf4j 注解**

```java
// 在类上增加
import lombok.extern.slf4j.Slf4j;
// ...
@Slf4j
@RestController
```

- [ ] **Step 2: getDetail 接口增加 language 参数**

```java
@ApiOperation("词汇详情")
@AnonymousGetMapping("/{id}")
public ResponseEntity<AppVocabWordDetailVO> getDetail(
        @PathVariable Integer id,
        @RequestParam String language) {
    if (language == null || language.trim().isEmpty()) {
        throw new IllegalArgumentException("language 参数不能为空");
    }
    VocabWordDto dto = vocabWordService.findPublishedById(id);
    Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);
    Map<Long, AliOssStorageDto> imageMap = collectAndBatchQueryImages(dto);
    AppVocabWordDetailVO vo = toDetailVO(dto, audioMap, imageMap, language);
    return new ResponseEntity<>(vo, HttpStatus.OK);
}
```

- [ ] **Step 3: toDetailVO 传 language 参数**

```java
private AppVocabWordDetailVO toDetailVO(VocabWordDto dto, Map<Long, AudioResourceDto> audioMap,
                                         Map<Long, AliOssStorageDto> imageMap, String language) {
    AppVocabWordDetailVO vo = new AppVocabWordDetailVO();
    vo.setId(dto.getId());
    vo.setWord(dto.getWord());
    vo.setWordTraditional(dto.getWordTraditional());
    vo.setPinyin(dto.getPinyin());
    if (dto.getAudioId() != null) {
        AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
        if (audioDto != null) {
            AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
            audioVO.setAudioUrl(audioDto.getFileUrl());
            vo.setAudio(audioVO);
        } else {
            log.error("词汇音频资源未找到, audioId={}", dto.getAudioId());
        }
    }
    vo.setHskLevel(dto.getHskLevel());
    vo.setSenses(toSenseVOList(dto.getSenses(), audioMap, imageMap, language));
    return vo;
}
```

- [ ] **Step 4: toSenseVOList 和 toSenseVO 传 language + 加 defImageSentence + 资源改日志**

```java
private List<AppVocabWordDetailVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> dtos,
        Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
    if (dtos == null) {
        return Collections.emptyList();
    }
    return dtos.stream().map(dto -> toSenseVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
}

private AppVocabWordDetailVO.VocabSenseVO toSenseVO(VocabSenseDto dto,
        Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
    AppVocabWordDetailVO.VocabSenseVO vo = new AppVocabWordDetailVO.VocabSenseVO();
    vo.setId(dto.getId());
    vo.setPartOfSpeech(dto.getPartOfSpeech());
    vo.setChineseDef(dto.getChineseDef());
    // defAudio
    if (dto.getDefAudioId() != null) {
        AudioResourceDto audioDto = audioMap.get(dto.getDefAudioId());
        if (audioDto != null) {
            AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
            audioVO.setAudioUrl(audioDto.getFileUrl());
            vo.setDefAudio(audioVO);
        } else {
            log.error("义项释义音频资源未找到, audioId={}", dto.getDefAudioId());
        }
    }
    // defImage
    if (dto.getDefImageId() != null) {
        AliOssStorageDto imgDto = imageMap.get(dto.getDefImageId());
        if (imgDto != null) {
            AppVocabWordDetailVO.ImageVO imageVO = new AppVocabWordDetailVO.ImageVO();
            imageVO.setImageUrl(imgDto.getFileUrl());
            vo.setDefImage(imageVO);
        } else {
            log.error("义项释义图片资源未找到, imageId={}", dto.getDefImageId());
        }
    }
    // translation（按语言筛选单条）
    vo.setTranslation(filterByLanguage(dto.getDefTranslations(), language));
    // defImageSentence（释义图片例句）
    if (dto.getDefImageSentence() != null) {
        vo.setDefImageSentence(toExampleVO(dto.getDefImageSentence(), audioMap, imageMap));
    }
    vo.setSynonyms(toSynonymVOList(dto.getSynonymWords()));
    vo.setAntonyms(toAntonymVOList(dto.getAntonymWords()));
    vo.setRelatedForward(toRelatedWordVOList(dto.getSequentialWords()));
    vo.setRelatedBackward(toRelatedWordVOList(dto.getReverseSequentialWords()));
    vo.setRelatedOther(toRelatedWordVOList(dto.getJumbledWords()));
    vo.setSenseOrder(dto.getSenseOrder());
    vo.setStructures(toStructureVOList(dto.getStructures(), audioMap, imageMap, language));
    return vo;
}
```

- [ ] **Step 5: toStructureVO 传 language + patternDefTranslation 改为单条 + 资源改日志**

```java
private List<AppVocabWordDetailVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> dtos,
        Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
    if (dtos == null) {
        return Collections.emptyList();
    }
    return dtos.stream().map(dto -> toStructureVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
}

private AppVocabWordDetailVO.VocabStructureVO toStructureVO(VocabStructureDto dto,
        Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
    AppVocabWordDetailVO.VocabStructureVO vo = new AppVocabWordDetailVO.VocabStructureVO();
    vo.setId(dto.getId());
    vo.setPattern(dto.getPattern());
    vo.setPatternDef(dto.getPatternDef());
    vo.setPatternDefTranslation(filterByLanguage(dto.getPatternDefTranslations(), language));
    vo.setStructureOrder(dto.getStructureOrder());
    vo.setExamples(toExampleVOList(dto.getStructureSentences(), audioMap, imageMap));
    return vo;
}
```

- [ ] **Step 6: toExampleVO / toExampleVOList 加 language 参数 + 翻译改单条 + 资源改日志**

```java
private AppVocabWordDetailVO.VocabExampleVO toExampleVO(ExampleSentenceDto dto,
        Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
    AppVocabWordDetailVO.VocabExampleVO vo = new AppVocabWordDetailVO.VocabExampleVO();
    vo.setId(dto.getId() != null ? dto.getId().intValue() : null);
    vo.setSentence(dto.getSentence());
    if (dto.getAudioId() != null) {
        AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
        if (audioDto != null) {
            AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
            audioVO.setAudioUrl(audioDto.getFileUrl());
            vo.setAudio(audioVO);
        } else {
            log.error("例句音频资源未找到, audioId={}", dto.getAudioId());
        }
    }
    vo.setPinyin(dto.getPinyin());
    vo.setTranslation(filterByLanguage(dto.getTranslations(), language));
    if (dto.getImageId() != null) {
        AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
        if (imgDto != null) {
            AppVocabWordDetailVO.ImageVO imageVO = new AppVocabWordDetailVO.ImageVO();
            imageVO.setImageUrl(imgDto.getFileUrl());
            vo.setImage(imageVO);
        } else {
            log.error("例句图片资源未找到, imageId={}", dto.getImageId());
        }
    }
    vo.setExampleOrder(dto.getOrder());
    return vo;
}

private List<AppVocabWordDetailVO.VocabExampleVO> toExampleVOList(List<ExampleSentenceDto> dtos,
        Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap, String language) {
    if (dtos == null) {
        return Collections.emptyList();
    }
    return dtos.stream().map(dto -> toExampleVO(dto, audioMap, imageMap, language)).collect(Collectors.toList());
}
```

同步更新 `toStructureVO` 中对 `toExampleVOList` 的调用，传入 language 参数：

```java
// toStructureVO 内
vo.setExamples(toExampleVOList(dto.getStructureSentences(), audioMap, imageMap, language));
```

- [ ] **Step 7: 新增 filterByLanguage 和 toTextTranslationVO 辅助方法**

```java
private TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
    if (translations == null || language == null) {
        return null;
    }
    return translations.stream()
            .filter(t -> language.equals(t.getLanguage()))
            .findFirst()
            .map(this::toTextTranslationVO)
            .orElse(null);
}

private TextTranslationVO toTextTranslationVO(TextTranslation translation) {
    if (translation == null) {
        return null;
    }
    TextTranslationVO vo = new TextTranslationVO();
    vo.setLanguage(translation.getLanguage());
    vo.setTranslation(translation.getTranslation());
    return vo;
}
```

注意：删除原有的 `toTextTranslationVOList` 方法（不再需要）。

- [ ] **Step 8: collectAndBatchQuery 补充 defImageSentence 的资源收集**

在 `collectAndBatchQueryAudios` 和 `collectAndBatchQueryImages` 中，在遍历 sense 时补充收集 `defImageSentence` 的 `audioId`/`imageId`。

```java
// collectAndBatchQueryAudios 补充，在 senses 遍历内：
if (sense.getDefImageSentence() != null && sense.getDefImageSentence().getAudioId() != null) {
    audioIds.add(sense.getDefImageSentence().getAudioId());
}

// collectAndBatchQueryImages 补充，在 senses 遍历内：
if (sense.getDefImageSentence() != null && sense.getDefImageSentence().getImageId() != null) {
    imageIds.add(sense.getDefImageSentence().getImageId());
}
```

- [ ] **Step 9: 验证编译通过**

```bash
cd /c/Users/nano/Desktop/nano-gemini
mvn compile -pl grid-app -am -DskipTests -q 2>&1 | tail -20
```
Expected: BUILD SUCCESS

---

### Task 3: 编译验证和提交

- [ ] **Step 1: 全量编译验证**

```bash
cd /c/Users/nano/Desktop/nano-gemini
mvn compile -DskipTests -q 2>&1 | tail -30
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 提交变更**

```bash
git add -A
git commit -m "feat: align AppVocabWordDetailVO with VocabWordVO, add language filter, add defImageSentence, improve resource error handling"
```
