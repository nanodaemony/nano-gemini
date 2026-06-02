# 汉字管理接口测试 JSON 示例

## 新增/更新汉字 JSON

```json
{
  "sequenceNo": 1,
  "character": "好",
  "level": "1",
  "pinyin": "hǎo",
  "audioId": 1001,
  "traditional": "好",
  "radical": "女",
  "stroke": "撇点、撇、横、横撇/横钩、竖钩、横",
  "charDesc": "表示优点多的、使人满意的",
  "descTranslations": [
    {
      "language": "en",
      "translation": "good; nice; fine"
    },
    {
      "language": "ja",
      "translation": "良い; すばらしい"
    }
  ],
  "discriminations": [
    {
      "id": null,
      "discrimChar": "号",
      "discrimPinyin": "hào",
      "discrimCharTranslations": [
        {
          "language": "en",
          "translation": "number; how many"
        }
      ],
      "comparisonTranslations": [
        {
          "language": "en",
          "translation": "“好” means good; “号” means number"
        }
      ]
    },
    {
      "id": null,
      "discrimChar": "仔",
      "discrimPinyin": "zǐ",
      "discrimCharTranslations": [
        {
          "language": "en",
          "translation": "child; son"
        }
      ],
      "comparisonTranslations": [
        {
          "language": "en",
          "translation": "“好” means good; “仔” means child"
        }
      ]
    }
  ],
  "words": [
    {
      "id": null,
      "wordItem": "好人",
      "level": "1",
      "pinyin": "hǎo rén",
      "partOfSpeech": "noun",
      "wordItemTranslations": [
        {
          "language": "en",
          "translation": "good person"
        },
        {
          "language": "ja",
          "translation": "善人"
        }
      ],
      "exampleSentence": "他是一个好人。",
      "examplePinyin": "tā shì yī gè hǎo rén",
      "exampleTranslations": [
        {
          "language": "en",
          "translation": "He is a good person."
        }
      ],
      "exampleImage": "2001"
    },
    {
      "id": null,
      "wordItem": "好看",
      "level": "2",
      "pinyin": "hǎo kàn",
      "partOfSpeech": "adj",
      "wordItemTranslations": [
        {
          "language": "en",
          "translation": "good-looking; nice"
        }
      ],
      "exampleSentence": "这本书很好看。",
      "examplePinyin": "zhè běn shū hěn hǎo kàn",
      "exampleTranslations": [
        {
          "language": "en",
          "translation": "This book is very nice."
        }
      ],
      "exampleImage": "2002"
    }
  ]
}
```

---

## 字段说明

### 主表字段
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sequenceNo | Integer | 否 | Excel中的序号 |
| character | String | 是 | 汉字 |
| level | String | 否 | HSK等级（"1"-"9"） |
| pinyin | String | 是 | 拼音 |
| audioId | Long | 否 | 读音音频资源ID |
| traditional | String | 否 | 繁体字 |
| radical | String | 否 | 部首 |
| stroke | String | 否 | 笔画 |
| charDesc | String | 否 | 汉字说明 |
| descTranslations | Array | 否 | 说明翻译列表 |
| discriminations | Array | 否 | 辨析列表 |
| words | Array | 否 | 组词列表 |

### 说明翻译 (descTranslations)
| 字段 | 类型 | 说明 |
|------|------|------|
| language | String | 语种（如 "en", "ja", "ko"） |
| translation | String | 翻译文案 |

### 辨析 (discriminations)
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 否 | 辨析ID（新增时不传，更新时传） |
| discrimChar | String | 是 | 辨析汉字 |
| discrimPinyin | String | 否 | 辨析拼音 |
| discrimCharTranslations | Array | 否 | 辨析汉字翻译列表 |
| comparisonTranslations | Array | 否 | 对比翻译列表 |

### 组词 (words)
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 否 | 组词ID（新增时不传，更新时传） |
| wordItem | String | 是 | 组词 |
| level | String | 否 | HSK等级（"1"-"9"） |
| pinyin | String | 否 | 拼音 |
| partOfSpeech | String | 否 | 词性（如 "noun", "verb", "adj"） |
| wordItemTranslations | Array | 否 | 组词翻译列表 |
| exampleSentence | String | 否 | 例句 |
| examplePinyin | String | 否 | 例句拼音 |
| exampleTranslations | Array | 否 | 例句翻译列表 |
| exampleImage | String | 否 | 例句图片（资源ID） |

---

## 测试流程

### 1. 新增汉字
```
POST /api/character
Content-Type: application/json
Body: 上面的JSON
```

### 2. 修改汉字
```
PUT /api/character/{id}
Content-Type: application/json
Body: 上面的JSON（修改部分字段）
```

### 3. 审核通过
```
PUT /api/character/{id}/review
```

### 4. 发布
```
PUT /api/character/{id}/publish
```

### 5. 下线
```
PUT /api/character/{id}/offline
```

### 6. 删除
```
DELETE /api/character/{id}
```
