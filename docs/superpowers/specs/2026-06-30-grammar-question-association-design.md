# Grammar-Question Association Design

## 概述

在语法点（GrammarPoint）中关联题目（ExerciseQuestion），允许语法后台管理语法点与题目之间的关联关系。每个语法点可以关联多个题目 ID。

## 数据库

表 `grammar_question` 已存在于 `sql/biz_grammer.sql` 中。每条记录存储一个语法点的题目 ID 列表（JSON 数组格式）：

```sql
CREATE TABLE `grammar_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '语法偏误ID',
    `grammar_id` BIGINT NOT NULL COMMENT '语法点ID',
    `question_ids` varchar(1024) NOT NULL COMMENT '语法题目ID列表, JSON格式',
    `order` INT NOT NULL DEFAULT 0 COMMENT '排序权重(值大的排前面)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `status` TINYINT NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    INDEX `idx_grammar_id` (`grammar_id`),
    INDEX `idx_status_order` (`status`, `order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语法点题目表';
```

一个语法点对应一条 `grammar_question` 记录，`question_ids` 存所有关联题目的 ID 数组。

## 数据流

### 编辑（草稿阶段）
- `questionIds` 作为 `GrammarPointDto` 的一个字段（`List<Long>`）
- create/update 时，整个 DTO（含 `questionIds`）被序列化为 `draft_content` JSON
- 编辑中所有变更都在草稿中，不影响已发布数据

### 发布
- `publishDraft()` 解析 `draft_content` 后调用 `syncQuestions()`
- 逻辑：soft-delete 旧记录 → 插入新记录（与 `syncMeanings` 相同模式）

### 查询
- **详情**：已发布 → 从 `grammar_question` 表加载；草稿/审核中 → 从 `draft_content` 读取
- **列表**：批量查询所有语法点的 `questionIds`，填充到 DTO

## 新增文件

### 1. `GrammarQuestion.java` — Entity

```java
@Entity
@Table(name = "grammar_question")
public class GrammarQuestion {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private Long grammarId;
    private String questionIds;        // JSON 字符串
    private Integer order = 0;
    private Integer status = 1;
    private Timestamp createTime;
    private Timestamp updateTime;
}
```

### 2. `GrammarQuestionRepository.java` — Repository

```java
public interface GrammarQuestionRepository extends JpaRepository<GrammarQuestion, Long> {
    List<GrammarQuestion> findByGrammarIdAndStatus(Long grammarId, Integer status);
    List<GrammarQuestion> findByGrammarIdInAndStatus(Collection<Long> grammarIds, Integer status);
}
```

### 3. `GrammarQuestionDto.java` — DTO

```java
public class GrammarQuestionDto {
    private Long id;
    private Long grammarId;
    private List<Long> questionIds;   // DTO 层用 List，自动 JSON 序列化/反序列化
    private Integer order;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
```

### 4. `GrammarQuestionService.java` + `GrammarQuestionServiceImpl.java` — Service

```java
public interface GrammarQuestionService {
    List<Long> findByGrammarId(Long grammarId);
    Map<Long, List<Long>> findByGrammarIds(Collection<Long> grammarIds);
    void syncFromDraft(Long grammarId, List<Long> questionIds);
}
```

`syncFromDraft` 实现：
1. 查找现有 `grammar_question` 记录
2. 如果存在：soft-delete 旧记录
3. 如果新 `questionIds` 非空：创建新记录

## 修改文件

### 1. `GrammarPointDto.java`
- 新增字段 `private List<Long> questionIds;`

### 2. `GrammarPointCreateRequest.java`
- 新增字段 `private List<Long> questionIds;`

### 3. `GrammarPointVO.java` 和 `GrammarPointBaseVO.java`
- 新增字段 `private List<Long> questionIds;`

### 4. `GrammarPointWrapper.java`
- `toDto()`: 将 `request.getQuestionIds()` 映射到 DTO
- `toVO()`: 将 `dto.getQuestionIds()` 映射到 VO
- `toBaseVO()`: 将 `dto.getQuestionIds()` 映射到 BaseVO

### 5. `GrammarPointServiceImpl.java`

三处改动：
1. **`publishDraft()`** — 在 `syncErrors` 后调用 `syncQuestions(grammarPoint.getId(), draftDto.getQuestionIds())`
2. **`toPublishedDetailDto()`** — 加载已发布数据时，从 `grammar_question` 表读取 questionIds
3. **`populateGrammarListStats()`** — 批量加载所有语法点的 questionIds

## API 变更（接口定义不变）

所有变更在现有的语法点 CRUD 接口中完成，无需新增 Controller 端点：

| 接口 | 变更 |
|------|------|
| `POST /api/grammar` | request body 增加 `questionIds` 字段 |
| `PUT /api/grammar/{id}` | request body 增加 `questionIds` 字段 |
| `GET /api/grammar/{id}` | response body 返回 `questionIds` |
| `GET /api/grammar` | 列表 response 每项返回 `questionIds` |
| `PUT /api/grammar/{id}/publish` | 同步到 `grammar_question` 表 |

## 不包含的范围

- 不返回题目详情（只返回 ID 列表）
- 不新增 Controller 端点（复用现有语法点 CRUD）
- 前端跳转逻辑由前端处理，后端不涉及
