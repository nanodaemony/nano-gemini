# 词汇模块 ER 图（HTML 单页）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 产出一个自包含、可双击打开的 HTML 单页，用于可视化 `VocabWordController` 涉及的 7 张数据表 ER 图，含字段中文释义、JSON 内部结构展示与 hover 高亮交互。

**Architecture:** 数据驱动渲染——表元数据放在内联 JS 对象 `SCHEMA` 中，DOM 由 JS 在 `DOMContentLoaded` 时生成。CSS Grid 控制布局（顶部独立/资源表，主区自上而下分层）。无外部依赖、无构建步骤。

**Tech Stack:** 纯 HTML5 + 内联 CSS + 内联原生 JS (ES2015+)，目标浏览器 Chrome/Edge 现代版本。

**Spec 参考:** `docs/superpowers/specs/2026-06-06-vocab-erd-design.md`

## 关于"测试"

本计划产出物是单页 HTML 文档，无后端逻辑、无测试框架。每个任务的"验证"步骤为**浏览器目测**，按 spec 第七节的验收标准检查。每完成一阶段必须在浏览器打开页面截图或目测确认后才能提交。

## 文件结构

仅产出一个文件：

- 创建：`docs/superpowers/specs/vocab-erd.html`

文件内部组织（按顺序）：

1. `<head>` — meta、title、内联 `<style>`
2. `<body>` 框架 — `<header>`（标题 + 图例）、`<main id="diagram">`
3. 内联 `<script>` —
   - `SCHEMA`：7 张表元数据
   - `JSON_SHAPES`：3 个 JSON 内部结构
   - 渲染函数：`renderTable`、`renderField`、`renderJsonShape`、`renderAuditToggle`
   - 交互绑定：hover 高亮、折叠展开
   - `DOMContentLoaded` 入口

---

## Task 1：创建 HTML 骨架

**Files:**
- Create: `docs/superpowers/specs/vocab-erd.html`

- [ ] **Step 1：写出最小 HTML 骨架**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <title>词汇模块 ER 图</title>
  <style>
    /* 后续任务填充 */
    body { margin: 0; font-family: -apple-system, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; background: #f5f6f8; color: #222; }
    header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #e3e5e8; }
    header h1 { margin: 0; font-size: 18px; }
    main#diagram { padding: 24px; }
  </style>
</head>
<body>
  <header>
    <h1>词汇模块 ER 图（VocabWordController 涉及表）</h1>
  </header>
  <main id="diagram">
    <p style="color:#888">渲染中...</p>
  </main>
  <script>
    // 后续任务填充
  </script>
</body>
</html>
```

- [ ] **Step 2：浏览器打开目测**

双击 `docs/superpowers/specs/vocab-erd.html`，预期：浅灰背景，顶部白色横条显示标题，主区显示"渲染中..."灰色提示。

- [ ] **Step 3：提交**

```bash
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd): bootstrap vocab ERD HTML skeleton"
```

---

## Task 2：填入完整的 SCHEMA 数据

**Files:**
- Modify: `docs/superpowers/specs/vocab-erd.html`（在 `<script>` 内追加）

- [ ] **Step 1：在 `<script>` 内添加 SCHEMA 常量**

替换原 `<script>` 内容为下方完整的 SCHEMA（字段数据全部来自 spec 第五节）：

```js
const SCHEMA = {
  vocab_word: {
    role: 'main',          // main | sub | resource | standalone
    aliasZh: '词汇主表',
    fields: [
      { name: 'id',               type: 'int',          pk: true,  notNull: true,  zh: '词汇唯一ID' },
      { name: 'word',             type: 'varchar(50)',              notNull: true,  zh: '词汇' },
      { name: 'word_traditional', type: 'varchar(50)',                              zh: '繁体词汇' },
      { name: 'pinyin',           type: 'varchar(100)',                             zh: '标准拼音（含声调）' },
      { name: 'audio_id',         type: 'bigint',       fk: 'audio_resource.id',   zh: '词汇读音音频资源ID' },
      { name: 'hsk_level',        type: 'varchar(20)',                              zh: 'HSK等级（"1"-"9"）' },
      { name: 'status',           type: 'int',                                       zh: '状态：1=可用，0=已删除' },
      { name: 'publish_status',   type: 'varchar(20)',                              zh: '发布状态：unpublished/published' },
      { name: 'edit_status',      type: 'varchar(20)',                              zh: '编辑状态：draft/reviewed/published' },
      { name: 'draft_content',    type: 'json',         jsonShape: 'draftContent', zh: '草稿内容JSON（含主表和子表快照）' },
    ],
    auditFields: [
      { name: 'create_by',   type: 'varchar', zh: '创建人' },
      { name: 'update_by',   type: 'varchar', zh: '更新人' },
      { name: 'create_time', type: 'timestamp', zh: '创建时间' },
      { name: 'update_time', type: 'timestamp', zh: '更新时间' },
    ],
  },

  vocab_sense: {
    role: 'sub',
    aliasZh: '义项',
    fields: [
      { name: 'id',               type: 'int',    pk: true, notNull: true,                          zh: '义项ID' },
      { name: 'word_id',          type: 'int',    fk: 'vocab_word.id', notNull: true,               zh: '所属词汇ID' },
      { name: 'part_of_speech',   type: 'varchar(50)',                                              zh: '词性' },
      { name: 'chinese_def',      type: 'text',                                                     zh: '中文释义' },
      { name: 'def_audio_id',     type: 'bigint', fk: 'audio_resource.id',                          zh: '中文释义音频资源ID' },
      { name: 'translations',     type: 'json',   jsonShape: 'textTranslation',                     zh: '外文翻译列表 → TextTranslation[]' },
      { name: 'synonyms',         type: 'text',                                                     zh: '近义词列表' },
      { name: 'antonyms',         type: 'text',                                                     zh: '反义词列表' },
      { name: 'related_forward',  type: 'text',                                                     zh: '正序关联词汇' },
      { name: 'related_backward', type: 'text',                                                     zh: '逆序关联词汇' },
      { name: 'sense_order',      type: 'int',    notNull: true,                                    zh: '义项排序权重' },
      { name: 'status',           type: 'int',                                                      zh: '状态' },
    ],
    auditFields: [
      { name: 'create_time', type: 'timestamp', zh: '创建时间' },
      { name: 'update_time', type: 'timestamp', zh: '更新时间' },
    ],
  },

  vocab_structure: {
    role: 'sub',
    aliasZh: '结构搭配',
    fields: [
      { name: 'id',              type: 'int',    pk: true, notNull: true,                zh: '结构搭配ID' },
      { name: 'word_id',         type: 'int',    fk: 'vocab_word.id', notNull: true,     zh: '所属词汇ID' },
      { name: 'sense_id',        type: 'int',    fk: 'vocab_sense.id', notNull: true,    zh: '所属义项ID' },
      { name: 'pattern',         type: 'varchar(255)', notNull: true,                    zh: '结构搭配文案' },
      { name: 'structure_order', type: 'int',    notNull: true,                          zh: '搭配排序权重' },
      { name: 'status',          type: 'int',                                            zh: '状态' },
    ],
    auditFields: [
      { name: 'create_time', type: 'timestamp', zh: '创建时间' },
      { name: 'update_time', type: 'timestamp', zh: '更新时间' },
    ],
  },

  vocab_example: {
    role: 'sub',
    aliasZh: '例句',
    fields: [
      { name: 'id',             type: 'int',    pk: true, notNull: true,                    zh: '例句唯一ID' },
      { name: 'word_id',        type: 'int',    fk: 'vocab_word.id', notNull: true,         zh: '所属词汇ID' },
      { name: 'sense_id',       type: 'int',    fk: 'vocab_sense.id', notNull: true,        zh: '所属义项ID' },
      { name: 'structure_id',   type: 'int',    fk: 'vocab_structure.id', notNull: true,    zh: '所属结构搭配ID' },
      { name: 'sentence',       type: 'text',   notNull: true,                              zh: '例句中文文案' },
      { name: 'audio_id',       type: 'bigint', fk: 'audio_resource.id',                    zh: '例句音频资源ID' },
      { name: 'pinyin',         type: 'varchar(500)',                                       zh: '例句拼音' },
      { name: 'translations',   type: 'json',   jsonShape: 'textTranslation',               zh: '例句外文翻译列表 → TextTranslation[]' },
      { name: 'example_order',  type: 'int',    notNull: true,                              zh: '例句排序权重' },
      { name: 'status',         type: 'int',                                                zh: '状态' },
    ],
    auditFields: [
      { name: 'create_time', type: 'timestamp', zh: '创建时间' },
      { name: 'update_time', type: 'timestamp', zh: '更新时间' },
    ],
  },

  vocab_exercise: {
    role: 'sub',
    aliasZh: '练习题',
    fields: [
      { name: 'id',              type: 'int',  pk: true, notNull: true,             zh: '练习题目唯一ID' },
      { name: 'word_id',         type: 'int',  fk: 'vocab_word.id', notNull: true,  zh: '所属词汇ID' },
      { name: 'question_type',   type: 'varchar(20)', notNull: true,                zh: '题目类型' },
      { name: 'question_text',   type: 'text',  notNull: true,                      zh: '练习题干描述' },
      { name: 'options',         type: 'json', jsonShape: 'exerciseOption',         zh: '选项列表 → ExerciseOption[]' },
      { name: 'answers',         type: 'json',                                      zh: '答案列表（字符串数组）' },
      { name: 'exercise_order',  type: 'int',  notNull: true,                       zh: '练习题目排序权重' },
      { name: 'status',          type: 'int',                                       zh: '状态' },
    ],
    auditFields: [
      { name: 'create_time', type: 'timestamp', zh: '创建时间' },
      { name: 'update_time', type: 'timestamp', zh: '更新时间' },
    ],
  },

  vocab_outline_record: {
    role: 'standalone',
    aliasZh: '纲外词记录',
    fields: [
      { name: 'id',           type: 'int',          pk: true, notNull: true, zh: '主键ID' },
      { name: 'word',         type: 'varchar(50)',  notNull: true,           zh: '词汇文本' },
      { name: 'search_count', type: 'int',          notNull: true,           zh: '未搜到次数' },
      { name: 'status',       type: 'int',                                   zh: '处理状态：0=未处理，1=已处理' },
    ],
    auditFields: [
      { name: 'create_time', type: 'timestamp', zh: '创建时间' },
      { name: 'update_time', type: 'timestamp', zh: '更新时间' },
    ],
  },

  audio_resource: {
    role: 'resource',
    aliasZh: '音频资源',
    fields: [
      { name: 'id',           type: 'bigint',       pk: true, notNull: true, zh: '主键' },
      { name: 'text_content', type: 'text',         notNull: true,           zh: '音频对应的文字内容' },
      { name: 'source_type',  type: 'varchar(50)',  notNull: true,           zh: '来源类型：tts/upload' },
      { name: 'file_url',     type: 'varchar(500)', notNull: true,           zh: '音频文件地址' },
      { name: 'file_format',  type: 'varchar(20)',                           zh: '文件格式：mp3/wav/m4a' },
      { name: 'file_size',    type: 'bigint',                                zh: '文件大小（字节）' },
      { name: 'status',       type: 'int',                                   zh: '有效状态：1=有效，0=无效' },
    ],
    auditFields: [
      { name: 'create_time', type: 'timestamp', zh: '创建时间' },
      { name: 'update_time', type: 'timestamp', zh: '更新时间' },
    ],
  },
};

const JSON_SHAPES = {
  textTranslation: {
    title: 'TextTranslation',
    note: '语种 + 翻译文本（一个翻译条目）',
    fields: [
      { name: 'language',    type: 'string', zh: '语种枚举（LanguageCodeEnum.code）' },
      { name: 'translation', type: 'string', zh: '翻译文案' },
    ],
  },
  exerciseOption: {
    title: 'ExerciseOption',
    note: '练习题的一个选项',
    fields: [
      { name: 'option', type: 'string', zh: '选项标识（A/B/C/D）' },
      { name: 'text',   type: 'string', zh: '选项文案' },
    ],
  },
  draftContent: {
    title: 'DraftContent（整词草稿快照）',
    note: '保存在 vocab_word.draft_content；发布时回写主表 + 子表',
    fields: [
      { name: 'word / word_traditional / pinyin / audio_id / hsk_level', type: '字段', zh: '同 vocab_word 同名字段' },
      { name: 'senses[]',     type: 'array',  zh: '义项快照（含 structures[] 和 examples[]）' },
      { name: 'exercises[]',  type: 'array',  zh: '练习题快照' },
    ],
  },
};
```

- [ ] **Step 2：浏览器刷新目测**

刷新页面，预期：页面外观不变（仍只显示"渲染中..."），打开浏览器控制台无 JS 报错。在控制台输入 `Object.keys(SCHEMA)` 应返回 7 个表名。

- [ ] **Step 3：提交**

```bash
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd): add full SCHEMA and JSON_SHAPES data"
```

---

## Task 3：实现表卡片渲染（基础版，不含 JSON 与审计折叠）

**Files:**
- Modify: `docs/superpowers/specs/vocab-erd.html`

- [ ] **Step 1：扩充 `<style>`，加入卡片与字段样式**

将 `<style>` 替换为：

```css
body { margin: 0; font-family: -apple-system, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; background: #f5f6f8; color: #222; }
header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #e3e5e8; }
header h1 { margin: 0 0 8px; font-size: 18px; }
header .legend { font-size: 12px; color: #666; display: flex; gap: 16px; flex-wrap: wrap; }
header .legend .item { display: inline-flex; align-items: center; gap: 4px; }
header .legend .badge { display: inline-block; width: 10px; height: 10px; border-radius: 2px; }

main#diagram { padding: 24px; display: flex; flex-direction: column; gap: 24px; }
.row { display: flex; gap: 24px; flex-wrap: wrap; justify-content: center; align-items: flex-start; }

.table-card { background: #fff; border: 2px solid #ccd; border-radius: 8px; min-width: 320px; max-width: 480px; box-shadow: 0 1px 3px rgba(0,0,0,.06); overflow: hidden; transition: border-color .15s, box-shadow .15s; }
.table-card.role-main      { border-color: #2f6fd6; }
.table-card.role-sub       { border-color: #2f9b6f; }
.table-card.role-resource  { border-color: #8a4fc9; }
.table-card.role-standalone{ border-color: #6c757d; }

.table-card .header { padding: 8px 12px; font-weight: 600; display: flex; align-items: baseline; gap: 8px; }
.table-card.role-main      .header { background: #e8f0fc; }
.table-card.role-sub       .header { background: #e6f6ee; }
.table-card.role-resource  .header { background: #f0e8fa; }
.table-card.role-standalone.header,
.table-card.role-standalone .header { background: #eceef1; }
.table-card .header .name  { font-family: ui-monospace, Consolas, monospace; font-size: 14px; }
.table-card .header .alias { font-size: 12px; color: #555; font-weight: normal; }
.table-card .header .role-tag { margin-left: auto; font-size: 11px; padding: 2px 6px; border-radius: 4px; background: rgba(0,0,0,.06); }

.table-card .fields { display: flex; flex-direction: column; }
.field-row { display: grid; grid-template-columns: 18px 1fr auto; gap: 6px; padding: 4px 12px; font-size: 12px; border-top: 1px solid #f1f2f4; align-items: baseline; }
.field-row:first-child { border-top: none; }
.field-row .icon { font-size: 11px; color: #888; }
.field-row .name { font-family: ui-monospace, Consolas, monospace; }
.field-row .name.notnull::after { content: ' *'; color: #c0392b; font-weight: bold; }
.field-row .meta { display: flex; gap: 4px; align-items: center; flex-wrap: wrap; justify-content: flex-end; }
.field-row .zh { grid-column: 2 / -1; color: #777; font-size: 11px; padding-top: 1px; }
.field-row .fk { grid-column: 2 / -1; font-size: 11px; color: #2f6fd6; font-family: ui-monospace, Consolas, monospace; }

.type-badge { font-size: 10px; padding: 1px 5px; border-radius: 3px; font-family: ui-monospace, Consolas, monospace; white-space: nowrap; }
.type-int       { background: #e3edfb; color: #2f6fd6; }
.type-bigint    { background: #e3edfb; color: #2f6fd6; }
.type-varchar   { background: #e6f6ee; color: #2f9b6f; }
.type-text      { background: #fbf3d6; color: #9a7300; }
.type-json      { background: #fde6cc; color: #b25a00; }
.type-timestamp { background: #ececec; color: #555; }
.type-string    { background: #e6f6ee; color: #2f9b6f; }
```

- [ ] **Step 2：在 `<script>` 末尾追加渲染函数（基础版）**

```js
function typeClass(type) {
  if (!type) return '';
  if (type.startsWith('varchar')) return 'type-varchar';
  if (type === 'int' || type === 'bigint') return `type-${type}`;
  if (type === 'text' || type === 'json' || type === 'timestamp' || type === 'string') return `type-${type}`;
  return '';
}

function el(tag, attrs, ...children) {
  const node = document.createElement(tag);
  if (attrs) {
    for (const k of Object.keys(attrs)) {
      if (k === 'class') node.className = attrs[k];
      else if (k === 'dataset') Object.assign(node.dataset, attrs[k]);
      else if (k.startsWith('on')) node.addEventListener(k.slice(2), attrs[k]);
      else node.setAttribute(k, attrs[k]);
    }
  }
  for (const c of children) {
    if (c == null || c === false) continue;
    node.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
  }
  return node;
}

function renderField(tableName, f) {
  const icon = f.pk ? '🔑' : (f.fk ? '🔗' : '');
  const row = el('div', { class: 'field-row', dataset: { table: tableName, field: f.name } },
    el('span', { class: 'icon' }, icon),
    el('span', { class: `name${f.notNull ? ' notnull' : ''}` }, f.name),
    el('span', { class: 'meta' },
      el('span', { class: `type-badge ${typeClass(f.type)}` }, f.type),
    ),
    el('span', { class: 'zh' }, f.zh || ''),
    f.fk ? el('span', { class: 'fk' }, `→ ${f.fk}`) : null,
  );
  return row;
}

function roleLabel(role) {
  return { main: '主表', sub: '子表', resource: '资源表', standalone: '独立表' }[role] || role;
}

function renderTable(name, schema) {
  const card = el('div', { class: `table-card role-${schema.role}`, dataset: { table: name } },
    el('div', { class: 'header' },
      el('span', { class: 'name' }, name),
      el('span', { class: 'alias' }, schema.aliasZh),
      el('span', { class: 'role-tag' }, roleLabel(schema.role)),
    ),
    el('div', { class: 'fields' },
      ...schema.fields.map(f => renderField(name, f)),
      // 审计字段后续任务添加
    ),
  );
  return card;
}

function render() {
  const root = document.getElementById('diagram');
  root.innerHTML = '';

  // 顶部行：独立表 + 资源表
  const topRow = el('div', { class: 'row' },
    renderTable('vocab_outline_record', SCHEMA.vocab_outline_record),
    renderTable('audio_resource', SCHEMA.audio_resource),
  );

  // 主区行 1：vocab_word（中）与 vocab_exercise（右）
  const mainRow1 = el('div', { class: 'row' },
    renderTable('vocab_word', SCHEMA.vocab_word),
    renderTable('vocab_exercise', SCHEMA.vocab_exercise),
  );

  const mainRow2 = el('div', { class: 'row' }, renderTable('vocab_sense',     SCHEMA.vocab_sense));
  const mainRow3 = el('div', { class: 'row' }, renderTable('vocab_structure', SCHEMA.vocab_structure));
  const mainRow4 = el('div', { class: 'row' }, renderTable('vocab_example',   SCHEMA.vocab_example));

  root.append(topRow, mainRow1, mainRow2, mainRow3, mainRow4);
}

document.addEventListener('DOMContentLoaded', render);
```

- [ ] **Step 3：扩充 `<header>` 加入图例**

将 `<header>` 替换为：

```html
<header>
  <h1>词汇模块 ER 图（VocabWordController 涉及表）</h1>
  <div class="legend">
    <span class="item"><span class="badge" style="background:#2f6fd6"></span>主表</span>
    <span class="item"><span class="badge" style="background:#2f9b6f"></span>子表</span>
    <span class="item"><span class="badge" style="background:#8a4fc9"></span>资源表</span>
    <span class="item"><span class="badge" style="background:#6c757d"></span>独立表</span>
    <span class="item">🔑 主键</span>
    <span class="item">🔗 外键</span>
    <span class="item"><span style="color:#c0392b">*</span> NOT NULL</span>
  </div>
</header>
```

- [ ] **Step 4：浏览器目测**

刷新页面，预期：
- 顶部图例齐全
- 顶部行：`vocab_outline_record` 和 `audio_resource` 两张卡片并排（灰色/紫色边框）
- 主区从上到下：`vocab_word`+`vocab_exercise` 行、`vocab_sense`、`vocab_structure`、`vocab_example`
- 每张卡片显示表名、中文别名、角色徽标
- 字段行显示：图标 / 名称 / 类型徽标 / 中文释义；外键字段下方显示 `→ 表名.字段`（蓝色）
- 审计字段此时尚未出现（下一任务添加）

- [ ] **Step 5：提交**

```bash
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd): render table cards with fields and layout"
```

---

## Task 4：增加审计字段折叠

**Files:**
- Modify: `docs/superpowers/specs/vocab-erd.html`

- [ ] **Step 1：在 `<style>` 末尾追加折叠样式**

```css
.audit-toggle { padding: 4px 12px; font-size: 11px; color: #2f6fd6; cursor: pointer; user-select: none; border-top: 1px dashed #e3e5e8; background: #fafbfc; }
.audit-toggle:hover { background: #f1f4f9; }
.audit-fields { display: none; background: #fafbfc; }
.audit-fields.open { display: flex; flex-direction: column; }
.audit-fields .field-row { background: #fafbfc; }
```

- [ ] **Step 2：在 `<script>` 中添加 `renderAuditBlock` 并把它接入 `renderTable`**

在 `renderField` 之后追加：

```js
function renderAuditBlock(tableName, auditFields) {
  if (!auditFields || auditFields.length === 0) return null;
  const list = el('div', { class: 'audit-fields' },
    ...auditFields.map(f => renderField(tableName, f)),
  );
  const toggle = el('div', { class: 'audit-toggle' }, `审计字段 (${auditFields.length}) ▾`);
  toggle.addEventListener('click', () => {
    const open = list.classList.toggle('open');
    toggle.textContent = `审计字段 (${auditFields.length}) ${open ? '▴' : '▾'}`;
  });
  const wrap = document.createDocumentFragment();
  wrap.appendChild(toggle);
  wrap.appendChild(list);
  return wrap;
}
```

修改 `renderTable`，将原来的：

```js
    el('div', { class: 'fields' },
      ...schema.fields.map(f => renderField(name, f)),
      // 审计字段后续任务添加
    ),
```

替换为：

```js
    el('div', { class: 'fields' },
      ...schema.fields.map(f => renderField(name, f)),
    ),
    renderAuditBlock(name, schema.auditFields) || el('span'),
```

注意：`el()` 现已接收 `DocumentFragment`，需要让 `el` 支持。修改 `el` 的子节点处理为：

```js
  for (const c of children) {
    if (c == null || c === false) continue;
    if (typeof c === 'string') node.appendChild(document.createTextNode(c));
    else node.appendChild(c);
  }
```

（如果 Task 3 已经是这样，跳过此步骤。）

- [ ] **Step 3：浏览器目测**

刷新页面。预期：
- 每张卡片底部出现"审计字段 (N) ▾"的浅蓝色按钮
- 点击展开，显示对应审计字段；再次点击收起
- `vocab_word` 显示 4 个审计字段（create_by/update_by/create_time/update_time）
- 其他表显示 2 个审计字段（create_time/update_time）

- [ ] **Step 4：提交**

```bash
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd): add collapsible audit fields block"
```

---

## Task 5：JSON 字段可展开区

**Files:**
- Modify: `docs/superpowers/specs/vocab-erd.html`

- [ ] **Step 1：追加 JSON 展开区样式**

在 `<style>` 末尾追加：

```css
.field-row.json-row { cursor: pointer; }
.field-row.json-row:hover { background: #fff7ed; }
.field-row.json-row .name::after { content: ' ▾'; color: #b25a00; font-size: 10px; }
.field-row.json-row.open .name::after { content: ' ▴'; }

.json-shape { display: none; padding: 6px 12px 8px 36px; background: #fffaf2; border-top: 1px dashed #f0d9b8; border-bottom: 1px solid #f1f2f4; }
.json-shape.open { display: block; }
.json-shape .title { font-weight: 600; font-size: 12px; color: #b25a00; margin-bottom: 2px; }
.json-shape .note  { font-size: 11px; color: #8a6a3a; margin-bottom: 6px; }
.json-shape .sub-field { display: grid; grid-template-columns: 1fr auto; gap: 6px; padding: 2px 0; font-size: 11px; }
.json-shape .sub-field .sname { font-family: ui-monospace, Consolas, monospace; }
.json-shape .sub-field .stype { font-family: ui-monospace, Consolas, monospace; color: #b25a00; font-size: 10px; }
.json-shape .sub-field .szh { grid-column: 1 / -1; color: #777; font-size: 11px; }
```

- [ ] **Step 2：实现 `renderJsonShape` 并改造 `renderField`**

在 `<script>` 中：

```js
function renderJsonShape(shapeKey) {
  const shape = JSON_SHAPES[shapeKey];
  if (!shape) return null;
  return el('div', { class: 'json-shape' },
    el('div', { class: 'title' }, shape.title),
    shape.note ? el('div', { class: 'note' }, shape.note) : null,
    ...shape.fields.map(sf =>
      el('div', { class: 'sub-field' },
        el('span', { class: 'sname' }, sf.name),
        el('span', { class: 'stype' }, sf.type),
        el('span', { class: 'szh' }, sf.zh || ''),
      ),
    ),
  );
}
```

修改 `renderField` 的返回逻辑，让 JSON 字段返回一个**包含 row + shape 的 fragment**，并在 row 上绑定点击：

```js
function renderField(tableName, f) {
  const icon = f.pk ? '🔑' : (f.fk ? '🔗' : '');
  const isJson = f.type === 'json';
  const row = el('div', {
      class: `field-row${isJson ? ' json-row' : ''}`,
      dataset: { table: tableName, field: f.name },
    },
    el('span', { class: 'icon' }, icon),
    el('span', { class: `name${f.notNull ? ' notnull' : ''}` }, f.name),
    el('span', { class: 'meta' },
      el('span', { class: `type-badge ${typeClass(f.type)}` }, f.type),
    ),
    el('span', { class: 'zh' }, f.zh || ''),
    f.fk ? el('span', { class: 'fk' }, `→ ${f.fk}`) : null,
  );

  if (!isJson || !f.jsonShape) return row;

  const shape = renderJsonShape(f.jsonShape);
  if (!shape) return row;

  row.addEventListener('click', () => {
    row.classList.toggle('open');
    shape.classList.toggle('open');
  });

  const frag = document.createDocumentFragment();
  frag.appendChild(row);
  frag.appendChild(shape);
  return frag;
}
```

- [ ] **Step 3：浏览器目测**

刷新页面。预期：
- 类型为 `json` 的字段行（如 `vocab_word.draft_content`、`vocab_sense.translations`、`vocab_example.translations`、`vocab_exercise.options`、`vocab_exercise.answers`）有橙色 `▾` 指示
- 点击展开，显示对应 JSON 结构标题（如"TextTranslation"）+ 字段列表 + 中文说明，浅橙背景
- 再次点击收起
- 注意：`vocab_exercise.answers` 没有指定 `jsonShape`，应当点击无反应（无展开区）

- [ ] **Step 4：提交**

```bash
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd): add collapsible JSON shape detail under json fields"
```

---

## Task 6：hover 高亮交互

**Files:**
- Modify: `docs/superpowers/specs/vocab-erd.html`

- [ ] **Step 1：追加高亮样式**

在 `<style>` 末尾追加：

```css
.table-card.highlight { box-shadow: 0 0 0 3px rgba(47, 111, 214, .25); border-color: #2f6fd6 !important; }
.table-card.secondary-highlight { box-shadow: 0 0 0 2px rgba(47, 155, 111, .2); }
.field-row.highlight { background: #fff3cd; }
```

- [ ] **Step 2：在 `<script>` 中追加 hover 绑定**

```js
function parseFk(fkStr) {
  if (!fkStr) return null;
  const [table, field] = fkStr.split('.');
  return { table, field };
}

function bindHover(root) {
  // 1) hover FK 字段 -> 高亮目标表卡 + 目标字段
  root.querySelectorAll('.field-row').forEach(row => {
    const fkText = row.querySelector('.fk');
    if (!fkText) return;
    const target = parseFk(fkText.textContent.replace('→ ', '').trim());
    if (!target) return;
    const card = root.querySelector(`.table-card[data-table="${target.table}"]`);
    const field = root.querySelector(`.field-row[data-table="${target.table}"][data-field="${target.field}"]`);
    row.addEventListener('mouseenter', () => {
      if (card) card.classList.add('highlight');
      if (field) field.classList.add('highlight');
    });
    row.addEventListener('mouseleave', () => {
      if (card) card.classList.remove('highlight');
      if (field) field.classList.remove('highlight');
    });
  });

  // 2) hover 表头 -> 高亮该表引用的 / 引用该表的所有表
  root.querySelectorAll('.table-card').forEach(card => {
    const tableName = card.dataset.table;
    const referencedTables = new Set();
    const schema = SCHEMA[tableName];
    if (schema) {
      schema.fields.forEach(f => {
        if (f.fk) referencedTables.add(f.fk.split('.')[0]);
      });
    }
    const referencingTables = new Set();
    Object.entries(SCHEMA).forEach(([otherName, otherSchema]) => {
      otherSchema.fields.forEach(f => {
        if (f.fk && f.fk.split('.')[0] === tableName) referencingTables.add(otherName);
      });
    });
    const allRelated = new Set([...referencedTables, ...referencingTables]);

    const header = card.querySelector('.header');
    header.addEventListener('mouseenter', () => {
      allRelated.forEach(t => {
        const c = root.querySelector(`.table-card[data-table="${t}"]`);
        if (c) c.classList.add('secondary-highlight');
      });
    });
    header.addEventListener('mouseleave', () => {
      allRelated.forEach(t => {
        const c = root.querySelector(`.table-card[data-table="${t}"]`);
        if (c) c.classList.remove('secondary-highlight');
      });
    });
  });
}
```

修改 `render()` 末尾，在 `root.append(...)` 之后追加：

```js
  bindHover(root);
```

- [ ] **Step 3：浏览器目测**

刷新页面。逐项检查：
- hover `vocab_sense.word_id` 字段 → `vocab_word` 卡片整体出现蓝色发光边框，`vocab_word.id` 字段变浅黄
- hover `vocab_example.structure_id` → `vocab_structure` 卡片高亮，`vocab_structure.id` 高亮
- hover `vocab_word` 表头 → 引用它的 4 张表（sense/structure/example/exercise）和它引用的 1 张表（audio_resource）都加上次级绿色发光
- hover `audio_resource` 表头 → vocab_word、vocab_sense、vocab_example 三张表加次级高亮
- hover `vocab_outline_record` 表头 → 没有任何其他表高亮（它无 FK）

- [ ] **Step 4：提交**

```bash
git add docs/superpowers/specs/vocab-erd.html
git commit -m "feat(erd): add hover highlight for foreign keys and table relations"
```

---

## Task 7：最终验收 + 视觉打磨

**Files:**
- Modify: `docs/superpowers/specs/vocab-erd.html`

- [ ] **Step 1：按 spec 第七节走一遍验收清单**

打开 `docs/superpowers/specs/vocab-erd.html`，逐项核对：

- [ ] 单 HTML 文件，无外部依赖（控制台 Network 面板应仅一项 HTML 请求）
- [ ] 7 张表卡片全部可见，所有字段（按 spec 第五节）及中文释义齐全
- [ ] 5 处 JSON 字段（draft_content / 2 处 translations / options / answers）— 其中 4 处可展开查看内部结构
- [ ] 审计字段默认折叠，可展开
- [ ] hover 外键字段时目标表卡片高亮
- [ ] hover 表头时关联表次级高亮
- [ ] 在 1920×1080 屏幕下无横向滚动

- [ ] **Step 2：若发现样式或对齐问题，逐一修正**

常见问题与修法：

- **卡片宽度不一致导致行错位**：调整 `.table-card { min-width: 320px; max-width: 480px }`，必要时改为 `flex: 1 1 320px`
- **字段太挤**：增加 `.field-row { padding: 6px 12px }`
- **JSON 展开区缩进不对齐**：调整 `.json-shape { padding-left: 36px }`
- **审计按钮文字与图标对齐**：检查 `▾`/`▴` 字符是否使用同宽

如无问题，跳过此步。

- [ ] **Step 3：核对 spec 验收清单全部通过后提交**

```bash
git add docs/superpowers/specs/vocab-erd.html
git commit -m "chore(erd): final polish and acceptance pass"
```

---

## 完成

完成后产出物：`docs/superpowers/specs/vocab-erd.html`

打开方式：在 Windows 资源管理器中双击文件，或在浏览器中按 `Ctrl+O` 打开。
