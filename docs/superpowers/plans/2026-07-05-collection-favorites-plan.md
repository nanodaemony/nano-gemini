# 用户收藏夹功能 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Little Grid 汉语学习平台开发用户收藏夹功能，支持默认收藏夹和自定义收藏夹，用户可收藏不同业务模块的学习内容。

**Architecture:** 全部代码位于 grid-app 模块，遵循现有 ReferralService 的用户域服务模式。实体/仓库/服务/控制器均在 grid-app 下，Controller 通过注入的 backend Service（CharCharacterService、VocabWordService 等）校验内容ID存在性并获取展示名称。AppTokenFilter 提供认证，AppSecurityUtils.getCurrentUserId() 获取当前用户。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, Lombok, Fastjson2, Knife4j (Swagger)

## Global Constraints

- Java 8，所有代码不使用 Java 9+ 特性
- 实体遵循项目规范：无数据库外键，软删除使用 `status` 字段（1=有效, 0=删除）
- Controller 不得包含转换逻辑：所有 Entity→VO 转换通过 Wrapper 静态方法完成
- 认证接口使用 `@GetMapping`/`@PostMapping` 等标准注解（非 Anonymous），当前用户通过 `AppSecurityUtils.getCurrentUserId()` 获取
- bizType 使用 VARCHAR 字符串枚举，支持扩展
- SQL 文件写到 `sql/biz_collection.sql`
- 收藏夹名称最多 32 个字符
- 每个收藏夹最多收藏 500 条有效内容（status=1 的计数）
- 内容 ID 校验：根据 bizType 分派到对应的 backend Service 验证存在性

---

### Task 1: 创建 SQL 文件

**Files:**
- Create: `sql/biz_collection.sql`

**Interfaces:**
- Produces: 两个表 `biz_collection_folder` 和 `biz_collection_item`，供 Task 2 实体映射

- [ ] **Step 1: 编写完整的 SQL DDL 文件**

```sql
-- ----------------------------
-- 用户收藏夹功能
-- ----------------------------
DROP TABLE IF EXISTS `biz_collection_item`;
DROP TABLE IF EXISTS `biz_collection_folder`;

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

CREATE TABLE `biz_collection_item` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏记录ID',
    `folder_id`       BIGINT NOT NULL COMMENT '所属收藏夹ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID（冗余，方便查询）',
    `biz_type`        VARCHAR(30) NOT NULL COMMENT '业务类型枚举：CHARACTER/VOCABULARY/RADICAL/GRAMMAR/GRAMMAR_COMPARISON/VOCAB_COMPARISON',
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

- [ ] **Step 2: 提交**

```bash
git add sql/biz_collection.sql
git commit -m "feat: add collection folder and item tables DDL

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 2: 创建业务类型枚举

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/enums/CollectionBizTypeEnum.java`

**Interfaces:**
- Produces: `CollectionBizTypeEnum` 枚举类，供 Task 5 Service、Task 8 Controller、Task 9 Request 使用

- [ ] **Step 1: 编写枚举类**

```java
package com.naon.grid.modules.app.enums;

import lombok.Getter;

/**
 * 收藏业务类型枚举
 */
@Getter
public enum CollectionBizTypeEnum {

    CHARACTER("CHARACTER", "汉字"),
    VOCABULARY("VOCABULARY", "词汇"),
    RADICAL("RADICAL", "部首"),
    GRAMMAR("GRAMMAR", "语法"),
    GRAMMAR_COMPARISON("GRAMMAR_COMPARISON", "语法辨析"),
    VOCAB_COMPARISON("VOCAB_COMPARISON", "词汇辨析");

    private final String code;
    private final String description;

    CollectionBizTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CollectionBizTypeEnum fromCode(String code) {
        for (CollectionBizTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/enums/CollectionBizTypeEnum.java
git commit -m "feat: add CollectionBizTypeEnum for collection content types

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 3: 创建域实体类

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/domain/BizCollectionFolder.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/domain/BizCollectionItem.java`

**Interfaces:**
- Produces: `BizCollectionFolder` 实体（映射 `biz_collection_folder` 表），`BizCollectionItem` 实体（映射 `biz_collection_item` 表），供 Task 4 Repository、Task 5 Service 使用

- [ ] **Step 1: 编写 BizCollectionFolder 实体**

```java
package com.naon.grid.modules.app.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "biz_collection_folder")
public class BizCollectionFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 32, nullable = false)
    private String name;

    @Column(name = "cover_image_id")
    private Long coverImageId;

    @Column(name = "is_default", nullable = false)
    private Integer isDefault = 0;

    @Column(name = "is_pinned", nullable = false)
    private Integer isPinned = 0;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 编写 BizCollectionItem 实体**

```java
package com.naon.grid.modules.app.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "biz_collection_item")
public class BizCollectionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folder_id", nullable = false)
    private Long folderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "biz_type", length = 30, nullable = false)
    private String bizType;

    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "content_text", length = 1024)
    private String contentText;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/domain/BizCollectionFolder.java grid-app/src/main/java/com/naon/grid/modules/app/domain/BizCollectionItem.java
git commit -m "feat: add BizCollectionFolder and BizCollectionItem entities

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 4: 创建 Repository 接口

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/repository/BizCollectionFolderRepository.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/repository/BizCollectionItemRepository.java`

**Interfaces:**
- Consumes: `BizCollectionFolder`, `BizCollectionItem` 实体 (Task 3)
- Produces: Spring Data JPA Repository 接口，供 Task 5 Service 使用

- [ ] **Step 1: 编写 BizCollectionFolderRepository**

```java
package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.BizCollectionFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BizCollectionFolderRepository extends JpaRepository<BizCollectionFolder, Long> {

    List<BizCollectionFolder> findByUserIdAndStatusOrderByIsPinnedDescCreateTimeDesc(
            Long userId, Integer status);

    Optional<BizCollectionFolder> findByUserIdAndIsDefaultAndStatus(
            Long userId, Integer isDefault, Integer status);

    Optional<BizCollectionFolder> findByIdAndUserIdAndStatus(
            Long id, Long userId, Integer status);

    @Modifying
    @Query("UPDATE BizCollectionFolder f SET f.status = 0 WHERE f.id = :folderId")
    int softDeleteById(@Param("folderId") Long folderId);
}
```

- [ ] **Step 2: 编写 BizCollectionItemRepository**

```java
package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.BizCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BizCollectionItemRepository extends JpaRepository<BizCollectionItem, Long> {

    List<BizCollectionItem> findByFolderIdAndStatusOrderByCreateTimeDesc(
            Long folderId, Integer status);

    long countByFolderIdAndStatus(Long folderId, Integer status);

    Optional<BizCollectionItem> findByFolderIdAndUserIdAndBizTypeAndContentIdAndStatus(
            Long folderId, Long userId, String bizType, Long contentId, Integer status);

    Optional<BizCollectionItem> findFirstByUserIdAndBizTypeAndContentIdAndStatusOrderByCreateTimeDesc(
            Long userId, String bizType, Long contentId, Integer status);

    @Modifying
    @Query("UPDATE BizCollectionItem i SET i.status = 0 WHERE i.id = :itemId")
    int softDeleteById(@Param("itemId") Long itemId);

    @Modifying
    @Query("UPDATE BizCollectionItem i SET i.status = 0 WHERE i.folderId = :folderId")
    int softDeleteByFolderId(@Param("folderId") Long folderId);
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/repository/BizCollectionFolderRepository.java grid-app/src/main/java/com/naon/grid/modules/app/repository/BizCollectionItemRepository.java
git commit -m "feat: add collection folder and item repositories

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 5: 创建 Service 接口和实现

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/CollectionService.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/CollectionServiceImpl.java`

**Interfaces:**
- Consumes: `BizCollectionFolder` (Task 3), `BizCollectionItem` (Task 3), Repositories (Task 4), `CollectionBizTypeEnum` (Task 2); Backend Services: `CharCharacterService`, `VocabWordService`, `CharRadicalService`, `GrammarPointService`, `GrammarComparisonGroupService`, `VocabComparisonGroupService`
- Produces: `CollectionService` 接口和 `CollectionServiceImpl` 实现，供 Task 8 Controller、Task 10 注册集成使用

- [ ] **Step 1: 编写 CollectionService 接口**

```java
package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.domain.BizCollectionFolder;
import com.naon.grid.modules.app.domain.BizCollectionItem;

import java.util.List;
import java.util.Map;

public interface CollectionService {

    /**
     * 创建默认收藏夹（用户注册时调用）
     */
    BizCollectionFolder createDefaultFolder(Long userId);

    /**
     * 新建自定义收藏夹
     */
    BizCollectionFolder createFolder(Long userId, String name, Long coverImageId);

    /**
     * 查询用户的所有收藏夹列表（按置顶+时间排序）
     */
    List<BizCollectionFolder> listFolders(Long userId);

    /**
     * 查询收藏夹详情（含归属校验）
     */
    BizCollectionFolder getFolder(Long folderId, Long userId);

    /**
     * 修改收藏夹名称
     */
    void updateFolderName(Long folderId, Long userId, String name);

    /**
     * 修改收藏夹封面图
     */
    void updateFolderCover(Long folderId, Long userId, Long coverImageId);

    /**
     * 删除收藏夹（级联软删所有收藏项）
     */
    void deleteFolder(Long folderId, Long userId);

    /**
     * 置顶收藏夹
     */
    void pinFolder(Long folderId, Long userId);

    /**
     * 取消置顶
     */
    void unpinFolder(Long folderId, Long userId);

    /**
     * 添加内容到收藏夹（folderId为null时使用默认收藏夹）
     */
    void addItem(Long userId, Long folderId, String bizType, Long contentId, String contentText);

    /**
     * 取消收藏（软删除）
     */
    void removeItem(Long itemId, Long userId);

    /**
     * 查询收藏夹下的所有有效收藏项（按bizType分组）
     */
    Map<String, List<BizCollectionItem>> getFolderItemsGrouped(Long folderId);

    /**
     * 查询某个内容是否已收藏
     * @return 收藏项，未收藏返回null
     */
    BizCollectionItem checkCollected(Long userId, String bizType, Long contentId);

    /**
     * 获取有效收藏项计数
     */
    long countActiveItems(Long folderId);
}
```

- [ ] **Step 2: 编写 CollectionServiceImpl 实现**

```java
package com.naon.grid.modules.app.service.impl;

import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.BizCollectionFolder;
import com.naon.grid.modules.app.domain.BizCollectionItem;
import com.naon.grid.modules.app.enums.CollectionBizTypeEnum;
import com.naon.grid.modules.app.repository.BizCollectionFolderRepository;
import com.naon.grid.modules.app.repository.BizCollectionItemRepository;
import com.naon.grid.modules.app.service.CollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private static final int MAX_ITEMS_PER_FOLDER = 500;

    private final BizCollectionFolderRepository folderRepository;
    private final BizCollectionItemRepository itemRepository;
    private final CharCharacterService charCharacterService;
    private final VocabWordService vocabWordService;
    private final CharRadicalService charRadicalService;
    private final GrammarPointService grammarPointService;
    private final GrammarComparisonGroupService grammarComparisonGroupService;
    private final VocabComparisonGroupService vocabComparisonGroupService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizCollectionFolder createDefaultFolder(Long userId) {
        BizCollectionFolder folder = new BizCollectionFolder();
        folder.setUserId(userId);
        folder.setName("默认收藏夹");
        folder.setIsDefault(1);
        folder.setIsPinned(0);
        folder.setSortOrder(0);
        folder.setStatus(1);
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());
        return folderRepository.save(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizCollectionFolder createFolder(Long userId, String name, Long coverImageId) {
        BizCollectionFolder folder = new BizCollectionFolder();
        folder.setUserId(userId);
        folder.setName(name);
        folder.setCoverImageId(coverImageId);
        folder.setIsDefault(0);
        folder.setIsPinned(0);
        folder.setSortOrder(0);
        folder.setStatus(1);
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());
        return folderRepository.save(folder);
    }

    @Override
    public List<BizCollectionFolder> listFolders(Long userId) {
        return folderRepository.findByUserIdAndStatusOrderByIsPinnedDescCreateTimeDesc(userId, 1);
    }

    @Override
    public BizCollectionFolder getFolder(Long folderId, Long userId) {
        return folderRepository.findByIdAndUserIdAndStatus(folderId, userId, 1)
                .orElseThrow(() -> new BadRequestException("收藏夹不存在"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFolderName(Long folderId, Long userId, String name) {
        BizCollectionFolder folder = getFolder(folderId, userId);
        folder.setName(name);
        folder.setUpdateTime(LocalDateTime.now());
        folderRepository.save(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFolderCover(Long folderId, Long userId, Long coverImageId) {
        BizCollectionFolder folder = getFolder(folderId, userId);
        folder.setCoverImageId(coverImageId);
        folder.setUpdateTime(LocalDateTime.now());
        folderRepository.save(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long folderId, Long userId) {
        BizCollectionFolder folder = getFolder(folderId, userId);
        if (folder.getIsDefault() == 1) {
            throw new BadRequestException("默认收藏夹不可删除");
        }
        folderRepository.softDeleteById(folderId);
        itemRepository.softDeleteByFolderId(folderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pinFolder(Long folderId, Long userId) {
        BizCollectionFolder folder = getFolder(folderId, userId);
        folder.setIsPinned(1);
        folder.setUpdateTime(LocalDateTime.now());
        folderRepository.save(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpinFolder(Long folderId, Long userId) {
        BizCollectionFolder folder = getFolder(folderId, userId);
        folder.setIsPinned(0);
        folder.setUpdateTime(LocalDateTime.now());
        folderRepository.save(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(Long userId, Long folderId, String bizType, Long contentId, String contentText) {
        // 1. 校验 contentId 和 contentText 至少有一个
        if (contentId == null && (contentText == null || contentText.trim().isEmpty())) {
            throw new BadRequestException("contentId和contentText至少需要提供一个");
        }

        // 2. 确定目标收藏夹
        BizCollectionFolder folder;
        if (folderId != null) {
            folder = getFolder(folderId, userId);
        } else {
            folder = folderRepository.findByUserIdAndIsDefaultAndStatus(userId, 1, 1)
                    .orElseThrow(() -> new BadRequestException("未指定收藏夹且默认收藏夹不存在"));
        }

        // 3. 校验内容 ID 存在性（如果有 contentId）
        if (contentId != null) {
            validateContentExists(bizType, contentId);
        }

        // 4. 去重检查
        if (contentId != null) {
            Optional<BizCollectionItem> existing = itemRepository
                    .findByFolderIdAndUserIdAndBizTypeAndContentIdAndStatus(
                            folder.getId(), userId, bizType, contentId, 1);
            if (existing.isPresent()) {
                return; // 幂等忽略
            }
        }

        // 5. 检查 500 条上限
        long activeCount = itemRepository.countByFolderIdAndStatus(folder.getId(), 1);
        if (activeCount >= MAX_ITEMS_PER_FOLDER) {
            throw new BadRequestException("收藏夹已满，最多收藏" + MAX_ITEMS_PER_FOLDER + "条内容");
        }

        // 6. 创建收藏记录
        BizCollectionItem item = new BizCollectionItem();
        item.setFolderId(folder.getId());
        item.setUserId(userId);
        item.setBizType(bizType);
        item.setContentId(contentId);
        item.setContentText(contentText);
        item.setStatus(1);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        itemRepository.save(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long itemId, Long userId) {
        BizCollectionItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BadRequestException("收藏记录不存在"));
        if (!item.getUserId().equals(userId)) {
            throw new BadRequestException("无权操作此收藏记录");
        }
        itemRepository.softDeleteById(itemId);
    }

    @Override
    public Map<String, List<BizCollectionItem>> getFolderItemsGrouped(Long folderId) {
        List<BizCollectionItem> items = itemRepository
                .findByFolderIdAndStatusOrderByCreateTimeDesc(folderId, 1);
        return items.stream()
                .collect(Collectors.groupingBy(BizCollectionItem::getBizType));
    }

    @Override
    public BizCollectionItem checkCollected(Long userId, String bizType, Long contentId) {
        return itemRepository
                .findFirstByUserIdAndBizTypeAndContentIdAndStatusOrderByCreateTimeDesc(
                        userId, bizType, contentId, 1)
                .orElse(null);
    }

    @Override
    public long countActiveItems(Long folderId) {
        return itemRepository.countByFolderIdAndStatus(folderId, 1);
    }

    /**
     * 根据业务类型分派校验 contentId 是否存在
     */
    private void validateContentExists(String bizType, Long contentId) {
        CollectionBizTypeEnum type = CollectionBizTypeEnum.fromCode(bizType);
        if (type == null) {
            // 未知类型，不做校验（兼容将来扩展）
            return;
        }
        boolean exists = false;
        switch (type) {
            case CHARACTER:
                exists = charCharacterService.findById(contentId.intValue()) != null;
                break;
            case VOCABULARY:
                exists = vocabWordService.findById(contentId.intValue()) != null;
                break;
            case RADICAL:
                exists = charRadicalService.findById(contentId) != null;
                break;
            case GRAMMAR:
                exists = grammarPointService.findById(contentId) != null;
                break;
            case GRAMMAR_COMPARISON:
                exists = grammarComparisonGroupService.findById(contentId) != null;
                break;
            case VOCAB_COMPARISON:
                exists = vocabComparisonGroupService.findById(contentId) != null;
                break;
            default:
                return; // 未知类型不校验
        }
        if (!exists) {
            throw new BadRequestException("收藏的" + type.getDescription() + "不存在");
        }
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/CollectionService.java grid-app/src/main/java/com/naon/grid/modules/app/service/impl/CollectionServiceImpl.java
git commit -m "feat: add CollectionService with folder/item CRUD + 500 limit + content validation

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 6: 创建 Request DTO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/CreateFolderRequest.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/UpdateFolderNameRequest.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/UpdateFolderCoverRequest.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AddItemRequest.java`

**Interfaces:**
- Consumes: `CollectionBizTypeEnum` (Task 2)
- Produces: Request DTO 类，供 Task 8 Controller 使用

- [ ] **Step 1: 编写 CreateFolderRequest**

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class CreateFolderRequest {

    @NotBlank(message = "收藏夹名称不能为空")
    @Size(max = 32, message = "收藏夹名称最多32个字符")
    @ApiModelProperty(value = "收藏夹名称", required = true)
    private String name;

    @ApiModelProperty(value = "封面图资源ID")
    private Long coverImageId;
}
```

- [ ] **Step 2: 编写 UpdateFolderNameRequest**

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UpdateFolderNameRequest {

    @NotBlank(message = "收藏夹名称不能为空")
    @Size(max = 32, message = "收藏夹名称最多32个字符")
    @ApiModelProperty(value = "新名称", required = true)
    private String name;
}
```

- [ ] **Step 3: 编写 UpdateFolderCoverRequest**

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateFolderCoverRequest {

    @ApiModelProperty(value = "封面图资源ID")
    private Long coverImageId;
}
```

- [ ] **Step 4: 编写 AddItemRequest**

```java
package com.naon.grid.modules.app.rest.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class AddItemRequest {

    @ApiModelProperty(value = "收藏夹ID（不传则使用默认收藏夹）")
    private Long folderId;

    @NotBlank(message = "业务类型不能为空")
    @ApiModelProperty(value = "业务类型：CHARACTER/VOCABULARY/RADICAL/GRAMMAR/GRAMMAR_COMPARISON/VOCAB_COMPARISON", required = true)
    private String bizType;

    @ApiModelProperty(value = "收藏内容ID（与contentText至少提供一个）")
    private Long contentId;

    @ApiModelProperty(value = "收藏内容文本（用于好词好句等无结构化ID的内容）")
    private String contentText;
}
```

- [ ] **Step 5: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/request/CreateFolderRequest.java grid-app/src/main/java/com/naon/grid/modules/app/rest/request/UpdateFolderNameRequest.java grid-app/src/main/java/com/naon/grid/modules/app/rest/request/UpdateFolderCoverRequest.java grid-app/src/main/java/com/naon/grid/modules/app/rest/request/AddItemRequest.java
git commit -m "feat: add collection request DTOs

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 7: 创建 VO 类

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/CollectionFolderVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/CollectionItemVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/CollectionGroupVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/CollectionFolderDetailVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/CollectionCheckVO.java`

**Interfaces:**
- Consumes: (纯数据结构，无依赖)
- Produces: VO 类，供 Task 8 Controller、Task 9 Wrapper 使用

- [ ] **Step 1: 编写 CollectionFolderVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class CollectionFolderVO implements Serializable {

    @ApiModelProperty("收藏夹ID")
    private Long id;

    @ApiModelProperty("收藏夹名称")
    private String name;

    @ApiModelProperty("封面图资源ID")
    private Long coverImageId;

    @ApiModelProperty("是否默认收藏夹")
    private Boolean isDefault;

    @ApiModelProperty("是否置顶")
    private Boolean isPinned;

    @ApiModelProperty("有效收藏数量")
    private Long itemCount;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 编写 CollectionItemVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
public class CollectionItemVO implements Serializable {

    @ApiModelProperty("收藏记录ID")
    private Long id;

    @ApiModelProperty("内容ID")
    private Long contentId;

    @ApiModelProperty("内容文本")
    private String contentText;

    @ApiModelProperty("内容展示名称（动态查询获得）")
    private String contentName;

    @ApiModelProperty("收藏时间")
    private LocalDateTime createTime;
}
```

- [ ] **Step 3: 编写 CollectionGroupVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CollectionGroupVO implements Serializable {

    @ApiModelProperty("业务类型")
    private String bizType;

    @ApiModelProperty("该类型下的收藏内容列表")
    private List<CollectionItemVO> items;
}
```

- [ ] **Step 4: 编写 CollectionFolderDetailVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CollectionFolderDetailVO implements Serializable {

    @ApiModelProperty("收藏夹ID")
    private Long id;

    @ApiModelProperty("收藏夹名称")
    private String name;

    @ApiModelProperty("封面图资源ID")
    private Long coverImageId;

    @ApiModelProperty("是否默认收藏夹")
    private Boolean isDefault;

    @ApiModelProperty("是否置顶")
    private Boolean isPinned;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("按业务类型分组的收藏列表")
    private List<CollectionGroupVO> groups;
}
```

- [ ] **Step 5: 编写 CollectionCheckVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
public class CollectionCheckVO implements Serializable {

    @ApiModelProperty("是否已收藏")
    private Boolean collected;

    @ApiModelProperty("收藏记录ID（已收藏时返回）")
    private Long itemId;

    @ApiModelProperty("所在收藏夹名称（已收藏时返回）")
    private String folderName;
}
```

- [ ] **Step 6: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/CollectionFolderVO.java grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/CollectionItemVO.java grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/CollectionGroupVO.java grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/CollectionFolderDetailVO.java grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/CollectionCheckVO.java
git commit -m "feat: add collection VO classes

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 8: 创建 Wrapper 类

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/CollectionWrapper.java`

**Interfaces:**
- Consumes: `BizCollectionFolder`, `BizCollectionItem` (Task 3), VO 类 (Task 7), Backend Services (来自 grid-system)
- Produces: 静态转换方法，供 Task 9 Controller 使用

- [ ] **Step 1: 编写 CollectionWrapper**

```java
package com.naon.grid.modules.app.rest.wrapper;

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
import com.naon.grid.modules.app.domain.BizCollectionFolder;
import com.naon.grid.modules.app.domain.BizCollectionItem;
import com.naon.grid.modules.app.rest.vo.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 收藏夹 VO 包装器
 */
public class CollectionWrapper {

    public static CollectionFolderVO toFolderVO(BizCollectionFolder folder, long itemCount) {
        CollectionFolderVO vo = new CollectionFolderVO();
        vo.setId(folder.getId());
        vo.setName(folder.getName());
        vo.setCoverImageId(folder.getCoverImageId());
        vo.setIsDefault(folder.getIsDefault() == 1);
        vo.setIsPinned(folder.getIsPinned() == 1);
        vo.setItemCount(itemCount);
        vo.setCreateTime(folder.getCreateTime());
        return vo;
    }

    public static List<CollectionFolderVO> toFolderVOList(List<BizCollectionFolder> folders,
            Map<Long, Long> itemCountMap) {
        if (folders == null) return Collections.emptyList();
        return folders.stream()
                .map(f -> toFolderVO(f, itemCountMap.getOrDefault(f.getId(), 0L)))
                .collect(Collectors.toList());
    }

    public static CollectionFolderDetailVO toDetailVO(
            BizCollectionFolder folder,
            Map<String, List<BizCollectionItem>> groupedItems,
            CharCharacterService charCharacterService,
            VocabWordService vocabWordService,
            CharRadicalService charRadicalService,
            GrammarPointService grammarPointService,
            GrammarComparisonGroupService grammarComparisonGroupService,
            VocabComparisonGroupService vocabComparisonGroupService) {

        CollectionFolderDetailVO vo = new CollectionFolderDetailVO();
        vo.setId(folder.getId());
        vo.setName(folder.getName());
        vo.setCoverImageId(folder.getCoverImageId());
        vo.setIsDefault(folder.getIsDefault() == 1);
        vo.setIsPinned(folder.getIsPinned() == 1);
        vo.setCreateTime(folder.getCreateTime());

        List<CollectionGroupVO> groups = new ArrayList<>();
        if (groupedItems != null) {
            for (Map.Entry<String, List<BizCollectionItem>> entry : groupedItems.entrySet()) {
                CollectionGroupVO group = new CollectionGroupVO();
                group.setBizType(entry.getKey());
                group.setItems(toItemVOList(entry.getValue(),
                        charCharacterService, vocabWordService, charRadicalService,
                        grammarPointService, grammarComparisonGroupService, vocabComparisonGroupService));
                groups.add(group);
            }
        }
        vo.setGroups(groups);
        return vo;
    }

    public static List<CollectionItemVO> toItemVOList(
            List<BizCollectionItem> items,
            CharCharacterService charCharacterService,
            VocabWordService vocabWordService,
            CharRadicalService charRadicalService,
            GrammarPointService grammarPointService,
            GrammarComparisonGroupService grammarComparisonGroupService,
            VocabComparisonGroupService vocabComparisonGroupService) {

        if (items == null) return Collections.emptyList();
        return items.stream()
                .map(item -> toItemVO(item,
                        charCharacterService, vocabWordService, charRadicalService,
                        grammarPointService, grammarComparisonGroupService, vocabComparisonGroupService))
                .collect(Collectors.toList());
    }

    public static CollectionItemVO toItemVO(
            BizCollectionItem item,
            CharCharacterService charCharacterService,
            VocabWordService vocabWordService,
            CharRadicalService charRadicalService,
            GrammarPointService grammarPointService,
            GrammarComparisonGroupService grammarComparisonGroupService,
            VocabComparisonGroupService vocabComparisonGroupService) {

        CollectionItemVO vo = new CollectionItemVO();
        vo.setId(item.getId());
        vo.setContentId(item.getContentId());
        vo.setContentText(item.getContentText());
        vo.setContentName(resolveContentName(item,
                charCharacterService, vocabWordService, charRadicalService,
                grammarPointService, grammarComparisonGroupService, vocabComparisonGroupService));
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    public static CollectionCheckVO toCheckVO(BizCollectionItem item, String folderName) {
        CollectionCheckVO vo = new CollectionCheckVO();
        if (item != null) {
            vo.setCollected(true);
            vo.setItemId(item.getId());
            vo.setFolderName(folderName);
        } else {
            vo.setCollected(false);
            vo.setItemId(null);
            vo.setFolderName(null);
        }
        return vo;
    }

    /**
     * 根据 bizType 动态查询 contentName
     */
    private static String resolveContentName(
            BizCollectionItem item,
            CharCharacterService charCharacterService,
            VocabWordService vocabWordService,
            CharRadicalService charRadicalService,
            GrammarPointService grammarPointService,
            GrammarComparisonGroupService grammarComparisonGroupService,
            VocabComparisonGroupService vocabComparisonGroupService) {

        // 纯文本类内容直接返回 contentText
        if (item.getContentId() == null && item.getContentText() != null) {
            return item.getContentText();
        }
        if (item.getContentId() == null) {
            return null;
        }

        try {
            switch (item.getBizType()) {
                case "CHARACTER": {
                    CharCharacterDto dto = charCharacterService.findById(
                            item.getContentId().intValue());
                    return dto != null ? dto.getCharacter() : null;
                }
                case "VOCABULARY": {
                    VocabWordDto dto = vocabWordService.findById(
                            item.getContentId().intValue());
                    return dto != null ? dto.getWord() : null;
                }
                case "RADICAL": {
                    CharRadicalDto dto = charRadicalService.findById(
                            item.getContentId());
                    return dto != null ? dto.getRadical() : null;
                }
                case "GRAMMAR": {
                    GrammarPointDto dto = grammarPointService.findById(
                            item.getContentId());
                    return dto != null ? dto.getName() : null;
                }
                case "GRAMMAR_COMPARISON": {
                    GrammarComparisonGroupDto dto = grammarComparisonGroupService.findById(
                            item.getContentId());
                    return dto != null ? dto.getGroupKey() : null;
                }
                case "VOCAB_COMPARISON": {
                    VocabComparisonGroupDto dto = vocabComparisonGroupService.findById(
                            item.getContentId());
                    return dto != null ? dto.getGroupKey() : null;
                }
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/CollectionWrapper.java
git commit -m "feat: add CollectionWrapper with dynamic content name resolution

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 9: 创建 Controller

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCollectionController.java`

**Interfaces:**
- Consumes: `CollectionService` (Task 5), `CollectionWrapper` (Task 8), Request DTOs (Task 6), VO 类 (Task 7), Backend Services (来自 grid-system), `AppSecurityUtils` (已有)
- Produces: REST API 端点，暴露给前端

- [ ] **Step 1: 编写 AppCollectionController**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.Log;
import com.naon.grid.backend.service.character.CharCharacterService;
import com.naon.grid.backend.service.charradical.CharRadicalService;
import com.naon.grid.backend.service.grammar.GrammarPointService;
import com.naon.grid.backend.service.grammarcomparison.GrammarComparisonGroupService;
import com.naon.grid.backend.service.vocabcomparison.VocabComparisonGroupService;
import com.naon.grid.backend.service.vocabulary.VocabWordService;
import com.naon.grid.modules.app.domain.BizCollectionFolder;
import com.naon.grid.modules.app.domain.BizCollectionItem;
import com.naon.grid.modules.app.rest.request.*;
import com.naon.grid.modules.app.rest.vo.*;
import com.naon.grid.modules.app.rest.wrapper.CollectionWrapper;
import com.naon.grid.modules.app.service.CollectionService;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/collection")
@Api(tags = "用户：收藏夹接口")
public class AppCollectionController {

    private final CollectionService collectionService;
    private final CharCharacterService charCharacterService;
    private final VocabWordService vocabWordService;
    private final CharRadicalService charRadicalService;
    private final GrammarPointService grammarPointService;
    private final GrammarComparisonGroupService grammarComparisonGroupService;
    private final VocabComparisonGroupService vocabComparisonGroupService;

    @Log("新建收藏夹")
    @ApiOperation("新建收藏夹")
    @PostMapping("/folder")
    public ResponseEntity<CollectionFolderVO> createFolder(
            @Validated @RequestBody CreateFolderRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        BizCollectionFolder folder = collectionService.createFolder(
                userId, request.getName(), request.getCoverImageId());
        return new ResponseEntity<>(
                CollectionWrapper.toFolderVO(folder, 0), HttpStatus.OK);
    }

    @ApiOperation("查询我的收藏夹列表")
    @GetMapping("/folder/list")
    public ResponseEntity<List<CollectionFolderVO>> listFolders() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        List<BizCollectionFolder> folders = collectionService.listFolders(userId);
        Map<Long, Long> itemCountMap = new java.util.HashMap<>();
        for (BizCollectionFolder f : folders) {
            itemCountMap.put(f.getId(), collectionService.countActiveItems(f.getId()));
        }
        return ResponseEntity.ok(CollectionWrapper.toFolderVOList(folders, itemCountMap));
    }

    @ApiOperation("查询收藏夹详情")
    @GetMapping("/folder/{folderId}")
    public ResponseEntity<CollectionFolderDetailVO> getFolderDetail(
            @PathVariable Long folderId) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        BizCollectionFolder folder = collectionService.getFolder(folderId, userId);
        Map<String, List<BizCollectionItem>> groupedItems =
                collectionService.getFolderItemsGrouped(folderId);
        return ResponseEntity.ok(CollectionWrapper.toDetailVO(
                folder, groupedItems,
                charCharacterService, vocabWordService, charRadicalService,
                grammarPointService, grammarComparisonGroupService, vocabComparisonGroupService));
    }

    @Log("修改收藏夹名称")
    @ApiOperation("修改收藏夹名称")
    @PutMapping("/folder/{folderId}/name")
    public ResponseEntity<Void> updateFolderName(
            @PathVariable Long folderId,
            @Validated @RequestBody UpdateFolderNameRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        collectionService.updateFolderName(folderId, userId, request.getName());
        return ResponseEntity.ok().build();
    }

    @Log("修改收藏夹封面图")
    @ApiOperation("修改收藏夹封面图")
    @PutMapping("/folder/{folderId}/cover")
    public ResponseEntity<Void> updateFolderCover(
            @PathVariable Long folderId,
            @Validated @RequestBody UpdateFolderCoverRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        collectionService.updateFolderCover(folderId, userId, request.getCoverImageId());
        return ResponseEntity.ok().build();
    }

    @Log("删除收藏夹")
    @ApiOperation("删除收藏夹")
    @DeleteMapping("/folder/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long folderId) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        collectionService.deleteFolder(folderId, userId);
        return ResponseEntity.ok().build();
    }

    @Log("置顶收藏夹")
    @ApiOperation("置顶收藏夹")
    @PutMapping("/folder/{folderId}/pin")
    public ResponseEntity<Void> pinFolder(@PathVariable Long folderId) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        collectionService.pinFolder(folderId, userId);
        return ResponseEntity.ok().build();
    }

    @Log("取消置顶收藏夹")
    @ApiOperation("取消置顶收藏夹")
    @PutMapping("/folder/{folderId}/unpin")
    public ResponseEntity<Void> unpinFolder(@PathVariable Long folderId) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        collectionService.unpinFolder(folderId, userId);
        return ResponseEntity.ok().build();
    }

    @Log("添加收藏")
    @ApiOperation("添加内容到收藏夹")
    @PostMapping("/item")
    public ResponseEntity<Void> addItem(@Validated @RequestBody AddItemRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        collectionService.addItem(userId, request.getFolderId(),
                request.getBizType(), request.getContentId(), request.getContentText());
        return ResponseEntity.ok().build();
    }

    @Log("取消收藏")
    @ApiOperation("取消收藏")
    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long itemId) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        collectionService.removeItem(itemId, userId);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("检查内容是否已收藏")
    @GetMapping("/item/check")
    public ResponseEntity<CollectionCheckVO> checkCollected(
            @RequestParam String bizType,
            @RequestParam Long contentId) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        BizCollectionItem item = collectionService.checkCollected(userId, bizType, contentId);
        String folderName = null;
        if (item != null) {
            try {
                BizCollectionFolder folder = collectionService.getFolder(
                        item.getFolderId(), userId);
                folderName = folder.getName();
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(CollectionWrapper.toCheckVO(item, folderName));
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppCollectionController.java
git commit -m "feat: add AppCollectionController with 11 REST endpoints

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### Task 10: 注册时创建默认收藏夹

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java`

**Interfaces:**
- Consumes: `CollectionService` (Task 5)

- [ ] **Step 1: 在 AppAuthServiceImpl 中注入 CollectionService**

在类字段声明区域，添加：

```java
private final CollectionService collectionService;
```

这需要放在 `@RequiredArgsConstructor` 的 final 字段列表中。在现有字段列表中（约第 62-74 行），追加：

```java
private final CollectionService collectionService;
```

- [ ] **Step 2: 在 register() 方法中添加默认收藏夹创建**

在 `register()` 方法中，`userRepository.save(user)` 之后（约第 134 行），添加：

```java
// 创建默认收藏夹
collectionService.createDefaultFolder(user.getId());
```

- [ ] **Step 3: 在 createSocialUser() 方法中添加默认收藏夹创建**

在 `createSocialUser()` 方法中，`userRepository.save(user)` 之后（约第 372 行），添加：

```java
// 创建默认收藏夹
collectionService.createDefaultFolder(user.getId());
```

- [ ] **Step 4: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java
git commit -m "feat: auto-create default collection folder on user registration

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

### 最终验证

- [ ] **编译验证**: `mvn clean compile -pl grid-app -am -DskipTests`
- [ ] **完整构建**: `mvn clean package -DskipTests`
