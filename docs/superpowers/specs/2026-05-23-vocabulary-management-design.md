# 词汇管理功能设计文档

## 概述

为汉语教学网站开发词汇管理后台功能，支持词汇的新增、查询详情、搜索等功能。

## 目标

- 在 grid-system 模块中实现词汇管理功能
- 支持完整的词汇层级结构（词汇 → 义项 → 搭配 → 例句 → 练习题）
- 支持音频资源管理
- 接口暂时不需要鉴权

---

## 一、数据库表结构

### 1.1 vocab_word 表（词汇表）

```sql
CREATE TABLE `vocab_word` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '词汇唯一ID',
  `word` varchar(50) NOT NULL COMMENT '词汇（如：啊）',
  `word_traditional` varchar(50) DEFAULT NULL COMMENT '繁体词汇',
  `pinyin` varchar(100) NOT NULL COMMENT '标准拼音（含声调）',
  `audio_id` int(11) DEFAULT NULL COMMENT '词汇读音音频资源ID',
  `hsk_level` varchar(20) DEFAULT NULL COMMENT 'HSK等级',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='词汇表';
```

### 1.2 vocab_sense 表（词汇义项表）

```sql
CREATE TABLE `vocab_sense` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID, 义项ID',
  `word_id` int(11) NOT NULL COMMENT '所属词汇ID',
  `part_of_speech` varchar(50) DEFAULT NULL COMMENT '词性（名词、动词、形容词等）',
  `chinese_def` text DEFAULT NULL COMMENT '中文释义',
  `def_audio_id` int(11) DEFAULT NULL COMMENT '中文释义音频资源ID',
  `translations` json DEFAULT NULL COMMENT '外文翻译列表（语种+翻译）',
  `synonyms` text DEFAULT NULL COMMENT '近义词列表（展示用）, JSON列表格式',
  `antonyms` text DEFAULT NULL COMMENT '反义词列表（展示用）, JSON列表格式',
  `related_forward` text DEFAULT NULL COMMENT '正序关联词汇, JSON列表格式',
  `related_backward` text DEFAULT NULL COMMENT '逆序关联词汇, JSON列表格式',
  `sense_order` int(11) NOT NULL DEFAULT '0' COMMENT '义项排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_word_id` (`word_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='词汇义项表';
```

### 1.3 vocab_structure 表（词汇结构搭配表）

```sql
CREATE TABLE `vocab_structure` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID, 结构搭配ID',
  `word_id` int(11) NOT NULL COMMENT '所属词汇ID',
  `sense_id` int(11) NOT NULL COMMENT '所属义项ID',
  `pattern` varchar(255) NOT NULL COMMENT '结构搭配文案',
  `structure_order` int(11) NOT NULL DEFAULT '0' COMMENT '搭配排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sense_id` (`sense_id`),
  KEY `idx_word_id` (`word_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='词汇结构搭配表';
```

### 1.4 vocab_example 表（词汇搭配例句表）

```sql
CREATE TABLE `vocab_example` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '例句唯一ID',
  `word_id` int(11) NOT NULL COMMENT '所属词汇ID',
  `sense_id` int(11) NOT NULL COMMENT '所属义项ID',
  `structure_id` int(11) NOT NULL COMMENT '所属结构搭配ID',
  `sentence` text NOT NULL COMMENT '例句中文文案',
  `audio_id` int(11) DEFAULT NULL COMMENT '例句音频资源ID',
  `pinyin` varchar(500) DEFAULT NULL COMMENT '例句拼音',
  `translations` json DEFAULT NULL COMMENT '例句外文翻译列表',
  `example_order` int(11) NOT NULL DEFAULT '0' COMMENT '例句排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_structure_id` (`structure_id`),
  KEY `idx_sense_id` (`sense_id`),
  KEY `idx_word_id` (`word_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='词汇搭配例句表';
```

### 1.5 vocab_exercise 表（词汇练习题表）

```sql
CREATE TABLE `vocab_exercise` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '练习题目唯一ID',
  `word_id` int(11) NOT NULL COMMENT '所属词汇ID',
  `question_type` varchar(20) NOT NULL COMMENT '题目类型（选择/填空等）',
  `question_text` text NOT NULL COMMENT '练习题干描述',
  `options` json DEFAULT NULL COMMENT '选项列表（JSON），结构：[{option:A/B/C/D, text:选项文案}]',
  `answers` json DEFAULT NULL COMMENT '答案列表（可多选）',
  `exercise_order` int(11) NOT NULL DEFAULT '0' COMMENT '练习题目排序权重（大在前）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_word_id` (`word_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='词汇练习表';
```

### 1.6 audio_resource 表（音频资源表）

```sql
CREATE TABLE `audio_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` varchar(50) NOT NULL COMMENT '业务类型: VOCAB_WORD/VOCAB_SENSE/VOCAB_EXAMPLE/CHARACTER/DIALOGUE/ARTICLE',
  `text_content` text DEFAULT NULL COMMENT '音频对应的文字内容',
  `source_type` varchar(50) NOT NULL COMMENT '来源类型: TTS/UPLOADED',
  `file_url` varchar(500) NOT NULL COMMENT '音频文件地址',
  `file_format` varchar(20) DEFAULT 'mp3' COMMENT '文件格式: mp3/wav/m4a',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小(字节)',
  `tts_record_id` bigint(20) DEFAULT NULL COMMENT '关联的TTS记录ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz_type` (`biz_type`),
  KEY `idx_text_content` (`text_content`),
  KEY `idx_tts_record_id` (`tts_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='音频资源表';
```

---

## 二、模块结构

在 `grid-system` 模块中新增词汇管理功能：

```
com.naon.grid.modules.vocabulary
├── domain/              # 实体类
│   ├── VocabWord.java
│   ├── VocabSense.java
│   ├── VocabStructure.java
│   ├── VocabExample.java
│   ├── VocabExercise.java
│   └── AudioResource.java
├── repository/          # Repository 接口
│   ├── VocabWordRepository.java
│   ├── VocabSenseRepository.java
│   ├── VocabStructureRepository.java
│   ├── VocabExampleRepository.java
│   ├── VocabExerciseRepository.java
│   └── AudioResourceRepository.java
├── service/             # Service 接口
│   ├── VocabWordService.java
│   └── AudioResourceService.java
├── service/impl/        # Service 实现
│   ├── VocabWordServiceImpl.java
│   └── AudioResourceServiceImpl.java
├── service/dto/         # DTO 类
│   ├── VocabWordDto.java
│   ├── VocabWordQueryCriteria.java
│   ├── VocabSenseDto.java
│   ├── VocabStructureDto.java
│   ├── VocabExampleDto.java
│   ├── VocabExerciseDto.java
│   ├── AudioResourceDto.java
│   └── AudioResourceQueryCriteria.java
├── service/mapstruct/   # MapStruct 映射
│   ├── VocabWordMapper.java
│   └── AudioResourceMapper.java
└── rest/                # Controller
    ├── VocabWordController.java
    └── AudioResourceController.java
```

---

## 三、API 接口设计

### 3.1 词汇管理接口

#### 3.1.1 新增词汇

**接口：** `POST /api/vocabulary`

**请求体：**
```json
{
  "word": "啊",
  "wordTraditional": null,
  "pinyin": "·a",
  "audioId": null,
  "hskLevel": null,
  "senses": [
    {
      "partOfSpeech": "m.p.",
      "chineseDef": "用在句子的最后，表示不同的语气。",
      "defAudioId": null,
      "translations": [{"lang": "en", "text": "used at the end of the sentences to show different tones"}],
      "synonyms": null,
      "antonyms": null,
      "relatedForward": null,
      "relatedBackward": null,
      "senseOrder": 0,
      "structures": [
        {
          "pattern": "Exclamatory Sentence+<key>啊</key>：加强语气",
          "structureOrder": 0,
          "examples": [
            {
              "sentence": "多美的姑娘啊！",
              "audioId": null,
              "pinyin": null,
              "translations": null,
              "exampleOrder": 0
            }
          ]
        }
      ],
      "exercises": [
        {
          "questionType": "choice",
          "questionText": "请选择正确的用法",
          "options": [{"option": "A", "text": "选项A"}, {"option": "B", "text": "选项B"}],
          "answers": ["A"],
          "exerciseOrder": 0
        }
      ]
    }
  ]
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1
  }
}
```

#### 3.1.2 根据ID查询词汇详情

**接口：** `GET /api/vocabulary/{id}`

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "word": "啊",
    "wordTraditional": null,
    "pinyin": "·a",
    "audioId": null,
    "hskLevel": null,
    "createTime": "2026-05-23T10:00:00",
    "updateTime": "2026-05-23T10:00:00",
    "senses": [
      {
        "id": 1,
        "wordId": 1,
        "partOfSpeech": "m.p.",
        "chineseDef": "用在句子的最后，表示不同的语气。",
        "defAudioId": null,
        "translations": [{"lang": "en", "text": "used at the end of the sentences to show different tones"}],
        "synonyms": null,
        "antonyms": null,
        "relatedForward": null,
        "relatedBackward": null,
        "senseOrder": 0,
        "structures": [
          {
            "id": 1,
            "wordId": 1,
            "senseId": 1,
            "pattern": "Exclamatory Sentence+<key>啊</key>：加强语气",
            "structureOrder": 0,
            "examples": [
              {
                "id": 1,
                "wordId": 1,
                "senseId": 1,
                "structureId": 1,
                "sentence": "多美的姑娘啊！",
                "audioId": null,
                "pinyin": null,
                "translations": null,
                "exampleOrder": 0
              }
            ]
          }
        ],
        "exercises": [
          {
            "id": 1,
            "wordId": 1,
            "questionType": "choice",
            "questionText": "请选择正确的用法",
            "options": [{"option": "A", "text": "选项A"}, {"option": "B", "text": "选项B"}],
            "answers": ["A"],
            "exerciseOrder": 0
          }
        ]
      }
    ]
  }
}
```

#### 3.1.3 分页查询词汇列表

**接口：** `GET /api/vocabulary?word=啊&page=0&size=30&sort=createTime,desc`

**响应：**
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "word": "啊",
        "wordTraditional": null,
        "pinyin": "·a",
        "audioId": null,
        "hskLevel": null,
        "createTime": "2026-05-23T10:00:00",
        "updateTime": "2026-05-23T10:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 30,
    "number": 0
  }
}
```

### 3.2 音频资源管理接口

#### 3.2.1 根据ID查询音频资源详情

**接口：** `GET /api/audio-resource/{id}`

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "bizType": "VOCAB_WORD",
    "textContent": "啊",
    "sourceType": "TTS",
    "fileUrl": "https://oss.example.com/audio/1.mp3",
    "fileFormat": "mp3",
    "fileSize": 10240,
    "ttsRecordId": 1,
    "createTime": "2026-05-23T10:00:00",
    "updateTime": "2026-05-23T10:00:00"
  }
}
```

#### 3.2.2 分页查询音频资源列表

**接口：** `GET /api/audio-resource?page=0&size=30&sort=createTime,desc`

**响应：**
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "bizType": "VOCAB_WORD",
        "textContent": "啊",
        "sourceType": "TTS",
        "fileUrl": "https://oss.example.com/audio/1.mp3",
        "fileFormat": "mp3",
        "fileSize": 10240,
        "ttsRecordId": 1,
        "createTime": "2026-05-23T10:00:00",
        "updateTime": "2026-05-23T10:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 30,
    "number": 0
  }
}
```

---

## 四、实现要点

1. **接口鉴权**：使用 `@AnonymousAccess` 注解，所有接口暂时不需要鉴权
2. **分页支持**：默认每页30条，支持排序
3. **搜索支持**：按词汇本身模糊匹配
4. **层级数据保存**：支持不完整的数据保存，义项、搭配、例句、练习题都可以为空
5. **MapStruct**：使用 MapStruct 进行 Entity 和 DTO 的转换
6. **事务处理**：新增词汇时使用 `@Transactional` 确保数据一致性

---

## 五、后续扩展

- grid-app 模块的用户词汇功能（本期暂不实现）
- 音频上传功能（本期暂不实现）
- 词汇的编辑和删除功能（本期暂不实现）
