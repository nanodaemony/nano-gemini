# 词汇与汉字后台列表页：草稿回填与状态筛选

- 日期：2026-06-07
- 作者：chenzeng
- 涉及模块：grid-system（backend 包）
- 涉及接口：`GET /api/vocabulary`、`GET /api/character`

## 背景

后台词汇管理与汉字管理使用同一套草稿工作流：

- 主表 `vocab_word` / `char_character` 上的业务字段（`pinyin`、`audio_id`、`hsk_level` / `level` 等）只有在 `publishDraft` 时才会从 `draft_content` 同步回写。
- 创建后未发布、或发布后再次编辑的行，主表业务字段为 `null`，真实的最新内容只存在于 `draft_content` JSON 中。
- 现有列表接口 `queryAll` 直接走 `MapStruct` 把主表实体转成 DTO，对草稿态行只能拿到一堆 `null`。
- 前端目前没有按"发布状态/编辑状态"过滤列表的能力——`VocabWordQueryRequest` 上没有这两个字段；`VocabWordQueryCriteria` 上虽然声明了 `publishStatus` 并带 `@Query` 注解，但 `VocabWordWrapper.toCriteria` 从未将其透传，是一个隐藏 bug。`CharCharacterQueryRequest` / `CharCharacterQueryCriteria` 完全对称。

## 目标

让两个列表页"显示最新内容"：

1. 入参新增 `publishStatus`、`editStatus` 两个可选筛选条件，单值精确匹配。
2. 草稿态行（`editStatus ∈ {draft, reviewed}`）从主表实体先得到 DTO 后，再用 `draftContent` 中的业务字段覆盖之；非草稿态行保持原样。

## 非目标

- 详情接口 `findById` 的草稿处理已经在两个 ServiceImpl 中各自实现（见 `VocabWordServiceImpl#findById`、`CharCharacterServiceImpl#findById`），本次不动。
- 子表（`senses` / `exercises` / `discriminations` / `words`）的展示——列表页本就不返回。
- 纲外词列表 `queryOutline` 与本次主题无关，不动。
- 前端代码。

## 影响范围

涉及两套各 5 个文件，共 10 个文件，结构完全对称：

| 层 | 词汇 | 汉字 |
|---|---|---|
| Request | `VocabWordQueryRequest` | `CharCharacterQueryRequest` |
| Criteria | `VocabWordQueryCriteria` | `CharCharacterQueryCriteria` |
| Wrapper | `VocabWordWrapper` | `CharCharacterWrapper` |
| BaseVO | `VocabWordBaseVO` | `CharCharacterBaseVO` |
| ServiceImpl | `VocabWordServiceImpl` | `CharCharacterServiceImpl` |

Controller、`findById` / `findPublishedById` / `create` / `update` / `reviewDraft` / `publishDraft` / `offline` / `delete` 均不改动。

## 设计

### 入参变更（两侧对称）

**Request 层**：`VocabWordQueryRequest` 与 `CharCharacterQueryRequest` 各新增两个可选字段：

```java
@ApiModelProperty(value = "发布状态: unpublished / published")
private String publishStatus;

@ApiModelProperty(value = "编辑状态: draft / reviewed / published")
private String editStatus;
```

**Criteria 层**：

- `VocabWordQueryCriteria` 已经有 `publishStatus`（带 `@Query` 注解），仅新增 `editStatus`：

```java
@ApiModelProperty(value = "编辑状态: draft / reviewed / published")
@Query
private String editStatus;
```

- `CharCharacterQueryCriteria` 同款新增 `editStatus`（`publishStatus` 已存在）。

**Wrapper 层**：`toCriteria` 透传两个字段，顺带修复 `publishStatus` 从未透传的 bug：

```java
public static VocabWordQueryCriteria toCriteria(VocabWordQueryRequest request) {
    VocabWordQueryCriteria criteria = new VocabWordQueryCriteria();
    criteria.setBlurry(request.getBlurry());
    criteria.setPublishStatus(request.getPublishStatus());
    criteria.setEditStatus(request.getEditStatus());
    return criteria;
}
```

`CharCharacterWrapper.toCriteria` 完全同款。注意：`CharCharacterQueryCriteria.searchCharacterOnly` 与本次无关，保留现状。

**过滤语义**：三个字段（`blurry` / `publishStatus` / `editStatus`）任意组合 AND，由 `QueryHelp.getPredicate` 配合 `@Query`（默认 `EQUAL`）完成；任一为 null / 空则不参与对应过滤——这是 `QueryHelp` 现有行为，无需额外代码。入参不做枚举值合法性校验，非法值就是查不出结果，与项目内其它枚举入参处理风格一致。

### 草稿回填核心（方案 A：显式逐字段覆盖）

#### `VocabWordServiceImpl.queryAll`

```java
@Override
public PageResult<VocabWordDto> queryAll(VocabWordQueryCriteria criteria, Pageable pageable) {
    Page<VocabWord> page = vocabWordRepository.findAll((root, cq, cb) -> {
        Predicate basePredicate = QueryHelp.getPredicate(root, criteria, cb);
        Predicate statusPredicate = cb.equal(root.get("status"), StatusEnum.ENABLED.getCode());
        return cb.and(basePredicate, statusPredicate);
    }, pageable);
    return PageUtil.toPage(page.map(this::toDtoWithDraftOverlay));
}

/** 主表 -> DTO；若处于 draft/reviewed，将 draftContent 中的业务字段叠加回 DTO。 */
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
 * 覆盖范围应与 VocabWordBaseVO 暴露的业务字段保持一致。BaseVO 新增业务字段时，
 * 请同步在此方法添加覆盖。
 *
 * 永远不覆盖：id、status、publishStatus、editStatus、createBy、updateBy、
 * createTime、updateTime——这些字段以主表为准。
 *
 * 不读取：senses、exercises——列表页不返回子表。
 *
 * @throws BadRequestException 草稿数据缺失（与 reviewDraft/publishDraft 一致）
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

#### `CharCharacterServiceImpl.queryAll`

完全同款；`applyDraftOverlay` 覆盖的字段集对应 `CharCharacterBaseVO` 暴露的业务字段：

- `sequenceNo`
- `character`
- `level`
- `pinyin`
- `audioId`
- `traditional`
- `radical`
- `stroke`
- `charDesc`
- `descTranslations`

`descTranslations` 是 `List<TextTranslation>`，`null` 与 `empty` 都按 `null` 处理（与其它字段一致：草稿里没填就不覆盖主表）。

#### 为什么不抽公共基类 / 公共工具

两个 Service 各管各的领域 DTO（`VocabWordDto` 与 `CharCharacterDto` 不共享接口），方法签名与字段集都不同；抽公共基类需要引入泛型反射或破坏现有 MapStruct 用法，收益不抵成本。把"逐字段覆盖"留在每个 ServiceImpl 内、命名一致（`toDtoWithDraftOverlay` + `applyDraftOverlay`）就已足够清晰；新增 BaseVO 字段时只需改一个集中位置。

### 响应 VO 变更

`VocabWordBaseVO` 与 `CharCharacterBaseVO`：删除 `hasDraft` 字段及其 `@ApiModelProperty`。

`VocabWordWrapper.toBaseVO` 与 `CharCharacterWrapper.toBaseVO`：删除 `vo.setHasDraft(...)` 那一行。其余 setter 不变——因为草稿回填已经在 Service 层完成，Wrapper 看到的 DTO 字段已经是"最新值"。

`VocabWordVO`（详情 VO）与 `CharCharacterVO` 的 `hasDraft` 字段保留不动；`toVO` 中的 `vo.setHasDraft(...)` 也保留——本次只动列表 VO。

## 边界与影响面

- **性能**：每页默认 20 行，单页内每个草稿态行做一次 `JsonUtils.fromJson` 反序列化，与既有 `findById` 单次解析同量级，不需要批量预读或缓存。
- **并发与事务**：`queryAll` 不需要 `@Transactional`（沿用现状），JPA Specification 单条查询，无并发风险。
- **草稿数据异常**：若发现某行 `editStatus ∈ {draft, reviewed}` 但 `draftContent` 为 `null` 或 JSON 解析失败 → 抛 `BadRequestException`，整页失败。语义上向 `reviewDraft` / `publishDraft` 看齐——这是数据脏漏，需要后台运维介入，而不是悄悄掩盖。
- **前端协议变更**（需要前端同步）：
  - 列表项响应**移除** `hasDraft`。
  - 列表入参**新增**可选 `publishStatus`、`editStatus`。
- **回归风险**：所有非 `queryAll` 接口零改动；`queryAll` 对非草稿态行的转换结果与改动前完全一致（`MapStruct` 转换路径未变），仅对草稿态行多了一步字段覆盖。
