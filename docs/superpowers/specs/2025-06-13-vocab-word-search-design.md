# 词汇搜索接口设计

## 背景

后台在新增/编辑词汇的关联词（近义词、反义词、正序词等）时，需要能搜索到目标词汇，选择词汇或义项后将其关联 ID 提交到词汇的新增/更新接口进行保存。

需要一个轻量的搜索接口，仅返回关联词选择场景所需的词汇和义项信息。

## 需求

- 入参：词汇文本（精确匹配）
- 出参：匹配文本的所有已发布词汇的基本信息（词汇ID、词汇文本）及对应的已发布义项信息（义项ID、词性、中文释义、中文释义翻译）
- 仅返回已发布（`publish_status = 'published'`）的数据
- 不分页

## 接口定义

```
GET /api/vocabulary/search?word=xxx
```

- 遵循现有 `@AnonymousGetMapping` 模式
- 返回 `List<VocabWordBaseSearchVO>`，`HttpStatus.OK`
- 无匹配时返回空列表（200），非 404

## VO 设计

```java
@Getter
@Setter
public class VocabWordBaseSearchVO implements Serializable {

    @ApiModelProperty(value = "词汇ID")
    private Integer id;

    @ApiModelProperty(value = "词汇词头")
    private String word;

    @ApiModelProperty(value = "对应义项列表")
    private List<VocabSenseSearchItemVO> senses;

    @Getter
    @Setter
    public static class VocabSenseSearchItemVO implements Serializable {

        @ApiModelProperty(value = "义项ID")
        private Integer id;

        @ApiModelProperty(value = "词性")
        private String partOfSpeech;

        @ApiModelProperty(value = "中文释义")
        private String chineseDef;

        @ApiModelProperty(value = "中文释义外文翻译")
        private List<TextTranslationVO> defTranslations;
    }
}
```

## Repository 层

在 `VocabWordRepository` 和 `VocabSenseRepository` 中添加查询方法：

```java
// VocabWordRepository
List<VocabWord> findByWordAndStatus(String word, Integer status);

// VocabSenseRepository — 已有 findByWordIdAndStatus，直接复用
```

## Service 层

在 `VocabWordService` 中新增：

```java
/**
 * 根据词汇文本精确搜索已发布的词汇及其义项
 */
List<VocabWordDto> searchByWord(String word);
```

实现逻辑：
1. 调用 `vocabWordRepository.findByWordAndStatus(word, ENABLED)` 获取词汇
2. 过滤 `publish_status = 'published'`
3. 对每个匹配词汇，调用 `vocabSenseRepository.findByWordIdAndStatus(id, ENABLED)` 获取义项
4. 过滤义项（已发布词汇的所有义项均为已发布子表数据）
5. 组装 DTO 返回（DTO 复用现有 `VocabWordDto`，但 senses 中只填充搜索需要的字段）

## Wrapper/Converter 层

在 `VocabWordWrapper` 中新增 VO 转换方法：

```java
public static List<VocabWordBaseSearchVO> toSearchVOList(List<VocabWordDto> dtos);
public static VocabWordBaseSearchVO toSearchVO(VocabWordDto dto);
public static VocabWordBaseSearchVO.VocabSenseSearchItemVO toSenseSearchItemVO(VocabSenseDto dto);
```

## 边界处理

- **无匹配**：返回空列表 `[]`（200）
- **匹配到未发布词汇**：不出现在结果中
- **词汇已发布但无义项**：返回 `senses: []`，词汇本身仍返回
- **特殊字符**：由 JPA 参数化查询处理，无 SQL 注入风险

## 未涉及

- 不分页、不排序（数据量小）
- 不返回关联词、结构、例句等无关数据
- 不关心草稿内容（仅查已发布数据）
