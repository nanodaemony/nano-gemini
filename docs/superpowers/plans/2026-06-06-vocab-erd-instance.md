# 词汇模块 ER 图 — 实例数据示例模块 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `docs/superpowers/specs/vocab-erd.html` 末尾追加一个独立"实例数据示例"区，用 爱好(àihào) + 爱好(àihǎo) 两条样本词演示 word → sense → structure → example 的 1:N 关系，每个节点支持点击展开下一层和"完整字段"展开。

**Architecture:** 数据驱动渲染 — `INSTANCE_DATA` 嵌套对象（word.senses[].structures[].examples[]）保留所有原始字段；递归渲染函数 `renderWordNode`/`renderSenseNode`/...产出树节点；与现有 `render()` 解耦，仅在其末尾追加 `renderInstance()` 调用。CSS 全部以 `.inst-` 前缀命名空间隔离。

**Tech Stack:** 纯 HTML5 + 内联 CSS + 内联原生 JS (ES2015+)。无新增外部依赖。

**Spec 参考:** `docs/superpowers/specs/2026-06-06-vocab-erd-instance-design.md`

## 关于"测试"

单文件 HTML，无测试框架。每个任务的"验证"步骤为：
1. 读回文件确认插入位置正确
2. （可选）控制台目测 — 由用户在浏览器中验证

每完成一阶段必须提交。后续任务依赖前面任务的插入位置和命名约定。

## 文件结构

唯一修改的文件：`C:\Users\nano\Desktop\nano-gemini\docs\superpowers\specs\vocab-erd.html`

修改后文件总览（追加部分）：

```
现有 <style> ... </style>
  + 追加 .inst-* 规则集（Task 3）

现有 <body>:
  现有 <header>...</header>
  现有 <main id="diagram"></main>
  + <section id="instance-section">...</section>（Task 1 骨架，Task 4-5 由 JS 填充）

现有 <script>:
  现有 SCHEMA, JSON_SHAPES
  + INSTANCE_DATA = {...}（Task 2）
  现有 typeClass, el, renderJsonShape, renderField, ...
  + findFkTarget(table, id), renderFkValue(...), renderFullFields(...)（Task 5）
  + renderInstance(), renderWordNode, renderSenseNode, renderStructureNode,
    renderExampleNode, renderExerciseNode（Task 4）
  现有 render() — 在末尾追加 renderInstance();（Task 1）
  现有 DOMContentLoaded 监听器
```

---

## Task 1：追加 `<section>` 骨架 + 入口 hook

**Files:**
- Modify: `C:\Users\nano\Desktop\nano-gemini\docs\superpowers\specs\vocab-erd.html`

- [ ] **Step 1：在 `</main>` 之后追加新的 section**

定位到 `</main>` 标签（它出现在 `<header>...</header>` 后的 `<main id="diagram"></main>`），在 `</main>` 之后、`<script>` 之前，插入：

```html
  <section id="instance-section">
    <h2>实例数据示例：爱好(àihào) vs. 爱好(àihǎo)</h2>
    <p class="intro">
      同一个"爱好"文本对应两条不同 ID 的词汇记录，分别有不同的义项、结构、例句。
      点击任意节点展开下一层；点击 📋 查看该行所有字段的实际值。
    </p>
    <div id="instance-tree">
      <p style="color:#888">渲染中...</p>
    </div>
  </section>
```

保持与 `<main>` 一致的 2 空格缩进。

- [ ] **Step 2：在 `<script>` 内的 `render()` 函数末尾追加调用**

定位到 `render()` 函数中已有的最后一行 `bindHover(root);`，在它之后追加一行 `renderInstance();`，使函数结尾看起来是：

```js
  root.append(topRow, mainRow1, mainRow2, mainRow3, mainRow4);
  bindHover(root);
  renderInstance();
}
```

同时，为了避免 `renderInstance is not defined` 报错（Task 4 才真正实现），现在在文件 `<script>` 内的某个位置（例如 `function render()` 之前一行）添加临时占位：

```js
function renderInstance() {
  // Task 4 中实现
}
```

- [ ] **Step 3：浏览器目测（可选）**

打开 `docs/superpowers/specs/vocab-erd.html`，预期：原有 ER 图正常显示；ER 图下方出现 "实例数据示例：..." 标题 + 说明文字 + "渲染中..." 灰字占位。浏览器控制台无 JS 报错。

- [ ] **Step 4：提交**

```bash
cd C:\Users\nano\Desktop\nano-gemini
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd-instance): add instance section skeleton and render entry hook"
```

---

## Task 2：追加 INSTANCE_DATA 数据

**Files:**
- Modify: `C:\Users\nano\Desktop\nano-gemini\docs\superpowers\specs\vocab-erd.html`

- [ ] **Step 1：在 `<script>` 内，`JSON_SHAPES` 定义之后追加 INSTANCE_DATA**

定位到 `<script>` 内的 `const JSON_SHAPES = {...};` 结束位置。在 `};` 之后、`function typeClass(...)` 之前，插入下面整段：

```js
const INSTANCE_DATA = {
  words: [
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
      senses: [
        {
          id: 2001,
          word_id: 1001,
          part_of_speech: '名',
          chinese_def: '对某种事物或活动的强烈喜欢；表达兴趣指向',
          def_audio_id: 9101,
          translations: [
            { language: 'en', translation: '(strong) interest; liking' },
            { language: 'ja', translation: '好み；興味' },
          ],
          synonyms: '兴趣、嗜好',
          antonyms: '厌恶',
          related_forward: '',
          related_backward: '',
          sense_order: 1,
          status: 1,
          create_time: '2026-05-20 10:05:00',
          update_time: '2026-05-20 10:05:00',
          structures: [
            {
              id: 3001,
              word_id: 1001,
              sense_id: 2001,
              pattern: '对 N 有/没/缺乏 爱好',
              structure_order: 1,
              status: 1,
              create_time: '2026-05-20 10:10:00',
              update_time: '2026-05-20 10:10:00',
              examples: [
                {
                  id: 4001, word_id: 1001, sense_id: 2001, structure_id: 3001,
                  sentence: '他对音乐很有爱好。',
                  audio_id: 9201,
                  pinyin: 'tā duì yīnyuè hěn yǒu àihào.',
                  translations: [{ language: 'en', translation: 'He has a strong interest in music.' }],
                  example_order: 1, status: 1,
                  create_time: '2026-05-20 10:12:00', update_time: '2026-05-20 10:12:00',
                },
                {
                  id: 4002, word_id: 1001, sense_id: 2001, structure_id: 3001,
                  sentence: '我对画画没什么爱好。',
                  audio_id: 9202,
                  pinyin: 'wǒ duì huàhuà méi shénme àihào.',
                  translations: [],
                  example_order: 2, status: 1,
                  create_time: '2026-05-20 10:13:00', update_time: '2026-05-20 10:13:00',
                },
              ],
            },
            {
              id: 3002,
              word_id: 1001,
              sense_id: 2001,
              pattern: '... 的爱好是 N',
              structure_order: 2,
              status: 1,
              create_time: '2026-05-20 10:15:00',
              update_time: '2026-05-20 10:15:00',
              examples: [
                {
                  id: 4003, word_id: 1001, sense_id: 2001, structure_id: 3002,
                  sentence: '我的爱好是读书。',
                  audio_id: 9203,
                  pinyin: 'wǒ de àihào shì dúshū.',
                  translations: [{ language: 'en', translation: 'My hobby is reading.' }],
                  example_order: 1, status: 1,
                  create_time: '2026-05-20 10:16:00', update_time: '2026-05-20 10:16:00',
                },
              ],
            },
          ],
        },
        {
          id: 2002,
          word_id: 1001,
          part_of_speech: '名',
          chinese_def: '喜爱并经常从事的某种活动本身（如阅读、运动）',
          def_audio_id: 9103,
          translations: [{ language: 'en', translation: 'hobby; pastime' }],
          synonyms: '',
          antonyms: '',
          related_forward: '',
          related_backward: '',
          sense_order: 2,
          status: 1,
          create_time: '2026-05-20 10:20:00',
          update_time: '2026-05-20 10:20:00',
          structures: [
            {
              id: 3003,
              word_id: 1001,
              sense_id: 2002,
              pattern: '有/没有 + 爱好',
              structure_order: 1,
              status: 1,
              create_time: '2026-05-20 10:22:00',
              update_time: '2026-05-20 10:22:00',
              examples: [
                {
                  id: 4004, word_id: 1001, sense_id: 2002, structure_id: 3003,
                  sentence: '你有什么爱好？',
                  audio_id: 9204,
                  pinyin: 'nǐ yǒu shénme àihào?',
                  translations: [{ language: 'en', translation: 'What hobbies do you have?' }],
                  example_order: 1, status: 1,
                  create_time: '2026-05-20 10:23:00', update_time: '2026-05-20 10:23:00',
                },
              ],
            },
          ],
        },
      ],
      exercises: [
        {
          id: 5001,
          word_id: 1001,
          question_type: 'choice',
          question_text: '下列哪个最接近"爱好(àihào)"的意思？',
          options: [
            { option: 'A', text: '职业' },
            { option: 'B', text: '兴趣' },
            { option: 'C', text: '工作' },
            { option: 'D', text: '任务' },
          ],
          answers: ['B'],
          exercise_order: 1,
          status: 1,
          create_time: '2026-05-20 11:00:00',
          update_time: '2026-05-20 11:00:00',
        },
      ],
    },
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
      create_by: 'admin',
      update_by: 'admin',
      create_time: '2026-05-22 09:00:00',
      update_time: '2026-05-22 09:00:00',
      senses: [
        {
          id: 2003,
          word_id: 1002,
          part_of_speech: '动',
          chinese_def: '喜爱（多用于书面语，对象常为某种活动或事物）',
          def_audio_id: 9102,
          translations: [{ language: 'en', translation: 'to be fond of; to love (formal/literary)' }],
          synonyms: '喜爱、热爱',
          antonyms: '',
          related_forward: '',
          related_backward: '',
          sense_order: 1,
          status: 1,
          create_time: '2026-05-22 09:05:00',
          update_time: '2026-05-22 09:05:00',
          structures: [
            {
              id: 3004,
              word_id: 1002,
              sense_id: 2003,
              pattern: 'S + 爱好 + N/V',
              structure_order: 1,
              status: 1,
              create_time: '2026-05-22 09:10:00',
              update_time: '2026-05-22 09:10:00',
              examples: [
                {
                  id: 4005, word_id: 1002, sense_id: 2003, structure_id: 3004,
                  sentence: '他爱好读书。',
                  audio_id: 9205,
                  pinyin: 'tā àihǎo dúshū.',
                  translations: [{ language: 'en', translation: 'He is fond of reading.' }],
                  example_order: 1, status: 1,
                  create_time: '2026-05-22 09:12:00', update_time: '2026-05-22 09:12:00',
                },
                {
                  id: 4006, word_id: 1002, sense_id: 2003, structure_id: 3004,
                  sentence: '鲁迅先生爱好收藏古籍。',
                  audio_id: 9206,
                  pinyin: 'Lǔxùn xiānsheng àihǎo shōucáng gǔjí.',
                  translations: [],
                  example_order: 2, status: 1,
                  create_time: '2026-05-22 09:13:00', update_time: '2026-05-22 09:13:00',
                },
              ],
            },
          ],
        },
      ],
      exercises: [],
    },
  ],
};
```

- [ ] **Step 2：浏览器目测（可选）**

打开页面，控制台输入：
- `INSTANCE_DATA.words.length` → 应返回 `2`
- `INSTANCE_DATA.words[0].senses.length` → 应返回 `2`
- `INSTANCE_DATA.words[0].senses[0].structures[0].examples.length` → 应返回 `2`
- `INSTANCE_DATA.words[1].exercises.length` → 应返回 `0`

- [ ] **Step 3：提交**

```bash
cd C:\Users\nano\Desktop\nano-gemini
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd-instance): add INSTANCE_DATA for 爱好 x2 sample"
```

---

## Task 3：追加 `.inst-*` CSS

**Files:**
- Modify: `C:\Users\nano\Desktop\nano-gemini\docs\superpowers\specs\vocab-erd.html`

- [ ] **Step 1：在 `</style>` 之前追加 CSS 块**

定位到 `<style>` 块结尾（在 Task 6 of ERD plan 之后添加的最后那行高亮规则之后），在 `</style>` 之前追加：

```css

/* ===== 实例数据示例区 ===== */
#instance-section { padding: 24px; background: #fff; border-top: 1px solid #e3e5e8; margin-top: 24px; }
#instance-section h2 { margin: 0 0 8px; font-size: 16px; color: #222; }
#instance-section .intro { margin: 0 0 16px; font-size: 13px; color: #666; line-height: 1.6; }
#instance-tree { display: flex; flex-direction: column; gap: 12px; }

.inst-node { border-left: 2px solid #e3e5e8; padding-left: 12px; }
.inst-node-header { display: flex; align-items: baseline; gap: 8px; padding: 6px 8px; cursor: pointer; border-radius: 4px; user-select: none; line-height: 1.5; font-size: 13px; }
.inst-node-header:hover { background: #f3f5f8; }
.inst-node-header .arrow { font-size: 10px; color: #888; margin-left: auto; }
.inst-node-children { display: none; margin: 4px 0 8px 14px; }
.inst-node.open > .inst-node-children { display: block; }

.inst-icon { font-size: 13px; }
.inst-id { font-family: ui-monospace, Consolas, monospace; font-size: 11px; color: #999; }
.inst-text { color: #222; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.inst-count { font-size: 11px; color: #555; background: rgba(0,0,0,.05); padding: 1px 6px; border-radius: 8px; white-space: nowrap; }

/* 各类型节点配色（边框 + 头部底色） */
.inst-role-word      { border-left-color: #2f6fd6; }
.inst-role-word      > .inst-node-header { background: #e8f0fc; font-weight: 600; }
.inst-role-word      > .inst-node-header:hover { background: #dce8fa; }

.inst-role-sense     { border-left-color: #2f9b6f; }
.inst-role-sense     > .inst-node-header { background: #e6f6ee; }
.inst-role-sense     > .inst-node-header:hover { background: #d6f0e1; }

.inst-role-structure { border-left-color: #4ea888; }
.inst-role-structure > .inst-node-header { background: #ecf7f1; }

.inst-role-example   { border-left-color: #c8a86a; }
.inst-role-example   > .inst-node-header { background: #fbf5e6; }

.inst-role-exercise  { border-left-color: #8a4fc9; }
.inst-role-exercise  > .inst-node-header { background: #f0e8fa; }

/* 完整字段折叠区 */
.inst-fields-toggle { display: inline-block; margin: 4px 0 4px 14px; padding: 2px 8px; font-size: 11px; color: #2f6fd6; cursor: pointer; user-select: none; border: 1px dashed #c8d4e8; border-radius: 4px; background: #fafbfc; }
.inst-fields-toggle:hover { background: #eef2f8; }
.inst-fields { display: none; margin: 4px 14px 8px; padding: 8px 12px; background: #fafbfc; border: 1px solid #e3e5e8; border-radius: 4px; font-size: 12px; }
.inst-fields.open { display: block; }
.inst-field-row { display: grid; grid-template-columns: 160px 1fr; gap: 8px; padding: 2px 0; align-items: baseline; }
.inst-field-row .fname { font-family: ui-monospace, Consolas, monospace; color: #555; font-size: 11px; }
.inst-field-row .fvalue { font-family: ui-monospace, Consolas, monospace; color: #222; word-break: break-all; }
.inst-field-row .fvalue.null { color: #999; font-style: italic; }
.inst-fk-hint { color: #888; font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif; font-size: 11px; margin-left: 6px; }
.inst-json-block { background: #fff7ed; border: 1px solid #f0d9b8; border-radius: 3px; padding: 4px 8px; font-family: ui-monospace, Consolas, monospace; font-size: 11px; color: #6a4400; white-space: pre-wrap; word-break: break-all; }

.inst-audit-toggle { display: inline-block; margin: 6px 0 2px; padding: 1px 6px; font-size: 11px; color: #888; cursor: pointer; user-select: none; border-top: 1px dashed #d8dce3; padding-top: 4px; }
.inst-audit-toggle:hover { color: #555; }
.inst-audit-fields { display: none; margin-top: 4px; padding-top: 4px; border-top: 1px dashed #e3e5e8; }
.inst-audit-fields.open { display: block; }
```

- [ ] **Step 2：浏览器目测（可选）**

刷新页面，确认页面布局没破。`#instance-section` 现在应该有白色背景、顶部分隔线、内边距。其他实例区元素还未渲染，仅"渲染中..."灰字。

- [ ] **Step 3：提交**

```bash
cd C:\Users\nano\Desktop\nano-gemini
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd-instance): add .inst-* CSS for tree nodes and field details"
```

---

## Task 4：实现 renderInstance + 树节点渲染（无字段折叠）

**Files:**
- Modify: `C:\Users\nano\Desktop\nano-gemini\docs\superpowers\specs\vocab-erd.html`

- [ ] **Step 1：删除 Task 1 中的占位 `renderInstance` 函数**

定位到 Task 1 中插入的临时占位：

```js
function renderInstance() {
  // Task 4 中实现
}
```

将整段（含函数定义）删除。

- [ ] **Step 2：在 `<script>` 中，`render()` 函数之前追加实例区渲染函数集**

定位到 `function render()` 这一行的上方一行（即所有渲染辅助函数之后、`render` 之前），插入：

```js
function truncate(s, n) {
  if (s == null) return '';
  const str = String(s);
  return str.length > n ? str.slice(0, n) + '…' : str;
}

function nodeHeader(role, icon, idText, mainText, countText) {
  const header = el('div', { class: 'inst-node-header' },
    el('span', { class: 'inst-icon' }, icon),
    el('span', { class: 'inst-id' }, `#${idText}`),
    el('span', { class: 'inst-text', title: mainText || '' }, truncate(mainText, 50)),
    countText ? el('span', { class: 'inst-count' }, countText) : null,
    el('span', { class: 'arrow' }, '▾'),
  );
  return header;
}

function makeCollapsibleNode(role, header, childrenChildren) {
  // childrenChildren: 数组，子层级 DOM 节点列表
  const node = el('div', { class: `inst-node inst-role-${role}` });
  node.appendChild(header);
  const children = el('div', { class: 'inst-node-children' });
  for (const c of childrenChildren) {
    if (c) children.appendChild(c);
  }
  node.appendChild(children);

  header.addEventListener('click', (e) => {
    // 防止点击字段折叠按钮或字段区时也触发整节点折叠
    if (e.target.closest('.inst-fields-toggle') || e.target.closest('.inst-fields')) return;
    const open = node.classList.toggle('open');
    const arrow = header.querySelector('.arrow');
    if (arrow) arrow.textContent = open ? '▴' : '▾';
  });

  return node;
}

function renderExampleNode(example) {
  const header = nodeHeader('example', '•', example.id, example.sentence, null);
  // Task 5 中会把字段折叠区作为 children 的一部分加进来；当前先空
  return makeCollapsibleNode('example', header, []);
}

function renderStructureNode(structure) {
  const examples = structure.examples || [];
  const countText = `${examples.length} 例句`;
  const header = nodeHeader('structure', '▶', structure.id, `「${structure.pattern}」`, countText);
  const children = examples.map(renderExampleNode);
  return makeCollapsibleNode('structure', header, children);
}

function renderSenseNode(sense) {
  const structures = sense.structures || [];
  const totalExamples = structures.reduce((sum, s) => sum + (s.examples ? s.examples.length : 0), 0);
  const countText = `${structures.length} 结构 · ${totalExamples} 例句`;
  const pos = sense.part_of_speech ? `[${sense.part_of_speech}] ` : '';
  const header = nodeHeader('sense', '📘', sense.id, pos + (sense.chinese_def || ''), countText);
  const children = structures.map(renderStructureNode);
  return makeCollapsibleNode('sense', header, children);
}

function renderExerciseNode(exercise) {
  const qt = exercise.question_type ? `[${exercise.question_type}] ` : '';
  const header = nodeHeader('exercise', '📝', exercise.id, qt + (exercise.question_text || ''), null);
  return makeCollapsibleNode('exercise', header, []);
}

function renderWordNode(word) {
  const senses = word.senses || [];
  const exercises = word.exercises || [];
  const parts = [];
  if (senses.length) parts.push(`${senses.length} 义项`);
  if (exercises.length) parts.push(`${exercises.length} 练习`);
  const countText = parts.length ? parts.join(' · ') : '空';
  const hsk = word.hsk_level ? ` HSK${word.hsk_level}` : '';
  const mainText = `${word.word} (${word.pinyin})${hsk}`;
  const header = nodeHeader('word', '🌿', word.id, mainText, countText);

  const children = [
    ...senses.map(renderSenseNode),
    ...exercises.map(renderExerciseNode),
  ];
  return makeCollapsibleNode('word', header, children);
}

function renderInstance() {
  const root = document.getElementById('instance-tree');
  if (!root) return;
  root.innerHTML = '';
  for (const w of INSTANCE_DATA.words) {
    root.appendChild(renderWordNode(w));
  }
}
```

- [ ] **Step 3：浏览器目测（强烈建议）**

刷新页面，预期：
- `#instance-tree` 区域显示两条 word 节点（蓝色头部、左边蓝色竖线）
- 头部内容形如 `🌿 #1001  爱好 (àihào) HSK4  [2 义项 · 1 练习] ▾`
- 点击 word 头部展开，看到 2 个 sense 节点（绿色） + 1 个 exercise 节点（紫色）
- 点击 sense 头部展开，看到对应 structure（中绿）
- 点击 structure 头部展开，看到对应 example（土黄色）
- 点击 word #1002 头部展开，看到 1 个 sense（无 exercise）
- 全部展开后所有 6 条例句可见
- 箭头 ▾/▴ 切换
- 控制台无报错

- [ ] **Step 4：提交**

```bash
cd C:\Users\nano\Desktop\nano-gemini
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd-instance): render tree nodes for word/sense/structure/example/exercise"
```

---

## Task 5：实现"完整字段"折叠区（含 FK 反向指示、JSON 紧凑展示、审计折叠）

**Files:**
- Modify: `C:\Users\nano\Desktop\nano-gemini\docs\superpowers\specs\vocab-erd.html`

- [ ] **Step 1：在 `<script>` 中，`renderInstance` 之前（与其他实例渲染函数同一块区域）追加字段渲染辅助函数**

找到 Task 4 添加的 `truncate(...)` 函数之前（即所有实例区相关函数的最前面），插入：

```js
function findFkTarget(fkFieldName, fkValue) {
  // 根据 FK 字段名找出目标记录的简短描述
  if (fkValue == null) return null;
  if (fkFieldName === 'word_id') {
    const w = INSTANCE_DATA.words.find(x => x.id === fkValue);
    return w ? { icon: '🌿', text: `${w.word}(${w.pinyin})` } : null;
  }
  if (fkFieldName === 'sense_id') {
    for (const w of INSTANCE_DATA.words) {
      const s = (w.senses || []).find(x => x.id === fkValue);
      if (s) return { icon: '📘', text: `${s.part_of_speech || ''} ${truncate(s.chinese_def, 20)}` };
    }
    return null;
  }
  if (fkFieldName === 'structure_id') {
    for (const w of INSTANCE_DATA.words) {
      for (const s of (w.senses || [])) {
        const st = (s.structures || []).find(x => x.id === fkValue);
        if (st) return { icon: '▶', text: `「${truncate(st.pattern, 25)}」` };
      }
    }
    return null;
  }
  if (fkFieldName === 'audio_id' || fkFieldName === 'def_audio_id') {
    // 实例数据未包含 audio_resource 记录，仅给出占位提示
    return { icon: '🔊', text: `(audio_resource 表)` };
  }
  return null;
}

function isJsonField(schemaName, fieldName) {
  const schema = SCHEMA[schemaName];
  if (!schema) return false;
  const f = schema.fields.find(x => x.name === fieldName);
  return f && f.type === 'json';
}

function fieldKeysForSchema(schemaName) {
  // 返回 schema 定义的普通字段名列表（不含审计字段）
  const schema = SCHEMA[schemaName];
  if (!schema) return [];
  return schema.fields.map(f => f.name);
}

function auditKeysForSchema(schemaName) {
  const schema = SCHEMA[schemaName];
  if (!schema || !schema.auditFields) return [];
  return schema.auditFields.map(f => f.name);
}

function formatJsonValue(v) {
  if (v == null) return 'null';
  return JSON.stringify(v, null, 0);
}

function isFkField(schemaName, fieldName) {
  const schema = SCHEMA[schemaName];
  if (!schema) return false;
  const f = schema.fields.find(x => x.name === fieldName);
  return !!(f && f.fk);
}

function renderFieldValueCell(schemaName, fieldName, value) {
  const cell = el('span', { class: 'fvalue' });
  if (value == null) {
    cell.classList.add('null');
    cell.textContent = 'null';
    return cell;
  }
  if (isJsonField(schemaName, fieldName)) {
    cell.appendChild(el('div', { class: 'inst-json-block' }, formatJsonValue(value)));
  } else {
    cell.textContent = String(value);
  }
  if (isFkField(schemaName, fieldName)) {
    const target = findFkTarget(fieldName, value);
    if (target) {
      cell.appendChild(el('span', { class: 'inst-fk-hint' },
        `→ ${target.icon} #${value} ${target.text}`));
    }
  }
  return cell;
}

function renderFieldRow(schemaName, fieldName, value) {
  return el('div', { class: 'inst-field-row' },
    el('span', { class: 'fname' }, fieldName),
    renderFieldValueCell(schemaName, fieldName, value),
  );
}

function renderFullFields(record, schemaName) {
  // 返回一个包含 fields-toggle 按钮 + .inst-fields 容器的 fragment
  const frag = document.createDocumentFragment();

  const toggle = el('div', { class: 'inst-fields-toggle' }, '📋 完整字段 ▾');
  const body = el('div', { class: 'inst-fields' });

  // 1) 普通字段
  const normalKeys = fieldKeysForSchema(schemaName);
  for (const k of normalKeys) {
    if (k in record) {
      body.appendChild(renderFieldRow(schemaName, k, record[k]));
    }
  }

  // 2) 审计字段（嵌套折叠）
  const auditKeys = auditKeysForSchema(schemaName).filter(k => k in record);
  if (auditKeys.length > 0) {
    const auditToggle = el('div', { class: 'inst-audit-toggle' },
      `审计字段 (${auditKeys.length}) ▾`);
    const auditBody = el('div', { class: 'inst-audit-fields' });
    for (const k of auditKeys) {
      auditBody.appendChild(renderFieldRow(schemaName, k, record[k]));
    }
    auditToggle.addEventListener('click', (e) => {
      e.stopPropagation();
      const open = auditBody.classList.toggle('open');
      auditToggle.textContent = `审计字段 (${auditKeys.length}) ${open ? '▴' : '▾'}`;
    });
    body.appendChild(auditToggle);
    body.appendChild(auditBody);
  }

  toggle.addEventListener('click', (e) => {
    e.stopPropagation();
    const open = body.classList.toggle('open');
    toggle.textContent = `📋 完整字段 ${open ? '▴' : '▾'}`;
  });

  frag.appendChild(toggle);
  frag.appendChild(body);
  return frag;
}
```

- [ ] **Step 2：把 `renderFullFields(...)` 插入到每个树节点的子层第一项**

修改 Task 4 添加的五个节点渲染函数，让它们在 `children` 数组的最前面插入一个 `renderFullFields` fragment。

将 `renderExampleNode`：

```js
function renderExampleNode(example) {
  const header = nodeHeader('example', '•', example.id, example.sentence, null);
  // Task 5 中会把字段折叠区作为 children 的一部分加进来；当前先空
  return makeCollapsibleNode('example', header, []);
}
```

替换为：

```js
function renderExampleNode(example) {
  const header = nodeHeader('example', '•', example.id, example.sentence, null);
  return makeCollapsibleNode('example', header, [renderFullFields(example, 'vocab_example')]);
}
```

将 `renderStructureNode`：

```js
function renderStructureNode(structure) {
  const examples = structure.examples || [];
  const countText = `${examples.length} 例句`;
  const header = nodeHeader('structure', '▶', structure.id, `「${structure.pattern}」`, countText);
  const children = examples.map(renderExampleNode);
  return makeCollapsibleNode('structure', header, children);
}
```

替换为：

```js
function renderStructureNode(structure) {
  const examples = structure.examples || [];
  const countText = `${examples.length} 例句`;
  const header = nodeHeader('structure', '▶', structure.id, `「${structure.pattern}」`, countText);
  const children = [
    renderFullFields(structure, 'vocab_structure'),
    ...examples.map(renderExampleNode),
  ];
  return makeCollapsibleNode('structure', header, children);
}
```

将 `renderSenseNode`：

```js
function renderSenseNode(sense) {
  const structures = sense.structures || [];
  const totalExamples = structures.reduce((sum, s) => sum + (s.examples ? s.examples.length : 0), 0);
  const countText = `${structures.length} 结构 · ${totalExamples} 例句`;
  const pos = sense.part_of_speech ? `[${sense.part_of_speech}] ` : '';
  const header = nodeHeader('sense', '📘', sense.id, pos + (sense.chinese_def || ''), countText);
  const children = structures.map(renderStructureNode);
  return makeCollapsibleNode('sense', header, children);
}
```

替换为：

```js
function renderSenseNode(sense) {
  const structures = sense.structures || [];
  const totalExamples = structures.reduce((sum, s) => sum + (s.examples ? s.examples.length : 0), 0);
  const countText = `${structures.length} 结构 · ${totalExamples} 例句`;
  const pos = sense.part_of_speech ? `[${sense.part_of_speech}] ` : '';
  const header = nodeHeader('sense', '📘', sense.id, pos + (sense.chinese_def || ''), countText);
  const children = [
    renderFullFields(sense, 'vocab_sense'),
    ...structures.map(renderStructureNode),
  ];
  return makeCollapsibleNode('sense', header, children);
}
```

将 `renderExerciseNode`：

```js
function renderExerciseNode(exercise) {
  const qt = exercise.question_type ? `[${exercise.question_type}] ` : '';
  const header = nodeHeader('exercise', '📝', exercise.id, qt + (exercise.question_text || ''), null);
  return makeCollapsibleNode('exercise', header, []);
}
```

替换为：

```js
function renderExerciseNode(exercise) {
  const qt = exercise.question_type ? `[${exercise.question_type}] ` : '';
  const header = nodeHeader('exercise', '📝', exercise.id, qt + (exercise.question_text || ''), null);
  return makeCollapsibleNode('exercise', header, [renderFullFields(exercise, 'vocab_exercise')]);
}
```

将 `renderWordNode`：

```js
function renderWordNode(word) {
  const senses = word.senses || [];
  const exercises = word.exercises || [];
  const parts = [];
  if (senses.length) parts.push(`${senses.length} 义项`);
  if (exercises.length) parts.push(`${exercises.length} 练习`);
  const countText = parts.length ? parts.join(' · ') : '空';
  const hsk = word.hsk_level ? ` HSK${word.hsk_level}` : '';
  const mainText = `${word.word} (${word.pinyin})${hsk}`;
  const header = nodeHeader('word', '🌿', word.id, mainText, countText);

  const children = [
    ...senses.map(renderSenseNode),
    ...exercises.map(renderExerciseNode),
  ];
  return makeCollapsibleNode('word', header, children);
}
```

替换为：

```js
function renderWordNode(word) {
  const senses = word.senses || [];
  const exercises = word.exercises || [];
  const parts = [];
  if (senses.length) parts.push(`${senses.length} 义项`);
  if (exercises.length) parts.push(`${exercises.length} 练习`);
  const countText = parts.length ? parts.join(' · ') : '空';
  const hsk = word.hsk_level ? ` HSK${word.hsk_level}` : '';
  const mainText = `${word.word} (${word.pinyin})${hsk}`;
  const header = nodeHeader('word', '🌿', word.id, mainText, countText);

  const children = [
    renderFullFields(word, 'vocab_word'),
    ...senses.map(renderSenseNode),
    ...exercises.map(renderExerciseNode),
  ];
  return makeCollapsibleNode('word', header, children);
}
```

- [ ] **Step 3：浏览器目测（强烈建议）**

刷新页面，预期：
- 展开 word #1001 → 子区第一项是 `📋 完整字段 ▾` 按钮
- 点击按钮展开，看到 word 的所有字段（id=1001、word=爱好、pinyin=àihào、audio_id=9001 → 🔊 #9001 (audio_resource 表)、...），底部有 `审计字段 (4) ▾` 按钮，可二级展开
- 展开任意 sense → `📋 完整字段` 中 `word_id` 行右侧附 `→ 🌿 #1001 爱好(àihào)` 灰字提示
- 展开任意 structure → `word_id` 提示 word，`sense_id` 提示对应义项
- 展开任意 example → `word_id`/`sense_id`/`structure_id` 三条 FK 都正确指向
- JSON 字段（translations、options、answers）以浅橙色等宽 JSON 块展示
- 点击 `📋` 按钮和 `审计字段` 按钮时，不会同时触发外层节点的折叠

- [ ] **Step 4：提交**

```bash
cd C:\Users\nano\Desktop\nano-gemini
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd-instance): add full-field detail panels with FK back-refs and JSON view"
```

---

## Task 6：最终验收 + 视觉打磨

**Files:**
- Modify (optional): `C:\Users\nano\Desktop\nano-gemini\docs\superpowers\specs\vocab-erd.html`

- [ ] **Step 1：按 spec 第八节走验收清单**

打开 `docs/superpowers/specs/vocab-erd.html`，逐项核对：

- [ ] 现有 ER 图正常显示，未受影响
- [ ] ER 图下方有 `<hr>`/分隔线和"实例数据示例：..."标题
- [ ] 实例区显示 2 张 word 节点（#1001 爱好 àihào + #1002 爱好 àihǎo），id 不同
- [ ] 默认折叠下，word #1001 显示 `[2 义项 · 1 练习]`，word #1002 显示 `[1 义项]`
- [ ] 点击 word #1001 展开，看到 2 个 sense 节点 + 1 个 exercise 节点
- [ ] 逐层展开，6 条例句最终可见（4001-4006）
- [ ] 任意节点 `📋 完整字段` 折叠按钮可展开，显示所有字段实际值
- [ ] FK 字段（word_id/sense_id/structure_id）的灰色反向提示能正确显示目标 id 和文本
- [ ] JSON 字段（translations/options/answers）以紧凑代码块展示
- [ ] 审计字段二级折叠默认收起，可展开
- [ ] 1920×1080 无横向滚动
- [ ] 无外部依赖（控制台 Network 面板仅 HTML 一项）

- [ ] **Step 2：若发现样式或对齐问题，逐一修正**

常见可能的问题与修法：

- **文本被截断过短**：调整 `truncate(mainText, 50)` 中的 50 为更大值（如 80）
- **节点缩进过深超出可视区**：调整 `.inst-node-children { margin-left: 14px }` 为更小值
- **JSON 块过长难看**：在 `.inst-json-block { max-height: 80px; overflow: auto }` 增加滚动
- **字段表格 label 列宽度不够**：调整 `.inst-field-row { grid-template-columns: 160px 1fr }` 的 160px

只在发现可见缺陷时调整，否则跳过此步。

- [ ] **Step 3：如有改动则提交**

```bash
cd C:\Users\nano\Desktop\nano-gemini
git add docs/superpowers/specs/vocab-erd.html
git commit -m "chore(erd-instance): final polish and acceptance pass"
```

无改动则跳过提交（不创建空提交）。

---

## 完成

完成后产出物：`docs/superpowers/specs/vocab-erd.html`（在原有 ER 图基础上追加了实例数据示例区）

打开方式：在 Windows 资源管理器中双击文件。
