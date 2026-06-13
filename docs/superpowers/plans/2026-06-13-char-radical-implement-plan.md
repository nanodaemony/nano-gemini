# 汉字部首后台管理功能 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 grid-system 模块中实现汉字部首（char_radical）后台管理功能，包括完整草稿→审核→发布工作流、分页查询、软删除和下线。

**Architecture:** 单表实体（无子表），使用 Spring Data JPA + Specification 分页查询，草稿内容以 JSON 存入 `draft_content` 字段。发布时将 draft JSON 回写主表字段后清空 draft。继承 `BaseEntity` 获取审计字段。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MySQL, Lombok, Fastjson2, Knife4j(Swagger)

---

## 文件清单

新建 12 个 Java 文件 + 1 个 SQL 文件（共 13 个文件）：

| # | 文件 | 包 |
|---|------|----|
| 1 | `sql/char_radical.sql` | — |
| 2 | `CharRadical.java` | `.../domain/charradical/` |
| 3 | `CharRadicalRepository.java` | `.../repo/charradical/` |
| 4 | `CharRadicalDto.java` | `.../service/charradical/dto/` |
| 5 | `CharRadicalQueryCriteria.java` | `.../service/charradical/dto/` |
| 6 | `CharRadicalService.java` | `.../service/charradical/` |
| 7 | `CharRadicalServiceImpl.java` | `.../service/charradical/impl/` |
| 8 | `CharRadicalUpdateRequest.java` | `.../rest/request/` |
| 9 | `CharRadicalQueryRequest.java` | `.../rest/request/` |
| 10 | `CharRadicalVO.java` | `.../rest/vo/` |
| 11 | `CharRadicalBaseVO.java` | `.../rest/vo/` |
| 12 | `CharRadicalWrapper.java` | `.../rest/wrapper/` |
| 13 | `CharRadicalController.java` | `.../rest/controller/` |

---

### Task 1: SQL 迁移脚本 + DO 实体 + Repository

**Files:**
- Create: `sql/char_radical.sql`
- Create: `grid-system/src/main/java/com/naon/grid/backend/domain/charradical/CharRadical.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/repo/charradical/CharRadicalRepository.java`

#### Step 1.1: 创建 SQL 迁移脚本

`sql/char_radical.sql`:
```sql
-- 汉字部首表
CREATE TABLE IF NOT EXISTS `char_radical` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部首ID',
    `radical` VARCHAR(10) NOT NULL COMMENT '部首名称',
    `stroke_num` int(11) NOT NULL COMMENT '笔画数',
    `evolution_desc` VARCHAR(2048) DEFAULT NULL COMMENT '演化解说',
    `evolution_desc_translations` TEXT DEFAULT NULL COMMENT '演化解说外文翻译（JSON多语言）',
    `evolution_image_id` VARCHAR(255) DEFAULT NULL COMMENT '演化解说图片（路径或资源ID）',
    `draft_content` text NULL COMMENT '草稿内容JSON',
    `create_by` varchar(255) NULL DEFAULT NULL COMMENT '创建人',
    `update_by` varchar(255) NULL DEFAULT NULL COMMENT '更新人',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `edit_status` varchar(20) NULL DEFAULT 'draft' COMMENT '编辑状态: draft=草稿, reviewed=已审核',
    `publish_status` varchar(20) NULL DEFAULT 'unpublished' COMMENT '发布状态: unpublished=未发布, published=已发布',
    `status` tinyint NOT NULL DEFAULT 1 COMMENT '有效状态, 1:有效 0:无效',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='汉字部首表';
```

#### Step 1.2: 创建 DO 实体

`grid-system/.../domain/charradical/CharRadical.java`:
```java
package com.naon.grid.backend.domain.charradical;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "char_radical")
public class CharRadical extends BaseEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "部首ID", hidden = true)
    private Long id;

    @Column(name = "radical", nullable = false, length = 10)
    @ApiModelProperty(value = "部首名称")
    private String radical;

    @Column(name = "stroke_num", nullable = false)
    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @Column(name = "evolution_desc", length = 2048)
    @ApiModelProperty(value = "演化解说")
    private String evolutionDesc;

    @Column(name = "evolution_desc_translations", columnDefinition = "text")
    @ApiModelProperty(value = "演化解说外文翻译（JSON多语言）")
    private String evolutionDescTranslations;

    @Column(name = "evolution_image_id", length = 255)
    @ApiModelProperty(value = "演化解说图片（路径或资源ID）")
    private String evolutionImageId;

    @Column(name = "draft_content", columnDefinition = "text")
    @ApiModelProperty(value = "草稿内容JSON")
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

#### Step 1.3: 创建 Repository

`grid-system/.../repo/charradical/CharRadicalRepository.java`:
```java
package com.naon.grid.backend.repo.charradical;

import com.naon.grid.backend.domain.charradical.CharRadical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CharRadicalRepository extends JpaRepository<CharRadical, Long>,
        JpaSpecificationExecutor<CharRadical> {
}
```

#### Step 1.4: Commit

```bash
git add sql/char_radical.sql \
  grid-system/src/main/java/com/naon/grid/backend/domain/charradical/CharRadical.java \
  grid-system/src/main/java/com/naon/grid/backend/repo/charradical/CharRadicalRepository.java
git commit -m "feat: add char_radical DO entity, repository and SQL migration

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: DTO + QueryCriteria

**Files:**
- Create: `grid-system/.../service/charradical/dto/CharRadicalDto.java`
- Create: `grid-system/.../service/charradical/dto/CharRadicalQueryCriteria.java`

#### Step 2.1: 创建 DTO

`grid-system/.../service/charradical/dto/CharRadicalDto.java`:
```java
package com.naon.grid.backend.service.charradical.dto;

import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CharRadicalDto extends BaseDTO {

    @ApiModelProperty(value = "部首ID")
    private Long id;

    @ApiModelProperty(value = "部首名称")
    private String radical;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "演化解说")
    private String evolutionDesc;

    @ApiModelProperty(value = "演化解说外文翻译")
    private List<TextTranslation> evolutionDescTranslations;

    @ApiModelProperty(value = "演化解说图片（路径或资源ID）")
    private String evolutionImageId;

    @ApiModelProperty(value = "草稿内容JSON")
    private String draftContent;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;

    @ApiModelProperty(value = "发布状态: unpublished=未发布, published=已发布")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft=草稿, reviewed=已审核, published=已发布")
    private String editStatus;
}
```

#### Step 2.2: 创建 QueryCriteria

`grid-system/.../service/charradical/dto/CharRadicalQueryCriteria.java`:
```java
package com.naon.grid.backend.service.charradical.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharRadicalQueryCriteria implements Serializable {

    @ApiModelProperty(value = "部首名称模糊查询")
    @Query(blurry = "radical")
    private String blurry;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    @Query
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    @Query
    private String editStatus;
}
```

#### Step 2.3: Commit

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/charradical/dto/CharRadicalDto.java \
  grid-system/src/main/java/com/naon/grid/backend/service/charradical/dto/CharRadicalQueryCriteria.java
git commit -m "feat: add CharRadicalDto and QueryCriteria

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: Service 接口 + 实现

**Files:**
- Create: `grid-system/.../service/charradical/CharRadicalService.java`
- Create: `grid-system/.../service/charradical/impl/CharRadicalServiceImpl.java`

#### Step 3.1: 创建 Service 接口

`grid-system/.../service/charradical/CharRadicalService.java`:
```java
package com.naon.grid.backend.service.charradical;

import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.backend.service.charradical.dto.CharRadicalQueryCriteria;
import com.naon.grid.utils.PageResult;
import org.springframework.data.domain.Pageable;

public interface CharRadicalService {

    PageResult<CharRadicalDto> queryAll(CharRadicalQueryCriteria criteria, Pageable pageable);

    CharRadicalDto findById(Long id);

    void update(Long id, CharRadicalDto resources);

    void delete(Long id);

    void reviewDraft(Long id);

    void publishDraft(Long id);

    void offline(Long id);
}
```

#### Step 3.2: 创建 Service 实现

`grid-system/.../service/charradical/impl/CharRadicalServiceImpl.java`:
```java
package com.naon.grid.backend.service.charradical.impl;

import com.naon.grid.backend.domain.charradical.CharRadical;
import com.naon.grid.backend.repo.charradical.CharRadicalRepository;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.backend.service.charradical.dto.CharRadicalQueryCriteria;
import com.naon.grid.enums.EditStatusEnum;
import com.naon.grid.enums.PublishStatusEnum;
import com.naon.grid.enums.StatusEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.exception.EntityNotFoundException;
import com.naon.grid.utils.JsonUtils;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class CharRadicalServiceImpl implements CharRadicalService {

    private final CharRadicalRepository charRadicalRepository;

    @Override
    public PageResult<CharRadicalDto> queryAll(CharRadicalQueryCriteria criteria, Pageable pageable) {
        Page<CharRadical> page = charRadicalRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        return PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CharRadicalDto findById(Long id) {
        CharRadical entity = charRadicalRepository.findById(id).orElse(null);
        if (entity == null || StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            if (entity.getDraftContent() == null) {
                throw new BadRequestException("草稿内容不存在");
            }
            CharRadicalDto dto;
            try {
                dto = JsonUtils.fromJson(entity.getDraftContent(), CharRadicalDto.class);
            } catch (Exception e) {
                throw new BadRequestException("草稿数据解析失败");
            }
            if (dto == null) {
                throw new BadRequestException("草稿内容不存在");
            }
            dto.setId(entity.getId());
            dto.setStatus(entity.getStatus());
            dto.setPublishStatus(entity.getPublishStatus());
            dto.setEditStatus(entity.getEditStatus());
            dto.setCreateTime(entity.getCreateTime());
            dto.setUpdateTime(entity.getUpdateTime());
            dto.setCreateBy(entity.getCreateBy());
            dto.setUpdateBy(entity.getUpdateBy());
            return dto;
        }

        return toBaseDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CharRadicalDto resources) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id));
        }

        if (EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.PUBLISHED.getCode().equals(entity.getEditStatus())) {
            entity.setEditStatus(EditStatusEnum.DRAFT.getCode());
        }

        entity.setDraftContent(JsonUtils.toJson(resources));
        charRadicalRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        entity.setStatus(StatusEnum.DISABLED.getCode());
        charRadicalRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDraft(Long id) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅草稿状态可审核");
        }
        entity.setEditStatus(EditStatusEnum.REVIEWED.getCode());
        charRadicalRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishDraft(Long id) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id));
        }
        if (entity.getDraftContent() == null) {
            throw new BadRequestException("草稿不存在");
        }
        if (!EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            throw new BadRequestException("仅已审核状态可发布");
        }

        CharRadicalDto draftDto;
        try {
            draftDto = JsonUtils.fromJson(entity.getDraftContent(), CharRadicalDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draftDto == null) {
            throw new BadRequestException("草稿数据解析失败");
        }

        // 回写主表字段
        entity.setRadical(draftDto.getRadical());
        entity.setStrokeNum(draftDto.getStrokeNum());
        entity.setEvolutionDesc(draftDto.getEvolutionDesc());
        entity.setEvolutionDescTranslations(JsonUtils.toTranslationJson(draftDto.getEvolutionDescTranslations()));
        entity.setEvolutionImageId(draftDto.getEvolutionImageId());

        // 更新状态，清除草稿
        entity.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        entity.setEditStatus(EditStatusEnum.PUBLISHED.getCode());
        entity.setDraftContent(null);
        charRadicalRepository.save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        CharRadical entity = charRadicalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id)));
        if (StatusEnum.DISABLED.getCode().equals(entity.getStatus())) {
            throw new EntityNotFoundException(CharRadical.class, "id", String.valueOf(id));
        }
        entity.setPublishStatus(PublishStatusEnum.UNPUBLISHED.getCode());
        charRadicalRepository.save(entity);
    }

    // ==================== Private Helper Methods ====================

    private CharRadicalDto toBaseDto(CharRadical entity) {
        CharRadicalDto dto = new CharRadicalDto();
        dto.setId(entity.getId());
        dto.setRadical(entity.getRadical());
        dto.setStrokeNum(entity.getStrokeNum());
        dto.setEvolutionDesc(entity.getEvolutionDesc());
        dto.setEvolutionDescTranslations(JsonUtils.parseTranslationList(entity.getEvolutionDescTranslations()));
        dto.setEvolutionImageId(entity.getEvolutionImageId());
        dto.setStatus(entity.getStatus());
        dto.setPublishStatus(entity.getPublishStatus());
        dto.setEditStatus(entity.getEditStatus());
        dto.setDraftContent(entity.getDraftContent());
        dto.setCreateBy(entity.getCreateBy());
        dto.setUpdateBy(entity.getUpdateBy());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    private CharRadicalDto toDtoWithDraftOverlay(CharRadical entity) {
        CharRadicalDto dto = toBaseDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    private void applyDraftOverlay(CharRadicalDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        CharRadicalDto draft;
        try {
            draft = JsonUtils.fromJson(draftJson, CharRadicalDto.class);
        } catch (Exception e) {
            throw new BadRequestException("草稿数据解析失败");
        }
        if (draft == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        if (draft.getRadical() != null)                dto.setRadical(draft.getRadical());
        if (draft.getStrokeNum() != null)              dto.setStrokeNum(draft.getStrokeNum());
        if (draft.getEvolutionDesc() != null)          dto.setEvolutionDesc(draft.getEvolutionDesc());
        if (draft.getEvolutionDescTranslations() != null) dto.setEvolutionDescTranslations(draft.getEvolutionDescTranslations());
        if (draft.getEvolutionImageId() != null)       dto.setEvolutionImageId(draft.getEvolutionImageId());
    }
}
```

#### Step 3.3: Commit

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/charradical/CharRadicalService.java \
  grid-system/src/main/java/com/naon/grid/backend/service/charradical/impl/CharRadicalServiceImpl.java
git commit -m "feat: add CharRadicalService interface and implementation

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: Request + VO 类

**Files:**
- Create: `grid-system/.../rest/request/CharRadicalUpdateRequest.java`
- Create: `grid-system/.../rest/request/CharRadicalQueryRequest.java`
- Create: `grid-system/.../rest/vo/CharRadicalVO.java`
- Create: `grid-system/.../rest/vo/CharRadicalBaseVO.java`

#### Step 4.1: 创建 UpdateRequest

`grid-system/.../rest/request/CharRadicalUpdateRequest.java`:
```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class CharRadicalUpdateRequest {

    @ApiModelProperty(value = "部首名称")
    private String radical;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "演化解说")
    private String evolutionDesc;

    @ApiModelProperty(value = "演化解说外文翻译")
    private List<TextTranslationRequest> evolutionDescTranslations;

    @ApiModelProperty(value = "演化解说图片（路径或资源ID）")
    private String evolutionImageId;
}
```

#### Step 4.2: 创建 QueryRequest

`grid-system/.../rest/request/CharRadicalQueryRequest.java`:
```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharRadicalQueryRequest implements Serializable {

    @ApiModelProperty(value = "部首名称模糊查询")
    private String blurry;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;
}
```

#### Step 4.3: 创建详情 VO

`grid-system/.../rest/vo/CharRadicalVO.java`:
```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class CharRadicalVO {

    @ApiModelProperty(value = "部首ID")
    private Long id;

    @ApiModelProperty(value = "部首名称")
    private String radical;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "演化解说")
    private String evolutionDesc;

    @ApiModelProperty(value = "演化解说外文翻译")
    private List<TextTranslationVO> evolutionDescTranslations;

    @ApiModelProperty(value = "演化解说图片（路径或资源ID）")
    private String evolutionImageId;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

#### Step 4.4: 创建列表 BaseVO

`grid-system/.../rest/vo/CharRadicalBaseVO.java`:
```java
package com.naon.grid.backend.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class CharRadicalBaseVO {

    @ApiModelProperty(value = "部首ID")
    private Long id;

    @ApiModelProperty(value = "部首名称")
    private String radical;

    @ApiModelProperty(value = "笔画数")
    private Integer strokeNum;

    @ApiModelProperty(value = "演化解说图片（路径或资源ID）")
    private String evolutionImageId;

    @ApiModelProperty(value = "发布状态")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    private String editStatus;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "更新人")
    private String updateBy;

    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;
}
```

#### Step 4.5: Commit

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/CharRadicalUpdateRequest.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/request/CharRadicalQueryRequest.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharRadicalVO.java \
  grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharRadicalBaseVO.java
git commit -m "feat: add CharRadical request and VO classes

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: Wrapper 转换类

**Files:**
- Create: `grid-system/.../rest/wrapper/CharRadicalWrapper.java`

#### Step 5.1: 创建 Wrapper

`grid-system/.../rest/wrapper/CharRadicalWrapper.java`:
```java
package com.naon.grid.backend.rest.wrapper;

import com.naon.grid.backend.rest.request.CharRadicalQueryRequest;
import com.naon.grid.backend.rest.request.CharRadicalUpdateRequest;
import com.naon.grid.backend.rest.request.TextTranslationRequest;
import com.naon.grid.backend.rest.vo.CharRadicalBaseVO;
import com.naon.grid.backend.rest.vo.CharRadicalVO;
import com.naon.grid.backend.rest.vo.TextTranslationVO;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.backend.service.charradical.dto.CharRadicalQueryCriteria;
import com.naon.grid.domain.common.TextTranslation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CharRadicalWrapper {

    // === Request → Criteria ===

    public static CharRadicalQueryCriteria toCriteria(CharRadicalQueryRequest request) {
        if (request == null) return null;
        CharRadicalQueryCriteria criteria = new CharRadicalQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }

    // === Request → Dto ===

    public static CharRadicalDto toDto(CharRadicalUpdateRequest request) {
        if (request == null) return null;
        CharRadicalDto dto = new CharRadicalDto();
        dto.setRadical(request.getRadical());
        dto.setStrokeNum(request.getStrokeNum());
        dto.setEvolutionDesc(request.getEvolutionDesc());
        dto.setEvolutionDescTranslations(toTextTranslationList(request.getEvolutionDescTranslations()));
        dto.setEvolutionImageId(request.getEvolutionImageId());
        return dto;
    }

    // === Dto → VO ===

    public static CharRadicalVO toVO(CharRadicalDto dto) {
        if (dto == null) return null;
        CharRadicalVO vo = new CharRadicalVO();
        vo.setId(dto.getId());
        vo.setRadical(dto.getRadical());
        vo.setStrokeNum(dto.getStrokeNum());
        vo.setEvolutionDesc(dto.getEvolutionDesc());
        vo.setEvolutionDescTranslations(toTextTranslationVOList(dto.getEvolutionDescTranslations()));
        vo.setEvolutionImageId(dto.getEvolutionImageId());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static CharRadicalBaseVO toBaseVO(CharRadicalDto dto) {
        if (dto == null) return null;
        CharRadicalBaseVO vo = new CharRadicalBaseVO();
        vo.setId(dto.getId());
        vo.setRadical(dto.getRadical());
        vo.setStrokeNum(dto.getStrokeNum());
        vo.setEvolutionImageId(dto.getEvolutionImageId());
        vo.setPublishStatus(dto.getPublishStatus());
        vo.setEditStatus(dto.getEditStatus());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }

    public static List<CharRadicalBaseVO> toBaseVOList(List<CharRadicalDto> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(CharRadicalWrapper::toBaseVO).collect(Collectors.toList());
    }

    // === TextTranslation 转换工具 ===

    private static List<TextTranslation> toTextTranslationList(List<TextTranslationRequest> requests) {
        if (requests == null) return Collections.emptyList();
        return requests.stream().map(CharRadicalWrapper::toTextTranslation).collect(Collectors.toList());
    }

    private static TextTranslation toTextTranslation(TextTranslationRequest request) {
        if (request == null) return null;
        TextTranslation t = new TextTranslation();
        t.setLanguage(request.getLanguage());
        t.setTranslation(request.getTranslation());
        return t;
    }

    private static List<TextTranslationVO> toTextTranslationVOList(List<TextTranslation> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(CharRadicalWrapper::toTextTranslationVO).collect(Collectors.toList());
    }

    private static TextTranslationVO toTextTranslationVO(TextTranslation t) {
        if (t == null) return null;
        TextTranslationVO vo = new TextTranslationVO();
        vo.setLanguage(t.getLanguage());
        vo.setTranslation(t.getTranslation());
        return vo;
    }
}
```

#### Step 5.2: Commit

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharRadicalWrapper.java
git commit -m "feat: add CharRadicalWrapper for DTO/VO conversion

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: Controller 控制器

**Files:**
- Create: `grid-system/.../rest/controller/CharRadicalController.java`

#### Step 6.1: 创建 Controller

`grid-system/.../rest/controller/CharRadicalController.java`:
```java
package com.naon.grid.backend.rest.controller;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousDeleteMapping;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.backend.rest.request.CharRadicalQueryRequest;
import com.naon.grid.backend.rest.request.CharRadicalUpdateRequest;
import com.naon.grid.backend.rest.vo.CharRadicalBaseVO;
import com.naon.grid.backend.rest.vo.CharRadicalVO;
import com.naon.grid.backend.rest.wrapper.CharRadicalWrapper;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
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

@RestController
@RequiredArgsConstructor
@Api(tags = "后台：汉字-部首管理")
@RequestMapping("/api/char/radical")
public class CharRadicalController {

    private final CharRadicalService charRadicalService;

    @Log("修改部首")
    @ApiOperation("修改部首")
    @AnonymousPutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody CharRadicalUpdateRequest request) {
        charRadicalService.update(id, CharRadicalWrapper.toDto(request));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("部首草稿审核通过")
    @ApiOperation("部首草稿审核通过（草稿→已审核）")
    @AnonymousPutMapping("/{id}/review")
    public ResponseEntity<Void> reviewDraft(@PathVariable Long id) {
        charRadicalService.reviewDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("发布部首")
    @ApiOperation("发布部首（已审核→已发布）")
    @AnonymousPutMapping("/{id}/publish")
    public ResponseEntity<Void> publishDraft(@PathVariable Long id) {
        charRadicalService.publishDraft(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("查询部首详情")
    @ApiOperation("根据ID查询部首详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<CharRadicalVO> findById(@PathVariable Long id) {
        return new ResponseEntity<>(
                CharRadicalWrapper.toVO(charRadicalService.findById(id)), HttpStatus.OK);
    }

    @Log("查询部首列表")
    @ApiOperation("分页查询部首列表")
    @AnonymousGetMapping
    public ResponseEntity<PageResult<CharRadicalBaseVO>> queryAll(
            CharRadicalQueryRequest request, Pageable pageable) {
        PageResult<CharRadicalDto> pageResult =
                charRadicalService.queryAll(CharRadicalWrapper.toCriteria(request), pageable);
        return new ResponseEntity<>(
                new PageResult<>(CharRadicalWrapper.toBaseVOList(pageResult.getContent()),
                        pageResult.getTotalElements()), HttpStatus.OK);
    }

    @Log("删除部首")
    @ApiOperation("删除部首")
    @AnonymousDeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        charRadicalService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Log("下线部首")
    @ApiOperation("下线部首")
    @AnonymousPutMapping("/{id}/offline")
    public ResponseEntity<Void> offline(@PathVariable Long id) {
        charRadicalService.offline(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
```

#### Step 6.2: 编译验证

```bash
cd grid-bootstrap
mvn compile -q
```

Expected: `BUILD SUCCESS` — 确认无编译错误。

#### Step 6.3: Commit

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/controller/CharRadicalController.java
git commit -m "feat: add CharRadicalController with CRUD + publish workflow

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 验证清单

- [ ] `mvn compile -q` 编译通过
- [ ] 启动应用，访问 Swagger http://localhost:8000/doc.html
- [ ] Swagger 中看到 "后台：汉字-部首管理" API 组
- [ ] 通过 `/api/char/radical` GET 分页查询到部首列表（需先导入数据）
- [ ] 通过 `/api/char/radical/{id}` PUT 更新部首（进入草稿状态）
- [ ] 通过 `/api/char/radical/{id}/review` PUT 审核草稿
- [ ] 通过 `/api/char/radical/{id}/publish` PUT 发布
- [ ] 通过 `/api/char/radical/{id}/offline` PUT 下线
- [ ] 通过 `/api/char/radical/{id}` DELETE 软删除
