# 多语言翻译字段 JSON 序列化实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将汉字和词汇模块的翻译字段从 String JSON 格式改为 `List<TextTranslation>` 对象格式，在 Service 层进行 JSON 序列化/反序列化转换

**Architecture:** Entity 层保持 String 类型，DTO/VO/Request 层使用 `List<TextTranslation>`，Service 层通过 JsonUtils 进行转换

**Tech Stack:** Java 8, Spring Boot 2.7, fastjson2, MapStruct, JPA

---

## 前置准备

让我们先查看 TextTranslation 和 LanguageCodeEnum 的完整定义：

**文件检查:**
- `grid-system/src/main/java/com/naon/grid/backend/domain/common/TextTranslation.java`
- `grid-system/src/main/java/com/naon/grid/backend/enums/LanguageCodeEnum.java`

---

## 任务 1: 创建 JsonUtils 工具类

**Files:**
- Create: `grid-common/src/main/java/com/naon/grid/utils/JsonUtils.java`

- [ ] **Step 1: 创建 JsonUtils.java**

```java
package com.naon.grid.utils;

import com.alibaba.fastjson2.JSON;
import com.naon.grid.backend.domain.common.TextTranslation;

import java.util.List;

public class JsonUtils {

    private JsonUtils() {
    }

    /**
     * 将 TextTranslation 列表序列化为 JSON 字符串
     *
     * @param list 翻译列表
     * @return JSON 字符串，null 或空列表返回 null
     */
    public static String toTranslationJson(List<TextTranslation> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(list);
    }

    /**
     * 将 JSON 字符串反序列化为 TextTranslation 列表
     *
     * @param json JSON 字符串
     * @return 翻译列表，null 或空白字符串返回 null
     */
    public static List<TextTranslation> parseTranslationList(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return JSON.parseArray(json, TextTranslation.class);
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-common -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add grid-common/src/main/java/com/naon/grid/utils/JsonUtils.java
git commit -m "feat: add JsonUtils for translation JSON serialization"
```

---

## 任务 2: 修改汉字模块 DTO 类

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java`

- [ ] **Step 2.1: 修改 CharCharacterDto.java**

修改 `descTranslations` 字段类型：

```java
package com.naon.grid.backend.service.character.dto;

import com.naon.grid.base.BaseDTO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;

@Getter
@Setter
public class CharCharacterDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字")
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "说明翻译")
    private List<TextTranslation> descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationDto> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordDto> words;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 2.2: 修改 CharDiscriminationDto.java**

修改 `discrimCharTranslations` 和 `comparisonTranslations` 字段类型：

```java
package com.naon.grid.backend.service.character.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;

@Getter
@Setter
public class CharDiscriminationDto implements Serializable {

    @ApiModelProperty(value = "辨析唯一ID")
    private Integer id;

    @ApiModelProperty(value = "汉字ID")
    private Integer charId;

    @ApiModelProperty(value = "辨析汉字")
    private String discrimChar;

    @ApiModelProperty(value = "辨析拼音")
    private String discrimPinyin;

    @ApiModelProperty(value = "辨析汉字翻译")
    private List<TextTranslation> discrimCharTranslations;

    @ApiModelProperty(value = "对比翻译")
    private List<TextTranslation> comparisonTranslations;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 2.3: 修改 CharWordDto.java**

修改 `wordItemTranslations` 和 `exampleTranslations` 字段类型：

```java
package com.naon.grid.backend.service.character.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;

@Getter
@Setter
public class CharWordDto implements Serializable {

    @ApiModelProperty(value = "组词唯一ID")
    private Integer id;

    @ApiModelProperty(value = "汉字ID")
    private Integer charId;

    @ApiModelProperty(value = "组词")
    private String wordItem;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "组词翻译")
    private List<TextTranslation> wordItemTranslations;

    @ApiModelProperty(value = "例句")
    private String exampleSentence;

    @ApiModelProperty(value = "例句拼音")
    private String examplePinyin;

    @ApiModelProperty(value = "例句翻译")
    private List<TextTranslation> exampleTranslations;

    @ApiModelProperty(value = "例句图片")
    private String exampleImage;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 2.4: 编译验证**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am`
Expected: BUILD SUCCESS

- [ ] **Step 2.5: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java
git commit -m "feat: update character module DTOs to use List<TextTranslation>"
```

---

## 任务 3: 修改词汇模块 DTO 类

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExampleDto.java`

- [ ] **Step 3.1: 修改 VocabSenseDto.java**

修改 `translations` 字段类型：

```java
package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;

@Getter
@Setter
public class VocabSenseDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "自增ID, 义项ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @ApiModelProperty(value = "中文释义")
    private String chineseDef;

    @ApiModelProperty(value = "中文释义音频资源ID")
    private Long defAudioId;

    @ApiModelProperty(value = "外文翻译列表")
    private List<TextTranslation> translations;

    @ApiModelProperty(value = "近义词列表")
    private String synonyms;

    @ApiModelProperty(value = "反义词列表")
    private String antonyms;

    @ApiModelProperty(value = "正序关联词汇")
    private String relatedForward;

    @ApiModelProperty(value = "逆序关联词汇")
    private String relatedBackward;

    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder;

    @ApiModelProperty(value = "搭配列表")
    private List<VocabStructureDto> structures;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 3.2: 修改 VocabExampleDto.java**

修改 `translations` 字段类型：

```java
package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;

@Getter
@Setter
public class VocabExampleDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "例句唯一ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "所属义项ID")
    private Integer senseId;

    @ApiModelProperty(value = "所属结构搭配ID")
    private Integer structureId;

    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @ApiModelProperty(value = "例句外文翻译列表")
    private List<TextTranslation> translations;

    @ApiModelProperty(value = "例句排序权重")
    private Integer exampleOrder;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 3.3: 编译验证**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am`
Expected: BUILD SUCCESS

- [ ] **Step 3.4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExampleDto.java
git commit -m "feat: update vocabulary module DTOs to use List<TextTranslation>"
```

---

## 任务 4: 修改 MapStruct Mapper 类

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/mapstruct/VocabWordMapper.java`

- [ ] **Step 4.1: 修改 CharCharacterMapper.java**

添加自定义转换方法：

```java
package com.naon.grid.backend.service.character.mapstruct;

import com.naon.grid.backend.domain.character.CharCharacter;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.base.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;
import com.naon.grid.utils.JsonUtils;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CharCharacterMapper extends BaseMapper<CharCharacterDto, CharCharacter> {

    default String toTranslationJson(List<TextTranslation> list) {
        return JsonUtils.toTranslationJson(list);
    }

    default List<TextTranslation> toTranslationList(String json) {
        return JsonUtils.parseTranslationList(json);
    }
}
```

- [ ] **Step 4.2: 修改 VocabWordMapper.java**

添加自定义转换方法：

```java
package com.naon.grid.backend.service.vocabulary.mapstruct;

import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.base.BaseMapper;
import com.naon.grid.backend.domain.vocabulary.VocabWord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;
import com.naon.grid.utils.JsonUtils;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VocabWordMapper extends BaseMapper<VocabWordDto, VocabWord> {

    default String toTranslationJson(List<TextTranslation> list) {
        return JsonUtils.toTranslationJson(list);
    }

    default List<TextTranslation> toTranslationList(String json) {
        return JsonUtils.parseTranslationList(json);
    }
}
```

- [ ] **Step 4.3: 编译验证**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am`
Expected: BUILD SUCCESS

- [ ] **Step 4.4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/mapstruct/VocabWordMapper.java
git commit -m "feat: add translation JSON conversion methods to mappers"
```

---

## 任务 5: 修改汉字模块 Service 实现

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

- [ ] **Step 5.1: 添加 import 语句**

在文件顶部添加：

```java
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.backend.domain.common.TextTranslation;
import java.util.List;
```

- [ ] **Step 5.2: 修改 convertToDiscriminationDto 方法**

```java
private CharDiscriminationDto convertToDiscriminationDto(CharDiscrimination discrimination) {
    CharDiscriminationDto dto = new CharDiscriminationDto();
    dto.setId(discrimination.getId());
    dto.setCharId(discrimination.getCharId());
    dto.setDiscrimChar(discrimination.getDiscrimChar());
    dto.setDiscrimPinyin(discrimination.getDiscrimPinyin());
    dto.setDiscrimCharTranslations(JsonUtils.parseTranslationList(discrimination.getDiscrimCharTranslations()));
    dto.setComparisonTranslations(JsonUtils.parseTranslationList(discrimination.getComparisonTranslations()));
    dto.setCreateTime(discrimination.getCreateTime());
    dto.setUpdateTime(discrimination.getUpdateTime());
    dto.setStatus(discrimination.getStatus());
    return dto;
}
```

- [ ] **Step 5.3: 修改 convertToWordDto 方法**

```java
private CharWordDto convertToWordDto(CharWord word) {
    CharWordDto dto = new CharWordDto();
    dto.setId(word.getId());
    dto.setCharId(word.getCharId());
    dto.setWordItem(word.getWordItem());
    dto.setLevel(word.getLevel());
    dto.setPinyin(word.getPinyin());
    dto.setPartOfSpeech(word.getPartOfSpeech());
    dto.setWordItemTranslations(JsonUtils.parseTranslationList(word.getWordItemTranslations()));
    dto.setExampleSentence(word.getExampleSentence());
    dto.setExamplePinyin(word.getExamplePinyin());
    dto.setExampleTranslations(JsonUtils.parseTranslationList(word.getExampleTranslations()));
    dto.setExampleImage(word.getExampleImage());
    dto.setCreateTime(word.getCreateTime());
    dto.setUpdateTime(word.getUpdateTime());
    dto.setStatus(word.getStatus());
    return dto;
}
```

- [ ] **Step 5.4: 修改 updateDiscrimination 方法**

```java
private void updateDiscrimination(CharDiscrimination entity, CharDiscriminationDto dto) {
    entity.setDiscrimChar(dto.getDiscrimChar());
    entity.setDiscrimPinyin(dto.getDiscrimPinyin());
    entity.setDiscrimCharTranslations(JsonUtils.toTranslationJson(dto.getDiscrimCharTranslations()));
    entity.setComparisonTranslations(JsonUtils.toTranslationJson(dto.getComparisonTranslations()));
}
```

- [ ] **Step 5.5: 修改 updateWord 方法**

```java
private void updateWord(CharWord entity, CharWordDto dto) {
    entity.setWordItem(dto.getWordItem());
    entity.setLevel(dto.getLevel());
    entity.setPinyin(dto.getPinyin());
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setWordItemTranslations(JsonUtils.toTranslationJson(dto.getWordItemTranslations()));
    entity.setExampleSentence(dto.getExampleSentence());
    entity.setExamplePinyin(dto.getExamplePinyin());
    entity.setExampleTranslations(JsonUtils.toTranslationJson(dto.getExampleTranslations()));
    entity.setExampleImage(dto.getExampleImage());
}
```

- [ ] **Step 5.6: 修改 convertToDiscriminationEntity 方法**

```java
private CharDiscrimination convertToDiscriminationEntity(CharDiscriminationDto dto, Integer charId) {
    CharDiscrimination entity = new CharDiscrimination();
    entity.setCharId(charId);
    entity.setDiscrimChar(dto.getDiscrimChar());
    entity.setDiscrimPinyin(dto.getDiscrimPinyin());
    entity.setDiscrimCharTranslations(JsonUtils.toTranslationJson(dto.getDiscrimCharTranslations()));
    entity.setComparisonTranslations(JsonUtils.toTranslationJson(dto.getComparisonTranslations()));
    entity.setStatus(StatusEnum.ENABLED.getCode());
    return entity;
}
```

- [ ] **Step 5.7: 修改 convertToWordEntity 方法**

```java
private CharWord convertToWordEntity(CharWordDto dto, Integer charId) {
    CharWord entity = new CharWord();
    entity.setCharId(charId);
    entity.setWordItem(dto.getWordItem());
    entity.setLevel(dto.getLevel());
    entity.setPinyin(dto.getPinyin());
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setWordItemTranslations(JsonUtils.toTranslationJson(dto.getWordItemTranslations()));
    entity.setExampleSentence(dto.getExampleSentence());
    entity.setExamplePinyin(dto.getExamplePinyin());
    entity.setExampleTranslations(JsonUtils.toTranslationJson(dto.getExampleTranslations()));
    entity.setExampleImage(dto.getExampleImage());
    entity.setStatus(StatusEnum.ENABLED.getCode());
    return entity;
}
```

- [ ] **Step 5.8: 修改 update 方法中的 CharCharacter 转换**

找到 `update` 方法中设置 `descTranslations` 的地方：

```java
charCharacter.setDescTranslations(JsonUtils.toTranslationJson(resources.getDescTranslations()));
```

- [ ] **Step 5.9: 修改 findById 和 queryAll 中的 CharCharacterDto 转换**

在 findById 方法中，`charCharacterMapper.toDto` 会自动处理转换，无需额外修改。

- [ ] **Step 5.10: 编译验证**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am`
Expected: BUILD SUCCESS

- [ ] **Step 5.11: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java
git commit -m "feat: update CharCharacterServiceImpl to use JsonUtils for translation conversion"
```

---

## 任务 6: 修改词汇模块 Service 实现

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

- [ ] **Step 6.1: 添加 import 语句**

在文件顶部添加：

```java
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.backend.domain.common.TextTranslation;
import java.util.List;
```

- [ ] **Step 6.2: 修改 convertToSenseDto 方法**

```java
private VocabSenseDto convertToSenseDto(VocabSense sense) {
    VocabSenseDto dto = new VocabSenseDto();
    dto.setId(sense.getId());
    dto.setWordId(sense.getWordId());
    dto.setPartOfSpeech(sense.getPartOfSpeech());
    dto.setChineseDef(sense.getChineseDef());
    dto.setDefAudioId(sense.getDefAudioId());
    dto.setTranslations(JsonUtils.parseTranslationList(sense.getTranslations()));
    dto.setSynonyms(sense.getSynonyms());
    dto.setAntonyms(sense.getAntonyms());
    dto.setRelatedForward(sense.getRelatedForward());
    dto.setRelatedBackward(sense.getRelatedBackward());
    dto.setSenseOrder(sense.getSenseOrder());
    dto.setCreateTime(sense.getCreateTime());
    dto.setUpdateTime(sense.getUpdateTime());
    dto.setStatus(sense.getStatus());

    List<VocabStructureDto> structureDtos = new ArrayList<>();
    List<VocabStructure> structures = vocabStructureRepository.findBySenseIdAndStatus(sense.getId(), StatusEnum.ENABLED.getCode());
    for (VocabStructure structure : structures) {
        VocabStructureDto structureDto = convertToStructureDto(structure);
        structureDtos.add(structureDto);
    }
    dto.setStructures(structureDtos);

    return dto;
}
```

- [ ] **Step 6.3: 修改 convertToExampleDto 方法**

```java
private VocabExampleDto convertToExampleDto(VocabExample example) {
    VocabExampleDto dto = new VocabExampleDto();
    dto.setId(example.getId());
    dto.setWordId(example.getWordId());
    dto.setSenseId(example.getSenseId());
    dto.setStructureId(example.getStructureId());
    dto.setSentence(example.getSentence());
    dto.setAudioId(example.getAudioId());
    dto.setPinyin(example.getPinyin());
    dto.setTranslations(JsonUtils.parseTranslationList(example.getTranslations()));
    dto.setExampleOrder(example.getExampleOrder());
    dto.setCreateTime(example.getCreateTime());
    dto.setUpdateTime(example.getUpdateTime());
    dto.setStatus(example.getStatus());
    return dto;
}
```

- [ ] **Step 6.4: 修改 updateSense 方法**

```java
private void updateSense(VocabSense entity, VocabSenseDto dto) {
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setChineseDef(dto.getChineseDef());
    entity.setDefAudioId(dto.getDefAudioId());
    entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
    entity.setSynonyms(dto.getSynonyms());
    entity.setAntonyms(dto.getAntonyms());
    entity.setRelatedForward(dto.getRelatedForward());
    entity.setRelatedBackward(dto.getRelatedBackward());
    entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
}
```

- [ ] **Step 6.5: 修改 updateExample 方法**

```java
private void updateExample(VocabExample entity, VocabExampleDto dto) {
    entity.setSentence(dto.getSentence());
    entity.setAudioId(dto.getAudioId());
    entity.setPinyin(dto.getPinyin());
    entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
    entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
}
```

- [ ] **Step 6.6: 修改 convertToSenseEntity 方法**

```java
private VocabSense convertToSenseEntity(VocabSenseDto dto, Integer wordId) {
    VocabSense entity = new VocabSense();
    entity.setWordId(wordId);
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setChineseDef(dto.getChineseDef());
    entity.setDefAudioId(dto.getDefAudioId());
    entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
    entity.setSynonyms(dto.getSynonyms());
    entity.setAntonyms(dto.getAntonyms());
    entity.setRelatedForward(dto.getRelatedForward());
    entity.setRelatedBackward(dto.getRelatedBackward());
    entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
    entity.setStatus(StatusEnum.ENABLED.getCode());
    return entity;
}
```

- [ ] **Step 6.7: 修改 convertToExampleEntity 方法**

```java
private VocabExample convertToExampleEntity(VocabExampleDto dto, Integer wordId, Integer senseId, Integer structureId) {
    VocabExample entity = new VocabExample();
    entity.setWordId(wordId);
    entity.setSenseId(senseId);
    entity.setStructureId(structureId);
    entity.setSentence(dto.getSentence());
    entity.setAudioId(dto.getAudioId());
    entity.setPinyin(dto.getPinyin());
    entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
    entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
    entity.setStatus(StatusEnum.ENABLED.getCode());
    return entity;
}
```

- [ ] **Step 6.8: 编译验证**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am`
Expected: BUILD SUCCESS

- [ ] **Step 6.9: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "feat: update VocabWordServiceImpl to use JsonUtils for translation conversion"
```

---

## 任务 7: 修改汉字模块 Request 类

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`

- [ ] **Step 7.1: 修改 CharCharacterCreateRequest.java**

修改翻译字段类型：

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;

@Getter
@Setter
public class CharCharacterCreateRequest implements Serializable {

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @NotBlank
    @ApiModelProperty(value = "汉字", required = true)
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @NotBlank
    @ApiModelProperty(value = "拼音", required = true)
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "说明翻译")
    private List<TextTranslation> descTranslations;

    @Valid
    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationRequest> discriminations;

    @Valid
    @ApiModelProperty(value = "组词列表")
    private List<CharWordRequest> words;

    @Getter
    @Setter
    public static class CharDiscriminationRequest implements Serializable {
        @ApiModelProperty(value = "辨析ID（新增时不传，更新时传）")
        private Integer id;

        @NotBlank
        @ApiModelProperty(value = "辨析汉字", required = true)
        private String discrimChar;

        @ApiModelProperty(value = "辨析拼音")
        private String discrimPinyin;

        @ApiModelProperty(value = "辨析汉字翻译")
        private List<TextTranslation> discrimCharTranslations;

        @ApiModelProperty(value = "对比翻译")
        private List<TextTranslation> comparisonTranslations;
    }

    @Getter
    @Setter
    public static class CharWordRequest implements Serializable {
        @ApiModelProperty(value = "组词ID（新增时不传，更新时传）")
        private Integer id;

        @NotBlank
        @ApiModelProperty(value = "组词", required = true)
        private String wordItem;

        @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
        private String level;

        @ApiModelProperty(value = "拼音")
        private String pinyin;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "组词翻译")
        private List<TextTranslation> wordItemTranslations;

        @ApiModelProperty(value = "例句")
        private String exampleSentence;

        @ApiModelProperty(value = "例句拼音")
        private String examplePinyin;

        @ApiModelProperty(value = "例句翻译")
        private List<TextTranslation> exampleTranslations;

        @ApiModelProperty(value = "例句图片")
        private String exampleImage;
    }
}
```

- [ ] **Step 7.2: 编译验证**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am`
Expected: BUILD SUCCESS

- [ ] **Step 7.3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java
git commit -m "feat: update CharCharacterCreateRequest to use List<TextTranslation>"
```

---

## 任务 8: 修改词汇模块 Request 类

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java`

- [ ] **Step 8.1: 修改 VocabWordCreateRequest.java**

修改翻译字段类型：

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;

@Getter
@Setter
public class VocabWordCreateRequest implements Serializable {

    @NotBlank
    @ApiModelProperty(value = "词汇", required = true)
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @NotBlank
    @ApiModelProperty(value = "标准拼音（含声调）", required = true)
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String hskLevel;

    @Valid
    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseRequest> senses;

    @Valid
    @ApiModelProperty(value = "练习题列表")
    private List<VocabExerciseRequest> exercises;

    @Getter
    @Setter
    public static class VocabSenseRequest implements Serializable {
        @ApiModelProperty(value = "义项ID（新增时不传，更新时传）")
        private Integer id;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "中文释义")
        private String chineseDef;

        @ApiModelProperty(value = "中文释义音频资源ID")
        private Long defAudioId;

        @ApiModelProperty(value = "外文翻译列表")
        private List<TextTranslation> translations;

        @ApiModelProperty(value = "近义词列表")
        private String synonyms;

        @ApiModelProperty(value = "反义词列表")
        private String antonyms;

        @ApiModelProperty(value = "正序关联词汇")
        private String relatedForward;

        @ApiModelProperty(value = "逆序关联词汇")
        private String relatedBackward;

        @ApiModelProperty(value = "义项排序权重，值大的排前面", required = true)
        private Integer senseOrder;

        @Valid
        @ApiModelProperty(value = "搭配列表")
        private List<VocabStructureRequest> structures;
    }

    @Getter
    @Setter
    public static class VocabStructureRequest implements Serializable {
        @ApiModelProperty(value = "结构搭配ID（新增时不传，更新时传）")
        private Integer id;

        @NotBlank
        @ApiModelProperty(value = "结构搭配文案", required = true)
        private String pattern;

        @ApiModelProperty(value = "搭配排序权重，值大的排前面", required = true)
        private Integer structureOrder;

        @Valid
        @ApiModelProperty(value = "例句列表")
        private List<VocabExampleRequest> examples;
    }

    @Getter
    @Setter
    public static class VocabExerciseRequest implements Serializable {
        @ApiModelProperty(value = "练习题目ID（新增时不传，更新时传）")
        private Integer id;

        @NotBlank
        @ApiModelProperty(value = "题目类型", required = true)
        private String questionType;

        @NotBlank
        @ApiModelProperty(value = "练习题干描述", required = true)
        private String questionText;

        @ApiModelProperty(value = "选项列表")
        private String options;

        @ApiModelProperty(value = "答案列表")
        private String answers;

        @ApiModelProperty(value = "练习题目排序权重，值大的排前面", required = true)
        private Integer exerciseOrder;
    }

    @Getter
    @Setter
    public static class VocabExampleRequest implements Serializable {
        @ApiModelProperty(value = "例句ID（新增时不传，更新时传）")
        private Integer id;

        @NotBlank
        @ApiModelProperty(value = "例句中文文案", required = true)
        private String sentence;

        @ApiModelProperty(value = "例句音频资源ID")
        private Long audioId;

        @ApiModelProperty(value = "例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "例句外文翻译列表")
        private List<TextTranslation> translations;

        @ApiModelProperty(value = "例句排序权重，值大的排前面", required = true)
        private Integer exampleOrder;
    }
}
```

- [ ] **Step 8.2: 编译验证**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am`
Expected: BUILD SUCCESS

- [ ] **Step 8.3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java
git commit -m "feat: update VocabWordCreateRequest to use List<TextTranslation>"
```

---

## 任务 9: 修改汉字模块 VO 类

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`

- [ ] **Step 9.1: 修改 CharCharacterVO.java**

修改翻译字段类型：

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;

@Getter
@Setter
public class CharCharacterVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字", required = true)
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音", required = true)
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔画")
    private String stroke;

    @ApiModelProperty(value = "汉字说明")
    private String charDesc;

    @ApiModelProperty(value = "说明翻译")
    private List<TextTranslation> descTranslations;

    @ApiModelProperty(value = "辨析列表")
    private List<CharDiscriminationVO> discriminations;

    @ApiModelProperty(value = "组词列表")
    private List<CharWordVO> words;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Getter
    @Setter
    public static class CharDiscriminationVO implements Serializable {
        @ApiModelProperty(value = "辨析唯一ID")
        private Integer id;

        @ApiModelProperty(value = "汉字ID", required = true)
        private Integer charId;

        @ApiModelProperty(value = "辨析汉字", required = true)
        private String discrimChar;

        @ApiModelProperty(value = "辨析拼音")
        private String discrimPinyin;

        @ApiModelProperty(value = "辨析汉字翻译")
        private List<TextTranslation> discrimCharTranslations;

        @ApiModelProperty(value = "对比翻译")
        private List<TextTranslation> comparisonTranslations;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class CharWordVO implements Serializable {
        @ApiModelProperty(value = "组词唯一ID")
        private Integer id;

        @ApiModelProperty(value = "汉字ID", required = true)
        private Integer charId;

        @ApiModelProperty(value = "组词", required = true)
        private String wordItem;

        @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
        private String level;

        @ApiModelProperty(value = "拼音")
        private String pinyin;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "组词翻译")
        private List<TextTranslation> wordItemTranslations;

        @ApiModelProperty(value = "例句")
        private String exampleSentence;

        @ApiModelProperty(value = "例句拼音")
        private String examplePinyin;

        @ApiModelProperty(value = "例句翻译")
        private List<TextTranslation> exampleTranslations;

        @ApiModelProperty(value = "例句图片")
        private String exampleImage;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }
}
```

- [ ] **Step 9.2: 修改 CharCharacterBaseVO.java**

修改 `descTranslations` 字段类型：

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;

@Getter
@Setter
public class CharCharacterBaseVO implements Serializable {

    @ApiModelProperty(value = "汉字唯一ID")
    private Integer id;

    @ApiModelProperty(value = "Excel中的序号")
    private Integer sequenceNo;

    @ApiModelProperty(value = "汉字", required = true)
    private String character;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String level;

    @ApiModelProperty(value = "拼音", required = true)
    private String pinyin;

    @ApiModelProperty(value = "读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "繁体字")
    private String traditional;

    @ApiModelProperty(value = "部首")
    private String radical;

    @ApiModelProperty(value = "笔顺")
    private String stroke;

    @ApiModelProperty(value = "部件组合中文说明")
    private String charDesc;

    @ApiModelProperty(value = "汉字说明的多语种翻译")
    private List<TextTranslation> descTranslations;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
```

- [ ] **Step 9.3: 编译验证**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am`
Expected: BUILD SUCCESS

- [ ] **Step 9.4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java
git commit -m "feat: update character module VOs to use List<TextTranslation>"
```

---

## 任务 10: 修改词汇模块 VO 类

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java`

- [ ] **Step 10.1: 修改 VocabWordVO.java**

修改翻译字段类型：

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import com.naon.grid.backend.domain.common.TextTranslation;

@Getter
@Setter
public class VocabWordVO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇", required = true)
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "标准拼音（含声调）", required = true)
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级，值为数字字符串\"1\"-\"9\"")
    private String hskLevel;

    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseVO> senses;

    @ApiModelProperty(value = "练习题列表")
    private List<VocabExerciseVO> exercises;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "修改人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Getter
    @Setter
    public static class VocabSenseVO implements Serializable {
        @ApiModelProperty(value = "自增ID, 义项ID")
        private Integer id;

        @ApiModelProperty(value = "所属词汇ID", required = true)
        private Integer wordId;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "中文释义")
        private String chineseDef;

        @ApiModelProperty(value = "中文释义音频资源ID")
        private Long defAudioId;

        @ApiModelProperty(value = "外文翻译列表")
        private List<TextTranslation> translations;

        @ApiModelProperty(value = "近义词列表")
        private String synonyms;

        @ApiModelProperty(value = "反义词列表")
        private String antonyms;

        @ApiModelProperty(value = "正序关联词汇")
        private String relatedForward;

        @ApiModelProperty(value = "逆序关联词汇")
        private String relatedBackward;

        @ApiModelProperty(value = "义项排序权重，值大的排前面", required = true)
        private Integer senseOrder;

        @ApiModelProperty(value = "搭配列表")
        private List<VocabStructureVO> structures;

        @ApiModelProperty(value = "创建人")
        private String createBy;

        @ApiModelProperty(value = "修改人")
        private String updateBy;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class VocabStructureVO implements Serializable {
        @ApiModelProperty(value = "自增ID, 结构搭配ID")
        private Integer id;

        @ApiModelProperty(value = "所属词汇ID", required = true)
        private Integer wordId;

        @ApiModelProperty(value = "所属义项ID", required = true)
        private Integer senseId;

        @ApiModelProperty(value = "结构搭配文案", required = true)
        private String pattern;

        @ApiModelProperty(value = "搭配排序权重，值大的排前面", required = true)
        private Integer structureOrder;

        @ApiModelProperty(value = "例句列表")
        private List<VocabExampleVO> examples;

        @ApiModelProperty(value = "创建人")
        private String createBy;

        @ApiModelProperty(value = "修改人")
        private String updateBy;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class VocabExerciseVO implements Serializable {
        @ApiModelProperty(value = "练习题目唯一ID")
        private Integer id;

        @ApiModelProperty(value = "所属词汇ID", required = true)
        private Integer wordId;

        @ApiModelProperty(value = "题目类型", required = true)
        private String questionType;

        @ApiModelProperty(value = "练习题干描述", required = true)
        private String questionText;

        @ApiModelProperty(value = "选项列表")
        private String options;

        @ApiModelProperty(value = "答案列表")
        private String answers;

        @ApiModelProperty(value = "练习题目排序权重，值大的排前面", required = true)
        private Integer exerciseOrder;

        @ApiModelProperty(value = "创建人")
        private String createBy;

        @ApiModelProperty(value = "修改人")
        private String updateBy;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class VocabExampleVO implements Serializable {
        @ApiModelProperty(value = "例句唯一ID")
        private Integer id;

        @ApiModelProperty(value = "所属词汇ID", required = true)
        private Integer wordId;

        @ApiModelProperty(value = "所属义项ID", required = true)
        private Integer senseId;

        @ApiModelProperty(value = "所属结构搭配ID", required = true)
        private Integer structureId;

        @ApiModelProperty(value = "例句中文文案", required = true)
        private String sentence;

        @ApiModelProperty(value = "例句音频资源ID")
        private Long audioId;

        @ApiModelProperty(value = "例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "例句外文翻译列表")
        private List<TextTranslation> translations;

        @ApiModelProperty(value = "例句排序权重，值大的排前面", required = true)
        private Integer exampleOrder;

        @ApiModelProperty(value = "创建人")
        private String createBy;

        @ApiModelProperty(value = "修改人")
        private String updateBy;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }
}
```

- [ ] **Step 10.2: 修改 VocabWordBaseVO.java**

VocabWordBaseVO 不包含翻译字段，无需修改。

- [ ] **Step 10.3: 编译验证**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn compile -pl grid-system -am`
Expected: BUILD SUCCESS

- [ ] **Step 10.4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java
git commit -m "feat: update vocabulary module VOs to use List<TextTranslation>"
```

---

## 任务 11: 编译和完整验证

**Files:**
- 无新增文件，验证所有修改

- [ ] **Step 11.1: 完整编译项目**

Run: `cd /Users/nano/Desktop/nano-gemini && mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 11.2: 验证 git 状态**

Run: `cd /Users/nano/Desktop/nano-gemini && git status`
Expected: 所有修改已提交

- [ ] **Step 11.3: 提交完整验证（如果有需要）**

如果发现任何问题需要修复：

```bash
# 修复问题后提交
git commit -am "fix: resolve compilation issues in translation JSON implementation"
```

---

## 自我检查

在提交最终实现前，请检查：

- [ ] **Spec Coverage Check:**
  - JsonUtils 已创建 ✅
  - DTO 层已修改 ✅
  - Mapper 层已修改 ✅
  - Service 层已修改 ✅
  - Request 层已修改 ✅
  - VO 层已修改 ✅
  - Entity 层保持不变 ✅
  - Repository 层保持不变 ✅

- [ ] **Placeholder Check:**
  - 无 TODO 或占位符 ✅
  - 所有方法都有完整实现 ✅

- [ ] **Type Consistency Check:**
  - 所有翻译字段一致使用 `List<TextTranslation>` ✅
  - JsonUtils 方法在 Service 中正确调用 ✅

---

## 执行方式选择

计划已完整保存到 `docs/superpowers/plans/2026-05-27-translation-json-serialization.md`。

**两种执行方式：**

1. **Subagent-Driven (推荐):** 使用 `superpowers:subagent-driven-development` 技能，每个任务一个子代理，分步执行和验证
2. **Inline Execution:** 使用 `superpowers:executing-plans` 技能，在当前会话中批量执行

**请选择执行方式：** (回复 "1" 或 "2")
