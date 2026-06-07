# 汉字子表展示排序字段设计

**日期**: 2026-06-07
**模块**: grid-system（汉字管理）+ grid-app（用户端汉字详情）
**关联表**: `char_discrimination`、`char_word`
**参考实现**: vocab 模块的 `VocabSense.senseOrder` / `VocabStructure.structureOrder` / `VocabExample.exampleOrder` / `VocabExercise.exerciseOrder`

## 背景与目标

汉字主表 `char_character` 下有两张子表：
- 辨析表 `char_discrimination`（一个汉字 → 多个辨析字）
- 组词表 `char_word`（一个汉字 → 多个组词）

目前两张子表没有展示排序字段，前端拿到的是数据库自然顺序，编辑后的展示顺序不可控。需要给两张子表各加一个排序字段，用于控制 App 端 / 后台详情页的展示顺序，**值大的排前面**。

vocab 模块在 4 个子表上已有完整的 `xxxOrder` 端到端实现（实体 → DTO → Request → VO → Wrapper → Service），本设计参照 vocab 的模板，将同款逻辑应用到 char 模块的两张子表。

## 数据库层

SQL 文件 `sql/character.sql` 中已新增字段（本次设计前已存在）：

```sql
-- char_discrimination 表
`discrimination_order` int(11) DEFAULT '0' COMMENT '辨析排序(值大的排前面)',

-- char_word 表
`word_order` int(11) DEFAULT '0' COMMENT '组词排序(值大的排前面)',
```

类型 `int(11)`、默认 `0`，与 vocab 子表的 `sense_order` 等保持一致。

## 总体方案

**核心决策**：

| 决策点 | 选择 | 理由 |
|--|--|--|
| Java 字段类型 | `Integer` | 与 vocab 模板（`VocabSense.senseOrder` 等）一致 |
| 排序方向 | 降序（值大在前） | 对齐 SQL 注释，便于"置顶"语义 |
| 排序实现位置 | Service 层手动排序 | Repo 不加 OrderBy 方法，便于未来扩展；统一收敛在两个 helper |
| 默认值兜底位置 | 实体（`= 0`）+ Wrapper（toDto 时 null→0）+ Service（convertToEntity / update 时 null→0） | 三重保险，与 vocab 模板一致 |
| App 端 VO 是否暴露 | 不暴露 | 终端用户无需看到 order，Service 已排好序，前端按顺序渲染 |

**改动分布**：

| 层 | 文件 | 改动 |
|--|--|--|
| SQL | `sql/character.sql` | 已完成（int(11) DEFAULT 0） |
| 实体 | `CharDiscrimination.java`、`CharWord.java` | 各新增 1 个字段 |
| DTO | `CharDiscriminationDto.java`、`CharWordDto.java` | 各新增 1 个字段 |
| Repo | `CharDiscriminationRepository.java`、`CharWordRepository.java` | **不改** |
| Request | `CharCharacterCreateRequest.java` 内嵌类 | 内嵌的 `CharDiscriminationRequest` 和 `CharWordRequest` 各新增 1 个字段 |
| 后台 VO | `CharCharacterVO.java` 内嵌类 | 内嵌的 `CharDiscriminationVO` 和 `CharWordVO` 各新增 1 个字段 |
| Wrapper | `CharCharacterWrapper.java` | 6 处改动（toDiscriminationDto / toWordDto / toDiscriminationVO / toWordVO）|
| Service | `CharCharacterServiceImpl.java` | 7 处改动（6 处兜底 + 1 处新排序 helper + 3 处调用 helper）|
| App 端 VO | `AppCharCharacterDetailVO.java` | **不改** |
| App 端 Controller | `AppCharCharacterController.java` | **不改**（透明受益于 Service 排序）|

## 详细设计

### 1. 实体层

**`grid-system/src/main/java/com/naon/grid/backend/domain/character/CharDiscrimination.java`** — 在 `comparisonTranslations` 之后、`createTime` 之前新增：

```java
@NotNull
@Column(name = "discrimination_order", nullable = false)
@ApiModelProperty(value = "辨析排序权重（值大的排前面）")
private Integer discriminationOrder = 0;
```

**`grid-system/src/main/java/com/naon/grid/backend/domain/character/CharWord.java`** — 在 `exampleImage` 之后、`createTime` 之前新增：

```java
@NotNull
@Column(name = "word_order", nullable = false)
@ApiModelProperty(value = "组词排序权重（值大的排前面）")
private Integer wordOrder = 0;
```

### 2. DTO 层

**`CharDiscriminationDto.java`** — 在 `updateTime` 之后、`status` 之前新增：

```java
@ApiModelProperty(value = "辨析排序权重（值大的排前面）")
private Integer discriminationOrder;
```

**`CharWordDto.java`** — 在 `updateTime` 之后、`status` 之前新增：

```java
@ApiModelProperty(value = "组词排序权重（值大的排前面）")
private Integer wordOrder;
```

DTO 字段不带默认值（与 vocab DTO 一致），允许"未提供"语义，兜底交给 Wrapper / Service 的 entity 转换。

### 3. Repo 层

`CharDiscriminationRepository` 和 `CharWordRepository` **不改动**。
排序由 Service 层处理，Repo 保持简单。

### 4. Request 层

**`CharCharacterCreateRequest.java`** 内的两个静态内嵌类：

`CharDiscriminationRequest`（在 `comparisonTranslations` 之后）新增：
```java
@ApiModelProperty(value = "辨析排序权重（值大的排前面，不传默认 0）")
private Integer discriminationOrder;
```

`CharWordRequest`（在 `exampleImage` 之后）新增：
```java
@ApiModelProperty(value = "组词排序权重（值大的排前面，不传默认 0）")
private Integer wordOrder;
```

不加 `@NotNull`：与 vocab Request 字段一致，允许前端不传（null）让后端兜底为 0。

### 5. 后台 VO 层

**`CharCharacterVO.java`** 内的两个静态内嵌类：

`CharDiscriminationVO`（在 `comparisonTranslations` 之后、`createTime` 之前）新增：
```java
@ApiModelProperty(value = "辨析排序权重（值大的排前面）")
private Integer discriminationOrder;
```

`CharWordVO`（在 `exampleImage` 之后、`createTime` 之前）新增：
```java
@ApiModelProperty(value = "组词排序权重（值大的排前面）")
private Integer wordOrder;
```

后台 VO **暴露** order，供管理后台编辑表单回显 / 拖拽排序使用。

### 6. App 端 VO 层

**`AppCharCharacterDetailVO.java` 不改动。**
终端用户无需看到 order 数值，前端按 Service 返回的顺序渲染即可。

### 7. Wrapper 层（CharCharacterWrapper）

6 处对位改动：

| 方法 | 位置 | 新增代码 |
|--|--|--|
| `toDiscriminationDto` | `setComparisonTranslations(...)` 后 | `dto.setDiscriminationOrder(request.getDiscriminationOrder() != null ? request.getDiscriminationOrder() : 0);` |
| `toWordDto` | `setExampleImage(...)` 后 | `dto.setWordOrder(request.getWordOrder() != null ? request.getWordOrder() : 0);` |
| `toDiscriminationVO` | `setComparisonTranslations(...)` 后 | `vo.setDiscriminationOrder(dto.getDiscriminationOrder());` |
| `toWordVO` | `setExampleImage(...)` 后 | `vo.setWordOrder(dto.getWordOrder());` |

`toDto`（请求→DTO）侧用 `!= null ? : 0` 兜底，与 `VocabWordWrapper.toSenseDto`/`toStructureDto`/`toExampleDto`/`toExerciseDto` 模板一致。
`toVO`（DTO→VO）侧直接透传，无需兜底（实体读出来的字段不会是 null）。

### 8. Service 层（CharCharacterServiceImpl）— 关键改动

#### 8.1 6 处字段兜底（写路径 + 读路径）

| 方法 | 性质 | 新增代码 |
|--|--|--|
| `convertToDiscriminationDto` | Entity → DTO（读路径） | `dto.setDiscriminationOrder(discrimination.getDiscriminationOrder());` |
| `convertToWordDto` | Entity → DTO（读路径） | `dto.setWordOrder(word.getWordOrder());` |
| `convertToDiscriminationEntity` | DTO → Entity（写路径，新增子项） | `entity.setDiscriminationOrder(dto.getDiscriminationOrder() != null ? dto.getDiscriminationOrder() : 0);` |
| `convertToWordEntity` | DTO → Entity（写路径，新增子项） | `entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);` |
| `updateDiscrimination` | 更新已存在的 entity（写路径） | `entity.setDiscriminationOrder(dto.getDiscriminationOrder() != null ? dto.getDiscriminationOrder() : 0);` |
| `updateWord` | 更新已存在的 entity（写路径） | `entity.setWordOrder(dto.getWordOrder() != null ? dto.getWordOrder() : 0);` |

新增代码位置均与同一方法内的"上一个翻译字段 / image 字段"对位，与 vocab `VocabWordServiceImpl` 的 6 处兜底完全同构。

#### 8.2 排序 helper（新增 2 个私有方法）

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
- **降序**（`reverseOrder`），值大的排前
- **null 容错**（`nullsLast`）：实体保证非空，但草稿 JSON 反序列化的历史数据可能是 null，防御一下
- **in-place 排序**：`convertToDiscriminationDtos` 返回新 `ArrayList`、fastjson 反序列化返回新 list，都安全可改

#### 8.3 排序调用点（覆盖所有读路径）

**`findById` 的"草稿/已审核"分支**（反序列化 `CharCharacterDto` 之后、return 之前）：
```java
dto.setDiscriminations(sortDiscriminationsDesc(dto.getDiscriminations()));
dto.setWords(sortWordsDesc(dto.getWords()));
```

**`findById` 的"已发布"分支**：
```java
charCharacterDto.setDiscriminations(sortDiscriminationsDesc(convertToDiscriminationDtos(
    charDiscriminationRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())
)));
charCharacterDto.setWords(sortWordsDesc(convertToWordDtos(
    charWordRepository.findByCharIdAndStatus(id, StatusEnum.ENABLED.getCode())
)));
```

**`findPublishedById`**：与 `findById` 已发布分支同样的包裹。

App 端 controller 不需要改动——透明受益于 Service 排序。

## 端到端数据流

**写路径（创建 / 更新）**：
```
前端 JSON (Integer discriminationOrder / wordOrder, 可能 null)
  → CharCharacterCreateRequest 内嵌类
  → CharCharacterWrapper.toDiscriminationDto/toWordDto       [null→0 兜底]
  → CharCharacterDto / CharDiscriminationDto / CharWordDto
  → CharCharacterServiceImpl.create/update                   [JSON.toJSONString → draftContent]
  → DB: char_character.draft_content
```

**发布路径**：
```
DB: char_character.draft_content (JSON)
  → JsonUtils.fromJson → CharCharacterDto
  → syncDiscriminations/syncWords
      ├─ 新增项 → convertToDiscriminationEntity/convertToWordEntity   [null→0 兜底]
      └─ 已有项 → updateDiscrimination/updateWord                     [null→0 兜底]
  → CharDiscriminationRepository.save / CharWordRepository.save
  → DB: char_discrimination.discrimination_order / char_word.word_order
```

**读路径（查询详情）**：
```
DB
  ├─ 已发布: char_discrimination / char_word 行
  │    → Service convertToDiscriminationDto/convertToWordDto          [Entity → DTO 透传 order]
  │    → sortDiscriminationsDesc / sortWordsDesc                       [降序]
  │    → CharCharacterDto.discriminations / words
  │
  └─ 草稿/已审核: char_character.draft_content
       → JsonUtils.fromJson → CharCharacterDto                          [order 已含在 JSON 里]
       → sortDiscriminationsDesc / sortWordsDesc                        [降序]

→ 后台: CharCharacterWrapper.toVO → CharCharacterVO                    [VO 暴露 order]
→ App端: AppCharCharacterController 直接转 AppCharCharacterDetailVO    [不暴露 order, 按顺序渲染]
```

## 兼容性 / 历史数据

- DDL 默认 `0`：历史行新增列后自动得到 `0`
- 反序列化历史草稿 JSON（不含 order 字段）：fastjson 反序列化为 `null` → 排序 helper `nullsLast` 兜底 → 排在最后
- 历史 `findById` 的所有调用方（后台 VO、App 端 controller、未来的批量接口）：自动得到有序 DTO，无需改动

## 不在本次范围

- App 端 VO 暴露 order（不需要）
- Repo 层 OrderBy 方法（用 Service 排序代替）
- 拖拽排序的 UI / 接口（前端职责，本次只提供字段）
- 给 vocab 模块加任何东西（vocab 已有完整实现）
