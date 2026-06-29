# 用户端词汇API实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps using checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为普通用户提供词汇查询接口，包括搜索和详情查询两个接口。

**Architecture:** 复用后台已有的 VocabWordService，在 grid-app 模块新建用户端 Controller、Request 和 VO，扩展 AudioResourceService 支持批量查询。

**Tech Stack:** Spring Boot 2.7, Spring Data JPA, Lombok, Swagger

---

## 文件变更清单

### grid-system 模块
- **Modify:** `grid-system/src/main/java/com/naon/grid/backend/service/resource/AudioResourceService.java` - 新增批量查询方法
- **Modify:** `grid-system/src/main/java/com/naon/grid/backend/repo/resource/AudioResourceRepository.java` - 新增批量查询方法
- **Modify:** `grid-system/src/main/java/com/naon/grid/backend/service/resource/impl/AudioResourceServiceImpl.java` - 实现批量查询

### grid-app 模块
- **Create:** `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppVocabWordSearchRequest.java` - 搜索请求
- **Create:** `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabWordBaseVO.java` - 词汇基础信息
- **Create:** `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabWordDetailVO.java` - 词汇详情
- **Create:** `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java` - 用户端词汇接口

---

## 任务分解

### Task 1: 扩展 AudioResourceRepository 批量查询

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/repo/resource/AudioResourceRepository.java`

- [ ] **Step 1: 添加批量查询方法**

修改 `AudioResourceRepository.java`，添加以下方法：

```java
List<AudioResource> findByIdInAndStatus(List<Long> ids, Integer status);
```

最终文件内容：
```java
package com.naon.grid.backend.repo.resource;

import com.naon.grid.backend.domain.resource.AudioResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioResourceRepository extends JpaRepository<AudioResource, Long>, JpaSpecificationExecutor<AudioResource> {

    List<AudioResource> findByIdInAndStatus(List<Long> ids, Integer status);
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/repo/resource/AudioResourceRepository.java
git commit -m "feat: add batch query method for AudioResourceRepository"
```

---

### Task 2: 扩展 AudioResourceService 接口

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/resource/AudioResourceService.java`

- [ ] **Step 1: 添加批量查询接口方法**

修改 `AudioResourceService.java`，添加以下方法：

```java
List<AudioResourceDto> findByIds(List<Long> ids);
```

最终文件内容：
```java
package com.naon.grid.backend.service.resource;

import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AudioResourceService {

    Long create(AudioResourceDto resources);

    AudioResourceDto findById(Long id);

    List<AudioResourceDto> findByIds(List<Long> ids);

    PageResult<AudioResourceDto> queryAll(AudioResourceQueryCriteria criteria, Pageable pageable);

    void delete(Long id);
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/service/resource/AudioResourceService.java
git commit -m "feat: add batch query method for AudioResourceService"
```

---

### Task 3: 实现 AudioResourceService 批量查询

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/resource/impl/AudioResourceServiceImpl.java`

- [ ] **Step 1: 添加批量查询实现**

修改 `AudioResourceServiceImpl.java`，添加以下方法：

```java
@Override
public List<AudioResourceDto> findByIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
        return Collections.emptyList();
    }
    List<AudioResource> entities = audioResourceRepository.findByIdInAndStatus(ids, 1);
    return entities.stream()
            .map(audioResourceMapper::toDto)
            .collect(Collectors.toList());
}
```

需要添加 import：
```java
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
```

最终文件内容：
```java
package com.naon.grid.backend.service.resource.impl;

import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.backend.service.resource.dto.AudioResourceQueryCriteria;
import lombok.RequiredArgsConstructor;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.backend.domain.resource.AudioResource;
import com.naon.grid.backend.repo.resource.AudioResourceRepository;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.vocabulary.mapstruct.AudioResourceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AudioResourceServiceImpl implements AudioResourceService {

    private final AudioResourceRepository audioResourceRepository;
    private final AudioResourceMapper audioResourceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AudioResourceDto resources) {
        AudioResource entity = audioResourceMapper.toEntity(resources);
        entity = audioResourceRepository.save(entity);
        return entity.getId();
    }

    @Override
    public PageResult<AudioResourceDto> queryAll(AudioResourceQueryCriteria criteria, Pageable pageable) {
        Page<AudioResource> page = audioResourceRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            javax.persistence.criteria.Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            javax.persistence.criteria.Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), 1);
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        return PageUtil.toPage(page.map(audioResourceMapper::toDto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AudioResourceDto findById(Long id) {
        AudioResource audioResource = audioResourceRepository.findById(id).orElseGet(AudioResource::new);
        if (audioResource.getId() == null || audioResource.getStatus() == 0) {
            throw new EntityNotFoundException(AudioResource.class, "id", String.valueOf(id));
        }
        return audioResourceMapper.toDto(audioResource);
    }

    @Override
    public List<AudioResourceDto> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<AudioResource> entities = audioResourceRepository.findByIdInAndStatus(ids, 1);
        return entities.stream()
                .map(audioResourceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AudioResource audioResource = audioResourceRepository.findById(id).orElseGet(AudioResource::new);
        if (audioResource.getId() == null) {
            throw new EntityNotFoundException(AudioResource.class, "id", String.valueOf(id));
        }
        audioResource.setStatus(0);
        audioResourceRepository.save(audioResource);
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/service/resource/impl/AudioResourceServiceImpl.java
git commit -m "feat: implement batch query for AudioResourceService"
```

---

### Task 4: 创建 AppVocabWordSearchRequest

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppVocabWordSearchRequest.java`

- [ ] **Step 1: 创建搜索请求类**

创建文件 `AppVocabWordSearchRequest.java`：

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class AppVocabWordSearchRequest implements Serializable {

    @ApiModelProperty(value = "搜索关键词")
    private String blurry;
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/nano/Desktop/nano-gemini
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppVocabWordSearchRequest.java
git commit -m "feat: add AppVocabWordSearchRequest"
```

---

### Task 5: 创建 AppVocabWordBaseVO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabWordBaseVO.java`

- [ ] **Step 1: 创建词汇基础信息VO**

创建文件 `AppVocabWordBaseVO.java`：

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppVocabWordBaseVO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇")
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/nano/Desktop/nano-gemini
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabWordBaseVO.java
git commit -m "feat: add AppVocabWordBaseVO"
```

---

### Task 6: 创建 AppVocabWordDetailVO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabWordDetailVO.java`

- [ ] **Step 1: 创建词汇详情VO**

创建文件 `AppVocabWordDetailVO.java`：

```java
package com.naon.grid.modules.app.rest.vo;

import com.naon.grid.backend.rest.vo.ExerciseOptionVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class AppVocabWordDetailVO implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇")
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频")
    private AudioVO audio;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseVO> senses;

    @ApiModelProperty(value = "练习题列表")
    private List<VocabExerciseVO> exercises;

    @Getter
    @Setter
    public static class AudioVO implements Serializable {
        @ApiModelProperty(value = "音频文件地址")
        private String audioUrl;
    }

    @Getter
    @Setter
    public static class VocabSenseVO implements Serializable {
        @ApiModelProperty(value = "义项ID")
        private Integer id;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "中文释义")
        private String chineseDef;

        @ApiModelProperty(value = "释义音频")
        private AudioVO defAudio;

        @ApiModelProperty(value = "外文翻译列表")
        private List<TextTranslationVO> translations;

        @ApiModelProperty(value = "近义词列表")
        private List<SynonymVO> synonyms;

        @ApiModelProperty(value = "反义词列表")
        private List<AntonymVO> antonyms;

        @ApiModelProperty(value = "正序关联词汇")
        private List<RelatedWordVO> relatedForward;

        @ApiModelProperty(value = "逆序关联词汇")
        private List<RelatedWordVO> relatedBackward;

        @ApiModelProperty(value = "义项排序")
        private Integer senseOrder;

        @ApiModelProperty(value = "搭配列表")
        private List<VocabStructureVO> structures;
    }

    @Getter
    @Setter
    public static class SynonymVO implements Serializable {
        @ApiModelProperty(value = "近义词内容")
        private String content;
    }

    @Getter
    @Setter
    public static class AntonymVO implements Serializable {
        @ApiModelProperty(value = "反义词内容")
        private String content;
    }

    @Getter
    @Setter
    public static class RelatedWordVO implements Serializable {
        @ApiModelProperty(value = "关联词汇内容")
        private String content;
    }

    @Getter
    @Setter
    public static class VocabStructureVO implements Serializable {
        @ApiModelProperty(value = "搭配ID")
        private Integer id;

        @ApiModelProperty(value = "搭配文案")
        private String pattern;

        @ApiModelProperty(value = "搭配排序")
        private Integer structureOrder;

        @ApiModelProperty(value = "例句列表")
        private List<VocabExampleVO> examples;
    }

    @Getter
    @Setter
    public static class VocabExampleVO implements Serializable {
        @ApiModelProperty(value = "例句ID")
        private Integer id;

        @ApiModelProperty(value = "例句中文文案")
        private String sentence;

        @ApiModelProperty(value = "例句音频")
        private AudioVO audio;

        @ApiModelProperty(value = "例句拼音")
        private String pinyin;

        @ApiModelProperty(value = "例句外文翻译列表")
        private List<TextTranslationVO> translations;

        @ApiModelProperty(value = "例句排序")
        private Integer exampleOrder;
    }

    @Getter
    @Setter
    public static class VocabExerciseVO implements Serializable {
        @ApiModelProperty(value = "练习题ID")
        private Integer id;

        @ApiModelProperty(value = "题目类型")
        private String questionType;

        @ApiModelProperty(value = "练习题干")
        private String questionText;

        @ApiModelProperty(value = "选项列表")
        private List<ExerciseOptionVO> options;

        @ApiModelProperty(value = "答案列表")
        private List<String> answers;

        @ApiModelProperty(value = "练习题排序")
        private Integer exerciseOrder;
    }
}
```

- [ ] **Step 2: Commit**

```bash
cd /Users/nano/Desktop/nano-gemini
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabWordDetailVO.java
git commit -m "feat: add AppVocabWordDetailVO"
```

---

### Task 7: 创建 AppVocabWordController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java`

- [ ] **Step 1: 创建用户端词汇接口Controller**

创建文件 `AppVocabWordController.java`：

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.rest.vo.ExerciseOptionVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabExampleDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabExerciseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.modules.app.rest.request.AppVocabWordSearchRequest;
import com.naon.grid.modules.app.rest.vo.AppVocabWordBaseVO;
import com.naon.grid.modules.app.rest.vo.AppVocabWordDetailVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/vocab")
@Api(tags = "用户：词汇接口")
public class AppVocabWordController {

    private final VocabWordService vocabWordService;
    private final AudioResourceService audioResourceService;

    @ApiOperation("搜索词汇")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppVocabWordBaseVO>> search(AppVocabWordSearchRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "id"));
        List<VocabWordDto> dtos = vocabWordService.queryAll(criteria, pageable).getContent();
        List<AppVocabWordBaseVO> vos = toBaseVOList(dtos);
        return new ResponseEntity<>(vos, HttpStatus.OK);
    }

    @ApiOperation("词汇详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppVocabWordDetailVO> getDetail(@PathVariable Integer id) {
        VocabWordDto dto = vocabWordService.findById(id);
        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);
        AppVocabWordDetailVO vo = toDetailVO(dto, audioMap);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

    private List<AppVocabWordBaseVO> toBaseVOList(List<VocabWordDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(this::toBaseVO).collect(Collectors.toList());
    }

    private AppVocabWordBaseVO toBaseVO(VocabWordDto dto) {
        AppVocabWordBaseVO vo = new AppVocabWordBaseVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setHskLevel(dto.getHskLevel());
        return vo;
    }

    private Map<Long, AudioResourceDto> collectAndBatchQueryAudios(VocabWordDto dto) {
        List<Long> audioIds = new ArrayList<>();
        if (dto.getAudioId() != null) {
            audioIds.add(dto.getAudioId());
        }
        if (dto.getSenses() != null) {
            for (VocabSenseDto sense : dto.getSenses()) {
                if (sense.getDefAudioId() != null) {
                    audioIds.add(sense.getDefAudioId());
                }
                if (sense.getStructures() != null) {
                    for (VocabStructureDto structure : sense.getStructures()) {
                        if (structure.getExamples() != null) {
                            for (VocabExampleDto example : structure.getExamples()) {
                                if (example.getAudioId() != null) {
                                    audioIds.add(example.getAudioId());
                                }
                            }
                        }
                    }
                }
            }
        }
        List<AudioResourceDto> audioDtos = audioResourceService.findByIds(audioIds);
        return audioDtos.stream()
                .collect(Collectors.toMap(AudioResourceDto::getId, audio -> audio));
    }

    private AppVocabWordDetailVO toDetailVO(VocabWordDto dto, Map<Long, AudioResourceDto> audioMap) {
        AppVocabWordDetailVO vo = new AppVocabWordDetailVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        if (dto.getAudioId() != null && audioMap.containsKey(dto.getAudioId())) {
            AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
            audioVO.setAudioUrl(audioMap.get(dto.getAudioId()).getFileUrl());
            vo.setAudio(audioVO);
        }
        vo.setHskLevel(dto.getHskLevel());
        vo.setSenses(toSenseVOList(dto.getSenses(), audioMap));
        vo.setExercises(toExerciseVOList(dto.getExercises()));
        return vo;
    }

    private List<AppVocabWordDetailVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> dtos, Map<Long, AudioResourceDto> audioMap) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toSenseVO(dto, audioMap)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabSenseVO toSenseVO(VocabSenseDto dto, Map<Long, AudioResourceDto> audioMap) {
        AppVocabWordDetailVO.VocabSenseVO vo = new AppVocabWordDetailVO.VocabSenseVO();
        vo.setId(dto.getId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        if (dto.getDefAudioId() != null && audioMap.containsKey(dto.getDefAudioId())) {
            AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
            audioVO.setAudioUrl(audioMap.get(dto.getDefAudioId()).getFileUrl());
            vo.setDefAudio(audioVO);
        }
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setSynonyms(toSynonymVOList(dto.getSynonyms()));
        vo.setAntonyms(toAntonymVOList(dto.getAntonyms()));
        vo.setRelatedForward(toRelatedWordVOList(dto.getRelatedForward()));
        vo.setRelatedBackward(toRelatedWordVOList(dto.getRelatedBackward()));
        vo.setSenseOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures(), audioMap));
        return vo;
    }

    private List<AppVocabWordDetailVO.SynonymVO> toSynonymVOList(List<String> contents) {
        if (contents == null) {
            return Collections.emptyList();
        }
        return contents.stream().map(content -> {
            AppVocabWordDetailVO.SynonymVO vo = new AppVocabWordDetailVO.SynonymVO();
            vo.setContent(content);
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AppVocabWordDetailVO.AntonymVO> toAntonymVOList(List<String> contents) {
        if (contents == null) {
            return Collections.emptyList();
        }
        return contents.stream().map(content -> {
            AppVocabWordDetailVO.AntonymVO vo = new AppVocabWordDetailVO.AntonymVO();
            vo.setContent(content);
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AppVocabWordDetailVO.RelatedWordVO> toRelatedWordVOList(List<String> contents) {
        if (contents == null) {
            return Collections.emptyList();
        }
        return contents.stream().map(content -> {
            AppVocabWordDetailVO.RelatedWordVO vo = new AppVocabWordDetailVO.RelatedWordVO();
            vo.setContent(content);
            return vo;
        }).collect(Collectors.toList());
    }

    private List<AppVocabWordDetailVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> dtos, Map<Long, AudioResourceDto> audioMap) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toStructureVO(dto, audioMap)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabStructureVO toStructureVO(VocabStructureDto dto, Map<Long, AudioResourceDto> audioMap) {
        AppVocabWordDetailVO.VocabStructureVO vo = new AppVocabWordDetailVO.VocabStructureVO();
        vo.setId(dto.getId());
        vo.setPattern(dto.getPattern());
        vo.setStructureOrder(dto.getStructureOrder());
        vo.setExamples(toExampleVOList(dto.getExamples(), audioMap));
        return vo;
    }

    private List<AppVocabWordDetailVO.VocabExampleVO> toExampleVOList(List<VocabExampleDto> dtos, Map<Long, AudioResourceDto> audioMap) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toExampleVO(dto, audioMap)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabExampleVO toExampleVO(VocabExampleDto dto, Map<Long, AudioResourceDto> audioMap) {
        AppVocabWordDetailVO.VocabExampleVO vo = new AppVocabWordDetailVO.VocabExampleVO();
        vo.setId(dto.getId());
        vo.setSentence(dto.getSentence());
        if (dto.getAudioId() != null && audioMap.containsKey(dto.getAudioId())) {
            AppVocabWordDetailVO.AudioVO audioVO = new AppVocabWordDetailVO.AudioVO();
            audioVO.setAudioUrl(audioMap.get(dto.getAudioId()).getFileUrl());
            vo.setAudio(audioVO);
        }
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setExampleOrder(dto.getExampleOrder());
        return vo;
    }

    private List<AppVocabWordDetailVO.VocabExerciseVO> toExerciseVOList(List<VocabExerciseDto> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(this::toExerciseVO).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabExerciseVO toExerciseVO(VocabExerciseDto dto) {
        AppVocabWordDetailVO.VocabExerciseVO vo = new AppVocabWordDetailVO.VocabExerciseVO();
        vo.setId(dto.getId());
        vo.setQuestionType(dto.getQuestionType());
        vo.setQuestionText(dto.getQuestionText());
        vo.setOptions(toExerciseOptionVOList(dto.getOptions()));
        vo.setAnswers(dto.getAnswers());
        vo.setExerciseOrder(dto.getExerciseOrder());
        return vo;
    }

    private List<ExerciseOptionVO> toExerciseOptionVOList(List<com.naon.grid.domain.common.QuestionOption> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream().map(this::toExerciseOptionVO).collect(Collectors.toList());
    }

    private ExerciseOptionVO toExerciseOptionVO(com.naon.grid.domain.common.QuestionOption option) {
        if (option == null) {
            return null;
        }
        ExerciseOptionVO vo = new ExerciseOptionVO();
        vo.setOption(option.getOption());
        vo.setText(option.getText());
        return vo;
    }

    private List<TextTranslationVO> toTextTranslationVOList(List<com.naon.grid.domain.common.TextTranslation> translations) {
        if (translations == null) {
            return Collections.emptyList();
        }
        return translations.stream().map(this::toTextTranslationVO).collect(Collectors.toList());
    }

    private TextTranslationVO toTextTranslationVO(com.naon.grid.domain.common.TextTranslation translation) {
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

- [ ] **Step 2: Commit**

```bash
cd /Users/nano/Desktop/nano-gemini
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java
git commit -m "feat: add AppVocabWordController"
```

---

### Task 8: 编译验证

**Files:** N/A

- [ ] **Step 1: 编译项目**

```bash
cd /Users/nano/Desktop/nano-gemini
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 检查 git status**

```bash
cd /Users/nano/Desktop/nano-gemini
git status
```

Expected: 所有变更已提交，working tree clean

---

## 自检清单

- [x] **Spec覆盖检查**：所有需求都有对应的任务实现
  - 搜索接口（仅匹配word字段）：Task 7
  - 详情接口（包含练习题）：Task 7
  - 不暴露审计字段：Task 5, Task 6
  - 音频包装为URL：Task 7
  - 批量查询音频：Task 1, Task 2, Task 3, Task 7
  - 近义词/反义词/关联词汇包装为VO：Task 6

- [x] **Placeholder扫描**：没有 TODO 或未完成内容

- [x] **类型一致性检查**：所有类型、方法签名、属性名都保持一致

---

## 执行选择

Plan complete and saved to `docs/superpowers/plans/2026-05-30-app-vocab-api.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
