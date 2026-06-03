# 词汇管理工作流重构设计文档

**日期**: 2026-06-02
**作者**: Claude Code
**版本**: v1.0

## 1. 概述

本次重构旨在统一词汇管理的工作流程，移除重复的接口定义，明确状态流转逻辑，确保实现与需求一致。完全参照汉字管理的重构方案进行。

## 2. 现状与问题

### 2.1 当前问题

1. **接口重复**: 同时存在常规 CRUD 接口和草稿专用接口，职责不清
2. **状态逻辑不符**:
   - `findById` 未根据 `editStatus` 优先返回 `draftContent`
   - `publishDraft` 未将 `editStatus` 设为 `published`
   - `offline` 逻辑与需求不符（当前会删除子表）
   - `delete` 逻辑与需求不符（当前会删除子表）

### 2.2 当前接口列表

| 方法 | 路径 | 状态 |
|------|------|------|
| GET | `/api/vocabulary/{id}` | 保留重构 |
| GET | `/api/vocabulary` | 保留重构 |
| POST | `/api/vocabulary` | 保留重构 |
| PUT | `/api/vocabulary/{id}` | 保留重构 |
| DELETE | `/api/vocabulary/{id}` | 保留重构 |
| GET | `/api/vocabulary/{id}/draft` | 移除 |
| POST | `/api/vocabulary/draft` | 移除 |
| PUT | `/api/vocabulary/{id}/draft` | 移除 |
| POST | `/api/vocabulary/{id}/draft/from-published` | 移除 |
| PUT | `/api/vocabulary/{id}/review` | 保留重构 |
| PUT | `/api/vocabulary/{id}/publish` | 保留重构 |
| PUT | `/api/vocabulary/{id}/offline` | 保留重构 |

## 3. 需求确认

### 3.1 核心状态

| 字段 | 类型 | 值 | 说明 |
|------|------|-----|------|
| `status` | Integer | 0 | 禁用(删除) |
| `status` | Integer | 1 | 启用 |
| `publishStatus` | String | unpublished | 未发布 |
| `publishStatus` | String | published | 已发布 |
| `editStatus` | String | draft | 草稿 |
| `editStatus` | String | reviewed | 已审核 |
| `editStatus` | String | published | 已发布 |

### 3.2 完整工作流程

1. **创建**: 保存 `word` 到主表（用于列表显示），完整数据保存到 `draftContent`，`status=1`，`publishStatus=unpublished`，`editStatus=draft`
2. **更新**: 保存到 `draftContent`，如果当前 `editStatus=reviewed/published`，则变回 `draft`
3. **审核**: `editStatus` 从 `draft` 变为 `reviewed`
4. **发布**: `draftContent` 同步到主表（**不更新 word 字段**）和子表，清空 `draftContent`，`publishStatus=published`，`editStatus=published`
5. **下线**: `publishStatus=unpublished`（后台仍可见，用户不可见，**不改动子表**）
6. **删除**: `status=0`（所有人都不可见，**不改动子表**）
7. **查看详情(后台)**: `editStatus=draft/reviewed` 时返回 `draftContent`；`editStatus=published` 时返回主表+子表

## 4. 设计方案

### 4.1 最终接口列表

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/vocabulary/{id}` | 后台查看详情（根据 editStatus 返回内容） |
| GET | `/api/vocabulary` | 后台分页列表（过滤 status=0） |
| POST | `/api/vocabulary` | 创建词汇（保存 word 到主表，其余到 draftContent） |
| PUT | `/api/vocabulary/{id}` | 更新词汇（仅更新 draftContent，状态处理） |
| DELETE | `/api/vocabulary/{id}` | 删除词汇（软删 status=0，不改动子表） |
| PUT | `/api/vocabulary/{id}/review` | 审核通过（editStatus: draft → reviewed） |
| PUT | `/api/vocabulary/{id}/publish` | 发布（同步 draftContent，不更新 word，更新状态） |
| PUT | `/api/vocabulary/{id}/offline` | 下线（publishStatus: published → unpublished，不改动子表） |

### 4.2 状态流转矩阵

| 操作 | 当前状态 | 新状态 | 说明 |
|------|----------|--------|------|
| 创建 | - | publishStatus=unpublished, editStatus=draft, status=1 | word 保存到主表，其余在 draftContent |
| 更新 | draft | draft | 更新 draftContent |
| 更新 | reviewed | draft | 更新 draftContent，状态回退 |
| 更新 | published | draft | 更新 draftContent，状态回退 |
| 审核 | draft | reviewed | draftContent 不变 |
| 发布 | reviewed | publishStatus=published, editStatus=published | draftContent 同步到主表（不更新 word）+子表，然后清空 draftContent |
| 下线 | published | publishStatus=unpublished | 主表+子表数据不变 |
| 删除 | 任意 | status=0 | 软删除，不改动子表 |

### 4.3 数据访问逻辑

**后台查看详情 (`findById`)**:
- 如果 `editStatus` = draft 或 reviewed → 解析并返回 `draftContent`
- 如果 `editStatus` = published → 返回主表 + 子表数据

**后台列表 (`queryAll`)**:
- 过滤 `status` = 1（不显示已删除的）
- 不过滤 publishStatus（显示所有未删除的）

**用户端查看**:
- 过滤 `status` = 1 AND `publishStatus` = published

## 5. 详细实现设计

### 5.1 Service 层变更

#### VocabWordService 接口
移除以下方法：
- `getDraft(Integer id)`
- `createDraft(VocabWordDraftDto draft)`
- `saveDraft(Integer id, VocabWordDraftDto draft)`
- `createDraftFromPublished(Integer id)`

保留并重构以下方法：
- `create(VocabWordDto resources)` - 保存 word 到主表，其余到 draftContent
- `update(Integer id, VocabWordDto resources)` - 仅更新 draftContent，处理状态回退
- `findById(Integer id)` - 根据 editStatus 返回对应内容（统一返回 VocabWordDto）
- `publishDraft(Integer id)` - 同步 draftContent（不更新 word），设置 editStatus=published
- `offline(Integer id)` - 仅设置 publishStatus=unpublished，不改动子表
- `delete(Integer id)` - 仅设置 status=0，不改动子表

**注意**：统一使用 `VocabWordDto`，不再使用 `VocabWordDraftDto`，`draftContent` 存储的就是 `VocabWordDto` 的 JSON

#### VocabWordServiceImpl 实现

**create 方法**:
1. 创建 VocabWord 实体
2. 设置 status=ENABLED, publishStatus=UNPUBLISHED, editStatus=DRAFT
3. 仅保存 `word` 字段到主表（用于列表显示）
4. 将入参 DTO 转为 JSON 存入 draftContent
5. 主表其他字段保持 null
6. 保存实体并返回 ID

**update 方法**:
1. 根据 ID 查询实体，检查 status != DISABLED
2. 如果当前 editStatus 是 REVIEWED 或 PUBLISHED，设置为 DRAFT
3. 将入参 DTO 转为 JSON 存入 draftContent
4. 保存实体（不改动主表其他字段，不改动子表）

**findById 方法**:
1. 根据 ID 查询实体，检查 status != DISABLED
2. 如果 editStatus == DRAFT 或 editStatus == REVIEWED:
   - 解析 draftContent 为 VocabWordDto
   - 设置 dto 的 id、status、publishStatus、editStatus、createTime、updateTime、createBy、updateBy 为实体当前值
   - 返回该 dto
3. 如果 editStatus == PUBLISHED:
   - 返回主表 + 子表数据（原有逻辑）

**publishDraft 方法**:
1. 根据 ID 查询实体，检查 status != DISABLED
2. 检查 draftContent != null
3. 检查 editStatus == REVIEWED
4. 解析 draftContent 为 VocabWordDto
5. 将 DTO 数据同步到主表字段（**不更新 word 字段**）
6. 调用 syncSenses 和 syncExercises 同步子表
7. 设置 publishStatus=PUBLISHED, editStatus=PUBLISHED
8. 清空 draftContent
9. 保存实体

**offline 方法**:
1. 根据 ID 查询实体，检查 status != DISABLED
2. 仅设置 publishStatus=UNPUBLISHED
3. 保存实体（**不改动子表**）

**delete 方法**:
1. 根据 ID 查询实体
2. 仅设置 status=DISABLED
3. 保存实体（**不改动子表，不改动 publishStatus**）

### 5.2 Controller 层变更

移除以下接口：
- `GET /api/vocabulary/{id}/draft`
- `POST /api/vocabulary/draft`
- `PUT /api/vocabulary/{id}/draft`
- `POST /api/vocabulary/{id}/draft/from-published`

重构以下接口：
- `POST /api/vocabulary` - 调用重构后的 create
- `PUT /api/vocabulary/{id}` - 调用重构后的 update
- `GET /api/vocabulary/{id}` - 调用重构后的 findById（统一返回 VocabWordDto，统一转为 VO）
- `PUT /api/vocabulary/{id}/publish` - 调用重构后的 publishDraft
- `PUT /api/vocabulary/{id}/offline` - 调用重构后的 offline
- `DELETE /api/vocabulary/{id}` - 调用重构后的 delete

### 5.3 子表处理说明

词汇包含以下子表，在发布时同步，下线和删除时不改动：
- `VocabSense` (义项)
- `VocabStructure` (搭配，属于义项)
- `VocabExample` (例句，属于搭配)
- `VocabExercise` (练习题)

发布时调用原有的 `syncSenses` 和 `syncExercises` 方法即可，这些方法内部已经处理了层级关系。

## 6. 兼容性考虑

- AppVocabWordController 无需改动，继续使用 findPublishedById 方法
- 已有的数据结构保持不变
- 数据库表结构无需变更
- VocabWordDraftDto 可以考虑后续清理（本次重构先保留以避免影响其他可能的引用）

## 7. 测试要点

1. 创建词汇，验证主表仅 word 有值，draftContent 有完整数据，状态正确
2. 更新不同状态下的词汇，验证状态回退逻辑
3. 审核草稿，验证状态变更
4. 发布已审核的草稿，验证数据同步到主表（word 不变）和子表，draftContent 清空，状态正确
5. 下线已发布的词汇，验证 publishStatus 变更，子表不变，后台仍可见
6. 删除词汇，验证 status=0，子表不变，所有人不可见
7. 查看不同状态下的详情，验证返回内容正确
