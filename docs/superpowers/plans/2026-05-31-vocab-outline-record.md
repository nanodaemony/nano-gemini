# 纲外词记录功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现纲外词记录和管理功能，包括用户搜索无结果时自动记录，以及后台分页查询和标记已处理接口

**Architecture:**
- 在 grid-system 模块新增实体类、Repository、Service、DTO、VO
- 修改 grid-app 模块的 AppVocabWordController 集成记录逻辑
- 在 VocabWordController 新增后台管理接口
- 使用 upsert 模式处理并发，保证数据一致性

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MySQL, MapStruct, Lombok

---

## 文件结构概览

| 文件路径 | 操作 | 说明 |
|----------|------|------|
| `sql/vocab-outline.sql` | Create | 数据库表创建脚本 |
| `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabOutlineRecord.java` | Create | 实体类 |
| `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabOutlineRecordRepository.java` | Create | Repository 接口 |
| `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabOutlineRecordDto.java` | Create | DTO 类 |
| `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabOutlineRecordQueryCriteria.java` | Create | 查询条件 DTO |
| `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/mapstruct/VocabOutlineRecordMapper.java` | Create | MapStruct Mapper |
| `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabOutlineRecordService.java` | Create | Service 接口 |
| `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabOutlineRecordServiceImpl.java` | Create | Service 实现 |
| `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabOutlineRecordVO.java` | Create | VO 类 |
| `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java` | Modify | 新增后台管理接口 |
| `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java` | Modify | 集成纲外词记录逻辑 |

---

### Task 1: 创建数据库表脚本

**Files:**
- Create: `sql/vocab-outline.sql`

- [ ] **Step 1: 创建 SQL 脚本**

```sql
-- 纲外词记录表
CREATE TABLE `vocab_outline_record` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `word` varchar(50) NOT NULL COMMENT '词汇文本（去空格后）',
  `search_count` int(11) NOT NULL DEFAULT '1' COMMENT '未搜到次数',
  `status` TINYINT(4) NOT NULL DEFAULT '0' COMMENT '处理状态, 0:未处理 1:已处理',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`),
  KEY `idx_status` (`status`),
  KEY `idx_search_count` (`search_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='纲外词记录表';
```

- [ ] **Step 2: 提交**

```bash
git add sql/vocab-outline.sql
git commit -m "feat: add vocab outline record table schema"
```

---

### Task 2: 创建实体类

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabOutlineRecord.java`

- [ ] **Step 1: 编写实体类**

```java
package com.naon.grid.backend.domain.vocabulary;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@Table(name = "vocab_outline_record")
public class VocabOutlineRecord implements Serializable {

    @Id
    @Column(name = "id")
    @ApiModelProperty(value = "主键ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "word", nullable = false, length = 50)
    @ApiModelProperty(value = "词汇文本")
    private String word;

    @Column(name = "search_count", nullable = false)
    @ApiModelProperty(value = "未搜到次数")
    private Integer searchCount = 1;

    @Column(name = "status")
    @ApiModelProperty(value = "处理状态, 0:未处理 1:已处理")
    private Integer status = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp updateTime;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabOutlineRecord.java
git commit -m "feat: add VocabOutlineRecord entity"
```

---

### Task 3: 创建 Repository 接口

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabOutlineRecordRepository.java`

- [ ] **Step 1: 编写 Repository**

```java
package com.naon.grid.backend.repo.vocabulary;

import com.naon.grid.backend.domain.vocabulary.VocabOutlineRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VocabOutlineRecordRepository extends JpaRepository<VocabOutlineRecord, Integer>, JpaSpecificationExecutor<VocabOutlineRecord> {

    Optional<VocabOutlineRecord> findByWord(String word);

    @Modifying
    @Query("UPDATE VocabOutlineRecord r SET r.searchCount = r.searchCount + 1, r.updateTime = CURRENT_TIMESTAMP WHERE r.word = :word")
    int incrementSearchCount(@Param("word") String word);
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/repo/vocabulary/VocabOutlineRecordRepository.java
git commit -m "feat: add VocabOutlineRecordRepository"
```

---

### Task 4: 创建 DTO 和 QueryCriteria

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabOutlineRecordDto.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabOutlineRecordQueryCriteria.java`

- [ ] **Step 1: 编写 VocabOutlineRecordDto**

```java
package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class VocabOutlineRecordDto implements Serializable {

    @ApiModelProperty(value = "主键ID")
    private Integer id;

    @ApiModelProperty(value = "词汇文本")
    private String word;

    @ApiModelProperty(value = "未搜到次数")
    private Integer searchCount;

    @ApiModelProperty(value = "处理状态, 0:未处理 1:已处理")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

- [ ] **Step 2: 编写 VocabOutlineRecordQueryCriteria**

```java
package com.naon.grid.backend.service.vocabulary.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class VocabOutlineRecordQueryCriteria implements Serializable {

    @ApiModelProperty(value = "处理状态")
    @Query
    private Integer status;
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabOutlineRecordDto.java
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabOutlineRecordQueryCriteria.java
git commit -m "feat: add VocabOutlineRecord DTOs"
```

---

### Task 5: 创建 VO 类

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabOutlineRecordVO.java`

- [ ] **Step 1: 编写 VO**

```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
public class VocabOutlineRecordVO implements Serializable {

    @ApiModelProperty(value = "主键ID")
    private Integer id;

    @ApiModelProperty(value = "词汇文本")
    private String word;

    @ApiModelProperty(value = "未搜到次数")
    private Integer searchCount;

    @ApiModelProperty(value = "处理状态, 0:未处理 1:已处理")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabOutlineRecordVO.java
git commit -m "feat: add VocabOutlineRecordVO"
```

---

### Task 6: 创建 MapStruct Mapper

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/mapstruct/VocabOutlineRecordMapper.java`

- [ ] **Step 1: 先查看现有 Mapper 以保持一致性**

Read: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/mapstruct/VocabWordMapper.java`

- [ ] **Step 2: 编写 Mapper**

```java
package com.naon.grid.backend.service.vocabulary.mapstruct;

import com.naon.grid.backend.domain.vocabulary.VocabOutlineRecord;
import com.naon.grid.backend.rest.vo.VocabOutlineRecordVO;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VocabOutlineRecordMapper {

    VocabOutlineRecordDto toDto(VocabOutlineRecord entity);

    VocabOutlineRecordVO toVo(VocabOutlineRecordDto dto);

    VocabOutlineRecord toEntity(VocabOutlineRecordDto dto);
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/mapstruct/VocabOutlineRecordMapper.java
git commit -m "feat: add VocabOutlineRecordMapper"
```

---

### Task 7: 创建 Service 接口

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabOutlineRecordService.java`

- [ ] **Step 1: 编写 Service 接口**

```java
package com.naon.grid.backend.service.vocabulary;

import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface VocabOutlineRecordService {

    /**
     * 记录纲外词（如果符合条件）
     * @param searchWord 用户原始搜索词
     */
    void recordIfNeeded(String searchWord);

    /**
     * 分页查询纲外词
     */
    PageResult<VocabOutlineRecordDto> queryAll(VocabOutlineRecordQueryCriteria criteria, Pageable pageable);

    /**
     * 标记为已处理
     */
    void markAsCompleted(Integer id);
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/VocabOutlineRecordService.java
git commit -m "feat: add VocabOutlineRecordService interface"
```

---

### Task 8: 创建 Service 实现类

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabOutlineRecordServiceImpl.java`

- [ ] **Step 1: 先查看 EntityNotFoundException 的位置**

Grep for `EntityNotFoundException` to find its package

- [ ] **Step 2: 编写 Service 实现**

```java
package com.naon.grid.backend.service.vocabulary.impl;

import com.naon.grid.backend.domain.vocabulary.VocabOutlineRecord;
import com.naon.grid.backend.repo.vocabulary.VocabOutlineRecordRepository;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordQueryCriteria;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabOutlineRecordMapper;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VocabOutlineRecordServiceImpl implements VocabOutlineRecordService {

    private static final Pattern CHINESE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fff\\u3000-\\u303f\\uff00-\\uffef]+$");

    private final VocabOutlineRecordRepository vocabOutlineRecordRepository;
    private final VocabOutlineRecordMapper vocabOutlineRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordIfNeeded(String searchWord) {
        String processedWord = preprocessSearchWord(searchWord);
        if (processedWord == null) {
            return;
        }

        // 尝试直接增加计数（避免先查询再插入的竞态条件）
        int updated = vocabOutlineRecordRepository.incrementSearchCount(processedWord);
        if (updated > 0) {
            return;
        }

        // 没有更新到记录，说明是新词，尝试插入
        try {
            VocabOutlineRecord record = new VocabOutlineRecord();
            record.setWord(processedWord);
            record.setSearchCount(1);
            record.setStatus(0); // 0=未处理
            vocabOutlineRecordRepository.save(record);
        } catch (DataIntegrityViolationException e) {
            // 并发情况下，另一个线程已经插入了，再次尝试增加计数
            vocabOutlineRecordRepository.incrementSearchCount(processedWord);
        }
    }

    @Override
    public PageResult<VocabOutlineRecordDto> queryAll(VocabOutlineRecordQueryCriteria criteria, Pageable pageable) {
        Page<VocabOutlineRecord> page = vocabOutlineRecordRepository.findAll(
                (root, criteriaQuery, criteriaBuilder) -> QueryHelp.getPredicate(root, criteria, criteriaBuilder),
                pageable
        );
        return PageUtil.toPage(page.map(vocabOutlineRecordMapper::toDto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsCompleted(Integer id) {
        VocabOutlineRecord record = vocabOutlineRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(VocabOutlineRecord.class, "id", String.valueOf(id)));
        record.setStatus(1); // 1=已处理
        vocabOutlineRecordRepository.save(record);
    }

    /**
     * 预处理并验证搜索词
     * @param searchWord 原始搜索词
     * @return 处理后的搜索词，如果不符合条件返回null
     */
    private String preprocessSearchWord(String searchWord) {
        if (searchWord == null || searchWord.trim().isEmpty()) {
            return null;
        }

        // 去掉所有空格（包括首尾和中间）
        String processed = searchWord.replaceAll("\\s+", "");

        // 检查长度（不超过50字符）
        if (processed.length() > 50) {
            return null;
        }

        // 检查是否为全中文+中文标点
        if (!CHINESE_PATTERN.matcher(processed).matches()) {
            return null;
        }

        return processed;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabOutlineRecordServiceImpl.java
git commit -m "feat: add VocabOutlineRecordServiceImpl"
```

---

### Task 9: 修改 VocabWordController 添加后台接口

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

- [ ] **Step 1: 先读取现有 Controller**

Read: `grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java`

- [ ] **Step 2: 添加必要的 import 和注入**

Add these imports:
```java
import com.naon.grid.backend.rest.vo.VocabOutlineRecordVO;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordQueryCriteria;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabOutlineRecordMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
```

Add this field:
```java
private final VocabOutlineRecordService vocabOutlineRecordService;
private final VocabOutlineRecordMapper vocabOutlineRecordMapper;
```

Update the constructor to include these two parameters.

- [ ] **Step 3: 添加后台接口方法**

Add these methods at the end of the class, before the private helper methods:

```java
    @Log("查询纲外词列表")
    @ApiOperation("分页查询纲外词列表")
    @AnonymousGetMapping("/outline")
    public ResponseEntity<PageResult<VocabOutlineRecordVO>> queryOutline(
            VocabOutlineRecordQueryCriteria criteria,
            Pageable pageable) {
        // 默认按搜索次数降序、创建时间降序
        if (pageable.getSort().isEmpty()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "searchCount")
                            .and(Sort.by(Sort.Direction.DESC, "createTime"))
            );
        }
        PageResult<VocabOutlineRecordDto> pageResult = vocabOutlineRecordService.queryAll(criteria, pageable);
        java.util.List<VocabOutlineRecordVO> vos = pageResult.getContent().stream()
                .map(vocabOutlineRecordMapper::toVo)
                .collect(java.util.stream.Collectors.toList());
        return new ResponseEntity<>(new PageResult<>(vos, pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("标记纲外词已处理")
    @ApiOperation("标记纲外词为已处理")
    @AnonymousPutMapping("/outline/{id}/complete")
    public ResponseEntity<Object> completeOutline(@PathVariable Integer id) {
        vocabOutlineRecordService.markAsCompleted(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
```

- [ ] **Step 4: 完整的修改后文件内容**

```java
package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.ExerciseOptionRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.request.VocabWordCreateRequest;
import com.naon.grid.backend.rest.request.VocabWordQueryRequest;
import com.naon.grid.backend.rest.vo.ExerciseOptionVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.rest.vo.VocabWordBaseVO;
import com.naon.grid.backend.rest.vo.VocabWordCreateVO;
import com.naon.grid.backend.rest.vo.VocabWordVO;
import com.naon.grid.backend.rest.vo.VocabOutlineRecordVO;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabExampleDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabExerciseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabOutlineRecordQueryCriteria;
import com.naon.grid.backend.service.vocabulary.dto.VocabSenseDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabStructureDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordQueryCriteria;
import com.naon.grid.backend.service.vocabulary.mapstruct.VocabOutlineRecordMapper;
import com.naon.grid.domain.common.QuestionOption;
import com.naon.grid.domain.common.TextTranslation;
import com.naon.grid.utils.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：词汇-词汇管理")
@RequestMapping("/api/vocabulary")
public class VocabWordController {

    private final VocabWordService vocabWordService;
    private final VocabOutlineRecordService vocabOutlineRecordService;
    private final VocabOutlineRecordMapper vocabOutlineRecordMapper;

    @Log("查询词汇详情")
    @ApiOperation("根据ID查询词汇详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<VocabWordVO> findById(@PathVariable Integer id) {
        return new ResponseEntity<>(toVO(vocabWordService.findById(id)), HttpStatus.OK);
    }

    @Log("查询词汇列表")
    @ApiOperation("分页查询词汇列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<VocabWordBaseVO>> queryAll(VocabWordQueryRequest request, Pageable pageable) {
        PageResult<VocabWordDto> pageResult = vocabWordService.queryAll(toCriteria(request), pageable);
        return new ResponseEntity<>(new PageResult<>(toBaseVOList(pageResult.getContent()), pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("新增词汇")
    @ApiOperation("新增词汇")
    @AnonymousPostMapping
    public ResponseEntity<VocabWordCreateVO> create(@Valid @RequestBody VocabWordCreateRequest request) {
        VocabWordCreateVO vo = new VocabWordCreateVO();
        vo.setId(vocabWordService.create(toDto(request)));
        return new ResponseEntity<>(vo, HttpStatus.CREATED);
    }

    @Log("更新词汇")
    @ApiOperation("更新词汇")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Integer id, @Valid @RequestBody VocabWordCreateRequest request) {
        vocabWordService.update(id, toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("删除词汇")
    @ApiOperation("删除词汇")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Integer id) {
        vocabWordService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询纲外词列表")
    @ApiOperation("分页查询纲外词列表")
    @AnonymousGetMapping("/outline")
    public ResponseEntity<PageResult<VocabOutlineRecordVO>> queryOutline(
            VocabOutlineRecordQueryCriteria criteria,
            Pageable pageable) {
        // 默认按搜索次数降序、创建时间降序
        if (pageable.getSort().isEmpty()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "searchCount")
                            .and(Sort.by(Sort.Direction.DESC, "createTime"))
            );
        }
        PageResult<VocabOutlineRecordDto> pageResult = vocabOutlineRecordService.queryAll(criteria, pageable);
        List<VocabOutlineRecordVO> vos = pageResult.getContent().stream()
                .map(vocabOutlineRecordMapper::toVo)
                .collect(Collectors.toList());
        return new ResponseEntity<>(new PageResult<>(vos, pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("标记纲外词已处理")
    @ApiOperation("标记纲外词为已处理")
    @AnonymousPutMapping("/outline/{id}/complete")
    public ResponseEntity<Object> completeOutline(@PathVariable Integer id) {
        vocabOutlineRecordService.markAsCompleted(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private VocabWordQueryCriteria toCriteria(VocabWordQueryRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        return criteria;
    }

    private VocabWordDto toDto(VocabWordCreateRequest request) {
        VocabWordDto dto = new VocabWordDto();
        dto.setWord(request.getWord());
        dto.setWordTraditional(request.getWordTraditional());
        dto.setPinyin(request.getPinyin());
        dto.setAudioId(request.getAudioId());
        dto.setHskLevel(request.getHskLevel());
        dto.setSenses(toSenseDtoList(request.getSenses()));
        dto.setExercises(toExerciseDtoList(request.getExercises()));
        return dto;
    }

    private List<VocabSenseDto> toSenseDtoList(List<VocabWordCreateRequest.VocabSenseRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toSenseDto).collect(Collectors.toList());
    }

    private VocabSenseDto toSenseDto(VocabWordCreateRequest.VocabSenseRequest request) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(request.getId());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setChineseDef(request.getChineseDef());
        dto.setDefAudioId(request.getDefAudioId());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setSynonyms(request.getSynonyms());
        dto.setAntonyms(request.getAntonyms());
        dto.setRelatedForward(request.getRelatedForward());
        dto.setRelatedBackward(request.getRelatedBackward());
        dto.setSenseOrder(request.getSenseOrder() != null ? request.getSenseOrder() : 0);
        dto.setStructures(toStructureDtoList(request.getStructures()));
        return dto;
    }

    private List<VocabStructureDto> toStructureDtoList(List<VocabWordCreateRequest.VocabStructureRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toStructureDto).collect(Collectors.toList());
    }

    private VocabStructureDto toStructureDto(VocabWordCreateRequest.VocabStructureRequest request) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(request.getId());
        dto.setPattern(request.getPattern());
        dto.setStructureOrder(request.getStructureOrder() != null ? request.getStructureOrder() : 0);
        dto.setExamples(toExampleDtoList(request.getExamples()));
        return dto;
    }

    private List<VocabExerciseDto> toExerciseDtoList(List<VocabWordCreateRequest.VocabExerciseRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toExerciseDto).collect(Collectors.toList());
    }

    private VocabExerciseDto toExerciseDto(VocabWordCreateRequest.VocabExerciseRequest request) {
        VocabExerciseDto dto = new VocabExerciseDto();
        dto.setId(request.getId());
        dto.setQuestionType(request.getQuestionType());
        dto.setQuestionText(request.getQuestionText());
        dto.setOptions(toExerciseOptionList(request.getOptions()));
        dto.setAnswers(request.getAnswers());
        dto.setExerciseOrder(request.getExerciseOrder() != null ? request.getExerciseOrder() : 0);
        return dto;
    }

    private List<ExerciseOption> toExerciseOptionList(List<ExerciseOptionRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toExerciseOption).collect(Collectors.toList());
    }

    private ExerciseOption toExerciseOption(ExerciseOptionRequest request) {
        if (request == null) {
            return null;
        }
        ExerciseOption option = new ExerciseOption();
        option.setOption(request.getOption());
        option.setText(request.getText());
        return option;
    }

    private List<VocabExampleDto> toExampleDtoList(List<VocabWordCreateRequest.VocabExampleRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toExampleDto).collect(Collectors.toList());
    }

    private VocabExampleDto toExampleDto(VocabWordCreateRequest.VocabExampleRequest request) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(request.getId());
        dto.setSentence(request.getSentence());
        dto.setAudioId(request.getAudioId());
        dto.setPinyin(request.getPinyin());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setExampleOrder(request.getExampleOrder() != null ? request.getExampleOrder() : 0);
        return dto;
    }

    private List<VocabWordBaseVO> toBaseVOList(List<VocabWordDto> resources) {
        return resources.stream().map(this::toBaseVO).collect(Collectors.toList());
    }

    private VocabWordBaseVO toBaseVO(VocabWordDto dto) {
        VocabWordBaseVO vo = new VocabWordBaseVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setHskLevel(dto.getHskLevel());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private VocabWordVO toVO(VocabWordDto dto) {
        VocabWordVO vo = new VocabWordVO();
        vo.setId(dto.getId());
        vo.setWord(dto.getWord());
        vo.setWordTraditional(dto.getWordTraditional());
        vo.setPinyin(dto.getPinyin());
        vo.setAudioId(dto.getAudioId());
        vo.setHskLevel(dto.getHskLevel());
        vo.setSenses(toSenseVOList(dto.getSenses()));
        vo.setExercises(toExerciseVOList(dto.getExercises()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<VocabWordVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toSenseVO).collect(Collectors.toList());
    }

    private VocabWordVO.VocabSenseVO toSenseVO(VocabSenseDto dto) {
        VocabWordVO.VocabSenseVO vo = new VocabWordVO.VocabSenseVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        vo.setDefAudioId(dto.getDefAudioId());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setSynonyms(dto.getSynonyms());
        vo.setAntonyms(dto.getAntonyms());
        vo.setRelatedForward(dto.getRelatedForward());
        vo.setRelatedBackward(dto.getRelatedBackward());
        vo.setSenseOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<VocabWordVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toStructureVO).collect(Collectors.toList());
    }

    private VocabWordVO.VocabStructureVO toStructureVO(VocabStructureDto dto) {
        VocabWordVO.VocabStructureVO vo = new VocabWordVO.VocabStructureVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setSenseId(dto.getSenseId());
        vo.setPattern(dto.getPattern());
        vo.setStructureOrder(dto.getStructureOrder());
        vo.setExamples(toExampleVOList(dto.getExamples()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<VocabWordVO.VocabExerciseVO> toExerciseVOList(List<VocabExerciseDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toExerciseVO).collect(Collectors.toList());
    }

    private VocabWordVO.VocabExerciseVO toExerciseVO(VocabExerciseDto dto) {
        VocabWordVO.VocabExerciseVO vo = new VocabWordVO.VocabExerciseVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setQuestionType(dto.getQuestionType());
        vo.setQuestionText(dto.getQuestionText());
        vo.setOptions(toExerciseOptionVOList(dto.getOptions()));
        vo.setAnswers(dto.getAnswers());
        vo.setExerciseOrder(dto.getExerciseOrder());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<ExerciseOptionVO> toExerciseOptionVOList(List<ExerciseOption> options) {
        if (options == null) {
            return Collections.emptyList();
        }
        return options.stream().map(this::toExerciseOptionVO).collect(Collectors.toList());
    }

    private ExerciseOptionVO toExerciseOptionVO(ExerciseOption option) {
        if (option == null) {
            return null;
        }
        ExerciseOptionVO vo = new ExerciseOptionVO();
        vo.setOption(option.getOption());
        vo.setText(option.getText());
        return vo;
    }

    private List<VocabWordVO.VocabExampleVO> toExampleVOList(List<VocabExampleDto> resources) {
        if (resources == null) {
            return Collections.emptyList();
        }
        return resources.stream().map(this::toExampleVO).collect(Collectors.toList());
    }

    private VocabWordVO.VocabExampleVO toExampleVO(VocabExampleDto dto) {
        VocabWordVO.VocabExampleVO vo = new VocabWordVO.VocabExampleVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setSenseId(dto.getSenseId());
        vo.setStructureId(dto.getStructureId());
        vo.setSentence(dto.getSentence());
        vo.setAudioId(dto.getAudioId());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setExampleOrder(dto.getExampleOrder());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    private List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }
        return requests.stream().map(this::toTextTranslation).collect(Collectors.toList());
    }

    private TextTranslation toTextTranslation(TextTranslationRequest request) {
        if (request == null) {
            return null;
        }
        TextTranslation translation = new TextTranslation();
        translation.setLanguage(request.getLanguage());
        translation.setTranslation(request.getTranslation());
        return translation;
    }

    private List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> translations) {
        if (translations == null) {
            return Collections.emptyList();
        }
        return translations.stream().map(this::toTextTranslationVO).collect(Collectors.toList());
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
}
```

- [ ] **Step 5: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/VocabWordController.java
git commit -m "feat: add vocab outline record admin APIs"
```

---

### Task 10: 修改 AppVocabWordController 集成记录逻辑

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java`

- [ ] **Step 1: 先读取现有 Controller**

Read: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java`

- [ ] **Step 2: 添加必要的 import 和注入**

Add this import:
```java
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
```

Add this field:
```java
private final VocabOutlineRecordService vocabOutlineRecordService;
```

Update the constructor to include this parameter.

- [ ] **Step 3: 修改 search 方法**

Update the `search` method to record outline word when results are empty:

```java
    @ApiOperation("搜索词汇")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppVocabWordBaseVO>> search(AppVocabWordSearchRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "id"));
        List<VocabWordDto> dtos = vocabWordService.queryAll(criteria, pageable).getContent();
        List<AppVocabWordBaseVO> vos = toBaseVOList(dtos);

        // 如果搜索结果为空，记录纲外词
        if (vos.isEmpty()) {
            vocabOutlineRecordService.recordIfNeeded(request.getBlurry());
        }

        return new ResponseEntity<>(vos, HttpStatus.OK);
    }
```

- [ ] **Step 4: 完整的修改后文件内容**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.backend.rest.vo.ExerciseOptionVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.vocabulary.VocabOutlineRecordService;
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
import org.springframework.web.bind.annotation.*;

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
    private final VocabOutlineRecordService vocabOutlineRecordService;

    @ApiOperation("搜索词汇")
    @AnonymousGetMapping("/search")
    public ResponseEntity<List<AppVocabWordBaseVO>> search(AppVocabWordSearchRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "id"));
        List<VocabWordDto> dtos = vocabWordService.queryAll(criteria, pageable).getContent();
        List<AppVocabWordBaseVO> vos = toBaseVOList(dtos);

        // 如果搜索结果为空，记录纲外词
        if (vos.isEmpty()) {
            vocabOutlineRecordService.recordIfNeeded(request.getBlurry());
        }

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

- [ ] **Step 5: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java
git commit -m "feat: integrate vocab outline record in search API"
```

---

## 计划完成

**Spec 覆盖率检查:**
- ✅ 数据库表设计 - Task 1
- ✅ 实体类 - Task 2
- ✅ Repository - Task 3
- ✅ DTO/VO - Task 4, 5
- ✅ Service 接口和实现 - Task 7, 8
- ✅ 搜索词过滤逻辑 - Task 8
- ✅ 并发安全的 upsert - Task 8
- ✅ 后台管理接口 - Task 9
- ✅ 用户搜索接口集成 - Task 10

**无占位符检查:** 所有步骤都包含完整的代码和命令

**类型一致性检查:** 所有类型、方法名、参数名在各任务中保持一致

---

Plan complete and saved to `docs/superpowers/plans/2026-05-31-vocab-outline-record.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
