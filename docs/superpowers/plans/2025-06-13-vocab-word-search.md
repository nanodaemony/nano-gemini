# 词汇搜索 API 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `GET /api/vocabulary/search?word=xxx` 接口，供后台关联词选择场景精确搜索已发布的词汇及其义项。

**Architecture:** 轻量搜索端点，Repository 层通过 Spring Data JPA 方法名查询，Service 层过滤已发布状态并加载义项，Wrapper 转换为面向响应的 VO。不引入新 DTO，复用 `VocabWordDto`/`VocabSenseDto`。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Java 8

---

### Task 1: 创建 VocabWordBaseSearchVO

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseSearchVO.java`

- [ ] **Step 1: 创建 VO 文件**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class VocabWordBaseSearchVO implements Serializable {

    @ApiModelProperty(value = "词汇ID")
    private Integer id;

    @ApiModelProperty(value = "词汇词头")
    private String word;

    @ApiModelProperty(value = "对应义项列表")
    private List<VocabSenseSearchItemVO> senses;

    @Getter
    @Setter
    public static class VocabSenseSearchItemVO implements Serializable {

        @ApiModelProperty(value = "义项ID")
        private Integer id;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "中文释义")
        private String chineseDef;

        @ApiModelProperty(value = "中文释义外文翻译")
        private List<TextTranslationVO> defTranslations;
    }
}
```

- [ ] **Step 2: 编译验证**

  Run: `mvn compile -pl grid-system -am -q`
  Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

  ```bash
  git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseSearchVO.java
  git commit -m "feat: add VocabWordBaseSearchVO for vocab search API"
  ```

---

### Task 2: Repository 层 — 新增 findByWordAndStatus

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabWordRepository.java`

- [ ] **Step 1: 添加查询方法**

  在 `VocabWordRepository` 接口中添加：

  ```java
  List<VocabWord> findByWordAndStatus(String word, Integer status);
  ```

  Spring Data JPA 根据方法名自动实现 `where word = ?1 and status = ?2`。

- [ ] **Step 2: 编译验证**

  Run: `mvn compile -pl grid-system -am -q`
  Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

  ```bash
  git add grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabWordRepository.java
  git commit -m "feat: add findByWordAndStatus to VocabWordRepository"
  ```

---

### Task 3: Service 接口 — 新增 searchByWord 方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java`

- [ ] **Step 1: 添加接口方法**

  在 `VocabWordService` 中添加：

  ```java
  /**
   * 根据词汇文本精确搜索已发布的词汇及其义项
   * @param word 词汇文本（精确匹配）
   * @return 匹配的已发布词汇列表，无匹配返回空列表
   */
  List<VocabWordDto> searchByWord(String word);
  ```

- [ ] **Step 2: 编译验证**

  Run: `mvn compile -pl grid-system -am -q`
  Expected: BUILD SUCCESS（此时实现类未实现会编译失败，这是预期行为，下一步实现）

  Actually since this is a non-TDD flow, let's skip this intermediate compile. The impl will be right after.

- [ ] **Step 3: 提交**

  ```bash
  git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabWordService.java
  git commit -m "feat: add searchByWord to VocabWordService interface"
  ```

---

### Task 4: Service 实现 — 实现 searchByWord 逻辑

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

- [ ] **Step 1: 注入 VocabSenseRepository**

  确认 `VocabSenseRepository` 已在字段列表中（已有 `private final VocabSenseRepository vocabSenseRepository;`）。

- [ ] **Step 2: 添加 searchByWord 实现**

  ```java
  @Override
  public List<VocabWordDto> searchByWord(String word) {
      // 1. 精确查询（findByWordAndStatus 通过方法名自动实现 word=? AND status=?）
      List<VocabWord> words = vocabWordRepository.findByWordAndStatus(word, StatusEnum.ENABLED.getCode());

      // 2. 过滤已发布的词汇
      List<VocabWordDto> result = new ArrayList<>();
      for (VocabWord vocabWord : words) {
          if (!PublishStatusEnum.PUBLISHED.getCode().equals(vocabWord.getPublishStatus())) {
              continue;
          }

          VocabWordDto dto = new VocabWordDto();
          dto.setId(vocabWord.getId());
          dto.setWord(vocabWord.getWord());

          // 3. 查询义项（已发布词汇的义项已写回 vocab_sense 表）
          List<VocabSense> senses = vocabSenseRepository.findByWordIdAndStatus(
                  vocabWord.getId(), StatusEnum.ENABLED.getCode());

          List<VocabSenseDto> senseDtos = new ArrayList<>();
          for (VocabSense sense : senses) {
              VocabSenseDto senseDto = new VocabSenseDto();
              senseDto.setId(sense.getId());
              senseDto.setPartOfSpeech(sense.getPartOfSpeech());
              senseDto.setChineseDef(sense.getChineseDef());
              senseDto.setDefTranslations(JsonUtils.parseTranslationList(sense.getDefTranslations()));
              senseDtos.add(senseDto);
          }
          dto.setSenses(senseDtos);

          result.add(dto);
      }

      return result;
  }
  ```

  需要新增 import：

  ```java
  import com.naon.grid.enums.PublishStatusEnum;
  // JsonUtils 和 StatusEnum 已有
  ```

- [ ] **Step 3: 编译验证**

  Run: `mvn compile -pl grid-system -am -q`
  Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

  ```bash
  git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
  git commit -m "feat: implement searchByWord in VocabWordServiceImpl"
  ```

---

### Task 5: Wrapper 层 — 新增 VO 转换方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java`

- [ ] **Step 1: 添加 import 和转换方法**

  在文件顶部添加 import：

  ```java
  import com.naon.grid.backend.rest.vo.VocabWordBaseSearchVO;
  ```

  在类中添加转换方法：

  ```java
  public static List<VocabWordBaseSearchVO> toSearchVOList(List<VocabWordDto> dtos) {
      if (dtos == null) return Collections.emptyList();
      return dtos.stream().map(VocabWordWrapper::toSearchVO).collect(Collectors.toList());
  }

  public static VocabWordBaseSearchVO toSearchVO(VocabWordDto dto) {
      VocabWordBaseSearchVO vo = new VocabWordBaseSearchVO();
      vo.setId(dto.getId());
      vo.setWord(dto.getWord());
      vo.setSenses(toSenseSearchItemVOList(dto.getSenses()));
      return vo;
  }

  private static List<VocabWordBaseSearchVO.VocabSenseSearchItemVO> toSenseSearchItemVOList(
          List<VocabSenseDto> dtos) {
      if (dtos == null) return Collections.emptyList();
      return dtos.stream().map(VocabWordWrapper::toSenseSearchItemVO).collect(Collectors.toList());
  }

  private static VocabWordBaseSearchVO.VocabSenseSearchItemVO toSenseSearchItemVO(VocabSenseDto dto) {
      VocabWordBaseSearchVO.VocabSenseSearchItemVO vo =
              new VocabWordBaseSearchVO.VocabSenseSearchItemVO();
      vo.setId(dto.getId());
      vo.setPartOfSpeech(dto.getPartOfSpeech());
      vo.setChineseDef(dto.getChineseDef());
      vo.setDefTranslations(toTextTranslationVOList(dto.getDefTranslations()));
      return vo;
  }
  ```

- [ ] **Step 2: 编译验证**

  Run: `mvn compile -pl grid-system -am -q`
  Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

  ```bash
  git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java
  git commit -m "feat: add toSearchVO conversion to VocabWordWrapper"
  ```

---

### Task 6: Controller — 新增 search 端点

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

- [ ] **Step 1: 添加 import**

  ```java
  import com.naon.grid.backend.rest.vo.VocabWordBaseSearchVO;
  ```

- [ ] **Step 2: 添加到 Controller 类中**

  ```java
  @Log("搜索已发布词汇")
  @ApiOperation("根据词汇文本精确搜索已发布词汇（用于关联词选择）")
  @AnonymousGetMapping("/search")
  public ResponseEntity<List<VocabWordBaseSearchVO>> search(@RequestParam String word) {
      List<VocabWordDto> dtos = vocabWordService.searchByWord(word);
      return new ResponseEntity<>(VocabWordWrapper.toSearchVOList(dtos), HttpStatus.OK);
  }
  ```

  注意：Spring MVC 需要 `@RequestParam`，来自 `org.springframework.web.bind.annotation.RequestParam`。

- [ ] **Step 3: 编译验证**

  Run: `mvn compile -pl grid-system -am -q`
  Expected: BUILD SUCCESS

- [ ] **Step 4: 全量编译验证**

  Run: `mvn compile -q`
  Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

  ```bash
  git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java
  git commit -m "feat: add GET /api/vocabulary/search endpoint for vocab lookup"
  ```

---

## 自检清单

- [x] 覆盖所有 spec 需求：精确匹配、已发布过滤、词汇+义项信息、不分页
- [x] 无占位符：每个步骤都有完整代码和命令
- [x] 类型一致性：所有方法签名在接口、实现、Wrapper、Controller 之间一致
- [x] 遵循现有模式：`@AnonymousGetMapping`、`@Log`、`ResponseEntity`、Wrapper 转换
