# 语法辨析 APP 端情景对话功能设计

**日期**: 2026-07-17
**状态**: 已设计，待实施

## 概述

为 APP 端语法辨析详情接口补充情景对话（chats）数据的返回。后端管理端（grid-system）已完整实现 chats 的 CRUD、草稿/审核/发布工作流，本次只需在 APP 端（grid-app）将 chats 数据暴露给终端用户。

## 背景

### 已有基础设施（无需修改）

| 层 | 文件 | 说明 |
|---|------|------|
| 实体 | `GrammarComparisonChat.java` | `grammar_comparison_chat` 表映射，字段：id, groupId, role, content, exampleSentenceId, chatOrder, status |
| 仓库 | `GrammarComparisonChatRepository.java` | 按 groupId 查询 |
| DTO | `GrammarComparisonChatDto.java` | 字段：id, role, content, pinyin, translations, audioId, exampleSentenceId, order, aiGeneratedFields |
| VO(后台) | `GrammarComparisonChatVO.java` | 后台管理端使用的 VO |
| Request | `GrammarComparisonGroupCreateRequest.GrammarChatRequest` | role, content, pinyin, translations, audioId, order, aiGeneratedFields |
| Service | `GrammarComparisonGroupServiceImpl.java` | syncChats 发布时同步、loadChats 查询时加载例句数据 |
| Wrapper(后台) | `GrammarComparisonGroupWrapper.java` | Request↔DTO↔VO 转换 |
| Controller(后台) | `GrammarComparisonController.java` | 完整 CRUD |
| SQL | `biz_grammer_comparison.sql` | `grammar_comparison_chat` 表已定义 |

### 数据模型

chat 表通过 `example_sentence_id` 关联 `example_sentence` 表。发布时 `syncChats()` 创建 example_sentence 记录并回填 ID，查询时 `loadChats()` 通过 example_sentence 批量加载 pinyin、translations、audioId 到 DTO。这与词汇辨析（`vocab_comparison_chat`）的设计一致。

### 参照模式

词汇辨析 APP 端已实现 chats 返回（`AppVocabComparisonController.getDetail()` + `AppVocabComparisonWrapper.toAppChatVO()`），本次设计保持与其一致。

## 修改文件

共 3 个文件：

### 1. `grid-app/.../vo/AppGrammarPointDetailVO.java`

**新增内部类 `ComparisonChatVO`**：

```java
@Getter
@Setter
public static class ComparisonChatVO implements Serializable {
    @ApiModelProperty(value = "角色: teacher=老师, student=学生")
    private String role;

    @ApiModelProperty(value = "中文对话内容")
    private String content;

    @ApiModelProperty(value = "拼音")
    private String pinyin;

    @ApiModelProperty(value = "外文翻译（按语言筛选后的单条）")
    private TextTranslationVO translation;

    @ApiModelProperty(value = "音频")
    private AudioVO audio;

    @ApiModelProperty(value = "排序")
    private Integer order;
}
```

**在 `GrammarComparisonVO` 中新增字段**：

```java
@ApiModelProperty(value = "情景对话列表")
private List<ComparisonChatVO> chats;
```

### 2. `grid-app/.../wrapper/AppGrammarPointWrapper.java`

**新增方法**：

- `toComparisonChatVOList(List<GrammarComparisonChatDto>, Map<Long, AudioResourceDto>, String language)` — 批量转换
- `toComparisonChatVO(GrammarComparisonChatDto, Map<Long, AudioResourceDto>, String language)` — 单条转换：
  - `role`、`content`、`order` 直接映射
  - `pinyin` 直接映射
  - `translation` 通过 `filterByLanguage(dto.getTranslations(), language)` 语言筛选
  - `audio` 从 `audioMap` 中按 `dto.getAudioId()` 查找，构造 `AudioVO(audioUrl)`

**修改 `toComparisonVO()`**：新增 `audioMap` 参数，调用 `toComparisonChatVOList()` 设置 `vo.setChats()`。

### 3. `grid-app/.../rest/AppGrammarPointController.java`

**修改 `getDetail()` 方法**：

在现有流程中插入 chat 音频资源的收集：

```java
// 现有：collectComparisonSentences 收集 items 的 usageSentenceId
// 新增：收集 chats 的 audioId
List<Long> chatAudioIds = collectChatAudioIds(comparisons);
audioMap = mergeAudioMap(audioMap, chatAudioIds);
```

**新增方法 `collectChatAudioIds()`**：遍历 comparison groups 的 chats，收集非空 `audioId`。

**修改 `AppGrammarPointWrapper.toDetailVO()` 调用**：传入 audioMap 给 toComparisonVO（当前已传入 audioMap，无需改签名，只需在 wrapper 内部使用）。

## 数据流

```
用户请求 GET /api/app/grammar/{id}?language=en
  → GrammarPointService.findPublishedById(id)
  → GrammarComparisonGroupService.searchByGrammarId(id)
    → 只返回已发布组
    → loadChats() 从 grammar_comparison_chat 表 + example_sentence 表
    → DTO 中已含 pinyin, translations, audioId
  → collectChatAudioIds(comparisons)  // 新增
  → 批量查询 AudioResourceService.findByIds()
  → mergeAudioMap()
  → AppGrammarPointWrapper.toDetailVO(..., audioMap, ...)
    → toComparisonVO(..., audioMap, ...)
      → toComparisonChatVOList(dto.getChats(), audioMap, language)  // 新增
  → 返回 AppGrammarPointDetailVO（含 comparisons[].chats）
```

## 与词汇辨析对比

| 项目 | 词汇辨析 AppChatVO | 语法辨析 ComparisonChatVO |
|------|-------------------|--------------------------|
| role | ✅ | ✅ |
| content | ✅ | ✅ |
| pinyin | ✅ | ✅ |
| translations | `List<TextTranslationVO>` | 单条 `TextTranslationVO`（语言筛选） |
| audio | `String audioUrl` | `AudioVO { audioUrl }`（VO包装） |
| order | ✅ | ✅ |

差异原因：语法模块统一使用 `AudioVO`/`ImageVO` 包装资源 URL，且翻译字段统一走语言筛选（与语法详情其他部分一致）。

## 不涉及

- 后台管理端（grid-system）：已完整实现，无需修改
- SQL 建表语句：`grammar_comparison_chat` 表已存在，无需修改
- 草稿/发布流程：后端 `GrammarComparisonGroupServiceImpl.syncChats()` / `loadChats()` 已实现
- AI 内容标记：后端 `collectGrammarComparisonMarkers()` 已覆盖 chat
