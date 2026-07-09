# 统一搜索接口设计

## 概述

将 C 端 4 个独立的搜索接口合并为一个统一接口 `GET /api/app/search?q={keyword}`，输入关键词，跨模块模糊匹配，按类别分组返回。原有 4 个接口保留不变。

## API 设计

### 请求

```
GET /api/app/search?q={keyword}
```

- 匿名访问（`@AnonymousGetMapping`）
- 参数 `q`：搜索关键词
- `q` 为空或空白时，返回四个空数组，不查数据库

### 响应

```json
{
  "vocab": [
    { "id": 1, "word": "花", "pinyin": "huā", "hskLevel": "3" }
  ],
  "character": [
    { "id": 5, "character": "花", "pinyin": "huā", "hskLevel": "3" }
  ],
  "grammar": [
    { "id": 3, "name": "花+时间/钱", "hskLevel": "4", "category": "结构" }
  ],
  "comparison": [
    {
      "groupId": 2,
      "groupKey": "花 vs 花费",
      "type": "vocab",
      "items": [
        { "wordId": 1, "word": "花" },
        { "wordId": 3, "word": "花费" }
      ]
    },
    {
      "groupId": 5,
      "groupKey": "了 vs 过",
      "type": "grammar",
      "items": [
        { "grammarId": 10, "grammarName": "了" },
        { "grammarId": 12, "grammarName": "过" }
      ]
    }
  ]
}
```

每个模块最多返回 20 条，无分页，满足搜索建议场景。

## 搜索规则

### 1. 词汇搜索
- 模糊匹配 `vocab_word.word`（LIKE %keyword%）
- 条件：`publish_status='published'` AND `status=1`
- 排序：id ASC，取前 20 条
- 调用：`vocabWordService.queryAll(criteria, PageRequest(0, 20, ASC, "id"))`
  - criteria: `setBlurry(q)`, `setPublishStatus("published")`
- VO：复用 `AppVocabWordWrapper.toBaseVOList()`
- 如果词汇搜索结果为空，调用 `vocabOutlineRecordService.recordIfNeeded(q)` 记录纲外词

### 2. 汉字搜索
- 模糊匹配 `char_character.character`（LIKE %keyword%）
- 条件：`publish_status='published'` AND `status=1`
- 调用：`charCharacterService.searchPublishedByCharacter(q)`，取前 20 条
- VO：复用 `AppCharCharacterWrapper.toBaseVOList()`

### 3. 语法搜索
- 模糊匹配 `grammar_point.name`（LIKE %keyword%）
- 条件：`publish_status='published'` AND `status=1`
- 排序：id ASC，取前 20 条
- 调用：`grammarPointService.searchPublished(q, PageRequest(0, 20, ASC, "id"))`
- VO：复用 `AppGrammarPointWrapper.toBaseVOList()`

### 4. 辨析搜索
两个来源，合并到 `comparison` 数组，用 `type` 区分：

**词汇辨析**：
- 模糊匹配 `vocab_comparison_item.word`（LIKE %keyword%）
- 找到匹配的 item → 反查 group → 仅返回已发布 group（`publish_status='published'` AND `status=1`）
- 取前 20 条
- 新增方法：`VocabComparisonGroupService.searchByWordFuzzy(String word, int limit)`

**语法辨析**：
- 模糊匹配 `grammar_comparison_item.grammar_name`（LIKE %keyword%）
- 找到匹配的 item → 反查 group → 仅返回已发布 group
- 取前 20 条
- 新增方法：`GrammarComparisonGroupService.searchByGrammarNameFuzzy(String name, int limit)`

**辨析 items 格式**（精简版）：
- vocab 类型：`{ "wordId": Long, "word": String }`
- grammar 类型：`{ "grammarId": Long, "grammarName": String }`

## 文件清单

### 新增文件（位于 grid-app 模块）

| 文件 | 包路径 | 说明 |
|------|--------|------|
| `AppSearchController.java` | `com.naon.grid.modules.app.rest` | 统一搜索入口 |
| `AppSearchWrapper.java` | `com.naon.grid.modules.app.rest.wrapper` | DTO → VO 转换 |
| `AppSearchResultVO.java` | `com.naon.grid.modules.app.rest.vo` | 返回结构 VO |
| `AppComparisonGroupVO.java` | `com.naon.grid.modules.app.rest.vo` | 辨析组精简 VO |
| `AppComparisonItemVO.java` | `com.naon.grid.modules.app.rest.vo` | 辨析子项精简 VO |

### 修改文件（位于 grid-system 模块）

| 文件 | 修改内容 |
|------|----------|
| `VocabComparisonGroupService.java` | 新增 `searchByWordFuzzy(String, int)` 接口方法 |
| `VocabComparisonGroupServiceImpl.java` | 实现 `searchByWordFuzzy` |
| `VocabComparisonItemRepository.java` | 新增 `findByWordContainingAndStatus(String, Integer)` |
| `GrammarComparisonGroupService.java` | 新增 `searchByGrammarNameFuzzy(String, int)` 接口方法 |
| `GrammarComparisonGroupServiceImpl.java` | 实现 `searchByGrammarNameFuzzy` |
| `GrammarComparisonItemRepository.java` | 新增 `findByGrammarNameContainingAndStatus(String, Integer)` |

## 数据流

```
GET /api/app/search?q=关键词
       │
       ▼
AppSearchController.search(q)
       │
       ├─ q 为空/空白 → 返回四个空数组
       │
       ├─ vocabWordService.queryAll(criteria, pageable)
       │     └─ 为空 → vocabOutlineRecordService.recordIfNeeded(q)
       │
       ├─ charCharacterService.searchPublishedByCharacter(q) → 取前 20
       │
       ├─ grammarPointService.searchPublished(q, pageable)
       │
       ├─ vocabComparisonGroupService.searchByWordFuzzy(q, 20)
       │     └─ type = "vocab"
       │
       └─ grammarComparisonGroupService.searchByGrammarNameFuzzy(q, 20)
             └─ type = "grammar"
       │
       ▼
  AppSearchWrapper.toResultVO(...)
       ▼
  AppSearchResultVO { vocab, character, grammar, comparison }
```

## 边界条件

| 场景 | 行为 |
|------|------|
| `q` 为空/空白 | 返回四个空数组，HTTP 200，不查数据库 |
| 某模块无匹配 | 该模块返回空数组 `[]` |
| 词汇搜索为空 | 调用 `vocabOutlineRecordService.recordIfNeeded(q)` |
| 全部模块无匹配 | 四个空数组，HTTP 200 |
| 运行时异常 | 由 `GlobalExceptionHandler` 统一处理 |

## 不做什么（第一版）

- 不分词
- 不同形词聚合
- 不排序优化（各模块独立按 ID ASC）
- 不分页
- 不缓存
- 原有 4 个搜索接口保持不变
