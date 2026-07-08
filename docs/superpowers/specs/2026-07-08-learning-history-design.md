# 最近学习记录功能设计

> 日期：2026-07-08
> 状态：设计中

## 1. 概述

用户在各个学习模块查看详情后，上报学习记录。前端通过查询最近学习记录，展示用户最近学了什么，点击可跳转回对应模块的详情页继续学习。

核心行为：
- 学习某个内容后调用接口上报，存入 Redis
- 同一内容重复学习时，自动提序到最新
- 最多保留 50 条，超出自动淘汰最旧的
- 90 天无活动自动过期

## 2. 方案选型

选择 **Redis Sorted Set + Hash** 方案：

- **ZSet** 存 member 与时间戳 score，ZADD 天然支持重复 member 自动更新 score 实现提序
- **Hash** 配套存储 contentName，查询时 HMGET 批量获取，无需查 DB
- 相比于 List 方案省去遍历匹配的手动去重逻辑

## 3. API 设计

Base path: `/api/app/learning-history`（所有接口需登录）

### 3.1 添加/更新学习记录

```
POST /api/app/learning-history
```

Request:

```json
{
  "bizType": "CHARACTER",
  "contentId": 123
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `bizType` | String | 是 | 复用 `CollectionBizTypeEnum`：CHARACTER / VOCABULARY / RADICAL / GRAMMAR / GRAMMAR_COMPARISON / VOCAB_COMPARISON |
| `contentId` | Long | 是 | 模块内容 ID（CHARACTER/VOCABULARY 类型用 Integer，传入时统一为 Long，内部转换） |

处理流程：

1. 校验 bizType 是否已知枚举，否则拒绝
2. 查对应 backend service 校验 contentId 存在性，不存在抛错
3. 解析 contentName：CHARACTER → `getCharacter()`、VOCABULARY → `getWord()`、RADICAL → `getRadical()`、GRAMMAR → `getName()`、GRAMMAR_COMPARISON / VOCAB_COMPARISON → `getGroupKey()`
4. `ZADD learning:history:{userId} {bizType}:{contentId} {System.currentTimeMillis()}`
5. `HSET learning:history:meta:{userId} {bizType}:{contentId} {"contentName":"..."}`
6. `ZREMRANGEBYRANK` 裁剪超出 50 条，同步 `HDEL` 清理对应 Hash
7. `EXPIRE` 刷新两个 key 的 90 天 TTL

### 3.2 查询最近学习记录

```
GET /api/app/learning-history
```

Response（按项目约定直接返回数组，与 `AppCollectionController` 一致）:

```json
[
  {
    "bizType": "CHARACTER",
    "contentId": 123,
    "contentName": "好",
    "learnedAt": "2026-07-08T10:30:00"
  },
  {
    "bizType": "VOCABULARY",
    "contentId": 456,
    "contentName": "学习",
    "learnedAt": "2026-07-08T09:15:00"
  }
]
```

处理流程：

1. `ZREVRANGE learning:history:{userId} 0 49` 按 score 降序
2. `HMGET learning:history:meta:{userId} members...` 批量获取名称
3. 组装 VO 返回，按时间倒序

空列表时返回 `[]`。

### 3.3 删除单条记录

```
DELETE /api/app/learning-history/{bizType}/{contentId}
```

- `ZREM` 删除 member → `HDEL` 清理 Hash 字段
- 删除不存在的 member 幂等（ZREM 返回 0，不报错）

### 3.4 一键清空

```
DELETE /api/app/learning-history
```

- `DEL` 删除 ZSet key + Hash key
- 不存在 key 时 DEL 返回 0，幂等

## 4. Redis 存储设计

| 角色 | Key | 类型 | 说明 |
|------|-----|------|------|
| 排序+去重 | `learning:history:{userId}` | ZSet | member=`{bizType}:{contentId}`, score=毫秒时间戳 |
| 名称存储 | `learning:history:meta:{userId}` | Hash | field=`{bizType}:{contentId}`, value=`{"contentName":"..."}` |

TTL：90 天（7,776,000 秒），每次添加记录时刷新。

## 5. RedisUtils 扩展

需在 `RedisUtils` 中新增 5 个 ZSet 方法：

```java
// 添加/更新成员，score 相同时覆盖
boolean zAdd(String key, Object value, double score);

// 按排名范围删除（裁剪用）
long zRemRangeByRank(String key, long start, long end);

// 移除指定成员
long zRemove(String key, Object... values);

// 按 score 降序取范围
Set<Object> zRevRange(String key, long start, long end);

// 获取成员数量
long zCard(String key);
```

底层使用 `redisTemplate.opsForZSet()`，与现有 `opsForList`/`opsForHash` 模式一致。

## 6. 文件清单

### 6.1 新增文件（6 个）

| 文件 | 路径 | 说明 |
|------|------|------|
| Controller | `rest/AppLearningHistoryController.java` | 4 个接口 |
| Request | `rest/request/AddLearningHistoryRequest.java` | bizType + contentId |
| VO | `rest/vo/LearningHistoryItemVO.java` | bizType, contentId, contentName, learnedAt |
| Wrapper | `rest/wrapper/AppLearningHistoryWrapper.java` | 静态转换方法 |
| Service 接口 | `service/LearningHistoryService.java` | 4 个方法定义 |
| Service 实现 | `service/impl/LearningHistoryServiceImpl.java` | ZSet 操作实现 |

所有文件位于 `grid-app/src/main/java/com/naon/grid/modules/app/` 包下。

### 6.2 修改文件（1 个）

```
grid-common/src/main/java/com/naon/grid/utils/
└── RedisUtils.java                              # 新增 5 个 ZSet 方法
```

## 7. 前端路由映射

| bizType | 详情页路径 |
|---------|-----------|
| `CHARACTER` | `/character/{contentId}` |
| `VOCABULARY` | `/vocab/{contentId}` |
| `RADICAL` | `/char-radical/{contentId}` |
| `GRAMMAR` | `/grammar/{contentId}` |
| `GRAMMAR_COMPARISON` | `/grammar-comparison/{contentId}` |
| `VOCAB_COMPARISON` | `/vocab-comparison/{contentId}` |

前端拿到 `bizType` + `contentId` 即可拼接跳转路径。

## 8. 边界情况与错误处理

| 场景 | 处理 |
|------|------|
| bizType 不在枚举范围内 | `BadRequestException("不支持的业务类型")` |
| contentId 在对应模块不存在 | `BadRequestException("{模块描述}不存在")` |
| CHARACTER/VOCABULARY 的 Integer ID 超出范围 | 在 Service 层统一做 `Long → int` 转换，溢出抛 `BadRequestException` |
| 学习记录列表为空 | 返回 `[]`，不报错 |
| 重复添加同一内容 | ZADD 自动更新 score 提序，幂等 |
| 删除不存在的记录 | ZREM 返回 0，不报错，幂等 |
| 裁剪时清理 Hash | ZSet 裁剪后同步 HDEL，避免 Hash 膨胀 |

## 9. 调用时机

前端在各模块**详情页加载成功后**调用添加记录接口。各模块详情接口列表：

| 模块 | 详情接口 | bizType |
|------|---------|---------|
| 汉字 | `GET /api/app/character/{id}` | `CHARACTER` |
| 词汇 | `GET /api/app/vocab/{id}` | `VOCABULARY` |
| 部首 | `GET /api/app/char/radical/{id}` | `RADICAL` |
| 语法 | `GET /api/app/grammar/{id}` | `GRAMMAR` |
| 语法辨析 |（语法详情关联数据） | `GRAMMAR_COMPARISON` |
| 词汇辨析 | `GET /api/app/vocab/comparison/{groupId}` | `VOCAB_COMPARISON` |

调用方式：详情接口响应成功后，前端静默调用 `POST /api/app/learning-history`，返回 200 即完成上报。调用失败不影响详情页正常使用。
