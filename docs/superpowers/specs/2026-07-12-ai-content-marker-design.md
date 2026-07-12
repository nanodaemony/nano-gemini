# AI 内容标记 — 设计文档

**日期**: 2026-07-12
**状态**: 设计中
**范围**: 后台管理系统 (grid-system)

## 一、背景与目标

需求文档（`0624有路中文_C端交互需求汇总.md`）第 9 节明确指出：

> 所有 AI 生成内容必须明显标识。纲外词释义、AI 例句、AI 翻译要与平台审定内容区分。

当前系统中，AI 生成的例句、翻译、释义等已大量投入使用，但后台没有统一的标记机制来区分 AI 内容与人工内容。本次设计建立一个通用的 **AI 内容来源标记机制**，覆盖所有后台管理模块中可能由 AI 生成字段的实体表。

## 二、设计原则

1. **独立表存储** — 不与任何业务表耦合，新增实体/字段无需 DDL
2. **标记跟随内容** — 前端提交内容时一并传入标记，查询时一并返回，不额外增加请求
3. **草稿友好** — 有草稿流程的实体，标记随 draft_content 存储，发布时落地到独立表
4. **扁平自描述** — 每个有独立 entity_id 的 VO 节点自描述自己的 AI 字段，无需全局路径
5. **不面向 C 端** — AI 标记仅供后台审核验收使用，C 端接口不暴露

## 三、数据模型

### 3.1 AI 内容标记表

```sql
CREATE TABLE `ai_content_marker` (
    `id`              BIGINT AUTO_INCREMENT,
    `entity_type`     VARCHAR(50)  NOT NULL COMMENT '实体表名',
    `entity_id`       BIGINT       NOT NULL COMMENT '实体记录ID',
    `field_name`      VARCHAR(255) NOT NULL COMMENT 'Java字段名(驼峰)',
    `ai_generated`    TINYINT      NOT NULL DEFAULT 1 COMMENT '1=AI生成 0=人工',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_entity_field` (`entity_type`, `entity_id`, `field_name`),
    KEY `idx_entity` (`entity_type`, `entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI内容来源标记表';
```

### 3.2 不存即为人工

`ai_content_marker` 中**只存 AI 生成字段**（`ai_generated=1`）。不在表里的字段一律视为人工内容。如果前端传空列表，表示该实体所有字段均为人工，表中没有记录。

### 3.3 一期覆盖范围

| entity_type | 表名 | 可能 AI 生成的字段 |
|---|---|---|
| `example_sentence` | example_sentence | sentence, pinyin, translations |
| `vocab_sense` | vocab_sense | chineseDef, defTranslations |
| `vocab_structure` | vocab_structure | pattern, patternDef, patternDefTranslations |
| `grammar_meaning` | grammar_meaning | meaningContent, meaningContentTranslations |
| `grammar_structure` | grammar_structure | structureContent |
| `grammar_notice` | grammar_notice | noticeContent, noticeContentTranslations |
| `grammar_error` | grammar_error | errorContent, errorAnalysis, errorAnalysisTranslations |
| `daily_vocabulary` | daily_vocabulary | phraseTranslations, plainExplanation, explanationTranslations, originStory |
| `char_character` | char_character | charDesc, charDescTranslations |
| `char_comparison` | char_comparison | comparisonCharTranslations, comparisonDescTranslations |
| `char_word` | char_word | wordItemTranslations |
| `vocab_comparison_item` | vocab_comparison_item | usageComparison, usageComparisonTranslations, commonUsage, commonUsageTranslations |
| `vocab_comparison_chat` | vocab_comparison_chat | content |
| `grammar_comparison_item` | grammar_comparison_item | 辨析内容字段 |
| `exercise_question` | exercise_question | 题目内容字段 |

新增实体/字段无需修改表结构，调用方传入新的 `entity_type` 或 `field_name` 即可。

## 四、API 设计

### 4.1 不新增独立接口

AI 标记不与业务接口解耦，而是嵌入现有的创建/更新/查询流程中：

- **写入**：Request 子对象上带 `aiGeneratedFields` 字段，随业务创建/更新接口一起提交
- **查询**：VO 子对象上带 `aiGeneratedFields` 字段，Wrapper 从 `ai_content_marker` 批量查询注入

### 4.2 Request 变更

在每个对应实体子表的 Request 类上追加一个字段：

```java
// 示例：VocabSenseRequest
@ApiModelProperty(value = "AI生成的字段名列表（Java字段名驼峰）")
private List<String> aiGeneratedFields;
```

需要追加的 Request 类：

| Request 类 | 所属模块 |
|---|---|
| `ExampleSentenceRequest` | 通用（被多处复用，加一次全生效） |
| `VocabSenseRequest` | 词汇 |
| `VocabStructureRequest` | 词汇 |
| `GrammarMeaningRequest` | 语法 |
| `GrammarStructureRequest` | 语法 |
| `GrammarNoticeRequest` | 语法 |
| `GrammarErrorRequest` | 语法 |
| `CharCharacterCreateRequest` 的相关子 Request | 汉字 |
| `DailyVocabularyCreateRequest` | 每日一语 |
| `VocabComparisonItem` 相关 Request | 词汇辨析 |
| `GrammarComparisonItem` 相关 Request | 语法辨析 |

### 4.3 VO 变更

在每个对应实体子表的 VO 类上追加：

```java
@ApiModelProperty(value = "AI生成的字段名列表")
private List<String> aiGeneratedFields;
```

需要追加的 VO 类：

| VO 类 |
|---|
| `ExampleSentenceVO` |
| `VocabWordVO.VocabSenseVO` |
| `VocabWordVO.VocabStructureVO` |
| `GrammarPointVO.GrammarMeaningVO` |
| `GrammarPointVO.GrammarStructureVO` |
| `GrammarPointVO.GrammarNoticeVO` |
| `GrammarPointVO.GrammarErrorVO` |
| `CharCharacterVO` 的相关子 VO |
| `DailyVocabularyVO` |
| `VocabComparisonGroupVO` 的相关子 VO |
| `GrammarComparisonGroupVO` 的相关子 VO |

### 4.4 提交示例

```json
// POST/PUT /api/vocab-words
{
    "word": "花",
    "senses": [{
        "chineseDef": "植物的生殖器官",
        "defTranslations": [{"language": "en", "translation": "..."}],
        "aiGeneratedFields": ["chineseDef", "defTranslations"],
        "structures": [{
            "pattern": "花 + O[时间/钱/...]",
            "aiGeneratedFields": ["pattern"],
            "structureSentences": [
                {
                    "sentence": "我花了一个小时做作业。",
                    "aiGeneratedFields": ["sentence"]
                }
            ]
        }]
    }]
}
```

### 4.5 返回示例

```json
// GET /api/vocab-words/123
{
    "id": 123,
    "word": "花",
    "senses": [{
        "id": 88,
        "chineseDef": "植物的生殖器官",
        "defTranslations": [...],
        "aiGeneratedFields": ["chineseDef", "defTranslations"],
        "structures": [{
            "id": 100,
            "pattern": "花 + O[...]",
            "aiGeneratedFields": ["pattern"],
            "structureSentences": [{
                "id": 42,
                "sentence": "我花了一个小时做作业。",
                "aiGeneratedFields": ["sentence"]
            }]
        }]
    }]
}
```

每个 VO 节点只声明**自身实体**的 AI 字段。`aiGeneratedFields` 里存的始终是当前 Java 对象的字段名，不是全局路径。

## 五、处理流程

### 5.1 写入：草稿实体 vs 无草稿实体

#### 无草稿实体（example_sentence、daily_vocabulary 等）

```
前端提交 Request（带 aiGeneratedFields）
  → Wrapper → Dto
    → Service 保存实体，得到 entity_id
    → Service 调用 AiContentMarkerService.replaceFields(entityType, entityId, aiGeneratedFields)
      → DELETE 旧标记
      → INSERT 新标记
```

#### 有草稿实体（vocab_word、char_character、grammar_point 等）

```
前端提交 Request（带 aiGeneratedFields）
  → Wrapper → Dto
    → Service 将 aiGeneratedFields 随其他字段一起写入 draft_content JSON
    → 数据库没有 ai_content_marker 记录（草稿状态）

发布时：
  发布 Service 写入子表，获得真实 ID
    → 从 draft_content 中提取每个子实体的 aiGeneratedFields
    → 对每个子实体调用 AiContentMarkerService.replaceFields()
```

### 5.2 查询

```
前端请求详情
  → Controller → Service 查询业务数据
    → Wrapper.toVO() 前：收集所有子实体的 (entity_type, entity_id) 列表
    → AiContentMarkerService.batchQuery(entities) 批量查询标记
      → 返回 Map<String, List<String>>  key="entity_type:entity_id"
    → Wrapper 按 (entity_type, entity_id) 分发标记到各个子 VO
```

### 5.3 更新语义

每个实体独立进行**全量替换**：

1. DELETE 该 `(entity_type, entity_id)` 下所有现有标记
2. INSERT 前端传入的 `aiGeneratedFields` 列表中的每一项

传空列表 = 清除所有标记 = 所有字段均为人工。

## 六、新增模块

### 6.1 文件清单

所有新文件放在 grid-system 下：

```
grid-system/src/main/java/com/naon/grid/
├── modules/system/
│   └── domain/
│       └── AiContentMarker.java                       # JPA 实体
│   └── repository/
│       └── AiContentMarkerRepository.java             # Spring Data JPA Repository
│   └── service/
│       └── AiContentMarkerService.java                # 标记查询与替换
│       └── impl/
│           └── AiContentMarkerServiceImpl.java
│   └── service/dto/
│       └── AiContentMarkerDto.java                    # (可选)内部传输对象
```

### 6.2 AiContentMarkerService 接口设计

```java
public interface AiContentMarkerService {

    /**
     * 全量替换指定实体的 AI 标记字段。
     * @param entityType 实体表名
     * @param entityId   实体记录ID
     * @param aiFields   AI生成的字段名列表（传null或空列表则清除所有标记）
     */
    void replaceFields(String entityType, Long entityId, List<String> aiFields);

    /**
     * 批量替换（一次数据库操作）。
     */
    void batchReplace(List<MarkerEntry> entries);

    /**
     * 批量查询 AI 标记。
     * @param entityKeys 实体键列表，格式 ["vocab_sense:88", "example_sentence:42"]
     * @return Map<"entity_type:entity_id", List<fieldName>>
     */
    Map<String, List<String>> batchQuery(List<String> entityKeys);

    @Data
    @AllArgsConstructor
    class MarkerEntry {
        private String entityType;
        private Long entityId;
        private List<String> aiFields;
    }
}
```

### 6.3 SQL 文件

新增到 `sql/biz_common.sql`（通用基础表）。

## 七、现有接口影响

### 7.1 Controller 层

- 创建/更新接口不需要修改 Controller 逻辑
- 详情查询接口：在 Controller 调用 Wrapper 之前，注入 `AiContentMarkerService` 进行批量查询，将结果传入 Wrapper 的 VO 转换方法

### 7.2 Wrapper 层

- `toVO()` 方法增加 `Map<String, List<String>> aiMarkers` 参数
- 在构建每个子 VO 时，根据 `(entity_type, entity_id)` 查找对应的 `aiGeneratedFields` 并设置

### 7.3 Service 层（发布流程）

- 词汇发布 Service、汉字发布 Service、语法发布 Service 等：在发布时从 draft_content 中提取 `aiGeneratedFields`，物化到 `ai_content_marker`

### 7.4 不影响的

- grid-app（C 端）不需要任何修改
- grid-billing、grid-tools、grid-common 不需要修改
- 现有的数据库表结构不需要任何变更（仅新增 `ai_content_marker` 表）

## 八、前端渲染约定

前端收到 VO 后，按以下规则渲染 AI 标识：

1. 字段所在 VO 节点的 `aiGeneratedFields` 包含该字段名 → 渲染 AI 标签/图标
2. 不在列表中 → 无标识（人工内容）
3. 标签样式：建议使用统一的 AI 小标签（如 `🤖 AI` 蓝色标签），放在字段值旁边或输入框上方

## 九、注意事项

- `field_name` 使用 Java 驼峰命名，与 Request/VO/Entity 的 Java 字段名一致，**不是**数据库 snake_case 列名
- `field_name` 始终相对于当前 VO 对象，不包含父级路径。例如 `VocabSenseVO.aiGeneratedFields = ["chineseDef"]`，而不是 `["senses[0].chineseDef"]`
- 有草稿实体的子表在发布前没有 `ai_content_marker` 记录，只有发布后才写入。编辑草稿期间前端从 draft_content JSON 中读取标记
- `ai_content_marker` 表的记录不参与业务查询/搜索，仅用于详情展示的标记回填
