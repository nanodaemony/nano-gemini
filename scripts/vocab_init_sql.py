import pandas as pd
import json
import os
import sys
from datetime import datetime

# ---------- 参数检查 ----------
if len(sys.argv) != 2:
    print("用法：")
    print("  python excel_to_vocab_sql.py <Excel文件路径>")
    sys.exit(1)

EXCEL_PATH = sys.argv[1]
SHEET_NAME = "sheet"

# 生成 SQL 文件名（与 Excel 同名）
BASE_NAME = os.path.splitext(os.path.basename(EXCEL_PATH))[0]
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_SQL = os.path.join(SCRIPT_DIR, f"vocab_{BASE_NAME}.sql")

# ---------- ID 生成器 ----------
word_id_seq = 1
sense_id_seq = 1
structure_id_seq = 1
example_id_seq = 1

word_map = {}
sense_map = {}

sql_lines = []
sql_lines.append("-- Vocab SQL Generated at " + datetime.now().isoformat())
sql_lines.append("SET NAMES utf8mb4;\n")

# ---------- 工具函数 ----------
def esc(s):
    if s is None or pd.isna(s):
        return "NULL"
    return "'" + str(s).replace("'", "\\'").replace("\n", "\\n") + "'"


def insert(table, data):
    cols = ", ".join(data.keys())
    vals = ", ".join(data.values())
    sql_lines.append(f"INSERT INTO `{table}` ({cols}) VALUES ({vals});")


# ---------- 读取 Excel ----------
try:
    df = pd.read_excel(EXCEL_PATH, sheet_name=SHEET_NAME)
except Exception as e:
    print("❌ 读取 Excel 失败：", e)
    sys.exit(1)

# ---------- 处理数据 ----------
for _, row in df.iterrows():
    head_word = row.get("词头")
    if not head_word:
        continue

    # ---- vocab_word ----
    if head_word not in word_map:
        wid = word_id_seq
        word_map[head_word] = wid
        word_id_seq += 1

        insert("vocab_word", {
            "id": str(wid),
            "word": esc(head_word),
            "word_traditional": "NULL",
            "pinyin": esc(row.get("拼音")),
            "audio_id": "NULL",
            "hsk_level": "NULL",
            "create_time": "NOW()",
            "update_time": "NOW()"
        })

    word_id = word_map[head_word]

    # ---- vocab_sense ----
    sense_no = str(row.get("义项", "")).strip()
    sense_key = (word_id, sense_no)

    if sense_key not in sense_map:
        sid = sense_id_seq
        sense_map[sense_key] = sid
        sense_id_seq += 1

        insert("vocab_sense", {
            "id": str(sid),
            "word_id": str(word_id),
            "part_of_speech": esc(row.get("词性")),
            "chinese_def": esc(row.get("中文释义")),
            "def_audio_id": "NULL",
            "translations": esc(json.dumps([
                {"lang": "en", "text": row.get("英文释义")}
            ], ensure_ascii=False)),
            "synonyms": esc(row.get("近义")),
            "antonyms": esc(row.get("反义")),
            "related_forward": esc(row.get("正序")),
            "related_backward": esc(row.get("逆序")),
            "sense_order": "0",
            "create_time": "NOW()",
            "update_time": "NOW()"
        })
    else:
        sid = sense_map[sense_key]

    # ---- vocab_structure ----
    pattern = row.get("结构搭配")
    if pd.notna(pattern):
        stid = structure_id_seq
        structure_id_seq += 1

        insert("vocab_structure", {
            "id": str(stid),
            "word_id": str(word_id),
            "sense_id": str(sid),
            "pattern": esc(pattern),
            "structure_order": "0",
            "create_time": "NOW()",
            "update_time": "NOW()"
        })

        # ---- vocab_example ----
        example = row.get("例句")
        if pd.notna(example):
            exid = example_id_seq
            example_id_seq += 1

            insert("vocab_example", {
                "id": str(exid),
                "word_id": str(word_id),
                "sense_id": str(sid),
                "structure_id": str(stid),
                "sentence": esc(example),
                "audio_id": "NULL",
                "pinyin": "NULL",
                "translations": "NULL",
                "example_order": "0",
                "create_time": "NOW()",
                "update_time": "NOW()"
            })

# ---------- 写出 SQL ----------
with open(OUTPUT_SQL, "w", encoding="utf-8") as f:
    f.write("\n".join(sql_lines))

print("✅ SQL 已生成：")
print(OUTPUT_SQL)