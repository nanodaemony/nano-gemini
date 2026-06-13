#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vocab Comparison API test suite.
Tests all 8 admin endpoints + 2 app endpoints.
"""
import urllib.request, json, sys

BASE = "http://localhost:8000"
GROUP_ID = None
PASS, FAIL = "[PASS]", "[FAIL]"

def api(method, path, data=None):
    url = BASE + path
    body = json.dumps(data, ensure_ascii=False).encode('utf-8') if data else None
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Content-Type", "application/json; charset=utf-8")
    req.add_header("Accept", "application/json")
    try:
        with urllib.request.urlopen(req) as r:
            text = r.read().decode('utf-8')
            return r.status, (json.loads(text) if text else None)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='replace')

def ok(c): return str(c)[0] in ("2", "3")

# ============================================================
# PHASE 1: Create (draft)
# ============================================================
print("=" * 60)
print("PHASE 1: Create groups (draft)")
print("=" * 60)

c, r = api("POST", "/api/vocab/comparison", {
    "groupKey": "biaozhun vs chidu",
    "groupOrder": 10,
    "items": [
        {"wordId": 10, "word": "biaozhun", "partOfSpeech": "n.",
         "usageComparison": "formal, widely used",
         "usageComparisonTranslations": [{"language": "en", "translation": "standard"}],
         "commonUsage": "raise/lower + standard",
         "order": 10},
        {"wordId": 11, "word": "chidu", "partOfSpeech": "n.",
         "usageComparison": "narrow usage, appropriate degree",
         "order": 5}
    ]
})
print(f"  {PASS if ok(c) else FAIL} Test 1: Create group 1 -> {c}")
if c == 201 and isinstance(r, dict):
    GROUP_ID = r.get("id")
    print(f"       GROUP_ID={GROUP_ID}")

c, r = api("POST", "/api/vocab/comparison", {
    "groupKey": "anxian vs anyi",
    "groupOrder": 20,
    "items": [
        {"wordId": 12, "word": "anxian", "partOfSpeech": "adj.",
         "usageComparison": "emphasizes calm life",
         "order": 10},
        {"wordId": 13, "word": "anyi", "partOfSpeech": "adj.",
         "usageComparison": "emphasizes comfort",
         "order": 5}
    ],
    "chats": [
        {"role": "teacher", "content": "What's the difference?",
         "pinyin": "you shenme butong", "order": 1},
        {"role": "student", "content": "One is calm, one is comfortable.",
         "pinyin": "yige pingjing yige shufu", "order": 2}
    ]
})
print(f"  {PASS if ok(c) else FAIL} Test 2: Create group 2 (with chats) -> {c}")

c, r = api("POST", "/api/vocab/comparison", {
    "groupKey": "fanyi vs yi",
    "groupOrder": 30,
    "items": [
        {"wordId": 14, "word": "fanyi", "partOfSpeech": "v.", "order": 10},
        {"wordId": 14, "word": "fanyi", "partOfSpeech": "n.", "order": 5},
        {"wordId": 15, "word": "yi", "partOfSpeech": "v.", "order": 0}
    ]
})
print(f"  {PASS if ok(c) else FAIL} Test 3: Create group 3 -> {c}")

# ============================================================
# PHASE 2: Query drafts
# ============================================================
print("\n" + "=" * 60)
print("PHASE 2: Query draft groups")
print("=" * 60)

c, r = api("GET", "/api/vocab/comparison?page=0&size=10")
total = r.get("totalElements", 0) if isinstance(r, dict) else "?"
print(f"  {PASS if ok(c) else FAIL} Test 4: List all -> {c}, total={total}")

c, r = api("GET", "/api/vocab/comparison?editStatus=draft&size=10")
draft_total = r.get("totalElements", 0) if isinstance(r, dict) else "?"
print(f"  {PASS if ok(c) else FAIL} Test 5: Filter editStatus=draft -> {c}, total={draft_total}")

# ============================================================
# PHASE 3: Update group 1 (add chats)
# ============================================================
print("\n" + "=" * 60)
print("PHASE 3: Update group")
print("=" * 60)

if GROUP_ID:
    c, r = api("PUT", f"/api/vocab/comparison/{GROUP_ID}", {
        "groupKey": "biaozhun vs chidu (updated)",
        "groupOrder": 15,
        "items": [
            {"wordId": 10, "word": "biaozhun", "partOfSpeech": "n.",
             "usageComparison": "formal, widely used",
             "order": 10},
            {"wordId": 11, "word": "chidu", "partOfSpeech": "n.",
             "usageComparison": "narrow usage",
             "order": 5}
        ],
        "chats": [
            {"role": "teacher", "content": "Please explain difference.",
             "pinyin": "qing shuoshuo qubie", "order": 1},
            {"role": "student", "content": "Standard is objective.",
             "pinyin": "biaozhun shi keguan de", "order": 2}
        ]
    })
    print(f"  {PASS if ok(c) else FAIL} Test 6: Update group {GROUP_ID} -> {c}")

    c, r = api("GET", f"/api/vocab/comparison/{GROUP_ID}")
    has_chats = len(r.get("chats", [])) if isinstance(r, dict) else 0
    has_items = len(r.get("items", [])) if isinstance(r, dict) else 0
    print(f"  {PASS if ok(c) else FAIL} Test 7: Get detail (draft) -> {c}, items={has_items}, chats={has_chats}")

# ============================================================
# PHASE 4: Review -> Publish
# ============================================================
print("\n" + "=" * 60)
print("PHASE 4: Review -> Publish workflow")
print("=" * 60)

if GROUP_ID:
    c, _ = api("PUT", f"/api/vocab/comparison/{GROUP_ID}/review")
    print(f"  {PASS if ok(c) else FAIL} Test 8: Review group {GROUP_ID} -> {c}")

    c, r = api("GET", f"/api/vocab/comparison/{GROUP_ID}")
    edit_status = r.get("editStatus") if isinstance(r, dict) else "?"
    print(f"  {PASS if ok(c) else FAIL} Test 9: Get after review -> {c}, editStatus={edit_status}")

    c, _ = api("PUT", f"/api/vocab/comparison/{GROUP_ID}/publish")
    print(f"  {PASS if ok(c) else FAIL} Test 10: Publish group {GROUP_ID} -> {c}")

    c, r = api("GET", f"/api/vocab/comparison/{GROUP_ID}")
    if isinstance(r, dict):
        items = r.get("items", [])
        chats = r.get("chats", [])
        dc = r.get("draftContent")
        es = r.get("editStatus")
        ps = r.get("publishStatus")
        print(f"  {PASS if ok(c) else FAIL} Test 11: Get after publish -> {c}")
        print(f"       editStatus={es}, publishStatus={ps}, items={len(items)}, chats={len(chats)}, draftContent={'null' if not dc else 'HAS VALUE!'}")
        if chats:
            print(f"       First chat: role={chats[0].get('role')}, pinyin={chats[0].get('pinyin')}")

    # Publish group 2 as well
    c2, r2 = api("GET", "/api/vocab/comparison?editStatus=draft&size=10")
    if isinstance(r2, dict) and r2.get("content"):
        for g in r2["content"]:
            gid = g["id"]
            if g.get("publishStatus") == "published":
                continue
            api("PUT", f"/api/vocab/comparison/{gid}/review")
            c_pub, _ = api("PUT", f"/api/vocab/comparison/{gid}/publish")
            print(f"  {PASS if ok(c_pub) else FAIL} Test 11b: Publish group {gid} ({g.get('groupKey')}) -> {c_pub}")

# ============================================================
# PHASE 5: Search by word / wordId
# ============================================================
print("\n" + "=" * 60)
print("PHASE 5: Search by word/wordId (admin)")
print("=" * 60)

c, r = api("GET", "/api/vocab/comparison?word=biaozhun&size=10")
total = r.get("totalElements", "?") if isinstance(r, dict) else "?"
print(f"  {PASS if ok(c) else FAIL} Test 12: Search by word=biaozhun -> {c}, found={total}")

c, r = api("GET", "/api/vocab/comparison?wordId=10&size=10")
total = r.get("totalElements", "?") if isinstance(r, dict) else "?"
print(f"  {PASS if ok(c) else FAIL} Test 13: Search by wordId=10 -> {c}, found={total}")

# ============================================================
# PHASE 6: App API
# ============================================================
print("\n" + "=" * 60)
print("PHASE 6: App API")
print("=" * 60)

c, r = api("GET", "/api/app/vocab/comparison/search?word=biaozhun")
count = len(r) if isinstance(r, list) else 0
print(f"  {PASS if ok(c) else FAIL} Test 14: App search word=biaozhun -> {c}, groups={count}")

if GROUP_ID:
    c, r = api("GET", f"/api/app/vocab/comparison/{GROUP_ID}")
    if isinstance(r, dict):
        items = len(r.get("items", []))
        chats = len(r.get("chats", []))
        chat_has_audio = any(c.get("audioUrl") for c2 in r.get("chats", []) if c2.get("audioUrl"))
        print(f"  {PASS if ok(c) else FAIL} Test 15: App detail {GROUP_ID} -> {c}")
        print(f"       items={items}, chats={chats}, hasAudioUrl={chat_has_audio}")

# ============================================================
# PHASE 7: Offline + Delete
# ============================================================
print("\n" + "=" * 60)
print("PHASE 7: Offline -> Delete")
print("=" * 60)

if GROUP_ID:
    c, _ = api("PUT", f"/api/vocab/comparison/{GROUP_ID}/offline")
    print(f"  {PASS if ok(c) else FAIL} Test 16: Offline group {GROUP_ID} -> {c}")

    c, r = api("GET", f"/api/app/vocab/comparison/{GROUP_ID}")
    print(f"  {PASS if c==404 else FAIL} Test 17: App detail after offline (expect 404) -> {c}")

    c, _ = api("PUT", f"/api/vocab/comparison/{GROUP_ID}/publish")
    print(f"  {PASS if ok(c) else FAIL} Test 18: Re-publish -> {c}")

    c, _ = api("DELETE", f"/api/vocab/comparison/{GROUP_ID}")
    print(f"  {PASS if ok(c) else FAIL} Test 19: Delete group {GROUP_ID} -> {c}")

    c, r = api("GET", f"/api/vocab/comparison/{GROUP_ID}")
    print(f"  {PASS if not ok(c) else FAIL} Test 20: Get after delete (expect error) -> {c}")

# ============================================================
# PHASE 8: Error scenarios
# ============================================================
print("\n" + "=" * 60)
print("PHASE 8: Error scenarios")
print("=" * 60)

c, r = api("POST", "/api/vocab/comparison", {"groupKey": "", "groupOrder": 0, "items": []})
print(f"  {PASS if not ok(c) else FAIL} Test 21: Create with empty groupKey -> {c}")

c, new = api("POST", "/api/vocab/comparison", {
    "groupKey": "dup review test", "groupOrder": 0,
    "items": [{"wordId": 1, "word": "test", "order": 0}]
})
if c == 201 and isinstance(new, dict):
    new_id = new["id"]
    api("PUT", f"/api/vocab/comparison/{new_id}/review")
    c2, _ = api("PUT", f"/api/vocab/comparison/{new_id}/review")
    print(f"  {PASS if not ok(c2) else FAIL} Test 22: Duplicate review (expect 400) -> {c2}")
    c3, _ = api("PUT", f"/api/vocab/comparison/{new_id}/publish")
    c4, _ = api("PUT", f"/api/vocab/comparison/{new_id}/publish")
    print(f"  {PASS if not ok(c4) else FAIL} Test 23: Duplicate publish (expect 400) -> {c4}")
    api("DELETE", f"/api/vocab/comparison/{new_id}")

c, r = api("GET", "/api/vocab/comparison?page=0&size=10")
total = r.get("totalElements", "?") if isinstance(r, dict) else "?"
print(f"\n{PASS if ok(c) else FAIL} Final check: All groups -> {total} remaining")

print("\n" + "=" * 60)
print("DONE")
print("=" * 60)
