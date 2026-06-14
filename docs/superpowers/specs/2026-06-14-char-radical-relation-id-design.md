# CharRadical 新增 relation_id 字段设计

## 背景

部首表中需要新增一个关联字段，允许一个部首关联到另一个部首（如变体、异体等关系）。

## 字段定义

| 属性 | 值 |
|------|-----|
| 字段名 | `relation_id` |
| 类型 | `bigint(20) DEFAULT NULL` |
| 备注 | 关联部首ID（可空，null 表示无关联） |
| Java 类型 | `Long`（包装类型，所有层统一可空） |

## 变更范围

### 涉及文件

| 文件 | 变更内容 |
|------|----------|
| `sql/biz_character.sql` | `NOT NULL` → `DEFAULT NULL` |
| `CharRadical.java` (Entity) | 新增 `Long relationId` 字段 + `@Column` + `@ApiModelProperty` |
| `CharRadicalDto.java` | 新增 `Long relationId` 字段 |
| `CharRadicalUpdateRequest.java` | 新增 `Long relationId` 字段（更新接口入参） |
| `CharRadicalVO.java` | 新增 `Long relationId`（详情接口出参） |
| `CharRadicalBaseVO.java` | 新增 `Long relationId`（列表接口出参） |
| `CharRadicalWrapper.java` | `toDto()`、`toVO()`、`toBaseVO()` 三处映射加上该字段 |
| `CharRadicalServiceImpl.java` | `toBaseDto()`、`applyDraftOverlay()`、`publishDraft()` 三处加上该字段处理 |

### 数据流

```
UpdateRequest.relationId → Wrapper.toDto → Dto.relationId
    → Service.update() → 存入 draftContent JSON
    → Service.publishDraft() → 回写 Entity.relationId

Entity.relationId → Service.toBaseDto() → Dto.relationId
    → 草稿态: applyDraftOverlay() 用草稿值覆盖
    → Wrapper.toVO/toBaseVO → VO/BaseVO.relationId
```

### 字段在各层的可空性

- SQL: `DEFAULT NULL`
- Entity: `Long`（null = 无关联）
- DTO: `Long`（null = 无关联）
- UpdateRequest: `Long`（不传则无关联）
- VO/BaseVO: `Long`（可能为 null）

## 实施计划

1. 修改 SQL 文件
2. 修改 Entity
3. 修改 DTO
4. 修改 UpdateRequest
5. 修改 VO 和 BaseVO
6. 修改 Wrapper（映射层）
7. 修改 ServiceImpl（业务逻辑层）
