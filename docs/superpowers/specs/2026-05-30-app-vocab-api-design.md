# 用户端词汇API设计文档

## 概述
为普通用户提供词汇查询接口，包括搜索和详情查询两个接口。

## 背景
后台已有 `VocabWordController` 提供词汇的CRUD操作，现在需要为用户端提供简化的只读接口。

## 需求

### 功能需求
1. **搜索接口**：根据关键词匹配词汇（仅匹配word字段），返回词汇基础信息列表
2. **详情接口**：根据词汇ID查询词汇完整信息，包含义项、搭配、例句、练习题

### 非功能需求
1. 不暴露审计字段（createTime、updateTime、createBy、updateBy）
2. 音频资源需包装成URL返回，不直接返回ID
3. 允许匿名访问（开发阶段暂不鉴权）
4. 性能优化：批量查询音频资源，避免循环查库

## 设计方案

### 架构
- 复用后台 `VocabWordService` 查询数据
- 在 `grid-app` 模块新建用户端 Controller、Request、VO
- 扩展 `AudioResourceService` 支持批量查询

### 接口设计

#### 1. 搜索词汇
- **路径**：`GET /api/app/vocab/search`
- **请求参数**：
  ```
  blurry: String  // 搜索关键词
  ```
- **响应**：`List<AppVocabWordBaseVO>`

#### 2. 词汇详情
- **路径**：`GET /api/app/vocab/{id}`
- **路径参数**：
  ```
  id: Integer  // 词汇ID
  ```
- **响应**：`AppVocabWordDetailVO`

### 文件结构

#### grid-app 模块新增

```
grid-app/src/main/java/com/naon/grid/modules/app/
├── rest/
│   ├── AppVocabWordController.java
│   ├── request/
│   │   └── AppVocabWordSearchRequest.java
│   └── vo/
│       ├── AppVocabWordBaseVO.java
│       └── AppVocabWordDetailVO.java
```

#### grid-system 模块扩展

```
grid-system/src/main/java/com/naon/grid/backend/
├── service/resource/
│   └── AudioResourceService.java  // 新增 findByIds 方法
├── repo/resource/
│   └── AudioResourceRepository.java  // 新增批量查询方法
└── service/resource/impl/
    └── AudioResourceServiceImpl.java  // 实现批量查询
```

### VO 详细设计

#### AppVocabWordBaseVO（搜索列表用）
```java
- id: Integer
- word: String
- wordTraditional: String
- pinyin: String
- hskLevel: String
```

#### AppVocabWordDetailVO（详情用）
```java
- id: Integer
- word: String
- wordTraditional: String
- pinyin: String
- audio: AudioVO?
- hskLevel: String
- senses: List<VocabSenseVO>
- exercises: List<VocabExerciseVO>

// 内部类
AudioVO:
  - audioUrl: String

VocabSenseVO:
  - id: Integer
  - partOfSpeech: String?
  - chineseDef: String?
  - defAudio: AudioVO?
  - translations: List<TextTranslationVO>
  - synonyms: List<SynonymVO>
  - antonyms: List<AntonymVO>
  - relatedForward: List<RelatedWordVO>
  - relatedBackward: List<RelatedWordVO>
  - senseOrder: Integer
  - structures: List<VocabStructureVO>

SynonymVO:
  - content: String

AntonymVO:
  - content: String

RelatedWordVO:
  - content: String

VocabStructureVO:
  - id: Integer
  - pattern: String
  - structureOrder: Integer
  - examples: List<VocabExampleVO>

VocabExampleVO:
  - id: Integer
  - sentence: String
  - audio: AudioVO?
  - pinyin: String?
  - translations: List<TextTranslationVO>
  - exampleOrder: Integer

VocabExerciseVO:
  - id: Integer
  - questionType: String
  - questionText: String
  - options: List<ExerciseOptionVO>
  - answers: List<String>
  - exerciseOrder: Integer
```

### 扩展 AudioResourceService

#### AudioResourceService 新增方法
```java
List<AudioResourceDto> findByIds(List<Long> ids);
```

#### AudioResourceRepository 新增方法
```java
List<AudioResource> findByIdInAndStatus(List<Long> ids, Integer status);
```

### 实现要点
1. 先收集所有音频ID（词汇音频、义项释义音频、例句音频），批量查询后转成 Map<id, AudioVO>
2. DTO 转 VO 时从 Map 获取对应的音频信息
3. 使用 `@AnonymousGetMapping` 注解允许匿名访问
4. 复用 `TextTranslationVO` 和 `ExerciseOptionVO`（这两个已经是干净的VO，不含审计字段）

## 风险与注意事项
1. `AudioResourceService.findById` 会在找不到时抛异常，批量查询时应过滤掉不存在的ID
2. 练习题的 options 字段复用已有的 `ExerciseOptionVO`
