# 话题 (Topic) 功能设计

## 概述

新增话题功能模块，支持后台管理（草稿/审核/发布）和 APP 端用户查询。话题是汉语教学中的"话题点"（如"希望"），每个话题包含多个句式，每个句式包含一轮或多轮情景对话。

数据层级：**Topic → TopicPattern → TopicChat**（三层结构）

## 数据库设计

详见 `sql/biz_topic.sql`。

### topic（话题主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 话题ID |
| name | varchar(128) | 话题名称（如"希望"） |
| pinyin | varchar(256) | 拼音 |
| audio_id | bigint | 音频资源ID |
| cover_image_id | bigint | 封面图片资源ID |
| translations | text | 多语言翻译 JSON |
| draft_content | text | 草稿内容 JSON（整个 DTO 树） |
| edit_status | varchar(20) | draft / reviewed |
| publish_status | varchar(20) | unpublished / published |
| status | tinyint | 1=有效, 0=无效 |
| create_by / update_by | varchar(255) | 审计字段 |
| create_time / update_time | datetime | 审计字段 |

### topic_pattern（句式表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 句式ID |
| topic_id | bigint | 所属话题ID |
| pattern | varchar(512) | 句式文本 |
| image_id | bigint | 句式示意图资源ID |
| order | int | 排序权重（大的在前） |
| status | tinyint | 1=有效, 0=无效 |
| create_time / update_time | datetime | 时间戳 |

### topic_chat（情景对话表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint PK | 对话ID |
| topic_id | bigint | 所属话题ID（冗余） |
| pattern_id | bigint | 所属句式ID |
| role | varchar(20) | teacher / student |
| content | varchar(1024) | 中文对话内容 |
| example_sentence_id | bigint | 关联 example_sentence 表（发布时回填） |
| order | int | 排序权重 |
| status | tinyint | 1=有效, 0=无效 |
| create_time / update_time | datetime | 时间戳 |

### 草稿工作流（与汉字/词汇辨析一致）

- **创建/更新**：整个请求序列化写入 `topic.draft_content` JSON。不写子表。
- **审核**：`edit_status: draft → reviewed`（仅状态变更）
- **发布**：解析 draft JSON → 更新 topic 主表字段 → sync topic_pattern 子表 → sync topic_chat 子表（含 example_sentence 创建/回填）→ AI marker 物化 → 清除 draft_content
- **下线**：`publish_status: published → unpublished`（不删数据）
- **删除**：`status: 1 → 0`（软删除）

### "最新数据"查询逻辑

分页列表和详情查询：若 `edit_status` 为 draft 或 reviewed，从 `draft_content` JSON 反序列化输出；否则（published）从三张表查询已发布数据。草稿覆盖时仅覆盖业务字段，元数据（id/status/editStatus 等）始终取自主表。

## 后台管理接口

**Controller**: `TopicController`，路径 `/api/topic`，位于 `grid-system` 模块。

| 方法 | 路径 | 说明 | 请求/响应 |
|------|------|------|-----------|
| POST | `/` | 新增话题 | `TopicCreateRequest` → `TopicCreateVO {id}` |
| PUT | `/{id}` | 更新话题 | `TopicCreateRequest` → 204 |
| PUT | `/{id}/review` | 审核通过 | — → 204 |
| PUT | `/{id}/publish` | 发布 | — → 204 |
| GET | `/{id}` | 详情（草稿优先） | — → `TopicVO` |
| GET | `/` | 分页列表（草稿优先） | `TopicQueryRequest` → `PageResult<TopicBaseVO>` |
| DELETE | `/{id}` | 删除 | — → 204 |
| PUT | `/{id}/offline` | 下线 | — → 204 |

### 请求结构

`TopicCreateRequest`（同时也是 update 的请求体）：

```
name: String @NotBlank          — 话题名称
pinyin: String                  — 拼音
audioId: Long                   — 音频资源ID
coverImageId: Long              — 封面图ID
translations: List<TextTranslationRequest>  — 多语言翻译
patterns: List<TopicPatternRequest> @Valid  — 句式列表
aiGeneratedFields: List<String> — AI生成字段标记

TopicPatternRequest:
  pattern: String @NotBlank     — 句式文本
  imageId: Long                 — 示意图资源ID
  order: Integer                — 排序权重
  chats: List<TopicChatRequest> @Valid  — 情景对话列表  ← 嵌套在句式下面

TopicChatRequest（与 VocabChatRequest 结构一致）:
  role: String @NotBlank        — teacher / student
  content: String @NotBlank     — 对话内容
  pinyin: String                — 拼音
  translations: List<TextTranslationRequest>  — 翻译
  audioId: Long                 — 音频资源ID
  order: Integer                — 排序
  aiGeneratedFields: List<String>  — AI生成字段
```

### 分页查询

`TopicQueryRequest`：`blurry`（模糊搜索 name）、`publishStatus`、`editStatus`。参考 `CharCharacterQueryRequest` 模式，支持 `@Query(blurry = "name")` 的动态查询。

## APP 端接口

**Controller**: `AppTopicController`，路径 `/api/app/topic`，位于 `grid-app` 模块。

| 方法 | 路径 | 说明 | 参数 | 响应 |
|------|------|------|------|------|
| GET | `/search` | 搜索话题 | `AppTopicSearchRequest` | `List<AppTopicBaseVO>` |
| GET | `/{id}` | 话题详情 | `id` + `language` | `AppTopicDetailVO` |

### 搜索接口

- 只按 `name` 模糊搜索已发布话题（`publishStatus=published`）
- 返回 `List<AppTopicBaseVO>`
- 参考 `AppVocabWordController#search`

### 详情接口

- 入参：`id` (Long) + `language` (String, 必填)
- 出参：`AppTopicDetailVO`，包含话题基本信息 + patterns 列表 + 每个 pattern 下的 chats 列表
- 翻译字段按 language 过滤，返回单语言 TextTranslationVO
- 资源字段（audio、image）的 ID 转换为 URL（AudioVO / ImageVO），找不到打 ERROR 日志不报错
- **不返回**管理字段：createBy、updateBy、createTime、updateTime、editStatus、publishStatus、draftContent 等

### 资源 ID → URL 解析

Controller 层在调用 Wrapper 前预加载资源：
1. 遍历 DTO 树收集所有 audioId
2. 批量查询 `AudioResourceService#findByIds`
3. 遍历收集所有 imageId（cover + pattern 示意图）
4. 批量查询 `AliOssStorageService#findByIds`
5. 将 Map 传入 Wrapper，由 Wrapper 做 ID→URL 映射

参考 `AppCharCharacterController#getDetail` 的实现模式。

## 收藏夹集成

在现有收藏系统中新增 TOPIC 类型，修改 3 处：

1. **`CollectionBizTypeEnum`** — 新增 `TOPIC("TOPIC", "话题")`
2. **`CollectionWrapper.resolveContentName()`** — 新增 `case "TOPIC"` 调用 `TopicService.findById()` 返回话题 name
3. **`CollectionServiceImpl.validateContentExists()`** — 新增 `case "TOPIC"` 校验话题存在

## 文件清单

### grid-system 新增文件

```
backend/domain/topic/
  Topic.java                 — 话题 DO
  TopicPattern.java          — 句式 DO
  TopicChat.java             — 情景对话 DO

backend/repo/topic/
  TopicRepository.java       — 话题 Repository
  TopicPatternRepository.java
  TopicChatRepository.java

backend/service/topic/
  TopicService.java          — 服务接口
  impl/TopicServiceImpl.java — 服务实现（含草稿/审核/发布工作流）
  dto/TopicDto.java          — 话题 DTO
  dto/TopicPatternDto.java   — 句式 DTO
  dto/TopicChatDto.java      — 对话 DTO
  dto/TopicQueryCriteria.java — 查询条件

backend/rest/controller/
  TopicController.java       — 后台 Controller

backend/rest/request/
  TopicCreateRequest.java    — 创建/更新请求（含 TopicPatternRequest、TopicChatRequest 内部类）
  TopicQueryRequest.java     — 分页查询请求

backend/rest/vo/
  TopicCreateVO.java         — 创建响应 {id}
  TopicBaseVO.java           — 列表项 VO
  TopicVO.java               — 详情 VO（含 patterns + chats 嵌套）
  TopicPatternVO.java
  TopicChatVO.java

backend/rest/wrapper/
  TopicWrapper.java          — Request ↔ DTO ↔ VO 转换
```

### grid-app 新增文件

```
modules/app/rest/
  AppTopicController.java    — APP 端 Controller

modules/app/rest/request/
  AppTopicSearchRequest.java — 搜索请求

modules/app/rest/vo/
  AppTopicBaseVO.java        — APP 列表项 VO（含 ImageVO/AudioVO）
  AppTopicDetailVO.java      — APP 详情 VO
  AppTopicPatternVO.java     — APP 句式 VO
  AppTopicChatVO.java        — APP 对话 VO

modules/app/rest/wrapper/
  AppTopicWrapper.java       — APP 端 DTO → VO 转换（含单语言过滤、URL 解析）
```

### 修改现有文件

| 文件 | 修改内容 |
|------|----------|
| `CollectionBizTypeEnum.java` | 新增 `TOPIC("TOPIC", "话题")` |
| `CollectionWrapper.java` | 新增 `case "TOPIC"` resolveContentName |
| `CollectionServiceImpl.java` | 新增 `case "TOPIC"` validateContentExists |

**共计：约 30 个新文件 + 3 处修改。**
