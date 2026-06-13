# 汉字部首后台管理功能设计

## 概述

为 Little Grid 系统新增汉字部首（Radical）后台管理功能。部首基础数据通过 SQL 脚本直接导入数据库，管理后台提供更新、审核、发布、分页查询、软删除和下线的完整生命周期管理。

## 数据库设计

### 部首表 `char_radical`

```sql
CREATE TABLE `char_radical` (
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

- `id` 使用 BIGINT（对应 Java `Long`），与现有资源类表一致
- `evolution_desc_translations` 使用 TEXT JSON 列，与现有 `TextTranslation` 多语言模式一致
- 审计字段（`create_by`/`update_by`/`create_time`/`update_time`）由 Java DO 的 `BaseEntity` 自动管理

## 架构

### 模块归属

所有代码位于 **grid-system** 模块，包名 `charradical`（与 `character`、`vocabcomparison` 同级）。

### 包结构

```
grid-system/src/main/java/com/naon/grid/backend/
├── domain/charradical/
│   └── CharRadical.java
├── repo/charradical/
│   └── CharRadicalRepository.java
├── service/charradical/
│   ├── CharRadicalService.java
│   ├── dto/
│   │   ├── CharRadicalDto.java
│   │   └── CharRadicalQueryCriteria.java
│   └── impl/
│       └── CharRadicalServiceImpl.java
├── rest/
│   ├── controller/
│   │   └── CharRadicalController.java
│   ├── request/
│   │   ├── CharRadicalUpdateRequest.java
│   │   └── CharRadicalQueryRequest.java
│   ├── vo/
│   │   ├── CharRadicalVO.java
│   │   └── CharRadicalBaseVO.java
│   └── wrapper/
│       └── CharRadicalWrapper.java
```

### 文件清单（14 个 Java 文件）

## 组件设计

### 1. DO — `CharRadical`

- 继承 `BaseEntity`（自动获得 `createBy`/`updateBy`/`createTime`/`updateTime`）
- `@Entity` + `@Table(name = "char_radical")`
- 主键 `Long id`，`@GeneratedValue(strategy = GenerationType.IDENTITY)`
- 字段：`radical`, `strokeNum`, `evolutionDesc`, `evolutionDescTranslations`（String JSON 存储）, `evolutionImageId`, `draftContent`, `status`, `publishStatus`, `editStatus`

### 2. Repository — `CharRadicalRepository`

- 继承 `JpaRepository<CharRadical, Long>` + `JpaSpecificationExecutor<CharRadical>`
- 标准声明，无需自定义查询方法

### 3. DTO — `CharRadicalDto`

- 继承 `BaseDTO`（自动获得审计字段 getter/setter）
- 字段与 DO 业务字段对应，但 `evolutionDescTranslations` 使用 `List<TextTranslation>`（反序列化后的对象格式）
- 包含 `draftContent`（用于传递草稿 JSON）
- 包含 `status`/`publishStatus`/`editStatus`

### 4. QueryCriteria — `CharRadicalQueryCriteria`

- 使用 `@Query` 注解构建 Specification
- `blurry` 字段：`@Query(blurry = "radical")` — 支持部首名称模糊搜索
- `publishStatus`：`@Query` — 精确匹配发布状态
- `editStatus`：`@Query` — 精确匹配编辑状态

### 5. Service 接口 — `CharRadicalService`

| 方法 | 说明 |
|------|------|
| `queryAll(criteria, pageable)` | 分页查询，支持部首名模糊搜索 + 状态筛选 |
| `findById(id)` | 按 ID 查询详情，草稿态从 draftContent 反序列化 |
| `update(id, dto)` | 更新部首内容（写入草稿 JSON + 回退编辑状态） |
| `delete(id)` | 软删除（status = 0） |
| `reviewDraft(id)` | 审核通过：draft → reviewed |
| `publishDraft(id)` | 发布：reviewed → published，回写主表字段 |
| `offline(id)` | 下线：published → unpublished |

**无 create 方法** — 部首数据通过 SQL 预导入。

### 6. Service 实现 — `CharRadicalServiceImpl`

核心逻辑与 `VocabComparisonGroupServiceImpl` 对齐，但由于无子表更简化：

**queryAll 流程：**
1. `QueryHelp` 根据 `@Query` 注解构建 Specification
2. `groupRepository.findAll(spec, pageable)` 分页查询
3. 每个 entity 通过 `toDtoWithDraftOverlay()` 处理：草稿态从 draftContent 覆盖字段值
4. 返回 `PageResult<CharRadicalDto>`

**findById 流程：**
1. 查询 entity，校验 status=1
2. 草稿/已审核态：从 `draftContent` JSON 反序列化为 DTO，叠加审计字段
3. 已发布态：直接 `toBaseDto(entity)` 转换

**update 流程：**
1. 查询 entity，校验 status=1
2. 如已是 reviewed/published 状态，回退为 draft
3. 将请求 DTO 序列化为 `draftContent` JSON 写入
4. 保存 entity

**publishDraft 流程：**
1. 校验：entity 存在、status=1、draftContent 不为空、editStatus=reviewed
2. 解析 draftContent JSON → DTO
3. 回写主表字段：`radical`、`strokeNum`、`evolutionDesc`、`evolutionDescTranslations`、`evolutionImageId`
4. 清空 `draftContent`
5. 设置 `publishStatus = published`、`editStatus = published`
6. 保存 entity

### 7. 请求类

**`CharRadicalUpdateRequest`：**
- `radical` (String) — 部首名称
- `strokeNum` (Integer) — 笔画数
- `evolutionDesc` (String) — 演化解说
- `evolutionDescTranslations` (List\<TextTranslationRequest\>) — 外文翻译列表
- `evolutionImageId` (String) — 图片 ID

**`CharRadicalQueryRequest`：**
- `blurry` (String) — 部首名称模糊搜索
- `publishStatus` (String) — 发布状态筛选
- `editStatus` (String) — 编辑状态筛选

### 8. VO 类

**`CharRadicalBaseVO`**（分页列表用）：
- `id`, `radical`, `strokeNum`, `evolutionImageId`
- `publishStatus`, `editStatus`
- `createBy`, `updateBy`, `createTime`, `updateTime`

**`CharRadicalVO`**（详情用）：
- `id`, `radical`, `strokeNum`
- `evolutionDesc`, `evolutionDescTranslations`(List\<TextTranslationVO\>), `evolutionImageId`
- `publishStatus`, `editStatus`
- `createBy`, `updateBy`, `createTime`, `updateTime`

### 9. Wrapper — `CharRadicalWrapper`

转换方法清单：

| 方法 | 方向 | 说明 |
|------|------|------|
| `toCriteria(request)` | QueryRequest → QueryCriteria | 提取 publishStatus/editStatus |
| `toDto(updateRequest)` | UpdateRequest → Dto | 字段映射 |
| `toVO(dto)` | Dto → DetailVO | 含翻译对象转换 |
| `toBaseVO(dto)` | Dto → BaseVO | 用于列表 |
| `toBaseVOList(dtos)` | List\<Dto\> → List\<BaseVO\> | 批量转换 |

翻译工具方法复用现有 `TextTranslationRequest` ↔ `TextTranslation` ↔ `TextTranslationVO` 模式。

### 10. Controller — `CharRadicalController`

| HTTP | 路径 | 方法 | 说明 |
|------|------|------|------|
| PUT | `/api/char/radical/{id}` | `update()` | 修改部首 |
| PUT | `/api/char/radical/{id}/review` | `reviewDraft()` | 草稿→已审核 |
| PUT | `/api/char/radical/{id}/publish` | `publishDraft()` | 已审核→已发布 |
| GET | `/api/char/radical/{id}` | `findById()` | 查询详情 |
| GET | `/api/char/radical` | `queryAll()` | 分页查询列表 |
| DELETE | `/api/char/radical/{id}` | `delete()` | 软删除 |
| PUT | `/api/char/radical/{id}/offline` | `offline()` | 下线 |

- 路由前缀 `/api/char/radical`
- 标注 `@Log` 用于操作日志记录
- 使用 `@AnonymousXxxMapping`（与项目现有风格一致）
- 出参统一为 `ResponseEntity<XXXVO>` 或 `ResponseEntity<PageResult<XXXBaseVO>>`
- **无新增（POST）接口**

## SQL 迁移脚本

```sql
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

## 不做的事项

- 普通用户侧（App API）的部首接口：本设计仅覆盖后台管理
- 部首与汉字的关联查询：`char_character` 表已有 `radical_id` 字段，但关联展示不在本设计范围内
- 部首的新增接口：数据通过 SQL 预导入
- 批量操作：所有接口均为单条操作
