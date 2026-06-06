# 词汇模块 ER 图（HTML 单页）设计文档

- 日期：2026-06-06
- 范围：`VocabWordController` 涉及的全部词汇相关数据表
- 产出：`docs/superpowers/specs/vocab-erd.html`（可双击打开）

## 一、目的

为 `VocabWordController` 涉及的数据表绘制一张可视化 ER 图，要求：

- 展示**所有相关数据表**及其**所有字段**，每个字段附中文释义
- 展示表之间的关系（一对多、被引用）
- 展示 JSON 字段（`translations`、`options`、`draft_content`）的内部结构
- 形式：**单文件 HTML**，不依赖外部资源，双击即可在浏览器中查看

## 二、范围

### 包含的数据表（7 张）

| 表名 | 角色 | 说明 |
|---|---|---|
| `vocab_word` | 主表 | 词汇主体，继承 `BaseEntity`，含草稿字段 |
| `vocab_sense` | 子表 | 义项（一个词可有多个义项） |
| `vocab_structure` | 子表 | 结构搭配（属于某个义项） |
| `vocab_example` | 子表 | 例句（属于某个结构搭配） |
| `vocab_exercise` | 子表 | 练习题（直接挂在词上） |
| `vocab_outline_record` | 独立表 | 纲外词搜索记录（与上述表无外键，弱关联） |
| `audio_resource` | 资源表 | 被 `vocab_word`/`vocab_sense`/`vocab_example` 通过 audio_id 引用 |

### 包含的 JSON 内部结构（不是独立表，但需展示）

- `TextTranslation { language, translation }` — 用于 `vocab_sense.translations`、`vocab_example.translations`
- `ExerciseOption { option, text }` — 用于 `vocab_exercise.options`
- `draft_content` — 整词草稿快照（包含主表字段 + senses[] + exercises[] 等），用于 `vocab_word.draft_content`

### 不在本次范围内（留待后续）

- 接口 ↔ 表对照说明
- 草稿到发布的状态流转示意

## 三、表关系

```
audio_resource (被引用)
        ↑ audio_id
        │
vocab_word ──→ vocab_exercise
   │ 1:N
   ↓
vocab_sense
   │ 1:N
   ↓
vocab_structure
   │ 1:N
   ↓
vocab_example

vocab_outline_record  (独立表，无 FK)
```

- 所有外键采用应用层维护，**不创建数据库外键约束**（符合项目规范）
- `vocab_structure.sense_id` → `vocab_sense.id`、`vocab_example.{sense_id,structure_id}` → 对应表
- `audio_resource.id` 被 `vocab_word.audio_id`、`vocab_sense.def_audio_id`、`vocab_example.audio_id` 引用

## 四、HTML 页面设计

### 4.1 文件结构

单文件 `vocab-erd.html`：

```
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <title>词汇模块 ER 图</title>
    <style>/* 全部样式 */</style>
  </head>
  <body>
    <header>标题 + 图例</header>
    <main id="diagram"></main>
    <script>
      const SCHEMA = { /* 表元数据 */ };
      const JSON_SHAPES = { /* JSON 内部结构 */ };
      // 渲染逻辑
    </script>
  </body>
</html>
```

### 4.2 布局（自上而下）

```
顶部区（独立 / 资源表，并排）：
  [vocab_outline_record]    [audio_resource]

主区（按层级自上而下）：
  [vocab_word] ───────────────→ [vocab_exercise]
       │
       ↓
  [vocab_sense]
       │
       ↓
  [vocab_structure]
       │
       ↓
  [vocab_example]
```

使用 CSS Grid / Flex 实现，**不画 SVG 连线**，关系通过字段旁的 `→ 表名.字段` 标记 + hover 高亮表达。

### 4.3 表卡片视觉结构

每张表卡片由三部分组成：

**1. 表头**
- 表名（等宽字体，大号）
- 中文别名（如 "词汇主表"）
- 类型徽标（● 主表 / ○ 子表 / □ 资源表 / ◇ 独立表）

**2. 字段行**

每行字段显示：

| 列 | 内容 |
|---|---|
| 主键/外键图标 | 🔑 PK / 🔗 FK |
| 字段名 | 等宽字体 |
| 类型徽标 | `int` `varchar(50)` `json` `text` `bigint` 等彩色小标签 |
| 约束 | NOT NULL 标注 |
| 中文释义 | 灰色辅文 |
| FK 目标（如有） | `→ vocab_word.id` |

**3. JSON 字段展开区**

字段类型为 `json` 时，下方有可折叠区域显示其内部结构：

- `translations` → 显示 `TextTranslation` 的字段列表（language、translation）
- `options` → 显示 `ExerciseOption` 的字段列表
- `draft_content` → 显示嵌套快照结构概要

**4. 审计字段折叠**

继承自 `BaseEntity` 的字段（`create_by`、`update_by`、`create_time`、`update_time`）默认折叠，标 "审计字段 (4)" 按钮，点击展开。`vocab_word` 适用；其他子表只有 `create_time`/`update_time`，同样折叠。

### 4.4 配色

| 表类型 | 边框 | 表头背景 |
|---|---|---|
| 主表（vocab_word） | 深蓝 | 浅蓝 |
| 子表（sense/structure/example/exercise） | 深绿 | 浅绿 |
| 资源表（audio_resource） | 深紫 | 浅紫 |
| 独立表（vocab_outline_record） | 深灰 | 浅灰 |

字段类型徽标：
- `int` / `bigint` → 蓝色
- `varchar` → 绿色
- `text` → 黄色
- `json` → 橙色
- `timestamp` → 灰色

### 4.5 交互

- **hover 外键字段** → 目标表卡片边框高亮，目标主键字段高亮
- **hover 表头** → 引用该表的所有表 + 该表引用的所有表 都加上次级高亮
- **点击审计字段折叠按钮** → 展开/收起
- **点击 JSON 字段** → 展开/收起 JSON 内部结构区

### 4.6 数据驱动

所有表元数据存在 JS 对象中，DOM 由 JS 渲染。结构示意：

```js
const SCHEMA = {
  vocab_word: {
    role: 'main',
    aliasZh: '词汇主表',
    fields: [
      { name: 'id', type: 'int', pk: true, notNull: true, zh: '词汇唯一ID' },
      { name: 'word', type: 'varchar(50)', notNull: true, zh: '词汇' },
      { name: 'audio_id', type: 'bigint', fk: 'audio_resource.id', zh: '词汇读音音频资源ID' },
      { name: 'draft_content', type: 'json', jsonShape: 'draftContent', zh: '草稿内容JSON' },
      // ...
    ],
    auditFields: ['create_by', 'update_by', 'create_time', 'update_time'],
  },
  // 其他表...
};

const JSON_SHAPES = {
  textTranslation: [
    { name: 'language', type: 'string', zh: '语种枚举' },
    { name: 'translation', type: 'string', zh: '翻译文案' },
  ],
  exerciseOption: [
    { name: 'option', type: 'string', zh: '选项标识 (A/B/C/D)' },
    { name: 'text', type: 'string', zh: '选项文案' },
  ],
  draftContent: [
    { name: '(整词草稿快照)', type: 'object', zh: '包含主表字段 + senses[] + exercises[] 等' },
  ],
};
```

## 五、字段清单（完整数据）

### vocab_word

| 字段 | 类型 | 约束 | 释义 |
|---|---|---|---|
| id | int | PK, IDENTITY | 词汇唯一ID |
| word | varchar(50) | NOT NULL | 词汇 |
| word_traditional | varchar(50) | | 繁体词汇 |
| pinyin | varchar(100) | | 标准拼音（含声调） |
| audio_id | bigint | FK → audio_resource.id | 词汇读音音频资源ID |
| hsk_level | varchar(20) | | HSK等级（"1"-"9"） |
| status | int | | 状态: 1=可用, 0=已删除 |
| publish_status | varchar(20) | | 发布状态: unpublished/published |
| edit_status | varchar(20) | | 编辑状态: draft/reviewed/published |
| draft_content | json | | 草稿内容JSON（含主表和子表快照） |
| create_by | varchar | 审计 | 创建人 |
| update_by | varchar | 审计 | 更新人 |
| create_time | timestamp | 审计 | 创建时间 |
| update_time | timestamp | 审计 | 更新时间 |

### vocab_sense

| 字段 | 类型 | 约束 | 释义 |
|---|---|---|---|
| id | int | PK, IDENTITY | 义项ID |
| word_id | int | FK → vocab_word.id, NOT NULL | 所属词汇ID |
| part_of_speech | varchar(50) | | 词性 |
| chinese_def | text | | 中文释义 |
| def_audio_id | bigint | FK → audio_resource.id | 中文释义音频资源ID |
| translations | json | | 外文翻译列表 → TextTranslation[] |
| synonyms | text | | 近义词列表 |
| antonyms | text | | 反义词列表 |
| related_forward | text | | 正序关联词汇 |
| related_backward | text | | 逆序关联词汇 |
| sense_order | int | NOT NULL | 义项排序权重 |
| status | int | | 状态 |
| create_time | timestamp | | 创建时间 |
| update_time | timestamp | | 更新时间 |

### vocab_structure

| 字段 | 类型 | 约束 | 释义 |
|---|---|---|---|
| id | int | PK, IDENTITY | 结构搭配ID |
| word_id | int | FK → vocab_word.id, NOT NULL | 所属词汇ID |
| sense_id | int | FK → vocab_sense.id, NOT NULL | 所属义项ID |
| pattern | varchar(255) | NOT NULL | 结构搭配文案 |
| structure_order | int | NOT NULL | 搭配排序权重 |
| status | int | | 状态 |
| create_time | timestamp | | 创建时间 |
| update_time | timestamp | | 更新时间 |

### vocab_example

| 字段 | 类型 | 约束 | 释义 |
|---|---|---|---|
| id | int | PK, IDENTITY | 例句唯一ID |
| word_id | int | FK → vocab_word.id, NOT NULL | 所属词汇ID |
| sense_id | int | FK → vocab_sense.id, NOT NULL | 所属义项ID |
| structure_id | int | FK → vocab_structure.id, NOT NULL | 所属结构搭配ID |
| sentence | text | NOT NULL | 例句中文文案 |
| audio_id | bigint | FK → audio_resource.id | 例句音频资源ID |
| pinyin | varchar(500) | | 例句拼音 |
| translations | json | | 例句外文翻译列表 → TextTranslation[] |
| example_order | int | NOT NULL | 例句排序权重 |
| status | int | | 状态 |
| create_time | timestamp | | 创建时间 |
| update_time | timestamp | | 更新时间 |

### vocab_exercise

| 字段 | 类型 | 约束 | 释义 |
|---|---|---|---|
| id | int | PK, IDENTITY | 练习题目唯一ID |
| word_id | int | FK → vocab_word.id, NOT NULL | 所属词汇ID |
| question_type | varchar(20) | NOT NULL | 题目类型 |
| question_text | text | NOT NULL | 练习题干描述 |
| options | json | | 选项列表 → ExerciseOption[] |
| answers | json | | 答案列表 |
| exercise_order | int | NOT NULL | 练习题目排序权重 |
| status | int | | 状态 |
| create_time | timestamp | | 创建时间 |
| update_time | timestamp | | 更新时间 |

### vocab_outline_record

| 字段 | 类型 | 约束 | 释义 |
|---|---|---|---|
| id | int | PK, IDENTITY | 主键ID |
| word | varchar(50) | NOT NULL | 词汇文本 |
| search_count | int | NOT NULL | 未搜到次数 |
| status | int | | 处理状态: 0=未处理, 1=已处理 |
| create_time | timestamp | | 创建时间 |
| update_time | timestamp | | 更新时间 |

### audio_resource

| 字段 | 类型 | 约束 | 释义 |
|---|---|---|---|
| id | bigint | PK, IDENTITY | 主键 |
| text_content | text | NOT NULL | 音频对应的文字内容 |
| source_type | varchar(50) | NOT NULL | 来源类型: tts/upload |
| file_url | varchar(500) | NOT NULL | 音频文件地址 |
| file_format | varchar(20) | | 文件格式: mp3/wav/m4a |
| file_size | bigint | | 文件大小（字节） |
| status | int | | 有效状态: 1=有效, 0=无效 |
| create_time | timestamp | | 创建时间 |
| update_time | timestamp | | 更新时间 |

## 六、非目标

- 不提供 PDF/截图导出按钮
- 不画 SVG 关系连线（用布局 + hover 高亮替代）
- 本次不绘制接口 ↔ 表对照、不绘制草稿/发布流程示意（留待后续追加）
- 不引入任何外部 JS/CSS 库（保持单文件）

## 七、验收标准

- 单 HTML 文件，双击在 Chrome/Edge 中正常显示
- 7 张表卡片全部可见，所有字段及中文释义齐全
- JSON 字段可展开查看内部结构（TextTranslation / ExerciseOption / draft_content）
- 审计字段默认折叠，可展开
- hover 外键字段时，目标表卡片高亮
- 在 1920×1080 屏幕下无横向滚动
