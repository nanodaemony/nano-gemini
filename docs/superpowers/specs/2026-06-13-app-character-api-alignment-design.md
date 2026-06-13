# App 端汉字接口与后台字段对齐设计

**日期**: 2026-06-13
**状态**: 设计稿

## 背景

后台汉字管理接口 (`CharCharacterController`) 的 VO 字段近期发生变更（字段重命名 `level` → `hskLevel`、移除 `stroke`、新增 `componentCombination` 等），导致普通用户端汉字查询接口 (`AppCharCharacterController`) 响应的 `AppCharCharacterDetailVO` 字段不匹配。本文档描述将其对齐的方案。

## 变更原则

1. **字段名完全对齐** — app 端 VO 字段名与 admin 端保持一致的命名（接受 breaking change）
2. **内部字段不暴露** — 不对外透出 `createBy`、`updateBy`、`createTime`、`updateTime`、`publishStatus`、`editStatus`、`status` 等审计/状态字段
3. **资源 ID 不暴露** — app 端保持当前的资源包装模式（`AudioVO`、`ImageVO`），将 `audioId` 解析为 `audioUrl`，不暴露原始 ID
4. **翻译字段按语言过滤** — 详情接口新增必填的 `language` 参数，所有 `List<TextTranslationVO>` 过滤为单条 `TextTranslationVO`

## 变更文件清单

| 操作 | 文件 |
|---|---|
| 🟢 新增 | `grid-app/.../vo/AppCharStrokeVO.java` |
| 🔴 修改 | `grid-app/.../vo/AppCharCharacterDetailVO.java` |
| 🔴 修改 | `grid-app/.../vo/AppCharCharacterBaseVO.java` |
| 🔴 修改 | `grid-app/.../rest/AppCharCharacterController.java` |

`grid-app/.../request/AppCharCharacterSearchRequest.java` 不变。

## 详细设计

### 1. AppCharCharacterDetailVO 顶层字段

| 字段 | 变更 | 说明 |
|---|---|---|
| `id` | ✅ 保留 | 汉字ID |
| `character` | ✅ 保留 | 汉字 |
| `level` | → `hskLevel` | 对齐 admin `CharCharacterVO` |
| `pinyin` | ✅ 保留 | 拼音 |
| `audio` → `AudioVO` | ✅ 保留 | 读音音频（保持 app 包装模式） |
| `traditional` | ✅ 保留 | 繁体字 |
| `radical` | ✅ 保留 | 部首名称 |
| `stroke` | ❌ 移除 | 已从 admin 实体/VO 删除，迁移到独立笔顺接口 |
| `charDesc` | ✅ 保留 | 汉字说明 |
| `descTranslations` | → `descTranslation` | 改为单条，按 `language` 过滤 |
| `discriminations` | ✅ 保留 | 辨析列表，内部字段调整 |
| `words` | ✅ 保留 | 组词列表，内部字段调整 |
| — | ➕ `componentCombination` | 部件组合，对齐 admin |

### 2. AppCharCharacterBaseVO 搜索列表字段

| 字段 | 变更 | 说明 |
|---|---|---|
| `id` | ✅ 保留 | 汉字ID |
| `character` | ✅ 保留 | 汉字 |
| `level` | → `hskLevel` | 对齐 admin |
| `pinyin` | ✅ 保留 | 拼音 |

### 3. 辨析内部类 CharDiscriminationVO 字段

| 字段 | 变更 | 说明 |
|---|---|---|
| `id` | ❌ 移除 | 不对外透出 |
| `discrimChar` | → `comparisonChar` | 对齐 admin `CharComparisonVO` |
| `discrimPinyin` | → `comparisonPinyin` | 对齐 admin |
| `discrimCharTranslations` | → `discrimCharTranslation` | 改为单条，按 `language` 过滤 |
| `comparisonTranslations` | → `comparisonDescTranslation` | 对齐 admin 命名 + 改为单条 |
| — | ➕ `order` | 排序权重，对齐 admin |

### 4. 组词内部类 CharWordVO 字段（保持扁平）

| 字段 | 变更 | 说明 |
|---|---|---|
| `wordItem` | ✅ 保留 | 组词 |
| `level` | → `hskLevel` | 对齐 admin |
| `pinyin` | ✅ 保留 | 拼音 |
| `partOfSpeech` | ✅ 保留 | 词性 |
| `wordItemTranslations` | → `wordItemTranslation` | 改为单条，按 `language` 过滤 |
| `exampleSentence` | ✅ 保留 | 例句（扁平） |
| `examplePinyin` | ✅ 保留 | 例句拼音（扁平） |
| `exampleTranslations` | → `exampleTranslation` | 改为单条，按 `language` 过滤 |
| `exampleImage` → `ImageVO` | ✅ 保留 | 例句图片（扁平） |

### 5. 语言参数 & 翻译过滤

详情接口新增 `language` 参数（必填）：

```java
@AnonymousGetMapping("/{id}")
public ResponseEntity<AppCharCharacterDetailVO> getDetail(
    @PathVariable Integer id,
    @RequestParam String language)  // 必填，如 "en"、"ja"
```

新增工具方法 `filterByLanguage`：

```java
private TextTranslationVO filterByLanguage(List<TextTranslation> translations, String language) {
    if (translations == null || language == null) return null;
    return translations.stream()
        .filter(t -> language.equals(t.getLanguage()))
        .findFirst()
        .map(t -> {
            TextTranslationVO vo = new TextTranslationVO();
            vo.setLanguage(t.getLanguage());
            vo.setTranslation(t.getTranslation());
            return vo;
        })
        .orElse(null);
}
```

未找到对应语言时返回 `null`，不报错。

### 6. App 端笔顺查询接口

**新增文件**: `AppCharStrokeVO.java`

```java
@Getter @Setter
public class AppCharStrokeVO implements Serializable {
    private String character;                       // 汉字
    private List<String> strokes;                   // 笔顺SVG路径数据
    private List<List<List<Integer>>> medians;      // 笔顺坐标参考线
    private List<Integer> radStrokes;               // 部首笔画索引
}
```

**新增端点**:

```
GET /api/app/character/stroke/{character}
```

实现逻辑：调用 `charStrokeService.findByCharacter(character)` 获取笔顺JSON，通过 `CharStrokeWrapper.toStrokeVO()` 解析后映射到 `AppCharStrokeVO`。

### 7. 对应逻辑调整汇总

| 方法 | 调整内容 |
|---|---|
| `getDetail()` | 新增 `@RequestParam String language`；参数校验 |
| `toDetailVO()` | 新增 `componentCombination`；移除 `stroke`；所有翻译字段改为调用 `filterByLanguage()` |
| `toBaseVO()` | `vo.setLevel(...)` → `vo.setHskLevel(...)` |
| `toDiscriminationVO()` | 字段名对齐；移除 `id`；新增 `order`；翻译改为单条 |
| `toWordVO()` | `vo.setLevel(...)` → `vo.setHskLevel(...)`；翻译改为单条 |
| `findStrokeByCharacter()` | 新增笔顺查询方法 |

## 不涉及的变更

- `AppCharCharacterSearchRequest` — 搜索接口无需 `language` 参数，且搜索返回的基础信息不含翻译字段
- `radicalId` — app 端不暴露原始 ID，仅保留 `radical`（部首名称）
- `audioId` — app 端通过 `AudioResourceService` 解析为 `AudioVO.audioUrl`
- 子对象排序字段（辨析 `order`、组词 `order`）保持透出，方便用户端排序展示
