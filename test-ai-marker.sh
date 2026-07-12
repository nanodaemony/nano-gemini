#!/bin/bash
# AI Content Marker - Final Comprehensive Test
BASE="http://localhost:8000"
P=0; F=0
green() { echo -e "\033[32m[PASS]\033[0m $*"; ((P++)); }
red()   { echo -e "\033[31m[FAIL]\033[0m $*"; ((F++)); }
check() { if echo "$2" | grep -q "$3"; then green "$1"; else red "$1 (got: $2)"; fi; }
check_not() { if ! echo "$2" | grep -q "$3"; then green "$1"; else red "$1"; fi; }
hdr()   { echo -e "\n\033[36m=== $* ===\033[0m"; }

# ====================================================================
hdr "1. VocabWord: Create Draft → Query → Publish → Review"
# ====================================================================
R=$(curl -s -X POST $BASE/api/vocabulary -H 'Content-Type: application/json' \
  -d '{"word":"finaltest1","pinyin":"ft","hskLevel":"HSK3","senses":[{"partOfSpeech":"n","chineseDef":"AI generated definition","defTranslations":[{"language":"en","translation":"test tr"}],"aiGeneratedFields":["chineseDef","defTranslations"],"structures":[{"pattern":"test+O","patternDef":"test something","aiGeneratedFields":["pattern","patternDef"],"structureSentences":[{"sentence":"AI sentence one.","aiGeneratedFields":["sentence"]},{"sentence":"Human sentence.","aiGeneratedFields":[]}]}]}]}')
VID=$(echo "$R" | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Created vocab $VID"

# Draft query
D=$(curl -s $BASE/api/vocabulary/$VID)
echo "$D" | python -c "
import sys,json; d=json.load(sys.stdin); s=d['senses'][0]
print('DRAFT sense ai:', s.get('aiGeneratedFields'))
st=s['structureExamples'][0]
print('DRAFT struct ai:', st.get('aiGeneratedFields'))
for i,se in enumerate(st['structureExamples']):
    print(f'DRAFT sent{i} ai:', se.get('aiGeneratedFields'))"
check "Draft sense has AI fields" "$D" "chineseDef"
check "Draft struct has AI" "$D" "pattern"

# Publish
curl -s -o /dev/null -X PUT $BASE/api/vocabulary/$VID/review
curl -s -o /dev/null -X PUT $BASE/api/vocabulary/$VID/publish

# Published query
D2=$(curl -s $BASE/api/vocabulary/$VID)
SID=$(echo "$D2" | python -c "import sys,json; print(json.load(sys.stdin)['senses'][0]['id'])")
STID=$(echo "$D2" | python -c "import sys,json; d=json.load(sys.stdin); s=d['senses'][0]; st=s['structureExamples'][0]; print(st['id'])")
SEID=$(echo "$D2" | python -c "import sys,json; d=json.load(sys.stdin); s=d['senses'][0]; st=s['structureExamples'][0]; print(st['structureExamples'][0]['id'])")
echo "IDs: sense=$SID struct=$STID sent0=$SEID"
PV=$(echo "$D2" | python -c "
import sys,json; d=json.load(sys.stdin); s=d['senses'][0]
print('PUB sense ai:', s.get('aiGeneratedFields'), 'rev:', s.get('aiReviewedFields'))
st=s['structureExamples'][0]
print('PUB struct ai:', st.get('aiGeneratedFields'))
for i,se in enumerate(st['structureExamples']):
    print(f'PUB sent{i} ai:', se.get('aiGeneratedFields'))")
check "Published sense has AI" "$PV" "chineseDef"
check "Published struct has AI" "$PV" "pattern"
check "Published sent0 has AI" "$PV" "sentence"
check_not "Published sent1 no AI" "$PV" "sent1.*sentence"

# Review
curl -s -o /dev/null -X PUT "$BASE/api/ai-content-markers/vocab_sense/$SID/review" -H 'Content-Type: application/json' -d '{"fieldName":"chineseDef","reviewed":true}'
D3=$(curl -s $BASE/api/vocabulary/$VID)
RV=$(echo "$D3" | python -c "import sys,json; s=json.load(sys.stdin)['senses'][0]; print(s.get('aiReviewedFields'))")
check "Review chineseDef=reviewed" "$RV" "chineseDef"

# Un-review
curl -s -o /dev/null -X PUT "$BASE/api/ai-content-markers/vocab_sense/$SID/review" -H 'Content-Type: application/json' -d '{"fieldName":"chineseDef","reviewed":false}'
D4=$(curl -s $BASE/api/vocabulary/$VID)
URV=$(echo "$D4" | python -c "import sys,json; print(json.load(sys.stdin)['senses'][0].get('aiReviewedFields'))")
check "Un-review = empty" "$URV" "\[\]"

# ====================================================================
hdr "2. GrammarPoint: Create → Publish → Check Markers"
# ====================================================================
GR=$(curl -s -X POST $BASE/api/grammar -H 'Content-Type: application/json' \
  -d '{"name":"test grammar point","hskLevel":"HSK1","project":"test","category":"test","meanings":[{"meaningContent":"AI meaning","aiGeneratedFields":["meaningContent"]}],"structures":[{"structureContent":"S+V+O","aiGeneratedFields":["structureContent"],"sentences":[{"sentence":"AI struct sentence.","aiGeneratedFields":["sentence"]}]}],"notices":[{"noticeContent":"AI notice","aiGeneratedFields":["noticeContent"]}],"errors":[{"errorContent":"AI error","errorAnalysis":"AI analysis","aiGeneratedFields":["errorContent","errorAnalysis"]}]}')
GID=$(echo "$GR" | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
echo "Created grammar $GID"
curl -s -o /dev/null -X PUT $BASE/api/grammar/$GID/review
curl -s -o /dev/null -X PUT $BASE/api/grammar/$GID/publish
GD=$(curl -s $BASE/api/grammar/$GID)
echo "$GD" | python -c "
import sys,json; d=json.load(sys.stdin)
print('meanings:', [(m.get('aiGeneratedFields'),m.get('id')) for m in d.get('meanings',[])])
print('structures:', [(s.get('aiGeneratedFields'),s.get('id')) for s in d.get('structures',[])])
print('notices:', [(n.get('aiGeneratedFields'),n.get('id')) for n in d.get('notices',[])])
print('errors:', [(e.get('aiGeneratedFields'),e.get('id')) for e in d.get('errors',[])])"
check "Grammar meaning AI" "$GD" "meaningContent"
check "Grammar structure AI" "$GD" "structureContent"
check "Grammar notice AI" "$GD" "noticeContent"
check "Grammar error AI" "$GD" "errorContent"

# Review grammar field
GMID=$(echo "$GD" | python -c "import sys,json; print(json.load(sys.stdin)['meanings'][0]['id'])")
curl -s -o /dev/null -X PUT "$BASE/api/ai-content-markers/grammar_meaning/$GMID/review" -H 'Content-Type: application/json' -d '{"fieldName":"meaningContent","reviewed":true}'
GD2=$(curl -s $BASE/api/grammar/$GID)
check "Grammar reviewed" "$GD2" "meaningContent.*Reviewed"

# ====================================================================
hdr "3. DailyVocabulary: Create → Publish → Review"
# ====================================================================
DR=$(curl -s -X POST $BASE/api/daily-vocabulary -H 'Content-Type: application/json' \
  -d '{"phrase":"final test idiom","phraseType":"IDIOM","pinyin":"fti","plainExplanation":"AI explanation","originStory":"AI story","aiGeneratedFields":["plainExplanation","originStory"],"order":0}')
DID=$(echo "$DR" | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
curl -s -o /dev/null -X PUT $BASE/api/daily-vocabulary/$DID/review
curl -s -o /dev/null -X PUT $BASE/api/daily-vocabulary/$DID/publish
DD=$(curl -s $BASE/api/daily-vocabulary/$DID)
check "Daily AI" "$DD" "plainExplanation"
curl -s -o /dev/null -X PUT "$BASE/api/ai-content-markers/daily_vocabulary/$DID/review" -H 'Content-Type: application/json' -d '{"fieldName":"plainExplanation","reviewed":true}'
DD2=$(curl -s $BASE/api/daily-vocabulary/$DID)
check "Daily reviewed" "$DD2" "plainExplanation.*Reviewed"

# ====================================================================
hdr "4. CharCharacter: Create → Publish → Check"
# ====================================================================
CR=$(curl -s -X POST $BASE/api/characters -H 'Content-Type: application/json' \
  -d '{"character":"测","pinyin":"ce","comparisons":[{"comparisonChar":"侧","comparisonPinyin":"ce","aiGeneratedFields":["comparisonPinyin"]}],"words":[{"wordItem":"测试","pinyin":"ceshi","aiGeneratedFields":["wordItem"]}]}')
CID=$(echo "$CR" | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
curl -s -o /dev/null -X PUT $BASE/api/characters/$CID/review
curl -s -o /dev/null -X PUT $BASE/api/characters/$CID/publish
CD=$(curl -s $BASE/api/characters/$CID)
check "Char comparison AI" "$CD" "comparisonPinyin"
check "Char word AI" "$CD" "wordItem"

# ====================================================================
hdr "5. ExerciseQuestion: Create (no draft) → Check"
# ====================================================================
ER=$(curl -s -X POST $BASE/api/exercise-question -H 'Content-Type: application/json' \
  -d '{"questionType":"CHOICE","stem":"What is 1+1?","options":[{"option":"A","optionText":"2"},{"option":"B","optionText":"3"}],"answer":["A"],"explanation":"Basic math","aiGeneratedFields":["stem","explanation"],"sort":1}')
EID=$(echo "$ER" | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
ED=$(curl -s $BASE/api/exercise-question/$EID)
check "Exercise AI" "$ED" "stem"
curl -s -o /dev/null -X PUT "$BASE/api/ai-content-markers/exercise_question/$EID/review" -H 'Content-Type: application/json' -d '{"fieldName":"stem","reviewed":true}'
ED2=$(curl -s $BASE/api/exercise-question/$EID)
check "Exercise reviewed" "$ED2" "stem.*Reviewed"

# ====================================================================
hdr "6. VocabComparison: Create → Publish → Check"
# ====================================================================
VR=$(curl -s -X POST $BASE/api/vocab-comparison -H 'Content-Type: application/json' \
  -d '{"groupKey":"test vs test2","items":[{"word":"test","partOfSpeech":"n","usageComparison":"AI comparison","aiGeneratedFields":["usageComparison"]}],"chats":[{"role":"teacher","content":"AI dialogue","aiGeneratedFields":["content"]}]}')
VCID=$(echo "$VR" | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
curl -s -o /dev/null -X PUT $BASE/api/vocab-comparison/$VCID/review
curl -s -o /dev/null -X PUT $BASE/api/vocab-comparison/$VCID/publish
VD=$(curl -s $BASE/api/vocab-comparison/$VCID)
check "VocabComp item AI" "$VD" "usageComparison"
check "VocabComp chat AI" "$VD" "AI dialogue"

# ====================================================================
hdr "7. UPDATE: Full replace AI markers"
# ====================================================================
curl -s -o /dev/null -X PUT "$BASE/api/vocabulary/$VID" -H 'Content-Type: application/json' \
  -d "{\"word\":\"finaltest1\",\"pinyin\":\"ft\",\"senses\":[{\"id\":$SID,\"partOfSpeech\":\"n\",\"chineseDef\":\"updated def\",\"aiGeneratedFields\":[\"chineseDef\"],\"structures\":[{\"id\":$STID,\"pattern\":\"updated\",\"aiGeneratedFields\":[],\"structureSentences\":[{\"id\":$SEID,\"sentence\":\"Updated sentence.\",\"aiGeneratedFields\":[\"sentence\"]}]}]}]}"
curl -s -o /dev/null -X PUT $BASE/api/vocabulary/$VID/review
curl -s -o /dev/null -X PUT $BASE/api/vocabulary/$VID/publish
UD=$(curl -s $BASE/api/vocabulary/$VID)
UP=$(echo "$UD" | python -c "
import sys,json; d=json.load(sys.stdin); s=d['senses'][0]
print('sen:', s.get('aiGeneratedFields'))
st=s['structureExamples'][0]
print('stu:', st.get('aiGeneratedFields'))
se=st['structureExamples'][0]
print('sent:', se.get('aiGeneratedFields'))")
check "Update: only chineseDef AI" "$UP" "chineseDef"
check_not "Update: defTranslations gone" "$UP" "defTranslations"
check "Update: struct empty" "$UP" "stu.*\[\]"
check "Update: sent has AI" "$UP" "sentence"

# ====================================================================
hdr "RESULTS"
# ====================================================================
T=$((P+F))
echo "Passed: $P / $T"
if [ $F -gt 0 ]; then
  echo -e "\033[31mFailed: $F\033[0m"
  exit 1
else
  echo -e "\033[32mALL TESTS PASSED!\033[0m"
fi
