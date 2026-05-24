# 词汇管理功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 grid-system 模块中实现词汇管理后台功能，支持词汇的新增、查询详情、搜索和音频资源管理。

**Architecture:** 遵循现有项目结构，使用 JPA + MapStruct，采用分层架构（Domain/Repository/Service/DTO/Controller）。

**Tech Stack:** Spring Boot 2.x, Spring Data JPA, MapStruct, MySQL

---

## 文件清单

| 操作 | 文件路径 | 说明 |
|------|---------|------|
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabWord.java` | 词汇实体类 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabSense.java` | 义项实体类 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabStructure.java` | 搭配实体类 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabExample.java` | 例句实体类 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabExercise.java` | 练习题实体类 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/AudioResource.java` | 音频资源实体类 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabWordRepository.java` | 词汇 Repository |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabSenseRepository.java` | 义项 Repository |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabStructureRepository.java` | 搭配 Repository |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabExampleRepository.java` | 例句 Repository |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabExerciseRepository.java` | 练习题 Repository |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/AudioResourceRepository.java` | 音频资源 Repository |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabWordDto.java` | 词汇 DTO |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabWordQueryCriteria.java` | 词汇查询条件 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabSenseDto.java` | 义项 DTO |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabStructureDto.java` | 搭配 DTO |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabExampleDto.java` | 例句 DTO |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabExerciseDto.java` | 练习题 DTO |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/AudioResourceDto.java` | 音频资源 DTO |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/AudioResourceQueryCriteria.java` | 音频资源查询条件 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/mapstruct/VocabWordMapper.java` | 词汇 MapStruct 映射 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/mapstruct/AudioResourceMapper.java` | 音频资源 MapStruct 映射 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/VocabWordService.java` | 词汇 Service 接口 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/AudioResourceService.java` | 音频资源 Service 接口 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/impl/VocabWordServiceImpl.java` | 词汇 Service 实现 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/impl/AudioResourceServiceImpl.java` | 音频资源 Service 实现 |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/rest/VocabWordController.java` | 词汇 Controller |
| Create | `grid-system/src/main/java/com/naon/grid/modules/vocabulary/rest/AudioResourceController.java` | 音频资源 Controller |

---

## Task 1: 创建词汇相关实体类

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabWord.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabSense.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabStructure.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabExample.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabExercise.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/AudioResource.java`

- [ ] **Step 1: 创建 VocabWord.java**

```java
package com.naon.grid.modules.vocabulary.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import com.naon.grid.base.BaseEntity;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_word")
public class VocabWord implements Serializable {

    @Id
    @Column(name = "id")
    @NotNull(groups = Update.class)
    @ApiModelProperty(value = "词汇唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank
    @Column(name = "word", nullable = false, length = 50)
    @ApiModelProperty(value = "词汇")
    private String word;

    @Column(name = "word_traditional", length = 50)
    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @NotBlank
    @Column(name = "pinyin", nullable = false, length = 100)
    @ApiModelProperty(value = "标准拼音（含声调）")
    private String pinyin;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @Column(name = "hsk_level", length = 20)
    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @Column(name = "create_time", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (this.createTime == null) {
            this.createTime = now;
        }
        if (this.updateTime == null) {
            this.updateTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}
```

- [ ] **Step 2: 创建 VocabSense.java**

```java
package com.naon.grid.modules.vocabulary.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_sense")
public class VocabSense implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "自增ID, 义项ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @Column(name = "part_of_speech", length = 50)
    @ApiModelProperty(value = "词性")
    private String partOfSpeech;

    @Column(name = "chinese_def", columnDefinition = "text")
    @ApiModelProperty(value = "中文释义")
    private String chineseDef;

    @Column(name = "def_audio_id")
    @ApiModelProperty(value = "中文释义音频资源ID")
    private Long defAudioId;

    @Column(name = "translations", columnDefinition = "json")
    @ApiModelProperty(value = "外文翻译列表")
    private String translations;

    @Column(name = "synonyms", columnDefinition = "text")
    @ApiModelProperty(value = "近义词列表")
    private String synonyms;

    @Column(name = "antonyms", columnDefinition = "text")
    @ApiModelProperty(value = "反义词列表")
    private String antonyms;

    @Column(name = "related_forward", columnDefinition = "text")
    @ApiModelProperty(value = "正序关联词汇")
    private String relatedForward;

    @Column(name = "related_backward", columnDefinition = "text")
    @ApiModelProperty(value = "逆序关联词汇")
    private String relatedBackward;

    @NotNull
    @Column(name = "sense_order", nullable = false)
    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (this.createTime == null) {
            this.createTime = now;
        }
        if (this.updateTime == null) {
            this.updateTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}
```

- [ ] **Step 3: 创建 VocabStructure.java**

```java
package com.naon.grid.modules.vocabulary.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_structure")
public class VocabStructure implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "自增ID, 结构搭配ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @NotNull
    @Column(name = "sense_id", nullable = false)
    @ApiModelProperty(value = "所属义项ID")
    private Integer senseId;

    @NotBlank
    @Column(name = "pattern", nullable = false, length = 255)
    @ApiModelProperty(value = "结构搭配文案")
    private String pattern;

    @NotNull
    @Column(name = "structure_order", nullable = false)
    @ApiModelProperty(value = "搭配排序权重")
    private Integer structureOrder = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (this.createTime == null) {
            this.createTime = now;
        }
        if (this.updateTime == null) {
            this.updateTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}
```

- [ ] **Step 4: 创建 VocabExample.java**

```java
package com.naon.grid.modules.vocabulary.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_example")
public class VocabExample implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "例句唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @NotNull
    @Column(name = "sense_id", nullable = false)
    @ApiModelProperty(value = "所属义项ID")
    private Integer senseId;

    @NotNull
    @Column(name = "structure_id", nullable = false)
    @ApiModelProperty(value = "所属结构搭配ID")
    private Integer structureId;

    @NotBlank
    @Column(name = "sentence", nullable = false, columnDefinition = "text")
    @ApiModelProperty(value = "例句中文文案")
    private String sentence;

    @Column(name = "audio_id")
    @ApiModelProperty(value = "例句音频资源ID")
    private Long audioId;

    @Column(name = "pinyin", length = 500)
    @ApiModelProperty(value = "例句拼音")
    private String pinyin;

    @Column(name = "translations", columnDefinition = "json")
    @ApiModelProperty(value = "例句外文翻译列表")
    private String translations;

    @NotNull
    @Column(name = "example_order", nullable = false)
    @ApiModelProperty(value = "例句排序权重")
    private Integer exampleOrder = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (this.createTime == null) {
            this.createTime = now;
        }
        if (this.updateTime == null) {
            this.updateTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}
```

- [ ] **Step 5: 创建 VocabExercise.java**

```java
package com.naon.grid.modules.vocabulary.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_exercise")
public class VocabExercise implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "练习题目唯一ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "word_id", nullable = false)
    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @NotBlank
    @Column(name = "question_type", nullable = false, length = 20)
    @ApiModelProperty(value = "题目类型")
    private String questionType;

    @NotBlank
    @Column(name = "question_text", nullable = false, columnDefinition = "text")
    @ApiModelProperty(value = "练习题干描述")
    private String questionText;

    @Column(name = "options", columnDefinition = "json")
    @ApiModelProperty(value = "选项列表")
    private String options;

    @Column(name = "answers", columnDefinition = "json")
    @ApiModelProperty(value = "答案列表")
    private String answers;

    @NotNull
    @Column(name = "exercise_order", nullable = false)
    @ApiModelProperty(value = "练习题目排序权重")
    private Integer exerciseOrder = 0;

    @Column(name = "create_time", nullable = false, updatable = false)
    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (this.createTime == null) {
            this.createTime = now;
        }
        if (this.updateTime == null) {
            this.updateTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}
```

- [ ] **Step 6: 创建 AudioResource.java**

```java
package com.naon.grid.modules.vocabulary.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "audio_resource")
public class AudioResource implements Serializable {

    @Id
    @Column(name = "id")
    @NotNull(groups = Update.class)
    @ApiModelProperty(value = "主键", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "biz_type", nullable = false, length = 50)
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    @Column(name = "text_content", columnDefinition = "text")
    @ApiModelProperty(value = "音频对应的文字内容")
    private String textContent;

    @NotBlank
    @Column(name = "source_type", nullable = false, length = 50)
    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    @NotBlank
    @Column(name = "file_url", nullable = false, length = 500)
    @ApiModelProperty(value = "音频文件地址")
    private String fileUrl;

    @Column(name = "file_format", length = 20)
    @ApiModelProperty(value = "文件格式")
    private String fileFormat = "mp3";

    @Column(name = "file_size")
    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;

    @Column(name = "tts_record_id")
    @ApiModelProperty(value = "关联的TTS记录ID")
    private Long ttsRecordId;

    @Column(name = "create_time", updatable = false)
    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @Column(name = "update_time")
    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (this.createTime == null) {
            this.createTime = now;
        }
        if (this.updateTime == null) {
            this.updateTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = new Timestamp(System.currentTimeMillis());
    }
}
```

- [ ] **Step 7: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabWord.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabSense.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabStructure.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabExample.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/VocabExercise.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/domain/AudioResource.java
git commit -m "feat: add vocabulary domain entities"
```

---

## Task 2: 创建 Repository 接口

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabWordRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabSenseRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabStructureRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabExampleRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabExerciseRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/AudioResourceRepository.java`

- [ ] **Step 1: 创建 VocabWordRepository.java**

```java
package com.naon.grid.modules.vocabulary.repository;

import com.naon.grid.modules.vocabulary.domain.VocabWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VocabWordRepository extends JpaRepository<VocabWord, Integer>, JpaSpecificationExecutor<VocabWord> {
}
```

- [ ] **Step 2: 创建 VocabSenseRepository.java**

```java
package com.naon.grid.modules.vocabulary.repository;

import com.naon.grid.modules.vocabulary.domain.VocabSense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabSenseRepository extends JpaRepository<VocabSense, Integer>, JpaSpecificationExecutor<VocabSense> {
    List<VocabSense> findByWordId(Integer wordId);
}
```

- [ ] **Step 3: 创建 VocabStructureRepository.java**

```java
package com.naon.grid.modules.vocabulary.repository;

import com.naon.grid.modules.vocabulary.domain.VocabStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabStructureRepository extends JpaRepository<VocabStructure, Integer>, JpaSpecificationExecutor<VocabStructure> {
    List<VocabStructure> findBySenseId(Integer senseId);
    List<VocabStructure> findByWordId(Integer wordId);
}
```

- [ ] **Step 4: 创建 VocabExampleRepository.java**

```java
package com.naon.grid.modules.vocabulary.repository;

import com.naon.grid.modules.vocabulary.domain.VocabExample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabExampleRepository extends JpaRepository<VocabExample, Integer>, JpaSpecificationExecutor<VocabExample> {
    List<VocabExample> findByStructureId(Integer structureId);
    List<VocabExample> findBySenseId(Integer senseId);
    List<VocabExample> findByWordId(Integer wordId);
}
```

- [ ] **Step 5: 创建 VocabExerciseRepository.java**

```java
package com.naon.grid.modules.vocabulary.repository;

import com.naon.grid.modules.vocabulary.domain.VocabExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VocabExerciseRepository extends JpaRepository<VocabExercise, Integer>, JpaSpecificationExecutor<VocabExercise> {
    List<VocabExercise> findByWordId(Integer wordId);
}
```

- [ ] **Step 6: 创建 AudioResourceRepository.java**

```java
package com.naon.grid.modules.vocabulary.repository;

import com.naon.grid.modules.vocabulary.domain.AudioResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AudioResourceRepository extends JpaRepository<AudioResource, Long>, JpaSpecificationExecutor<AudioResource> {
}
```

- [ ] **Step 7: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabWordRepository.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabSenseRepository.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabStructureRepository.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabExampleRepository.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/VocabExerciseRepository.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/repository/AudioResourceRepository.java
git commit -m "feat: add vocabulary repositories"
```

---

## Task 3: 创建 DTO 类

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabWordDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabWordQueryCriteria.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabSenseDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabStructureDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabExampleDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabExerciseDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/AudioResourceDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/AudioResourceQueryCriteria.java`

- [ ] **Step 1: 创建 VocabWordDto.java**

```java
package com.naon.grid.modules.vocabulary.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Data
public class VocabWordDto implements Serializable {

    @ApiModelProperty(value = "词汇唯一ID")
    private Integer id;

    @ApiModelProperty(value = "词汇")
    private String word;

    @ApiModelProperty(value = "繁体词汇")
    private String wordTraditional;

    @ApiModelProperty(value = "标准拼音（含声调）")
    private String pinyin;

    @ApiModelProperty(value = "词汇读音音频资源ID")
    private Long audioId;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "义项列表")
    private List<VocabSenseDto> senses;
}
```

- [ ] **Step 2: 创建 VocabWordQueryCriteria.java**

```java
package com.naon.grid.modules.vocabulary.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.naon.grid.annotation.Query;
import java.io.Serializable;

@Data
public class VocabWordQueryCriteria implements Serializable {

    @ApiModelProperty(value = "词汇模糊查询")
    @Query(blurry = "word")
    private String blurry;
}
```

- [ ] **Step 3: 创建 VocabSenseDto.java**

```java
package com.naon.grid.modules.vocabulary.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Data
public class VocabSenseDto implements Serializable {

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
    private String translations;

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

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "搭配列表")
    private List<VocabStructureDto> structures;

    @ApiModelProperty(value = "练习题列表")
    private List<VocabExerciseDto> exercises;
}
```

- [ ] **Step 4: 创建 VocabStructureDto.java**

```java
package com.naon.grid.modules.vocabulary.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Data
public class VocabStructureDto implements Serializable {

    @ApiModelProperty(value = "自增ID, 结构搭配ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "所属义项ID")
    private Integer senseId;

    @ApiModelProperty(value = "结构搭配文案")
    private String pattern;

    @ApiModelProperty(value = "搭配排序权重")
    private Integer structureOrder;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "例句列表")
    private List<VocabExampleDto> examples;
}
```

- [ ] **Step 5: 创建 VocabExampleDto.java**

```java
package com.naon.grid.modules.vocabulary.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class VocabExampleDto implements Serializable {

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
    private String translations;

    @ApiModelProperty(value = "例句排序权重")
    private Integer exampleOrder;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

- [ ] **Step 6: 创建 VocabExerciseDto.java**

```java
package com.naon.grid.modules.vocabulary.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class VocabExerciseDto implements Serializable {

    @ApiModelProperty(value = "练习题目唯一ID")
    private Integer id;

    @ApiModelProperty(value = "所属词汇ID")
    private Integer wordId;

    @ApiModelProperty(value = "题目类型")
    private String questionType;

    @ApiModelProperty(value = "练习题干描述")
    private String questionText;

    @ApiModelProperty(value = "选项列表")
    private String options;

    @ApiModelProperty(value = "答案列表")
    private String answers;

    @ApiModelProperty(value = "练习题目排序权重")
    private Integer exerciseOrder;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

- [ ] **Step 7: 创建 AudioResourceDto.java**

```java
package com.naon.grid.modules.vocabulary.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class AudioResourceDto implements Serializable {

    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "业务类型")
    private String bizType;

    @ApiModelProperty(value = "音频对应的文字内容")
    private String textContent;

    @ApiModelProperty(value = "来源类型")
    private String sourceType;

    @ApiModelProperty(value = "音频文件地址")
    private String fileUrl;

    @ApiModelProperty(value = "文件格式")
    private String fileFormat;

    @ApiModelProperty(value = "文件大小(字节)")
    private Long fileSize;

    @ApiModelProperty(value = "关联的TTS记录ID")
    private Long ttsRecordId;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

- [ ] **Step 8: 创建 AudioResourceQueryCriteria.java**

```java
package com.naon.grid.modules.vocabulary.service.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.naon.grid.annotation.Query;
import java.io.Serializable;

@Data
public class AudioResourceQueryCriteria implements Serializable {

    @ApiModelProperty(value = "业务类型")
    @Query
    private String bizType;
}
```

- [ ] **Step 9: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabWordDto.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabWordQueryCriteria.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabSenseDto.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabStructureDto.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabExampleDto.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/VocabExerciseDto.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/AudioResourceDto.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/dto/AudioResourceQueryCriteria.java
git commit -m "feat: add vocabulary DTOs"
```

---

## Task 4: 创建 MapStruct 映射接口

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/mapstruct/VocabWordMapper.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/mapstruct/AudioResourceMapper.java`

- [ ] **Step 1: 创建 VocabWordMapper.java**

```java
package com.naon.grid.modules.vocabulary.service.mapstruct;

import com.naon.grid.base.BaseMapper;
import com.naon.grid.modules.vocabulary.domain.VocabWord;
import com.naon.grid.modules.vocabulary.service.dto.VocabWordDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VocabWordMapper extends BaseMapper<VocabWordDto, VocabWord> {
}
```

- [ ] **Step 2: 创建 AudioResourceMapper.java**

```java
package com.naon.grid.modules.vocabulary.service.mapstruct;

import com.naon.grid.base.BaseMapper;
import com.naon.grid.modules.vocabulary.domain.AudioResource;
import com.naon.grid.modules.vocabulary.service.dto.AudioResourceDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AudioResourceMapper extends BaseMapper<AudioResourceDto, AudioResource> {
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/mapstruct/VocabWordMapper.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/mapstruct/AudioResourceMapper.java
git commit -m "feat: add vocabulary mappers"
```

---

## Task 5: 创建音频资源 Service 和 Controller

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/AudioResourceService.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/impl/AudioResourceServiceImpl.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/rest/AudioResourceController.java`

- [ ] **Step 1: 创建 AudioResourceService.java**

```java
package com.naon.grid.modules.vocabulary.service;

import com.naon.grid.utils.PageResult;
import com.naon.grid.modules.vocabulary.service.dto.AudioResourceDto;
import com.naon.grid.modules.vocabulary.service.dto.AudioResourceQueryCriteria;
import org.springframework.data.domain.Pageable;

public interface AudioResourceService {

    PageResult<AudioResourceDto> queryAll(AudioResourceQueryCriteria criteria, Pageable pageable);

    AudioResourceDto findById(Long id);
}
```

- [ ] **Step 2: 创建 AudioResourceServiceImpl.java**

```java
package com.naon.grid.modules.vocabulary.service.impl;

import lombok.RequiredArgsConstructor;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.modules.vocabulary.domain.AudioResource;
import com.naon.grid.modules.vocabulary.repository.AudioResourceRepository;
import com.naon.grid.modules.vocabulary.service.AudioResourceService;
import com.naon.grid.modules.vocabulary.service.dto.AudioResourceDto;
import com.naon.grid.modules.vocabulary.service.dto.AudioResourceQueryCriteria;
import com.naon.grid.modules.vocabulary.service.mapstruct.AudioResourceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AudioResourceServiceImpl implements AudioResourceService {

    private final AudioResourceRepository audioResourceRepository;
    private final AudioResourceMapper audioResourceMapper;

    @Override
    public PageResult<AudioResourceDto> queryAll(AudioResourceQueryCriteria criteria, Pageable pageable) {
        Page<AudioResource> page = audioResourceRepository.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder), pageable);
        return PageUtil.toPage(page.map(audioResourceMapper::toDto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AudioResourceDto findById(Long id) {
        AudioResource audioResource = audioResourceRepository.findById(id).orElseGet(AudioResource::new);
        if (audioResource.getId() == null) {
            throw new EntityNotFoundException(AudioResource.class, "id", id);
        }
        return audioResourceMapper.toDto(audioResource);
    }
}
```

- [ ] **Step 3: 创建 AudioResourceController.java**

```java
package com.naon.grid.modules.vocabulary.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import com.naon.grid.annotation.Log;
import com.naon.grid.utils.PageResult;
import com.naon.grid.modules.vocabulary.service.AudioResourceService;
import com.naon.grid.modules.vocabulary.service.dto.AudioResourceDto;
import com.naon.grid.modules.vocabulary.service.dto.AudioResourceQueryCriteria;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Api(tags = "词汇：音频资源管理")
@RequestMapping("/api/audio-resource")
public class AudioResourceController {

    private final AudioResourceService audioResourceService;

    @Log("查询音频资源列表")
    @ApiOperation("分页查询音频资源列表")
    @GetMapping
    public ResponseEntity<PageResult<AudioResourceDto>> queryAll(AudioResourceQueryCriteria criteria, Pageable pageable) {
        return new ResponseEntity<>(audioResourceService.queryAll(criteria, pageable), HttpStatus.OK);
    }

    @Log("查询音频资源详情")
    @ApiOperation("根据ID查询音频资源详情")
    @GetMapping("/{id}")
    public ResponseEntity<AudioResourceDto> findById(@PathVariable Long id) {
        return new ResponseEntity<>(audioResourceService.findById(id), HttpStatus.OK);
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/AudioResourceService.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/impl/AudioResourceServiceImpl.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/rest/AudioResourceController.java
git commit -m "feat: add audio resource service and controller"
```

---

## Task 6: 创建词汇 Service 接口和实现

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/VocabWordService.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/impl/VocabWordServiceImpl.java`

- [ ] **Step 1: 创建 VocabWordService.java**

```java
package com.naon.grid.modules.vocabulary.service;

import com.naon.grid.utils.PageResult;
import com.naon.grid.modules.vocabulary.domain.VocabWord;
import com.naon.grid.modules.vocabulary.service.dto.VocabWordDto;
import com.naon.grid.modules.vocabulary.service.dto.VocabWordQueryCriteria;
import org.springframework.data.domain.Pageable;

public interface VocabWordService {

    PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable);

    VocabWordDto findById(Integer id);

    Integer create(VocabWordDto resources);
}
```

- [ ] **Step 2: 创建 VocabWordServiceImpl.java**

```java
package com.naon.grid.modules.vocabulary.service.impl;

import lombok.RequiredArgsConstructor;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.modules.vocabulary.service.VocabWordService;
import com.naon.grid.modules.vocabulary.service.mapstruct.VocabWordMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class VocabWordServiceImpl implements VocabWordService {

    private final VocabWordRepository vocabWordRepository;
    private final VocabSenseRepository vocabSenseRepository;
    private final VocabStructureRepository vocabStructureRepository;
    private final VocabExampleRepository vocabExampleRepository;
    private final VocabExerciseRepository vocabExerciseRepository;
    private final VocabWordMapper vocabWordMapper;

    @Override
    public PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable) {
        Page<VocabWord> page = vocabWordRepository.findAll((root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder), pageable);
        return PageUtil.toPage(page.map(vocabWordMapper::toDto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VocabWordDto findById(Integer id) {
        VocabWord vocabWord = vocabWordRepository.findById(id).orElseGet(VocabWord::new);
        if (vocabWord.getId() == null) {
            throw new EntityNotFoundException(VocabWord.class, "id", id);
        }
        VocabWordDto vocabWordDto = vocabWordMapper.toDto(vocabWord);

        List<VocabSenseDto> senseDtos = new ArrayList<>();
        List<VocabSense> senses = vocabSenseRepository.findByWordId(id);
        for (VocabSense sense : senses) {
            VocabSenseDto senseDto = convertToSenseDto(sense);
            senseDtos.add(senseDto);
        }
        vocabWordDto.setSenses(senseDtos);

        return vocabWordDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer create(VocabWordDto resources) {
        VocabWord vocabWord = vocabWordMapper.toEntity(resources);
        vocabWord = vocabWordRepository.save(vocabWord);

        if (resources.getSenses() != null) {
            for (VocabSenseDto senseDto : resources.getSenses()) {
                VocabSense sense = convertToSenseEntity(senseDto, vocabWord.getId());
                sense = vocabSenseRepository.save(sense);

                if (senseDto.getStructures() != null) {
                    for (VocabStructureDto structureDto : senseDto.getStructures()) {
                        VocabStructure structure = convertToStructureEntity(structureDto, vocabWord.getId(), sense.getId());
                        structure = vocabStructureRepository.save(structure);

                        if (structureDto.getExamples() != null) {
                            for (VocabExampleDto exampleDto : structureDto.getExamples()) {
                                VocabExample example = convertToExampleEntity(exampleDto, vocabWord.getId(), sense.getId(), structure.getId());
                                vocabExampleRepository.save(example);
                            }
                        }
                    }
                }

                if (senseDto.getExercises() != null) {
                    for (VocabExerciseDto exerciseDto : senseDto.getExercises()) {
                        VocabExercise exercise = convertToExerciseEntity(exerciseDto, vocabWord.getId());
                        vocabExerciseRepository.save(exercise);
                    }
                }
            }
        }

        return vocabWord.getId();
    }

    private VocabSenseDto convertToSenseDto(VocabSense sense) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(sense.getId());
        dto.setWordId(sense.getWordId());
        dto.setPartOfSpeech(sense.getPartOfSpeech());
        dto.setChineseDef(sense.getChineseDef());
        dto.setDefAudioId(sense.getDefAudioId());
        dto.setTranslations(sense.getTranslations());
        dto.setSynonyms(sense.getSynonyms());
        dto.setAntonyms(sense.getAntonyms());
        dto.setRelatedForward(sense.getRelatedForward());
        dto.setRelatedBackward(sense.getRelatedBackward());
        dto.setSenseOrder(sense.getSenseOrder());
        dto.setCreateTime(sense.getCreateTime());
        dto.setUpdateTime(sense.getUpdateTime());

        List<VocabStructureDto> structureDtos = new ArrayList<>();
        List<VocabStructure> structures = vocabStructureRepository.findBySenseId(sense.getId());
        for (VocabStructure structure : structures) {
            VocabStructureDto structureDto = convertToStructureDto(structure);
            structureDtos.add(structureDto);
        }
        dto.setStructures(structureDtos);

        List<VocabExerciseDto> exerciseDtos = new ArrayList<>();
        List<VocabExercise> exercises = vocabExerciseRepository.findByWordId(sense.getWordId());
        for (VocabExercise exercise : exercises) {
            VocabExerciseDto exerciseDto = convertToExerciseDto(exercise);
            exerciseDtos.add(exerciseDto);
        }
        dto.setExercises(exerciseDtos);

        return dto;
    }

    private VocabStructureDto convertToStructureDto(VocabStructure structure) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(structure.getId());
        dto.setWordId(structure.getWordId());
        dto.setSenseId(structure.getSenseId());
        dto.setPattern(structure.getPattern());
        dto.setStructureOrder(structure.getStructureOrder());
        dto.setCreateTime(structure.getCreateTime());
        dto.setUpdateTime(structure.getUpdateTime());

        List<VocabExampleDto> exampleDtos = new ArrayList<>();
        List<VocabExample> examples = vocabExampleRepository.findByStructureId(structure.getId());
        for (VocabExample example : examples) {
            VocabExampleDto exampleDto = convertToExampleDto(example);
            exampleDtos.add(exampleDto);
        }
        dto.setExamples(exampleDtos);

        return dto;
    }

    private VocabExampleDto convertToExampleDto(VocabExample example) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(example.getId());
        dto.setWordId(example.getWordId());
        dto.setSenseId(example.getSenseId());
        dto.setStructureId(example.getStructureId());
        dto.setSentence(example.getSentence());
        dto.setAudioId(example.getAudioId());
        dto.setPinyin(example.getPinyin());
        dto.setTranslations(example.getTranslations());
        dto.setExampleOrder(example.getExampleOrder());
        dto.setCreateTime(example.getCreateTime());
        dto.setUpdateTime(example.getUpdateTime());
        return dto;
    }

    private VocabExerciseDto convertToExerciseDto(VocabExercise exercise) {
        VocabExerciseDto dto = new VocabExerciseDto();
        dto.setId(exercise.getId());
        dto.setWordId(exercise.getWordId());
        dto.setQuestionType(exercise.getQuestionType());
        dto.setQuestionText(exercise.getQuestionText());
        dto.setOptions(exercise.getOptions());
        dto.setAnswers(exercise.getAnswers());
        dto.setExerciseOrder(exercise.getExerciseOrder());
        dto.setCreateTime(exercise.getCreateTime());
        dto.setUpdateTime(exercise.getUpdateTime());
        return dto;
    }

    private VocabSense convertToSenseEntity(VocabSenseDto dto, Integer wordId) {
        VocabSense entity = new VocabSense();
        entity.setWordId(wordId);
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setChineseDef(dto.getChineseDef());
        entity.setDefAudioId(dto.getDefAudioId());
        entity.setTranslations(dto.getTranslations());
        entity.setSynonyms(dto.getSynonyms());
        entity.setAntonyms(dto.getAntonyms());
        entity.setRelatedForward(dto.getRelatedForward());
        entity.setRelatedBackward(dto.getRelatedBackward());
        entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
        return entity;
    }

    private VocabStructure convertToStructureEntity(VocabStructureDto dto, Integer wordId, Integer senseId) {
        VocabStructure entity = new VocabStructure();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setPattern(dto.getPattern());
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
        return entity;
    }

    private VocabExample convertToExampleEntity(VocabExampleDto dto, Integer wordId, Integer senseId, Integer structureId) {
        VocabExample entity = new VocabExample();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setStructureId(structureId);
        entity.setSentence(dto.getSentence());
        entity.setAudioId(dto.getAudioId());
        entity.setPinyin(dto.getPinyin());
        entity.setTranslations(dto.getTranslations());
        entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
        return entity;
    }

    private VocabExercise convertToExerciseEntity(VocabExerciseDto dto, Integer wordId) {
        VocabExercise entity = new VocabExercise();
        entity.setWordId(wordId);
        entity.setQuestionType(dto.getQuestionType());
        entity.setQuestionText(dto.getQuestionText());
        entity.setOptions(dto.getOptions());
        entity.setAnswers(dto.getAnswers());
        entity.setExerciseOrder(dto.getExerciseOrder() != null ? dto.getExerciseOrder() : 0);
        return entity;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/VocabWordService.java
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/service/impl/VocabWordServiceImpl.java
git commit -m "feat: add vocabulary word service"
```

---

## Task 7: 创建词汇 Controller

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/vocabulary/rest/VocabWordController.java`

- [ ] **Step 1: 创建 VocabWordController.java**

```java
package com.naon.grid.modules.vocabulary.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import com.naon.grid.annotation.Log;
import com.naon.grid.utils.PageResult;
import com.naon.grid.modules.vocabulary.service.VocabWordService;
import com.naon.grid.modules.vocabulary.service.dto.VocabWordDto;
import com.naon.grid.modules.vocabulary.service.dto.VocabWordQueryCriteria;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Api(tags = "词汇：词汇管理")
@RequestMapping("/api/vocabulary")
public class VocabWordController {

    private final VocabWordService vocabWordService;

    @Log("查询词汇列表")
    @ApiOperation("分页查询词汇列表")
    @GetMapping
    public ResponseEntity<PageResult<VocabWordDto>> queryAll(VocabWordQueryCriteria criteria, Pageable pageable) {
        return new ResponseEntity<>(vocabWordService.queryAll(criteria, pageable), HttpStatus.OK);
    }

    @Log("查询词汇详情")
    @ApiOperation("根据ID查询词汇详情")
    @GetMapping("/{id}")
    public ResponseEntity<VocabWordDto> findById(@PathVariable Integer id) {
        return new ResponseEntity<>(vocabWordService.findById(id), HttpStatus.OK);
    }

    @Log("新增词汇")
    @ApiOperation("新增词汇")
    @PostMapping
    public ResponseEntity<Object> create(@RequestBody VocabWordDto resources) {
        Integer id = vocabWordService.create(resources);
        Map<String, Integer> result = new HashMap<>();
        result.put("id", id);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/vocabulary/rest/VocabWordController.java
git commit -m "feat: add vocabulary word controller"
```

---

## Task 8: 编译和验证

**Files:**

- [ ] **Step 1: 编译项目**

```bash
mvn clean compile -DskipTests
```

Expected: 编译成功，无错误

- [ ] **Step 2: 验证代码结构**

Check: 所有文件都已创建，包结构正确

---

## 计划自检

**1. Spec coverage:** 检查设计规范中的需求：
- ✅ 数据库表结构 - Task 1
- ✅ 词汇管理接口 - Task 2-7
- ✅ 音频资源管理接口 - Task 5
- ✅ 分页查询（默认30条） - Task 6-7
- ✅ 模糊查询 - Task 3-7
- ✅ 完整层级返回 - Task 6
- ✅ 无需鉴权 - Task 5-7（Controller 未使用 @PreAuthorize）

**2. Placeholder scan:** 检查计划：
- ✅ 无 TBD/TODO
- ✅ 所有代码都已完整给出
- ✅ 所有步骤都清晰

**3. Type consistency:** 检查类型一致性：
- ✅ Entity 和 DTO 的字段名一致
- ✅ Repository 方法名一致
- ✅ Service 方法签名一致
