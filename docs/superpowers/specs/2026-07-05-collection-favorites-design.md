# 用户收藏夹功能 — 设计说明书

**日期**: 2026-07-05  
**状态**: 已确认  
**模块**: grid-app (Service/Entity/Controller), grid-system (不涉及)

---

## 1. 概述

为 Little Grid 汉语学习平台开发用户收藏夹功能。用户可将不同业务模块（汉字、词汇、部首、语法、辨析等）的学习内容收藏到收藏夹中，支持默认收藏夹和自定义收藏夹，用于回顾和整理。

### 核心需求

- 每个用户注册时自动创建一个**默认收藏夹**（不可删除）
- 用户可创建**自定义收藏夹**（名称最多 32 字符，可选封面图）
- 每个收藏夹最多收藏 **500 条**有效内容（软删除的不计入）
- 支持按业务模块分组查看收藏内容
- 已收藏内容重复收藏时**幂等忽略**
- 收藏时校验内容 ID 在对应业务表中**真实存在**

---

## 2. 数据库设计

SQL 文件位置: `sql/biz_collection.sql`

### 2.1 收藏夹表 `biz_collection_folder`

```sql
CREATE TABLE `biz_collection_folder` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏夹ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `name`            VARCHAR(32) NOT NULL COMMENT '收藏夹名称',
    `cover_image_id`  BIGINT DEFAULT NULL COMMENT '封面图资源ID',
    `is_default`      TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认收藏夹：0-否 1-是',
    `is_pinned`       TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    `sort_order`      INT NOT NULL DEFAULT 0 COMMENT '排序权重（大在前）',
    `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效 0-已删除',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏夹表';
```

### 2.2 收藏内容表 `biz_collection_item`

```sql
CREATE TABLE `biz_collection_item` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏记录ID',
    `folder_id`       BIGINT NOT NULL COMMENT '所属收藏夹ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID（冗余，方便查询）',
    `biz_type`        VARCHAR(30) NOT NULL COMMENT '业务类型枚举',
    `content_id`      BIGINT DEFAULT NULL COMMENT '收藏的内容ID（如词汇ID、汉字ID等）',
    `content_text`    VARCHAR(1024) DEFAULT NULL COMMENT '收藏内容文本（用于无结构化ID的内容，如好词好句）',
    `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '有效状态：1-有效 0-已取消收藏',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_folder_id` (`folder_id`),
    KEY `idx_user_biz_content` (`user_id`, `biz_type`, `content_id`),
    KEY `idx_folder_status` (`folder_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏内容表';
```

### 2.3 设计要点

- **无外键**: 遵循项目规范，关联字段不使用数据库外键
- **content_id 可为 NULL**: 好词好句等自由文本内容仅存 content_text，无结构化 ID
- **biz_type 使用 VARCHAR**: 字符串枚举，新增业务类型无需改表结构
- **idx_user_biz_content**: 联合索引支持去重检查和收藏状态查询
- **userId 冗余**: 避免跨表关联用户查询
- **软删除**: status=0 表示删除/取消收藏，不计入 500 条限制

---

## 3. 业务类型枚举

### CollectionBizTypeEnum

| 值 | 说明 | 对应表 | 查询 Service |
|---|---|---|---|
| `CHARACTER` | 汉字 | `char_character` | CharCharacterService |
| `VOCABULARY` | 词汇 | `vocab_word` | VocabWordService |
| `RADICAL` | 部首 | `char_radical` | CharRadicalService |
| `GRAMMAR` | 语法 | `grammar_point` | GrammarPointService |
| `GRAMMAR_COMPARISON` | 语法辨析 | `grammar_comparison_group` | GrammarComparisonGroupService |
| `VOCAB_COMPARISON` | 词汇辨析 | `vocab_comparison_group` | VocabComparisonGroupService |

扩展：将来新增业务类型只需在枚举加值 + Service 中添加对应 dispatch 分支。

---

## 4. API 设计

基础路径: `/api/app/collection`（需登录认证，非 Anonymous）

### 4.1 收藏夹接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/folder` | 新建收藏夹 |
| `GET` | `/folder/list` | 查询我的收藏夹列表 |
| `GET` | `/folder/{folderId}` | 查询收藏夹详情 |
| `PUT` | `/folder/{folderId}/name` | 修改名称 |
| `PUT` | `/folder/{folderId}/cover` | 修改封面图 |
| `DELETE` | `/folder/{folderId}` | 删除收藏夹（级联软删） |
| `PUT` | `/folder/{folderId}/pin` | 置顶 |
| `PUT` | `/folder/{folderId}/unpin` | 取消置顶 |

### 4.2 收藏内容接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/item` | 添加内容到收藏夹 |
| `DELETE` | `/item/{itemId}` | 取消收藏（软删除） |
| `GET` | `/item/check` | 检查某内容是否已收藏 |

### 4.3 请求/响应结构

**新建收藏夹** `POST /folder`

```json
// Request
{ "name": "HSK1词汇", "coverImageId": 123 }
// Response
{ "id": 1, "name": "HSK1词汇", "coverImageId": 123,
  "isDefault": false, "isPinned": false, "itemCount": 0, "createTime": "..." }
```

**收藏夹列表** `GET /folder/list`

```json
// Response (排序: isPinned DESC, createTime DESC)
[{
  "id": 1, "name": "HSK1词汇", "coverImageId": 123,
  "isDefault": true, "isPinned": true, "itemCount": 15, "createTime": "..."
}]
```

**收藏夹详情** `GET /folder/{folderId}`

```json
// Response: 收藏夹基础信息 + 按 bizType 分组的收藏列表
{
  "id": 1, "name": "默认收藏夹", "coverImageId": null,
  "isDefault": true, "isPinned": false, "createTime": "...",
  "groups": {
    "CHARACTER": [
      { "id": 1, "contentId": 100, "contentName": "啊", "createTime": "..." }
    ],
    "VOCABULARY": [
      { "id": 2, "contentId": 200, "contentName": "学习", "createTime": "..." }
    ]
  }
}
// contentName 通过动态查询对应业务表获取
// 纯文本类收藏（无 contentId，仅 contentText）直接取 contentText 作为 contentName
// 每组内按 createTime DESC 排序
// groups key 由 biz_type 动态决定
```

**添加收藏** `POST /item`

```json
// Request
{ "folderId": 1, "bizType": "CHARACTER", "contentId": 100 }
// folderId 可选，不传则使用默认收藏夹
// contentText 可选，用于好词好句等无结构化ID的内容
// 已收藏则幂等忽略（不报错）
// 超过500条则抛异常
```

**检查收藏状态** `GET /item/check?bizType=CHARACTER&contentId=100`

```json
// Response (同一内容在多个收藏夹时返回最近收藏的一条)
{ "collected": true, "itemId": 1, "folderName": "默认收藏夹" }
// 未收藏: { "collected": false, "itemId": null, "folderName": null }
```

---

## 5. 代码架构

### 5.1 模块归属

全部代码位于 **grid-app** 模块，遵循现有 `AppAuthService` / `ReferralService` 的用户域服务模式。

### 5.2 文件清单

```
grid-app/src/main/java/com/naon/grid/modules/app/
├── domain/
│   ├── BizCollectionFolder.java
│   └── BizCollectionItem.java
├── enums/
│   └── CollectionBizTypeEnum.java
├── repository/
│   ├── BizCollectionFolderRepository.java
│   └── BizCollectionItemRepository.java
├── service/
│   ├── CollectionService.java
│   └── impl/
│       └── CollectionServiceImpl.java
├── rest/
│   ├── request/
│   │   ├── CreateFolderRequest.java
│   │   ├── UpdateFolderNameRequest.java
│   │   ├── UpdateFolderCoverRequest.java
│   │   └── AddItemRequest.java
│   ├── vo/
│   │   ├── CollectionFolderVO.java
│   │   ├── CollectionFolderDetailVO.java
│   │   ├── CollectionGroupVO.java
│   │   ├── CollectionItemVO.java
│   │   └── CollectionCheckVO.java
│   ├── wrapper/
│   │   └── CollectionWrapper.java
│   └── AppCollectionController.java
```

### 5.3 核心逻辑

**添加收藏流程**:
1. 校验 contentId 和 contentText 至少有一个非空
2. 获取当前 userId，校验 folderId 属于该用户
3. 根据 bizType dispatch 校验 contentId 在对应业务表中存在
4. 去重检查：同一用户+folder+bizType+contentId 已存在则幂等返回
5. 统计该文件夹有效条数（status=1），>=500 则抛 BadRequestException
6. 创建 BizCollectionItem 并保存

**详情查询流程**:
1. 查询收藏夹（校验归属）
2. 查询该收藏夹下所有 status=1 的 item
3. 按 bizType 分组
4. 每组 dispatch 到对应 backend Service 批量查询 contentName
5. 组装 CollectionFolderDetailVO 返回

**内容校验 dispatch**:
在 ServiceImpl 中按 bizType 调用对应 backend Service（CharCharacterService、VocabWordService 等），查询 contentId 是否存在。无结构化表的内容类型（contentText 模式）跳过 ID 校验。

---

## 6. 集成点

### 6.1 用户注册时创建默认收藏夹

在 `AppAuthServiceImpl.register()` 和 `AppAuthServiceImpl.createSocialUser()` 中，用户保存成功后调用：

```java
collectionService.createDefaultFolder(user.getId());
```

### 6.2 用户认证

所有接口通过 `AppTokenFilter` 认证，Controller 中使用 `AppSecurityUtils.getCurrentUserId()` 获取当前用户。

---

## 7. 错误处理

| 场景 | 处理 |
|------|------|
| 收藏夹名称超长 (>32) | `@Size` Bean Validation → 400 |
| 收藏夹名称重复 | 允许同名，不限制 |
| 收藏内容超 500 条 | `BadRequestException("收藏夹已满，最多收藏500条内容")` |
| 重复收藏同一内容 | 幂等忽略，静默跳过 |
| 内容 ID 不存在 | `BadRequestException("收藏的[汉字]不存在")` |
| 操作他人收藏夹 | userId 过滤 → `BadRequestException` |
| 删除默认收藏夹 | `BadRequestException("默认收藏夹不可删除")` |
| 无 folderId 且无默认收藏夹 | `BadRequestException("未指定收藏夹且默认收藏夹不存在")` |
| contentId 和 contentText 均为空 | `@Validated` → 400 |

---

## 8. 扩展性

### 新增业务板块接入步骤

1. 在 `CollectionBizTypeEnum` 添加新值
2. 在 `CollectionServiceImpl` 的 content validator dispatch 中添加分支
3. 在详情查询的 display name dispatch 中添加分支

无需改表结构、SQL、或前端分组逻辑。

### 收藏夹数量

当前不限制用户创建收藏夹的数量上限，后续可调整。

---

## 9. 前端交互（参考）

以下为前端设计参考，本次不实现：

1. **收藏夹列表页**: 展示用户所有收藏夹，按置顶+时间排序，可新建/编辑/删除
2. **收藏夹详情页**: 展示收藏夹信息 + 按业务模块分组的收藏内容列表
3. **学习详情页**: 根据 contentId + bizType 构造跳转链接，展示收藏/取消收藏按钮
4. **检查收藏状态**: 进入详情页时调用 `/item/check` 接口决定按钮状态

---

## 10. 设计决策记录

| 决策 | 选择 | 理由 |
|------|------|------|
| 内容元数据获取 | 动态查询 | 数据始终最新，符合项目现有模式 |
| 服务层位置 | grid-app | 收藏夹是用户域数据，遵循 AppAuthService 模式 |
| bizType 存储格式 | VARCHAR 字符串 | 可扩展，新增类型无需改表 |
| 收藏夹数量上限 | 暂不限制 | 后续可按需调整 |
| 内容ID校验 | 校验并报错 | 保证数据完整性 |
| 内容去重范围 | 同一文件夹内 | 允许同一内容在不同收藏夹 |
