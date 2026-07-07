# App 端练习题接口设计

**日期**: 2026-07-07
**状态**: 已批准

## 概述

为 grid-app 模块新增两个面向终端用户的练习题查询接口：
1. 根据 ID 查询单个已发布题目详情
2. 根据 ID 列表批量查询题目详情

仅返回 `publish_status = 'published'` 且 `status = 1`（未删除）的题目。

## 接口设计

### 1. 题目详情

- **方法**: `GET`
- **路径**: `/api/app/exercise-question/{id}`
- **参数**: `id` (Long, 路径变量)
- **返回**: `AppExerciseQuestionDetailVO`

### 2. 批量查询

- **方法**: `POST`
- **路径**: `/api/app/exercise-question/batch`
- **参数**: `AppExerciseQuestionBatchRequest` (body)，包含 `List<Long> ids`
- **返回**: `List<AppExerciseQuestionDetailVO>`

## 架构

```
grid-app/.../AppExerciseQuestionController
    ↓ 调用
grid-system/.../ExerciseQuestionService (新增方法)
    ↓ 查询
ExerciseQuestionRepository → exercise_question 表
```

Controller 注入：
- `ExerciseQuestionService` — 查询题目
- `AudioResourceService` — 批量查询音频资源
- `AliOssStorageService` — 批量查询图片资源

## 新增文件清单

### grid-system 模块

| 文件 | 说明 |
|---|---|
| `ExerciseQuestionService.java` | 新增 `findPublishedById`、`findPublishedByIds` 方法声明 |
| `ExerciseQuestionServiceImpl.java` | 新增两个方法的实现，只查已发布且未删除的题目 |

### grid-app 模块

| 文件 | 说明 |
|---|---|
| `AppExerciseQuestionController.java` | App 端题目查询控制器 |
| `AppExerciseQuestionDetailVO.java` | 题目详情 VO（含 AudioVO、ImageVO 内部类） |
| `AppExerciseQuestionBatchRequest.java` | 批量查询请求体 |
| `AppExerciseQuestionWrapper.java` | DTO → VO 映射包装器 |

## VO 字段设计

`AppExerciseQuestionDetailVO` 字段与后台 `ExerciseQuestionVO` 对比：

| 后台 VO 字段 | App VO | 变更说明 |
|---|---|---|
| `id` | ✅ 保留 | |
| `questionType` | ✅ 保留 | |
| `stem` | ✅ 保留 | |
| `content` | ✅ 保留 | 内部 `contentImageId`(String) → `image`(ImageVO) |
| `options` | ✅ 保留 | 内部 `optionImageId`(String) → `image`(ImageVO) |
| `answer` | ✅ 保留 | |
| `explanation` | ✅ 保留 | |
| `audioId`(Long) | → `audio`(AudioVO) | 解析为 `audioUrl` |
| `audioText` | ✅ 保留 | |
| `sort` | ✅ 保留 | |
| `children` | ✅ 保留 | 递归同结构 |
| `publishStatus` | ❌ 移除 | 管理字段 |
| `editStatus` | ❌ 移除 | 管理字段 |
| `createBy` | ❌ 移除 | 管理字段 |
| `updateBy` | ❌ 移除 | 管理字段 |
| `createTime` | ❌ 移除 | 管理字段 |
| `updateTime` | ❌ 移除 | 管理字段 |

## 资源解析流程

Controller 中每个请求的处理流程：

1. 调用 `exerciseQuestionService.findPublishedById(id)` 或 `findPublishedByIds(ids)` 获取 DTO 列表
2. 遍历 DTO（含 `children`），收集：
   - **音频 ID**：`dto.audioId` + 所有子题的 `audioId`
   - **图片 ID**：`content.contentImageId` 解析为 `Long` + 每个 `option.optionImageId` 解析为 `Long` + 所有子题同样逻辑
3. 批量查询：`audioResourceService.findByIds(audioIds)` → `Map<Long, AudioResourceDto>`
4. 批量查询：`aliOssStorageService.findByIds(imageIds)` → `Map<Long, AliOssStorageDto>`
5. 调用 `AppExerciseQuestionWrapper.toDetailVO(dto, audioMap, imageMap)` 转换

## Wrapper 映射规则

`AppExerciseQuestionWrapper.toDetailVO()`:
- 纯静态方法，只做字段映射
- 从 `audioMap` 查找 `audioId`：找到则设置 `AudioVO(audioUrl=fileUrl)`，找不到则 `log.error("题目音频资源未找到, audioId={}", id)`
- 从 `imageMap` 查找图片 ID：找到则设置 `ImageVO(imageUrl=fileUrl)`，找不到则 `log.error("题目图片资源未找到, imageId={}", id)`
- `children` 递归调用 `toDetailVO` 处理
- `contentImageId` 和 `optionImageId` 为 String 类型，需解析为 Long 后再查 imageMap

## Service 层新方法

### findPublishedById

```java
ExerciseQuestionDto findPublishedById(Long id);
```

- 查询条件：`id = ? AND status = 1 AND publish_status = 'published'`
- 不满足条件抛出 `EntityNotFoundException`
- 不走草稿覆盖，直接返回主表字段 + 子题列表

### findPublishedByIds

```java
List<ExerciseQuestionDto> findPublishedByIds(List<Long> ids);
```

- 查询条件：`id IN (?) AND status = 1 AND publish_status = 'published'`
- 空列表或 null 返回空列表
- 不去重，顺序与输入无关（调用方可自行排序）
- 返回的每个 DTO 含子题列表

## 错误处理

- 资源（音频/图片）找不到：**不报错**，仅 `log.error` 记录日志，对应资源字段返回 null
- 题目未发布/已删除：抛出 `EntityNotFoundException`
- 图片 ID 字符串无法解析为 Long：`log.error` 并跳过该图片

## 测试要点

- 已发布题目可正常查询，含音频/图片资源 URL
- 未发布/已删除题目返回 404
- 音频/图片资源缺失时不报错，资源字段为 null
- 有子题的父题返回完整 children 列表
- 批量查询空 ID 列表返回空列表
- 批量查询部分 ID 不存在时只返回存在的题目
