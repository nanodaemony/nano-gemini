# 词汇模块 ER 图 — 实例数据示例（追加模块）设计文档

- 日期：2026-06-06
- 范围：在 `docs/superpowers/specs/vocab-erd.html` 末尾追加一个"实例数据示例"模块
- 目的：用真实业务样例演示表之间的 1:N 关系如何在数据上落地，方便对照表结构讲解

## 一、目的

现有的 `vocab-erd.html` 展示了**表结构**（字段、约束、关系），但读者难以直观感受"一个 word 有多个 sense、一个 sense 有多个 structure、一个 structure 有多个 example"这种层级关系在真实数据中是什么样。

本次新增一个**实例数据示例区**，用 `爱好(àihào)` 和 `爱好(àihǎo)` 作为示范：

- 同名（word 文本相同）但 ID 不同的两条词汇
- 每条词汇拥有不同数量的义项 / 结构 / 例句 / 练习
- 直接呈现各表中真实的字段值（包括 PK、FK、JSON 列）
- 让读者一眼看到 `sense.word_id`、`structure.sense_id`、`example.structure_id` 这些外键的实际取值与所指对象

## 二、范围

### 包含

- 在 `vocab-erd.html` 末尾追加一段独立区域（用 `<hr>` 和小标题与现有 ER 图分隔）
- 两条 word 实例（爱好 3 声 + 爱好 4 声），共 3 个义项、4 个结构、6 个例句、1 个练习题
- 树状层级展开 UI（点击展开/折叠）
- 每个节点支持"完整字段"折叠详情区，展开后展示所有字段实际值、FK 反向指示、JSON 字段紧凑展示
- 复用现有页面的色彩体系（与 ER 图卡片角色色一致）

### 不包含

- 不修改现有 ER 图部分
- 不引入路由、Tab 切换
- 不画 SVG 连线
- 仅 1 条示范数据集（不做"多组示例切换"）
- 不涉及音频实际播放、不渲染 audio_resource 的具体记录（仅在字段值中展示 `audio_id` 数字）

## 三、布局与视觉结构

### 3.1 整体位置

在现有页面的 `<main id="diagram">` 之后追加：

```
<main id="diagram"> ...现有 ER 图... </main>
<section id="instance-section">
  <h2>实例数据示例：爱好(àihào) vs. 爱好(àihǎo)</h2>
  <p class="intro">同一个 "爱好" 文本对应两条不同 ID 的词汇记录，分别有不同的义项、结构、例句。点击任意节点展开下一层；点击 📋 查看该行所有字段的实际值。</p>
  <div id="instance-tree"></div>
</section>
```

### 3.2 树形展开布局

默认状态：两条 word 卡片纵向并排，子层折叠。

```
🌿 word #1001  爱好 (àihào)  HSK4  ▾  [2 义项 · 1 练习]
🌿 word #1002  爱好 (àihǎo)  HSK6  ▾  [1 义项]
```

word 节点展开后：

```
🌿 word #1001  爱好 (àihào)  HSK4  ▴
  📋 完整字段 ▾
  ───────────────
  📘 sense #2001  [名] 对某种事物或活动的强烈喜欢  ▾  [2 结构]
    📋 完整字段 ▾
    ▶ structure #3001  「对 N 有/没/缺乏 爱好」  ▾  [2 例句]
      📋 完整字段 ▾
      • example #4001  "他对音乐很有爱好。"  ▾
        📋 完整字段 ▾
      • example #4002  "我对画画没什么爱好。"  ▾
    ▶ structure #3002  「... 的爱好是 N」  ▾  [1 例句]
      • example #4003  "我的爱好是读书。"
  📘 sense #2002  [名] 喜爱并经常从事的某种活动本身  ▾  [1 结构]
    ▶ structure #3003  「有/没有 + 爱好」  ▾
      • example #4004  "你有什么爱好？"
  📝 exercise #5001  [choice] 下列哪个最接近"爱好(àihào)"的意思？  ▾
    📋 完整字段 ▾
```

### 3.3 节点视觉规范

- **层级缩进**：每深一层缩进 24px；缩进区有浅灰竖线指示父子关系
- **节点行高**：紧凑（约 26px），便于俯瞰
- **配色**（与 ER 图卡片配色保持一致）：
  - word 节点 → 浅蓝边/底（与 `role-main` 一致）
  - sense / structure / example / exercise → 浅绿系不同深浅，区分层级
- **图标**：🌿 word | 📘 sense | ▶ structure | • example | 📝 exercise
- **节点头部行**包含：
  - 类型图标
  - `#id`（等宽字体，灰色）
  - 关键文本（word/chinese_def/pattern/sentence/question_text，截断到约 50 字符 + `...`，hover 显示全文 title）
  - 计数徽标（如 `[2 结构·3 例句]`，仅父类型节点显示）
  - 展开/折叠箭头 ▾/▴
- **"📋 完整字段" 折叠区**：默认折叠，点击展开后用紧凑两列表格列出该节点所有字段实际值
  - FK 字段在右侧附加灰色反向提示，如 `word_id: 1001 → 🌿 #1001 爱好(àihào)`
  - JSON 字段（translations、options、answers）展示为浅橙背景的等宽 JSON 代码块，1-2 行紧凑显示
  - 审计字段（create_time / update_time / create_by / update_by）默认在表格底部分组、再次折叠"审计 (N) ▾"

## 四、内容数据（INSTANCE_DATA）

### 4.1 word #1001 爱好(àihào)（名词）

```js
{
  id: 1001,
  word: '爱好',
  word_traditional: '愛好',
  pinyin: 'àihào',
  audio_id: 9001,
  hsk_level: '4',
  status: 1,
  publish_status: 'published',
  edit_status: 'published',
  draft_content: null,
  create_by: 'admin',
  update_by: 'admin',
  create_time: '2026-05-20 10:00:00',
  update_time: '2026-05-21 14:30:00',
}
```

#### sense #2001（对某种事物或活动的强烈喜欢）

- `id: 2001`、`word_id: 1001`、`part_of_speech: '名'`
- `chinese_def: '对某种事物或活动的强烈喜欢；表达兴趣指向'`
- `def_audio_id: 9101`
- `translations: [{language:'en', translation:'(strong) interest; liking'},{language:'ja', translation:'好み；興味'}]`
- `synonyms: '兴趣、嗜好'`、`antonyms: '厌恶'`
- `sense_order: 1`

structures + examples：

- **structure #3001** `pattern: '对 N 有/没/缺乏 爱好'`、`sense_id: 2001`、`structure_order: 1`
  - example #4001 `sentence: '他对音乐很有爱好。'`、`pinyin: 'tā duì yīnyuè hěn yǒu àihào.'`、`translations: [{language:'en', translation:'He has a strong interest in music.'}]`、`example_order: 1`
  - example #4002 `sentence: '我对画画没什么爱好。'`、`pinyin: 'wǒ duì huàhuà méi shénme àihào.'`、`example_order: 2`

- **structure #3002** `pattern: '... 的爱好是 N'`、`sense_id: 2001`、`structure_order: 2`
  - example #4003 `sentence: '我的爱好是读书。'`、`pinyin: 'wǒ de àihào shì dúshū.'`、`translations: [{language:'en', translation:'My hobby is reading.'}]`、`example_order: 1`

#### sense #2002（喜爱并经常从事的某种活动本身）

- `id: 2002`、`word_id: 1001`、`part_of_speech: '名'`
- `chinese_def: '喜爱并经常从事的某种活动本身（如阅读、运动）'`
- `translations: [{language:'en', translation:'hobby; pastime'}]`
- `sense_order: 2`

structures + examples：

- **structure #3003** `pattern: '有/没有 + 爱好'`、`sense_id: 2002`、`structure_order: 1`
  - example #4004 `sentence: '你有什么爱好？'`、`pinyin: 'nǐ yǒu shénme àihào?'`、`translations: [{language:'en', translation:'What hobbies do you have?'}]`

#### exercise #5001（绑在 word #1001 上，与 sense 平行）

- `id: 5001`、`word_id: 1001`
- `question_type: 'choice'`
- `question_text: '下列哪个最接近"爱好(àihào)"的意思？'`
- `options: [{option:'A', text:'职业'},{option:'B', text:'兴趣'},{option:'C', text:'工作'},{option:'D', text:'任务'}]`
- `answers: ['B']`
- `exercise_order: 1`

### 4.2 word #1002 爱好(àihǎo)（动词，书面语）

```js
{
  id: 1002,
  word: '爱好',
  word_traditional: '愛好',
  pinyin: 'àihǎo',
  audio_id: 9002,
  hsk_level: '6',
  status: 1,
  publish_status: 'published',
  edit_status: 'published',
  draft_content: null,
}
```

#### sense #2003（喜爱；多用于书面语）

- `id: 2003`、`word_id: 1002`、`part_of_speech: '动'`
- `chinese_def: '喜爱（多用于书面语，对象常为某种活动或事物）'`
- `def_audio_id: 9102`
- `translations: [{language:'en', translation:'to be fond of; to love (formal/literary)'}]`
- `synonyms: '喜爱、热爱'`
- `sense_order: 1`

structures + examples：

- **structure #3004** `pattern: 'S + 爱好 + N/V'`、`sense_id: 2003`、`structure_order: 1`
  - example #4005 `sentence: '他爱好读书。'`、`pinyin: 'tā àihǎo dúshū.'`、`translations: [{language:'en', translation:'He is fond of reading.'}]`
  - example #4006 `sentence: '鲁迅先生爱好收藏古籍。'`、`pinyin: 'Lǔxùn xiānsheng àihǎo shōucáng gǔjí.'`

word #1002 不设练习题（exercises 为空数组），以演示"练习是可选的"。

### 4.3 数据总览

| 类型 | 数量 | ID 范围 |
|---|---|---|
| word | 2 | 1001, 1002 |
| sense | 3 | 2001, 2002, 2003 |
| structure | 4 | 3001-3004 |
| example | 6 | 4001-4006 |
| exercise | 1 | 5001 |

## 五、JS 数据结构

数据以嵌套的方式组织（便于树形渲染），但每条记录保留所有原始字段（包括 FK），以体现关系数据库结构：

```js
const INSTANCE_DATA = {
  words: [
    {
      // ...word #1001 全部字段...
      senses: [
        {
          // ...sense #2001 全部字段（含 word_id）...
          structures: [
            {
              // ...structure #3001 全部字段（含 word_id, sense_id）...
              examples: [
                { /* example #4001 全部字段（含 word_id, sense_id, structure_id） */ },
                { /* example #4002 */ },
              ],
            },
            { /* structure #3002 + examples */ },
          ],
        },
        { /* sense #2002 + structures */ },
      ],
      exercises: [
        { /* exercise #5001 全部字段（含 word_id） */ },
      ],
    },
    { /* word #1002 + senses + (exercises: []) */ },
  ],
};
```

约定：嵌套关系只是渲染便利；FK 字段（`word_id` / `sense_id` / `structure_id`）仍在子节点中显式保留，渲染时正是这些字段在"完整字段"展开区显示并提示反向指向。

## 六、渲染实现要点

### 6.1 入口

在现有 `render()` 函数末尾追加一行：

```js
renderInstance();
```

`renderInstance()` 单独负责实例区，与 ER 图渲染解耦。

### 6.2 主要函数

- `renderInstance()` — 在 `#instance-tree` 内挂载所有 word 节点
- `renderWordNode(word)` — 一个 word 节点（含其 senses 子树和 exercises 子树）
- `renderSenseNode(sense, wordCtx)` — 一个 sense 节点
- `renderStructureNode(structure, wordCtx, senseCtx)` — 一个 structure 节点
- `renderExampleNode(example, ctx)` — 叶节点
- `renderExerciseNode(exercise, wordCtx)` — exercise 叶节点
- `renderFullFields(record, schemaName)` — 通用的"完整字段"折叠区
- `renderFkValue(fkFieldName, fkValue)` — 把 `word_id: 1001` 渲染为 `1001 → 🌿 #1001 爱好(àihào)`，需要查 INSTANCE_DATA 找到目标

### 6.3 复用

- 复用 `el()` 帮助函数
- 复用 `SCHEMA` 元数据来决定字段类型徽标（int/varchar/json 等），保持视觉与上方 ER 图一致
- 不复用 `bindHover` —— 实例区是独立交互（点击展开、点击 📋 展开字段），无 hover 高亮

## 七、CSS 新增类（命名空间 `.inst-`）

为避免与现有 `.table-card` / `.field-row` 冲突，全部加 `inst-` 前缀：

- `#instance-section` — 容器，与 ER 图之间有 `border-top`
- `#instance-section h2` — 小节标题
- `#instance-section .intro` — 说明文字
- `.inst-node` — 通用节点（树节点容器）
- `.inst-node-header` — 节点头部行（图标 + id + 文本 + 计数 + ▾/▴）
- `.inst-node-children` — 子节点容器（默认 `display: none`，open 时 `display: block`）
- `.inst-node.open > .inst-node-children` — 展开状态
- `.inst-role-word / .inst-role-sense / .inst-role-structure / .inst-role-example / .inst-role-exercise` — 各类型配色
- `.inst-id` — `#1001` 灰色等宽
- `.inst-text` — 关键文本
- `.inst-count` — 计数徽标
- `.inst-fields-toggle` — 📋 完整字段折叠按钮
- `.inst-fields` / `.inst-fields.open` — 字段表格
- `.inst-field-row` — 字段表格中的一行
- `.inst-fk-hint` — FK 反向指示（灰色）
- `.inst-json-block` — JSON 紧凑展示块
- `.inst-audit-toggle` / `.inst-audit-fields` — 审计字段嵌套折叠

## 八、验收标准

- [ ] 页面打开后，先看到原有 ER 图，下方有 `<hr>` 分隔的实例区
- [ ] 实例区显示 2 张 word 节点卡（爱好 àihào + 爱好 àihǎo），ID 不同
- [ ] 默认折叠状态下，每个 word 节点显示计数徽标（如 `[2 义项 · 1 练习]`、`[1 义项]`）
- [ ] 点击 word 头部，展开后能看到 2 个义项节点（word #1001）或 1 个（word #1002）+ exercise 节点
- [ ] 逐层点击展开，所有 6 个例句最终都能展示
- [ ] 任意节点的"📋 完整字段"折叠按钮，点击展开后能看到该节点所有字段的实际值
- [ ] FK 字段（word_id、sense_id、structure_id）的反向指示能正确显示目标 id 和文本
- [ ] JSON 字段（translations、options、answers）以紧凑代码块展示
- [ ] 审计字段默认折叠，可二级展开
- [ ] 在 1920×1080 屏幕下页面无横向滚动
- [ ] 不引入外部依赖；现有 ER 图行为不受影响

## 九、非目标

- 不修改现有 ER 图渲染逻辑（仅在 `render()` 末尾追加调用）
- 不引入任何 hover 高亮（点击式交互即可）
- 不画 SVG 关系连线
- 不做多组示例切换（只有一组：爱好×2）
- 不打印 / 导出按钮
- 不展示音频实际播放控件
