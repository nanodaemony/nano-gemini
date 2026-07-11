# 汉字大挑战 — 设计文档

> **版本**: v1.0
> **日期**: 2026-07-11
> **需求来源**: 《汉字大挑战-需求说明.md》v1.0
> **状态**: 设计中

---

## 一、设计决策

经需求澄清，实施范围已简化：

| 原始需求 | 最终决策 |
|---------|---------|
| 游戏次数限制（未登录1轮/试用3轮/已购无限） | 移除，全部匿名开放 |
| 错题集后端存储（user_wrong_question 表） | 移除，前端本地管理 |
| POST /result 提交结果 | 移除，前端本地判题计分 |
| GET /wrong-questions | 移除 |
| 题目数量参数 count | 固定 10 题/轮 |
| 多语言响应 | 遵循现有 App 接口模式，接收 language 参数，单语言筛选 |

**范围**: 三个匿名 GET 接口，各返回 10 道题目 + 正确选项 + 单语言解析。纯出题，零状态。

---

## 二、架构概览

遵循项目已有分层惯例（参考 `CharCharacterService` → `AppCharCharacterController` 模式）：

```
grid-system（业务层）                     grid-app（表现层）
├── service/game/                       ├── rest/
│   ├── GameCharacterService.java       │   └── AppGameController.java
│   └── impl/GameCharacterServiceImpl   ├── rest/vo/
│       .java                           │   └── AppGameQuestionVO.java
└── service/game/dto/                   └── rest/wrapper/
    └── GameQuestionDTO.java                └── AppGameWrapper.java
```

```
grid-common（枚举）
└── enums/HskLevelRange.java
```

依赖关系：grid-app → grid-system（通过 `GameCharacterService` 接口注入）

---

## 三、API 设计

### 3.1 部首识记

```
GET /api/app/character/game/radical?level={elementary|intermediate|advanced}&language={zh|en}
```

- `level`: 前端传语义 key，后端 `HskLevelRange` 映射到 HSK 等级列表
  - `elementary` → HSK 1, 2
  - `intermediate` → HSK 3, 4
  - `advanced` → HSK 5, 6
- `language`: 必填，如 `zh`、`en`，用于筛选解析内容语言

### 3.2 形近字辨析

```
GET /api/app/character/game/comparison?language={zh|en}
```

无难度参数（直接从 `char_comparison` 表取题）。

### 3.3 汉字组词

```
GET /api/app/character/game/word-formation?level={elementary|intermediate|advanced}&language={zh|en}
```

参数同部首识记。

### 3.4 权限

三个接口均为匿名访问，使用 `@AnonymousGetMapping`。

---

## 四、数据模型

### 4.1 GameQuestionDTO（grid-system 层）

```java
public class GameQuestionDTO implements Serializable {
    private String gameType;          // "radical" | "comparison" | "word_formation"
    private Integer questionIndex;    // 1-10
    private String stem;              // 题干（展示用的汉字或句子语境）
    private String character;         // 目标汉字
    private String pinyin;            // 目标汉字拼音
    private List<GameOptionDTO> options;   // 4个选项
    private String correctKey;        // 正确答案的 key "A"|"B"|"C"|"D"
    private GameExplanationDTO explanation; // 解析信息（多语言原始列表）
}
```

### 4.2 GameOptionDTO（grid-system 层）

```java
public class GameOptionDTO implements Serializable {
    private String key;               // "A" | "B" | "C" | "D"
    private String text;              // 选项文字
    private Boolean isCorrect;        // 是否正确答案
}
```

### 4.3 GameExplanationDTO（grid-system 层）

```java
public class GameExplanationDTO implements Serializable {
    // 部首游戏
    private String radical;                    // 部首
    private String radicalName;                // 部首名称
    private List<TextTranslation> meaning;     // 部首含义（多语言）

    // 形近字辨析游戏
    private String comparisonChar;             // 对比字
    private String comparisonPinyin;           // 对比字拼音
    private List<TextTranslation> comparisonDesc; // 对比说明（多语言）

    // 组词游戏
    private String correctWord;                // 正确组词
    private String correctWordPinyin;          // 正确组词拼音
    private String correctWordPos;             // 正确组词词性
    private List<TextTranslation> correctWordMeaning; // 正确组词释义（多语言）
}
```

### 4.4 AppGameQuestionVO（grid-app 层，单语言筛选后）

```java
public class AppGameQuestionVO implements Serializable {
    private String gameType;
    private Integer questionIndex;
    private String stem;
    private String character;
    private String pinyin;
    private List<GameOptionVO> options;        // 4个选项
    private String correctKey;
    private GameExplanationVO explanation;     // 单语言解析
}
```

GameOptionVO 字段同 GameOptionDTO（无翻译字段，无需筛选）。

GameExplanationVO 结构与 GameExplanationDTO 完全相同，但各 `List<TextTranslation>` 字段替换为单个 `TextTranslationVO`（由 Wrapper 通过 `filterByLanguage` 筛选）。

### 4.5 HskLevelRange（grid-common 枚举/工具类）

```java
public enum HskLevelRange {
    ELEMENTARY("elementary", Arrays.asList("1", "2")),
    INTERMEDIATE("intermediate", Arrays.asList("3", "4")),
    ADVANCED("advanced", Arrays.asList("5", "6"));

    private final String key;
    private final List<String> levels;

    public static List<String> fromKey(String key) {
        for (HskLevelRange r : values()) {
            if (r.key.equals(key)) return r.levels;
        }
        throw new IllegalArgumentException("Invalid level: " + key + ". Valid: elementary, intermediate, advanced");
    }
}
```

---

## 五、出题算法

### 5.1 部首识记

```
输入: hskLevels (如 ["1", "2"]), language
输出: 10 道部首选择题

流程:
1. 从 char_character 查 hsk_level IN hskLevels 且 status=1 且 publish_status='published'
   随机取 10 个汉字
2. 对每个汉字:
   a. 题干: 汉字本身（前端渲染 "选择'{character}'的正确部首"）
   b. 正确答案: 该字的 radical + radicalId → char_radical.radicalName
   c. 干扰项（3个）:
      - 从 char_radical 查已发布部首，排除正确答案部首
      - 随机取 3 个不同的部首作为干扰项
   d. 解析: radical + radicalName + meaning（多语言，由 char_radical.evolutionDesc 提供）
3. 每道题的 4 个选项用 Collections.shuffle 打乱顺序，打乱后按 A/B/C/D 重新分配 key
4. 正确答案的 key 记录到 correctKey
```

### 5.2 形近字辨析

```
输入: language
输出: 10 道形近字选择题

流程:
1. 从 char_comparison 查 status=1 的记录，随机取 10 条
2. 对每条 comparison:
   a. 通过 char_character 查 charId 对应的汉字和拼音
   b. 题干: 使用 char_word 表该字的 example_sentence（优先），将目标字位置替换为 ____
      - 无例句时: 用释义描述作为上下文
   c. 正确答案: char_character.character
   d. 干扰项（3个）:
      - 优先: 同一 char_id 下的其他 comparison_char（从 char_comparison 查）
      - 不足时: 同一 radical 下的其他汉字
      - 再不足: 同等级随机汉字（排除已选的 comparison_char）
   e. 解析: comparison_char + comparison_pinyin + comparisonDesc（多语言）
3. 选项打乱，分配 key
4. 同一轮内不重复使用同一 comparison id
```

### 5.3 汉字组词

```
输入: hskLevels, language
输出: 10 道组词选择题

流程:
1. 从 char_character 查 hsk_level IN hskLevels 且 status=1 且 publish_status='published'
   关联 char_word 表，筛选 word 记录数 ≥2 的汉字，随机取 10 个
2. 对每个汉字:
   a. 题干: 汉字 + 拼音（前端渲染 "'{character}'（{pinyin}）能组成以下哪个词？"）
   b. 正确答案: 从该字的 char_word 中随机取 1 条（word_item）
   c. 干扰项（3个）:
      - 从 vocab_word 查同等级已发布词汇
      - WHERE word NOT LIKE '%{character}%'（排除包含目标字的词）
      - 随机取 3 个
      - 如果同等级不足，放宽等级范围重试
   d. 解析: 正确组词的 word_item + pinyin + part_of_speech + word_item_translations
3. 选项打乱，分配 key
4. 同一轮内不重复使用同一汉字
```

### 5.4 边缘情况处理

| 场景 | 处理 |
|------|------|
| 字库不足 10 个 | 返回实际可用数量（最少 0），前端显示"暂无足够题目" |
| 干扰项不足 3 个 | 有几个用几个，options 数组长度可为 2-4 |
| char_comparison 无数据 | 返回空列表 `[]` |
| level 参数非法 | 返回 400 + 有效值提示 |

---

## 六、Repository 层新增查询

### CharCharacterRepository

```java
// 按 HSK 等级列表随机取已发布汉字
@Query(value = "SELECT * FROM char_character WHERE hsk_level IN ?1 " +
    "AND status = 1 AND publish_status = 'published' " +
    "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
List<CharCharacter> findRandomPublishedByHskLevels(List<String> levels, int limit);

// 按 HSK 等级列表随机取有 ≥2 组词的已发布汉字
@Query(value = "SELECT cc.* FROM char_character cc " +
    "INNER JOIN char_word cw ON cc.id = cw.char_id " +
    "WHERE cc.hsk_level IN ?1 AND cc.status = 1 AND cc.publish_status = 'published' " +
    "AND cw.status = 1 GROUP BY cc.id HAVING COUNT(cw.id) >= 2 " +
    "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
List<CharCharacter> findRandomPublishedWithMinWords(List<String> levels, int limit);
```

### CharComparisonRepository

```java
// 随机取已启用辨析记录
@Query(value = "SELECT * FROM char_comparison WHERE status = 1 " +
    "ORDER BY RAND() LIMIT ?1", nativeQuery = true)
List<CharComparison> findRandomEnabled(int limit);

// 按 charId 批量查所有 status=1 的 comparison（用于干扰项）
List<CharComparison> findByCharIdInAndStatus(List<Integer> charIds, Integer status);
```

### CharWordRepository

```java
// 按 charId IN 批量查已启用的组词
List<CharWord> findByCharIdInAndStatus(List<Integer> charIds, Integer status);

// 按 charId 查已启用的组词（单个）
List<CharWord> findByCharIdAndStatus(Integer charId, Integer status);
```

### CharRadicalRepository

```java
// 随机取已发布部首（排除指定 ID）
@Query(value = "SELECT * FROM char_radical WHERE status = 1 " +
    "AND publish_status = 'published' AND id NOT IN ?1 " +
    "ORDER BY RAND() LIMIT ?2", nativeQuery = true)
List<CharRadical> findRandomPublishedExcluding(List<Long> excludeIds, int limit);
```

### VocabWordRepository

```java
// 随机取已发布词汇（排除包含某字的词）
@Query(value = "SELECT * FROM vocab_word WHERE hsk_level IN ?1 " +
    "AND status = 1 AND publish_status = 'published' " +
    "AND word NOT LIKE CONCAT('%', ?2, '%') " +
    "ORDER BY RAND() LIMIT ?3", nativeQuery = true)
List<VocabWord> findRandomPublishedExcludingChar(List<String> levels, String excludeChar, int limit);
```

---

## 七、Controller 设计

```java
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/character/game")
@Api(tags = "用户：汉字游戏接口")
public class AppGameController {

    private final GameCharacterService gameCharacterService;

    @AnonymousGetMapping("/radical")
    @ApiOperation("获取部首识记题目（10题）")
    public ResponseEntity<List<AppGameQuestionVO>> getRadicalQuestions(
            @RequestParam @ApiParam("难度: elementary|intermediate|advanced")
            String level,
            @RequestParam @ApiParam("语言: zh|en")
            String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        List<String> hskLevels = HskLevelRange.fromKey(level);
        List<GameQuestionDTO> dtos = gameCharacterService.generateRadicalQuestions(hskLevels);
        return ResponseEntity.ok(AppGameWrapper.toQuestionVOList(dtos, language));
    }

    @AnonymousGetMapping("/comparison")
    @ApiOperation("获取形近字辨析题目（10题）")
    public ResponseEntity<List<AppGameQuestionVO>> getComparisonQuestions(
            @RequestParam @ApiParam("语言: zh|en")
            String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        List<GameQuestionDTO> dtos = gameCharacterService.generateComparisonQuestions();
        return ResponseEntity.ok(AppGameWrapper.toQuestionVOList(dtos, language));
    }

    @AnonymousGetMapping("/word-formation")
    @ApiOperation("获取组词游戏题目（10题）")
    public ResponseEntity<List<AppGameQuestionVO>> getWordFormationQuestions(
            @RequestParam @ApiParam("难度: elementary|intermediate|advanced")
            String level,
            @RequestParam @ApiParam("语言: zh|en")
            String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("language 参数不能为空");
        }
        List<String> hskLevels = HskLevelRange.fromKey(level);
        List<GameQuestionDTO> dtos = gameCharacterService.generateWordFormationQuestions(hskLevels);
        return ResponseEntity.ok(AppGameWrapper.toQuestionVOList(dtos, language));
    }
}
```

---

## 八、Wrapper 设计

`AppGameWrapper` 遵循现有 Wrapper 模式（参考 `AppCharCharacterWrapper`）：

- `toQuestionVOList(List<GameQuestionDTO>, String language)` — 批量转换
- `toQuestionVO(GameQuestionDTO, String language)` — 单个转换
- `filterByLanguage(List<TextTranslation>, String language)` — 复刻现有 `AppCharCharacterWrapper.filterByLanguage` 逻辑，从列表中筛选匹配语言的单个 `TextTranslationVO`

---

## 九、文件清单

| 层 | 文件 | 模块 | 类型 |
|----|------|------|------|
| Enum | `HskLevelRange.java` | grid-common | 新增 |
| DTO | `GameQuestionDTO.java`（含内嵌 `GameOptionDTO`、`GameExplanationDTO`） | grid-system | 新增 |
| Service | `GameCharacterService.java` 接口 | grid-system | 新增 |
| Service | `GameCharacterServiceImpl.java` | grid-system | 新增 |
| Repository | `CharCharacterRepository` 新增 2 方法 | grid-system | 修改 |
| Repository | `CharComparisonRepository` 新增 2 方法 | grid-system | 修改 |
| Repository | `CharWordRepository` 新增 2 方法 | grid-system | 修改 |
| Repository | `CharRadicalRepository` 新增 1 方法 | grid-system | 修改 |
| Repository | `VocabWordRepository` 新增 1 方法 | grid-system | 修改 |
| Controller | `AppGameController.java` | grid-app | 新增 |
| VO | `AppGameQuestionVO.java`（含内嵌 `GameOptionVO`、`GameExplanationVO`） | grid-app | 新增 |
| Wrapper | `AppGameWrapper.java` | grid-app | 新增 |
| 测试 | `AppGameControllerTest.java` | grid-app | 新增 |

共 **11 个新增文件，5 个 Repository 增量修改**。

---

## 十、验收对照

| 编号 | 验收项 | 实现 |
|------|--------|------|
| AC-1 | 部首识记出题 | `generateRadicalQuestions` |
| AC-2 | 形近字辨析出题 | `generateComparisonQuestions` |
| AC-3 | 组词游戏出题 | `generateWordFormationQuestions` |
| AC-4 | 即时反馈 | `correctKey` + `explanation` 返回，前端本地判题 |
| AC-5 | 一轮计分 | 前端本地计算 |
| AC-6 | 难度分层 | `HskLevelRange` 映射 |
| AC-7 | 去重 | 算法内去重 |
| AC-8 | 权限控制 | 全部匿名访问（已移除限制） |
| AC-9 | 错题回顾 | 前端本地存储 |
| AC-10 | 多语言 | `language` 参数 + Wrapper 单语言筛选 |

---

*此设计文档由脑力激荡流程生成，经用户确认后进入 writing-plans 阶段。*
