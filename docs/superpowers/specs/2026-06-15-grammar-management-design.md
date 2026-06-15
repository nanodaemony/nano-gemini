# Grammar Management Backend Design

> **Date:** 2026-06-15
> **Project:** Little Grid (grid) - 汉语教学服务端
> **Module:** grid-system

## 1. Overview

为汉语教学系统新增**语法管理后台功能**，实现对语法点的完整 CRUD 操作及草稿→审核→发布的发布流程。设计完全对齐已存在的汉字管理模块（`CharCharacterController`）。

## 2. Database Tables

5 张新表（参考 `sql/biz_grammer.sql`）：

| 表名 | 类型 | 说明 |
|------|------|------|
| `grammar_point` | 主表 | 语法点，含草稿工作流（`draft_content`, `edit_status`, `publish_status`） |
| `grammar_meaning` | 子表 | 语法意义，关联 `grammar_point.id`，含例句 ID 列表 JSON |
| `grammar_structure` | 子表 | 语法结构，关联 `grammar_point.id`，含例句 ID 列表 JSON |
| `grammar_notice` | 子表 | 语法注意，关联 `grammar_point.id`，含例句 ID 列表 JSON |
| `grammar_error` | 子表 | 语法偏误，关联 `grammar_point.id`，无例句关联 |

### 主表字段

- `id` BIGINT PK AUTO_INCREMENT
- `name` VARCHAR(50) NOT NULL - 语法点名称
- `hsk_level` VARCHAR(20) - HSK等级
- `project` VARCHAR(20) - 项目
- `category` VARCHAR(50) - 类别
- `sub_category` VARCHAR(50) - 细目
- `draft_content` TEXT - 草稿内容 JSON
- `edit_status` VARCHAR(20) DEFAULT 'draft' - 编辑状态：draft / reviewed / published
- `publish_status` VARCHAR(20) DEFAULT 'unpublished' - 发布状态：unpublished / published
- 审计字段（继承 BaseEntity）：create_by, update_by, create_time, update_time
- `status` TINYINT DEFAULT 1 - 有效状态：1=可用，0=已删除

### 子表公共字段

- `id` BIGINT PK AUTO_INCREMENT
- `grammar_id` BIGINT NOT NULL - 关联主表
- `*_content` 等业务字段
- `*_translations` TEXT - 外文翻译 JSON
- `\`order\`` INT DEFAULT 0 - 排序权重（值大的排前面）
- `status` TINYINT DEFAULT 1
- `create_time`, `update_time`

子表特有的例句字段：
- `grammar_meaning.meaning_sentence_ids` VARCHAR(128) - 例句ID列表 JSON
- `grammar_structure.structure_sentence_ids` VARCHAR(128) - 例句ID列表 JSON
- `grammar_notice.notice_sentence_ids` VARCHAR(128) - 例句ID列表 JSON

> 注意：原始 SQL 中 `grammar_point` 表定义末尾有多余逗号，实现时需修正。

## 3. Draft Workflow

与汉字管理完全一致：

```
[DRAFT] → review → [REVIEWED] → publish → [PUBLISHED]
    ↑              ← update 重置为 DRAFT       |
    |                                            ↓
    └──────── offline (PUBLISHED → UNPUBLISHED) ←─┘
```

- **创建**: 写入主表，`edit_status=draft`, `publish_status=unpublished`，所有数据存入 `draft_content` JSON
- **更新**: 更新 `draft_content`，如果当前状态是 `reviewed` 或 `published` 则重置为 `draft`
- **审核**: `draft` → `reviewed`，仅校验草稿存在
- **发布**: `reviewed` → `published`，将 `draft_content` 数据写入主表字段和各子表真实行
- **下线**: 仅将 `publish_status` 设为 `unpublished`，不修改子表
- **删除**: 软删除（`status=0`）
- **分页查询**: 草稿/已审核状态的数据，列表字段从 `draft_content` 中读取；已发布状态的数据从 DB 字段读取（与汉字逻辑一致）

## 4. Package Structure

所有文件位于 `grid-system` 模块：

```
com.naon.grid.backend
├── domain/grammar/
│   ├── GrammarPoint.java           (extends BaseEntity)
│   ├── GrammarMeaning.java
│   ├── GrammarStructure.java
│   ├── GrammarNotice.java
│   └── GrammarError.java
├── repo/grammar/
│   ├── GrammarPointRepository.java      (extends JpaRepository + JpaSpecificationExecutor)
│   ├── GrammarMeaningRepository.java
│   ├── GrammarStructureRepository.java
│   ├── GrammarNoticeRepository.java
│   └── GrammarErrorRepository.java
├── service/grammar/
│   ├── GrammarPointService.java
│   ├── dto/
│   │   ├── GrammarPointDto.java
│   │   ├── GrammarPointQueryCriteria.java
│   │   ├── GrammarMeaningDto.java
│   │   ├── GrammarStructureDto.java
│   │   ├── GrammarNoticeDto.java
│   │   └── GrammarErrorDto.java
│   ├── mapstruct/
│   │   └── GrammarPointMapper.java
│   └── impl/
│       └── GrammarPointServiceImpl.java
├── rest/
│   ├── controller/
│   │   └── GrammarPointController.java
│   ├── request/
│   │   ├── GrammarPointCreateRequest.java
│   │   └── GrammarPointQueryRequest.java
│   ├── vo/
│   │   ├── GrammarPointVO.java
│   │   └── GrammarPointBaseVO.java
│   └── wrapper/
│       └── GrammarPointWrapper.java
```

## 5. Key Classes Design

### 5.1 主键类型

所有语法相关表（主表和子表）使用 **Long** 主键，匹配 SQL 中 `BIGINT` 定义。与 `char_character`（Integer）不同，因为语法数据量预期更大。

### 5.2 DO (Domain Objects)

**GrammarPoint** 继承 `BaseEntity`，包含 `id(name=id)`, `name`, `hskLevel`, `project`, `category`, `subCategory`, `draftContent(columnDefinition=text)`, `status`, `publishStatus`, `editStatus`。

各子表 DO 不继承 `BaseEntity`，自行管理 `create_time/update_time`（使用 `@CreationTimestamp/@UpdateTimestamp`），与已有子表（`CharComparison`, `CharWord`）模式一致。

`order` 字段使用 `` @Column(name = "`order`") `` 映射，与 `CharComparison.comparisonOrder` 一样用反引号转义 SQL 保留字。

### 5.3 DTO (Data Transfer Objects)

**GrammarPointDto** 继承 `BaseDTO`，包含：
- 主表业务字段：`id`, `name`, `hskLevel`, `project`, `category`, `subCategory`
- 子表列表：`meanings(List<GrammarMeaningDto>)`, `structures(...)`, `notices(...)`, `errors(...)`
- 状态字段：`status`, `publishStatus`, `editStatus`, `draftContent`
- 列表统计字段：`meaningCount`, `structureCount`, `noticeCount`, `errorCount`

各子表 DTO 包含：
- 业务字段（如 `meaningContent`, `meaningContentTranslations` 等）
- 例句列表：`sentences(List<ExampleSentenceDto>)` — 不单独存 ID 列表，ID 在发布时自动提取
- `order` 排序字段
- 审计和时间字段

### 5.4 Request

**GrammarPointCreateRequest**：
- `name`(@NotBlank), `hskLevel`, `project`, `category`, `subCategory`
- `meanings`, `structures`, `notices`, `errors` — 各为嵌套内部类列表
- 嵌套类中例句字段：`List<@Valid ExampleSentenceRequest> sentences`（多条例句）

**GrammarPointQueryRequest**：
- `blurry`(语法点名称模糊搜索), `publishStatus`, `editStatus`, `hskLevel`, `category`

### 5.5 VO

**GrammarPointBaseVO**（列表页）：
- 主表字段 + `publishStatus`, `editStatus`, `createBy`, `updateBy`, `createTime`, `updateTime`
- 统计字段：`meaningCount`, `structureCount`, `noticeCount`, `errorCount`

**GrammarPointVO**（详情页）：
- 主表字段 + 状态/审计字段
- 子表 VO 列表（`GrammarMeaningVO`, `GrammarStructureVO`, `GrammarNoticeVO`, `GrammarErrorVO`）
- 子表 VO 中例句字段：`sentences(List<ExampleSentenceVO>)`

### 5.6 Controller

`GrammarPointController`，`@Api(tags = "后台：语法-语法点管理")`，`@RequestMapping("/api/grammar")`

| Method | Path | 功能 | Request Body | Response |
|--------|------|------|-------------|----------|
| POST | `/api/grammar` | 新增语法点 | `GrammarPointCreateRequest` | `{id: Long}` (201) |
| PUT | `/api/grammar/{id}` | 修改语法点 | `GrammarPointCreateRequest` | 204 |
| PUT | `/api/grammar/{id}/review` | 审核通过 | - | 204 |
| PUT | `/api/grammar/{id}/publish` | 发布上线 | - | 204 |
| PUT | `/api/grammar/{id}/offline` | 下线 | - | 204 |
| GET | `/api/grammar/{id}` | 查询详情 | - | `GrammarPointVO` |
| GET | `/api/grammar` | 分页列表 | `GrammarPointQueryRequest` + `Pageable` | `PageResult<GrammarPointBaseVO>` |
| DELETE | `/api/grammar/{id}` | 删除 | - | 204 |

所有端点使用 `@Anonymous*Mapping` 注解（匹配现有模式）。

### 5.7 Service

**GrammarPointService** 接口：

```java
PageResult<GrammarPointDto> queryAll(GrammarPointQueryCriteria criteria, Pageable pageable);
GrammarPointDto findById(Long id);
Long create(GrammarPointDto resources);
void update(Long id, GrammarPointDto resources);
void delete(Long id);
void reviewDraft(Long id);
void publishDraft(Long id);
void offline(Long id);
```

**GrammarPointServiceImpl** 关键逻辑：

1. **分页查询的草稿覆盖**：与 `CharCharacterServiceImpl.toDtoWithDraftOverlay()` 逻辑一致，草稿/审核状态时从 `draftContent` 解析 DTO 覆盖列表页统计字段
2. **发布逻辑**：
   - 解析 `draftContent` → `GrammarPointDto`
   - 主表字段回写（name, hskLevel, project, category, subCategory）
   - 各子表 sync：对每个子表执行 sync（新增/更新/软删除对比）
   - 对于含例句的子表：先将 `sentences` 逐个保存到 `example_sentence` 表，收集返回的 ID，序列化为 JSON 数组存入 `*_sentence_ids` 列
   - 清理旧例句：sync 过程中被删除的子项的旧例句调用 `exampleSentenceService.disableByIds()`
3. **详情查询**：草稿/审核状态从 `draftContent` 反序列化返回；已发布状态从 DB 主表+子表查询并 hydrate 例句
4. **Helper 方法**：复用现有的 `ExampleSentenceService`（`save`, `findByIds`, `disableByIds`）

### 5.8 Wrapper

**GrammarPointWrapper** 负责 Request ↔ Dto ↔ VO 的转换，与 `CharCharacterWrapper` 同模式，包含：
- `toCriteria(GrammarPointQueryRequest)` → `GrammarPointQueryCriteria`
- `toDto(GrammarPointCreateRequest)` → `GrammarPointDto`
- `toVO(GrammarPointDto)` → `GrammarPointVO`
- `toBaseVOList(List<GrammarPointDto>)` → `List<GrammarPointBaseVO>`
- 子表相关的私有转换方法
- 例句相关的 `toExampleSentenceDto` / `toExampleSentenceVO` 方法

## 6. Example Sentence Usage

遵循已有模式：

- **存储**：子表的 `*_sentence_ids` 列存储 JSON 数组字符串（如 `"[1,2,3]"`）
- **保存**：发布时调用 `exampleSentenceService.save(dto)` 逐条写入 `example_sentence` 表
- **查询**：发布后通过 `exampleSentenceService.findByIds(ids)` 批量查询并 hydrate 到 VO
- **删除**：子项被删除时，关联的例句通过 `exampleSentenceService.disableByIds(ids)` 软删除
- `ExampleSentenceService` 和 `ExampleSentenceDto` 等已是共享组件，无需新建

## 7. Error Handling

与现有模式一致：
- `EntityNotFoundException`：实体不存在或已软删除
- `BadRequestException`：状态不允许的操作（如非草稿状态审核、草稿内容缺失等）
- `GlobalExceptionHandler` 统一处理异常响应

## 8. Out of Scope

- App 端普通用户的语法内容接口（后续再实现）
- 语法点与汉字的关联功能
- 批量导入/导出
- 语法点搜索/推荐算法

## 9. Files To Create

总计约 22 个新文件：

1. `grid-system/.../domain/grammar/GrammarPoint.java`
2. `grid-system/.../domain/grammar/GrammarMeaning.java`
3. `grid-system/.../domain/grammar/GrammarStructure.java`
4. `grid-system/.../domain/grammar/GrammarNotice.java`
5. `grid-system/.../domain/grammar/GrammarError.java`
6. `grid-system/.../repo/grammar/GrammarPointRepository.java`
7. `grid-system/.../repo/grammar/GrammarMeaningRepository.java`
8. `grid-system/.../repo/grammar/GrammarStructureRepository.java`
9. `grid-system/.../repo/grammar/GrammarNoticeRepository.java`
10. `grid-system/.../repo/grammar/GrammarErrorRepository.java`
11. `grid-system/.../service/grammar/GrammarPointService.java`
12. `grid-system/.../service/grammar/dto/GrammarPointDto.java`
13. `grid-system/.../service/grammar/dto/GrammarPointQueryCriteria.java`
14. `grid-system/.../service/grammar/dto/GrammarMeaningDto.java`
15. `grid-system/.../service/grammar/dto/GrammarStructureDto.java`
16. `grid-system/.../service/grammar/dto/GrammarNoticeDto.java`
17. `grid-system/.../service/grammar/dto/GrammarErrorDto.java`
18. `grid-system/.../service/grammar/mapstruct/GrammarPointMapper.java`
19. `grid-system/.../service/grammar/impl/GrammarPointServiceImpl.java`
20. `grid-system/.../rest/controller/GrammarPointController.java`
21. `grid-system/.../rest/request/GrammarPointCreateRequest.java`
22. `grid-system/.../rest/request/GrammarPointQueryRequest.java`
23. `grid-system/.../rest/vo/GrammarPointVO.java`
24. `grid-system/.../rest/vo/GrammarPointBaseVO.java`
25. `grid-system/.../rest/wrapper/GrammarPointWrapper.java`
