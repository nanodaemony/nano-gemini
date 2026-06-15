# Grammar Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build admin CRUD backend for Chinese grammar point management with draft/review/publish workflow, matching the existing character management pattern.

**Architecture:** Add a new `grammar` package in domain/repo/service layers under `grid-system`. Grammar point data uses `draft_content` JSON for un-published edits and writes to real DB rows on publish. Example sentences reuse the shared `ExampleSentenceService`. All sub-tables (meaning, structure, notice, error) follow the same sync pattern as `CharComparison`/`CharWord`.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Spring Data JPA, MapStruct, Lombok, Swagger annotations, Fastjson2, Maven.

---

## File Structure

All files under `grid-system/src/main/java/com/naon/grid/backend/`:

```
domain/grammar/
├── GrammarPoint.java          主表 entity (extends BaseEntity)
├── GrammarMeaning.java        子表：语法意义
├── GrammarStructure.java      子表：语法结构
├── GrammarNotice.java         子表：语法注意
└── GrammarError.java          子表：语法偏误

repo/grammar/
├── GrammarPointRepository.java      主表 repo (Jpa + Spec)
├── GrammarMeaningRepository.java
├── GrammarStructureRepository.java
├── GrammarNoticeRepository.java
└── GrammarErrorRepository.java

service/grammar/
├── GrammarPointService.java         接口
├── dto/
│   ├── GrammarPointDto.java         聚合 DTO
│   ├── GrammarPointQueryCriteria.java
│   ├── GrammarMeaningDto.java
│   ├── GrammarStructureDto.java
│   ├── GrammarNoticeDto.java
│   └── GrammarErrorDto.java
├── mapstruct/
│   └── GrammarPointMapper.java      MapStruct mapper
└── impl/
    └── GrammarPointServiceImpl.java 核心实现

rest/
├── controller/
│   └── GrammarPointController.java
├── request/
│   ├── GrammarPointCreateRequest.java
│   └── GrammarPointQueryRequest.java
├── vo/
│   ├── GrammarPointCreateVO.java
│   ├── GrammarPointVO.java
│   └── GrammarPointBaseVO.java
└── wrapper/
    └── GrammarPointWrapper.java
```

**Total: 25 new files**

---

### Task 1: Domain Entities and Repositories

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/grammar/GrammarPoint.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/grammar/GrammarMeaning.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/grammar/GrammarStructure.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/grammar/GrammarNotice.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/grammar/GrammarError.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/grammar/GrammarPointRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/grammar/GrammarMeaningRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/grammar/GrammarStructureRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/grammar/GrammarNoticeRepository.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/grammar/GrammarErrorRepository.java`

- [ ] **Step 1: Create GrammarPoint.java**

```java
package com.naon.grid.backend.domain.grammar;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "grammar_point")
public class GrammarPoint extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "语法点ID", hidden = true)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "hsk_level", length = 20)
    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @Column(name = "project", length = 20)
    @ApiModelProperty(value = "项目")
    private String project;

    @Column(name = "category", length = 50)
    @ApiModelProperty(value = "类别")
    private String category;

    @Column(name = "sub_category", length = 50)
    @ApiModelProperty(value = "细目")
    private String subCategory;

    @Column(name = "draft_content", columnDefinition = "text")
    @ApiModelProperty(value = "草稿内容JSON（包含主表和子表）")
    private String draftContent;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();

    @Column(name = "publish_status", length = 20)
    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();

    @Column(name = "edit_status", length = 20)
    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus = EditStatusEnum.DRAFT.getCode();
}
```

- [ ] **Step 2: Create GrammarMeaning.java**

```java
package com.naon.grid.backend.domain.grammar;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "grammar_meaning")
public class GrammarMeaning implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "语法意义ID", hidden = true)
    private Long id;

    @NotNull
    @Column(name = "grammar_id", nullable = false)
    private Long grammarId;

    @NotBlank
    @Column(name = "meaning_content", nullable = false, length = 2048)
    private String meaningContent;

    @Column(name = "meaning_content_translations", columnDefinition = "text")
    private String meaningContentTranslations;

    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "meaning_sentence_ids", length = 128)
    private String meaningSentenceIds;

    @NotNull
    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer meaningOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 3: Create GrammarStructure.java**

```java
package com.naon.grid.backend.domain.grammar;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "grammar_structure")
public class GrammarStructure implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "语法结构ID", hidden = true)
    private Long id;

    @NotNull
    @Column(name = "grammar_id", nullable = false)
    private Long grammarId;

    @NotBlank
    @Column(name = "structure_content", nullable = false, length = 1024)
    private String structureContent;

    @Column(name = "structure_sentence_ids", length = 128)
    private String structureSentenceIds;

    @NotNull
    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer structureOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 4: Create GrammarNotice.java**

```java
package com.naon.grid.backend.domain.grammar;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "grammar_notice")
public class GrammarNotice implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "语法注意ID", hidden = true)
    private Long id;

    @NotNull
    @Column(name = "grammar_id", nullable = false)
    private Long grammarId;

    @NotBlank
    @Column(name = "notice_content", nullable = false, length = 1024)
    private String noticeContent;

    @Column(name = "notice_content_translations", columnDefinition = "text")
    private String noticeContentTranslations;

    @Column(name = "notice_sentence_ids", length = 128)
    private String noticeSentenceIds;

    @NotNull
    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer noticeOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 5: Create GrammarError.java**

```java
package com.naon.grid.backend.domain.grammar;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "grammar_error")
public class GrammarError implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "语法偏误ID", hidden = true)
    private Long id;

    @NotNull
    @Column(name = "grammar_id", nullable = false)
    private Long grammarId;

    @NotBlank
    @Column(name = "error_content", nullable = false, length = 1024)
    private String errorContent;

    @Column(name = "error_analysis", length = 1024)
    private String errorAnalysis;

    @Column(name = "error_analysis_translations", columnDefinition = "text")
    private String errorAnalysisTranslations;

    @NotNull
    @Column(name = "`order`", nullable = false)
    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer errorOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @Column(name = "status")
    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

- [ ] **Step 6: Create GrammarPointRepository.java**

```java
package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GrammarPointRepository extends JpaRepository<GrammarPoint, Long>, JpaSpecificationExecutor<GrammarPoint> {
}
```

- [ ] **Step 7: Create GrammarMeaningRepository.java**

```java
package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarMeaning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GrammarMeaningRepository extends JpaRepository<GrammarMeaning, Long> {

    List<GrammarMeaning> findByGrammarIdAndStatus(Long grammarId, Integer status);

    @Query("SELECT m.grammarId, COUNT(m) FROM GrammarMeaning m WHERE m.grammarId IN :grammarIds AND m.status = :status GROUP BY m.grammarId")
    List<Object[]> countByGrammarIdInGroupByGrammarId(@Param("grammarIds") Collection<Long> grammarIds, @Param("status") Integer status);
}
```

- [ ] **Step 8: Create GrammarStructureRepository.java**

```java
package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GrammarStructureRepository extends JpaRepository<GrammarStructure, Long> {

    List<GrammarStructure> findByGrammarIdAndStatus(Long grammarId, Integer status);

    @Query("SELECT s.grammarId, COUNT(s) FROM GrammarStructure s WHERE s.grammarId IN :grammarIds AND s.status = :status GROUP BY s.grammarId")
    List<Object[]> countByGrammarIdInGroupByGrammarId(@Param("grammarIds") Collection<Long> grammarIds, @Param("status") Integer status);
}
```

- [ ] **Step 9: Create GrammarNoticeRepository.java**

```java
package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GrammarNoticeRepository extends JpaRepository<GrammarNotice, Long> {

    List<GrammarNotice> findByGrammarIdAndStatus(Long grammarId, Integer status);

    @Query("SELECT n.grammarId, COUNT(n) FROM GrammarNotice n WHERE n.grammarId IN :grammarIds AND n.status = :status GROUP BY n.grammarId")
    List<Object[]> countByGrammarIdInGroupByGrammarId(@Param("grammarIds") Collection<Long> grammarIds, @Param("status") Integer status);
}
```

- [ ] **Step 10: Create GrammarErrorRepository.java**

```java
package com.naon.grid.backend.repo.grammar;

import com.naon.grid.backend.domain.grammar.GrammarError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface GrammarErrorRepository extends JpaRepository<GrammarError, Long> {

    List<GrammarError> findByGrammarIdAndStatus(Long grammarId, Integer status);

    @Query("SELECT e.grammarId, COUNT(e) FROM GrammarError e WHERE e.grammarId IN :grammarIds AND e.status = :status GROUP BY e.grammarId")
    List<Object[]> countByGrammarIdInGroupByGrammarId(@Param("grammarIds") Collection<Long> grammarIds, @Param("status") Integer status);
}
```

- [ ] **Step 11: Verify compilation**

Run:
```bash
cd grid-bootstrap
mvn compile -DskipTests
```
Expected: BUILD SUCCESS (no errors)

- [ ] **Step 12: Commit Task 1**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/grammar/
git add grid-system/src/main/java/com/naon/grid/backend/repo/grammar/
git commit -m "feat: add grammar domain entities and repositories"
```

---

### Task 2: Create DTOs

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarPointDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarPointQueryCriteria.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarMeaningDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarStructureDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarNoticeDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/GrammarErrorDto.java`

- [ ] **Step 1: Create GrammarPointDto.java**

```java
package com.naon.grid.backend.service.grammar.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class GrammarPointDto extends BaseDTO implements Serializable {

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
    private List<GrammarMeaningDto> meanings;

    @ApiModelProperty(value = "语法结构列表")
    private List<GrammarStructureDto> structures;

    @ApiModelProperty(value = "语法注意列表")
    private List<GrammarNoticeDto> notices;

    @ApiModelProperty(value = "语法偏误列表")
    private List<GrammarErrorDto> errors;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    // ===== 列表统计字段 =====
    @ApiModelProperty(value = "意义个数")
    private Integer meaningCount;

    @ApiModelProperty(value = "结构个数")
    private Integer structureCount;

    @ApiModelProperty(value = "注意个数")
    private Integer noticeCount;

    @ApiModelProperty(value = "偏误个数")
    private Integer errorCount;
}
```

- [ ] **Step 2: Create GrammarPointQueryCriteria.java**

```java
package com.naon.grid.backend.service.grammar.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class GrammarPointQueryCriteria implements Serializable {

    @ApiModelProperty(value = "语法点名称模糊查询")
    @Query(blurry = "name")
    private String blurry;

    @ApiModelProperty(value = "发布状态")
    @Query
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    @Query
    private String editStatus;

    @ApiModelProperty(value = "HSK等级")
    @Query
    private String hskLevel;

    @ApiModelProperty(value = "类别")
    @Query
    private String category;
}
```

- [ ] **Step 3: Create GrammarMeaningDto.java**

```java
package com.naon.grid.backend.service.grammar.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class GrammarMeaningDto implements Serializable {

    @ApiModelProperty(value = "语法意义ID")
    private Long id;

    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @ApiModelProperty(value = "语法意义内容")
    private String meaningContent;

    @ApiModelProperty(value = "语法意义外文翻译")
    private List<TextTranslation> meaningContentTranslations;

    @ApiModelProperty(value = "语法意义图片ID")
    private Long imageId;

    @ApiModelProperty(value = "例句列表")
    private List<ExampleSentenceDto> sentences;

    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer order;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "有效状态")
    private Integer status;
}
```

- [ ] **Step 4: Create GrammarStructureDto.java**

```java
package com.naon.grid.backend.service.grammar.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class GrammarStructureDto implements Serializable {

    @ApiModelProperty(value = "语法结构ID")
    private Long id;

    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @ApiModelProperty(value = "结构文本")
    private String structureContent;

    @ApiModelProperty(value = "例句列表")
    private List<ExampleSentenceDto> sentences;

    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer order;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "有效状态")
    private Integer status;
}
```

- [ ] **Step 5: Create GrammarNoticeDto.java**

```java
package com.naon.grid.backend.service.grammar.dto;

import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class GrammarNoticeDto implements Serializable {

    @ApiModelProperty(value = "语法注意ID")
    private Long id;

    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @ApiModelProperty(value = "注意内容")
    private String noticeContent;

    @ApiModelProperty(value = "注意内容外文翻译")
    private List<TextTranslation> noticeContentTranslations;

    @ApiModelProperty(value = "例句列表")
    private List<ExampleSentenceDto> sentences;

    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer order;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "有效状态")
    private Integer status;
}
```

- [ ] **Step 6: Create GrammarErrorDto.java**

```java
package com.naon.grid.backend.service.grammar.dto;

import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class GrammarErrorDto implements Serializable {

    @ApiModelProperty(value = "语法偏误ID")
    private Long id;

    @ApiModelProperty(value = "语法点ID")
    private Long grammarId;

    @ApiModelProperty(value = "偏误描述")
    private String errorContent;

    @ApiModelProperty(value = "偏误分析")
    private String errorAnalysis;

    @ApiModelProperty(value = "偏误分析外文翻译")
    private List<TextTranslation> errorAnalysisTranslations;

    @ApiModelProperty(value = "排序权重（值大的排前面）")
    private Integer order;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "有效状态")
    private Integer status;
}
```

- [ ] **Step 7: Verify compilation**

Run:
```bash
cd grid-bootstrap
mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit Task 2**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/grammar/dto/
git commit -m "feat: add grammar DTOs"
```

---

### Task 3: Create Mapper and Service Interface

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/mapstruct/GrammarPointMapper.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/GrammarPointService.java`

- [ ] **Step 1: Create GrammarPointMapper.java**

```java
package com.naon.grid.backend.service.grammar.mapstruct;

import com.naon.grid.backend.domain.grammar.GrammarPoint;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.base.BaseMapper;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.utils.JsonUtils;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GrammarPointMapper extends BaseMapper<GrammarPointDto, GrammarPoint> {

    default String toTranslationJson(List<TextTranslation> list) {
        return JsonUtils.toTranslationJson(list);
    }

    default List<TextTranslation> toTranslationList(String json) {
        return JsonUtils.parseTranslationList(json);
    }
}
```

- [ ] **Step 2: Create GrammarPointService.java**

```java
package com.naon.grid.backend.service.grammar;

import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface GrammarPointService {

    PageResult<GrammarPointDto> queryAll(GrammarPointQueryCriteria criteria, Pageable pageable);

    GrammarPointDto findById(Long id);

    Long create(GrammarPointDto resources);

    void update(Long id, GrammarPointDto resources);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);
}
```

- [ ] **Step 3: Verify compilation**

Run:
```bash
cd grid-bootstrap
mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit Task 3**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/grammar/mapstruct/
git add grid-system/src/main/java/com/naon/grid/backend/service/grammar/GrammarPointService.java
git commit -m "feat: add grammar mapper and service interface"
```

---

### Task 4: Create Request, VO, and Wrapper Classes

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/GrammarPointCreateRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/request/GrammarPointQueryRequest.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarPointCreateVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarPointVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/GrammarPointBaseVO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/GrammarPointWrapper.java`

- [ ] **Step 1: Create GrammarPointCreateRequest.java**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class GrammarPointCreateRequest implements Serializable {

    @NotBlank
    @ApiModelProperty(value = "语法点名称", required = true)
    private String name;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "项目")
    private String project;

    @ApiModelProperty(value = "类别")
    private String category;

    @ApiModelProperty(value = "细目")
    private String subCategory;

    @Valid
    @ApiModelProperty(value = "语法意义列表")
    private List<GrammarMeaningRequest> meanings;

    @Valid
    @ApiModelProperty(value = "语法结构列表")
    private List<GrammarStructureRequest> structures;

    @Valid
    @ApiModelProperty(value = "语法注意列表")
    private List<GrammarNoticeRequest> notices;

    @Valid
    @ApiModelProperty(value = "语法偏误列表")
    private List<GrammarErrorRequest> errors;

    @Getter
    @Setter
    public static class GrammarMeaningRequest implements Serializable {

        @ApiModelProperty(value = "语法意义ID, 新增时不传, 更新时传")
        private Long id;

        @NotBlank
        @ApiModelProperty(value = "语法意义内容", required = true)
        private String meaningContent;

        @ApiModelProperty(value = "语法意义外文翻译")
        private List<TextTranslationRequest> meaningContentTranslations;

        @ApiModelProperty(value = "语法意义图片ID")
        private Long imageId;

        @Valid
        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceRequest> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面，不传默认 0）")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarStructureRequest implements Serializable {

        @ApiModelProperty(value = "语法结构ID, 新增时不传, 更新时传")
        private Long id;

        @NotBlank
        @ApiModelProperty(value = "结构文本", required = true)
        private String structureContent;

        @Valid
        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceRequest> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面，不传默认 0）")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarNoticeRequest implements Serializable {

        @ApiModelProperty(value = "语法注意ID, 新增时不传, 更新时传")
        private Long id;

        @NotBlank
        @ApiModelProperty(value = "注意内容", required = true)
        private String noticeContent;

        @ApiModelProperty(value = "注意内容外文翻译")
        private List<TextTranslationRequest> noticeContentTranslations;

        @Valid
        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceRequest> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面，不传默认 0）")
        private Integer order;
    }

    @Getter
    @Setter
    public static class GrammarErrorRequest implements Serializable {

        @ApiModelProperty(value = "语法偏误ID, 新增时不传, 更新时传")
        private Long id;

        @NotBlank
        @ApiModelProperty(value = "偏误描述", required = true)
        private String errorContent;

        @ApiModelProperty(value = "偏误分析")
        private String errorAnalysis;

        @ApiModelProperty(value = "偏误分析外文翻译")
        private List<TextTranslationRequest> errorAnalysisTranslations;

        @ApiModelProperty(value = "排序权重（值大的排前面，不传默认 0）")
        private Integer order;
    }
}
```

- [ ] **Step 2: Create GrammarPointQueryRequest.java**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class GrammarPointQueryRequest implements Serializable {

    @ApiModelProperty(value = "语法点名称模糊查询")
    private String blurry;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "类别")
    private String category;
}
```

- [ ] **Step 3: Create GrammarPointCreateVO.java**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class GrammarPointCreateVO implements Serializable {

    @ApiModelProperty(value = "语法点ID")
    private Long id;
}
```

- [ ] **Step 4: Create GrammarPointBaseVO.java**

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

@Getter
@Setter
public class GrammarPointBaseVO implements Serializable {

    @ApiModelProperty(value = "语法点ID")
    private Long id;

    @ApiModelProperty(value = "语法点名称", required = true)
    private String name;

    @ApiModelProperty(value = "HSK等级")
    private String hskLevel;

    @ApiModelProperty(value = "项目")
    private String project;

    @ApiModelProperty(value = "类别")
    private String category;

    @ApiModelProperty(value = "细目")
    private String subCategory;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @ApiModelProperty(value = "意义个数")
    private Integer meaningCount;

    @ApiModelProperty(value = "结构个数")
    private Integer structureCount;

    @ApiModelProperty(value = "注意个数")
    private Integer noticeCount;

    @ApiModelProperty(value = "偏误个数")
    private Integer errorCount;
}
```

- [ ] **Step 5: Create GrammarPointVO.java**

```java
package com.naon.grid.backend.rest.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class GrammarPointVO implements Serializable {

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

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;

    @ApiModelProperty(value = "语法意义列表")
    private List<GrammarMeaningVO> meanings;

    @ApiModelProperty(value = "语法结构列表")
    private List<GrammarStructureVO> structures;

    @ApiModelProperty(value = "语法注意列表")
    private List<GrammarNoticeVO> notices;

    @ApiModelProperty(value = "语法偏误列表")
    private List<GrammarErrorVO> errors;

    @Getter
    @Setter
    public static class GrammarMeaningVO implements Serializable {

        @ApiModelProperty(value = "语法意义ID")
        private Long id;

        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "语法意义内容")
        private String meaningContent;

        @ApiModelProperty(value = "语法意义外文翻译")
        private List<TextTranslationVO> meaningContentTranslations;

        @ApiModelProperty(value = "语法意义图片ID")
        private Long imageId;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceVO> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面）")
        private Integer order;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class GrammarStructureVO implements Serializable {

        @ApiModelProperty(value = "语法结构ID")
        private Long id;

        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "结构文本")
        private String structureContent;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceVO> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面）")
        private Integer order;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class GrammarNoticeVO implements Serializable {

        @ApiModelProperty(value = "语法注意ID")
        private Long id;

        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "注意内容")
        private String noticeContent;

        @ApiModelProperty(value = "注意内容外文翻译")
        private List<TextTranslationVO> noticeContentTranslations;

        @ApiModelProperty(value = "例句列表")
        private List<ExampleSentenceVO> sentences;

        @ApiModelProperty(value = "排序权重（值大的排前面）")
        private Integer order;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }

    @Getter
    @Setter
    public static class GrammarErrorVO implements Serializable {

        @ApiModelProperty(value = "语法偏误ID")
        private Long id;

        @ApiModelProperty(value = "语法点ID")
        private Long grammarId;

        @ApiModelProperty(value = "偏误描述")
        private String errorContent;

        @ApiModelProperty(value = "偏误分析")
        private String errorAnalysis;

        @ApiModelProperty(value = "偏误分析外文翻译")
        private List<TextTranslationVO> errorAnalysisTranslations;

        @ApiModelProperty(value = "排序权重（值大的排前面）")
        private Integer order;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;

        @ApiModelProperty(value = "更新时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp updateTime;
    }
}
```

- [ ] **Step 6: Create GrammarPointWrapper.java**

```java
package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.ExampleSentenceRequest;
import com.naon.grid.backend.rest.request.GrammarPointCreateRequest;
import com.naon.grid.backend.rest.request.GrammarPointQueryRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.ExampleSentenceVO;
import com.naon.grid.backend.rest.vo.GrammarPointBaseVO;
import com.naon.grid.backend.rest.vo.GrammarPointVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.backend.service.grammar.dto.GrammarErrorDto;
import com.naon.grid.backend.service.grammar.dto.GrammarMeaningDto;
import com.naon.grid.backend.service.grammar.dto.GrammarNoticeDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointQueryCriteria;
import com.naon.grid.backend.service.grammar.dto.GrammarStructureDto;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GrammarPointWrapper {

    // ===== Request → Criteria =====
    public static GrammarPointQueryCriteria toCriteria(GrammarPointQueryRequest request) {
        GrammarPointQueryCriteria criteria = new GrammarPointQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        criteria.setHskLevel(request.getHskLevel());
        criteria.setCategory(request.getCategory());
        return criteria;
    }

    // ===== Request → Dto =====
    public static GrammarPointDto toDto(GrammarPointCreateRequest request) {
        GrammarPointDto dto = new GrammarPointDto();
        dto.setName(request.getName());
        dto.setHskLevel(request.getHskLevel());
        dto.setProject(request.getProject());
        dto.setCategory(request.getCategory());
        dto.setSubCategory(request.getSubCategory());
        dto.setMeanings(toMeaningDtoList(request.getMeanings()));
        dto.setStructures(toStructureDtoList(request.getStructures()));
        dto.setNotices(toNoticeDtoList(request.getNotices()));
        dto.setErrors(toErrorDtoList(request.getErrors()));
        return dto;
    }

    // ===== Dto → VO (detail) =====
    public static GrammarPointVO toVO(GrammarPointDto dto) {
        if (dto == null) return null;
        GrammarPointVO vo = new GrammarPointVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setHskLevel(dto.getHskLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setSubCategory(dto.getSubCategory());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setMeanings(toMeaningVOList(dto.getMeanings()));
        vo.setStructures(toStructureVOList(dto.getStructures()));
        vo.setNotices(toNoticeVOList(dto.getNotices()));
        vo.setErrors(toErrorVOList(dto.getErrors()));
        return vo;
    }

    // ===== Dto List → BaseVO List =====
    public static List<GrammarPointBaseVO> toBaseVOList(List<GrammarPointDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(GrammarPointWrapper::toBaseVO).collect(Collectors.toList());
    }

    private static GrammarPointBaseVO toBaseVO(GrammarPointDto dto) {
        GrammarPointBaseVO vo = new GrammarPointBaseVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setHskLevel(dto.getHskLevel());
        vo.setProject(dto.getProject());
        vo.setCategory(dto.getCategory());
        vo.setSubCategory(dto.getSubCategory());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        vo.setMeaningCount(dto.getMeaningCount());
        vo.setStructureCount(dto.getStructureCount());
        vo.setNoticeCount(dto.getNoticeCount());
        vo.setErrorCount(dto.getErrorCount());
        return vo;
    }

    // ===== Sub DTO conversion methods =====
    private static List<GrammarMeaningDto> toMeaningDtoList(List<GrammarPointCreateRequest.GrammarMeaningRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toMeaningDto).collect(Collectors.toList());
    }

    private static GrammarMeaningDto toMeaningDto(GrammarPointCreateRequest.GrammarMeaningRequest request) {
        GrammarMeaningDto dto = new GrammarMeaningDto();
        dto.setId(request.getId());
        dto.setMeaningContent(request.getMeaningContent());
        dto.setMeaningContentTranslations(toTextTranslationList(request.getMeaningContentTranslations()));
        dto.setImageId(request.getImageId());
        dto.setSentences(toExampleSentenceDtoList(request.getSentences()));
        dto.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        return dto;
    }

    private static List<GrammarStructureDto> toStructureDtoList(List<GrammarPointCreateRequest.GrammarStructureRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toStructureDto).collect(Collectors.toList());
    }

    private static GrammarStructureDto toStructureDto(GrammarPointCreateRequest.GrammarStructureRequest request) {
        GrammarStructureDto dto = new GrammarStructureDto();
        dto.setId(request.getId());
        dto.setStructureContent(request.getStructureContent());
        dto.setSentences(toExampleSentenceDtoList(request.getSentences()));
        dto.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        return dto;
    }

    private static List<GrammarNoticeDto> toNoticeDtoList(List<GrammarPointCreateRequest.GrammarNoticeRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toNoticeDto).collect(Collectors.toList());
    }

    private static GrammarNoticeDto toNoticeDto(GrammarPointCreateRequest.GrammarNoticeRequest request) {
        GrammarNoticeDto dto = new GrammarNoticeDto();
        dto.setId(request.getId());
        dto.setNoticeContent(request.getNoticeContent());
        dto.setNoticeContentTranslations(toTextTranslationList(request.getNoticeContentTranslations()));
        dto.setSentences(toExampleSentenceDtoList(request.getSentences()));
        dto.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        return dto;
    }

    private static List<GrammarErrorDto> toErrorDtoList(List<GrammarPointCreateRequest.GrammarErrorRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toErrorDto).collect(Collectors.toList());
    }

    private static GrammarErrorDto toErrorDto(GrammarPointCreateRequest.GrammarErrorRequest request) {
        GrammarErrorDto dto = new GrammarErrorDto();
        dto.setId(request.getId());
        dto.setErrorContent(request.getErrorContent());
        dto.setErrorAnalysis(request.getErrorAnalysis());
        dto.setErrorAnalysisTranslations(toTextTranslationList(request.getErrorAnalysisTranslations()));
        dto.setOrder(request.getOrder() != null ? request.getOrder() : 0);
        return dto;
    }

    // ===== Sub VO conversion methods =====
    private static List<GrammarPointVO.GrammarMeaningVO> toMeaningVOList(List<GrammarMeaningDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(GrammarPointWrapper::toMeaningVO).collect(Collectors.toList());
    }

    private static GrammarPointVO.GrammarMeaningVO toMeaningVO(GrammarMeaningDto dto) {
        GrammarPointVO.GrammarMeaningVO vo = new GrammarPointVO.GrammarMeaningVO();
        vo.setId(dto.getId());
        vo.setGrammarId(dto.getGrammarId());
        vo.setMeaningContent(dto.getMeaningContent());
        vo.setMeaningContentTranslations(toTextTranslationVOList(dto.getMeaningContentTranslations()));
        vo.setImageId(dto.getImageId());
        vo.setSentences(toExampleSentenceVOList(dto.getSentences()));
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<GrammarPointVO.GrammarStructureVO> toStructureVOList(List<GrammarStructureDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(GrammarPointWrapper::toStructureVO).collect(Collectors.toList());
    }

    private static GrammarPointVO.GrammarStructureVO toStructureVO(GrammarStructureDto dto) {
        GrammarPointVO.GrammarStructureVO vo = new GrammarPointVO.GrammarStructureVO();
        vo.setId(dto.getId());
        vo.setGrammarId(dto.getGrammarId());
        vo.setStructureContent(dto.getStructureContent());
        vo.setSentences(toExampleSentenceVOList(dto.getSentences()));
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<GrammarPointVO.GrammarNoticeVO> toNoticeVOList(List<GrammarNoticeDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(GrammarPointWrapper::toNoticeVO).collect(Collectors.toList());
    }

    private static GrammarPointVO.GrammarNoticeVO toNoticeVO(GrammarNoticeDto dto) {
        GrammarPointVO.GrammarNoticeVO vo = new GrammarPointVO.GrammarNoticeVO();
        vo.setId(dto.getId());
        vo.setGrammarId(dto.getGrammarId());
        vo.setNoticeContent(dto.getNoticeContent());
        vo.setNoticeContentTranslations(toTextTranslationVOList(dto.getNoticeContentTranslations()));
        vo.setSentences(toExampleSentenceVOList(dto.getSentences()));
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private static List<GrammarPointVO.GrammarErrorVO> toErrorVOList(List<GrammarErrorDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(GrammarPointWrapper::toErrorVO).collect(Collectors.toList());
    }

    private static GrammarPointVO.GrammarErrorVO toErrorVO(GrammarErrorDto dto) {
        GrammarPointVO.GrammarErrorVO vo = new GrammarPointVO.GrammarErrorVO();
        vo.setId(dto.getId());
        vo.setGrammarId(dto.getGrammarId());
        vo.setErrorContent(dto.getErrorContent());
        vo.setErrorAnalysis(dto.getErrorAnalysis());
        vo.setErrorAnalysisTranslations(toTextTranslationVOList(dto.getErrorAnalysisTranslations()));
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    // ===== Example Sentence conversion =====
    private static List<ExampleSentenceDto> toExampleSentenceDtoList(List<ExampleSentenceRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toExampleSentenceDto).collect(Collectors.toList());
    }

    private static ExampleSentenceDto toExampleSentenceDto(ExampleSentenceRequest request) {
        if (request == null) return null;
        ExampleSentenceDto dto = new ExampleSentenceDto();
        dto.setId(request.getId());
        dto.setSentence(request.getSentence());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setImageId(request.getImageId());
        dto.setOrder(request.getOrder());
        return dto;
    }

    private static List<ExampleSentenceVO> toExampleSentenceVOList(List<ExampleSentenceDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(GrammarPointWrapper::toExampleSentenceVO).collect(Collectors.toList());
    }

    private static ExampleSentenceVO toExampleSentenceVO(ExampleSentenceDto dto) {
        if (dto == null) return null;
        ExampleSentenceVO vo = new ExampleSentenceVO();
        vo.setId(dto.getId());
        vo.setSentence(dto.getSentence());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setImageId(dto.getImageId());
        vo.setOrder(dto.getOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    // ===== TextTranslation conversion =====
    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(GrammarPointWrapper::toTextTranslation).collect(Collectors.toList());
    }

    private static TextTranslation toTextTranslation(TextTranslationRequest request) {
        if (request == null) return null;
        TextTranslation translation = new TextTranslation();
        translation.setLanguage(request.getLanguage());
        translation.setTranslation(request.getTranslation());
        return translation;
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> translations) {
        if (translations == null) return Collections.emptyList();
        return translations.stream().map(GrammarPointWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation translation) {
        if (translation == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(translation.getLanguage());
        vo.setTranslation(translation.getTranslation());
        return vo;
    }
}
```

- [ ] **Step 7: Verify compilation**

Run:
```bash
cd grid-bootstrap
mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit Task 4**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/
git commit -m "feat: add grammar request, VO, and wrapper classes"
```

---

### Task 5: Service Implementation (Core Logic)

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/GrammarPointServiceImpl.java`

This is the largest and most complex file. It mirrors `CharCharacterServiceImpl` exactly.

- [ ] **Step 1: Create GrammarPointServiceImpl.java**

```java
package com.naon.grid.backend.service.grammar.impl;

import com.naon.grid.backend.domain.grammar.GrammarError;
import com.naon.grid.backend.domain.grammar.GrammarMeaning;
import com.naon.grid.backend.domain.grammar.GrammarNotice;
import com.naon.grid.backend.domain.grammar.GrammarPoint;
import com.naon.grid.backend.domain.grammar.GrammarStructure;
import com.naon.grid.backend.repo.grammar.GrammarErrorRepository;
import com.naon.grid.backend.repo.grammar.GrammarMeaningRepository;
import com.naon.grid.backend.repo.grammar.GrammarNoticeRepository;
import com.naon.grid.backend.repo.grammar.GrammarPointRepository;
import com.naon.grid.backend.repo.grammar.GrammarStructureRepository;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarErrorDto;
import com.naon.grid.backend.service.grammar.dto.GrammarMeaningDto;
import com.naon.grid.backend.service.grammar.dto.GrammarNoticeDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammar.dto.GrammarPointQueryCriteria;
import com.naon.grid.backend.service.grammar.dto.GrammarStructureDto;
import com.naon.grid.backend.service.grammar.mapstruct.GrammarPointMapper;
import com.naon.grid.backend.service.common.ExampleSentenceService;
import com.naon.grid.backend.service.common.dto.ExampleSentenceDto;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrammarPointServiceImpl implements GrammarPointService {

    private final GrammarPointRepository grammarPointRepository;
    private final GrammarMeaningRepository grammarMeaningRepository;
    private final GrammarStructureRepository grammarStructureRepository;
    private final GrammarNoticeRepository grammarNoticeRepository;
    private final GrammarErrorRepository grammarErrorRepository;
    private final GrammarPointMapper grammarPointMapper;
    private final ExampleSentenceService exampleSentenceService;

    @Override
    public PageResult<GrammarPointDto> queryAll(GrammarPointQueryCriteria criteria, Pageable pageable) {
        Page<GrammarPoint> page = grammarPointRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        PageResult<GrammarPointDto> pageResult = PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
        populateGrammarListStats(pageResult.getContent());
        return pageResult;
    }

    private void populateGrammarListStats(List<GrammarPointDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<Long> ids = dtos.stream().map(GrammarPointDto::getId).collect(Collectors.toList());

        Map<Long, Long> meaningCountMap = grammarMeaningRepository
                .countByGrammarIdInGroupByGrammarId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Long> structureCountMap = grammarStructureRepository
                .countByGrammarIdInGroupByGrammarId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Long> noticeCountMap = grammarNoticeRepository
                .countByGrammarIdInGroupByGrammarId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Map<Long, Long> errorCountMap = grammarErrorRepository
                .countByGrammarIdInGroupByGrammarId(ids, StatusEnum.ENABLED.getCode())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        for (GrammarPointDto dto : dtos) {
            if (dto.getMeaningCount() == null)
                dto.setMeaningCount(meaningCountMap.getOrDefault(dto.getId(), 0L).intValue());
            if (dto.getStructureCount() == null)
                dto.setStructureCount(structureCountMap.getOrDefault(dto.getId(), 0L).intValue());
            if (dto.getNoticeCount() == null)
                dto.setNoticeCount(noticeCountMap.getOrDefault(dto.getId(), 0L).intValue());
            if (dto.getErrorCount() == null)
                dto.setErrorCount(errorCountMap.getOrDefault(dto.getId(), 0L).intValue());
        }
    }

    private boolean isDraftOrReviewed(GrammarPointDto dto) {
        String editStatus = dto.getEditStatus();
        return EditStatusEnum.DRAFT.getCode().equals(editStatus)
            || EditStatusEnum.REVIEWED.getCode().equals(editStatus);
    }

    private GrammarPointDto toDtoWithDraftOverlay(GrammarPoint entity) {
        GrammarPointDto dto = grammarPointMapper.toDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    private void applyDraftOverlay(GrammarPointDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        GrammarPointDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, GrammarPointDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }

        if (draft.getName() != null)         dto.setName(draft.getName());
        if (draft.getHskLevel() != null)     dto.setHskLevel(draft.getHskLevel());
        if (draft.getProject() != null)      dto.setProject(draft.getProject());
        if (draft.getCategory() != null)     dto.setCategory(draft.getCategory());
        if (draft.getSubCategory() != null)  dto.setSubCategory(draft.getSubCategory());

        // 从草稿计算列表统计字段
        if (draft.getMeanings() != null) {
            dto.setMeaningCount(draft.getMeanings().size());
        }
        if (draft.getStructures() != null) {
            dto.setStructureCount(draft.getStructures().size());
        }
        if (draft.getNotices() != null) {
            dto.setNoticeCount(draft.getNotices().size());
        }
        if (draft.getErrors() != null) {
            dto.setErrorCount(draft.getErrors().size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrammarPointDto findById(Long id) {
        if (id == null) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }
        GrammarPoint grammarPoint = grammarPointRepository.findById(id).orElseGet(GrammarPoint::new);
        if (grammarPoint.getId() == null || StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.DRAFT.getCode().equals(grammarPoint.getEditStatus()) ||
            EditStatusEnum.REVIEWED.getCode().equals(grammarPoint.getEditStatus())) {
            if (grammarPoint.getDraftContent() == null) {
                throw new BadRequestException("Draft content not found");
            }
            GrammarPointDto dto = JsonUtils.fromJson(grammarPoint.getDraftContent(), GrammarPointDto.class);
            dto.setId(grammarPoint.getId());
            dto.setStatus(grammarPoint.getStatus());
            dto.setPublishStatus(grammarPoint.getPublishStatus());
            dto.setEditStatus(grammarPoint.getEditStatus());
            dto.setCreateBy(grammarPoint.getCreateBy());
            dto.setUpdateBy(grammarPoint.getUpdateBy());
            dto.setCreateTime(grammarPoint.getCreateTime());
            dto.setUpdateTime(grammarPoint.getUpdateTime());
            dto.setMeanings(sortMeaningsDesc(dto.getMeanings()));
            dto.setStructures(sortStructuresDesc(dto.getStructures()));
            dto.setNotices(sortNoticesDesc(dto.getNotices()));
            dto.setErrors(sortErrorsDesc(dto.getErrors()));
            return dto;
        }

        return toPublishedDetailDto(grammarPoint);
    }

    private GrammarPointDto toPublishedDetailDto(GrammarPoint grammarPoint) {
        Long id = grammarPoint.getId();
        GrammarPointDto dto = grammarPointMapper.toDto(grammarPoint);
        dto.setMeanings(sortMeaningsDesc(hydrateMeaningSentences(convertToMeaningDtos(
                grammarMeaningRepository.findByGrammarIdAndStatus(id, StatusEnum.ENABLED.getCode())))));
        dto.setStructures(sortStructuresDesc(hydrateStructureSentences(convertToStructureDtos(
                grammarStructureRepository.findByGrammarIdAndStatus(id, StatusEnum.ENABLED.getCode())))));
        dto.setNotices(sortNoticesDesc(hydrateNoticeSentences(convertToNoticeDtos(
                grammarNoticeRepository.findByGrammarIdAndStatus(id, StatusEnum.ENABLED.getCode())))));
        dto.setErrors(sortErrorsDesc(convertToErrorDtos(
                grammarErrorRepository.findByGrammarIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
        return dto;
    }

    // ===== Example sentence hydration methods =====

    private List<GrammarMeaningDto> hydrateMeaningSentences(List<GrammarMeaningDto> meanings) {
        if (meanings == null || meanings.isEmpty()) return meanings;
        List<Long> allSentenceIds = new ArrayList<>();
        for (GrammarMeaningDto m : meanings) {
            if (m.getSentences() != null) {
                m.getSentences().forEach(s -> { if (s.getId() != null) allSentenceIds.add(s.getId()); });
            }
        }
        if (allSentenceIds.isEmpty()) return meanings;
        Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(allSentenceIds);
        for (GrammarMeaningDto m : meanings) {
            if (m.getSentences() != null) {
                for (ExampleSentenceDto s : m.getSentences()) {
                    if (s.getId() != null && sentenceMap.containsKey(s.getId())) {
                        ExampleSentenceDto hydrated = sentenceMap.get(s.getId());
                        s.setSentence(hydrated.getSentence());
                        s.setPinyin(hydrated.getPinyin());
                        s.setAudioId(hydrated.getAudioId());
                        s.setTranslations(hydrated.getTranslations());
                        s.setImageId(hydrated.getImageId());
                        s.setOrder(hydrated.getOrder());
                    }
                }
            }
        }
        return meanings;
    }

    private List<GrammarStructureDto> hydrateStructureSentences(List<GrammarStructureDto> structures) {
        if (structures == null || structures.isEmpty()) return structures;
        List<Long> allSentenceIds = new ArrayList<>();
        for (GrammarStructureDto s : structures) {
            if (s.getSentences() != null) {
                s.getSentences().forEach(se -> { if (se.getId() != null) allSentenceIds.add(se.getId()); });
            }
        }
        if (allSentenceIds.isEmpty()) return structures;
        Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(allSentenceIds);
        for (GrammarStructureDto s : structures) {
            if (s.getSentences() != null) {
                for (ExampleSentenceDto se : s.getSentences()) {
                    if (se.getId() != null && sentenceMap.containsKey(se.getId())) {
                        ExampleSentenceDto hydrated = sentenceMap.get(se.getId());
                        se.setSentence(hydrated.getSentence());
                        se.setPinyin(hydrated.getPinyin());
                        se.setAudioId(hydrated.getAudioId());
                        se.setTranslations(hydrated.getTranslations());
                        se.setImageId(hydrated.getImageId());
                        se.setOrder(hydrated.getOrder());
                    }
                }
            }
        }
        return structures;
    }

    private List<GrammarNoticeDto> hydrateNoticeSentences(List<GrammarNoticeDto> notices) {
        if (notices == null || notices.isEmpty()) return notices;
        List<Long> allSentenceIds = new ArrayList<>();
        for (GrammarNoticeDto n : notices) {
            if (n.getSentences() != null) {
                n.getSentences().forEach(s -> { if (s.getId() != null) allSentenceIds.add(s.getId()); });
            }
        }
        if (allSentenceIds.isEmpty()) return notices;
        Map<Long, ExampleSentenceDto> sentenceMap = exampleSentenceService.findByIds(allSentenceIds);
        for (GrammarNoticeDto n : notices) {
            if (n.getSentences() != null) {
                for (ExampleSentenceDto s : n.getSentences()) {
                    if (s.getId() != null && sentenceMap.containsKey(s.getId())) {
                        ExampleSentenceDto hydrated = sentenceMap.get(s.getId());
                        s.setSentence(hydrated.getSentence());
                        s.setPinyin(hydrated.getPinyin());
                        s.setAudioId(hydrated.getAudioId());
                        s.setTranslations(hydrated.getTranslations());
                        s.setImageId(hydrated.getImageId());
                        s.setOrder(hydrated.getOrder());
                    }
                }
            }
        }
        return notices;
    }

    // ===== Create =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(GrammarPointDto resources) {
        GrammarPoint grammarPoint = new GrammarPoint();
        grammarPoint.setStatus(StatusEnum.ENABLED.getCode());
        grammarPoint.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        grammarPoint.setEditStatus(EditStatusEnum.DRAFT.getCode());
        grammarPoint.setName(resources.getName());
        grammarPoint.setDraftContent(JsonUtils.toJson(resources));
        grammarPoint = grammarPointRepository.save(grammarPoint);
        return grammarPoint.getId();
    }

    // ===== Update =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, GrammarPointDto resources) {
        GrammarPoint grammarPoint = grammarPointRepository.findById(id).orElseGet(GrammarPoint::new);
        if (grammarPoint.getId() == null || StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.REVIEWED.getCode().equals(grammarPoint.getEditStatus()) ||
            EditStatusEnum.PUBLISHED.getCode().equals(grammarPoint.getEditStatus())) {
            grammarPoint.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        grammarPoint.setDraftContent(JsonUtils.toJson(resources));
        grammarPointRepository.save(grammarPoint);
    }

    // ===== Delete =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        GrammarPoint grammarPoint = grammarPointRepository.findById(id).orElseGet(GrammarPoint::new);
        if (grammarPoint.getId() == null) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }
        grammarPoint.setStatus(StatusEnum.DISABLED.getCode());
        grammarPointRepository.save(grammarPoint);
    }

    // ===== Review =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        GrammarPoint grammarPoint = grammarPointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }

        if (grammarPoint.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }

        if (!EditStatusEnum.DRAFT.getCode().equals(grammarPoint.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }

        grammarPoint.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        grammarPointRepository.save(grammarPoint);
    }

    // ===== Publish =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id) {
        GrammarPoint grammarPoint = grammarPointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }

        if (grammarPoint.getDraftContent() == null) {
            throw new BadRequestException("Draft content not found");
        }

        if (!EditStatusEnum.REVIEWED.getCode().equals(grammarPoint.getEditStatus())) {
            throw new BadRequestException("Only reviewed drafts can be published");
        }

        GrammarPointDto draftDto = JsonUtils.fromJson(grammarPoint.getDraftContent(), GrammarPointDto.class);

        // Update main table fields
        grammarPoint.setName(draftDto.getName());
        grammarPoint.setHskLevel(draftDto.getHskLevel());
        grammarPoint.setProject(draftDto.getProject());
        grammarPoint.setCategory(draftDto.getCategory());
        grammarPoint.setSubCategory(draftDto.getSubCategory());

        // Sync child tables
        syncMeanings(id, draftDto.getMeanings());
        syncStructures(id, draftDto.getStructures());
        syncNotices(id, draftDto.getNotices());
        syncErrors(id, draftDto.getErrors());

        // Update status
        grammarPoint.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        grammarPoint.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        grammarPoint.setDraftContent(null);
        grammarPointRepository.save(grammarPoint);
    }

    // ===== Offline =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        GrammarPoint grammarPoint = grammarPointRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id)));

        if (StatusEnum.DISABLED.getCode().equals(grammarPoint.getStatus())) {
            throw new EntityNotFoundException(GrammarPoint.class, "id", String.valueOf(id));
        }

        grammarPoint.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        grammarPointRepository.save(grammarPoint);
    }

    // ===== Sync methods for child tables =====

    private void syncMeanings(Long grammarId, List<GrammarMeaningDto> submittedDtos) {
        List<GrammarMeaningDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<GrammarMeaning> existing = grammarMeaningRepository.findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        Map<Long, GrammarMeaning> existingMap = new HashMap<>();
        for (GrammarMeaning m : existing) {
            existingMap.put(m.getId(), m);
        }

        Set<Long> submittedIds = new HashSet<>();
        List<GrammarMeaning> toSave = new ArrayList<>();

        for (GrammarMeaningDto dto : submitted) {
            GrammarMeaning entity;
            if (dto.getId() == null || dto.getId() == 0) {
                entity = new GrammarMeaning();
                entity.setGrammarId(grammarId);
                entity.setStatus(StatusEnum.ENABLED.getCode());
            } else {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("语法意义ID重复: " + dto.getId());
                }
                GrammarMeaning existingEntity = existingMap.get(dto.getId());
                if (existingEntity == null) {
                    throw new BadRequestException("语法意义ID不属于当前语法点: " + dto.getId());
                }
                entity = existingEntity;
            }

            entity.setMeaningContent(dto.getMeaningContent());
            entity.setMeaningContentTranslations(JsonUtils.toTranslationJson(dto.getMeaningContentTranslations()));
            entity.setImageId(dto.getImageId());
            entity.setMeaningOrder(dto.getOrder() != null ? dto.getOrder() : 0);

            // Save sentences and collect IDs
            List<Long> sentenceIds = saveSentencesAndCollectIds(dto.getSentences());
            entity.setMeaningSentenceIds(JsonUtils.toJson(sentenceIds));

            toSave.add(entity);
        }

        // Soft delete removed items
        List<GrammarMeaning> toDelete = new ArrayList<>();
        for (GrammarMeaning m : existing) {
            if (!submittedIds.contains(m.getId())) {
                toDelete.add(m);
            }
        }
        for (GrammarMeaning m : toDelete) {
            m.setStatus(StatusEnum.DISABLED.getCode());
            grammarMeaningRepository.save(m);
        }
        disableChildSentences(toDelete, GrammarMeaning::getMeaningSentenceIds);

        grammarMeaningRepository.saveAll(toSave);
    }

    private void syncStructures(Long grammarId, List<GrammarStructureDto> submittedDtos) {
        List<GrammarStructureDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<GrammarStructure> existing = grammarStructureRepository.findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        Map<Long, GrammarStructure> existingMap = new HashMap<>();
        for (GrammarStructure s : existing) {
            existingMap.put(s.getId(), s);
        }

        Set<Long> submittedIds = new HashSet<>();
        List<GrammarStructure> toSave = new ArrayList<>();

        for (GrammarStructureDto dto : submitted) {
            GrammarStructure entity;
            if (dto.getId() == null || dto.getId() == 0) {
                entity = new GrammarStructure();
                entity.setGrammarId(grammarId);
                entity.setStatus(StatusEnum.ENABLED.getCode());
            } else {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("语法结构ID重复: " + dto.getId());
                }
                GrammarStructure existingEntity = existingMap.get(dto.getId());
                if (existingEntity == null) {
                    throw new BadRequestException("语法结构ID不属于当前语法点: " + dto.getId());
                }
                entity = existingEntity;
            }

            entity.setStructureContent(dto.getStructureContent());
            entity.setStructureOrder(dto.getOrder() != null ? dto.getOrder() : 0);

            List<Long> sentenceIds = saveSentencesAndCollectIds(dto.getSentences());
            entity.setStructureSentenceIds(JsonUtils.toJson(sentenceIds));

            toSave.add(entity);
        }

        List<GrammarStructure> toDelete = new ArrayList<>();
        for (GrammarStructure s : existing) {
            if (!submittedIds.contains(s.getId())) {
                toDelete.add(s);
            }
        }
        for (GrammarStructure s : toDelete) {
            s.setStatus(StatusEnum.DISABLED.getCode());
            grammarStructureRepository.save(s);
        }
        disableChildSentences(toDelete, GrammarStructure::getStructureSentenceIds);

        grammarStructureRepository.saveAll(toSave);
    }

    private void syncNotices(Long grammarId, List<GrammarNoticeDto> submittedDtos) {
        List<GrammarNoticeDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<GrammarNotice> existing = grammarNoticeRepository.findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        Map<Long, GrammarNotice> existingMap = new HashMap<>();
        for (GrammarNotice n : existing) {
            existingMap.put(n.getId(), n);
        }

        Set<Long> submittedIds = new HashSet<>();
        List<GrammarNotice> toSave = new ArrayList<>();

        for (GrammarNoticeDto dto : submitted) {
            GrammarNotice entity;
            if (dto.getId() == null || dto.getId() == 0) {
                entity = new GrammarNotice();
                entity.setGrammarId(grammarId);
                entity.setStatus(StatusEnum.ENABLED.getCode());
            } else {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("语法注意ID重复: " + dto.getId());
                }
                GrammarNotice existingEntity = existingMap.get(dto.getId());
                if (existingEntity == null) {
                    throw new BadRequestException("语法注意ID不属于当前语法点: " + dto.getId());
                }
                entity = existingEntity;
            }

            entity.setNoticeContent(dto.getNoticeContent());
            entity.setNoticeContentTranslations(JsonUtils.toTranslationJson(dto.getNoticeContentTranslations()));
            entity.setNoticeOrder(dto.getOrder() != null ? dto.getOrder() : 0);

            List<Long> sentenceIds = saveSentencesAndCollectIds(dto.getSentences());
            entity.setNoticeSentenceIds(JsonUtils.toJson(sentenceIds));

            toSave.add(entity);
        }

        List<GrammarNotice> toDelete = new ArrayList<>();
        for (GrammarNotice n : existing) {
            if (!submittedIds.contains(n.getId())) {
                toDelete.add(n);
            }
        }
        for (GrammarNotice n : toDelete) {
            n.setStatus(StatusEnum.DISABLED.getCode());
            grammarNoticeRepository.save(n);
        }
        disableChildSentences(toDelete, GrammarNotice::getNoticeSentenceIds);

        grammarNoticeRepository.saveAll(toSave);
    }

    private void syncErrors(Long grammarId, List<GrammarErrorDto> submittedDtos) {
        List<GrammarErrorDto> submitted = submittedDtos == null ? Collections.emptyList() : submittedDtos;
        List<GrammarError> existing = grammarErrorRepository.findByGrammarIdAndStatus(grammarId, StatusEnum.ENABLED.getCode());
        Map<Long, GrammarError> existingMap = new HashMap<>();
        for (GrammarError e : existing) {
            existingMap.put(e.getId(), e);
        }

        Set<Long> submittedIds = new HashSet<>();
        List<GrammarError> toSave = new ArrayList<>();

        for (GrammarErrorDto dto : submitted) {
            GrammarError entity;
            if (dto.getId() == null || dto.getId() == 0) {
                entity = new GrammarError();
                entity.setGrammarId(grammarId);
                entity.setStatus(StatusEnum.ENABLED.getCode());
            } else {
                if (!submittedIds.add(dto.getId())) {
                    throw new BadRequestException("语法偏误ID重复: " + dto.getId());
                }
                GrammarError existingEntity = existingMap.get(dto.getId());
                if (existingEntity == null) {
                    throw new BadRequestException("语法偏误ID不属于当前语法点: " + dto.getId());
                }
                entity = existingEntity;
            }

            entity.setErrorContent(dto.getErrorContent());
            entity.setErrorAnalysis(dto.getErrorAnalysis());
            entity.setErrorAnalysisTranslations(JsonUtils.toTranslationJson(dto.getErrorAnalysisTranslations()));
            entity.setErrorOrder(dto.getOrder() != null ? dto.getOrder() : 0);

            toSave.add(entity);
        }

        List<GrammarError> toDelete = new ArrayList<>();
        for (GrammarError e : existing) {
            if (!submittedIds.contains(e.getId())) {
                toDelete.add(e);
            }
        }
        for (GrammarError e : toDelete) {
            e.setStatus(StatusEnum.DISABLED.getCode());
            grammarErrorRepository.save(e);
        }

        grammarErrorRepository.saveAll(toSave);
    }

    // ===== Example sentence helpers =====

    private List<Long> saveSentencesAndCollectIds(List<ExampleSentenceDto> sentences) {
        if (sentences == null || sentences.isEmpty()) return Collections.emptyList();
        List<Long> ids = new ArrayList<>();
        for (ExampleSentenceDto s : sentences) {
            if (s != null && s.getSentence() != null && !s.getSentence().trim().isEmpty()) {
                ExampleSentenceDto saved = exampleSentenceService.save(s);
                if (saved != null && saved.getId() != null) {
                    ids.add(saved.getId());
                }
            }
        }
        return ids;
    }

    @FunctionalInterface
    private interface SentenceIdsGetter {
        String getSentenceIds(Object entity);
    }

    private <T> void disableChildSentences(List<T> entities, SentenceIdsGetter idsGetter) {
        if (entities == null || entities.isEmpty()) return;
        List<Long> allIds = new ArrayList<>();
        for (T entity : entities) {
            String idsJson = idsGetter.getSentenceIds(entity);
            if (idsJson != null) {
                try {
                    List<Long> ids = JSON.parseArray(idsJson, Long.class);
                    if (ids != null) allIds.addAll(ids);
                } catch (Exception ignored) {}
            }
        }
        if (!allIds.isEmpty()) {
            exampleSentenceService.disableByIds(allIds);
        }
    }

    // ===== Entity ↔ DTO conversion =====

    private List<GrammarMeaningDto> convertToMeaningDtos(List<GrammarMeaning> entities) {
        if (entities == null) return Collections.emptyList();
        List<GrammarMeaningDto> dtos = new ArrayList<>();
        for (GrammarMeaning entity : entities) {
            GrammarMeaningDto dto = new GrammarMeaningDto();
            dto.setId(entity.getId());
            dto.setGrammarId(entity.getGrammarId());
            dto.setMeaningContent(entity.getMeaningContent());
            dto.setMeaningContentTranslations(JsonUtils.parseTranslationList(entity.getMeaningContentTranslations()));
            dto.setImageId(entity.getImageId());
            dto.setOrder(entity.getMeaningOrder());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setStatus(entity.getStatus());

            // Parse sentence IDs → ExampleSentenceDto stubs
            String idsJson = entity.getMeaningSentenceIds();
            if (idsJson != null) {
                try {
                    List<Long> ids = JSON.parseArray(idsJson, Long.class);
                    if (ids != null) {
                        dto.setSentences(ids.stream().map(id -> {
                            ExampleSentenceDto s = new ExampleSentenceDto();
                            s.setId(id);
                            return s;
                        }).collect(Collectors.toList()));
                    }
                } catch (Exception ignored) {}
            }

            dtos.add(dto);
        }
        return dtos;
    }

    private List<GrammarStructureDto> convertToStructureDtos(List<GrammarStructure> entities) {
        if (entities == null) return Collections.emptyList();
        List<GrammarStructureDto> dtos = new ArrayList<>();
        for (GrammarStructure entity : entities) {
            GrammarStructureDto dto = new GrammarStructureDto();
            dto.setId(entity.getId());
            dto.setGrammarId(entity.getGrammarId());
            dto.setStructureContent(entity.getStructureContent());
            dto.setOrder(entity.getStructureOrder());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setStatus(entity.getStatus());

            String idsJson = entity.getStructureSentenceIds();
            if (idsJson != null) {
                try {
                    List<Long> ids = JSON.parseArray(idsJson, Long.class);
                    if (ids != null) {
                        dto.setSentences(ids.stream().map(id -> {
                            ExampleSentenceDto s = new ExampleSentenceDto();
                            s.setId(id);
                            return s;
                        }).collect(Collectors.toList()));
                    }
                } catch (Exception ignored) {}
            }

            dtos.add(dto);
        }
        return dtos;
    }

    private List<GrammarNoticeDto> convertToNoticeDtos(List<GrammarNotice> entities) {
        if (entities == null) return Collections.emptyList();
        List<GrammarNoticeDto> dtos = new ArrayList<>();
        for (GrammarNotice entity : entities) {
            GrammarNoticeDto dto = new GrammarNoticeDto();
            dto.setId(entity.getId());
            dto.setGrammarId(entity.getGrammarId());
            dto.setNoticeContent(entity.getNoticeContent());
            dto.setNoticeContentTranslations(JsonUtils.parseTranslationList(entity.getNoticeContentTranslations()));
            dto.setOrder(entity.getNoticeOrder());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setStatus(entity.getStatus());

            String idsJson = entity.getNoticeSentenceIds();
            if (idsJson != null) {
                try {
                    List<Long> ids = JSON.parseArray(idsJson, Long.class);
                    if (ids != null) {
                        dto.setSentences(ids.stream().map(id -> {
                            ExampleSentenceDto s = new ExampleSentenceDto();
                            s.setId(id);
                            return s;
                        }).collect(Collectors.toList()));
                    }
                } catch (Exception ignored) {}
            }

            dtos.add(dto);
        }
        return dtos;
    }

    private List<GrammarErrorDto> convertToErrorDtos(List<GrammarError> entities) {
        if (entities == null) return Collections.emptyList();
        List<GrammarErrorDto> dtos = new ArrayList<>();
        for (GrammarError entity : entities) {
            GrammarErrorDto dto = new GrammarErrorDto();
            dto.setId(entity.getId());
            dto.setGrammarId(entity.getGrammarId());
            dto.setErrorContent(entity.getErrorContent());
            dto.setErrorAnalysis(entity.getErrorAnalysis());
            dto.setErrorAnalysisTranslations(JsonUtils.parseTranslationList(entity.getErrorAnalysisTranslations()));
            dto.setOrder(entity.getErrorOrder());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setStatus(entity.getStatus());
            dtos.add(dto);
        }
        return dtos;
    }

    // ===== Sort helpers =====

    private List<GrammarMeaningDto> sortMeaningsDesc(List<GrammarMeaningDto> list) {
        if (list == null || list.isEmpty()) return list;
        list.sort(Comparator.comparing(GrammarMeaningDto::getOrder, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private List<GrammarStructureDto> sortStructuresDesc(List<GrammarStructureDto> list) {
        if (list == null || list.isEmpty()) return list;
        list.sort(Comparator.comparing(GrammarStructureDto::getOrder, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private List<GrammarNoticeDto> sortNoticesDesc(List<GrammarNoticeDto> list) {
        if (list == null || list.isEmpty()) return list;
        list.sort(Comparator.comparing(GrammarNoticeDto::getOrder, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private List<GrammarErrorDto> sortErrorsDesc(List<GrammarErrorDto> list) {
        if (list == null || list.isEmpty()) return list;
        list.sort(Comparator.comparing(GrammarErrorDto::getOrder, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run:
```bash
cd grid-bootstrap
mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit Task 5**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/grammar/impl/
git commit -m "feat: add grammar service implementation"
```

---

### Task 6: Create Controller

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/GrammarPointController.java`

- [ ] **Step 1: Create GrammarPointController.java**

```java
package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.GrammarPointCreateRequest;
import com.naon.grid.backend.rest.request.GrammarPointQueryRequest;
import com.naon.grid.backend.rest.vo.GrammarPointBaseVO;
import com.naon.grid.backend.rest.vo.GrammarPointCreateVO;
import com.naon.grid.backend.rest.vo.GrammarPointVO;
import com.naon.grid.backend.rest.wrapper.GrammarPointWrapper;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.naon.grid.backend.rest.wrapper.GrammarPointWrapper.toBaseVOList;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：语法-语法点管理")
@RequestMapping("/api/grammar")
public class GrammarPointController {

    private final GrammarPointService grammarPointService;

    @Log("新增语法点")
    @ApiOperation("新增语法点")
    @AnonymousPostMapping
    public ResponseEntity<GrammarPointCreateVO> create(@Valid @RequestBody GrammarPointCreateRequest request) {
        GrammarPointCreateVO vo = new GrammarPointCreateVO();
        vo.setId(grammarPointService.create(GrammarPointWrapper.toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("修改语法点内容")
    @ApiOperation("修改语法点内容")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody GrammarPointCreateRequest request) {
        grammarPointService.update(id, GrammarPointWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("语法点草稿审核通过")
    @ApiOperation("语法点草稿审核通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        grammarPointService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布语法点")
    @ApiOperation("发布语法点（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        grammarPointService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询语法点详情")
    @ApiOperation("根据ID查询语法点详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<GrammarPointVO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(GrammarPointWrapper.toVO(grammarPointService.findById(id)), HttpStatus.OK);
    }

    @Log("查询语法点列表")
    @ApiOperation("分页查询语法点列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<GrammarPointBaseVO>> queryAll(GrammarPointQueryRequest request, Pageable pageable) {
        PageResult<GrammarPointDto> pageResult = grammarPointService.queryAll(GrammarPointWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除语法点")
    @ApiOperation("删除语法点")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        grammarPointService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线语法点")
    @ApiOperation("下线语法点")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        grammarPointService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run:
```bash
cd grid-bootstrap
mvn compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit Task 6**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/GrammarPointController.java
git commit -m "feat: add grammar management controller"
```

---

### Summary Verification

After all tasks are complete:

- [ ] **Full build check**

Run:
```bash
cd grid-bootstrap
mvn clean compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Final commit**

```bash
git add .
git commit -m "feat: complete grammar management backend"
```

Total: 25 new files, zero modifications to existing files.
