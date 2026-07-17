# 语法辨析 APP 端情景对话功能 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 APP 端语法详情接口补充情景对话（chats）数据的返回，将 `GrammarComparisonChatDto` 中的对话数据以 `ComparisonChatVO` 形式输出。

**Architecture:** 在 `AppGrammarPointDetailVO` 中新增 `ComparisonChatVO` 内部类，在 `AppGrammarPointWrapper` 中新增 chat→VO 转换方法，在 `AppGrammarPointController.getDetail()` 中新增 chat 音频资源的预加载收集。遵循现有语法模块的 `AudioVO` 包装和 `filterByLanguage` 翻译筛选模式。

**Tech Stack:** Java 8, Lombok, Spring Boot 2.7.18

## Global Constraints

- 资源（音频、图片）不暴露原始 ID，包装为含 URL 的 VO 对象返回
- 翻译字段通过 `filterByLanguage` 按请求语言筛选为单条，与语法详情其它部分一致
- 使用已有的 `AudioVO`（含 `audioUrl`）内部类包装音频，不新增冗余类型
- Wrapper 方法使用 `public static`，与项目代码规范一致
- 遵循现有命名和代码风格

---

### Task 1: 新增 ComparisonChatVO 内部类

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppGrammarPointDetailVO.java`

**Interfaces:**
- Produces: `AppGrammarPointDetailVO.ComparisonChatVO` — 供 Task 2 Wrapper 转换使用

- [ ] **Step 1: 在 GrammarComparisonVO 中添加 chats 字段**

在 `GrammarComparisonVO` 类的 `items` 字段之后，`}` 之前插入：

```java
    @ApiModelProperty(value = "情景对话列表")
    private List<ComparisonChatVO> chats;
```

- [ ] **Step 2: 新增 ComparisonChatVO 内部类**

在 `ComparisonItemVO` 类的闭合 `}` 之后（约第 198 行），`AppGrammarPointDetailVO` 顶层闭合 `}` 之前插入：

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

- [ ] **Step 3: 编译验证**

Run: `cd grid-app && mvn compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppGrammarPointDetailVO.java
git commit -m "feat: add ComparisonChatVO inner class and chats field to GrammarComparisonVO

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: 新增 Wrapper chat 转换方法

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppGrammarPointWrapper.java`

**Interfaces:**
- Consumes: `AppGrammarPointDetailVO.ComparisonChatVO` (from Task 1), `GrammarComparisonChatDto`, `Map<Long, AudioResourceDto>`, `String language`
- Produces: `toComparisonChatVOList()`, `toComparisonChatVO()` — 供 `toComparisonVO()` 内部调用

- [ ] **Step 1: 新增 toComparisonChatVOList 和 toComparisonChatVO 方法**

在 `toComparisonItemVO` 方法之后（约第 222 行，`// ===== 例句（公共） =====` 注释之前）插入：

```java
    private static List<AppGrammarPointDetailVO.ComparisonChatVO> toComparisonChatVOList(
            List<GrammarComparisonChatDto> dtos, Map<Long, AudioResourceDto> audioMap,
            String language) {
        if (dtos == null) {
            return Collections.emptyList();
        }
        return dtos.stream().map(d -> toComparisonChatVO(d, audioMap, language)).collect(Collectors.toList());
    }

    private static AppGrammarPointDetailVO.ComparisonChatVO toComparisonChatVO(
            GrammarComparisonChatDto dto, Map<Long, AudioResourceDto> audioMap,
            String language) {
        AppGrammarPointDetailVO.ComparisonChatVO vo = new AppGrammarPointDetailVO.ComparisonChatVO();
        vo.setRole(dto.getRole());
        vo.setContent(dto.getContent());
        vo.setPinyin(dto.getPinyin());
        vo.setTranslation(filterByLanguage(dto.getTranslations(), language));
        if (dto.getAudioId() != null && audioMap != null) {
            AudioResourceDto audioDto = audioMap.get(dto.getAudioId());
            if (audioDto != null) {
                AppGrammarPointDetailVO.AudioVO audioVO = new AppGrammarPointDetailVO.AudioVO();
                audioVO.setAudioUrl(audioDto.getFileUrl());
                vo.setAudio(audioVO);
            } else {
                log.error("情景对话音频资源未找到, audioId={}", dto.getAudioId());
            }
        }
        vo.setOrder(dto.getOrder());
        return vo;
    }
```

- [ ] **Step 2: 修改 toComparisonVO 方法，调用 chat 转换**

在 `toComparisonVO` 方法中，`vo.setItems(...)` 之后，`return vo;` 之前插入：

```java
        vo.setChats(toComparisonChatVOList(dto.getChats(), audioMap, language));
```

修改后的 `toComparisonVO` 方法如下：

```java
    private static AppGrammarPointDetailVO.GrammarComparisonVO toComparisonVO(
            GrammarComparisonGroupDto dto, Map<Long, AudioResourceDto> audioMap,
            Map<Long, AliOssStorageDto> imageMap, Map<Long, ExampleSentenceDto> sentenceMap,
            String language) {
        AppGrammarPointDetailVO.GrammarComparisonVO vo = new AppGrammarPointDetailVO.GrammarComparisonVO();
        vo.setId(dto.getId());
        vo.setGroupKey(dto.getGroupKey());
        vo.setItems(toComparisonItemVOList(dto.getItems(), audioMap, imageMap, sentenceMap, language));
        vo.setChats(toComparisonChatVOList(dto.getChats(), audioMap, language));
        return vo;
    }
```

- [ ] **Step 3: 编译验证**

Run: `cd grid-app && mvn compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppGrammarPointWrapper.java
git commit -m "feat: add chat-to-VO conversion methods in AppGrammarPointWrapper

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: 新增 Controller chat 音频资源预加载

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppGrammarPointController.java`

**Interfaces:**
- Consumes: `GrammarComparisonGroupDto.getChats()` (DTO 中已有 pinyin, translations, audioId), `mergeAudioMap()` (已有)
- Produces: `collectChatAudioIds()` 方法

- [ ] **Step 1: 新增 collectChatAudioIds 方法**

在 `collectSentenceImages` 方法之后（约第 221 行），`mergeAudioMap` 方法之前插入：

```java
    private List<Long> collectChatAudioIds(List<GrammarComparisonGroupDto> comparisons) {
        List<Long> audioIds = new ArrayList<>();
        if (comparisons != null) {
            for (GrammarComparisonGroupDto group : comparisons) {
                if (group.getChats() != null) {
                    for (GrammarComparisonChatDto chat : group.getChats()) {
                        if (chat.getAudioId() != null) {
                            audioIds.add(chat.getAudioId());
                        }
                    }
                }
            }
        }
        return audioIds;
    }
```

- [ ] **Step 2: 在 getDetail 方法中调用 collectChatAudioIds**

在 `getDetail()` 方法中，`imageMap = mergeImageMap(imageMap, comparisonImageIds);` 之后，`AppGrammarPointDetailVO vo = ...` 之前插入：

```java
        List<Long> chatAudioIds = collectChatAudioIds(comparisons);
        audioMap = mergeAudioMap(audioMap, chatAudioIds);
```

修改后的 `getDetail()` 方法相关片段：

```java
        // 从辨析组中收集额外的资源：usageSentenceId → ExampleSentenceDto + 音频ID
        Map<Long, ExampleSentenceDto> sentenceMap = collectComparisonSentences(comparisons);
        List<Long> comparisonAudioIds = collectSentenceAudios(sentenceMap);
        List<Long> comparisonImageIds = collectSentenceImages(sentenceMap);
        audioMap = mergeAudioMap(audioMap, comparisonAudioIds);
        imageMap = mergeImageMap(imageMap, comparisonImageIds);

        // 收集情景对话的音频资源
        List<Long> chatAudioIds = collectChatAudioIds(comparisons);
        audioMap = mergeAudioMap(audioMap, chatAudioIds);

        AppGrammarPointDetailVO vo = AppGrammarPointWrapper.toDetailVO(dto, audioMap, imageMap, sentenceMap, comparisons, language);
```

- [ ] **Step 3: 添加 import 语句**

在文件顶部的 import 区域，确认已存在 `GrammarComparisonChatDto` 的 import（该文件已在第 13 行导入）：

```java
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonChatDto;
```

此 import 已存在（用于 `collectComparisonSentences`），无需新增。

- [ ] **Step 4: 编译验证**

Run: `cd grid-app && mvn compile -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppGrammarPointController.java
git commit -m "feat: preload chat audio resources in grammar detail endpoint

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: 全量编译验证

- [ ] **Step 1: 全量编译**

Run: `cd C:/Users/nano/Desktop/nano-gemini && mvn compile -DskipTests`
Expected: BUILD SUCCESS for all modules

- [ ] **Step 2: 确认无遗漏**

Run: `git diff --name-only HEAD~3`
Expected: 只有 3 个文件被修改：
- `grid-app/.../vo/AppGrammarPointDetailVO.java`
- `grid-app/.../wrapper/AppGrammarPointWrapper.java`
- `grid-app/.../rest/AppGrammarPointController.java`
