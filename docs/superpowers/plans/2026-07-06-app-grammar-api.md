# App 端语法接口实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 grid-app 模块实现 App 端语法点搜索和详情接口，复用 grid-system 现有服务。

**Architecture:** 在 GrammarPointService 中新增 `findPublishedById`/`searchPublished` 方法；在 grid-app 新建 Controller、VO、Request、Wrapper 类，遵循 `AppVocabWordController` 的预加载音频/图片模式。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Fastjson2, Lombok

## Global Constraints

- App 端 VO/Request 类不与后台共用对象
- 翻译字段按 `language` 参数筛选为单条 TextTranslationVO
- 音频/图片通过 ID 批量查询后直接返回 URL，找不到则 log.error 不抛异常
- 仅返回 `publishStatus = "published"` 的数据
- 草稿态/审核人/创建时间等管理字段不暴露给 App 端
- Wrapper 类放 `wrapper/` 包下，方法为 `public static`

---

### Task 1: 扩展 GrammarPointService 接口和实现

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/GrammarPointService.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/GrammarPointServiceImpl.java`

**Interfaces:**
- Produces: `GrammarPointDto findPublishedById(Long id)`, `PageResult<GrammarPointDto> searchPublished(String keyword, Pageable pageable)`

- [ ] **Step 1: 在 GrammarPointService 接口中新增两个方法声明**

```java
// 在 GrammarPointService.java 接口中新增：

/**
 * 查询已发布的语法点详情（不走草稿覆盖逻辑，仅返回发布态数据）
 */
GrammarPointDto findPublishedById(Long id);

/**
 * 按关键词搜索已发布的语法点
 */
PageResult<GrammarPointDto> searchPublished(String keyword, Pageable pageable);
```

- [ ] **Step 2: 在 GrammarPointServiceImpl 中实现 findPublishedById**

在 `GrammarPointServiceImpl.java` 中，在 `findById` 方法之后添加：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public GrammarPointDto findPublishedById(Long id) {
    if (id == null) {
        throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
    }
    GrammarPoint grammarPoint = grammarPointRepository.findById(id).orElseGet(GrammarPoint::new);
    if (grammarPoint.getId() == null || StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
        throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
    }
    if (!PublishStatusEnum.PUBLISHED.getCode().equals(grammarPoint.getPublishStatus())) {
        throw new BadRequestException("语法点尚未发布");
    }
    return toPublishedDetailDto(grammarPoint);
}
```

- [ ] **Step 3: 在 GrammarPointServiceImpl 中实现 searchPublished**

在 `findPublishedById` 方法之后添加：

```java
@Override
public PageResult<GrammarPointDto> searchPublished(String keyword, Pageable pageable) {
    Page<GrammarPoint> page = grammarPointRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode()));
        predicates.add(criteriaBuilder.equal(root.get("publishStatus"), PublishStatusEnum.PUBLISHED.getCode()));
        if (keyword != null && !keyword.trim().isEmpty()) {
            predicates.add(criteriaBuilder.like(root.get("name"), "%" + keyword.trim() + "%"));
        }
        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }, pageable);
    PageResult<GrammarPointDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
    populateGrammarListStats(pageResult.getContent());
    return pageResult;
}
```

需要在文件顶部添加 import：

```java
import java.util.ArrayList;
```

- [ ] **Step 4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/grammar/GrammarPointService.java
git add grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/GrammarPointServiceImpl.java
git commit -m "feat: add findPublishedById and searchPublished to GrammarPointService"
```

---

### Task 2: 创建 AppGrammarPointSearchRequest

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppGrammarPointSearchRequest.java`

**Interfaces:**
- Produces: `AppGrammarPointSearchRequest` with `String keyword` field

- [ ] **Step 1: 创建请求类**

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppGrammarPointSearchRequest implements Serializable {

    @ApiModelProperty(value = "搜索关键词（模糊匹配语法点名称）")
    private String keyword;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppGrammarPointSearchRequest.java
git commit -m "feat: add AppGrammarPointSearchRequest"
```

---

### Task 3: 创建 AppGrammarPointBaseVO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppGrammarPointBaseVO.java`

**Interfaces:**
- Produces: `AppGrammarPointBaseVO` — 搜索列表项 VO

- [ ] **Step 1: 创建 VO 类**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppGrammarPointBaseVO implements Serializable {

    @ApiModelProperty(value = "语法点ID")
    private Long id;

    @ApiModelProperty(value = "语法点名称")
    private String name;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "项目")
    private String project;

    @ApiModelProperty(value = "类别")
    private String category;

    @ApiModelProperty(value = "细目")
    private String subCategory;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppGrammarPointBaseVO.java
git commit -m "feat: add AppGrammarPointBaseVO"
```

---

### Task 4: 创建 AppGrammarPointDetailVO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppGrammarPointDetailVO.java`

**Interfaces:**
- Produces: `AppGrammarPointDetailVO` 及其所有嵌套内部类

- [ ] **Step 1: 创建完整 VO 类**

```java
package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppGrammarPointDetailVO implements Serializable {

    @ApiModelProperty(value = "语法点ID")
    private Long id;

    @ApiModelProperty(value = "语法点名称")
    private String name;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "项目")
    private String project;

    @ApiModelProperty(value = "类别")
    private String category;

    @ApiModelProperty(value = "细目")
    private String subCategory;

    @ApiModelProperty(value = "语法意义列表")
    private List<GrammarMeaningVO> meanings;

    @ApiModelProperty(value = "语法结构列表")
    private List<GrammarStructureVO> structures;

    @ApiModelProperty(value = "语法注意列表")
    private List<GrammarNoticeVO> notices;

    @ApiModelProperty(value = "语法偏误列表")
    private List<GrammarErrorVO> errors;

    @ApiModelProperty(value = "关联题目ID列表")
    private List<Long> questionIds;

    @ApiModelProperty(value = "关联辨析组列表")
    private List<GrammarComparisonVO> comparisons;

    // ===== 嵌套 VO =====

    @Getter
    @Setter
    public static class AudioVO implements Serializable {
        @ApiModelProperty(value = "音频文件地址")
        private String audioUrl;
    }

    @Getter
    @Setter
    public static class ImageVO implements Serializable {
        @ApiModelProperty(value = "图片文件地址")
        private String imageUrl;
    }

    @Getter
    @Setter
    public static class ExampleVO implements Serializable {
        @ApiModelProperty(value = "例句中文文案")
        private String sentence;

        @ApiModelProperty(value = "例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "例句外文翻译（按语言筛选后的单条）")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "例句音频")
        private AudioVO audio;

        @ApiModelProperty(value = "例句图片")
        private ImageVO image;

        @ApiModelProperty(value = "排序")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarMeaningVO implements Serializable {
        @ApiModelProperty(value = "语法意义ID")
        private Long id;

        @ApiModelProperty(value = "语法意义内容")
        private String content;

        @ApiModelProperty(value = "外文翻译（按语言筛选后的单条）")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "图片")
        private ImageVO image;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleVO> sentences;

        @ApiModelProperty(value = "排序")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarStructureVO implements Serializable {
        @ApiModelProperty(value = "语法结构ID")
        private Long id;

        @ApiModelProperty(value = "结构文本")
        private String content;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleVO> sentences;

        @ApiModelProperty(value = "排序")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarNoticeVO implements Serializable {
        @ApiModelProperty(value = "语法注意ID")
        private Long id;

        @ApiModelProperty(value = "注意内容")
        private String content;

        @ApiModelProperty(value = "外文翻译（按语言筛选后的单条）")
        private TextTranslationVO translation;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleVO> sentences;

        @ApiModelProperty(value = "排序")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarErrorVO implements Serializable {
        @ApiModelProperty(value = "语法偏误ID")
        private Long id;

        @ApiModelProperty(value = "偏误描述")
        private String content;

        @ApiModelProperty(value = "偏误分析")
        private String analysis;

        @ApiModelProperty(value = "偏误分析外文翻译（按语言筛选后的单条）")
        private TextTranslationVO analysisTranslation;

        @ApiModelProperty(value = "排序")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarComparisonVO implements Serializable {
        @ApiModelProperty(value = "辨析组ID")
        private Long id;

        @ApiModelProperty(value = "辨析组标识（如\"会vs能\"）")
        private String groupKey;

        @ApiModelProperty(value = "辨析条目列表")
        private List<ComparisonItemVO> items;
    }

    @Getter
    @Setter
    public static class ComparisonItemVO implements Serializable {
        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "语法点名称")
        private String grammarName;

        @ApiModelProperty(value = "用法对比说明")
        private String usageComparison;

        @ApiModelProperty(value = "用法对比外文翻译（按语言筛选后的单条）")
        private TextTranslationVO usageComparisonTranslation;

        @ApiModelProperty(value = "例句文本（含正误标记）")
        private String exampleSentences;

        @ApiModelProperty(value = "用法例句")
        private ExampleVO usageSentence;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppGrammarPointDetailVO.java
git commit -m "feat: add AppGrammarPointDetailVO with nested VOs"
```

---

### Task 5: 创建 AppGrammarPointWrapper

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppGrammarPointWrapper.java`

**Interfaces:**
- Consumes: `GrammarPointDto`, `GrammarMeaningDto`, `GrammarStructureDto`, `GrammarNoticeDto`, `GrammarErrorDto`, `GrammarComparisonGroupDto`, `GrammarComparisonItemDto`, `GrammarComparisonChatDto`, `ExampleSentenceDto`, `AudioResourceDto`, `AliOssStorageDto`, `TextTranslation`, `TextTranslationVO`
- Produces: `List<AppGrammarPointBaseVO>`, `AppGrammarPointDetailVO`

- [ ] **Step 1: 创建 Wrapper 类**

```java
package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.grammar.dto.GrammarErrorDto;
import com.naon.grid.backend.service.grammar.dto.GrammarMeaningDto;
import com.naon.grid.backend.service.grammar.dto.GrammarNoticeDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarStructureDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonChatDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonItemDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointBaseVO;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointDetailVO;
import com.naon.grid.service.dto.AliOssStorageDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AppGrammarPointWrapper {

    // ===== 搜索列表 =====

    public static List<AppGrammarPointBaseVO> toBaseVOList(List<GrammarPointDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(AppGrammarPointWrapper::toBaseVO).collect(Collectors.toList());
    }

    public static AppGrammarPointBaseVO toBaseVO(GrammarPointDto dto) {
        AppGrammarPointBaseVO vo = new AppGrammarPointBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setHskLevel(dto.getHskLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setSubCategory(dto.getSubCategory());
        return vo;
    }

    // ===== 详情 =====

    public static AppGrammarPointDetailVO toDetailVO(GrammarPointDto dto,
            Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap,
            Map<Long, ExampleSentenceDto> sentenceMap,
            List<GrammarComparisonGroupDto> comparisons,
            String language) {
        AppGrammarPointDetailVO vo = new AppGrammarPointDetailVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setHskLevel(dto.getHskLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setSubCategory(dto.getSubCategory());
        vo.setMeanings(toMeaningVOList(dto.getMeanings(), audioMap, imageMap, language));
        vo.setStructures(toStructureVOList(dto.getStructures(), audioMap, imageMap, language));
        vo.setNotices(toNoticeVOList(dto.getNotices(), audioMap, imageMap, language));
        vo.setErrors(toErrorVOList(dto.getErrors(), language));
        vo.setQuestionIds(dto.getQuestionIds());
        vo.setComparisons(toComparisonVOList(comparisons, audioMap, imageMap, sentenceMap, language));
        return vo;
    }

    // ===== 意义 =====

    private static List<AppGrammarPointDetailVO.GrammarMeaningVO> toMeaningVOList(
            List<GrammarMeaningDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toMeaningVO(d, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.GrammarMeaningVO toMeaningVO(
            GrammarMeaningDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        AppGrammarPointDetailVO.GrammarMeaningVO vo = new AppGrammarPointDetailVO.GrammarMeaningVO();
        vo.setId(dto.getId());
        vo.setContent(dto.getMeaningContent());
        vo.setTranslation(filterByLanguage(dto.getMeaningContentTranslations(), language));
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                AppGrammarPointDetailVO.ImageVO imageVO = new AppGrammarPointDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setImage(imageVO);
            } else {
                log.error("语法意义图片资源未找到, imageId={}", dto.getImageId());
            }
        }
        vo.setSentences(toExampleVOList(dto.getSentences(), audioMap, imageMap, language));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 结构 =====

    private static List<AppGrammarPointDetailVO.GrammarStructureVO> toStructureVOList(
            List<GrammarStructureDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toStructureVO(d, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.GrammarStructureVO toStructureVO(
            GrammarStructureDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        AppGrammarPointDetailVO.GrammarStructureVO vo = new AppGrammarPointDetailVO.GrammarStructureVO();
        vo.setId(dto.getId());
        vo.setContent(dto.getStructureContent());
        vo.setSentences(toExampleVOList(dto.getSentences(), audioMap, imageMap, language));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 注意 =====

    private static List<AppGrammarPointDetailVO.GrammarNoticeVO> toNoticeVOList(
            List<GrammarNoticeDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toNoticeVO(d, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.GrammarNoticeVO toNoticeVO(
            GrammarNoticeDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        AppGrammarPointDetailVO.GrammarNoticeVO vo = new AppGrammarPointDetailVO.GrammarNoticeVO();
        vo.setId(dto.getId());
        vo.setContent(dto.getNoticeContent());
        vo.setTranslation(filterByLanguage(dto.getNoticeContentTranslations(), language));
        vo.setSentences(toExampleVOList(dto.getSentences(), audioMap, imageMap, language));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 偏误 =====

    private static List<AppGrammarPointDetailVO.GrammarErrorVO> toErrorVOList(
            List<GrammarErrorDto> dtos, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toErrorVO(d, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.GrammarErrorVO toErrorVO(GrammarErrorDto dto, String language) {
        AppGrammarPointDetailVO.GrammarErrorVO vo = new AppGrammarPointDetailVO.GrammarErrorVO();
        vo.setId(dto.getId());
        vo.setContent(dto.getErrorContent());
        vo.setAnalysis(dto.getErrorAnalysis());
        vo.setAnalysisTranslation(filterByLanguage(dto.getErrorAnalysisTranslations(), language));
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 辨析组 =====

    private static List<AppGrammarPointDetailVO.GrammarComparisonVO> toComparisonVOList(
            List<GrammarComparisonGroupDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, Map<Long, ExampleSentenceDto> sentenceMap,
            String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toComparisonVO(d, audioMap, imageMap, sentenceMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.GrammarComparisonVO toComparisonVO(
            GrammarComparisonGroupDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, Map<Long, ExampleSentenceDto> sentenceMap,
            String language) {
        AppGrammarPointDetailVO.GrammarComparisonVO vo = new AppGrammarPointDetailVO.GrammarComparisonVO();
        vo.setId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setItems(toComparisonItemVOList(dto.getItems(), audioMap, imageMap, sentenceMap, language));
        return vo;
    }

    private static List<AppGrammarPointDetailVO.ComparisonItemVO> toComparisonItemVOList(
            List<GrammarComparisonItemDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, Map<Long, ExampleSentenceDto> sentenceMap,
            String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toComparisonItemVO(d, audioMap, imageMap, sentenceMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.ComparisonItemVO toComparisonItemVO(
            GrammarComparisonItemDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, Map<Long, ExampleSentenceDto> sentenceMap,
            String language) {
        AppGrammarPointDetailVO.ComparisonItemVO vo = new AppGrammarPointDetailVO.ComparisonItemVO();
        vo.setGrammarId(dto.getGrammarId());
        vo.setGrammarName(dto.getGrammarName());
        vo.setUsageComparison(dto.getUsageComparison());
        vo.setUsageComparisonTranslation(filterByLanguage(dto.getUsageComparisonTranslations(), language));
        vo.setExampleSentences(dto.getExampleSentences());
        if (dto.getUsageSentenceId() != null && sentenceMap != null) {
            ExampleSentenceDto sentenceDto = sentenceMap.get(dto.getUsageSentenceId());
            if (sentenceDto != null) {
                vo.setUsageSentence(toExampleVO(sentenceDto, audioMap, imageMap, language));
            } else {
                log.error("辨析条目的用法例句未找到, usageSentenceId={}", dto.getUsageSentenceId());
            }
        }
        return vo;
    }

    // ===== 例句（公共） =====

    private static List<AppGrammarPointDetailVO.ExampleVO> toExampleVOList(
            List<ExampleSentenceDto> dtos, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toExampleVO(d, audioMap, imageMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.ExampleVO toExampleVO(
            ExampleSentenceDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, String language) {
        AppGrammarPointDetailVO.ExampleVO vo = new AppGrammarPointDetailVO.ExampleVO();
        vo.setSentence(dto.getSentence());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppGrammarPointDetailVO.AudioVO audioVO = new AppGrammarPointDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            } else {
                log.error("例句音频资源未找到, audioId={}", dto.getAudioId());
            }
        }
        if (dto.getImageId() != null && imageMap != null) {
            AliOssStorageDto imgDto = imageMap.get(dto.getImageId());
            if (imgDto != null) {
                AppGrammarPointDetailVO.ImageVO imageVO = new AppGrammarPointDetailVO.ImageVO();
                imageVO.setImageUrl(imgDto.getFileUrl());
                vo.setImage(imageVO);
            } else {
                log.error("例句图片资源未找到, imageId={}", dto.getImageId());
            }
        }
        vo.setOrder(dto.getOrder());
        return vo;
    }

    // ===== 翻译过滤（公共） =====

    private static TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
        if (translations == null || language == null) {
            return null;
        }
        return translations.stream()
                .filter(t -> language.equals(t.getLanguage()))
                .findFirst()
                .map(AppGrammarPointWrapper::toTextTranslationVO)
                .orElse(null);
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation translation) {
        if (translation == null) {
            return null;
        }
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppGrammarPointWrapper.java
git commit -m "feat: add AppGrammarPointWrapper with audio/image URL resolution"
```

---

### Task 6: 创建 AppGrammarPointController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppGrammarPointController.java`

**Interfaces:**
- Consumes: `GrammarPointService`, `AudioResourceService`, `AliOssStorageService`, `GrammarComparisonGroupService`, `ExampleSentenceService`, `AppGrammarPointWrapper`
- Produces: `GET /api/app/grammar/search`, `GET /api/app/grammar/{id}`

- [ ] **Step 1: 创建 Controller 类**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarErrorDto;
import com.naon.grid.backend.service.grammar.dto.GrammarMeaningDto;
import com.naon.grid.backend.service.grammar.dto.GrammarNoticeDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarStructureDto;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonChatDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonItemDto;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.modules.app.rest.request.AppGrammarPointSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointBaseVO;
import com.naon.grid.modules.app.rest.vo.AppGrammarPointDetailVO;
import com.naon.grid.modules.app.rest.wrapper.AppGrammarPointWrapper;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/grammar")
@Api(tags = "用户：语法接口")
public class AppGrammarPointController {

    private final GrammarPointService grammarPointService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;
    private final GrammarComparisonGroupService grammarComparisonGroupService;
    private final ExampleSentenceService exampleSentenceService;

    @ApiOperation("搜索语法点")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppGrammarPointBaseVO>> search(AppGrammarPointSearchRequest request) {
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "id"));
        List<GrammarPointDto> dtos = grammarPointService.searchPublished(request.getKeyword(), pageable).getContent();
        return new ResponseEntity<>(AppGrammarPointWrapper.toBaseVOList(dtos), HttpStatus.OK);
    }

    @ApiOperation("语法点详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppGrammarPointDetailVO> getDetail(
            @PathVariable Long id,
            @RequestParam String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        GrammarPointDto dto = grammarPointService.findPublishedById(id);

        // 预加载音频和图片资源
        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);
        Map<Long, AliOssStorageDto> imageMap = collectAndBatchQueryImages(dto);

        // 预加载辨析组（含条目、对话例句等）
        List<GrammarComparisonGroupDto> comparisons = grammarComparisonGroupService.searchByGrammarId(id);
        // 从辨析组中收集额外的资源：usageSentenceId → ExampleSentenceDto + 音频ID
        Map<Long, ExampleSentenceDto> sentenceMap = collectComparisonSentences(comparisons);
        List<Long> comparisonAudioIds = collectSentenceAudios(sentenceMap);
        List<Long> comparisonImageIds = collectSentenceImages(sentenceMap);
        // 也从 chat 中收集音频ID
        comparisonAudioIds.addAll(collectChatAudios(comparisons));
        audioMap = mergeAudioMap(audioMap, comparisonAudioIds);
        imageMap = mergeImageMap(imageMap, comparisonImageIds);

        AppGrammarPointDetailVO vo = AppGrammarPointWrapper.toDetailVO(dto, audioMap, imageMap, sentenceMap, comparisons, language);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    // ===== 音频/图片预加载 =====

    private Map<Long, AudioResourceDto> collectAndBatchQueryAudios(GrammarPointDto dto) {
        List<Long> audioIds = new ArrayList<>();
        collectFromMeanings(dto.getMeanings(), audioIds);
        collectFromStructures(dto.getStructures(), audioIds);
        collectFromNotices(dto.getNotices(), audioIds);
        // errors 没有例句，跳过
        return batchQueryAudios(audioIds);
    }

    private void collectFromMeanings(List<GrammarMeaningDto> meanings, List<Long> audioIds) {
        if (meanings == null) return;
        for (GrammarMeaningDto m : meanings) {
            collectFromSentences(m.getSentences(), audioIds);
        }
    }

    private void collectFromStructures(List<GrammarStructureDto> structures, List<Long> audioIds) {
        if (structures == null) return;
        for (GrammarStructureDto s : structures) {
            collectFromSentences(s.getSentences(), audioIds);
        }
    }

    private void collectFromNotices(List<GrammarNoticeDto> notices, List<Long> audioIds) {
        if (notices == null) return;
        for (GrammarNoticeDto n : notices) {
            collectFromSentences(n.getSentences(), audioIds);
        }
    }

    private void collectFromSentences(List<ExampleSentenceDto> sentences, List<Long> audioIds) {
        if (sentences == null) return;
        for (ExampleSentenceDto s : sentences) {
            if (s.getAudioId() != null) {
                audioIds.add(s.getAudioId());
            }
        }
    }

    private Map<Long, AudioResourceDto> batchQueryAudios(List<Long> audioIds) {
        if (audioIds.isEmpty()) return Collections.emptyMap();
        List<AudioResourceDto> audioDtos = audioResourceService.findByIds(audioIds);
        return audioDtos.stream()
                .collect(Collectors.toMap(AudioResourceDto::getId, a -> a, (a, b) -> a));
    }

    private Map<Long, AliOssStorageDto> collectAndBatchQueryImages(GrammarPointDto dto) {
        List<Long> imageIds = new ArrayList<>();
        if (dto.getMeanings() != null) {
            for (GrammarMeaningDto m : dto.getMeanings()) {
                if (m.getImageId() != null) {
                    imageIds.add(m.getImageId());
                }
                if (m.getSentences() != null) {
                    for (ExampleSentenceDto s : m.getSentences()) {
                        if (s.getImageId() != null) {
                            imageIds.add(s.getImageId());
                        }
                    }
                }
            }
        }
        if (dto.getStructures() != null) {
            for (GrammarStructureDto s : dto.getStructures()) {
                if (s.getSentences() != null) {
                    for (ExampleSentenceDto se : s.getSentences()) {
                        if (se.getImageId() != null) {
                            imageIds.add(se.getImageId());
                        }
                    }
                }
            }
        }
        if (dto.getNotices() != null) {
            for (GrammarNoticeDto n : dto.getNotices()) {
                if (n.getSentences() != null) {
                    for (ExampleSentenceDto se : n.getSentences()) {
                        if (se.getImageId() != null) {
                            imageIds.add(se.getImageId());
                        }
                    }
                }
            }
        }
        if (imageIds.isEmpty()) return Collections.emptyMap();
        List<AliOssStorageDto> imageDtos = aliOssStorageService.findByIds(imageIds);
        return imageDtos.stream()
                .collect(Collectors.toMap(AliOssStorageDto::getId, i -> i, (i, j) -> i));
    }

    // ===== 辨析组资源收集 =====

    private Map<Long, ExampleSentenceDto> collectComparisonSentences(List<GrammarComparisonGroupDto> comparisons) {
        List<Long> sentenceIds = new ArrayList<>();
        if (comparisons != null) {
            for (GrammarComparisonGroupDto group : comparisons) {
                if (group.getItems() != null) {
                    for (GrammarComparisonItemDto item : group.getItems()) {
                        if (item.getUsageSentenceId() != null) {
                            sentenceIds.add(item.getUsageSentenceId());
                        }
                    }
                }
            }
        }
        if (sentenceIds.isEmpty()) return Collections.emptyMap();
        return exampleSentenceService.findByIds(sentenceIds);
    }

    private List<Long> collectSentenceAudios(Map<Long, ExampleSentenceDto> sentenceMap) {
        List<Long> audioIds = new ArrayList<>();
        for (ExampleSentenceDto s : sentenceMap.values()) {
            if (s.getAudioId() != null) {
                audioIds.add(s.getAudioId());
            }
        }
        return audioIds;
    }

    private List<Long> collectSentenceImages(Map<Long, ExampleSentenceDto> sentenceMap) {
        List<Long> imageIds = new ArrayList<>();
        for (ExampleSentenceDto s : sentenceMap.values()) {
            if (s.getImageId() != null) {
                imageIds.add(s.getImageId());
            }
        }
        return imageIds;
    }

    private List<Long> collectChatAudios(List<GrammarComparisonGroupDto> comparisons) {
        List<Long> audioIds = new ArrayList<>();
        if (comparisons == null) return audioIds;
        for (GrammarComparisonGroupDto group : comparisons) {
            if (group.getChats() != null) {
                for (GrammarComparisonChatDto chat : group.getChats()) {
                    if (chat.getAudioId() != null) {
                        audioIds.add(chat.getAudioId());
                    }
                }
            }
        }
        return audioIds;
    }

    private Map<Long, AudioResourceDto> mergeAudioMap(Map<Long, AudioResourceDto> existing, List<Long> newIds) {
        if (newIds.isEmpty()) return existing;
        List<Long> missing = newIds.stream()
                .filter(id -> !existing.containsKey(id))
                .collect(Collectors.toList());
        if (missing.isEmpty()) return existing;
        Map<Long, AudioResourceDto> merged = new java.util.HashMap<>(existing);
        List<AudioResourceDto> newDtos = audioResourceService.findByIds(missing);
        for (AudioResourceDto a : newDtos) {
            merged.put(a.getId(), a);
        }
        return merged;
    }

    private Map<Long, AliOssStorageDto> mergeImageMap(Map<Long, AliOssStorageDto> existing, List<Long> newIds) {
        if (newIds.isEmpty()) return existing;
        List<Long> missing = newIds.stream()
                .filter(id -> !existing.containsKey(id))
                .collect(Collectors.toList());
        if (missing.isEmpty()) return existing;
        Map<Long, AliOssStorageDto> merged = new java.util.HashMap<>(existing);
        List<AliOssStorageDto> newDtos = aliOssStorageService.findByIds(missing);
        for (AliOssStorageDto i : newDtos) {
            merged.put(i.getId(), i);
        }
        return merged;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppGrammarPointController.java
git commit -m "feat: add AppGrammarPointController with search and detail endpoints"
```

---

### Task 7: 编译验证

**Files:** 无新建/修改

- [ ] **Step 1: 编译所有模块**

```bash
cd grid-bootstrap && mvn compile -pl ../grid-system,../grid-app -am -DskipTests
```

预期: BUILD SUCCESS

- [ ] **Step 2: 确认无编译错误后完成**

```bash
echo "All tasks complete"
```
