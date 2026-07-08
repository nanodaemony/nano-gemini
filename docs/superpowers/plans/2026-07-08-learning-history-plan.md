# 最近学习记录功能 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现用户最近学习记录功能，使用 Redis ZSet+Hash 存储，支持添加/查询/单删/清空四个接口。

**Architecture:** Controller → Service → RedisUtils(ZSet)，遵循项目现有 Wrapper 模式。ZSet 存 member+时间戳实现排序去重，Hash 存名称实现查询免DB。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Redis (RedisTemplate), Fastjson2, Lombok

## Global Constraints

- 所有接口需登录，由 `AppTokenFilter` 统一拦截
- `bizType` 复用 `CollectionBizTypeEnum` 六种类型
- 最多保留 50 条记录，超出自动裁剪
- 90 天 TTL（7,776,000 秒），每次添加刷新
- Controller 不包含转换逻辑，所有权在 Wrapper 中
- RedisUtils 方法遵循现有 try-catch + log 模式
- VO 使用 Lombok `@Getter @Setter` + `Serializable`
- 包路径：`com.naon.grid.modules.app`

---

### Task 1: Extend RedisUtils with ZSet operations

**Files:**
- Modify: `grid-common/src/main/java/com/naon/grid/utils/RedisUtils.java`

**Produces (6 new methods, add before the `// ============================incr=============================` section):**
- `boolean zAdd(String key, Object value, double score)` — add/update member with score
- `long zRemRangeByRank(String key, long start, long end)` — remove members by rank range
- `long zRemove(String key, Object... values)` — remove specified members
- `Set<Object> zRevRange(String key, long start, long end)` — members by score desc (no scores)
- `Set<ZSetOperations.TypedTuple<Object>> zRevRangeWithScores(String key, long start, long end)` — members with scores desc
- `long zCard(String key)` — member count

- [ ] **Step 1: Add ZSet methods**

Open `grid-common/src/main/java/com/naon/grid/utils/RedisUtils.java`.
Add the following new section immediately before line 804 (`// ============================incr=============================`):

```java
    // ===============================zSet=============================

    /**
     * 添加/更新ZSet成员，member相同时score覆盖
     *
     * @param key   键
     * @param value 成员值
     * @param score 分数
     * @return true 成功 false 失败
     */
    public boolean zAdd(String key, Object value, double score) {
        try {
            Boolean added = redisTemplate.opsForZSet().add(key, value, score);
            return Boolean.TRUE.equals(added);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 按排名范围删除成员
     *
     * @param key   键
     * @param start 起始排名（含，0=最低score）
     * @param end   结束排名（含，-1=最高score）
     * @return 删除的成员数量
     */
    public long zRemRangeByRank(String key, long start, long end) {
        try {
            Long removed = redisTemplate.opsForZSet().removeRange(key, start, end);
            return removed != null ? removed : 0;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 移除指定成员
     *
     * @param key    键
     * @param values 成员值（可多个）
     * @return 移除的成员数量
     */
    public long zRemove(String key, Object... values) {
        try {
            Long removed = redisTemplate.opsForZSet().remove(key, values);
            return removed != null ? removed : 0;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 按score降序取范围
     *
     * @param key   键
     * @param start 起始位置
     * @param end   结束位置
     * @return 成员集合（按score降序）
     */
    public Set<Object> zRevRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().reverseRange(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 按score降序取范围（带score）
     *
     * @param key   键
     * @param start 起始位置
     * @param end   结束位置
     * @return 带score的成员集合（按score降序）
     */
    public Set<ZSetOperations.TypedTuple<Object>> zRevRangeWithScores(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取ZSet成员数量
     *
     * @param key 键
     * @return 成员数量
     */
    public long zCard(String key) {
        try {
            Long size = redisTemplate.opsForZSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }
```

Also add the required import at the top of the file (near the other Redis imports):

```java
import org.springframework.data.redis.core.ZSetOperations;
```

- [ ] **Step 2: Verify compilation**

```bash
cd grid-common && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-common/src/main/java/com/naon/grid/utils/RedisUtils.java
git commit -m "feat: add ZSet operations to RedisUtils (zAdd, zRemRangeByRank, zRemove, zRevRange, zRevRangeWithScores, zCard)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Task 2: Create AddLearningHistoryRequest DTO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AddLearningHistoryRequest.java`

**Produces:**
- `AddLearningHistoryRequest` class with `bizType: String` and `contentId: Long`

- [ ] **Step 1: Create the request class**

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class AddLearningHistoryRequest {

    @NotBlank(message = "业务类型不能为空")
    @ApiModelProperty(value = "业务类型：CHARACTER/VOCABULARY/RADICAL/GRAMMAR/GRAMMAR_COMPARISON/VOCAB_COMPARISON", required = true)
    private String bizType;

    @NotNull(message = "内容ID不能为空")
    @ApiModelProperty(value = "学习内容ID", required = true)
    private Long contentId;
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd grid-app && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AddLearningHistoryRequest.java
git commit -m "feat: add AddLearningHistoryRequest DTO

Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Task 3: Create LearningHistoryItemVO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/LearningHistoryItemVO.java`

**Produces:**
- `LearningHistoryItemVO` with `bizType`, `contentId`, `contentName`, `learnedAt`

- [ ] **Step 1: Create the VO class**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class LearningHistoryItemVO implements Serializable {

    @ApiModelProperty("业务类型：CHARACTER/VOCABULARY/RADICAL/GRAMMAR/GRAMMAR_COMPARISON/VOCAB_COMPARISON")
    private String bizType;

    @ApiModelProperty("内容ID")
    private Long contentId;

    @ApiModelProperty("内容展示名称")
    private String contentName;

    @ApiModelProperty(value = "学习时间", example = "2026-07-08 10:30:00")
    private String learnedAt;
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd grid-app && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/LearningHistoryItemVO.java
git commit -m "feat: add LearningHistoryItemVO

Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Task 4: Create LearningHistoryService interface

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/LearningHistoryService.java`

**Consumes:**
- `LearningHistoryItemVO` type from Task 3

**Produces:**
- `LearningHistoryService` interface with 4 methods

- [ ] **Step 1: Create the service interface**

```java
package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.rest.vo.LearningHistoryItemVO;

import java.util.List;

public interface LearningHistoryService {

    /**
     * 添加或更新学习记录（已存在则提序到最新）
     *
     * @param userId    用户ID
     * @param bizType   业务类型
     * @param contentId 内容ID
     */
    void addRecord(Long userId, String bizType, Long contentId);

    /**
     * 查询最近学习记录（最多50条，按学习时间倒序）
     *
     * @param userId 用户ID
     * @return 学习记录列表
     */
    List<LearningHistoryItemVO> getHistory(Long userId);

    /**
     * 删除单条学习记录
     *
     * @param userId    用户ID
     * @param bizType   业务类型
     * @param contentId 内容ID
     */
    void removeRecord(Long userId, String bizType, Long contentId);

    /**
     * 清空所有学习记录
     *
     * @param userId 用户ID
     */
    void clearAll(Long userId);
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd grid-app && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/LearningHistoryService.java
git commit -m "feat: add LearningHistoryService interface

Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Task 5: Create LearningHistoryServiceImpl

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/LearningHistoryServiceImpl.java`

**Consumes:**
- `RedisUtils` ZSet + Hash methods from Task 1
- `LearningHistoryService` interface from Task 4
- `CollectionBizTypeEnum` from `com.naon.grid.modules.app.enums.CollectionBizTypeEnum`
- Backend services: `CharCharacterService.findById(Integer)`, `VocabWordService.findById(Integer)`, `CharRadicalService.findById(Long)`, `GrammarPointService.findById(Long)`, `GrammarComparisonGroupService.findById(Long)`, `VocabComparisonGroupService.findById(Long)`
- `BadRequestException` from `com.naon.grid.exception.BadRequestException`
- `LearningHistoryItemVO` from Task 3

**Produces:**
- Full service implementation coordinating ZSet+Hash operations

- [ ] **Step 1: Write the implementation**

```java
package com.naon.grid.modules.app.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.character.dto.CharCharacterDto;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.charradical.dto.CharRadicalDto;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammar.dto.GrammarPointDto;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.grammarcomparison.dto.GrammarComparisonGroupDto;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.dto.VocabComparisonGroupDto;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.backend.service.vocabulary.dto.VocabWordDto;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.enums.CollectionBizTypeEnum;
import com.naon.grid.modules.app.rest.vo.LearningHistoryItemVO;
import com.naon.grid.modules.app.service.LearningHistoryService;
import com.naon.grid.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningHistoryServiceImpl implements LearningHistoryService {

    private static final int MAX_SIZE = 50;
    private static final long TTL_SECONDS = 7776000L; // 90 days
    private static final String ZSET_KEY_PREFIX = "learning:history:";
    private static final String HASH_KEY_PREFIX = "learning:history:meta:";

    private final RedisUtils redisUtils;
    private final CharCharacterService charCharacterService;
    private final VocabWordService vocabWordService;
    private final CharRadicalService charRadicalService;
    private final GrammarPointService grammarPointService;
    private final GrammarComparisonGroupService grammarComparisonGroupService;
    private final VocabComparisonGroupService vocabComparisonGroupService;

    @Override
    public void addRecord(Long userId, String bizType, Long contentId) {
        // 1. validate bizType
        CollectionBizTypeEnum type = CollectionBizTypeEnum.fromCode(bizType);
        if (type == null) {
            throw new BadRequestException("不支持的业务类型: " + bizType);
        }

        // 2. validate contentId exists + resolve contentName
        String contentName = resolveContentName(type, contentId);

        // 3. build keys and member
        String zsetKey = ZSET_KEY_PREFIX + userId;
        String hashKey = HASH_KEY_PREFIX + userId;
        String member = bizType + ":" + contentId;
        double score = System.currentTimeMillis();

        // 4. ZADD (auto updates score if member exists)
        redisUtils.zAdd(zsetKey, member, score);

        // 5. HSET metadata
        JSONObject meta = new JSONObject();
        meta.put("contentName", contentName);
        redisUtils.hset(hashKey, member, meta.toJSONString());

        // 6. trim to MAX_SIZE, clean orphan Hash fields
        long count = redisUtils.zCard(zsetKey);
        if (count > MAX_SIZE) {
            long toRemove = count - MAX_SIZE;
            // get the entries to be removed (oldest = lowest score = ranks 0..toRemove-1)
            Set<Object> removedMembers = redisUtils.zRevRange(zsetKey, MAX_SIZE, count - 1);
            redisUtils.zRemRangeByRank(zsetKey, 0, toRemove - 1);
            if (removedMembers != null) {
                for (Object rm : removedMembers) {
                    redisUtils.hdel(hashKey, rm);
                }
            }
        }

        // 7. refresh TTL on both keys
        redisUtils.expire(zsetKey, TTL_SECONDS);
        redisUtils.expire(hashKey, TTL_SECONDS);
    }

    @Override
    public List<LearningHistoryItemVO> getHistory(Long userId) {
        String zsetKey = ZSET_KEY_PREFIX + userId;
        String hashKey = HASH_KEY_PREFIX + userId;

        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisUtils.zRevRangeWithScores(zsetKey, 0, MAX_SIZE - 1);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<LearningHistoryItemVO> result = new ArrayList<>();

        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Object value = tuple.getValue();
            Double score = tuple.getScore();
            if (value == null) continue;

            String member = String.valueOf(value);
            int colonIdx = member.lastIndexOf(':');
            if (colonIdx < 0) continue;

            String bizType = member.substring(0, colonIdx);
            String contentIdStr = member.substring(colonIdx + 1);
            Long contentId;
            try {
                contentId = Long.parseLong(contentIdStr);
            } catch (NumberFormatException e) {
                continue;
            }

            // get contentName from Hash
            Object metaObj = redisUtils.hget(hashKey, member);
            String contentName = null;
            if (metaObj != null) {
                JSONObject meta = JSON.parseObject(metaObj.toString());
                contentName = meta.getString("contentName");
            }

            LearningHistoryItemVO vo = new LearningHistoryItemVO();
            vo.setBizType(bizType);
            vo.setContentId(contentId);
            vo.setContentName(contentName);
            if (score != null) {
                vo.setLearnedAt(sdf.format(new Date(score.longValue())));
            }
            result.add(vo);
        }

        return result;
    }

    @Override
    public void removeRecord(Long userId, String bizType, Long contentId) {
        String zsetKey = ZSET_KEY_PREFIX + userId;
        String hashKey = HASH_KEY_PREFIX + userId;
        String member = bizType + ":" + contentId;

        redisUtils.zRemove(zsetKey, member);
        redisUtils.hdel(hashKey, member);
    }

    @Override
    public void clearAll(Long userId) {
        redisUtils.del(ZSET_KEY_PREFIX + userId, HASH_KEY_PREFIX + userId);
    }

    /**
     * Resolve content name by bizType and contentId.
     * Mirrors CollectionWrapper.resolveContentName() pattern.
     * All backend service findById methods throw EntityNotFoundException on miss.
     */
    private String resolveContentName(CollectionBizTypeEnum type, Long contentId) {
        try {
            switch (type) {
                case CHARACTER: {
                    if (contentId > Integer.MAX_VALUE || contentId < Integer.MIN_VALUE) {
                        throw new BadRequestException("汉字ID超出范围");
                    }
                    CharCharacterDto dto = charCharacterService.findById(contentId.intValue());
                    return dto != null ? dto.getCharacter() : null;
                }
                case VOCABULARY: {
                    if (contentId > Integer.MAX_VALUE || contentId < Integer.MIN_VALUE) {
                        throw new BadRequestException("词汇ID超出范围");
                    }
                    VocabWordDto dto = vocabWordService.findById(contentId.intValue());
                    return dto != null ? dto.getWord() : null;
                }
                case RADICAL: {
                    CharRadicalDto dto = charRadicalService.findById(contentId);
                    return dto != null ? dto.getRadical() : null;
                }
                case GRAMMAR: {
                    GrammarPointDto dto = grammarPointService.findById(contentId);
                    return dto != null ? dto.getName() : null;
                }
                case GRAMMAR_COMPARISON: {
                    GrammarComparisonGroupDto dto = grammarComparisonGroupService.findById(contentId);
                    return dto != null ? dto.getGroupKey() : null;
                }
                case VOCAB_COMPARISON: {
                    VocabComparisonGroupDto dto = vocabComparisonGroupService.findById(contentId);
                    return dto != null ? dto.getGroupKey() : null;
                }
                default:
                    return null;
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException(type.getDescription() + "不存在");
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd grid-app && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/LearningHistoryServiceImpl.java
git commit -m "feat: add LearningHistoryServiceImpl with ZSet+Hash coordination

Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Task 6: Create AppLearningHistoryWrapper + Controller

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppLearningHistoryWrapper.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppLearningHistoryController.java`

**Consumes:**
- `LearningHistoryService` interface from Task 4
- `LearningHistoryItemVO` from Task 3
- `AddLearningHistoryRequest` from Task 2
- `AppSecurityUtils.getCurrentUserId()` for auth

**Produces:**
- Wrapper: static utility methods for any future conversion needs
- Controller: 4 REST endpoints at `/api/app/learning-history`

- [ ] **Step 1: Create the Wrapper**

```java
package com.naon.grid.modules.app.rest.wrapper;

/**
 * 学习记录 VO 包装器
 * 当前返回的 LearningHistoryItemVO 由 Service 层直接构建，
 * Wrapper 保留作为未来扩展点（如需要统一格式化、过滤等）。
 */
public class AppLearningHistoryWrapper {

    private AppLearningHistoryWrapper() {
    }
}
```

- [ ] **Step 2: Create the Controller**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.Log;
import com.naon.grid.modules.app.rest.request.AddLearningHistoryRequest;
import com.naon.grid.modules.app.rest.vo.LearningHistoryItemVO;
import com.naon.grid.modules.app.service.LearningHistoryService;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/learning-history")
@Api(tags = "用户：学习记录接口")
public class AppLearningHistoryController {

    private final LearningHistoryService learningHistoryService;

    @Log("添加学习记录")
    @ApiOperation("添加或更新学习记录（重复学习自动提序）")
    @PostMapping
    public ResponseEntity<Void> addRecord(
            @Validated @RequestBody AddLearningHistoryRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        learningHistoryService.addRecord(userId, request.getBizType(), request.getContentId());
        return ResponseEntity.ok().build();
    }

    @ApiOperation("查询最近学习记录（最多50条，按时间倒序）")
    @GetMapping
    public ResponseEntity<List<LearningHistoryItemVO>> getHistory() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(learningHistoryService.getHistory(userId));
    }

    @Log("删除学习记录")
    @ApiOperation("删除单条学习记录")
    @DeleteMapping("/{bizType}/{contentId}")
    public ResponseEntity<Void> removeRecord(
            @PathVariable String bizType,
            @PathVariable Long contentId) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        learningHistoryService.removeRecord(userId, bizType, contentId);
        return ResponseEntity.ok().build();
    }

    @Log("清空学习记录")
    @ApiOperation("清空所有学习记录")
    @DeleteMapping
    public ResponseEntity<Void> clearAll() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        learningHistoryService.clearAll(userId);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd grid-app && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppLearningHistoryWrapper.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppLearningHistoryController.java
git commit -m "feat: add AppLearningHistoryController and Wrapper

Co-Authored-By: Claude <noreply@anthropic.com>"
```

### Task 7: Integration verification

- [ ] **Step 1: Full project compilation**

```bash
mvn compile -q
```

Expected: BUILD SUCCESS for all modules

- [ ] **Step 2: Verify all files are tracked**

```bash
git status
```

Expected: clean working tree (all files committed)

- [ ] **Step 3: Start application and verify Swagger**

```bash
cd grid-bootstrap && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Open http://localhost:8000/doc.html, verify:
- "用户：学习记录接口" tag group appears with 4 endpoints
- POST `/api/app/learning-history` has bizType + contentId parameters
- GET `/api/app/learning-history` returns `List<LearningHistoryItemVO>`
- DELETE `/api/app/learning-history/{bizType}/{contentId}` takes path params
- DELETE `/api/app/learning-history` takes no params

- [ ] **Step 4: Commit final verification**

```bash
# If any minor fixes were needed:
git add -A && git commit -m "chore: final verification adjustments

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## Quick Reference: Redis Key Pattern

| Purpose | Key | Type | Example Value |
|---------|-----|------|---------------|
| Ordering | `learning:history:{userId}` | ZSet | member=`CHARACTER:123`, score=`1751880000000` |
| Metadata | `learning:history:meta:{userId}` | Hash | field=`CHARACTER:123`, value=`{"contentName":"好"}` |

## Quick Reference: API Summary

| Method | Path | Request | Response | Auth |
|--------|------|---------|----------|------|
| POST | `/api/app/learning-history` | `{bizType, contentId}` | `200 OK` | Yes |
| GET | `/api/app/learning-history` | — | `[LearningHistoryItemVO]` | Yes |
| DELETE | `/api/app/learning-history/{bizType}/{contentId}` | — | `200 OK` | Yes |
| DELETE | `/api/app/learning-history` | — | `200 OK` | Yes |
