#!/usr/bin/env python3
"""
将 hanzi-writer-data 的 JSON 笔顺文件导出为 SQL INSERT 语句。
用法: python scripts/export_stroke_data.py
"""

import json
import os
import glob

INPUT_DIR = r"C:/Users/nano/Desktop/hanzi-writer-data/data"
OUTPUT_FILE = r"C:/Users/nano/Desktop/nano-gemini/sql/char_stroke_data.sql"


def main():
    json_files = sorted(glob.glob(os.path.join(INPUT_DIR, "*.json")))
    print(f"找到 {len(json_files)} 个 JSON 文件")

    values = []
    for filepath in json_files:
        char_name = os.path.splitext(os.path.basename(filepath))[0]
        with open(filepath, "r", encoding="utf-8") as f:
            stroke_data = f.read()
        # 转义单引号
        stroke_escaped = stroke_data.replace("'", "''")
        values.append(f"('{char_name}', '{stroke_escaped}', 1)")

    sql = f"INSERT INTO `char_stroke` (`character`, `stroke`, `status`) VALUES\n"
    sql += ",\n".join(values) + ";\n"

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(sql)

    print(f"已导出 {len(values)} 条记录到 {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
