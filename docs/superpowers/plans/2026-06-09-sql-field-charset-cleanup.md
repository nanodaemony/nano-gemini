# SQL Field Charset Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Simplify `sql/vocabulary.sql` and `sql/character.sql` by removing field-level charset/collation clauses while keeping table-level charset/collation defaults.

**Architecture:** This is a schema script formatting cleanup only. The table-level charset/collation declarations remain the source of default text encoding behavior, while individual text columns inherit those defaults.

**Tech Stack:** MySQL DDL scripts, Spring Boot project SQL resources.

---

## File Structure

- Modify: `sql/vocabulary.sql` — remove ` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci` from column definitions only.
- Modify: `sql/character.sql` — remove ` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci` from column definitions only.
- No Java code changes.
- No database migration runner changes.

### Task 1: Remove field-level charset/collation clauses

**Files:**
- Modify: `sql/vocabulary.sql`
- Modify: `sql/character.sql`

- [ ] **Step 1: Confirm the target pattern exists before editing**

Run:
```bash
python - <<'PY'
from pathlib import Path
for file in [Path('sql/vocabulary.sql'), Path('sql/character.sql')]:
    text = file.read_text(encoding='utf-8')
    count = text.count(' CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci')
    print(f'{file}: {count}')
PY
```

Expected: both files print a count greater than `0`.

- [ ] **Step 2: Remove only the field-level pattern**

Run:
```bash
python - <<'PY'
from pathlib import Path
pattern = ' CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci'
for file in [Path('sql/vocabulary.sql'), Path('sql/character.sql')]:
    text = file.read_text(encoding='utf-8')
    updated = text.replace(pattern, '')
    file.write_text(updated, encoding='utf-8')
PY
```

This exact replacement removes clauses such as:
```sql
`word` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '词汇（如：啊）'
```

and leaves them as:
```sql
`word` varchar(50) NOT NULL COMMENT '词汇（如：啊）'
```

- [ ] **Step 3: Verify table-level charset/collation remains**

Run:
```bash
python - <<'PY'
from pathlib import Path
for file in [Path('sql/vocabulary.sql'), Path('sql/character.sql')]:
    text = file.read_text(encoding='utf-8')
    table_charset_lines = [line for line in text.splitlines() if 'CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci' in line or 'DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci' in line]
    print(f'{file}: {len(table_charset_lines)} table-level charset lines')
    for line in table_charset_lines:
        print(line)
PY
```

Expected:
- `sql/vocabulary.sql` still has table-ending charset/collation declarations.
- `sql/character.sql` still has table-ending charset/collation declarations.

- [ ] **Step 4: Verify no target field-level clauses remain**

Run:
```bash
python - <<'PY'
from pathlib import Path
pattern = ' CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci'
failed = False
for file in [Path('sql/vocabulary.sql'), Path('sql/character.sql')]:
    text = file.read_text(encoding='utf-8')
    count = text.count(pattern)
    print(f'{file}: {count}')
    if count != 0:
        failed = True
raise SystemExit(1 if failed else 0)
PY
```

Expected:
```text
sql/vocabulary.sql: 0
sql/character.sql: 0
```

- [ ] **Step 5: Review the diff**

Run:
```bash
git diff -- sql/biz_vocabulary.sql sql/biz_character.sql
```

Expected: the diff only removes `CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci` from column definitions. It must not remove table-level charset/collation declarations and must not change column types, comments, defaults, indexes, or table options.

- [ ] **Step 6: Commit if requested by the user**

Only commit if the user asks for a commit. If requested, run:
```bash
git add sql/biz_vocabulary.sql sql/biz_character.sql docs/superpowers/plans/2026-06-09-sql-field-charset-cleanup.md
git commit -m "chore(sql): simplify field charset declarations

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

## Self-Review

- Spec coverage: The plan removes only field-level `CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci` from `sql/vocabulary.sql` and `sql/character.sql`, and explicitly preserves table-level charset/collation declarations.
- Placeholder scan: No placeholder steps remain.
- Type/name consistency: File paths and target pattern are consistent across all steps.
