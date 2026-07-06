# App 端语法接口设计

**日期**: 2026-07-06
**范围**: 为普通 App 用户端提供语法点查询接口，复现 `AppVocabWordController` 模式。

## 1. 目标

为 grid-app 模块新增 App 端语法点接口，支持：
- 关键词搜索已发布的语法点
- 查看已发布语法点详情（含关联辨析组和题目 ID）
- 按语言参数筛选翻译内容
- 音频/图片资源直接返回 URL，找不到则 log.error 不抛异常

## 2. 端点

```
GET  /api/app/grammar/search?keyword=xxx        → List<AppGrammarPointBaseVO>
GET  /api/app/grammar/{id}?language=en           → AppGrammarPointDetailVO
```

## 3. Service 层变更（grid-system）

### GrammarPointService 新增方法

```java
GrammarPointDto findPublishedById(Long id);
PageResult<GrammarPointDto> searchPublished(String keyword, Pageable pageable);
```

- `findPublishedById`: 复用现有 `toPublishedDetailDto()` 逻辑，仅从数据库子表加载已发布数据，绕过草稿覆盖。
- `searchPublished`: JPA Specification 在 `name` 字段模糊匹配，过滤 `publishStatus="published"` 和 `status=1`。

### 复用（不改动）

- `GrammarComparisonGroupService.searchByGrammarId(grammarId)` — 已存在
- `AudioResourceService.findByIds(ids)` — 已存在
- `AliOssStorageService.findByIds(ids)` — 已存在

## 4. Controller 层（grid-app）

### AppGrammarPointController

注入：`GrammarPointService`、`AudioResourceService`、`AliOssStorageService`、`GrammarComparisonGroupService`

**search 端点**：
1. 调用 `grammarPointService.searchPublished(keyword, pageable)`
2. 通过 `AppGrammarPointWrapper.toBaseVOList(dtos)` 转换

**getDetail 端点**：
1. 验证 `language` 参数非空
2. 调用 `grammarPointService.findPublishedById(id)` 获取 DTO
3. 遍历 DTO 中所有子数据收集 audio/image ID
4. 批量查询 `AudioResourceService.findByIds()`、`AliOssStorageService.findByIds()`
5. 调用 `grammarComparisonGroupService.searchByGrammarId(id)` 获取辨析组
6. 传入 `AppGrammarPointWrapper.toDetailVO(dto, audioMap, imageMap, comparisons, language)`

## 5. VO 结构

### AppGrammarPointBaseVO（搜索列表项）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 语法点ID |
| name | String | 名称 |
| hskLevel | String | HSK等级 |
| project | String | 项目 |
| category | String | 类别 |
| subCategory | String | 细目 |

### AppGrammarPointDetailVO（详情）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 语法点ID |
| name | String | 名称 |
| hskLevel | String | HSK等级 |
| project | String | 项目 |
| category | String | 类别 |
| subCategory | String | 细目 |
| meanings | List\<GrammarMeaningVO\> | 意义列表 |
| structures | List\<GrammarStructureVO\> | 结构列表 |
| notices | List\<GrammarNoticeVO\> | 注意列表 |
| errors | List\<GrammarErrorVO\> | 偏误列表 |
| questionIds | List\<Long\> | 关联题目ID |
| comparisons | List\<GrammarComparisonVO\> | 关联辨析组 |

### 嵌套 VO

**GrammarMeaningVO**: `id, content, translation(TextTranslationVO), image(ImageVO), sentences(List<ExampleVO>), order`

**GrammarStructureVO**: `id, content, sentences(List<ExampleVO>), order`

**GrammarNoticeVO**: `id, content, translation(TextTranslationVO), sentences(List<ExampleVO>), order`

**GrammarErrorVO**: `id, content, analysis, analysisTranslation(TextTranslationVO), order`

**GrammarComparisonVO**: `id, groupKey, items(List<ComparisonItemVO>)`

**ComparisonItemVO**: `grammarId, grammarName, usageComparison, usageComparisonTranslation(TextTranslationVO), exampleSentences, usageSentence(ExampleVO)`

**ExampleVO**: `sentence, pinyin, translation(TextTranslationVO), audio(AudioVO), image(ImageVO), order`

**AudioVO**: `audioUrl` — 直接返回音频文件 URL

**ImageVO**: `imageUrl` — 直接返回图片文件 URL

## 6. Wrapper 层（grid-app）

### AppGrammarPointWrapper

所有方法 `public static`，与 `AppVocabWordWrapper` 模式一致：

- `toBaseVOList(List<GrammarPointDto>)` → `List<AppGrammarPointBaseVO>`
- `toDetailVO(GrammarPointDto, Map<Long,AudioResourceDto>, Map<Long,AliOssStorageDto>, List<GrammarComparisonGroupDto>, String language)` → `AppGrammarPointDetailVO`

### 翻译过滤

`filterByLanguage(List<TextTranslation>, String language)` → `TextTranslationVO`，与 `AppVocabWordWrapper` 实现一致。

### 资源处理

- Controller 预加载所有 audio/image ID → 批量查询 → 传入 Map
- Wrapper 通过 ID 查 Map，找到则返回 URL，找不到则 `log.error("...")`

### 辨析组转换

`GrammarComparisonGroupDto` → `GrammarComparisonVO`：
- `groupKey` 映射为辨析组标题
- `items` 转换为 `ComparisonItemVO` 列表

## 7. 文件清单

### 新建（grid-app）

- `rest/AppGrammarPointController.java`
- `rest/request/AppGrammarPointSearchRequest.java`
- `rest/vo/AppGrammarPointBaseVO.java`
- `rest/vo/AppGrammarPointDetailVO.java`
- `rest/wrapper/AppGrammarPointWrapper.java`

### 修改（grid-system）

- `service/grammar/GrammarPointService.java` — 接口新增 2 方法
- `service/grammar/impl/GrammarPointServiceImpl.java` — 实现 2 方法

### 不改动

- `GrammarComparisonGroupService` / `AudioResourceService` / `AliOssStorageService`
- 所有 Repository / Entity
