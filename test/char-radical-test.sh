#!/bin/bash
# ============================================
# 汉字部首后台接口测试脚本
# 前提: 已执行 sql/char_radical_data.sql 导入基础数据
# 服务地址: http://localhost:8000
# 用法: bash test/char-radical-test.sh
# ============================================

BASE_URL="http://localhost:8000/api/char/radical"
PASS=0
FAIL=0

check() {
    local desc="$1"
    local expected="$2"
    local actual="$3"
    if echo "$actual" | grep -q "$expected"; then
        echo "  ✅ PASS: $desc"
        PASS=$((PASS+1))
    else
        echo "  ❌ FAIL: $desc"
        echo "     期望包含: $expected"
        echo "     实际响应: $(echo "$actual" | head -c 200)"
        FAIL=$((FAIL+1))
    fi
}

echo ""
echo "=============================================="
echo "  汉字部首接口测试"
echo "=============================================="
echo ""

# ---- 测试 1: 分页查询列表（全部） ----
echo "【测试 1】分页查询部首列表（默认全部）"
RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL?page=0&size=10")
check "GET /api/char/radical?page=0&size=10 返回 200" "200" "$RESP"

echo ""
echo "----- 列表内容 -----"
curl -s "$BASE_URL?page=0&size=10" | python -m json.tool 2>/dev/null || curl -s "$BASE_URL?page=0&size=10"
echo ""

# ---- 测试 2: 分页查询（按发布状态筛选） ----
echo "【测试 2】按发布状态筛选 published"
RESP=$(curl -s "$BASE_URL?publishStatus=published&page=0&size=10")
check "筛选 published 返回成功" "radical" "$RESP"

# ---- 测试 3: 分页查询（按部首名称模糊搜索） ----
echo ""
echo "【测试 3】按部首名称模糊搜索「口」"
RESP=$(curl -s "$BASE_URL?blurry=%E5%8F%A3&page=0&size=10")
check "搜索「口」找到数据" "radical" "$RESP"

# ---- 测试 4: 查询详情 ----
echo ""
echo "【测试 4】查询部首详情 ID=1（厂）"
RESP=$(curl -s "$BASE_URL/1")
echo "  → 响应:"
echo "$RESP" | python -m json.tool 2>/dev/null || echo "$RESP"
check "查询详情返回 radical=厂" '"radical":"厂"' "$RESP"
check "查询详情包含 strokeNum" "strokeNum" "$RESP"
check "查询详情包含 evolutionDesc" "evolutionDesc" "$RESP"

# ---- 测试 5: 查询不存在的部首 ----
echo ""
echo "【测试 5】查询不存在的部首 ID=999"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/999")
check "查询不存在返回 404" "404" "$HTTP_CODE"

# ---- 测试 6: 修改部首（写入草稿） ----
echo ""
echo "【测试 6】修改部首 ID=1（厂），写入草稿"
RESP=$(curl -s -X PUT "$BASE_URL/1" \
  -H "Content-Type: application/json" \
  -d '{
    "radical": "厂",
    "strokeNum": 2,
    "evolutionDesc": "「厂」是象形字，古文字模拟山崖的形状。本义是山崖。【已更新】",
    "evolutionImageId": "img_001"
  }' -o /dev/null -w "%{http_code}")
check "PUT /{id} 返回 204" "204" "$RESP"

# ---- 测试 7: 审核草稿 ----
echo ""
echo "【测试 7】审核部首 ID=1 的草稿（draft→reviewed）"
RESP=$(curl -s -X PUT "$BASE_URL/1/review" -o /dev/null -w "%{http_code}")
check "PUT /{id}/review 返回 204" "204" "$RESP"

# ---- 测试 8: 发布部首 ----
echo ""
echo "【测试 8】发布部首 ID=1（reviewed→published）"
RESP=$(curl -s -X PUT "$BASE_URL/1/publish" -o /dev/null -w "%{http_code}")
check "PUT /{id}/publish 返回 204" "204" "$RESP"

echo ""
echo "----- 发布后查询详情确认回写 -----"
RESP=$(curl -s "$BASE_URL/1")
check "发布后 evolutionDesc 已更新" "已更新" "$RESP"
check "发布后 editStatus=published" '"editStatus":"published"' "$RESP"

# ---- 测试 9: 状态流转异常测试 ----
echo ""
echo "【测试 9】异常流程：已发布状态再次审核"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/1/review")
check "已发布再次审核返回 400" "400" "$HTTP_CODE"

# ---- 测试 10: 新增部首应不存在 ----
echo ""
echo "【测试 10】POST 新增接口是否存在（应返回 405 或 404）"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{"radical":"test","strokeNum":1}')
check "POST 返回 405 或 404（无新增接口）" "405|404" "$HTTP_CODE"

# ---- 测试 11: 再次修改并走完整流程 ----
echo ""
echo "【测试 11】修改部首 ID=10（口），走完整工作流"

# 11a: 修改
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/10" \
  -H "Content-Type: application/json" \
  -d '{
    "radical": "口",
    "strokeNum": 3,
    "evolutionDesc": "「口」是象形字，模拟张开的嘴。本义是人嘴。",
    "evolutionImageId": "img_010"
  }')
check "11a PUT 修改返回 204" "204" "$HTTP_CODE"

# 11b: 审核
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/10/review")
check "11b PUT review 返回 204" "204" "$HTTP_CODE"

# 11c: 发布
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/10/publish")
check "11c PUT publish 返回 204" "204" "$HTTP_CODE"

# 11d: 下线
echo ""
echo "【测试 12】下线部首 ID=10"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/10/offline")
check "PUT /{id}/offline 返回 204" "204" "$HTTP_CODE"

# 11e: 查询确认已下线
RESP=$(curl -s "$BASE_URL/10")
check "下线后 publishStatus=unpublished" '"publishStatus":"unpublished"' "$RESP"

# ---- 测试 13: 软删除 ----
echo ""
echo "【测试 13】软删除部首 ID=10"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/10")
check "DELETE /{id} 返回 204" "204" "$HTTP_CODE"

# 确认删除后查不到
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/10")
check "删除后查询返回 404" "404" "$HTTP_CODE"

# ---- 测试 14: 分页查询验证数量 ----
echo ""
echo "【测试 14】验证分页 totalElements（应 = 10，删了 ID=10 后剩 10）"
RESP=$(curl -s "$BASE_URL?page=0&size=20")
TOTAL=$(echo "$RESP" | python -c "import sys,json; print(json.load(sys.stdin).get('totalElements', 0))" 2>/dev/null)
echo "  → totalElements = $TOTAL"
check "totalElements 为 10" "10" "$TOTAL"

# ---- 总结 ----
echo ""
echo "=============================================="
echo "  测试完成"
echo "  通过: $PASS"
echo "  失败: $FAIL"
echo "=============================================="
