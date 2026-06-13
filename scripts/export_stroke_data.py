#!/usr/bin/env python3
"""
将 hanzi-writer-data 的 JSON 笔顺文件导出为 SQL 语句。
自动创建输出目录。
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

    errors = 0
    lines = []
    for filepath in json_files:
        char_name = os.path.splitext(os.path.basename(filepath))[0]
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                stroke_data = f.read()
            # 验证 JSON 合法性
            json.loads(stroke_data)
        except (IOError, OSError, json.JSONDecodeError) as e:
            print(f"警告: 跳过文件 {filepath}: {e}")
            errors += 1
            continue
        # 转义单引号
        stroke_escaped = stroke_data.replace("'", "''")
        lines.append(f"INSERT INTO `char_stroke` (`character`, `stroke`, `status`) VALUES ('{char_name}', '{stroke_escaped}', 1);")

    sql = "\n".join(lines) + "\n"

    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(sql)

    print(f"已导出 {len(lines)} 条记录到 {OUTPUT_FILE}")
    if errors:
        print(f"跳过 {errors} 个文件（详见上述警告）")


if __name__ == "__main__":
    main()
