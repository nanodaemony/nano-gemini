#!/bin/bash
# 收藏夹功能测试脚本
# 用法: bash test-collection.sh
# 前提: 1. 重新编译并重启服务（让 @AnonymousAccess 生效）
#       2. 确保存在 userId=1 的用户（如已注册，则默认收藏夹已自动创建）

BASE="http://localhost:8000/api/app/collection"
TMP=$(mktemp -d)
PASS=0
FAIL=0

green() { echo -e "\033[32m  PASS\033[0m $1"; }
red() { echo -e "\033[31m  FAIL\033[0m $1"; }
section() { echo ""; echo "=== $1 ==="; }

# 写 JSON 到临时文件避免 shell 编码问题
json() { echo "$1" > "$TMP/body.json"; }

# 封装 curl，统一用文件传 JSON
post() { curl -s -w "\n%{http_code}" -X POST "$1" -H "Content-Type: application/json; charset=utf-8" --data-binary "@$TMP/body.json"; }
put()  { curl -s -w "\n%{http_code}" -X PUT  "$1" -H "Content-Type: application/json; charset=utf-8" --data-binary "@$TMP/body.json"; }
get()  { curl -s -w "\n%{http_code}" -X GET  "$1"; }
del()  { curl -s -w "\n%{http_code}" -X DELETE "$1"; }

assert_status() {
  local actual="$1" expected="$2" msg="$3"
  if [ "$actual" = "$expected" ]; then
    green "$msg (HTTP $actual)"
    PASS=$((PASS+1))
  else
    red "$msg — expected HTTP $expected, got $actual"
    FAIL=$((FAIL+1))
  fi
}

assert_contains() {
  local body="$1" pattern="$2" msg="$3"
  if echo "$body" | grep -q "$pattern"; then
    green "$msg"
    PASS=$((PASS+1))
  else
    red "$msg — response did not contain '$pattern'"
    echo "    Body: $body"
    FAIL=$((FAIL+1))
  fi
}

###############################################################################
# 0. 前置检查
###############################################################################
section "0. 前置检查"

RESP=$(get "$BASE/folder/list")
BODY=$(echo "$RESP" | sed '$d')
if echo "$BODY" | grep -q '"message"'; then
  red "服务未重启或注解未生效，请重新编译并重启服务"
  echo "    Response: $BODY"
  echo "    mvn clean compile -pl grid-bootstrap -am -DskipTests"
  echo "    然后重启 spring-boot:run"
  rm -rf "$TMP"
  exit 1
fi
green "服务可达，匿名访问已生效"

###############################################################################
# 1. 收藏夹 CRUD
###############################################################################
section "1. 收藏夹 CRUD"

# 1.1 新建收藏夹
echo ">>> 1.1 新建收藏夹"
json '{"name":"HSK1词汇","coverImageId":null}'
RESP=$(post "$BASE/folder")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "$HTTP" "200" "新建收藏夹"
FOLDER_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "    创建成功, folderId=$FOLDER_ID"

# 1.2 新建第二个收藏夹
echo ">>> 1.2 新建第二个收藏夹"
json '{"name":"CommonGrammar","coverImageId":null}'
RESP=$(post "$BASE/folder")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "$HTTP" "200" "新建第二个收藏夹"
FOLDER_ID2=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

# 1.3 查询收藏夹列表
echo ">>> 1.3 查询收藏夹列表"
RESP=$(get "$BASE/folder/list")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "$HTTP" "200" "查询收藏夹列表"
assert_contains "$BODY" "HSK1词汇" "列表中包含HSK1词汇"
assert_contains "$BODY" "CommonGrammar" "列表中包含CommonGrammar"

# 1.4 查询默认收藏夹（注册时自动创建）
echo ">>> 1.4 默认收藏夹是否存在"
assert_contains "$BODY" "默认收藏夹" "列表中存在默认收藏夹"

# 1.5 修改收藏夹名称
echo ">>> 1.5 修改收藏夹名称"
json '{"name":"HSK1词汇(已更名)"}'
RESP=$(put "$BASE/folder/$FOLDER_ID/name")
HTTP=$(echo "$RESP" | tail -1)
assert_status "$HTTP" "200" "修改收藏夹名称"

# 1.6 查询详情
echo ">>> 1.6 查询收藏夹详情"
RESP=$(get "$BASE/folder/$FOLDER_ID")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "$HTTP" "200" "查询收藏夹详情"
assert_contains "$BODY" "HSK1词汇(已更名)" "详情中名称已更新"

###############################################################################
# 2. 置顶功能
###############################################################################
section "2. 置顶功能"

# 2.1 置顶第二个收藏夹
echo ">>> 2.1 置顶"
RESP=$(put "$BASE/folder/$FOLDER_ID2/pin")
HTTP=$(echo "$RESP" | tail -1)
assert_status "$HTTP" "200" "置顶收藏夹"

# 2.2 验证排序（置顶的在前）
echo ">>> 2.2 验证置顶排序"
RESP=$(get "$BASE/folder/list")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
FIRST_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
if [ "$FIRST_ID" = "$FOLDER_ID2" ]; then
  green "列表中第一个是置顶收藏夹(id=$FOLDER_ID2)"
  PASS=$((PASS+1))
else
  red "置顶收藏夹不在列表最前面 (期望id=$FOLDER_ID2, 实际id=$FIRST_ID)"
  FAIL=$((FAIL+1))
fi

# 2.3 取消置顶
echo ">>> 2.3 取消置顶"
RESP=$(put "$BASE/folder/$FOLDER_ID2/unpin")
HTTP=$(echo "$RESP" | tail -1)
assert_status "$HTTP" "200" "取消置顶"

###############################################################################
# 3. 收藏内容
###############################################################################
section "3. 收藏内容"

# 3.1 添加收藏（指定收藏夹-汉字）
echo ">>> 3.1 添加收藏-汉字"
json "{\"folderId\":$FOLDER_ID,\"bizType\":\"CHARACTER\",\"contentId\":1}"
RESP=$(post "$BASE/item")
HTTP=$(echo "$RESP" | tail -1)
assert_status "$HTTP" "200" "添加汉字收藏"

# 3.2 添加收藏（词汇）
echo ">>> 3.2 添加收藏-词汇"
json "{\"folderId\":$FOLDER_ID,\"bizType\":\"VOCABULARY\",\"contentId\":1}"
RESP=$(post "$BASE/item")
HTTP=$(echo "$RESP" | tail -1)
assert_status "$HTTP" "200" "添加词汇收藏"

# 3.3 重复收藏（幂等测试）
echo ">>> 3.3 重复收藏(幂等)"
json "{\"folderId\":$FOLDER_ID,\"bizType\":\"CHARACTER\",\"contentId\":1}"
RESP=$(post "$BASE/item")
HTTP=$(echo "$RESP" | tail -1)
assert_status "$HTTP" "200" "重复收藏不报错(幂等)"

# 3.4 不指定folderId（使用默认收藏夹）
echo ">>> 3.4 不指定folderId使用默认收藏夹"
json '{"bizType":"GRAMMAR","contentId":1}'
RESP=$(post "$BASE/item")
HTTP=$(echo "$RESP" | tail -1)
assert_status "$HTTP" "200" "默认收藏夹添加成功"

# 3.5 检查收藏状态-已收藏
echo ">>> 3.5 检查收藏状态-已收藏"
RESP=$(get "$BASE/item/check?bizType=CHARACTER&contentId=1")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "$HTTP" "200" "检查收藏状态"
assert_contains "$BODY" '"collected":true' "返回collected=true"

# 3.6 检查未收藏状态
echo ">>> 3.6 检查收藏状态-未收藏"
RESP=$(get "$BASE/item/check?bizType=CHARACTER&contentId=99999")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "$HTTP" "200" "检查收藏状态-未收藏"
assert_contains "$BODY" '"collected":false' "返回collected=false"

###############################################################################
# 4. 收藏夹详情（按业务类型分组）
###############################################################################
section "4. 收藏夹详情-分组"

echo ">>> 4.1 查询详情验证分组"
RESP=$(get "$BASE/folder/$FOLDER_ID")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "$HTTP" "200" "查询收藏夹详情"
assert_contains "$BODY" "CHARACTER" "详情中包含CHARACTER分组"
assert_contains "$BODY" "VOCABULARY" "详情中包含VOCABULARY分组"

###############################################################################
# 5. 取消收藏和删除
###############################################################################
section "5. 取消收藏和删除"

# 5.1 查询详情获取收藏项ID
echo ">>> 5.1 获取收藏项ID"
ITEM_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "    item ID: $ITEM_ID"

# 5.2 取消收藏
echo ">>> 5.2 取消收藏"
RESP=$(del "$BASE/item/$ITEM_ID")
HTTP=$(echo "$RESP" | tail -1)
assert_status "$HTTP" "200" "取消收藏"

# 5.3 删除收藏夹（非默认）
echo ">>> 5.3 删除收藏夹"
RESP=$(del "$BASE/folder/$FOLDER_ID2")
HTTP=$(echo "$RESP" | tail -1)
assert_status "$HTTP" "200" "删除自定义收藏夹"

###############################################################################
# 6. 错误场景
###############################################################################
section "6. 错误场景"

# 6.1 内容ID不存在
echo ">>> 6.1 内容ID不存在"
json "{\"folderId\":$FOLDER_ID,\"bizType\":\"CHARACTER\",\"contentId\":99999999}"
RESP=$(post "$BASE/item")
HTTP=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | sed '$d')
assert_status "$HTTP" "400" "内容不存在返回400"

# 6.2 收藏夹名称超长（>32字符）
echo ">>> 6.2 名称超长"
json '{"name":"aaaaaaaaaabbbbbbbbbbccccccccccdddd"}'
RESP=$(post "$BASE/folder")
HTTP=$(echo "$RESP" | tail -1)
assert_status "$HTTP" "400" "名称超长返回400"

###############################################################################
# 结果汇总
###############################################################################
rm -rf "$TMP"
section "结果汇总"
echo "总测试: $((PASS+FAIL)), 通过: $PASS, 失败: $FAIL"

if [ $FAIL -eq 0 ]; then
  echo -e "\n\033[32m全部测试通过!\033[0m"
  exit 0
else
  echo -e "\n\033[31m有 $FAIL 个测试失败\033[0m"
  exit 1
fi
