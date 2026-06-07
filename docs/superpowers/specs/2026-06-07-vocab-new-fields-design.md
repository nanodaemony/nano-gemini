# 词汇表新增字段补充设计

- 日期：2026-06-07
- 范围：`grid-system` 后台 `VocabWordController` 全链路 + `grid-app` 用户端 `AppVocabWordController` 详情接口
- 触发：`sql/vocabulary.sql` 在 `vocab_sense` / `vocab_structure` / `vocab_example` 三张表新增 5 个字段，Java 各层尚未跟进

## 一、SQL 新增字段（diff 自 HEAD）

| 表 | 字段 | 类型 | 含义 |
|---|---|---|---|
| `vocab_sense` | `def_image` | `int(11) DEFAULT NULL` | 中文释义图片（OSS 资源 ID） |
| `vocab_sense` | `related_other` | `text DEFAULT NULL` | 其他关联词汇，JSON `List<String>` |
| `vocab_structure` | `pattern_def` | `varchar(512)` | 结构搭配释义 |
| `vocab_structure` | `pattern_def_translations` | `varchar(1024)` | 结构搭配释义外文翻译，JSON `List<TextTranslation>` |
| `vocab_example` | `image` | `int(11) DEFAULT NULL` | 例句图片（OSS 资源 ID） |

### SQL 修订

`vocab_structure.pattern_def` 与 `pattern_def_translations` 在 DDL 中误标 `NOT NULL`，需要改为可空（与注释及业务一致）：

```sql
`pattern_def` varchar(512) DEFAULT NULL COMMENT '结构搭配释义(可空)',
`pattern_def_translations` varchar(1024) DEFAULT NULL COMMENT '结构搭配释义外文翻译, JSON列表格式(List<TextTranslation>)',
```

## 二、接口影响评估

`VocabWordController` 9 个端点中：

- 🔴 **需要补字段**：`POST /api/vocabulary`（新增）、`PUT /api/vocabulary/{id}`（更新）、`PUT /api/vocabulary/{id}/publish`（发布）、`GET /api/vocabulary/{id}`（详情）
- 🟡 **无需改动**：`GET /api/vocabulary`（列表 VO 仅展示主表字段，新字段全在子表）
- ⚪ **无影响**：`/{id}/review`、`DELETE /{id}`、`/{id}/offline`、`/outline`、`/outline/{id}/complete`

App 端 `AppVocabWordController.getDetail` 需要同步暴露新字段；其中两个图片字段必须经 OSS 服务解析为 URL 后返回，**不暴露资源 ID**。

## 三、各层补充清单

### A. `vocab_sense` 新增 `defImage` + `relatedOther`

| 层 | 文件 | 改动 |
|---|---|---|
| Entity | `grid-system/.../domain/vocabulary/VocabSense.java` | 加 `Long defImage`（映射 `def_image`，类型对齐 `defAudioId`）；加 `String relatedOther`（`columnDefinition="text"`） |
| DTO | `grid-system/.../service/vocabulary/dto/VocabSenseDto.java` | 加 `Long defImage`；加 `List<String> relatedOther` |
| Request | `VocabWordCreateRequest.VocabSenseRequest` | 加同上字段，**不加** `@NotNull` |
| VO | `VocabWordVO.VocabSenseVO` | 加同上字段 |
| Wrapper | `VocabWordWrapper.toSenseDto / toSenseVO` | 拷贝两字段 |
| Service | `VocabWordServiceImpl.convertToSenseDto` | `dto.setDefImage(sense.getDefImage()); dto.setRelatedOther(JsonUtils.parseStringList(sense.getRelatedOther()));` |
| Service | `convertToSenseEntity` / `updateSense` | 反向拷贝；`relatedOther` 走 `JsonUtils.toStringListJson` |

> 类型说明：DB 是 `int(11)`，但 Entity 沿用 `defAudioId` 的 `Long` 风格，避免引入新混乱。

### B. `vocab_structure` 新增 `patternDef` + `patternDefTranslations`

| 层 | 文件 | 改动 |
|---|---|---|
| Entity | `VocabStructure.java` | 加 `String patternDef`（length=512, nullable=true）；加 `String patternDefTranslations`（length=1024, nullable=true）— Entity 内仍存 JSON 字符串 |
| DTO | `VocabStructureDto.java` | 加 `String patternDef`；加 `List<TextTranslation> patternDefTranslations` |
| Request | `VocabWordCreateRequest.VocabStructureRequest` | 加 `String patternDef`；加 `List<TextTranslationRequest> patternDefTranslations`（不加 `@NotBlank`） |
| VO | `VocabWordVO.VocabStructureVO` | 加 `String patternDef`；加 `List<TextTranslationVO> patternDefTranslations` |
| Wrapper | `VocabWordWrapper.toStructureDto / toStructureVO` | 拷贝；translations 复用现有 `toTextTranslationList / toTextTranslationVOList` |
| Service | `convertToStructureDto` | `dto.setPatternDef(structure.getPatternDef()); dto.setPatternDefTranslations(JsonUtils.parseTranslationList(structure.getPatternDefTranslations()));` |
| Service | `convertToStructureEntity` / `updateStructure` | 反向拷贝；translations 走 `JsonUtils.toTranslationJson` |

### C. `vocab_example` 新增 `image`

| 层 | 文件 | 改动 |
|---|---|---|
| Entity | `VocabExample.java` | 加 `Long image`（`@Column(name="image")`，类型对齐 `audioId` 的 `Long`） |
| DTO | `VocabExampleDto.java` | 加 `Long image` |
| Request | `VocabWordCreateRequest.VocabExampleRequest` | 加 `Long image` |
| VO | `VocabWordVO.VocabExampleVO` | 加 `Long image` |
| Wrapper | `toExampleDto / toExampleVO` | 拷贝 |
| Service | `convertToExampleDto / convertToExampleEntity / updateExample` | 各加一行 image 拷贝 |

> 命名：保持 DB 原名 `image`，Java 字段就叫 `image`（`Long` 类型，语义是 OSS 资源 ID）。

### D. 后台 4 个核心接口字段流转验证

| 接口 | 流转路径 |
|---|---|
| 新增 / 更新 | Request → `Wrapper.toDto` → `VocabWordDto` → `JsonUtils.toJson` 写入 `vocab_word.draft_content`。**只要 DTO 加了字段，Fastjson2 自动序列化** |
| 详情（草稿态） | `JsonUtils.fromJson(draftContent, VocabWordDto.class)` → `Wrapper.toVO`。**只要 DTO/VO 字段对齐即可** |
| 详情（已发布态） | 走 `convertToSenseDto / convertToStructureDto / convertToExampleDto`，**需要在这三处补字段拷贝**（已列在 A/B/C） |
| 发布 | `publishDraft` → `syncSenses → syncStructures → syncExamples` → 最终走 `convertTo*Entity` / `update*`，**需要在这些处补字段拷贝**（已列在 A/B/C） |

### E. App 端详情（图片需经 OSS 解析为 URL）

#### E.1 OSS 服务补一个批量查询接口

`AliOssStorageService` 目前只有 `findById(Long)`，为避免 N+1，参考 `AudioResourceService.findByIds` 的模式新增：

- 接口：`grid-tools/.../service/AliOssStorageService.java`
  ```java
  /**
   * 批量根据 ID 查询 OSS 资源
   */
  List<AliOssStorageDto> findByIds(List<Long> ids);
  ```
- 实现：`grid-tools/.../service/impl/AliOssStorageServiceImpl.java`
  ```java
  @Override
  public List<AliOssStorageDto> findByIds(List<Long> ids) {
      if (ids == null || ids.isEmpty()) {
          return Collections.emptyList();
      }
      return aliOssStorageMapper.toDto(aliOssStorageRepository.findAllById(ids));
  }
  ```

#### E.2 `AppVocabWordController` 改动

- 注入 `AliOssStorageService`
- 新增 `Map<Long, AliOssStorageDto> collectAndBatchQueryImages(VocabWordDto dto)`：
  - 遍历 `dto.senses[*].defImage` 与 `dto.senses[*].structures[*].examples[*].image`，收集非空 ID 列表
  - `aliOssStorageService.findByIds(ids)` 一次性查回
  - 用 `AliOssStorageDto::getId` 作 key 组装 Map
- `toDetailVO(dto, audioMap)` 改为 `toDetailVO(dto, audioMap, imageMap)`；`toSenseVO` / `toExampleVO` 同步增加 `imageMap` 参数
- 在 `toSenseVO`：若 `dto.getDefImage() != null && imageMap.containsKey(...)`，构造 `ImageVO` 并 set；否则字段保持 null（与 audio 同款容错）
- 在 `toStructureVO`：直接 set `patternDef` 与 `patternDefTranslations`（无外部依赖）
- 在 `toExampleVO`：同 `toSenseVO` 处理 image

#### E.3 `AppVocabWordDetailVO` 改动

- 新增内部类 `ImageVO { String imageUrl; }`，结构对齐现有 `AudioVO`
- `VocabSenseVO`：加 `ImageVO defImage`、`List<RelatedWordVO> relatedOther`
- `VocabStructureVO`：加 `String patternDef`、`List<TextTranslationVO> patternDefTranslations`
- `VocabExampleVO`：加 `ImageVO image`

### F. 跨模块依赖说明

- 依赖链：`grid-app → grid-system → grid-tools`，`AliOssStorageService` 在 `grid-tools`，`grid-app` 可直接注入（与 `AudioResourceService` 同款用法）
- `AliOssStorageDto` 已包含 `fileUrl` 字段（OSS 完整访问 URL），无需改 OSS 侧 DTO

## 四、错误处理与校验

- 5 个字段全部可空，Request 端不加 `@NotBlank` / `@NotNull`
- `patternDefTranslations` 内部的 `TextTranslationRequest` 仍按其自身现有校验
- App 端图片字段命不中 OSS（图片被删 / 资源 ID 失效）时，对应字段返回 null，**不抛错**
- Service 层对新字段不做额外业务校验

## 五、不在本次范围

- 列表接口 `GET /api/vocabulary`（`VocabWordBaseVO`）维持只展示主表字段
- 不引入 `relatedOther` 在列表/搜索条件上的索引或查询能力
- 不改 OSS 业务类型枚举（图片上传已有 `OssBusinessType` 流程，由调用方决定）

## 六、验证清单

1. `mvn -pl grid-system,grid-app,grid-tools -am compile` 通过
2. 数据库手动加列（脚本里的 5 个字段 + `pattern_def`、`pattern_def_translations` 改可空）
3. Knife4j 手测：
   - 后台：新增（携带 5 个新字段） → 详情（草稿态字段不丢） → 审核 → 发布 → 详情（已发布态字段不丢）
   - 后台：更新（修改新字段值） → 再次发布 → 校验子表新字段值与 Request 一致
   - App 端：访问已发布词汇详情，确认 `defImage.imageUrl` / `examples[*].image.imageUrl` 是有效 OSS URL；`relatedOther`、`patternDef`、`patternDefTranslations` 正常返回；图片 ID 无效时字段为 null 而非报错
