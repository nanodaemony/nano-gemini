# 词汇与汉字后台列表页：草稿回填与状态筛选 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让后台 `GET /api/vocabulary` 与 `GET /api/character` 两个列表页支持按发布状态 / 编辑状态精确筛选，并对草稿态行从 `draftContent` 显式覆盖业务字段，使列表展示"最新内容"。

**Architecture:** 自底向上修改两套对称的 5 个层（Request → Criteria → Wrapper → BaseVO → ServiceImpl），共 10 个 Java 文件。过滤条件透传依赖既有 `QueryHelp` + `@Query(EQUAL)` 机制，无需新代码。草稿回填集中在两个 ServiceImpl 各自的私有方法 `applyDraftOverlay` 中，按 `XxxBaseVO` 暴露字段集逐字段覆盖。

**Tech Stack:** Spring Boot 2.7.18 + Spring Data JPA + Lombok + Swagger (Knife4j) + Fastjson2 + Maven。无新增依赖。

**关联 Spec:** `docs/superpowers/specs/2026-06-07-vocab-char-queryAll-draft-overlay-design.md`

---

## File Structure

本计划共改 **10 个 Java 文件**，不新增任何文件、无数据库 DDL：

### 词汇侧（5 个文件）

| 文件 | 责任 | 改动量 |
|--|--|--|
| `grid-system/.../rest/request/VocabWordQueryRequest.java` | 列表入参 | +2 字段 |
| `grid-system/.../service/vocabulary/dto/VocabWordQueryCriteria.java` | Service 查询条件 | +1 字段（`publishStatus` 已有） |
| `grid-system/.../rest/wrapper/VocabWordWrapper.java` | Request→Criteria 透传 + 列表 VO 装配 | +2 行透传，-1 行 setHasDraft |
| `grid-system/.../rest/vo/VocabWordBaseVO.java` | 列表 VO | -1 字段（`hasDraft`） |
| `grid-system/.../service/vocabulary/impl/VocabWordServiceImpl.java` | 业务实现 | `queryAll` 改 1 行 + 2 个私有方法 + 1 行 import |

### 汉字侧（5 个文件，完全对称）

| 文件 | 责任 | 改动量 |
|--|--|--|
| `grid-system/.../rest/request/CharCharacterQueryRequest.java` | 列表入参 | +2 字段 |
| `grid-system/.../service/character/dto/CharCharacterQueryCriteria.java` | Service 查询条件 | +1 字段（`publishStatus` 已有） |
| `grid-system/.../rest/wrapper/CharCharacterWrapper.java` | Request→Criteria 透传 + 列表 VO 装配 | +2 行透传，-1 行 setHasDraft |
| `grid-system/.../rest/vo/CharCharacterBaseVO.java` | 列表 VO | -1 字段（`hasDraft`） |
| `grid-system/.../service/character/impl/CharCharacterServiceImpl.java` | 业务实现 | `queryAll` 改 1 行 + 2 个私有方法 |

**实施顺序**：词汇侧先做完 5 个 task 走通一遍，再把汉字侧 5 个 task 1:1 镜像做完。每个 task 结束都 `mvn -pl grid-system -am compile -q` 验证，独立 commit。最后第 11 个 task 整体编译 + Knife4j 手动验证 + 总 commit。

> **TDD 说明**：本项目 `pom.xml` 默认 `-DskipTests`，仓库中没有对 Service / Controller 的单元测试基础设施（参考最近的 plan `2026-06-07-char-subtable-order.md` 同样无测试）。验证手段为**编译通过** + **最终 Bootstrap 启动 + Knife4j 调用接口的手动验证**。每个 task 都给出具体编译命令与预期输出。

---

## Task 1: 词汇 Request 层 — VocabWordQueryRequest 加 publishStatus / editStatus

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordQueryRequest.java`

- [ ] **Step 1: 用以下完整内容替换文件**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;

@Data
public class VocabWordQueryRequest implements Serializable {

    @ApiModelProperty(value = "词汇模糊查询")
    private String blurry;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;
}
```

- [ ] **Step 2: 编译验证**

Run（Windows bash，路径用正斜杠）:
```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功，无 `BUILD FAILURE` 行。

- [ ] **Step 3: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordQueryRequest.java
git commit -m "feat(vocab): add publishStatus and editStatus to VocabWordQueryRequest"
```

---

## Task 2: 词汇 Criteria 层 — VocabWordQueryCriteria 加 editStatus

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordQueryCriteria.java`

> 当前文件 17 行 `private String publishStatus;`，第 18 行 `}`。仅需在 `publishStatus` 后面新增 `editStatus`。

- [ ] **Step 1: 用以下完整内容替换文件**

```java
package com.naon.grid.backend.service.vocabulary.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import com.naon.grid.annotation.Query;
import java.io.Serializable;

@Data
public class VocabWordQueryCriteria implements Serializable {

    @ApiModelProperty(value = "词汇模糊查询")
    @Query(blurry = "word")
    private String blurry;

    @ApiModelProperty(value = "发布状态")
    @Query
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    @Query
    private String editStatus;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 3: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordQueryCriteria.java
git commit -m "feat(vocab): add editStatus filter field to VocabWordQueryCriteria"
```

---

## Task 3: 词汇 Wrapper — toCriteria 透传两字段 + toBaseVO 移除 hasDraft

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java`

- [ ] **Step 1: 修改 `toCriteria` 方法（28-32 行）**

把原方法体：

```java
    public static VocabWordQueryCriteria toCriteria(VocabWordQueryRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        return criteria;
    }
```

替换为：

```java
    public static VocabWordQueryCriteria toCriteria(VocabWordQueryRequest request) {
        VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }
```

- [ ] **Step 2: 修改 `toBaseVO` 方法（157 行），移除 setHasDraft 那一行**

把第 157 行：

```java
        vo.setHasDraft(dto.getDraftContent() != null);
```

**删除**。`toBaseVO` 的其余 setter 全部保留不变。

> 注意：`toVO` 方法中的第 175 行 `vo.setHasDraft(dto.getDraftContent() != null);` **保留**，本次不动详情 VO。

- [ ] **Step 3: 编译验证**

```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 4: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java
git commit -m "feat(vocab): pass status filters in toCriteria; drop hasDraft from list VO"
```

---

## Task 4: 词汇 BaseVO — 移除 hasDraft 字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java`

- [ ] **Step 1: 删除 hasDraft 字段及其注解（38-39 行）**

把这两行：

```java
    @ApiModelProperty(value = "是否有草稿")
    private Boolean hasDraft;
```

整段**删除**（包括其前后空行处理保持 VO 内部其它字段之间的空行风格一致）。改完后 `editStatus` 字段与 `createBy` 字段之间应只有一个空行。

- [ ] **Step 2: 编译验证**

```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。Task 3 已经把 `setHasDraft` 从 `toBaseVO` 移除，所以此时不会有"找不到方法"的报错。

- [ ] **Step 3: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java
git commit -m "feat(vocab): remove hasDraft field from VocabWordBaseVO"
```

---

## Task 5: 词汇 ServiceImpl — queryAll 接入草稿回填

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

> 当前 `queryAll`（38-46 行）：
> ```java
>     @Override
>     public PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable) {
>         Page<VocabWord> page = vocabWordRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
>             Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
>             Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
>             return criteriaBuilder.and(basePredicate, statusPredicate);
>         }, pageable);
>         return PageUtil.toPage(page.map(vocabWordMapper::toDto));
>     }
> ```

- [ ] **Step 1: 把 `queryAll` 最后一行 `page.map(vocabWordMapper::toDto)` 改为 `page.map(this::toDtoWithDraftOverlay)`**

修改后 `queryAll` 整体为：

```java
    @Override
    public PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable) {
        Page<VocabWord> page = vocabWordRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        return PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
    }
```

- [ ] **Step 2: 在 `queryAll` 方法正下方（46 行之后、`findById` 之前）插入两个私有方法**

```java
    /**
     * 主表实体 → DTO；若处于 draft/reviewed，将 draftContent 中的业务字段覆盖回 DTO。
     * 仅供 {@link #queryAll} 调用。
     */
    private VocabWordDto toDtoWithDraftOverlay(VocabWord entity) {
        VocabWordDto dto = vocabWordMapper.toDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    /**
     * 把 draftContent JSON 中"列表页需要的业务字段"覆盖到 DTO 上。
     *
     * 覆盖范围应与 {@link com.naon.grid.backend.rest.vo.VocabWordBaseVO} 暴露的业务字段保持一致。
     * BaseVO 新增业务字段时，请同步在此方法添加覆盖。
     *
     * 永远不覆盖：id、status、publishStatus、editStatus、createBy、updateBy、
     * createTime、updateTime —— 这些字段以主表为准。
     *
     * 不读取：senses、exercises —— 列表页不返回子表。
     *
     * @throws BadRequestException 草稿数据缺失或解析失败
     */
    private void applyDraftOverlay(VocabWordDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        VocabWordDto draft = JsonUtils.fromJson(draftJson, VocabWordDto.class);
        if (draft.getWord() != null)            dto.setWord(draft.getWord());
        if (draft.getWordTraditional() != null) dto.setWordTraditional(draft.getWordTraditional());
        if (draft.getPinyin() != null)          dto.setPinyin(draft.getPinyin());
        if (draft.getAudioId() != null)         dto.setAudioId(draft.getAudioId());
        if (draft.getHskLevel() != null)        dto.setHskLevel(draft.getHskLevel());
    }
```

> 说明：`BadRequestException`、`EditStatusEnum`、`JsonUtils` 已经在文件顶部 import 过（参见现有 `findById` / `update` 等方法），无需新增 import。

- [ ] **Step 3: 编译验证**

```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 4: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "feat(vocab): overlay draftContent onto list DTO for draft/reviewed rows"
```

---

## Task 6: 汉字 Request 层 — CharCharacterQueryRequest 加 publishStatus / editStatus

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterQueryRequest.java`

- [ ] **Step 1: 用以下完整内容替换文件**

```java
package com.naon.grid.backend.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharCharacterQueryRequest implements Serializable {

    @ApiModelProperty(value = "汉字或拼音模糊查询")
    private String blurry;

    @ApiModelProperty(value = "发布状态: unpublished / published")
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态: draft / reviewed / published")
    private String editStatus;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 3: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterQueryRequest.java
git commit -m "feat(char): add publishStatus and editStatus to CharCharacterQueryRequest"
```

---

## Task 7: 汉字 Criteria 层 — CharCharacterQueryCriteria 加 editStatus

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterQueryCriteria.java`

> 当前 21 行 `private String publishStatus;`，第 22 行 `}`。仅在 `publishStatus` 后面新增 `editStatus`，`searchCharacterOnly` 字段保持原位不动。

- [ ] **Step 1: 用以下完整内容替换文件**

```java
package com.naon.grid.backend.service.character.dto;

import com.naon.grid.annotation.Query;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class CharCharacterQueryCriteria implements Serializable {

    @ApiModelProperty(value = "汉字或拼音模糊查询")
    @Query(blurry = "character,pinyin")
    private String blurry;

    @ApiModelProperty(value = "是否仅搜索汉字字段（true=仅匹配character，false=匹配character和pinyin）")
    private Boolean searchCharacterOnly = false;

    @ApiModelProperty(value = "发布状态")
    @Query
    private String publishStatus;

    @ApiModelProperty(value = "编辑状态")
    @Query
    private String editStatus;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 3: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterQueryCriteria.java
git commit -m "feat(char): add editStatus filter field to CharCharacterQueryCriteria"
```

---

## Task 8: 汉字 Wrapper — toCriteria 透传两字段 + toBaseVO 移除 hasDraft

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java`

- [ ] **Step 1: 修改 `toCriteria` 方法（28-32 行）**

把原方法体：

```java
    public static CharCharacterQueryCriteria toCriteria(CharCharacterQueryRequest request) {
        CharCharacterQueryCriteria criteria = new CharCharacterQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        return criteria;
    }
```

替换为：

```java
    public static CharCharacterQueryCriteria toCriteria(CharCharacterQueryRequest request) {
        CharCharacterQueryCriteria criteria = new CharCharacterQueryCriteria();
        criteria.setBlurry(request.getBlurry());
        criteria.setPublishStatus(request.getPublishStatus());
        criteria.setEditStatus(request.getEditStatus());
        return criteria;
    }
```

- [ ] **Step 2: 修改 `toBaseVO` 方法（111 行），移除 setHasDraft 那一行**

把第 111 行：

```java
        vo.setHasDraft(dto.getDraftContent() != null);
```

**删除**。`toBaseVO` 的其余 setter 全部保留不变。

> 注意：`toVO` 方法中的第 134 行 `vo.setHasDraft(dto.getDraftContent() != null);` **保留**，本次不动详情 VO。

- [ ] **Step 3: 编译验证**

```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 4: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java
git commit -m "feat(char): pass status filters in toCriteria; drop hasDraft from list VO"
```

---

## Task 9: 汉字 BaseVO — 移除 hasDraft 字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`

- [ ] **Step 1: 删除 hasDraft 字段及其注解（55-56 行）**

把这两行：

```java
    @ApiModelProperty(value = "是否有草稿")
    private Boolean hasDraft;
```

整段**删除**。改完后 `editStatus` 字段与 `createBy` 字段之间应只有一个空行。

- [ ] **Step 2: 编译验证**

```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。Task 8 已经把 `setHasDraft` 从 `toBaseVO` 移除，所以此时不会有"找不到方法"的报错。

- [ ] **Step 3: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java
git commit -m "feat(char): remove hasDraft field from CharCharacterBaseVO"
```

---

## Task 10: 汉字 ServiceImpl — queryAll 接入草稿回填

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

> 当前 `queryAll`（51-59 行）：
> ```java
>     @Override
>     public PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable) {
>         Page<CharCharacter> page = charCharacterRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
>             Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
>             Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
>             return criteriaBuilder.and(basePredicate, statusPredicate);
>         }, pageable);
>         return PageUtil.toPage(page.map(charCharacterMapper::toDto));
>     }
> ```

- [ ] **Step 1: 把 `queryAll` 最后一行 `page.map(charCharacterMapper::toDto)` 改为 `page.map(this::toDtoWithDraftOverlay)`**

修改后 `queryAll` 整体为：

```java
    @Override
    public PageResult<CharCharacterDto> queryAll(CharCharacterQueryCriteria criteria, Pageable pageable) {
        Page<CharCharacter> page = charCharacterRepository.findAll((root, criteriaQuery, criteriaBuilder) -> {
            Predicate basePredicate = QueryHelp.getPredicate(root, criteria, criteriaBuilder);
            Predicate statusPredicate = criteriaBuilder.equal(root.get("status"), StatusEnum.ENABLED.getCode());
            return criteriaBuilder.and(basePredicate, statusPredicate);
        }, pageable);
        return PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
    }
```

- [ ] **Step 2: 在 `queryAll` 方法正下方（59 行之后、`findById` 之前）插入两个私有方法**

```java
    /**
     * 主表实体 → DTO；若处于 draft/reviewed，将 draftContent 中的业务字段覆盖回 DTO。
     * 仅供 {@link #queryAll} 调用。
     */
    private CharCharacterDto toDtoWithDraftOverlay(CharCharacter entity) {
        CharCharacterDto dto = charCharacterMapper.toDto(entity);
        if (EditStatusEnum.DRAFT.getCode().equals(entity.getEditStatus())
                || EditStatusEnum.REVIEWED.getCode().equals(entity.getEditStatus())) {
            applyDraftOverlay(dto, entity.getDraftContent());
        }
        return dto;
    }

    /**
     * 把 draftContent JSON 中"列表页需要的业务字段"覆盖到 DTO 上。
     *
     * 覆盖范围应与 {@link com.naon.grid.backend.rest.vo.CharCharacterBaseVO} 暴露的业务字段保持一致。
     * BaseVO 新增业务字段时，请同步在此方法添加覆盖。
     *
     * 永远不覆盖：id、status、publishStatus、editStatus、createBy、updateBy、
     * createTime、updateTime —— 这些字段以主表为准。
     *
     * 不读取：discriminations、words —— 列表页不返回子表。
     *
     * @throws BadRequestException 草稿数据缺失或解析失败
     */
    private void applyDraftOverlay(CharCharacterDto dto, String draftJson) {
        if (draftJson == null) {
            throw new BadRequestException("草稿内容不存在");
        }
        CharCharacterDto draft = JsonUtils.fromJson(draftJson, CharCharacterDto.class);
        if (draft.getSequenceNo() != null)       dto.setSequenceNo(draft.getSequenceNo());
        if (draft.getCharacter() != null)        dto.setCharacter(draft.getCharacter());
        if (draft.getLevel() != null)            dto.setLevel(draft.getLevel());
        if (draft.getPinyin() != null)           dto.setPinyin(draft.getPinyin());
        if (draft.getAudioId() != null)          dto.setAudioId(draft.getAudioId());
        if (draft.getTraditional() != null)      dto.setTraditional(draft.getTraditional());
        if (draft.getRadical() != null)          dto.setRadical(draft.getRadical());
        if (draft.getStroke() != null)           dto.setStroke(draft.getStroke());
        if (draft.getCharDesc() != null)         dto.setCharDesc(draft.getCharDesc());
        if (draft.getDescTranslations() != null) dto.setDescTranslations(draft.getDescTranslations());
    }
```

> 说明：`BadRequestException`、`EditStatusEnum`、`JsonUtils` 已在文件顶部 import 过（参见现有 `findById` / `publishDraft` 等方法），无需新增 import。

- [ ] **Step 3: 编译验证**

```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 4: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java
git commit -m "feat(char): overlay draftContent onto list DTO for draft/reviewed rows"
```

---

## Task 11: 整体编译 + 启动 + Knife4j 手动验证

**Files:** 无新增

- [ ] **Step 1: 整仓编译**

```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn clean install -DskipTests -q
```
Expected: 全部模块 `BUILD SUCCESS`，无 ERROR / FAILURE。

- [ ] **Step 2: 启动后端（grid-bootstrap）**

新开一个终端窗口运行：

```bash
cd /c/Users/nano/Desktop/nano-gemini/grid-bootstrap && mvn spring-boot:run
```

Expected: 控制台出现 `Tomcat started on port(s): 8000` 类似日志，无 ERROR 堆栈。

- [ ] **Step 3: 打开 Knife4j 验证词汇列表接口**

浏览器访问：`http://localhost:8000/doc.html`

找到 **后台：词汇-词汇管理** → `GET /api/vocabulary`，验证：

a. 入参面板应显示三个查询字段：`blurry`、`publishStatus`、`editStatus`。
b. 不带任何参数调用一次，返回列表项的 JSON 字段中**不再包含** `hasDraft`。
c. 准备数据：通过 `POST /api/vocabulary` 新建一个词条（例如 word="测试词汇A"，pinyin="cè shì cí huì A"），此时该词处于 `editStatus=draft / publishStatus=unpublished`。
d. 不带参数调用 `GET /api/vocabulary`，找到刚建的词条，确认响应里 `word`、`pinyin`、`hskLevel` 等字段**有值**（即来自 draftContent 覆盖，而不是主表 null）。
e. 带 `editStatus=draft` 调用，确认仅返回 draft 态的词条。
f. 带 `editStatus=published` 调用，确认返回不包含步骤 c 新建的草稿态词条。
g. 带 `publishStatus=published` 调用，确认仅返回已发布的词条。
h. 带 `publishStatus=published&editStatus=draft` 调用（理论上无交集），确认返回 0 条且无异常。

- [ ] **Step 4: 打开 Knife4j 验证汉字列表接口**

找到 **后台：汉字-汉字管理** → `GET /api/character`，重复 Step 3 的 a~h，把字段名换成汉字侧的对应字段（如 `character` / `pinyin` / `level`），并通过 `POST /api/character` 新建测试汉字。

- [ ] **Step 5: 关闭后端**

回到 `mvn spring-boot:run` 窗口按 Ctrl+C，确认进程退出。

- [ ] **Step 6: 终态 Commit（如有未跟踪的日志等）**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git status
```

如果只有 `logs/` 等无关产物，无需 commit。本计划至此完成，分支上应有 10 个 `feat(...)` 提交（每个 Task 1~10 一个），均推送到 `spec/vocab-char-queryAll-draft-overlay` 分支。

---

## Self-Review

**Spec coverage（对照 spec 五段）：**

| Spec 要求 | 对应 Task |
|--|--|
| Request 加 publishStatus / editStatus | Task 1（vocab）/ Task 6（char） |
| Criteria 加 editStatus（publishStatus 已有） | Task 2（vocab）/ Task 7（char） |
| Wrapper.toCriteria 透传两字段（修复 publishStatus 从未透传的 bug） | Task 3 Step 1 / Task 8 Step 1 |
| Wrapper.toBaseVO 删除 setHasDraft | Task 3 Step 2 / Task 8 Step 2 |
| BaseVO 删除 hasDraft 字段 | Task 4 / Task 9 |
| ServiceImpl.queryAll 改走 toDtoWithDraftOverlay + applyDraftOverlay | Task 5 / Task 10 |
| draftContent 缺失抛 BadRequestException | Task 5 Step 2 / Task 10 Step 2（代码块内已含） |
| 不动 findById / findPublishedById / 详情 VO 的 hasDraft | Task 3 Step 2 / Task 8 Step 2 注释提示已说明 |
| 整体编译 + Knife4j 手动验证 | Task 11 |

无遗漏。

**Placeholder 扫描：** 无 TBD / TODO；每个 Step 给出了实际代码块或具体命令；命令期望输出明确；前后 Task 引用的字段名、方法名一致（`toDtoWithDraftOverlay`、`applyDraftOverlay` 在词汇和汉字侧命名完全相同）。

**类型一致性：** `Integer sequenceNo`、`Long audioId`、`List<TextTranslation> descTranslations` 与 DTO 定义一致；`EditStatusEnum.DRAFT.getCode()` / `REVIEWED.getCode()` 返回 String 与 entity 字段类型一致；`BadRequestException` 包路径与现有 ServiceImpl 一致；`JsonUtils.fromJson(String, Class<T>)` 签名匹配。

Self-Review 通过。
