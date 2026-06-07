# 词汇表新增字段补充 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `sql/vocabulary.sql` 新增的 5 个字段（`def_image`、`related_other`、`pattern_def`、`pattern_def_translations`、`image`）沿 Entity → DTO → Request → VO → Wrapper → Service → App 详情 一条线补齐，App 端图片字段以 OSS URL 形式返回。

**Architecture:** 在现有"草稿工作流 + JSON 落库 + 子表同步"骨架上做纯字段透传补充。Entity 仍以 `String` 形态承载 JSON 列；DTO 层做反序列化。App 端新增对 `AliOssStorageService.findByIds` 的批量查询，将图片资源 ID 解析为 URL，绝不暴露原始 ID。

**Tech Stack:** Spring Boot 2.7.18 + JPA + Hibernate + Fastjson2 + Lombok + Swagger/Knife4j；Java 8；项目默认 `skipTests=true`，验证以编译 + Knife4j 手测为准。

**前置说明：**
- 本项目无 JUnit 测试集，验证步骤仅含 `mvn compile` 与 Knife4j 手测，不写测试代码
- 所有提交信息末尾必须带：`Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`
- 当前已切到分支 `docs/vocab-new-fields-design`，但实现工作建议起一个新分支 `feat/vocab-new-fields`（任务 0 会做）
- 项目使用 Windows + bash，路径用正斜杠；shell 命令避免 `cat`/`grep`，用 Read/Grep 工具

---

## 文件结构（一览）

**修改：**
- `sql/vocabulary.sql` — 把 `pattern_def`、`pattern_def_translations` 改成 `DEFAULT NULL`
- `grid-tools/src/main/java/com/naon/grid/service/AliOssStorageService.java` — 加 `findByIds`
- `grid-tools/src/main/java/com/naon/grid/service/impl/AliOssStorageServiceImpl.java` — 实现 `findByIds`
- `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabSense.java` — 加 `defImage`、`relatedOther`
- `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabStructure.java` — 加 `patternDef`、`patternDefTranslations`
- `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabExample.java` — 加 `image`
- `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java` — 加 `defImage`、`relatedOther`
- `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabStructureDto.java` — 加 `patternDef`、`patternDefTranslations`
- `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExampleDto.java` — 加 `image`
- `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java` — 三个内嵌 Request 都加新字段
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java` — 三个内嵌 VO 都加新字段
- `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java` — 拷贝新字段（双向）
- `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java` — `convertTo*Dto / convertTo*Entity / update*` 三组都补字段
- `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabWordDetailVO.java` — 加 `ImageVO` 内部类 + 新字段
- `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java` — 注入 OSS 服务、批量查图片、装配 URL

---

## Task 0：准备分支

**Files:** —

- [ ] **Step 1：从 master 起新工作分支**

```bash
git fetch origin
git checkout master
git pull --ff-only origin master
git checkout -b feat/vocab-new-fields
```

预期输出：`Switched to a new branch 'feat/vocab-new-fields'`

- [ ] **Step 2：确认起点干净**

```bash
git status
```

预期：`nothing to commit, working tree clean`

---

## Task 1：修订 SQL — 让 pattern_def 和 pattern_def_translations 可空

**Files:**
- Modify: `sql/vocabulary.sql:51-52`

- [ ] **Step 1：把两行 NOT NULL 改成 DEFAULT NULL**

用 Read 读出 `sql/vocabulary.sql`，定位到 `vocab_structure` 的 `pattern_def` 行，按下面 Edit 替换：

```
old_string:
  `pattern` varchar(255) NOT NULL COMMENT '结构搭配文案',
  `pattern_def` varchar(512) NOT NULL COMMENT '结构搭配释义(可空)',
  `pattern_def_translations` varchar(1024) NOT NULL COMMENT '结构搭配释义外文翻译',

new_string:
  `pattern` varchar(255) NOT NULL COMMENT '结构搭配文案',
  `pattern_def` varchar(512) DEFAULT NULL COMMENT '结构搭配释义(可空)',
  `pattern_def_translations` varchar(1024) DEFAULT NULL COMMENT '结构搭配释义外文翻译, JSON列表格式(List<TextTranslation>)',
```

- [ ] **Step 2：提交**

```bash
git add sql/vocabulary.sql
git commit -m "fix(sql): make vocab_structure.pattern_def and pattern_def_translations nullable

Comment said 可空 but DDL was NOT NULL. Align DDL with intent.
Also clarify pattern_def_translations stores JSON List<TextTranslation>.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 2：OSS 服务加 findByIds 批量查询接口

**Files:**
- Modify: `grid-tools/src/main/java/com/naon/grid/service/AliOssStorageService.java`
- Modify: `grid-tools/src/main/java/com/naon/grid/service/impl/AliOssStorageServiceImpl.java`

- [ ] **Step 1：接口加 findByIds 声明**

在 `AliOssStorageService.java` 中 `findById(Long id)` 方法之后插入：

```
old_string:
    /**
     * 根据 ID 查询
     * @param id 文件 ID
     * @return 文件信息 DTO
     */
    AliOssStorageDto findById(Long id);

new_string:
    /**
     * 根据 ID 查询
     * @param id 文件 ID
     * @return 文件信息 DTO
     */
    AliOssStorageDto findById(Long id);

    /**
     * 根据 ID 列表批量查询
     * @param ids 文件 ID 列表
     * @return 文件信息 DTO 列表（不存在的 ID 不在结果中）
     */
    List<AliOssStorageDto> findByIds(List<Long> ids);
```

注意：`List` import 已存在（`java.util.List`），无需新增。

- [ ] **Step 2：实现 findByIds**

`AliOssStorageServiceImpl.java` 中 `findById` 方法之后插入：

```
old_string:
    @Override
    public AliOssStorageDto findById(Long id) {
        AliOssStorage storage = aliOssStorageRepository.findById(id).orElseGet(AliOssStorage::new);
        ValidationUtil.isNull(storage.getId(), "AliOssStorage", "id", id);
        return aliOssStorageMapper.toDto(storage);
    }

new_string:
    @Override
    public AliOssStorageDto findById(Long id) {
        AliOssStorage storage = aliOssStorageRepository.findById(id).orElseGet(AliOssStorage::new);
        ValidationUtil.isNull(storage.getId(), "AliOssStorage", "id", id);
        return aliOssStorageMapper.toDto(storage);
    }

    @Override
    public List<AliOssStorageDto> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return aliOssStorageMapper.toDto(aliOssStorageRepository.findAllById(ids));
    }
```

注意：`Collections` 与 `List` 已通过 `import java.util.*` 在文件顶部导入。

- [ ] **Step 3：编译 grid-tools 验证**

```bash
mvn -pl grid-tools -am compile -q
```

预期：BUILD SUCCESS，无错误。

- [ ] **Step 4：提交**

```bash
git add grid-tools/src/main/java/com/naon/grid/service/AliOssStorageService.java grid-tools/src/main/java/com/naon/grid/service/impl/AliOssStorageServiceImpl.java
git commit -m "feat(oss): add AliOssStorageService.findByIds for batch lookup

Mirrors AudioResourceService.findByIds. Empty/null input returns
emptyList. Used by App vocab detail to resolve image IDs to URLs in
one query.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 3：VocabSense Entity 补 defImage + relatedOther

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabSense.java`

- [ ] **Step 1：插入两个新字段**

在 `defAudioId` 字段后插入 `defImage`，在 `relatedBackward` 后插入 `relatedOther`：

第一处 Edit：

```
old_string:
    @Column(name = "def_audio_id")
    @ApiModelProperty(value = "中文释义音频资源ID")
    private Long defAudioId;

    @Column(name = "translations", columnDefinition = "json")

new_string:
    @Column(name = "def_audio_id")
    @ApiModelProperty(value = "中文释义音频资源ID")
    private Long defAudioId;

    @Column(name = "def_image")
    @ApiModelProperty(value = "中文释义图片资源ID")
    private Long defImage;

    @Column(name = "translations", columnDefinition = "json")
```

第二处 Edit：

```
old_string:
    @Column(name = "related_backward", columnDefinition = "text")
    @ApiModelProperty(value = "逆序关联词汇")
    private String relatedBackward;

    @NotNull
    @Column(name = "sense_order", nullable = false)

new_string:
    @Column(name = "related_backward", columnDefinition = "text")
    @ApiModelProperty(value = "逆序关联词汇")
    private String relatedBackward;

    @Column(name = "related_other", columnDefinition = "text")
    @ApiModelProperty(value = "其他关联词汇")
    private String relatedOther;

    @NotNull
    @Column(name = "sense_order", nullable = false)
```

- [ ] **Step 2：编译 grid-system 验证**

```bash
mvn -pl grid-system -am compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 3：提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabSense.java
git commit -m "feat(vocab): add defImage and relatedOther to VocabSense entity

Mirrors def_image (Long, OSS resource ID, type-aligned with defAudioId)
and related_other (JSON text List<String>, parallel to related_forward).

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 4：VocabStructure Entity 补 patternDef + patternDefTranslations

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabStructure.java`

- [ ] **Step 1：在 pattern 之后插入两个新字段**

```
old_string:
    @NotBlank
    @Column(name = "pattern", nullable = false, length = 255)
    @ApiModelProperty(value = "结构搭配文案")
    private String pattern;

    @NotNull
    @Column(name = "structure_order", nullable = false)

new_string:
    @NotBlank
    @Column(name = "pattern", nullable = false, length = 255)
    @ApiModelProperty(value = "结构搭配文案")
    private String pattern;

    @Column(name = "pattern_def", length = 512)
    @ApiModelProperty(value = "结构搭配释义")
    private String patternDef;

    @Column(name = "pattern_def_translations", length = 1024)
    @ApiModelProperty(value = "结构搭配释义外文翻译（JSON）")
    private String patternDefTranslations;

    @NotNull
    @Column(name = "structure_order", nullable = false)
```

- [ ] **Step 2：编译验证**

```bash
mvn -pl grid-system -am compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 3：提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabStructure.java
git commit -m "feat(vocab): add patternDef and patternDefTranslations to VocabStructure

Both nullable. patternDefTranslations stores JSON List<TextTranslation>
serialized via JsonUtils, mirroring the translations column on
vocab_sense and vocab_example.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 5：VocabExample Entity 补 image

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabExample.java`

- [ ] **Step 1：在 translations 之后插入 image 字段**

```
old_string:
    @Column(name = "translations", columnDefinition = "json")
    @ApiModelProperty(value = "例句外文翻译列表")
    private String translations;

    @NotNull
    @Column(name = "example_order", nullable = false)

new_string:
    @Column(name = "translations", columnDefinition = "json")
    @ApiModelProperty(value = "例句外文翻译列表")
    private String translations;

    @Column(name = "image")
    @ApiModelProperty(value = "例句图片资源ID")
    private Long image;

    @NotNull
    @Column(name = "example_order", nullable = false)
```

- [ ] **Step 2：编译验证**

```bash
mvn -pl grid-system -am compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 3：提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/domain/vocabulary/VocabExample.java
git commit -m "feat(vocab): add image to VocabExample entity

Long, nullable, maps to image column (OSS resource ID).
Type-aligned with audioId.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 6：DTO 层补字段（Sense + Structure + Example）

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabStructureDto.java`
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExampleDto.java`

- [ ] **Step 1：`VocabSenseDto` — 加 defImage 和 relatedOther**

第一处 Edit（defImage 紧跟 defAudioId）：

```
old_string:
    @ApiModelProperty(value = "中文释义音频资源ID")
    private Long defAudioId;

    @ApiModelProperty(value = "外文翻译列表")
    private List<TextTranslation> translations;

new_string:
    @ApiModelProperty(value = "中文释义音频资源ID")
    private Long defAudioId;

    @ApiModelProperty(value = "中文释义图片资源ID")
    private Long defImage;

    @ApiModelProperty(value = "外文翻译列表")
    private List<TextTranslation> translations;
```

第二处 Edit（relatedOther 紧跟 relatedBackward）：

```
old_string:
    @ApiModelProperty(value = "逆序关联词汇")
    private List<String> relatedBackward;

    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder;

new_string:
    @ApiModelProperty(value = "逆序关联词汇")
    private List<String> relatedBackward;

    @ApiModelProperty(value = "其他关联词汇")
    private List<String> relatedOther;

    @ApiModelProperty(value = "义项排序权重")
    private Integer senseOrder;
```

- [ ] **Step 2：`VocabStructureDto` — 加 patternDef + patternDefTranslations**

在 import 区加 `TextTranslation`：

```
old_string:
import com.naon.grid.base.BaseDTO;
import java.io.Serializable;
import java.util.List;

new_string:
import com.naon.grid.base.BaseDTO;
import com.naon.grid.domain.common.TextTranslation;
import java.io.Serializable;
import java.util.List;
```

在 `pattern` 字段之后插入新字段：

```
old_string:
    @ApiModelProperty(value = "结构搭配文案")
    private String pattern;

    @ApiModelProperty(value = "搭配排序权重")
    private Integer structureOrder;

new_string:
    @ApiModelProperty(value = "结构搭配文案")
    private String pattern;

    @ApiModelProperty(value = "结构搭配释义")
    private String patternDef;

    @ApiModelProperty(value = "结构搭配释义外文翻译列表")
    private List<TextTranslation> patternDefTranslations;

    @ApiModelProperty(value = "搭配排序权重")
    private Integer structureOrder;
```

- [ ] **Step 3：`VocabExampleDto` — 加 image**

```
old_string:
    @ApiModelProperty(value = "例句外文翻译列表")
    private List<TextTranslation> translations;

    @ApiModelProperty(value = "例句排序权重")
    private Integer exampleOrder;

new_string:
    @ApiModelProperty(value = "例句外文翻译列表")
    private List<TextTranslation> translations;

    @ApiModelProperty(value = "例句图片资源ID")
    private Long image;

    @ApiModelProperty(value = "例句排序权重")
    private Integer exampleOrder;
```

- [ ] **Step 4：编译**

```bash
mvn -pl grid-system -am compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 5：提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabStructureDto.java grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExampleDto.java
git commit -m "feat(vocab): add new fields to Sense/Structure/Example DTOs

- VocabSenseDto: defImage (Long), relatedOther (List<String>)
- VocabStructureDto: patternDef (String), patternDefTranslations (List<TextTranslation>)
- VocabExampleDto: image (Long)

DTO uses business types; JSON columns deserialized at service layer.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 7：Request 层补字段（VocabWordCreateRequest 三个内嵌类）

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java`

- [ ] **Step 1：`VocabSenseRequest` — 加 defImage + relatedOther**

第一处 Edit（defImage）：

```
old_string:
        @ApiModelProperty(value = "中文释义音频资源ID")
        private Long defAudioId;

        @ApiModelProperty(value = "外文翻译列表")
        private List<TextTranslationRequest> translations;

new_string:
        @ApiModelProperty(value = "中文释义音频资源ID")
        private Long defAudioId;

        @ApiModelProperty(value = "中文释义图片资源ID")
        private Long defImage;

        @ApiModelProperty(value = "外文翻译列表")
        private List<TextTranslationRequest> translations;
```

第二处 Edit（relatedOther）：

```
old_string:
        @ApiModelProperty(value = "逆序关联词汇")
        private List<String> relatedBackward;

        @ApiModelProperty(value = "义项排序权重，值大的排前面", required = true)
        private Integer senseOrder;

new_string:
        @ApiModelProperty(value = "逆序关联词汇")
        private List<String> relatedBackward;

        @ApiModelProperty(value = "其他关联词汇")
        private List<String> relatedOther;

        @ApiModelProperty(value = "义项排序权重，值大的排前面", required = true)
        private Integer senseOrder;
```

- [ ] **Step 2：`VocabStructureRequest` — 加 patternDef + patternDefTranslations**

```
old_string:
        @NotBlank
        @ApiModelProperty(value = "结构搭配文案", required = true)
        private String pattern;

        @ApiModelProperty(value = "搭配排序权重，值大的排前面", required = true)
        private Integer structureOrder;

new_string:
        @NotBlank
        @ApiModelProperty(value = "结构搭配文案", required = true)
        private String pattern;

        @ApiModelProperty(value = "结构搭配释义")
        private String patternDef;

        @ApiModelProperty(value = "结构搭配释义外文翻译列表")
        private List<TextTranslationRequest> patternDefTranslations;

        @ApiModelProperty(value = "搭配排序权重，值大的排前面", required = true)
        private Integer structureOrder;
```

- [ ] **Step 3：`VocabExampleRequest` — 加 image**

```
old_string:
        @ApiModelProperty(value = "例句外文翻译列表")
        private List<TextTranslationRequest> translations;

        @ApiModelProperty(value = "例句排序权重，值大的排前面", required = true)
        private Integer exampleOrder;

new_string:
        @ApiModelProperty(value = "例句外文翻译列表")
        private List<TextTranslationRequest> translations;

        @ApiModelProperty(value = "例句图片资源ID")
        private Long image;

        @ApiModelProperty(value = "例句排序权重，值大的排前面", required = true)
        private Integer exampleOrder;
```

- [ ] **Step 4：编译**

```bash
mvn -pl grid-system -am compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 5：提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java
git commit -m "feat(vocab): add new fields to VocabWordCreateRequest sub-types

All optional (no @NotNull/@NotBlank). Maps 1:1 with DTO additions.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 8：VO 层补字段（VocabWordVO 三个内嵌类）

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java`

- [ ] **Step 1：`VocabSenseVO` — 加 defImage + relatedOther**

第一处 Edit：

```
old_string:
        @ApiModelProperty(value = "中文释义音频资源ID")
        private Long defAudioId;

        @ApiModelProperty(value = "外文翻译列表")
        private List<TextTranslationVO> translations;

new_string:
        @ApiModelProperty(value = "中文释义音频资源ID")
        private Long defAudioId;

        @ApiModelProperty(value = "中文释义图片资源ID")
        private Long defImage;

        @ApiModelProperty(value = "外文翻译列表")
        private List<TextTranslationVO> translations;
```

第二处 Edit：

```
old_string:
        @ApiModelProperty(value = "逆序关联词汇")
        private List<String> relatedBackward;

        @ApiModelProperty(value = "义项排序权重，值大的排前面", required = true)
        private Integer senseOrder;

new_string:
        @ApiModelProperty(value = "逆序关联词汇")
        private List<String> relatedBackward;

        @ApiModelProperty(value = "其他关联词汇")
        private List<String> relatedOther;

        @ApiModelProperty(value = "义项排序权重，值大的排前面", required = true)
        private Integer senseOrder;
```

- [ ] **Step 2：`VocabStructureVO` — 加 patternDef + patternDefTranslations**

```
old_string:
        @ApiModelProperty(value = "结构搭配文案", required = true)
        private String pattern;

        @ApiModelProperty(value = "搭配排序权重，值大的排前面", required = true)
        private Integer structureOrder;

new_string:
        @ApiModelProperty(value = "结构搭配文案", required = true)
        private String pattern;

        @ApiModelProperty(value = "结构搭配释义")
        private String patternDef;

        @ApiModelProperty(value = "结构搭配释义外文翻译列表")
        private List<TextTranslationVO> patternDefTranslations;

        @ApiModelProperty(value = "搭配排序权重，值大的排前面", required = true)
        private Integer structureOrder;
```

- [ ] **Step 3：`VocabExampleVO` — 加 image**

```
old_string:
        @ApiModelProperty(value = "例句外文翻译列表")
        private List<TextTranslationVO> translations;

        @ApiModelProperty(value = "例句排序权重，值大的排前面", required = true)
        private Integer exampleOrder;

new_string:
        @ApiModelProperty(value = "例句外文翻译列表")
        private List<TextTranslationVO> translations;

        @ApiModelProperty(value = "例句图片资源ID")
        private Long image;

        @ApiModelProperty(value = "例句排序权重，值大的排前面", required = true)
        private Integer exampleOrder;
```

- [ ] **Step 4：编译**

```bash
mvn -pl grid-system -am compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 5：提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java
git commit -m "feat(vocab): add new fields to VocabWordVO sub-types

Mirrors DTO additions for backoffice detail response.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 9：Wrapper 层补字段拷贝

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java`

- [ ] **Step 1：`toSenseDto` 加 defImage + relatedOther 拷贝**

```
old_string:
    private static VocabSenseDto toSenseDto(VocabWordCreateRequest.VocabSenseRequest request) {
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

new_string:
    private static VocabSenseDto toSenseDto(VocabWordCreateRequest.VocabSenseRequest request) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(request.getId());
        dto.setPartOfSpeech(request.getPartOfSpeech());
        dto.setChineseDef(request.getChineseDef());
        dto.setDefAudioId(request.getDefAudioId());
        dto.setDefImage(request.getDefImage());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setSynonyms(request.getSynonyms());
        dto.setAntonyms(request.getAntonyms());
        dto.setRelatedForward(request.getRelatedForward());
        dto.setRelatedBackward(request.getRelatedBackward());
        dto.setRelatedOther(request.getRelatedOther());
        dto.setSenseOrder(request.getSenseOrder() != null ? request.getSenseOrder() : 0);
        dto.setStructures(toStructureDtoList(request.getStructures()));
        return dto;
    }
```

- [ ] **Step 2：`toStructureDto` 加 patternDef + patternDefTranslations 拷贝**

```
old_string:
    private static VocabStructureDto toStructureDto(VocabWordCreateRequest.VocabStructureRequest request) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(request.getId());
        dto.setPattern(request.getPattern());
        dto.setStructureOrder(request.getStructureOrder() != null ? request.getStructureOrder() : 0);
        dto.setExamples(toExampleDtoList(request.getExamples()));
        return dto;
    }

new_string:
    private static VocabStructureDto toStructureDto(VocabWordCreateRequest.VocabStructureRequest request) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(request.getId());
        dto.setPattern(request.getPattern());
        dto.setPatternDef(request.getPatternDef());
        dto.setPatternDefTranslations(toTextTranslationList(request.getPatternDefTranslations()));
        dto.setStructureOrder(request.getStructureOrder() != null ? request.getStructureOrder() : 0);
        dto.setExamples(toExampleDtoList(request.getExamples()));
        return dto;
    }
```

- [ ] **Step 3：`toExampleDto` 加 image 拷贝**

```
old_string:
    private static VocabExampleDto toExampleDto(VocabWordCreateRequest.VocabExampleRequest request) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(request.getId());
        dto.setSentence(request.getSentence());
        dto.setAudioId(request.getAudioId());
        dto.setPinyin(request.getPinyin());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setExampleOrder(request.getExampleOrder() != null ? request.getExampleOrder() : 0);
        return dto;
    }

new_string:
    private static VocabExampleDto toExampleDto(VocabWordCreateRequest.VocabExampleRequest request) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(request.getId());
        dto.setSentence(request.getSentence());
        dto.setAudioId(request.getAudioId());
        dto.setPinyin(request.getPinyin());
        dto.setTranslations(toTextTranslationList(request.getTranslations()));
        dto.setImage(request.getImage());
        dto.setExampleOrder(request.getExampleOrder() != null ? request.getExampleOrder() : 0);
        return dto;
    }
```

- [ ] **Step 4：`toSenseVO` 加 defImage + relatedOther 拷贝**

```
old_string:
    private static VocabWordVO.VocabSenseVO toSenseVO(VocabSenseDto dto) {
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

new_string:
    private static VocabWordVO.VocabSenseVO toSenseVO(VocabSenseDto dto) {
        VocabWordVO.VocabSenseVO vo = new VocabWordVO.VocabSenseVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setPartOfSpeech(dto.getPartOfSpeech());
        vo.setChineseDef(dto.getChineseDef());
        vo.setDefAudioId(dto.getDefAudioId());
        vo.setDefImage(dto.getDefImage());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setSynonyms(dto.getSynonyms());
        vo.setAntonyms(dto.getAntonyms());
        vo.setRelatedForward(dto.getRelatedForward());
        vo.setRelatedBackward(dto.getRelatedBackward());
        vo.setRelatedOther(dto.getRelatedOther());
        vo.setSenseOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
```

- [ ] **Step 5：`toStructureVO` 加 patternDef + patternDefTranslations 拷贝**

```
old_string:
    private static VocabWordVO.VocabStructureVO toStructureVO(VocabStructureDto dto) {
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

new_string:
    private static VocabWordVO.VocabStructureVO toStructureVO(VocabStructureDto dto) {
        VocabWordVO.VocabStructureVO vo = new VocabWordVO.VocabStructureVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setSenseId(dto.getSenseId());
        vo.setPattern(dto.getPattern());
        vo.setPatternDef(dto.getPatternDef());
        vo.setPatternDefTranslations(toTextTranslationVOList(dto.getPatternDefTranslations()));
        vo.setStructureOrder(dto.getStructureOrder());
        vo.setExamples(toExampleVOList(dto.getExamples()));
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
```

- [ ] **Step 6：`toExampleVO` 加 image 拷贝**

```
old_string:
    private static VocabWordVO.VocabExampleVO toExampleVO(VocabExampleDto dto) {
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

new_string:
    private static VocabWordVO.VocabExampleVO toExampleVO(VocabExampleDto dto) {
        VocabWordVO.VocabExampleVO vo = new VocabWordVO.VocabExampleVO();
        vo.setId(dto.getId());
        vo.setWordId(dto.getWordId());
        vo.setSenseId(dto.getSenseId());
        vo.setStructureId(dto.getStructureId());
        vo.setSentence(dto.getSentence());
        vo.setAudioId(dto.getAudioId());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setImage(dto.getImage());
        vo.setExampleOrder(dto.getExampleOrder());
        vo.setCreateBy(dto.getCreateBy());
        vo.setUpdateBy(dto.getUpdateBy());
        vo.setCreateTime(dto.getCreateTime());
        vo.setUpdateTime(dto.getUpdateTime());
        return vo;
    }
```

- [ ] **Step 7：编译**

```bash
mvn -pl grid-system -am compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 8：提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/rest/wrapper/VocabWordWrapper.java
git commit -m "feat(vocab): copy new fields in VocabWordWrapper

Request->DTO and DTO->VO both updated for defImage, relatedOther,
patternDef, patternDefTranslations, image.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 10：Service 层补字段拷贝（已发布态读 + 发布写）

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

- [ ] **Step 1：`convertToSenseDto` — 加 defImage + relatedOther 读出**

```
old_string:
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

new_string:
    private VocabSenseDto convertToSenseDto(VocabSense sense) {
        VocabSenseDto dto = new VocabSenseDto();
        dto.setId(sense.getId());
        dto.setWordId(sense.getWordId());
        dto.setPartOfSpeech(sense.getPartOfSpeech());
        dto.setChineseDef(sense.getChineseDef());
        dto.setDefAudioId(sense.getDefAudioId());
        dto.setDefImage(sense.getDefImage());
        dto.setTranslations(JsonUtils.parseTranslationList(sense.getTranslations()));
        dto.setSynonyms(JsonUtils.parseStringList(sense.getSynonyms()));
        dto.setAntonyms(JsonUtils.parseStringList(sense.getAntonyms()));
        dto.setRelatedForward(JsonUtils.parseStringList(sense.getRelatedForward()));
        dto.setRelatedBackward(JsonUtils.parseStringList(sense.getRelatedBackward()));
        dto.setRelatedOther(JsonUtils.parseStringList(sense.getRelatedOther()));
        dto.setSenseOrder(sense.getSenseOrder());
```

- [ ] **Step 2：`convertToStructureDto` — 加 patternDef + patternDefTranslations 读出**

```
old_string:
    private VocabStructureDto convertToStructureDto(VocabStructure structure) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(structure.getId());
        dto.setWordId(structure.getWordId());
        dto.setSenseId(structure.getSenseId());
        dto.setPattern(structure.getPattern());
        dto.setStructureOrder(structure.getStructureOrder());

new_string:
    private VocabStructureDto convertToStructureDto(VocabStructure structure) {
        VocabStructureDto dto = new VocabStructureDto();
        dto.setId(structure.getId());
        dto.setWordId(structure.getWordId());
        dto.setSenseId(structure.getSenseId());
        dto.setPattern(structure.getPattern());
        dto.setPatternDef(structure.getPatternDef());
        dto.setPatternDefTranslations(JsonUtils.parseTranslationList(structure.getPatternDefTranslations()));
        dto.setStructureOrder(structure.getStructureOrder());
```

- [ ] **Step 3：`convertToExampleDto` — 加 image 读出**

```
old_string:
    private VocabExampleDto convertToExampleDto(VocabExample example) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(example.getId());
        dto.setWordId(example.getWordId());
        dto.setSenseId(example.getSenseId());
        dto.setStructureId(example.getStructureId());
        dto.setSentence(example.getSentence());
        dto.setAudioId(example.getAudioId());
        dto.setPinyin(example.getPinyin());
        dto.setTranslations(JsonUtils.parseTranslationList(example.getTranslations()));
        dto.setExampleOrder(example.getExampleOrder());

new_string:
    private VocabExampleDto convertToExampleDto(VocabExample example) {
        VocabExampleDto dto = new VocabExampleDto();
        dto.setId(example.getId());
        dto.setWordId(example.getWordId());
        dto.setSenseId(example.getSenseId());
        dto.setStructureId(example.getStructureId());
        dto.setSentence(example.getSentence());
        dto.setAudioId(example.getAudioId());
        dto.setPinyin(example.getPinyin());
        dto.setTranslations(JsonUtils.parseTranslationList(example.getTranslations()));
        dto.setImage(example.getImage());
        dto.setExampleOrder(example.getExampleOrder());
```

- [ ] **Step 4：`updateSense` — 发布时写回 defImage + relatedOther**

```
old_string:
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

new_string:
    private void updateSense(VocabSense entity, VocabSenseDto dto) {
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setChineseDef(dto.getChineseDef());
        entity.setDefAudioId(dto.getDefAudioId());
        entity.setDefImage(dto.getDefImage());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setSynonyms(JsonUtils.toStringListJson(dto.getSynonyms()));
        entity.setAntonyms(JsonUtils.toStringListJson(dto.getAntonyms()));
        entity.setRelatedForward(JsonUtils.toStringListJson(dto.getRelatedForward()));
        entity.setRelatedBackward(JsonUtils.toStringListJson(dto.getRelatedBackward()));
        entity.setRelatedOther(JsonUtils.toStringListJson(dto.getRelatedOther()));
        entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
    }
```

- [ ] **Step 5：`updateStructure` — 发布时写回 patternDef + patternDefTranslations**

```
old_string:
    private void updateStructure(VocabStructure entity, VocabStructureDto dto) {
        entity.setPattern(dto.getPattern());
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
    }

new_string:
    private void updateStructure(VocabStructure entity, VocabStructureDto dto) {
        entity.setPattern(dto.getPattern());
        entity.setPatternDef(dto.getPatternDef());
        entity.setPatternDefTranslations(JsonUtils.toTranslationJson(dto.getPatternDefTranslations()));
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
    }
```

- [ ] **Step 6：`updateExample` — 发布时写回 image**

```
old_string:
    private void updateExample(VocabExample entity, VocabExampleDto dto) {
        entity.setSentence(dto.getSentence());
        entity.setAudioId(dto.getAudioId());
        entity.setPinyin(dto.getPinyin());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
    }

new_string:
    private void updateExample(VocabExample entity, VocabExampleDto dto) {
        entity.setSentence(dto.getSentence());
        entity.setAudioId(dto.getAudioId());
        entity.setPinyin(dto.getPinyin());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setImage(dto.getImage());
        entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
    }
```

- [ ] **Step 7：`convertToSenseEntity` — 新建实体时写入新字段**

```
old_string:
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

new_string:
    private VocabSense convertToSenseEntity(VocabSenseDto dto, Integer wordId) {
        VocabSense entity = new VocabSense();
        entity.setWordId(wordId);
        entity.setPartOfSpeech(dto.getPartOfSpeech());
        entity.setChineseDef(dto.getChineseDef());
        entity.setDefAudioId(dto.getDefAudioId());
        entity.setDefImage(dto.getDefImage());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setSynonyms(JsonUtils.toStringListJson(dto.getSynonyms()));
        entity.setAntonyms(JsonUtils.toStringListJson(dto.getAntonyms()));
        entity.setRelatedForward(JsonUtils.toStringListJson(dto.getRelatedForward()));
        entity.setRelatedBackward(JsonUtils.toStringListJson(dto.getRelatedBackward()));
        entity.setRelatedOther(JsonUtils.toStringListJson(dto.getRelatedOther()));
        entity.setSenseOrder(dto.getSenseOrder() != null ? dto.getSenseOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }
```

- [ ] **Step 8：`convertToStructureEntity` — 新建实体写入新字段**

```
old_string:
    private VocabStructure convertToStructureEntity(VocabStructureDto dto, Integer wordId, Integer senseId) {
        VocabStructure entity = new VocabStructure();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setPattern(dto.getPattern());
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

new_string:
    private VocabStructure convertToStructureEntity(VocabStructureDto dto, Integer wordId, Integer senseId) {
        VocabStructure entity = new VocabStructure();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setPattern(dto.getPattern());
        entity.setPatternDef(dto.getPatternDef());
        entity.setPatternDefTranslations(JsonUtils.toTranslationJson(dto.getPatternDefTranslations()));
        entity.setStructureOrder(dto.getStructureOrder() != null ? dto.getStructureOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }
```

- [ ] **Step 9：`convertToExampleEntity` — 新建实体写入 image**

```
old_string:
    private VocabExample convertToExampleEntity(VocabExampleDto dto, Integer wordId, Integer senseId, Integer structureId) {
        VocabExample entity = new VocabExample();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setStructureId(structureId);
        entity.setSentence(dto.getSentence());
        entity.setAudioId(dto.getAudioId());
        entity.setPinyin(dto.getPinyin());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }

new_string:
    private VocabExample convertToExampleEntity(VocabExampleDto dto, Integer wordId, Integer senseId, Integer structureId) {
        VocabExample entity = new VocabExample();
        entity.setWordId(wordId);
        entity.setSenseId(senseId);
        entity.setStructureId(structureId);
        entity.setSentence(dto.getSentence());
        entity.setAudioId(dto.getAudioId());
        entity.setPinyin(dto.getPinyin());
        entity.setTranslations(JsonUtils.toTranslationJson(dto.getTranslations()));
        entity.setImage(dto.getImage());
        entity.setExampleOrder(dto.getExampleOrder() != null ? dto.getExampleOrder() : 0);
        entity.setStatus(StatusEnum.ENABLED.getCode());
        return entity;
    }
```

- [ ] **Step 10：编译**

```bash
mvn -pl grid-system -am compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 11：提交**

```bash
git add grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java
git commit -m "feat(vocab): wire new fields through service convert/update helpers

convertTo*Dto: read new entity fields (with JSON deserialization for
relatedOther and patternDefTranslations).
convertTo*Entity / update*: write new fields on publish (with JSON
serialization).

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 11：App 端 VO — 加 ImageVO 内部类 + 新字段

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabWordDetailVO.java`

- [ ] **Step 1：在 AudioVO 后新增 ImageVO 内部类**

```
old_string:
    @Getter
    @Setter
    public static class AudioVO implements Serializable {
        @ApiModelProperty(value = "音频文件地址")
        private String audioUrl;
    }

    @Getter
    @Setter
    public static class VocabSenseVO implements Serializable {

new_string:
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
    public static class VocabSenseVO implements Serializable {
```

- [ ] **Step 2：`VocabSenseVO` 加 defImage + relatedOther**

第一处 Edit：

```
old_string:
        @ApiModelProperty(value = "释义音频")
        private AudioVO defAudio;

        @ApiModelProperty(value = "外文翻译列表")
        private List<TextTranslationVO> translations;

new_string:
        @ApiModelProperty(value = "释义音频")
        private AudioVO defAudio;

        @ApiModelProperty(value = "释义图片")
        private ImageVO defImage;

        @ApiModelProperty(value = "外文翻译列表")
        private List<TextTranslationVO> translations;
```

第二处 Edit：

```
old_string:
        @ApiModelProperty(value = "逆序关联词汇")
        private List<RelatedWordVO> relatedBackward;

        @ApiModelProperty(value = "义项排序")
        private Integer senseOrder;

new_string:
        @ApiModelProperty(value = "逆序关联词汇")
        private List<RelatedWordVO> relatedBackward;

        @ApiModelProperty(value = "其他关联词汇")
        private List<RelatedWordVO> relatedOther;

        @ApiModelProperty(value = "义项排序")
        private Integer senseOrder;
```

- [ ] **Step 3：`VocabStructureVO` 加 patternDef + patternDefTranslations**

```
old_string:
        @ApiModelProperty(value = "搭配文案")
        private String pattern;

        @ApiModelProperty(value = "搭配排序")
        private Integer structureOrder;

new_string:
        @ApiModelProperty(value = "搭配文案")
        private String pattern;

        @ApiModelProperty(value = "搭配释义")
        private String patternDef;

        @ApiModelProperty(value = "搭配释义外文翻译列表")
        private List<TextTranslationVO> patternDefTranslations;

        @ApiModelProperty(value = "搭配排序")
        private Integer structureOrder;
```

- [ ] **Step 4：`VocabExampleVO` 加 image**

```
old_string:
        @ApiModelProperty(value = "例句外文翻译列表")
        private List<TextTranslationVO> translations;

        @ApiModelProperty(value = "例句排序")
        private Integer exampleOrder;

new_string:
        @ApiModelProperty(value = "例句外文翻译列表")
        private List<TextTranslationVO> translations;

        @ApiModelProperty(value = "例句图片")
        private ImageVO image;

        @ApiModelProperty(value = "例句排序")
        private Integer exampleOrder;
```

- [ ] **Step 5：编译**

```bash
mvn -pl grid-app -am compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 6：提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppVocabWordDetailVO.java
git commit -m "feat(app-vocab): add ImageVO and new fields to AppVocabWordDetailVO

ImageVO mirrors AudioVO ({ imageUrl }). New fields:
- VocabSenseVO: defImage (ImageVO), relatedOther (List<RelatedWordVO>)
- VocabStructureVO: patternDef (String), patternDefTranslations
- VocabExampleVO: image (ImageVO)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 12：App 端 Controller — 批量查 OSS + 装配新字段

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java`

- [ ] **Step 1：补 import**

第一处 Edit（顶部 import 区，紧跟 AudioResourceDto 之后）：

```
old_string:
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.modules.app.rest.request.AppVocabWordSearchRequest;

new_string:
import com.naon.grid.backend.service.resource.AudioResourceService;
import com.naon.grid.backend.service.resource.dto.AudioResourceDto;
import com.naon.grid.service.AliOssStorageService;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.modules.app.rest.request.AppVocabWordSearchRequest;
```

- [ ] **Step 2：注入 AliOssStorageService**

```
old_string:
    private final VocabWordService vocabWordService;
    private final AudioResourceService audioResourceService;
    private final VocabOutlineRecordService vocabOutlineRecordService;

new_string:
    private final VocabWordService vocabWordService;
    private final AudioResourceService audioResourceService;
    private final AliOssStorageService aliOssStorageService;
    private final VocabOutlineRecordService vocabOutlineRecordService;
```

- [ ] **Step 3：getDetail 同时查 audioMap + imageMap，传给 toDetailVO**

```
old_string:
    @ApiOperation("词汇详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppVocabWordDetailVO> getDetail(@PathVariable Integer id) {
        VocabWordDto dto = vocabWordService.findPublishedById(id);
        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);
        AppVocabWordDetailVO vo = toDetailVO(dto, audioMap);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }

new_string:
    @ApiOperation("词汇详情")
    @AnonymousGetMapping("/{id}")
    public ResponseEntity<AppVocabWordDetailVO> getDetail(@PathVariable Integer id) {
        VocabWordDto dto = vocabWordService.findPublishedById(id);
        Map<Long, AudioResourceDto> audioMap = collectAndBatchQueryAudios(dto);
        Map<Long, AliOssStorageDto> imageMap = collectAndBatchQueryImages(dto);
        AppVocabWordDetailVO vo = toDetailVO(dto, audioMap, imageMap);
        return new ResponseEntity<>(vo, HttpStatus.OK);
    }
```

- [ ] **Step 4：新增 collectAndBatchQueryImages 方法**

放在 `collectAndBatchQueryAudios` 方法之后：

```
old_string:
        List<AudioResourceDto> audioDtos = audioResourceService.findByIds(audioIds);
        return audioDtos.stream()
                .collect(Collectors.toMap(AudioResourceDto::getId, audio -> audio));
    }

    private AppVocabWordDetailVO toDetailVO(VocabWordDto dto, Map<Long, AudioResourceDto> audioMap) {

new_string:
        List<AudioResourceDto> audioDtos = audioResourceService.findByIds(audioIds);
        return audioDtos.stream()
                .collect(Collectors.toMap(AudioResourceDto::getId, audio -> audio));
    }

    private Map<Long, AliOssStorageDto> collectAndBatchQueryImages(VocabWordDto dto) {
        List<Long> imageIds = new ArrayList<>();
        if (dto.getSenses() != null) {
            for (VocabSenseDto sense : dto.getSenses()) {
                if (sense.getDefImage() != null) {
                    imageIds.add(sense.getDefImage());
                }
                if (sense.getStructures() != null) {
                    for (VocabStructureDto structure : sense.getStructures()) {
                        if (structure.getExamples() != null) {
                            for (VocabExampleDto example : structure.getExamples()) {
                                if (example.getImage() != null) {
                                    imageIds.add(example.getImage());
                                }
                            }
                        }
                    }
                }
            }
        }
        List<AliOssStorageDto> imageDtos = aliOssStorageService.findByIds(imageIds);
        return imageDtos.stream()
                .collect(Collectors.toMap(AliOssStorageDto::getId, img -> img));
    }

    private AppVocabWordDetailVO toDetailVO(VocabWordDto dto, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
```

- [ ] **Step 5：toDetailVO 内部调 toSenseVOList 传 imageMap**

```
old_string:
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

new_string:
        vo.setHskLevel(dto.getHskLevel());
        vo.setSenses(toSenseVOList(dto.getSenses(), audioMap, imageMap));
        vo.setExercises(toExerciseVOList(dto.getExercises()));
        return vo;
    }

    private List<AppVocabWordDetailVO.VocabSenseVO> toSenseVOList(List<VocabSenseDto> dtos, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toSenseVO(dto, audioMap, imageMap)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabSenseVO toSenseVO(VocabSenseDto dto, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
```

- [ ] **Step 6：toSenseVO 内部装配 defImage、relatedOther，并把 imageMap 透传到 toStructureVOList**

```
old_string:
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setSynonyms(toSynonymVOList(dto.getSynonyms()));
        vo.setAntonyms(toAntonymVOList(dto.getAntonyms()));
        vo.setRelatedForward(toRelatedWordVOList(dto.getRelatedForward()));
        vo.setRelatedBackward(toRelatedWordVOList(dto.getRelatedBackward()));
        vo.setSenseOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures(), audioMap));
        return vo;
    }

new_string:
        if (dto.getDefImage() != null && imageMap.containsKey(dto.getDefImage())) {
            AppVocabWordDetailVO.ImageVO imageVO = new AppVocabWordDetailVO.ImageVO();
            imageVO.setImageUrl(imageMap.get(dto.getDefImage()).getFileUrl());
            vo.setDefImage(imageVO);
        }
        vo.setTranslations(toTextTranslationVOList(dto.getTranslations()));
        vo.setSynonyms(toSynonymVOList(dto.getSynonyms()));
        vo.setAntonyms(toAntonymVOList(dto.getAntonyms()));
        vo.setRelatedForward(toRelatedWordVOList(dto.getRelatedForward()));
        vo.setRelatedBackward(toRelatedWordVOList(dto.getRelatedBackward()));
        vo.setRelatedOther(toRelatedWordVOList(dto.getRelatedOther()));
        vo.setSenseOrder(dto.getSenseOrder());
        vo.setStructures(toStructureVOList(dto.getStructures(), audioMap, imageMap));
        return vo;
    }
```

- [ ] **Step 7：toStructureVOList / toStructureVO 透传 imageMap，装配 patternDef、patternDefTranslations**

```
old_string:
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

new_string:
    private List<AppVocabWordDetailVO.VocabStructureVO> toStructureVOList(List<VocabStructureDto> dtos, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toStructureVO(dto, audioMap, imageMap)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabStructureVO toStructureVO(VocabStructureDto dto, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        AppVocabWordDetailVO.VocabStructureVO vo = new AppVocabWordDetailVO.VocabStructureVO();
        vo.setId(dto.getId());
        vo.setPattern(dto.getPattern());
        vo.setPatternDef(dto.getPatternDef());
        vo.setPatternDefTranslations(toTextTranslationVOList(dto.getPatternDefTranslations()));
        vo.setStructureOrder(dto.getStructureOrder());
        vo.setExamples(toExampleVOList(dto.getExamples(), audioMap, imageMap));
        return vo;
    }
```

- [ ] **Step 8：toExampleVOList / toExampleVO 透传 imageMap，装配 image**

```
old_string:
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

new_string:
    private List<AppVocabWordDetailVO.VocabExampleVO> toExampleVOList(List<VocabExampleDto> dtos, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(dto -> toExampleVO(dto, audioMap, imageMap)).collect(Collectors.toList());
    }

    private AppVocabWordDetailVO.VocabExampleVO toExampleVO(VocabExampleDto dto, Map<Long, AudioResourceDto> audioMap, Map<Long, AliOssStorageDto> imageMap) {
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
        if (dto.getImage() != null && imageMap.containsKey(dto.getImage())) {
            AppVocabWordDetailVO.ImageVO imageVO = new AppVocabWordDetailVO.ImageVO();
            imageVO.setImageUrl(imageMap.get(dto.getImage()).getFileUrl());
            vo.setImage(imageVO);
        }
        vo.setExampleOrder(dto.getExampleOrder());
        return vo;
    }
```

- [ ] **Step 9：编译 grid-app 验证**

```bash
mvn -pl grid-app -am compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 10：提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppVocabWordController.java
git commit -m "feat(app-vocab): batch resolve image IDs to OSS URLs in detail

Adds collectAndBatchQueryImages mirroring the audio flow. Sense.defImage
and Example.image are returned as ImageVO { imageUrl }. Missing OSS
records yield null fields (graceful, like audio). Sense.relatedOther,
Structure.patternDef, Structure.patternDefTranslations also exposed.
Image resource IDs are never returned to the client.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Task 13：全量编译 + 数据库迁移脚本

**Files:** —

- [ ] **Step 1：全工程编译**

```bash
mvn clean compile -DskipTests -q
```

预期：BUILD SUCCESS（所有 6 个模块）。

- [ ] **Step 2：在 dev MySQL 上手动执行字段迁移**

把下面 SQL 在本地/dev MySQL 中执行（与 `sql/vocabulary.sql` 一致）：

```sql
ALTER TABLE `vocab_sense`
  ADD COLUMN `def_image` int(11) DEFAULT NULL COMMENT '中文释义图片(ID)' AFTER `def_audio_id`,
  ADD COLUMN `related_other` text DEFAULT NULL COMMENT '其他关联词汇, JSON列表格式List<String>' AFTER `related_backward`;

-- 已存在但被旧脚本建成 NOT NULL 的列：先 MODIFY 成 DEFAULT NULL（用 IF NOT EXISTS 友好处理或手动判断后只跑一种）
-- A. 全新环境：ADD COLUMN
ALTER TABLE `vocab_structure`
  ADD COLUMN `pattern_def` varchar(512) DEFAULT NULL COMMENT '结构搭配释义(可空)' AFTER `pattern`,
  ADD COLUMN `pattern_def_translations` varchar(1024) DEFAULT NULL COMMENT '结构搭配释义外文翻译, JSON列表格式(List<TextTranslation>)' AFTER `pattern_def`;

-- B. 已经按旧（NOT NULL）DDL 建过列的环境：MODIFY COLUMN 改为可空
ALTER TABLE `vocab_structure`
  MODIFY COLUMN `pattern_def` varchar(512) DEFAULT NULL COMMENT '结构搭配释义(可空)',
  MODIFY COLUMN `pattern_def_translations` varchar(1024) DEFAULT NULL COMMENT '结构搭配释义外文翻译, JSON列表格式(List<TextTranslation>)';

ALTER TABLE `vocab_example`
  ADD COLUMN `image` int(11) DEFAULT NULL COMMENT '例句图片(ID)' AFTER `translations`;
```

预期：3 个 ALTER 语句执行成功。

- [ ] **Step 3：启动应用，准备 Knife4j 手测**

```bash
cd grid-bootstrap && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

等待启动完成（监听 8000），保持运行，进入下一步。

---

## Task 14：Knife4j 手测验证（不提交代码）

**Files:** —

> 浏览器打开 http://localhost:8000/doc.html ，定位 "后台：词汇-词汇管理" 与 "用户：词汇接口"。

- [ ] **Step 1：新增带新字段的词汇**

调 `POST /api/vocabulary`，请求体示例（包含 5 个新字段）：

```json
{
  "word": "测试词",
  "wordTraditional": "測試詞",
  "pinyin": "cè shì cí",
  "audioId": null,
  "hskLevel": "3",
  "senses": [
    {
      "partOfSpeech": "名词",
      "chineseDef": "用于测试",
      "defAudioId": null,
      "defImage": 1,
      "translations": [{"language": "en", "translation": "test word"}],
      "synonyms": ["测验词"],
      "antonyms": [],
      "relatedForward": [],
      "relatedBackward": [],
      "relatedOther": ["示例", "样本"],
      "senseOrder": 0,
      "structures": [
        {
          "pattern": "测试 + N",
          "patternDef": "对某事物进行测试",
          "patternDefTranslations": [{"language": "en", "translation": "to test something"}],
          "structureOrder": 0,
          "examples": [
            {
              "sentence": "测试系统",
              "audioId": null,
              "pinyin": "cè shì xì tǒng",
              "translations": [{"language": "en", "translation": "test the system"}],
              "image": 1,
              "exampleOrder": 0
            }
          ]
        }
      ]
    }
  ],
  "exercises": []
}
```

预期：返回 `201`，body 含新建 id。**记下这个 id 为 `WID`**。

- [ ] **Step 2：查询草稿态详情，确认 5 个字段都回来了**

调 `GET /api/vocabulary/{WID}`，预期响应中：
- `senses[0].defImage == 1`
- `senses[0].relatedOther == ["示例", "样本"]`
- `senses[0].structures[0].patternDef == "对某事物进行测试"`
- `senses[0].structures[0].patternDefTranslations[0].language == "en"`
- `senses[0].structures[0].examples[0].image == 1`

- [ ] **Step 3：审核并发布**

调 `PUT /api/vocabulary/{WID}/review` → 预期 `204`。
调 `PUT /api/vocabulary/{WID}/publish` → 预期 `204`。

- [ ] **Step 4：查询已发布态详情，确认 5 个字段从子表读回**

再次调 `GET /api/vocabulary/{WID}`，预期同 Step 2 各字段值仍存在。

通过 MySQL 客户端核验：

```sql
SELECT id, def_image, related_other FROM vocab_sense WHERE word_id = <WID>;
SELECT id, pattern_def, pattern_def_translations FROM vocab_structure WHERE word_id = <WID>;
SELECT id, image FROM vocab_example WHERE word_id = <WID>;
```

预期：5 列都有值（关联表的 JSON 列是合法 JSON 字符串）。

- [ ] **Step 5：App 端详情验证 OSS URL**

> 前置：确保 `ali_oss_storage` 表中存在 id=1 的图片记录（可通过 `POST /api/aliOssStorage/pictures` 上传一张），不存在的话 App 端 image 字段会是 null（这也是合规行为）。

调 `GET /api/app/vocab/{WID}`，预期：
- `senses[0].defImage.imageUrl` 是有效 OSS URL（或字段为 null 当 id=1 不存在时）
- `senses[0].relatedOther[*].content` 是 `"示例"` 等
- `senses[0].structures[0].patternDef == "对某事物进行测试"`
- `senses[0].structures[0].patternDefTranslations[0].translation == "to test something"`
- `senses[0].structures[0].examples[0].image.imageUrl` 是有效 OSS URL 或 null
- 响应中**完全没有** `defImage: <数字>` 或 `image: <数字>` 这样的裸 ID

- [ ] **Step 6：更新接口（验证旧记录修改）**

调 `PUT /api/vocabulary/{WID}`，请求体把 `relatedOther` 改成 `["示例"]`，把 `patternDef` 改成 `"测试用法变体"`。预期：`204`。

调 `GET /api/vocabulary/{WID}`，确认草稿里这两个字段值已更新。

再次审核 + 发布，调 `GET /api/vocabulary/{WID}` 与 `GET /api/app/vocab/{WID}`，确认子表字段值也已更新。

- [ ] **Step 7：手测全部通过后停止应用**

ctrl+C 关闭 `mvn spring-boot:run`。

---

## Task 15：合并准备

**Files:** —

- [ ] **Step 1：确认所有提交记录**

```bash
git log --oneline master..HEAD
```

预期：约 11 条 commit（task 1-12 中除 task 0/13/14 外）。

- [ ] **Step 2：合并设计文档分支（可选）**

如果设计文档分支 `docs/vocab-new-fields-design` 也想合入，先 merge 进来：

```bash
git merge --no-ff docs/vocab-new-fields-design -m "docs: include vocab new-fields design"
```

- [ ] **Step 3：推送等待 PR**

```bash
git push -u origin feat/vocab-new-fields
```

然后由你决定是否走 PR / 直接合并到 master。**计划到此结束**，PR 与合并步骤需要用户授权后再执行。
