# 多语言翻译字段 JSON 序列化设计

## 一、概述

当前系统中，汉字和词汇相关的多语言翻译字段以 JSON 字符串形式存储在数据库中，直接传递给前端。本次改造将在 Service 层进行 JSON 序列化/反序列化处理，使得前端能直接使用结构化的 `List<TextTranslation>` 对象。

## 二、背景

### 2.1 当前问题

- 前端接收的翻译字段是 JSON 字符串，需要自行解析
- 创建/更新接口也需要前端传递 JSON 字符串，使用不便
- 没有类型安全，容易出现 JSON 格式错误

### 2.2 目标

- 前端接收结构化的 `List<TextTranslation>` 对象
- 前端创建/更新时直接传递 `List<TextTranslation>` 结构
- 在 Service 层统一处理 JSON 转换逻辑
- Entity 层保持 String 类型不变，Repository 层无需改动

## 三、涉及的表和字段

### 3.1 汉字模块 (Character)

| 表名 | 字段 | 类型 | 说明 |
|------|------|------|------|
| char_character | desc_translations | TEXT | 汉字说明翻译 |
| char_discrimination | discrim_char_translations | TEXT | 辨析汉字翻译 |
| char_discrimination | comparison_translations | TEXT | 对比辨析翻译 |
| char_word | word_item_translations | TEXT | 组词翻译 |
| char_word | example_translations | TEXT | 例句翻译 |

### 3.2 词汇模块 (Vocabulary)

| 表名 | 字段 | 类型 | 说明 |
|------|------|------|------|
| vocab_sense | translations | JSON | 词汇翻译 |
| vocab_example | translations | JSON | 例句翻译 |

## 四、架构设计

```
┌─────────────────────────────────────────────────────────┐
│                      Controller 层                       │
│  Request/VO 使用 List<TextTranslation> 类型              │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                      Service 层                          │
│         JSON 转换在这一层处理                            │
│  - 读操作: Entity (String) → DTO (List<TextTranslation>)│
│  - 写操作: DTO (List<TextTranslation>) → Entity (String)│
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    Repository 层                         │
│         Entity 使用 String 类型（保持不变）              │
└─────────────────────────────────────────────────────────┘
```

## 五、实现方案

### 5.1 创建 JSON 工具类 (grid-common)

在 `grid-common/src/main/java/com/naon/grid/utils/` 下创建 `JsonUtils.java`：

```java
package com.naon.grid.utils;

import com.alibaba.fastjson2.JSON;
import com.naon.grid.backend.domain.common.TextTranslation;

import java.util.List;

public class JsonUtils {

    private JsonUtils() {
    }

    /**
     * 将 TextTranslation 列表序列化为 JSON 字符串
     *
     * @param list 翻译列表
     * @return JSON 字符串，null 或空列表返回 null
     */
    public static String toTranslationJson(List<TextTranslation> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(list);
    }

    /**
     * 将 JSON 字符串反序列化为 TextTranslation 列表
     *
     * @param json JSON 字符串
     * @return 翻译列表，null 或空白字符串返回 null
     */
    public static List<TextTranslation> parseTranslationList(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        return JSON.parseArray(json, TextTranslation.class);
    }
}
```

### 5.2 修改 Request 类

#### 5.2.1 CharCharacterCreateRequest

文件路径: `grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java`

修改翻译字段类型：
- `descTranslations`: `String` → `List<TextTranslation>`
- `CharDiscriminationRequest.discrimCharTranslations`: `String` → `List<TextTranslation>`
- `CharDiscriminationRequest.comparisonTranslations`: `String` → `List<TextTranslation>`
- `CharWordRequest.wordItemTranslations`: `String` → `List<TextTranslation>`
- `CharWordRequest.exampleTranslations`: `String` → `List<TextTranslation>`

#### 5.2.2 VocabWordCreateRequest

文件路径: `grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java`

修改翻译字段类型：
- `VocabSenseRequest.translations`: `String` → `List<TextTranslation>`
- `VocabExampleRequest.translations`: `String` → `List<TextTranslation>`

### 5.3 修改 VO 类

#### 5.3.1 CharCharacter 相关 VO

文件路径:
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java`
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java`

修改翻译字段类型为 `List<TextTranslation>`

#### 5.3.2 VocabWord 相关 VO

文件路径:
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java`
- `grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java`

修改翻译字段类型为 `List<TextTranslation>`

### 5.4 修改 DTO 类

#### 5.4.1 CharCharacter 相关 DTO

文件路径:
- `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java`
- `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java`
- `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java`

修改翻译字段类型为 `List<TextTranslation>`

#### 5.4.2 VocabWord 相关 DTO

文件路径:
- `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDto.java`
- `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java`
- `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExampleDto.java`

修改翻译字段类型为 `List<TextTranslation>`

### 5.5 修改 MapStruct Mapper

#### 5.5.1 CharCharacterMapper

文件路径: `grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java`

添加自定义转换方法：

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CharCharacterMapper extends BaseMapper<CharCharacterDto, CharCharacter> {

    default String toTranslationJson(List<TextTranslation> list) {
        return JsonUtils.toTranslationJson(list);
    }

    default List<TextTranslation> toTranslationList(String json) {
        return JsonUtils.parseTranslationList(json);
    }
}
```

#### 5.5.2 Vocabulary 相关 Mapper

同样为 Vocabulary 模块的 Mapper 添加自定义转换方法（如果有的话）。

### 5.6 修改 Service 实现类

#### 5.6.1 CharCharacterServiceImpl

文件路径: `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java`

需要修改的方法：

1. **findById** - 在转换 Entity → DTO 时调用 `JsonUtils.parseTranslationList()`
2. **queryAll** - 在转换 Entity → DTO 时调用 `JsonUtils.parseTranslationList()`
3. **create** - 在转换 DTO → Entity 时调用 `JsonUtils.toTranslationJson()`
4. **update** - 在转换 DTO → Entity 时调用 `JsonUtils.toTranslationJson()`

手动转换方法需要更新：
- `convertToDiscriminationDto()`
- `convertToWordDto()`
- `updateDiscrimination()`
- `updateWord()`
- `convertToDiscriminationEntity()`
- `convertToWordEntity()`

#### 5.6.2 VocabWordServiceImpl

文件路径: `grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java`

同样进行类似的修改，在手动转换方法中添加 JSON 转换逻辑。

### 5.7 Controller 层调整

Controller 层的手动映射代码保持逻辑不变，只是字段类型从 String 变为 `List<TextTranslation>`，由于赋值操作兼容，无需大改。

## 六、涉及的文件清单

### 6.1 新增文件

| 文件路径 | 说明 |
|----------|------|
| grid-common/src/main/java/com/naon/grid/utils/JsonUtils.java | JSON 工具类 |

### 6.2 修改文件 - Request 层

| 文件路径 |
|----------|
| grid-system/src/main/java/com/naon/grid/backend/rest/request/CharCharacterCreateRequest.java |
| grid-system/src/main/java/com/naon/grid/backend/rest/request/VocabWordCreateRequest.java |

### 6.3 修改文件 - VO 层

| 文件路径 |
|----------|
| grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterVO.java |
| grid-system/src/main/java/com/naon/grid/backend/rest/vo/CharCharacterBaseVO.java |
| grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordVO.java |
| grid-system/src/main/java/com/naon/grid/backend/rest/vo/VocabWordBaseVO.java |

### 6.4 修改文件 - DTO 层

| 文件路径 |
|----------|
| grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterDto.java |
| grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharDiscriminationDto.java |
| grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharWordDto.java |
| grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabWordDto.java |
| grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabSenseDto.java |
| grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/dto/VocabExampleDto.java |

### 6.5 修改文件 - Mapper 层

| 文件路径 |
|----------|
| grid-system/src/main/java/com/naon/grid/backend/service/character/mapstruct/CharCharacterMapper.java |
| (Vocabulary 模块 Mapper 如果存在也需要修改) |

### 6.6 修改文件 - Service 层

| 文件路径 |
|----------|
| grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java |
| grid-system/src/main/java/com/naon/grid/backend/service/vocabulary/impl/VocabWordServiceImpl.java |

### 6.7 不修改文件

- Entity 类（保持 String 类型）
- Repository 接口
- Controller 类（只需重新编译，逻辑基本不变）
- 数据库表结构

## 七、前后端数据格式对比

### 7.1 查询接口响应

**旧格式:**
```json
{
  "descTranslations": "[{\"language\":\"en\",\"translation\":\"Character meaning\"}]"
}
```

**新格式:**
```json
{
  "descTranslations": [
    {"language": "en", "translation": "Character meaning"}
  ]
}
```

### 7.2 创建/更新接口请求

**旧格式:**
```json
{
  "descTranslations": "[{\"language\":\"en\",\"translation\":\"Character meaning\"}]"
}
```

**新格式:**
```json
{
  "descTranslations": [
    {"language": "en", "translation": "Character meaning"}
  ]
}
```

## 八、注意事项

1. **LanguageCodeEnum 的使用**: 翻译中的 language 字段应使用 `LanguageCodeEnum` 中定义的 code 值（如 "en", "mys" 等）

2. **null 值处理**: JsonUtils 中已处理 null 和 empty 情况，空列表会序列化为 null，反序列化 null 会返回 null

3. **fastjson2**: 使用项目中已有的 fastjson2 库进行 JSON 处理

4. **向后兼容性**: 数据库存储格式不变，已有数据能正常读取

5. **Swagger 文档**: Request/VO 类修改后，Swagger 文档会自动更新
