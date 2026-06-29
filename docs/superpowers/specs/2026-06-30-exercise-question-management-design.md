# Exercise Question Management 设计文档

## 概述

新增练习题目（exercise_question）的后台管理模块，包含草稿、审核、发布上线等完整工作流。对齐汉字（CharCharacter）管理流程。

## 表结构

参考 `sql/biz_common.sql` 中 `exercise_question` 表：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 题目ID |
| parent_id | bigint, default 0 | 父题ID（0=大题） |
| question_type | varchar(32) | 题型，参考 QuestionTypeEnum |
| stem | varchar(512) | 题干 |
| content | varchar(4096) | 题目内容材料 JSON（QuestionContent） |
| options | varchar(2048) | 选项列表 JSON（List\<QuestionOption\>） |
| answer | varchar(512) | 答案列表 JSON（List\<String\>） |
| explanation | varchar(1024) | 解析 |
| audio_id | bigint | 听力音频ID |
| sort | int | 排序（值大靠前） |
| draft_content | text | 草稿内容 JSON |
| edit_status | varchar(20) | draft / reviewed / published |
| publish_status | varchar(20) | unpublished / published |
| create_by / update_by | varchar(255) | 审计 |
| create_time / update_time | datetime | 审计 |
| status | tinyint | 1=有效，0=删除 |

## 文件清单

```
grid-system/src/main/java/com/naon/grid/backend/
├── domain/question/
│   └── ExerciseQuestion.java
├── repo/question/
│   └── ExerciseQuestionRepository.java
├── service/question/
│   ├── ExerciseQuestionService.java
│   ├── dto/
│   │   ├── ExerciseQuestionDto.java
│   │   └── ExerciseQuestionQueryCriteria.java
│   └── impl/
│       └── ExerciseQuestionServiceImpl.java
├── rest/
│   ├── controller/
│   │   └── ExerciseQuestionController.java
│   ├── request/
│   │   ├── ExerciseQuestionCreateRequest.java
│   │   └── ExerciseQuestionQueryRequest.java
│   ├── vo/
│   │   ├── ExerciseQuestionVO.java
│   │   ├── ExerciseQuestionBaseVO.java
│   │   └── ExerciseQuestionCreateVO.java
│   └── wrapper/
│       └── ExerciseQuestionWrapper.java
```

## DO — ExerciseQuestion

JPA Entity，继承 `BaseEntity`，字段与表一一映射。

```java
@Entity
@Table(name = "exercise_question")
public class ExerciseQuestion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId = 0L;
    private String questionType;
    private String stem;
    private String content;          // JSON string
    private String options;          // JSON string
    private String answer;           // JSON string
    private String explanation;
    private Long audioId;
    private Integer sort = 0;
    private String draftContent;     // JSON string

    private String editStatus = EditStatusEnum.DRAFT.getCode();
    private String publishStatus = PublishStatusEnum.UNPUBLISHED.getCode();
    private Integer status = StatusEnum.ENABLED.getCode();
}
```

## DTO — ExerciseQuestionDto

业务数据载体（不含 JPA 注解），JSON 字段在此展开为结构化类型。

```java
public class ExerciseQuestionDto extends BaseDTO {
    private Long id;
    private Long parentId;
    private String questionType;
    private String stem;
    private QuestionContent content;           // 结构化
    private List<QuestionOption> options;       // 结构化
    private List<String> answer;                // 结构化
    private String explanation;
    private Long audioId;
    private Integer sort;

    private String editStatus;
    private String publishStatus;
    private Integer status;
    private String draftContent;

    // 子题列表（仅在详情需要）
    private List<ExerciseQuestionDto> children;

    // 列表统计（仅在列表 VO 需要）
    private Integer childCount;
}
```

## Repository — ExerciseQuestionRepository

继承 `JpaRepository<ExerciseQuestion, Long>` 和 `JpaSpecificationExecutor<ExerciseQuestion>`，提供：

- `findByParentIdAndStatus(Long parentId, Integer status)` — 查询某大题的所有子题
- `countByParentIdAndStatus(Long parentId, Integer status)` — 统计子题数

## Service 接口

```java
public interface ExerciseQuestionService {
    PageResult<ExerciseQuestionDto> queryAll(ExerciseQuestionQueryCriteria criteria, Pageable pageable);
    ExerciseQuestionDto findById(Long id);
    Long create(ExerciseQuestionDto dto);
    void update(Long id, ExerciseQuestionDto dto);
    void delete(Long id);
    void reviewDraft(Long id);
    void publishDraft(Long id);
    void offline(Long id);
}
```

## Service 实现 — 草稿工作流

### 创建
1. 新建 `ExerciseQuestion` 实体
2. 设置 `status=1, editStatus=draft, publishStatus=unpublished`
3. 将传入 dto（含 children）json 序列化存入 `draft_content`
4. 业务字段（questionType, stem 等）同步写入主表相应列，确保搜索可用

### 更新
1. 加载实体，校验存在性
2. 如果已发布，不允许修改 questionType（同汉字不允许改汉字本身的约束）
3. `editStatus` 设为 draft（已审核/已发布都回退到草稿）
4. 更新 `draft_content`

### 审核（review）
1. 校验 `editStatus = draft`，`draft_content` 不为空
2. `editStatus = reviewed`

### 发布（publish）
1. 校验 `editStatus = reviewed`，`draft_content` 不为空
2. 解析 draft_content，获取完整数据（含 children）
3. 回写父题业务字段到主表
4. syncChildren：根据 draft 中的 children 列表，与数据库现有子题做差量更新
   - 新增：无 id → INSERT
   - 更新：有 id → UPDATE
   - 删除：DB 中有但 draft 中没有 → 软删除（status=0）
5. `editStatus = published, publishStatus = published, draft_content = null`

### 详情查询
- draft/reviewed 状态：从 `draft_content` 解析完整结构（含 children），以草稿数据覆盖业务字段返回
- published 状态：从主表字段 + 子表查询 children 组装

### 分页列表
1. 只查 `parent_id = 0` 的记录
2. 使用 `QueryHelp.getPredicate` 构建查询条件
3. 对每条记录应用 `toDtoWithDraftOverlay`：
   - draft/reviewed：从 draft_content 解析并覆盖业务字段
   - published：直接使用主表字段
4. `childCount`：draft/reviewed 从 children 列表大小获取，published 通过 DB count 获取

## REST 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/exercise-question` | 新增 |
| PUT | `/api/exercise-question/{id}` | 更新 |
| PUT | `/api/exercise-question/{id}/review` | 审核通过 |
| PUT | `/api/exercise-question/{id}/publish` | 发布 |
| PUT | `/api/exercise-question/{id}/offline` | 下线 |
| DELETE | `/api/exercise-question/{id}` | 删除 |
| GET | `/api/exercise-question/{id}` | 详情 |
| GET | `/api/exercise-question` | 分页列表 |

使用 `@Log`、`@AnonymousPostMapping` 等自定义注解，与现有 Controller 风格一致。

### 请求类

**ExerciseQuestionCreateRequest**（创建/更新共用）：
```java
private String questionType;
private String stem;
private QuestionContentReq content;
private List<QuestionOptionReq> options;
private List<String> answer;
private String explanation;
private Long audioId;
private Integer sort;
private List<ExerciseQuestionCreateRequest> children;

// 内部类
static QuestionContentReq { String contentText; String contentImageId; }
static QuestionOptionReq { String option; String optionText; String optionImageId; }
```

**ExerciseQuestionQueryRequest**（分页查询）：
```java
private String blurry;       // 题干模糊搜索
private String questionType; // 题型筛选
private String publishStatus;
private String editStatus;
```

### VO 类

**ExerciseQuestionVO**（详情）— 大题嵌套子题：
```java
private Long id;
private String questionType;
private String stem;
private QuestionContentVO content;
private List<QuestionOptionVO> options;
private List<String> answer;
private String explanation;
private Long audioId;
private Integer sort;
private String editStatus;
private String publishStatus;
private String createBy, updateBy;
private Timestamp createTime, updateTime;
private List<ExerciseQuestionVO> children;   // 子题列表（不嵌套孙子题）
```

**ExerciseQuestionBaseVO**（列表）— 仅大题概要：
```java
private Long id;
private String questionType;
private String stem;
private Long audioId;
private Integer sort;
private Integer childCount;
private String editStatus;
private String publishStatus;
private String createBy, updateBy;
private Timestamp createTime, updateTime;
```

**ExerciseQuestionCreateVO**（创建结果）：
```java
private Long id;
```

### Wrapper

纯静态方法，只做映射，不涉及外部服务调用：

- `toDto(ExerciseQuestionCreateRequest)` — Request → Dto
- `toCriteria(ExerciseQuestionQueryRequest)` — QueryRequest → Criteria
- `toVO(ExerciseQuestionDto)` — Dto → VO（递归转换 children）
- `toBaseVO(ExerciseQuestionDto)` — Dto → BaseVO（列表用）
- `toBaseVOList(List<ExerciseQuestionDto>)` — 批量转列表 VO

JSON 字段的序列化/反序列化使用 `JsonUtils`：
- `QuestionContent` ↔ `JsonUtils.toJson` / `JsonUtils.fromJson`
- `List<QuestionOption>` ↔ `JsonUtils.toExerciseOptionListJson` / `JsonUtils.parseExerciseOptionList`
- `List<String>` ↔ `JsonUtils.toStringListJson` / `JsonUtils.parseStringList`

## 不包含的范围

- App 端（终端用户）的题目查询接口
- 子题独立的 CRUD 接口（通过整体更新管理）
- 听力音频、图片资源的上传与管理
- 题目批改、作答记录等功能
