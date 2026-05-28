# 词汇模块列表字段 JSON 序列化实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将词汇模块的列表字段从 String JSON 格式改为对象类型，在 Service 层进行 JSON 序列化/反序列化转换

**Architecture:** Entity 层保持 String 类型，DTO/Request/VO 层改为对象类型，在 Service 层和 JsonUtils 中进行转换

**Tech Stack:** Spring Boot, JPA, Fastjson2, MapStruct

---

## 文件结构总览

| 文件 | 操作 | 说明 |
|------|------|------|
| `JsonUtils.java` | 修改 | 新增字符串列表和 ExerciseOption 列表的 JSON 转换方法 |
| `ExerciseOption.java` | 新建 | 选项对象类 |
| `VocabSenseDto.java` | 修改 | 四个字段改为 `List<String>` |
| `VocabExerciseDto.java` | 修改 | options 改为 `List<ExerciseOption>`，answers 改为 `List<String>` |
| `VocabWordVO.java` | 修改 | 内部类对应字段类型修改 |
| `VocabWordCreateRequest.java` | 修改 | 内部类对应字段类型修改 |
| `VocabWordController.java` | 修改 | 转换方法同步修改 |
| `VocabWordServiceImpl.java` | 修改 | 添加 JSON 转换逻辑 |

---

## Task 1: 扩展 JsonUtils - 新增字符串列表和 ExerciseOption 列表转换方法

**Files:**
- Modify: `grid-common/src/main/java/com/naon/grid/utils/JsonUtils.java`

- [ ] **Step 1: 修改 JsonUtils.java，新增方法**

```java
package com.naon.grid.utils;

import com.alibaba.fastjson2.JSON;
import com.naon.grid.backend.domain.vocabulary.ExerciseOption;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        // 过滤掉空对象
        List<TextTranslation> filteredList = list.stream()
                .filter(item -> !isEmptyTranslation(item))
                .collect(Collectors.toList());
        if (filteredList.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(filteredList);
    }

    /**
     * 将 JSON 字符串反序列化为 TextTranslation 列表
     *
     * @param json JSON 字符串
     * @return 翻译列表，null 或空白字符串返回空列表，会过滤掉空对象
     */
    public static List<TextTranslation> parseTranslationList(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        List<TextTranslation> list = JSON.parseArray(json, TextTranslation.class);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // 过滤掉空对象（language和translation都为null或空字符串的）
        return list.stream()
                .filter(item -> !isEmptyTranslation(item))
                .collect(Collectors.toList());
    }

    /**
     * 判断是否为空的翻译对象
     */
    private static boolean isEmptyTranslation(TextTranslation item) {
        if (item == null) {
            return true;
        }
        return StringUtils.isBlank(item.getLanguage()) && StringUtils.isBlank(item.getTranslation());
    }

    /**
     * 将字符串列表序列化为 JSON 字符串
     *
     * @param list 字符串列表
     * @return JSON 字符串，null 或空列表返回 null
     */
    public static String toStringListJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        // 过滤掉 null 和空白字符串
        List<String> filteredList = list.stream()
                .filter(item -> !StringUtils.isBlank(item))
                .collect(Collectors.toList());
        if (filteredList.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(filteredList);
    }

    /**
     * 将 JSON 字符串反序列化为字符串列表
     *
     * @param json JSON 字符串
     * @return 字符串列表，null 或空白字符串返回空列表，会过滤掉空白字符串
     */
    public static List<String> parseStringList(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        List<String> list = JSON.parseArray(json, String.class);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // 过滤掉 null 和空白字符串
        return list.stream()
                .filter(item -> !StringUtils.isBlank(item))
                .collect(Collectors.toList());
    }

    /**
     * 将 ExerciseOption 列表序列化为 JSON 字符串
     *
     * @param list 选项列表
     * @return JSON 字符串，null 或空列表返回 null
     */
    public static String toExerciseOptionListJson(List<ExerciseOption> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        // 过滤掉空对象
        List<ExerciseOption> filteredList = list.stream()
                .filter(item -> !isEmptyExerciseOption(item))
                .collect(Collectors.toList());
        if (filteredList.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(filteredList);
    }

    /**
     * 将 JSON 字符串反序列化为 ExerciseOption 列表
     *
     * @param json JSON 字符串
     * @return 选项列表，null 或空白字符串返回空列表，会过滤掉空对象
     */
    public static List<ExerciseOption> parseExerciseOptionList(String json) {
        if (StringUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        List<ExerciseOption> list = JSON.parseArray(json, ExerciseOption.class);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        // 过滤掉空对象
        return list.stream()
                .filter(item -> !isEmptyExerciseOption(item))
                .collect(Collectors.toList());
    }

    /**
     * 判断是否为空的 ExerciseOption 对象
     */
    private static boolean isEmptyExerciseOption(ExerciseOption item) {
        if (item == null) {
            return true;
        }
        return StringUtils.isBlank(item.getOption()) && StringUtils.isBlank(item.getText());
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-common -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-common/src/main/java/com/naon/grid/utils/JsonUtils.java
git commit -m "feat: add string list and ExerciseOption list json conversion to JsonUtils"
```

---

## Task 2: 创建 ExerciseOption 类

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/ExerciseOption.java`

- [ ] **Step 1: 创建 ExerciseOption.java**

```java
package com.naon.grid.backend.domain.vocabulary;

import lombok.Getter;
import lombok.Setter;

/**
 * 练习题选项
 */
@Getter
@Setter
public class ExerciseOption {
    /**
     * 选项标识，如 "A", "B", "C", "D"
     */
    private String option;

    /**
     * 选项文案
     */
    private String text;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/ExerciseOption.java
git commit -m "feat: add ExerciseOption domain class"
```

---

## Task 3: 修改 VocabSenseDto - 字段类型改为 List<String>

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java`

- [ ] **Step 1: 修改 VocabSenseDto.java**

```java
package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import java.io.Serializable;
import java.util.List;

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
    private List<String> synonyms;

    @ApiModelProperty(value = "反义词列表")
    private List<String> antonyms;

    @ApiModelProperty(value = "正序关联词汇")
    private List<String> relatedForward;

    @ApiModelProperty(value = "逆序关联词汇")
    private List<String> relatedBackward;

    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder;

    @ApiModelProperty(value = "搭配列表")
    private List<VocabStructureDto> structures;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system -am -DskipTests`
Expected: BUILD SUCCESS (可能会有报错，因为 VocabWordServiceImpl 还没改，没关系，继续)

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java
git commit -m "feat: update VocabSenseDto fields to List<String>"
```

---

## Task 4: 修改 VocabExerciseDto - 字段类型改为对象列表

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExerciseDto.java`

- [ ] **Step 1: 修改 VocabExerciseDto.java**

```java
package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.backend.domain.vocabulary.ExerciseOption;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabExerciseDto extends BaseDTO implements Serializable {

    @ApiModelProperty(value = "练习题目唯一ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "题目类型")
    private String questionType;

    @ApiModelProperty(value = "练习题干描述")
    private String questionText;

    @ApiModelProperty(value = "选项列表")
    private List<ExerciseOption> options;

    @ApiModelProperty(value = "答案列表")
    private List<String> answers;

    @ApiModelProperty(value = "练习题目排序权重")
    private Integer exerciseOrder;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system -am -DskipTests`
Expected: 可能会有报错，没关系，继续

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExerciseDto.java
git commit -m "feat: update VocabExerciseDto fields to object list types"
```

---

## Task 5: 修改 VocabWordVO - 内部类字段类型

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java`

- [ ] **Step 1: 修改 VocabWordVO.java**

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.backend.domain.vocabulary.ExerciseOption;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

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
        private List<TextTranslationVO> translations;

        @ApiModelProperty(value = "近义词列表")
        private List<String> synonyms;

        @ApiModelProperty(value = "反义词列表")
        private List<String> antonyms;

        @ApiModelProperty(value = "正序关联词汇")
        private List<String> relatedForward;

        @ApiModelProperty(value = "逆序关联词汇")
        private List<String> relatedBackward;

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
        private List<ExerciseOption> options;

        @ApiModelProperty(value = "答案列表")
        private List<String> answers;

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
        private List<TextTranslationVO> translations;

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

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system -am -DskipTests`
Expected: 可能会有报错，没关系，继续

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java
git commit -m "feat: update VocabWordVO inner class fields to object list types"
```

---

## Task 6: 修改 VocabWordCreateRequest - 内部类字段类型

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java`

- [ ] **Step 1: 修改 VocabWordCreateRequest.java**

```java
package com.naon.grid.backend.rest.request;

import com.naon.grid.backend.domain.vocabulary.ExerciseOption;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

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
        private List<TextTranslationRequest> translations;

        @ApiModelProperty(value = "近义词列表")
        private List<String> synonyms;

        @ApiModelProperty(value = "反义词列表")
        private List<String> antonyms;

        @ApiModelProperty(value = "正序关联词汇")
        private List<String> relatedForward;

        @ApiModelProperty(value = "逆序关联词汇")
        private List<String> relatedBackward;

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
        private List<ExerciseOption> options;

        @ApiModelProperty(value = "答案列表")
        private List<String> answers;

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
        private List<TextTranslationRequest> translations;

        @ApiModelProperty(value = "例句排序权重，值大的排前面", required = true)
        private Integer exampleOrder;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl grid-system -am -DskipTests`
Expected: 可能会有报错，没关系，继续

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java
git commit -m "feat: update VocabWordCreateRequest inner class fields to object list types"
```

---

## Task 7: 修改 VocabWordController - 转换方法同步修改

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

- [ ] **Step 1: 修改 VocabWordController.java - 检查是否需要修改转换方法**

实际上，Controller 中的 toXxx 方法只是简单的字段赋值，不需要特殊转换逻辑，因为 Request/DTO/VO 的类型已经一致了。

确认文件内容不需要改动（只是字段赋值），跳过修改。

- [ ] **Step 2: Commit（如有改动）**

如果没有改动，跳过此步骤。

---

## Task 8: 修改 VocabWordServiceImpl - 添加 JSON 转换逻辑

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

- [ ] **Step 1: 修改 convertToSenseDto 方法**

修改位置：`convertToSenseDto` 方法（约第 377 行）

```java
private VocabSenseDto convertToSenseDto(VocabSense sense) {
    VocabSenseDto dto = new VocabSenseDto();
    dto.setId(sense.getId());
    dto.setWordId(sense.getWordId());
    dto.setPartOfSpeech(sense.getPartOfSpeech());
    dto.setChineseDef(sense.getChineseDef());
    dto.setDefAudioId(sense.getDefAudioId());
    dto.setTranslations(JsonUtils.parseTranslationList(sense.getTranslations()));
    dto.setSynonyms(JsonUtils.parseStringList(sense.getSynonyms()));
    dto.setAntonyms(JsonUtils.parseStringList(sense.getAntonyms()));
    dto.setRelatedForward(JsonUtils.parseStringList(sense.getRelatedForward()));
    dto.setRelatedBackward(JsonUtils.parseStringList(sense.getRelatedBackward()));
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

- [ ] **Step 2: 修改 convertToExerciseDto 方法**

修改位置：`convertToExerciseDto` 方法（约第 444 行）

```java
private VocabExerciseDto convertToExerciseDto(VocabExercise exercise) {
    VocabExerciseDto dto = new VocabExerciseDto();
    dto.setId(exercise.getId());
    dto.setWordId(exercise.getWordId());
    dto.setQuestionType(exercise.getQuestionType());
    dto.setQuestionText(exercise.getQuestionText());
    dto.setOptions(JsonUtils.parseExerciseOptionList(exercise.getOptions()));
    dto.setAnswers(JsonUtils.parseStringList(exercise.getAnswers()));
    dto.setExerciseOrder(exercise.getExerciseOrder());
    dto.setCreateTime(exercise.getCreateTime());
    dto.setUpdateTime(exercise.getUpdateTime());
    dto.setStatus(exercise.getStatus());
    return dto;
}
```

- [ ] **Step 3: 修改 updateSense 方法**

修改位置：`updateSense` 方法（约第 459 行）

```java
private void updateSense(VocabSense entity, VocabSenseDto dto) {
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setChineseDef(dto.getChineseDef());
    entity.setDefAudioId(dto.getDefAudioId());
    entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
    entity.setSynonyms(JsonUtils.toStringListJson(dto.getSynonyms()));
    entity.setAntonyms(JsonUtils.toStringListJson(dto.getAntonyms()));
    entity.setRelatedForward(JsonUtils.toStringListJson(dto.getRelatedForward()));
    entity.setRelatedBackward(JsonUtils.toStringListJson(dto.getRelatedBackward()));
    entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
}
```

- [ ] **Step 4: 修改 updateExercise 方法**

修改位置：`updateExercise` 方法（约第 484 行）

```java
private void updateExercise(VocabExercise entity, VocabExerciseDto dto) {
    entity.setQuestionType(dto.getQuestionType());
    entity.setQuestionText(dto.getQuestionText());
    entity.setOptions(JsonUtils.toExerciseOptionListJson(dto.getOptions()));
    entity.setAnswers(JsonUtils.toStringListJson(dto.getAnswers()));
    entity.setExerciseOrder(dto.getExerciseOrder() != null ? dto.getExerciseOrder() : 0);
}
```

- [ ] **Step 5: 修改 convertToSenseEntity 方法**

修改位置：`convertToSenseEntity` 方法（约第 492 行）

```java
private VocabSense convertToSenseEntity(VocabSenseDto dto, Integer wordId) {
    VocabSense entity = new VocabSense();
    entity.setWordId(wordId);
    entity.setPartOfSpeech(dto.getPartOfSpeech());
    entity.setChineseDef(dto.getChineseDef());
    entity.setDefAudioId(dto.getDefAudioId());
    entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
    entity.setSynonyms(JsonUtils.toStringListJson(dto.getSynonyms()));
    entity.setAntonyms(JsonUtils.toStringListJson(dto.getAntonyms()));
    entity.setRelatedForward(JsonUtils.toStringListJson(dto.getRelatedForward()));
    entity.setRelatedBackward(JsonUtils.toStringListJson(dto.getRelatedBackward()));
    entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
    entity.setStatus(StatusEnum.ENABLED.getCode());
    return entity;
}
```

- [ ] **Step 6: 修改 convertToExerciseEntity 方法**

修改位置：`convertToExerciseEntity` 方法（约第 532 行）

```java
private VocabExercise convertToExerciseEntity(VocabExerciseDto dto, Integer wordId) {
    VocabExercise entity = new VocabExercise();
    entity.setWordId(wordId);
    entity.setQuestionType(dto.getQuestionType());
    entity.setQuestionText(dto.getQuestionText());
    entity.setOptions(JsonUtils.toExerciseOptionListJson(dto.getOptions()));
    entity.setAnswers(JsonUtils.toStringListJson(dto.getAnswers()));
    entity.setExerciseOrder(dto.getExerciseOrder() != null ? dto.getExerciseOrder() : 0);
    entity.setStatus(StatusEnum.ENABLED.getCode());
    return entity;
}
```

- [ ] **Step 7: 编译验证**

Run: `mvn compile -pl grid-system -am -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "feat: add json conversion logic to VocabWordServiceImpl"
```

---

## Task 9: 整体编译和验证

**Files:**
- 无新文件，验证已修改的文件

- [ ] **Step 1: 清理并重新编译整个项目**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: Commit（如有修复）**

如果有编译错误需要修复，修复后 commit。

---

## Plan Self-Review

1. **Spec Coverage:** ✅ 所有 spec 要求都有对应任务
   - ExerciseOption 类创建
   - JsonUtils 方法扩展
   - DTO/VO/Request 类型修改
   - Service 层转换逻辑

2. **Placeholder Scan:** ✅ 无占位符，所有步骤都有完整代码

3. **Type Consistency:** ✅ 所有类型一致，ExerciseOption 的包路径正确

