# 汉字子表展示排序字段 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 给 `char_discrimination` / `char_word` 两张汉字子表加上 `discriminationOrder` / `wordOrder` 排序字段，端到端打通（实体 → DTO → Request → 后台 VO → Wrapper → Service），后台详情读取时按降序排列，App 端透明受益。

**Architecture:** 复用 vocab 模块的 `senseOrder` 端到端模板。Java 类型统一 `Integer`、默认 `0`。排序放在 `CharCharacterServiceImpl` 内两个新的 `sortXxxDesc` helper，对所有读路径（findById 的草稿分支 / 已发布分支、findPublishedById）统一调用。App 端 VO 不暴露 order，依赖 Service 已排序的输出顺序。

**Tech Stack:** Spring Boot 2.7.18 + Spring Data JPA + Lombok + Swagger (Knife4j) + Fastjson2 + Maven。无新增依赖。

**关联 Spec:** `docs/superpowers/specs/2026-06-07-char-subtable-order-design.md`

---

## File Structure

本计划共改 **8 个 Java 文件**（SQL 已在 spec 阶段改完），不新增任何文件：

| 文件 | 责任 | 改动量 |
|--|--|--|
| `grid-system/.../domain/character/CharDiscrimination.java` | JPA 实体（辨析） | +1 字段 |
| `grid-system/.../domain/character/CharWord.java` | JPA 实体（组词） | +1 字段 |
| `grid-system/.../service/character/dto/CharDiscriminationDto.java` | Service 层 DTO | +1 字段 |
| `grid-system/.../service/character/dto/CharWordDto.java` | Service 层 DTO | +1 字段 |
| `grid-system/.../rest/request/CharCharacterCreateRequest.java` | 后台创建/更新请求 | +2 字段（两个内嵌类各 1） |
| `grid-system/.../rest/vo/CharCharacterVO.java` | 后台详情 VO | +2 字段（两个内嵌类各 1） |
| `grid-system/.../rest/wrapper/CharCharacterWrapper.java` | DTO ↔ Request / VO 转换 | +4 行（4 个方法各 1） |
| `grid-system/.../service/character/impl/CharCharacterServiceImpl.java` | 业务实现 | +6 行字段兜底 + 2 个 helper + 3 处调用 + 1 个 import |

**实施顺序**：自底向上（实体 → DTO → Request/VO → Wrapper → Service），每个 task 结束都 `mvn compile` 验证、独立 commit。最后一个 task 整体编译 + 手动验证 + 总 commit。

> **TDD 说明**：本项目 `pom.xml` 默认 `-DskipTests`，仓库中也没有对 `CharCharacterServiceImpl` 等的单元测试基础设施（参考最近的 plan 如 `2026-06-07-vocab-new-fields.md`，同样 spec 模板也未引入测试）。验证手段为 **`mvn -pl grid-system -am compile` 编译通过** + **最终 Bootstrap 启动 + Knife4j 调用接口的手动验证**。每个 task 都明确给出验证命令和预期输出。

---

## Task 1: 实体层 — CharDiscrimination 加 discriminationOrder

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharDiscrimination.java`

- [ ] **Step 1: 编辑实体，新增字段**

在 `comparisonTranslations` 字段（约 44 行）之后、`@CreationTimestamp` 注解之前，插入：

```java
    @NotNull
    @Column(name = "discrimination_order", nullable = false)
    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer discriminationOrder = 0;
```

完整上下文（修改后的 41-50 行应为）：

```java
    @Column(name = "discrim_char_translations", columnDefinition = "text")
    private String discrimCharTranslations;

    @Column(name = "comparison_translations", columnDefinition = "text")
    private String comparisonTranslations;

    @NotNull
    @Column(name = "discrimination_order", nullable = false)
    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer discriminationOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
```

- [ ] **Step 2: 编译验证**

Run（Windows bash，注意路径用正斜杠）:
```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功，无 BUILD FAILURE 行。

- [ ] **Step 3: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/domain/character/CharDiscrimination.java
git commit -m "feat(char): add discriminationOrder field to CharDiscrimination entity"
```

---

## Task 2: 实体层 — CharWord 加 wordOrder

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java`

- [ ] **Step 1: 编辑实体，新增字段**

在 `exampleImage` 字段（约 59-60 行）之后、`@CreationTimestamp` 注解之前，插入：

```java
    @NotNull
    @Column(name = "word_order", nullable = false)
    @ApiModelProperty(value = "组词排序权重（值大的排前面）")
    private Integer wordOrder = 0;
```

完整上下文（修改后的 56-65 行应为）：

```java
    @Column(name = "example_translations", columnDefinition = "text")
    private String exampleTranslations;

    @Column(name = "example_image", length = 255)
    private String exampleImage;

    @NotNull
    @Column(name = "word_order", nullable = false)
    @ApiModelProperty(value = "组词排序权重（值大的排前面）")
    private Integer wordOrder = 0;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
```

- [ ] **Step 2: 编译验证**

Run:
```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 3: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java
git commit -m "feat(char): add wordOrder field to CharWord entity"
```

---

## Task 3: DTO 层 — CharDiscriminationDto 加 discriminationOrder

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java`

- [ ] **Step 1: 编辑 DTO，新增字段**

在 `updateTime` 字段（约 38 行）之后、`status` 字段之前，插入：

```java
    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer discriminationOrder;
```

完整上下文（修改后应为）：

```java
    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
    private Integer discriminationOrder;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
```

注意：DTO 字段**不**带 `= 0` 默认值（与 `VocabSenseDto.senseOrder` 等 vocab DTO 模板一致），允许 null 表达"未提供"，兜底交给 Wrapper/Service。

- [ ] **Step 2: 编译验证**

Run:
```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 3: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java
git commit -m "feat(char): add discriminationOrder field to CharDiscriminationDto"
```

---

## Task 4: DTO 层 — CharWordDto 加 wordOrder

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java`

- [ ] **Step 1: 编辑 DTO，新增字段**

在 `updateTime` 字段（约 53 行）之后、`status` 字段之前，插入：

```java
    @ApiModelProperty(value = "组词排序权重（值大的排前面）")
    private Integer wordOrder;
```

完整上下文（修改后应为）：

```java
    @ApiModelProperty(value = "创建时间")
    private Timestamp createTime;

    @ApiModelProperty(value = "更新时间")
    private Timestamp updateTime;

    @ApiModelProperty(value = "组词排序权重（值大的排前面）")
    private Integer wordOrder;

    @ApiModelProperty(value = "状态: 1=可用, 0=不可用")
    private Integer status;
```

- [ ] **Step 2: 编译验证**

Run:
```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 3: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java
git commit -m "feat(char): add wordOrder field to CharWordDto"
```

---

## Task 5: Request 层 — CharCharacterCreateRequest 两个内嵌类各加字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`

- [ ] **Step 1: 在 CharDiscriminationRequest 内嵌类加字段**

在 `comparisonTranslations` 字段（约 73 行）之后、内嵌类闭合 `}` 之前，插入：

```java
        @ApiModelProperty(value = "辨析排序权重（值大的排前面，不传默认 0）")
        private Integer discriminationOrder;
```

完整上下文（修改后 CharDiscriminationRequest 末尾应为）：

```java
        @ApiModelProperty(value = "对比翻译")
        private List<TextTranslationRequest> comparisonTranslations;

        @ApiModelProperty(value = "辨析排序权重（值大的排前面，不传默认 0）")
        private Integer discriminationOrder;
    }
```

- [ ] **Step 2: 在 CharWordRequest 内嵌类加字段**

在 `exampleImage` 字段（约 108 行）之后、内嵌类闭合 `}` 之前，插入：

```java
        @ApiModelProperty(value = "组词排序权重（值大的排前面，不传默认 0）")
        private Integer wordOrder;
```

完整上下文（修改后 CharWordRequest 末尾应为）：

```java
        @ApiModelProperty(value = "例句图片")
        private String exampleImage;

        @ApiModelProperty(value = "组词排序权重（值大的排前面，不传默认 0）")
        private Integer wordOrder;
    }
```

注意：两个字段都**不**加 `@NotNull`，与 `VocabWordCreateRequest` 内嵌 Request 字段一致。允许前端不传，后端兜底为 0。

- [ ] **Step 3: 编译验证**

Run:
```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 4: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java
git commit -m "feat(char): add order fields to CharCharacterCreateRequest nested classes"
```

---

## Task 6: 后台 VO 层 — CharCharacterVO 两个内嵌类各加字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`

- [ ] **Step 1: 在 CharDiscriminationVO 内嵌类加字段**

在 `comparisonTranslations` 字段（约 97 行）之后、`createTime` 字段之前，插入：

```java
        @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
        private Integer discriminationOrder;
```

完整上下文（修改后应为）：

```java
        @ApiModelProperty(value = "对比翻译")
        private List<TextTranslationVO> comparisonTranslations;

        @ApiModelProperty(value = "辨析排序权重（值大的排前面）")
        private Integer discriminationOrder;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;
```

- [ ] **Step 2: 在 CharWordVO 内嵌类加字段**

在 `exampleImage` 字段（约 142 行）之后、`createTime` 字段之前，插入：

```java
        @ApiModelProperty(value = "组词排序权重（值大的排前面）")
        private Integer wordOrder;
```

完整上下文（修改后应为）：

```java
        @ApiModelProperty(value = "例句图片")
        private String exampleImage;

        @ApiModelProperty(value = "组词排序权重（值大的排前面）")
        private Integer wordOrder;

        @ApiModelProperty(value = "创建时间: yyyy-MM-dd HH:mm:ss", required = true)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Timestamp createTime;
```

- [ ] **Step 3: 编译验证**

Run:
```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 4: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java
git commit -m "feat(char): expose order fields in CharCharacterVO nested VOs"
```

---

## Task 7: Wrapper 层 — CharCharacterWrapper 4 处对位改动

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java`

- [ ] **Step 1: 在 toDiscriminationDto 添加 order 兜底**

在 `toDiscriminationDto` 方法的 `dto.setComparisonTranslations(...)` 行（约 64 行）之后，插入：

```java
        dto.setDiscriminationOrder(request.getDiscriminationOrder() != null ? request.getDiscriminationOrder() : 0);
```

完整上下文（修改后方法体）：

```java
    private static CharDiscriminationDto toDiscriminationDto(CharCharacterCreateRequest.CharDiscriminationRequest request) {
        CharDiscriminationDto dto = new CharDiscriminationDto();
        dto.setId(request.getId());
        dto.setDiscrimChar(request.getDiscrimChar());
        dto.setDiscrimPinyin(request.getDiscrimPinyin());
        dto.setDiscrimCharTranslations(toTextTranslationList(request.getDiscrimCharTranslations()));
        dto.setComparisonTranslations(toTextTranslationList(request.getComparisonTranslations()));
        dto.setDiscriminationOrder(request.getDiscriminationOrder() != null ? request.getDiscriminationOrder() : 0);
        return dto;
    }
```

- [ ] **Step 2: 在 toWordDto 添加 order 兜底**

在 `toWordDto` 方法的 `dto.setExampleImage(...)` 行（约 86 行）之后，插入：

```java
        dto.setWordOrder(request.getWordOrder() != null ? request.getWordOrder() : 0);
```

完整上下文（修改后方法体）：

```java
    private static CharWordDto toWordDto(CharCharacterCreateRequest.CharWordRequest request) {
        CharWordDto dto = new CharWordDto();
        dto.setId(request.getId());
        dto.setWordItem(request.getWordItem());
        dto.setLevel(request.getLevel());
        dto.setPinyin(request.getPinyin());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setWordItemTranslations(toTextTranslationList(request.getWordItemTranslations()));
        dto.setExampleSentence(request.getExampleSentence());
        dto.setExamplePinyin(request.getExamplePinyin());
        dto.setExampleTranslations(toTextTranslationList(request.getExampleTranslations()));
        dto.setExampleImage(request.getExampleImage());
        dto.setWordOrder(request.getWordOrder() != null ? request.getWordOrder() : 0);
        return dto;
    }
```

- [ ] **Step 3: 在 toDiscriminationVO 透传 order**

在 `toDiscriminationVO` 方法的 `vo.setComparisonTranslations(...)` 行（约 156 行）之后，插入：

```java
        vo.setDiscriminationOrder(dto.getDiscriminationOrder());
```

完整上下文（修改后方法体）：

```java
    private static CharCharacterVO.CharDiscriminationVO toDiscriminationVO(CharDiscriminationDto dto) {
        CharCharacterVO.CharDiscriminationVO vo = new CharCharacterVO.CharDiscriminationVO();
        vo.setId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setDiscrimChar(dto.getDiscrimChar());
        vo.setDiscrimPinyin(dto.getDiscrimPinyin());
        vo.setDiscrimCharTranslations(toTextTranslationVOList(dto.getDiscrimCharTranslations()));
        vo.setComparisonTranslations(toTextTranslationVOList(dto.getComparisonTranslations()));
        vo.setDiscriminationOrder(dto.getDiscriminationOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
```

- [ ] **Step 4: 在 toWordVO 透传 order**

在 `toWordVO` 方法的 `vo.setExampleImage(...)` 行（约 181 行）之后，插入：

```java
        vo.setWordOrder(dto.getWordOrder());
```

完整上下文（修改后方法体）：

```java
    private static CharCharacterVO.CharWordVO toWordVO(CharWordDto dto) {
        CharCharacterVO.CharWordVO vo = new CharCharacterVO.CharWordVO();
        vo.setId(dto.getId());
        vo.setCharId(dto.getCharId());
        vo.setWordItem(dto.getWordItem());
        vo.setLevel(dto.getLevel());
        vo.setPinyin(dto.getPinyin());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setWordItemTranslations(toTextTranslationVOList(dto.getWordItemTranslations()));
        vo.setExampleSentence(dto.getExampleSentence());
        vo.setExamplePinyin(dto.getExamplePinyin());
        vo.setExampleTranslations(toTextTranslationVOList(dto.getExampleTranslations()));
        vo.setExampleImage(dto.getExampleImage());
        vo.setWordOrder(dto.getWordOrder());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
```

- [ ] **Step 5: 编译验证**

Run:
```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功。

- [ ] **Step 6: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/CharCharacterWrapper.java
git commit -m "feat(char): wire order fields through CharCharacterWrapper toDto/toVO"
```

---

## Task 8: Service 层 — 字段兜底 + 排序 helper + 调用点

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

> 本 task 改动 12 处：1 处 import + 6 处字段兜底 + 2 处新 helper + 3 处调用 helper。逐步骤完成、最后一起编译 + commit。

- [ ] **Step 1: 添加 Comparator import**

在文件顶部 import 区域找到现有的 `java.util.*` imports（约 33-39 行）。在 `import java.util.ArrayList;` 之后插入：

```java
import java.util.Comparator;
```

完整上下文（修改后约 33-40 行）：

```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
```

- [ ] **Step 2: convertToDiscriminationDto — Entity → DTO 透传 order**

找到 `convertToDiscriminationDto(CharDiscrimination)` 方法（约 288-300 行）。在 `dto.setComparisonTranslations(...)` 行之后插入：

```java
        dto.setDiscriminationOrder(discrimination.getDiscriminationOrder());
```

完整上下文（修改后方法）：

```java
    private CharDiscriminationDto convertToDiscriminationDto(CharDiscrimination discrimination) {
        CharDiscriminationDto dto = new CharDiscriminationDto();
        dto.setId(discrimination.getId());
        dto.setCharId(discrimination.getCharId());
        dto.setDiscrimChar(discrimination.getDiscrimChar());
        dto.setDiscrimPinyin(discrimination.getDiscrimPinyin());
        dto.setDiscrimCharTranslations(JsonUtils.parseTranslationList(discrimination.getDiscrimCharTranslations()));
        dto.setComparisonTranslations(JsonUtils.parseTranslationList(discrimination.getComparisonTranslations()));
        dto.setDiscriminationOrder(discrimination.getDiscriminationOrder());
        dto.setCreateTime(discrimination.getCreateTime());
        dto.setUpdateTime(discrimination.getUpdateTime());
        dto.setStatus(discrimination.getStatus());
        return dto;
    }
```

- [ ] **Step 3: convertToWordDto — Entity → DTO 透传 order**

找到 `convertToWordDto(CharWord)` 方法（约 311-328 行）。在 `dto.setExampleImage(...)` 行之后插入：

```java
        dto.setWordOrder(word.getWordOrder());
```

完整上下文（修改后方法）：

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
        dto.setWordOrder(word.getWordOrder());
        dto.setCreateTime(word.getCreateTime());
        dto.setUpdateTime(word.getUpdateTime());
        dto.setStatus(word.getStatus());
        return dto;
    }
```

- [ ] **Step 4: updateDiscrimination — 更新已存在 entity 时写 order**

找到 `updateDiscrimination(CharDiscrimination, CharDiscriminationDto)` 方法（约 330-335 行）。在 `entity.setComparisonTranslations(...)` 行之后插入：

```java
        entity.setDiscriminationOrder(dto.getDiscriminationOrder() != null ? dto.getDiscriminationOrder() : 0);
```

完整上下文（修改后方法）：

```java
    private void updateDiscrimination(CharDiscrimination entity, CharDiscriminationDto dto) {
        entity.setDiscrimChar(dto.getDiscrimChar());
        entity.setDiscrimPinyin(dto.getDiscrimPinyin());
        entity.setDiscrimCharTranslations(JsonUtils.toTranslationJson(dto.getDiscrimCharTranslations()));
        entity.setComparisonTranslations(JsonUtils.toTranslationJson(dto.getComparisonTranslations()));
        entity.setDiscriminationOrder(dto.getDiscriminationOrder() != null ? dto.getDiscriminationOrder() : 0);
    }
```

- [ ] **Step 5: updateWord — 更新已存在 entity 时写 order**

找到 `updateWord(CharWord, CharWordDto)` 方法（约 337-347 行）。在 `entity.setExampleImage(...)` 行之后插入：

```java
        entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);
```

完整上下文（修改后方法）：

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
        entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);
    }
```

- [ ] **Step 6: convertToDiscriminationEntity — 新增 entity 时写 order**

找到 `convertToDiscriminationEntity(CharDiscriminationDto, Integer)` 方法（约 349-358 行）。在 `entity.setComparisonTranslations(...)` 行之后、`entity.setStatus(...)` 之前插入：

```java
        entity.setDiscriminationOrder(dto.getDiscriminationOrder() != null ? dto.getDiscriminationOrder() : 0);
```

完整上下文（修改后方法）：

```java
    private CharDiscrimination convertToDiscriminationEntity(CharDiscriminationDto dto, Integer charId) {
        CharDiscrimination entity = new CharDiscrimination();
        entity.setCharId(charId);
        entity.setDiscrimChar(dto.getDiscrimChar());
        entity.setDiscrimPinyin(dto.getDiscrimPinyin());
        entity.setDiscrimCharTranslations(JsonUtils.toTranslationJson(dto.getDiscrimCharTranslations()));
        entity.setComparisonTranslations(JsonUtils.toTranslationJson(dto.getComparisonTranslations()));
        entity.setDiscriminationOrder(dto.getDiscriminationOrder() != null ? dto.getDiscriminationOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }
```

- [ ] **Step 7: convertToWordEntity — 新增 entity 时写 order**

找到 `convertToWordEntity(CharWordDto, Integer)` 方法（约 360-374 行）。在 `entity.setExampleImage(...)` 行之后、`entity.setStatus(...)` 之前插入：

```java
        entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);
```

完整上下文（修改后方法）：

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
        entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }
```

- [ ] **Step 8: 新增两个排序 helper**

在 `convertToWordEntity` 方法之后（约 375 行附近，紧跟 Step 7 修改的方法之后、`reviewDraft` 之前），插入以下两个私有方法：

```java
    private List<CharDiscriminationDto> sortDiscriminationsDesc(List<CharDiscriminationDto> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        list.sort(Comparator.comparing(
            CharDiscriminationDto::getDiscriminationOrder,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return list;
    }

    private List<CharWordDto> sortWordsDesc(List<CharWordDto> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        list.sort(Comparator.comparing(
            CharWordDto::getWordOrder,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return list;
    }
```

要点：
- `Comparator.comparing(..., Comparator.nullsLast(Comparator.reverseOrder()))` 实现降序、null 排最后
- in-place 排序（返回的还是同一个 list 引用，仅为链式调用方便）
- 两个 helper 私有，仅本 Service 内使用

- [ ] **Step 9: 在 findById "草稿/已审核" 分支调用排序**

找到 `findById` 方法的草稿分支（约 71-87 行）。在 `dto.setUpdateTime(charCharacter.getUpdateTime());` 行（约 85 行）之后、`return dto;` 之前插入：

```java
            dto.setDiscriminations(sortDiscriminationsDesc(dto.getDiscriminations()));
            dto.setWords(sortWordsDesc(dto.getWords()));
```

完整上下文（修改后的草稿分支末尾）：

```java
            dto.setCreateBy(charCharacter.getCreateBy());
            dto.setUpdateBy(charCharacter.getUpdateBy());
            dto.setCreateTime(charCharacter.getCreateTime());
            dto.setUpdateTime(charCharacter.getUpdateTime());
            dto.setDiscriminations(sortDiscriminationsDesc(dto.getDiscriminations()));
            dto.setWords(sortWordsDesc(dto.getWords()));
            return dto;
        }
```

- [ ] **Step 10: 在 findById "已发布" 分支包裹排序**

找到 `findById` 方法的已发布分支末尾两行（约 91-92 行）：

```java
        charCharacterDto.setDiscriminations(convertToDiscriminationDtos(charDiscriminationRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())));
        charCharacterDto.setWords(convertToWordDtos(charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())));
```

整体替换为：

```java
        charCharacterDto.setDiscriminations(sortDiscriminationsDesc(convertToDiscriminationDtos(charDiscriminationRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
        charCharacterDto.setWords(sortWordsDesc(convertToWordDtos(charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
```

- [ ] **Step 11: 在 findPublishedById 包裹排序**

找到 `findPublishedById` 方法末尾两行（约 109-110 行）：

```java
        charCharacterDto.setDiscriminations(convertToDiscriminationDtos(charDiscriminationRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())));
        charCharacterDto.setWords(convertToWordDtos(charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())));
```

整体替换为：

```java
        charCharacterDto.setDiscriminations(sortDiscriminationsDesc(convertToDiscriminationDtos(charDiscriminationRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
        charCharacterDto.setWords(sortWordsDesc(convertToWordDtos(charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode()))));
```

- [ ] **Step 12: 编译验证**

Run:
```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn -pl grid-system -am compile -q
```
Expected: 编译成功，无 BUILD FAILURE 行。

- [ ] **Step 13: Commit**

```bash
cd /c/Users/nano/Desktop/nano-gemini
git add grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java
git commit -m "feat(char): wire order fields and sort children desc in CharCharacterServiceImpl

- Pass-through discriminationOrder/wordOrder in convertTo*Dto (Entity -> DTO)
- Default null -> 0 in convertTo*Entity / update* (DTO -> Entity)
- Add sortDiscriminationsDesc / sortWordsDesc helpers (desc, nulls last)
- Apply sort in findById (both draft and published branches) and findPublishedById"
```

---

## Task 9: 整体编译 + 启动验证 + 手动接口验证

**Files:** 无新增改动；本 task 是端到端验收。

- [ ] **Step 1: 全模块编译**

Run:
```bash
cd /c/Users/nano/Desktop/nano-gemini && mvn clean install -DskipTests -q
```
Expected: BUILD SUCCESS。所有 7 个模块（grid-common / grid-logging / grid-tools / grid-system / grid-app / grid-bootstrap / grid 父）都通过。

- [ ] **Step 2: 启动 grid-bootstrap**

Run（前台启动，看启动日志；待启动完成后 Ctrl+C 也可以，本步骤目的是确认 JPA 实体能 ddl-validate / 启动期没报错）：

```bash
cd /c/Users/nano/Desktop/nano-gemini/grid-bootstrap && mvn spring-boot:run
```

Expected:
- 控制台出现 `Tomcat started on port(s): 8000`
- 没有以下任何报错：
  - `Schema-validation: missing column [discrimination_order] in table [char_discrimination]`
  - `Schema-validation: missing column [word_order] in table [char_word]`
  - 任何 `ClassNotFoundException` / `NoSuchMethodException` / `BeanCreationException`

注意：如果你的本地 MySQL `char_discrimination` 和 `char_word` 表还没有 `discrimination_order` / `word_order` 列，需要先在本地执行 SQL DDL 加列：

```sql
ALTER TABLE char_discrimination ADD COLUMN discrimination_order int(11) DEFAULT '0' COMMENT '辨析排序(值大的排前面)' AFTER comparison_translations;
ALTER TABLE char_word ADD COLUMN word_order int(11) DEFAULT '0' COMMENT '组词排序(值大的排前面)' AFTER example_image;
```

- [ ] **Step 3: 手动验证 — 创建带 order 的汉字**

打开 Knife4j: http://localhost:8000/doc.html

调用 `POST /api/character`，body 示例（关键看 `discriminationOrder` / `wordOrder` 字段被接受）：

```json
{
  "character": "测",
  "pinyin": "cè",
  "discriminations": [
    {"discrimChar": "侧", "discrimPinyin": "cè", "discriminationOrder": 10},
    {"discrimChar": "厕", "discrimPinyin": "cè", "discriminationOrder": 30},
    {"discrimChar": "册", "discrimPinyin": "cè", "discriminationOrder": 20}
  ],
  "words": [
    {"wordItem": "测试", "pinyin": "cè shì", "wordOrder": 5},
    {"wordItem": "测量", "pinyin": "cè liáng", "wordOrder": 15},
    {"wordItem": "猜测", "pinyin": "cāi cè", "wordOrder": 10}
  ]
}
```

Expected: 返回 201 + `{"id": <number>}`，记下 id。

- [ ] **Step 4: 手动验证 — 草稿查询排序**

调用 `GET /api/character/{id}`，使用上一步返回的 id。

Expected: 响应 JSON 中：
- `discriminations` 数组按 `discriminationOrder` 降序排：`厕(30)` → `册(20)` → `侧(10)`
- `words` 数组按 `wordOrder` 降序排：`测量(15)` → `猜测(10)` → `测试(5)`
- 每个子项都包含 `discriminationOrder` / `wordOrder` 字段

- [ ] **Step 5: 手动验证 — 发布后查询排序**

依次调用：
1. `PUT /api/character/{id}/review`（预期 204）
2. `PUT /api/character/{id}/publish`（预期 204）
3. `GET /api/character/{id}`（预期 200，已发布分支走 DB）

Expected: 响应中 `discriminations` / `words` 顺序与 Step 4 相同（降序），证明已发布分支走 `sortDiscriminationsDesc` / `sortWordsDesc` 的路径也正确。

- [ ] **Step 6: 手动验证 — App 端接口透明排序**

调用 App 端汉字详情接口（参考 `AppCharCharacterController` 的路径，例如 `GET /api/app/character/{id}` 或 `GET /api/app/character?character=测`，以实际接口为准），用上一步发布的汉字。

Expected: 响应中 `discriminations` / `words` 顺序为降序（与后台一致），但 VO 中**不包含** `discriminationOrder` / `wordOrder` 字段。

- [ ] **Step 7: 停止 Bootstrap（Ctrl+C）**

- [ ] **Step 8: 全计划完成总结 commit（可选）**

如果 Step 1-7 全部验证通过，工作树是干净的（之前每个 task 都已 commit），跳过 commit。若 Step 2 触发了 SQL DDL 改动需要更新 `sql/character.sql`，确认 spec 阶段已写入；如未写入则补一个 commit：

```bash
cd /c/Users/nano/Desktop/nano-gemini
git status   # 确认干净
git log --oneline -10   # 确认 8 个 task 都有对应 commit
```

Expected: 最近 10 条 commit 包含本计划各 task 对应的 8 条 feat(char): ... 提交（Task 1-8 各 1 条），以及更早的 spec commit。

---

## 完成判定

- [ ] 全部 9 个 task 的所有 checkbox 都已勾选
- [ ] `mvn clean install -DskipTests` 通过
- [ ] Knife4j 上手动调用创建 / 草稿查询 / 发布后查询，三种状态下子表都按 order 降序返回
- [ ] App 端接口子表顺序与后台一致，但不暴露 order 字段
- [ ] git log 中有 Task 1-8 对应的独立 commit
