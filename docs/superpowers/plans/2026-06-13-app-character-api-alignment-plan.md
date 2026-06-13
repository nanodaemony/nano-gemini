# App 端汉字接口字段对齐 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `AppCharCharacterDetailVO` 与 admin `CharCharacterVO` 字段对齐，新增笔顺接口、语言参数过滤翻译

**Architecture:** grid-app 模块内部变更，不涉及底层 service/entity，仅修改 VO 类和 Controller 映射逻辑。新增 `AppCharStrokeVO` 和笔顺端点，复用 `CharStrokeService` 和 `CharStrokeWrapper` 做 JSON 解析。

**Tech Stack:** Spring Boot, Java 8, Lombok, Fastjson2

---

### Task 1: 修改 AppCharCharacterDetailVO（顶层字段 + 辨析内部类 + 组词内部类）

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterDetailVO.java`

- [ ] **Step 1: 修改顶层字段**

改造前后的完整 VO：

```java
@Getter
@Setter
public class AppCharCharacterDetailVO implements Serializable {

    private Integer id;
    private String character;
    private String hskLevel;                          // 原 level
    private String pinyin;
    private AudioVO audio;
    private String traditional;
    private String radical;
    private String componentCombination;              // ➕ 新增
    private String charDesc;
    private TextTranslationVO descTranslation;        // 原 List<TextTranslationVO> descTranslations
    private List<CharDiscriminationVO> discriminations;
    private List<CharWordVO> words;
    // stroke 已移除

    // ... 内部类保持不变，后续步骤修改
}
```

将以下代码行替换（需要 Read 文件后做 Edit）：

原字段 `private String level;` 改为 `private String hskLevel;`

原字段 `private List<TextTranslationVO> descTranslations;` 改为 `private TextTranslationVO descTranslation;`

在 `private String charDesc;` 后新增一行 `private String componentCombination;`

删除 `private String stroke;` 字段行

- [ ] **Step 2: 修改辨析内部类（CharDiscriminationVO）**

替换 `CharDiscriminationVO` 内部类为：

```java
@Getter
@Setter
public static class CharDiscriminationVO implements Serializable {

    @ApiModelProperty(value = "辨析汉字")
    private String comparisonChar;                           // 原 discrimChar

    @ApiModelProperty(value = "辨析拼音")
    private String comparisonPinyin;                         // 原 discrimPinyin

    @ApiModelProperty(value = "辨析汉字翻译")
    private TextTranslationVO discrimCharTranslation;        // 原 List<TextTranslationVO> discrimCharTranslations

    @ApiModelProperty(value = "对比辨析说明翻译")
    private TextTranslationVO comparisonDescTranslation;      // 原 List<TextTranslationVO> comparisonTranslations

    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer order;                                   // ➕ 新增
}
```

- [ ] **Step 3: 修改组词内部类（CharWordVO）中变更字段**

找到 `CharWordVO` 内部类，做以下修改：

原 `private String level;` → `private String hskLevel;`

原 `private List<TextTranslationVO> wordItemTranslations;` → `private TextTranslationVO wordItemTranslation;`

原 `private List<TextTranslationVO> exampleTranslations;` → `private TextTranslationVO exampleTranslation;`

- [ ] **Step 4: 确认文件完整**

Read 最终文件确认所有修改都已正确应用，没有遗留旧的字段名。

---

### Task 2: 修改 AppCharCharacterBaseVO

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterBaseVO.java`

- [ ] **Step 1: 重命名字段**

原 `private String level;` 改为 `private String hskLevel;`

```java
@ApiModelProperty(value = "HSK等级")
private String hskLevel;            // 原 level
```

---

### Task 3: 新增 AppCharStrokeVO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharStrokeVO.java`

- [ ] **Step 1: 创建新文件**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppCharStrokeVO implements Serializable {

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "笔顺SVG路径数据")
    private List<String> strokes;

    @ApiModelProperty(value = "笔顺坐标参考线数据（每个元素为笔画的坐标点列表）")
    private List<List<List<Integer>>> medians;

    @ApiModelProperty(value = "部首笔画索引（部分汉字有此数据）")
    private List<Integer> radStrokes;
}
```

---

### Task 4: 修改 AppCharCharacterController 逻辑

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java`

此任务需要改动多处。为减少冲突，按方法逐一修改。

- [ ] **Step 1: 更新 import 和入参**

新增 import：

```java
import com.naon.grid.backend.rest.vo.CharStrokeVO;
import com.naon.grid.backend.rest.wrapper.CharStrokeWrapper;
import com.naon.grid.modules.app.rest.vo.AppCharStrokeVO;
```

修改 `getDetail` 方法签名，新增 `language` 参数：

```java
@ApiOperation("根据ID查询汉字详情")
@AnonymousGetMapping("/{id}")
public ResponseEntity<AppCharCharacterDetailVO> getDetail(
        @PathVariable Integer id,
        @RequestParam String language) {
    if (language == null || language.trim().isEmpty()) {
        throw new IllegalArgumentException("language 参数不能为空");
    }
    CharCharacterDto dto = charCharacterService.findPublishedById(id);
    AppCharCharacterDetailVO vo = toDetailVO(dto, language);
    return new ResponseEntity<>(vo, HttpStatus.OK);
}
```

注意：`toDetailVO` 调用改为带 `language` 参数的重载，下一步会实现。

- [ ] **Step 2: 新增翻译过滤工具方法**

在 `toTextTranslationVO` 方法之后新增：

```java
/**
 * 从翻译列表中按语言过滤出对应语言的单条翻译
 */
private TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
    if (translations == null || language == null) {
        return null;
    }
    return translations.stream()
            .filter(t -> language.equals(t.getLanguage()))
            .findFirst()
            .map(t -> {
                TextTranslationVO vo = new TextTranslationVO();
                vo.setLanguage(t.getLanguage());
                vo.setTranslation(t.getTranslation());
                return vo;
            })
            .orElse(null);
}
```

- [ ] **Step 3: 修改 toDetailVO 方法**

替换原 `toDetailVO`：

```java
private AppCharCharacterDetailVO toDetailVO(CharCharacterDto dto, String language) {
    AppCharCharacterDetailVO vo = new AppCharCharacterDetailVO();
    vo.setId(dto.getId());
    vo.setCharacter(dto.getCharacter());
    vo.setHskLevel(dto.getHskLevel());
    vo.setPinyin(dto.getPinyin());
    if (dto.getAudioId() != null) {
        AudioResourceDto audioDto = audioResourceService.findById(dto.getAudioId());
        if (audioDto != null) {
            AppCharCharacterDetailVO.AudioVO audioVO = new AppCharCharacterDetailVO.AudioVO();
            audioVO.setAudioUrl(audioDto.getFileUrl());
            vo.setAudio(audioVO);
        }
    }
    vo.setTraditional(dto.getTraditional());
    vo.setRadical(dto.getRadical());
    vo.setComponentCombination(dto.getComponentCombination());
    vo.setCharDesc(dto.getCharDesc());
    vo.setDescTranslation(filterByLanguage(dto.getDescTranslations(), language));
    vo.setDiscriminations(toDiscriminationVOList(dto.getComparisons(), language));
    vo.setWords(toWordVOList(dto.getWords(), language));
    return vo;
}
```

关键变更点：
- `vo.setLevel(dto.getHskLevel())` → `vo.setHskLevel(dto.getHskLevel())`
- 新增 `vo.setComponentCombination(dto.getComponentCombination())`
- 移除 `stroke` 相关代码
- `descTranslations` 改为 `descTranslation` + `filterByLanguage`
- `toDiscriminationVOList` 和 `toWordVOList` 传递 `language`

- [ ] **Step 4: 修改 toBaseVO 方法**

```java
private AppCharCharacterBaseVO toBaseVO(CharCharacterDto dto) {
    AppCharCharacterBaseVO vo = new AppCharCharacterBaseVO();
    vo.setId(dto.getId());
    vo.setCharacter(dto.getCharacter());
    vo.setHskLevel(dto.getHskLevel());
    vo.setPinyin(dto.getPinyin());
    return vo;
}
```

变更：`vo.setLevel(...)` → `vo.setHskLevel(...)`

- [ ] **Step 5: 修改辨析相关方法**

```java
private List<AppCharCharacterDetailVO.CharDiscriminationVO> toDiscriminationVOList(
        List<CharComparisonDto> dtos, String language) {
    if (dtos == null) {
        return Collections.emptyList();
    }
    return dtos.stream().map(dto -> toDiscriminationVO(dto, language)).collect(Collectors.toList());
}

private AppCharCharacterDetailVO.CharDiscriminationVO toDiscriminationVO(
        CharComparisonDto dto, String language) {
    AppCharCharacterDetailVO.CharDiscriminationVO vo = new AppCharCharacterDetailVO.CharDiscriminationVO();
    vo.setComparisonChar(dto.getComparisonChar());
    vo.setComparisonPinyin(dto.getComparisonPinyin());
    vo.setDiscrimCharTranslation(filterByLanguage(dto.getComparisonCharTranslations(), language));
    vo.setComparisonDescTranslation(filterByLanguage(dto.getComparisonDescTranslations(), language));
    vo.setOrder(dto.getOrder());
    return vo;
}
```

变更点：
- `setDiscrimChar` → `setComparisonChar`
- `setDiscrimPinyin` → `setComparisonPinyin`
- 移除 `setId`
- `setDiscrimCharTranslations` → `setDiscrimCharTranslation` + `filterByLanguage`
- `setComparisonTranslations` → `setComparisonDescTranslation` + `filterByLanguage`
- 新增 `setOrder`

- [ ] **Step 6: 修改组词相关方法**

```java
private List<AppCharCharacterDetailVO.CharWordVO> toWordVOList(
        List<CharWordDto> dtos, String language) {
    if (dtos == null) {
        return Collections.emptyList();
    }
    return dtos.stream().map(dto -> toWordVO(dto, language)).collect(Collectors.toList());
}

private AppCharCharacterDetailVO.CharWordVO toWordVO(CharWordDto dto, String language) {
    AppCharCharacterDetailVO.CharWordVO vo = new AppCharCharacterDetailVO.CharWordVO();
    vo.setWordItem(dto.getWordItem());
    vo.setHskLevel(dto.getHskLevel());
    vo.setPinyin(dto.getPinyin());
    vo.setPartOfSpeech(dto.getPartOfSpeech());
    vo.setWordItemTranslation(filterByLanguage(dto.getWordItemTranslations(), language));
    ExampleSentenceDto sentenceDto = dto.getWordItemSentence();
    if (sentenceDto != null) {
        vo.setExampleSentence(sentenceDto.getSentence());
        vo.setExamplePinyin(sentenceDto.getPinyin());
        vo.setExampleTranslation(filterByLanguage(sentenceDto.getTranslations(), language));
        if (sentenceDto.getImageId() != null) {
            AliOssStorageDto ossDto = aliOssStorageService.findById(sentenceDto.getImageId());
            if (ossDto != null) {
                AppCharCharacterDetailVO.ImageVO imageVO = new AppCharCharacterDetailVO.ImageVO();
                imageVO.setImageUrl(ossDto.getFileUrl());
                vo.setExampleImage(imageVO);
            }
        }
    }
    return vo;
}
```

变更点：
- `vo.setLevel(...)` → `vo.setHskLevel(...)`
- `vo.setWordItemTranslations(...)` → `vo.setWordItemTranslation(filterByLanguage(...))`
- `vo.setExampleTranslations(...)` → `vo.setExampleTranslation(filterByLanguage(...))`

- [ ] **Step 7: 新增笔顺查询端点**

在类中新增方法（放在 `search` 方法之前或之后均可）：

```java
@ApiOperation("根据汉字查询笔顺数据（SVG路径、坐标参考线）")
@AnonymousGetMapping("/stroke/{character}")
public ResponseEntity<AppCharStrokeVO> findStrokeByCharacter(@PathVariable String character) {
    String strokeJson = charStrokeService.findByCharacter(character);
    CharStrokeVO adminVo = CharStrokeWrapper.toStrokeVO(character, strokeJson);
    AppCharStrokeVO vo = new AppCharStrokeVO();
    vo.setCharacter(adminVo.getCharacter());
    vo.setStrokes(adminVo.getStrokes());
    vo.setMedians(adminVo.getMedians());
    vo.setRadStrokes(adminVo.getRadStrokes());
    return new ResponseEntity<>(vo, HttpStatus.OK);
}
```

注意需要在类中注入 `CharStrokeService`：

```java
// 当前已注入的字段：
private final CharCharacterService charCharacterService;
private final AudioResourceService audioResourceService;
private final AliOssStorageService aliOssStorageService;

// ➕ 新增注入：
private final CharStrokeService charStrokeService;
```

同时需要在构造方法参数中加上 `CharStrokeService`（Lombok `@RequiredArgsConstructor` 会自动处理 final 字段）。

- [ ] **Step 8: 编译验证**

```bash
cd grid-bootstrap && mvn compile -DskipTests
```

预期：BUILD SUCCESS。如果编译失败，根据错误提示修复类型不匹配问题。

---

### Task 5: 提交变更

- [ ] **Step 1: 整理所有变更文件**

确认所有改动文件：

```
grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterDetailVO.java
grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterBaseVO.java
grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharStrokeVO.java
grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterDetailVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterBaseVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharStrokeVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java
git commit -m "feat: align app character API fields with admin CharCharacterVO

- Rename level -> hskLevel, remove stroke from AppCharCharacterDetailVO
- Add componentCombination field
- Add language parameter to detail endpoint for translation filtering
- Align discrimination nested class field naming with admin
- Add order field to discrimination VO
- Align word item field naming with admin
- Filter all translation lists to single item by language
- Add stroke query endpoint GET /api/app/character/stroke/{character}
- Create AppCharStrokeVO

Co-Authored-By: Claude <noreply@anthropic.com>"
```
