# 用户端汉字 API 设计文档

## 1. 概述

本文档描述为普通用户提供的汉字查询接口设计，这些接口基于现有的后台汉字管理功能，但进行了简化和裁剪，仅提供查询功能。

## 2. 背景与目标

### 2.1 背景
- 后台已有完整的汉字管理功能（`CharCharacterController`）
- 需要为普通用户提供只读的汉字查询接口
- 用户端接口不需要认证（开发阶段）

### 2.2 目标
- 提供汉字搜索功能（仅匹配汉字字段）
- 提供汉字详情查询功能
- 返回数据不包含创建时间、更新时间等审计字段
- 遵循项目现有的代码规范和架构

## 3. 方案选型

### 3.1 可选方案

**方案一：直接复用后台 Service（已选）**
- 优点：代码复用度高，维护简单，实现快速
- 缺点：grid-app 依赖 grid-system（已存在依赖关系）

**方案二：在 grid-app 中独立实现**
- 优点：完全解耦，灵活定制
- 缺点：代码重复，维护成本高

### 3.2 最终选择
**方案一**，因为 grid-app 本来就依赖 grid-system，复用现有 Service 可以大大减少开发和维护成本。

## 4. 详细设计

### 4.1 整体架构

```
AppCharCharacterController (grid-app)
        ↓
CharCharacterService (grid-system)
        ↓
CharCharacterRepository (grid-system)
```

### 4.2 接口定义

#### 4.2.1 搜索汉字接口

- **路径**：`GET /api/app/character/search`
- **方法**：GET
- **权限**：允许匿名访问
- **描述**：根据汉字模糊搜索，返回所有匹配结果（不分页）

**请求参数**：
| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| blurry | String | 否 | 汉字模糊查询关键词 |

**响应示例**：
```json
[
  {
    "id": 1,
    "sequenceNo": 1,
    "character": "你",
    "level": "1",
    "pinyin": "nǐ",
    "audioId": 1001,
    "traditional": "你",
    "radical": "亻",
    "stroke": "撇、竖、撇、横钩、竖钩、撇、点",
    "charDesc": "表示第二人称",
    "descTranslations": [
      {
        "language": "en",
        "translation": "you"
      }
    ]
  }
]
```

#### 4.2.2 汉字详情接口

- **路径**：`GET /api/app/character/{id}`
- **方法**：GET
- **权限**：允许匿名访问
- **描述**：根据汉字ID查询完整详情

**路径参数**：
| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| id | Integer | 是 | 汉字唯一ID |

**响应示例**：
```json
{
  "id": 1,
  "sequenceNo": 1,
  "character": "你",
  "level": "1",
  "pinyin": "nǐ",
  "audioId": 1001,
  "traditional": "你",
  "radical": "亻",
  "stroke": "撇、竖、撇、横钩、竖钩、撇、点",
  "charDesc": "表示第二人称",
  "descTranslations": [
    {
      "language": "en",
      "translation": "you"
    }
  ],
  "discriminations": [
    {
      "id": 10,
      "charId": 1,
      "discrimChar": "您",
      "discrimPinyin": "nín",
      "discrimCharTranslations": [
        {
          "language": "en",
          "translation": "you (polite)"
        }
      ],
      "comparisonTranslations": [
        {
          "language": "en",
          "translation": "\"您\" is more polite than \"你\""
        }
      ]
    }
  ],
  "words": [
    {
      "id": 20,
      "charId": 1,
      "wordItem": "你们",
      "level": "1",
      "pinyin": "nǐ men",
      "partOfSpeech": "pronoun",
      "wordItemTranslations": [
        {
          "language": "en",
          "translation": "you (plural)"
        }
      ],
      "exampleSentence": "你们好！",
      "examplePinyin": "nǐ men hǎo",
      "exampleTranslations": [
        {
          "language": "en",
          "translation": "Hello everyone!"
        }
      ],
      "exampleImage": "https://example.com/image.jpg"
    }
  ]
}
```

### 4.3 文件清单

#### 4.3.1 新增文件（grid-app 模块）

| 文件路径 | 描述 |
|----------|------|
| `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCharCharacterController.java` | 用户端汉字接口控制器 |
| `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AppCharCharacterSearchRequest.java` | 搜索请求类 |
| `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterBaseVO.java` | 汉字列表项VO |
| `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppCharCharacterDetailVO.java` | 汉字详情VO |

#### 4.3.2 修改文件（grid-system 模块）

| 文件路径 | 描述 |
|----------|------|
| `grid-system/src/main/java/com/naon/grid/backend/service/character/CharCharacterService.java` | 新增仅匹配汉字的查询方法 |
| `grid-system/src/main/java/com/naon/grid/backend/service/character/impl/CharCharacterServiceImpl.java` | 实现新的查询方法 |
| `grid-system/src/main/java/com/naon/grid/backend/service/character/dto/CharCharacterQueryCriteria.java` | 新增字段控制查询行为 |

### 4.4 VO 字段设计

#### AppCharCharacterBaseVO
- `id` - 汉字唯一ID
- `sequenceNo` - Excel中的序号
- `character` - 汉字
- `level` - HSK等级
- `pinyin` - 拼音
- `audioId` - 读音音频资源ID
- `traditional` - 繁体字
- `radical` - 部首
- `stroke` - 笔顺
- `charDesc` - 汉字说明
- `descTranslations` - 汉字说明的多语种翻译

#### AppCharCharacterDetailVO
- 包含 BaseVO 的所有字段
- `discriminations` - 辨析列表
- `words` - 组词列表

#### AppCharCharacterDetailVO.CharDiscriminationVO
- `id` - 辨析唯一ID
- `charId` - 汉字ID
- `discrimChar` - 辨析汉字
- `discrimPinyin` - 辨析拼音
- `discrimCharTranslations` - 辨析汉字翻译
- `comparisonTranslations` - 对比翻译

#### AppCharCharacterDetailVO.CharWordVO
- `id` - 组词唯一ID
- `charId` - 汉字ID
- `wordItem` - 组词
- `level` - HSK等级
- `pinyin` - 拼音
- `partOfSpeech` - 词性
- `wordItemTranslations` - 组词翻译
- `exampleSentence` - 例句
- `examplePinyin` - 例句拼音
- `exampleTranslations` - 例句翻译
- `exampleImage` - 例句图片

## 5. 实现要点

### 5.1 Service 层扩展
- 在 `CharCharacterService` 中新增方法 `List<CharCharacterDto> searchByCharacter(String blurry)`
- 实现类中使用 JPA Criteria API 构建查询，仅模糊匹配 `character` 字段
- 过滤条件：`status = 1`（已启用）

### 5.2 Controller 层实现
- 使用 `@AnonymousGetMapping` 注解允许匿名访问
- 注入 `CharCharacterService`
- 调用 Service 方法后，将 DTO 转换为用户端 VO
- VO 转换时省略 createTime、updateTime、createBy、updateBy 等字段

### 5.3 复用组件
- 复用 `TextTranslationVO`（来自 grid-system）
- 复用 `CharCharacterService` 的 `findById` 方法
- 复用 JPA Repository 层

## 6. 安全考虑

- 开发阶段：所有接口允许匿名访问
- 未来：可以在 `AppSecurityConfig` 中配置相应的权限规则

## 7. 注意事项

- 用户端接口仅提供查询功能，不提供增删改
- 所有返回数据必须过滤掉已禁用（status = 0）的记录
- VO 中不包含任何审计字段（创建时间、更新时间、创建人、更新人）
