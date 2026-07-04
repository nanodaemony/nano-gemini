# 订阅与权益体系重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构订阅与权益体系：三表分离（entitlement + user_entitlement_record + user_entitlement），商品 JSON 映射权益，移除 legacy VIP/SVIP 模型

**Architecture:** 权益元数据定义「权益是什么」，商品通过 JSON 数组关联多项权益，购买/赠送时写流水+更新汇总，鉴权直接查汇总表

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MySQL, Fastjson2, Lombok

## Global Constraints

- Java 1.8，Spring Boot 2.7.18
- 不需要数据迁移（未上线）
- SQL 文件修改在 `/Users/nano/Desktop/nano-gemini/sql/` 下
- 实体继承 `BaseEntity` 的审计字段模式
- Controller 转换逻辑抽取到 Wrapper 类
- 状态字段用 Integer（1=可用 0=删除/禁用）

---

## File Structure

**新建：**
- `grid-common/.../enums/BizModuleEnum.java` — 业务模块枚举
- `grid-billing/.../domain/Entitlement.java` — 权益元数据实体
- `grid-billing/.../domain/UserEntitlementRecord.java` — 用户权益流水实体
- `grid-billing/.../domain/UserEntitlement.java` — 用户权益汇总实体
- `grid-billing/.../repository/EntitlementRepository.java`
- `grid-billing/.../repository/UserEntitlementRecordRepository.java`
- `grid-billing/.../repository/UserEntitlementRepository.java`
- `grid-billing/.../service/EntitlementService.java` — 替代 EntitlementEngine
- `grid-billing/.../service/impl/EntitlementServiceImpl.java`
- `grid-billing/.../service/dto/UserEntitlementVO.java` — 鉴权查询结果
- `grid-billing/.../test/.../EntitlementServiceImplTest.java`

**修改：**
- `sql/billing.sql` — 新表 DDL + 种子数据
- `sql/normal_user.sql` — grid_user 加 country 列
- `grid-billing/.../domain/GridProduct.java` — 加 entitlementIds, institutionConfig, coverImage
- `grid-billing/.../domain/PaymentSubscription.java` — 精简字段
- `grid-billing/.../repository/GridProductRepository.java` — 按需调整
- `grid-billing/.../service/impl/PaymentServiceImpl.java` — 调 grantEntitlements
- `grid-billing/.../service/dto/EntitlementResult.java` — 适配新模型
- `grid-app/.../aspect/ProductAccessAspect.java` — 切换为 EntitlementService
- `grid-app/.../rest/AppSubscriptionController.java` — 重写
- `grid-app/.../rest/AppOrderController.java` — 微调
- `grid-app/.../service/impl/AppAuthServiceImpl.java` — 试用发放切换
- `grid-app/.../domain/GridUser.java` — 加 country 字段
- `grid-app/.../security/AppTokenProvider.java` — roles 参数简化
- `grid-bootstrap/.../rest/ProductController.java` — 适配

**删除：**
- `grid-billing/.../domain/EntitlementSource.java`
- `grid-billing/.../domain/ProductModule.java`
- `grid-billing/.../repository/EntitlementSourceRepository.java`
- `grid-billing/.../repository/ProductModuleRepository.java`
- `grid-billing/.../service/EntitlementEngine.java`
- `grid-billing/.../service/impl/EntitlementEngineImpl.java`
- `grid-billing/.../test/.../EntitlementEngineImplTest.java`
- `grid-app/.../domain/GridUserRole.java`
- `grid-app/.../repository/GridUserRoleRepository.java`
- `grid-app/.../service/SubscriptionService.java`
- `grid-app/.../service/impl/SubscriptionServiceImpl.java`
- `grid-app/.../aspect/SubscriptionAspect.java`
- `grid-common/.../enums/UserLevel.java`
- `grid-common/.../annotation/RequireSubscription.java`
- `grid-app/.../service/dto/AppSubscriptionVO.java`（重建）
- `grid-app/.../service/dto/ActivateSubscriptionDTO.java`
- `grid-app/.../service/dto/CreateOrderDTO.java`（重建）

---

### Task 1: 更新 SQL DDL — billing.sql

**Files:**
- Modify: `sql/billing.sql`（全量重写）
- Modify: `sql/normal_user.sql`（追加 ALTER TABLE）

**Interfaces:**
- Produces: `entitlement`, `user_entitlement_record`, `user_entitlement` 三表 DDL；grid_product 扩展字段；payment_subscription 精简；grid_user.country 列

- [ ] **Step 1: 重写 billing.sql**

用以下内容完全替换 `sql/billing.sql`：

```sql
-- ----------------------------
-- 权益元数据表
-- ----------------------------
CREATE TABLE `entitlement` (
    `id`          INT AUTO_INCREMENT COMMENT '主键ID',
    `code`        VARCHAR(50) NOT NULL COMMENT '权益唯一标识 VOCAB_ACCESS等',
    `name`        VARCHAR(200) NOT NULL COMMENT '权益名称',
    `module_code` VARCHAR(50) COMMENT '关联业务模块, BizModuleEnum',
    `sort_order`  INT DEFAULT 0 COMMENT '排序',
    `status`      INT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `create_by`   VARCHAR(50) COMMENT '创建人',
    `update_by`   VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权益元数据表';

-- ----------------------------
-- 用户权益流水表
-- ----------------------------
CREATE TABLE `user_entitlement_record` (
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `entitlement_id`  INT NOT NULL COMMENT '权益ID',
    `source_type`     VARCHAR(30) NOT NULL COMMENT 'TRIAL / PURCHASE / ADMIN_GRANT',
    `source_id`       VARCHAR(100) COMMENT '来源业务ID(订单号等)',
    `duration_days`   INT NOT NULL COMMENT '本次有效天数',
    `expire_at`       DATETIME COMMENT '原始到期时间',
    `region`          VARCHAR(10) COMMENT '授予时区域',
    `remark`          VARCHAR(500) COMMENT '备注',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_entitlement` (`user_id`, `entitlement_id`),
    KEY `idx_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权益流水表';

-- ----------------------------
-- 用户权益汇总表
-- ----------------------------
CREATE TABLE `user_entitlement` (
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `entitlement_id`  INT NOT NULL COMMENT '权益ID',
    `expire_at`       DATETIME COMMENT '堆叠后到期时间',
    `status`          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / EXPIRED',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次获得时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_entitlement` (`user_id`, `entitlement_id`),
    KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权益汇总表';

-- ----------------------------
-- 商品表（重建）
-- ----------------------------
CREATE TABLE `grid_product` (
    `id`                 INT AUTO_INCREMENT COMMENT '主键ID',
    `code`               VARCHAR(50) NOT NULL COMMENT '商品代码',
    `name`               VARCHAR(200) NOT NULL COMMENT '商品名称',
    `product_type`       VARCHAR(30) NOT NULL COMMENT 'PLUS/SINGLE_MODULE/INSTITUTION/ENTERPRISE',
    `entitlement_ids`    VARCHAR(500) COMMENT 'JSON数组, 购买后获得的权益ID列表',
    `institution_config` VARCHAR(500) COMMENT 'JSON, 机构商品配置 {"maxMembers":30,"maxAdmins":1}',
    `cover_image`        VARCHAR(500) COMMENT '封面图URL',
    `description`        TEXT COMMENT '商品描述',
    `sort_order`         INT DEFAULT 0 COMMENT '排序',
    `status`             INT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `create_by`          VARCHAR(50) COMMENT '创建人',
    `update_by`          VARCHAR(50) COMMENT '更新人',
    `create_time`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ----------------------------
-- 区域定价表
-- ----------------------------
CREATE TABLE `region_pricing` (
    `id`              INT AUTO_INCREMENT PRIMARY KEY,
    `product_id`      INT NOT NULL,
    `region`          VARCHAR(10) NOT NULL COMMENT 'A/B/C/D/E',
    `billing_cycle`   VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY/YEARLY',
    `price`           DECIMAL(12,2) NOT NULL COMMENT '金额',
    `currency`        VARCHAR(10) NOT NULL COMMENT 'USD/EUR/CNY',
    `status`          INT NOT NULL DEFAULT 1,
    `create_by`       VARCHAR(50), `update_by` VARCHAR(50),
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_product_region_cycle` (`product_id`, `region`, `billing_cycle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域定价表';

-- ----------------------------
-- 订阅记录表
-- ----------------------------
CREATE TABLE `payment_subscription` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT NOT NULL,
    `product_code`    VARCHAR(50) NOT NULL COMMENT '商品代码',
    `channel`         VARCHAR(30) NOT NULL COMMENT 'STRIPE/PHOTONPAY',
    `channel_sub_id`  VARCHAR(200) COMMENT '渠道侧订阅ID',
    `status`          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CANCELLED/EXPIRED',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `cancel_at`       DATETIME COMMENT '取消时间',
    KEY `idx_user` (`user_id`),
    KEY `idx_channel_sub` (`channel`, `channel_sub_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅记录表';

-- ----------------------------
-- 订单表
-- ----------------------------
CREATE TABLE `grid_order` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no`        VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    `user_id`         BIGINT NOT NULL,
    `org_id`          INT COMMENT '机构下单时为机构ID',
    `product_code`    VARCHAR(50) NOT NULL,
    `region`          VARCHAR(10) NOT NULL,
    `billing_cycle`   VARCHAR(20) NOT NULL,
    `amount`          DECIMAL(12,2) NOT NULL,
    `currency`        VARCHAR(10) NOT NULL,
    `status`          VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/REFUNDING/REFUNDED/EXPIRED',
    `payment_method`  VARCHAR(30),
    `paid_at`         DATETIME,
    `expire_at`       DATETIME COMMENT '订单过期时间',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `channel`         VARCHAR(30),
    `channel_order_id` VARCHAR(200),
    `channel_sub_id`  VARCHAR(200),
    `invoice_no`      VARCHAR(64),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ----------------------------
-- 支付流水表
-- ----------------------------
CREATE TABLE `payment_record` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id`        BIGINT NOT NULL,
    `payment_method`  VARCHAR(30) NOT NULL,
    `transaction_id`  VARCHAR(200) COMMENT '支付平台交易号',
    `amount`          DECIMAL(12,2) NOT NULL,
    `currency`        VARCHAR(10) NOT NULL,
    `status`          VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED/REFUND',
    `raw_callback`    TEXT COMMENT '原始回调JSON',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_order_id` (`order_id`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ----------------------------
-- 种子数据
-- ----------------------------
-- 权益元数据
INSERT INTO `entitlement` (`code`, `name`, `module_code`, `sort_order`, `status`) VALUES
('VOCAB_ACCESS', '词汇模块使用权', 'VOCAB', 1, 1),
('GRAMMAR_ACCESS', '语法模块使用权', 'GRAMMAR', 2, 1),
('CHARACTER_ACCESS', '汉字模块使用权', 'CHARACTER', 3, 1),
('CONFUSING_WORDS_ACCESS', '易混淆词辨析使用权', 'CONFUSING_WORDS', 4, 1),
('CULTURE_ACCESS', '文化模块使用权', 'CULTURE', 5, 1),
('TOPIC_ACCESS', '话题模块使用权', 'TOPIC', 6, 1);

-- 商品
INSERT INTO `grid_product` (`code`, `name`, `product_type`, `entitlement_ids`, `institution_config`, `sort_order`, `status`) VALUES
('PLUS', '全平台Plus会员', 'PLUS',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 NULL, 1, 1),
('VOCAB', '词汇模块', 'SINGLE_MODULE', '["VOCAB_ACCESS"]', NULL, 2, 1),
('GRAMMAR', '语法模块', 'SINGLE_MODULE', '["GRAMMAR_ACCESS"]', NULL, 3, 1),
('CHARACTER', '汉字模块', 'SINGLE_MODULE', '["CHARACTER_ACCESS"]', NULL, 4, 1),
('CONFUSING_WORDS', '易混淆词辨析模块', 'SINGLE_MODULE', '["CONFUSING_WORDS_ACCESS"]', NULL, 5, 1),
('CULTURE', '文化模块', 'SINGLE_MODULE', '["CULTURE_ACCESS"]', NULL, 6, 1),
('TOPIC', '话题模块', 'SINGLE_MODULE', '["TOPIC_ACCESS"]', NULL, 7, 1),
('INST_STARTER', 'Institution Starter', 'INSTITUTION',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 '{"maxMembers":30,"maxAdmins":1}', 10, 1),
('INST_BASIC', 'Institution Basic', 'INSTITUTION',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 '{"maxMembers":100,"maxAdmins":2}', 11, 1),
('INST_PRO', 'Institution Pro', 'INSTITUTION',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 '{"maxMembers":500,"maxAdmins":5}', 12, 1);
```

- [ ] **Step 2: 更新 normal_user.sql**

在文件末尾追加（在机构表建表语句之后）：

```sql
-- grid_user 扩展
ALTER TABLE `grid_user`
    ADD COLUMN `country` VARCHAR(50) COMMENT '注册国家' AFTER `region`;
```

- [ ] **Step 3: 提交**

```bash
git add sql/billing.sql sql/normal_user.sql
git commit -m "feat: redesign billing DDL — entitlement 3-table split, product JSON entitlements, subscription slimdown"
```

---

### Task 2: 创建 BizModuleEnum（grid-common）

**Files:**
- Create: `grid-common/src/main/java/com/naon/grid/enums/BizModuleEnum.java`

**Interfaces:**
- Produces: `BizModuleEnum` — VOCAB, GRAMMAR, CHARACTER, CONFUSING_WORDS, CULTURE, TOPIC，带 code 和 name

- [ ] **Step 1: 创建枚举**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum BizModuleEnum {
    VOCAB("VOCAB", "词汇"),
    GRAMMAR("GRAMMAR", "语法"),
    CHARACTER("CHARACTER", "汉字"),
    CONFUSING_WORDS("CONFUSING_WORDS", "易混淆词辨析"),
    CULTURE("CULTURE", "文化"),
    TOPIC("TOPIC", "话题");

    private final String code;
    private final String name;

    BizModuleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static BizModuleEnum fromCode(String code) {
        for (BizModuleEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-common/src/main/java/com/naon/grid/enums/BizModuleEnum.java
git commit -m "feat: add BizModuleEnum for business module codes"
```

---

### Task 3: 创建 Entitlement 实体

**Files:**
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/Entitlement.java`

**Interfaces:**
- Produces: JPA Entity mapped to `entitlement` table，字段 id, code, name, moduleCode, sortOrder, status + BaseEntity 审计字段

- [ ] **Step 1: 写实体**

```java
package com.naon.grid.modules.billing.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "entitlement")
public class Entitlement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, unique = true, nullable = false)
    private String code;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 50)
    private String moduleCode;

    private Integer sortOrder = 0;

    private Integer status = 1;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-billing/src/main/java/com/naon/grid/modules/billing/domain/Entitlement.java
git commit -m "feat: add Entitlement entity"
```

---

### Task 4: 创建 UserEntitlementRecord 实体

**Files:**
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/UserEntitlementRecord.java`

**Interfaces:**
- Produces: JPA Entity mapped to `user_entitlement_record` table

- [ ] **Step 1: 写实体**

```java
package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "user_entitlement_record", indexes = {
    @Index(name = "idx_user_entitlement", columnList = "userId, entitlementId"),
    @Index(name = "idx_source", columnList = "sourceType, sourceId")
})
public class UserEntitlementRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer entitlementId;

    @Column(length = 30, nullable = false)
    private String sourceType;

    @Column(length = 100)
    private String sourceId;

    @Column(nullable = false)
    private Integer durationDays;

    private LocalDateTime expireAt;

    @Column(length = 10)
    private String region;

    @Column(length = 500)
    private String remark;

    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-billing/src/main/java/com/naon/grid/modules/billing/domain/UserEntitlementRecord.java
git commit -m "feat: add UserEntitlementRecord entity (ledger)"
```

---

### Task 5: 创建 UserEntitlement 实体

**Files:**
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/UserEntitlement.java`

**Interfaces:**
- Produces: JPA Entity mapped to `user_entitlement` table

- [ ] **Step 1: 写实体**

```java
package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "user_entitlement", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "entitlementId"})
}, indexes = {
    @Index(name = "idx_expire", columnList = "expireAt")
})
public class UserEntitlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer entitlementId;

    private LocalDateTime expireAt;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-billing/src/main/java/com/naon/grid/modules/billing/domain/UserEntitlement.java
git commit -m "feat: add UserEntitlement entity (summary)"
```

---

### Task 6: 修改 GridProduct 实体 + PaymentSubscription 精简

**Files:**
- Modify: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/GridProduct.java`
- Modify: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/PaymentSubscription.java`

**Interfaces:**
- Modifies: GridProduct 加 entitlementIds, institutionConfig, coverImage；PaymentSubscription 移除 orderId, billingCycle, region, nextBillingAt, lastChargedAt, updateTime

- [ ] **Step 1: 更新 GridProduct**

```java
package com.naon.grid.modules.billing.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "grid_product")
public class GridProduct extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, length = 50, nullable = false)
    private String code;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 30, nullable = false)
    private String productType;

    @Column(length = 500)
    private String entitlementIds;

    @Column(length = 500)
    private String institutionConfig;

    @Column(length = 500)
    private String coverImage;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer sortOrder;

    private Integer status = 1;
}
```

- [ ] **Step 2: 精简 PaymentSubscription**

```java
package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "payment_subscription", indexes = {
    @Index(name = "idx_user", columnList = "userId"),
    @Index(name = "idx_channel_sub", columnList = "channel, channelSubId")
})
public class PaymentSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 50, nullable = false)
    private String productCode;

    @Column(length = 30, nullable = false)
    private String channel = "PHOTONPAY";

    @Column(length = 200)
    private String channelSubId;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    private LocalDateTime createTime;

    private LocalDateTime cancelAt;
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-billing/src/main/java/com/naon/grid/modules/billing/domain/GridProduct.java \
        grid-billing/src/main/java/com/naon/grid/modules/billing/domain/PaymentSubscription.java
git commit -m "feat: extend GridProduct (entitlementIds, institutionConfig, coverImage), slim PaymentSubscription"
```

---

### Task 7: 更新 GridUser 实体加 country 字段

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/domain/GridUser.java`

**Interfaces:**
- Modifies: 在 region 字段后加 country 字段

- [ ] **Step 1: 在 region 字段后加 country**

在 `private String region;` 这行之后添加：

```java
    @Column(length = 50)
    private String country;
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/domain/GridUser.java
git commit -m "feat: add country field to GridUser"
```

---

### Task 8: 创建 Repositories

**Files:**
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/EntitlementRepository.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/UserEntitlementRecordRepository.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/UserEntitlementRepository.java`

**Interfaces:**
- Consumes: Entitlement, UserEntitlementRecord, UserEntitlement 实体
- Produces: Spring Data JPA Repository 接口

- [ ] **Step 1: 创建 EntitlementRepository**

```java
package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.Entitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EntitlementRepository extends JpaRepository<Entitlement, Integer> {
    Optional<Entitlement> findByCode(String code);
    Optional<Entitlement> findByModuleCode(String moduleCode);
    List<Entitlement> findByStatusOrderBySortOrder(Integer status);
    List<Entitlement> findByCodeIn(List<String> codes);
}
```

- [ ] **Step 2: 创建 UserEntitlementRecordRepository**

```java
package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.UserEntitlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserEntitlementRecordRepository extends JpaRepository<UserEntitlementRecord, Long> {
    List<UserEntitlementRecord> findByUserIdAndEntitlementIdOrderByCreateTimeAsc(Long userId, Integer entitlementId);
    List<UserEntitlementRecord> findByUserIdOrderByCreateTimeAsc(Long userId);
    boolean existsByUserIdAndSourceType(Long userId, String sourceType);
}
```

- [ ] **Step 3: 创建 UserEntitlementRepository**

```java
package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.UserEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserEntitlementRepository extends JpaRepository<UserEntitlement, Long> {
    Optional<UserEntitlement> findByUserIdAndEntitlementId(Long userId, Integer entitlementId);
    List<UserEntitlement> findByUserIdAndStatus(Long userId, String status);
    List<UserEntitlement> findByUserId(Long userId);
}
```

- [ ] **Step 4: 提交**

```bash
git add grid-billing/src/main/java/com/naon/grid/modules/billing/repository/EntitlementRepository.java \
        grid-billing/src/main/java/com/naon/grid/modules/billing/repository/UserEntitlementRecordRepository.java \
        grid-billing/src/main/java/com/naon/grid/modules/billing/repository/UserEntitlementRepository.java
git commit -m "feat: add entitlement repositories"
```

---

### Task 9: 创建 EntitlementService 接口 + DTO

**Files:**
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/EntitlementService.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/dto/UserEntitlementVO.java`

**Interfaces:**
- Produces: `EntitlementService` — grantEntitlements, hasModuleAccess, hasTrial, getUserEntitlements；`UserEntitlementVO` — 鉴权/查询结果的 DTO

- [ ] **Step 1: 创建 UserEntitlementVO**

```java
package com.naon.grid.modules.billing.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntitlementVO {
    private Integer entitlementId;
    private String entitlementCode;
    private String entitlementName;
    private String moduleCode;
    private LocalDateTime expireAt;
    private boolean active;
}
```

- [ ] **Step 2: 创建 EntitlementService**

```java
package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.service.dto.UserEntitlementVO;

import java.time.LocalDateTime;
import java.util.List;

public interface EntitlementService {

    /**
     * 批量授予权益，在同一事务内写流水+更新汇总
     *
     * @param userId        用户ID
     * @param entitlementIds 权益ID列表
     * @param sourceType    来源类型 TRIAL/PURCHASE/ADMIN_GRANT
     * @param sourceId      来源业务ID（订单号等）
     * @param durationDays  有效天数
     * @param region        授予时的区域
     */
    void grantEntitlements(Long userId, List<Integer> entitlementIds,
                          String sourceType, String sourceId,
                          int durationDays, String region);

    /**
     * 检查用户是否有指定模块的访问权限
     */
    boolean hasModuleAccess(Long userId, String moduleCode);

    /**
     * 检查用户是否已领过试用
     */
    boolean hasReceivedTrial(Long userId);

    /**
     * 获取用户所有权益状态
     */
    List<UserEntitlementVO> getUserEntitlements(Long userId);
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-billing/src/main/java/com/naon/grid/modules/billing/service/EntitlementService.java \
        grid-billing/src/main/java/com/naon/grid/modules/billing/service/dto/UserEntitlementVO.java
git commit -m "feat: add EntitlementService interface and UserEntitlementVO"
```

---

### Task 10: 创建 EntitlementServiceImpl

**Files:**
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/EntitlementServiceImpl.java`

**Interfaces:**
- Consumes: EntitlementService 接口, EntitlementRepository, UserEntitlementRecordRepository, UserEntitlementRepository, EntitlementRepository
- Produces: grantEntitlements 实现（流水+汇总同事务）, hasModuleAccess 实现（查汇总表）

- [ ] **Step 1: 写实现**

```java
package com.naon.grid.modules.billing.service.impl;

import com.naon.grid.modules.billing.domain.Entitlement;
import com.naon.grid.modules.billing.domain.UserEntitlement;
import com.naon.grid.modules.billing.domain.UserEntitlementRecord;
import com.naon.grid.modules.billing.repository.EntitlementRepository;
import com.naon.grid.modules.billing.repository.UserEntitlementRecordRepository;
import com.naon.grid.modules.billing.repository.UserEntitlementRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import com.naon.grid.modules.billing.service.dto.UserEntitlementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementServiceImpl implements EntitlementService {

    private final UserEntitlementRecordRepository recordRepository;
    private final UserEntitlementRepository userEntitlementRepository;
    private final EntitlementRepository entitlementRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantEntitlements(Long userId, List<Integer> entitlementIds,
                                  String sourceType, String sourceId,
                                  int durationDays, String region) {
        if (entitlementIds == null || entitlementIds.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();

        for (Integer entitlementId : entitlementIds) {
            // 1. 写流水
            UserEntitlementRecord record = new UserEntitlementRecord();
            record.setUserId(userId);
            record.setEntitlementId(entitlementId);
            record.setSourceType(sourceType);
            record.setSourceId(sourceId);
            record.setDurationDays(durationDays);
            record.setExpireAt(now.plusDays(durationDays));
            record.setRegion(region);
            record.setCreateTime(now);
            recordRepository.save(record);

            // 2. UPSERT 汇总
            Optional<UserEntitlement> existing =
                    userEntitlementRepository.findByUserIdAndEntitlementId(userId, entitlementId);

            LocalDateTime cursor = now;
            UserEntitlement userEntitlement;
            if (existing.isPresent()) {
                userEntitlement = existing.get();
                if (userEntitlement.getExpireAt() != null && userEntitlement.getExpireAt().isAfter(now)) {
                    cursor = userEntitlement.getExpireAt();
                }
                userEntitlement.setExpireAt(cursor.plusDays(durationDays));
                userEntitlement.setStatus("ACTIVE");
                userEntitlement.setUpdateTime(now);
            } else {
                userEntitlement = new UserEntitlement();
                userEntitlement.setUserId(userId);
                userEntitlement.setEntitlementId(entitlementId);
                userEntitlement.setExpireAt(cursor.plusDays(durationDays));
                userEntitlement.setStatus("ACTIVE");
                userEntitlement.setCreateTime(now);
                userEntitlement.setUpdateTime(now);
            }
            userEntitlementRepository.save(userEntitlement);
        }

        log.info("Granted {} entitlements: userId={}, sourceType={}, sourceId={}, days={}",
                entitlementIds.size(), userId, sourceType, sourceId, durationDays);
    }

    @Override
    public boolean hasModuleAccess(Long userId, String moduleCode) {
        // 根据 moduleCode 查 entitlement
        Optional<Entitlement> entitlementOpt = entitlementRepository.findByModuleCode(moduleCode);
        if (!entitlementOpt.isPresent()) {
            return false; // 未知模块，拒绝
        }

        Integer entitlementId = entitlementOpt.get().getId();
        Optional<UserEntitlement> userEntitlement =
                userEntitlementRepository.findByUserIdAndEntitlementId(userId, entitlementId);

        return userEntitlement.isPresent()
                && "ACTIVE".equals(userEntitlement.get().getStatus())
                && userEntitlement.get().getExpireAt() != null
                && userEntitlement.get().getExpireAt().isAfter(LocalDateTime.now());
    }

    @Override
    public boolean hasReceivedTrial(Long userId) {
        return recordRepository.existsByUserIdAndSourceType(userId, "TRIAL");
    }

    @Override
    public List<UserEntitlementVO> getUserEntitlements(Long userId) {
        List<UserEntitlement> summaryList = userEntitlementRepository.findByUserId(userId);
        if (summaryList.isEmpty()) {
            return new ArrayList<>();
        }

        // Batch load entitlement metadata
        List<Entitlement> entitlements = entitlementRepository.findAllById(
                summaryList.stream().map(UserEntitlement::getEntitlementId)
                        .collect(Collectors.toList()));

        java.util.Map<Integer, Entitlement> entitlementMap = new java.util.HashMap<>();
        for (Entitlement e : entitlements) {
            entitlementMap.put(e.getId(), e);
        }

        LocalDateTime now = LocalDateTime.now();
        return summaryList.stream().map(s -> {
            Entitlement e = entitlementMap.get(s.getEntitlementId());
            return UserEntitlementVO.builder()
                    .entitlementId(s.getEntitlementId())
                    .entitlementCode(e != null ? e.getCode() : null)
                    .entitlementName(e != null ? e.getName() : null)
                    .moduleCode(e != null ? e.getModuleCode() : null)
                    .expireAt(s.getExpireAt())
                    .active("ACTIVE".equals(s.getStatus())
                            && s.getExpireAt() != null
                            && s.getExpireAt().isAfter(now))
                    .build();
        }).collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/EntitlementServiceImpl.java
git commit -m "feat: add EntitlementServiceImpl with grant, hasModuleAccess, getUserEntitlements"
```

---

### Task 11: 更新 PaymentServiceImpl — 切换 grant 调用

**Files:**
- Modify: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/PaymentServiceImpl.java`

**Interfaces:**
- Consumes: EntitlementService.grantEntitlements, GridProduct.entitlementIds (JSON)
- Modifies: 支付回调中 grant 逻辑从单产品改为按 entitlementIds 批量发放

- [ ] **Step 1: 改 imports 和注入**

将文件开头的 import 和注入改为：

```java
package com.naon.grid.modules.billing.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.naon.grid.enums.BillingCycleEnum;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.domain.PaymentRecord;
import com.naon.grid.modules.billing.domain.PaymentSubscription;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.repository.GridProductRepository;
import com.naon.grid.modules.billing.repository.PaymentRecordRepository;
import com.naon.grid.modules.billing.repository.PaymentSubscriptionRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import com.naon.grid.modules.billing.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final GridOrderRepository orderRepository;
    private final GridProductRepository productRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final EntitlementService entitlementService;
    private final PaymentSubscriptionRepository subscriptionRepository;
```

- [ ] **Step 2: 改 handlePaymentCallback 中的 grant 逻辑**

将文件中 `entitlementEngine.grant(...)` 调用替换为：

```java
        // Grant entitlements from product's entitlementIds
        GridProduct product = productRepository.findByCode(order.getProductCode())
                .orElse(null);
        if (product != null && product.getEntitlementIds() != null
                && !product.getEntitlementIds().isEmpty()) {
            JSONArray arr = JSON.parseArray(product.getEntitlementIds());
            // entitlementIds 存的是 code（如 "VOCAB_ACCESS"），需转为数字 ID
            // 这里改为在 grantEntitlements 中支持 code 列表，或先查 ID
            // 暂时先查表转换
            List<Integer> ids = arr.stream()
                    .map(Object::toString)
                    .map(code -> entitlementRepository.findByCode(code)
                            .orElseThrow(() -> new RuntimeException("权益不存在: " + code)))
                    .map(e -> e.getId())
                    .collect(Collectors.toList());
            entitlementService.grantEntitlements(
                    order.getUserId(), ids,
                    "PURCHASE", order.getOrderNo(), days, order.getRegion());
        }
```

- [ ] **Step 3: 加 entitlementRepository 注入**

在 PaymentServiceImpl 的字段声明中添加：

```java
    private final EntitlementRepository entitlementRepository;
```

并在 imports 中加上：

```java
import com.naon.grid.modules.billing.repository.EntitlementRepository;
```

- [ ] **Step 4: 精简 PaymentSubscription 创建逻辑**

由于 PaymentSubscription 字段已精简（无 orderId, billingCycle, region 等字段），将 subscription 创建部分改为：

```java
        if (order.getChannelSubId() != null) {
            PaymentSubscription sub = subscriptionRepository
                    .findByChannelAndChannelSubId("PHOTONPAY", order.getChannelSubId())
                    .orElseGet(() -> {
                        PaymentSubscription newSub = new PaymentSubscription();
                        newSub.setUserId(order.getUserId());
                        newSub.setProductCode(order.getProductCode());
                        newSub.setChannel("PHOTONPAY");
                        newSub.setChannelSubId(order.getChannelSubId());
                        newSub.setCreateTime(LocalDateTime.now());
                        return newSub;
                    });
            sub.setStatus("ACTIVE");
            subscriptionRepository.save(sub);
            log.info("Payment subscription processed: userId={}, product={}, subId={}",
                    order.getUserId(), order.getProductCode(), order.getChannelSubId());
        }
```

- [ ] **Step 5: 提交**

```bash
git add grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/PaymentServiceImpl.java
git commit -m "feat: switch PaymentServiceImpl to EntitlementService.grantEntitlements based on product entitlementIds"
```

---

### Task 12: 更新 ProductAccessAspect — 切换为 EntitlementService

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/aspect/ProductAccessAspect.java`

**Interfaces:**
- Consumes: EntitlementService.hasModuleAccess 替代 EntitlementEngine.hasAccess
- Modifies: 注入从 EntitlementEngine 改为 EntitlementService；RequireProduct.value() 对应 moduleCode 而非 productCode

- [ ] **Step 1: 改注入和鉴权调用**

```java
package com.naon.grid.modules.app.aspect;

import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.annotation.RequireOrgRole;
import com.naon.grid.modules.app.annotation.RequireProduct;
import com.naon.grid.modules.app.enums.AppErrorCode;
import com.naon.grid.modules.app.security.AppAuthenticationToken;
import com.naon.grid.modules.billing.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ProductAccessAspect {

    private final EntitlementService entitlementService;

    @Pointcut("@annotation(com.naon.grid.modules.app.annotation.RequireProduct)")
    public void pointcut() {}

    @Around("pointcut()")
    public Object checkAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AppAuthenticationToken)) {
            throw new BadRequestException(AppErrorCode.SUBSCRIPTION_REQUIRED.getMessage());
        }

        AppAuthenticationToken appAuth = (AppAuthenticationToken) authentication;
        Long userId = appAuth.getUserId();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireProduct annotation = AnnotationUtils.findAnnotation(method, RequireProduct.class);
        if (annotation == null) return joinPoint.proceed();

        // Check module access via EntitlementService
        String[] requiredModules = annotation.value();
        if (requiredModules.length > 0) {
            boolean hasAccess = false;
            for (String moduleCode : requiredModules) {
                if (entitlementService.hasModuleAccess(userId, moduleCode)) {
                    hasAccess = true;
                    break;
                }
            }
            if (!hasAccess) {
                log.warn("Module access denied: userId={}, required={}", userId, requiredModules);
                throw new BadRequestException(AppErrorCode.SUBSCRIPTION_REQUIRED.getMessage());
            }
        }

        // Check org role
        RequireOrgRole requiredOrgRole = annotation.orgRole();
        if (requiredOrgRole == RequireOrgRole.ADMIN) {
            if (!"ADMIN".equals(appAuth.getOrgRole())) {
                log.warn("Org role access denied: userId={}, required=ADMIN, actual={}",
                        userId, appAuth.getOrgRole());
                throw new BadRequestException(AppErrorCode.FORBIDDEN.getMessage());
            }
        } else if (requiredOrgRole == RequireOrgRole.MEMBER) {
            if (appAuth.getOrgId() == null) {
                log.warn("Org role access denied: userId={}, required=MEMBER, not in any org", userId);
                throw new BadRequestException(AppErrorCode.FORBIDDEN.getMessage());
            }
        }

        return joinPoint.proceed();
    }
}
```

关键变更：
- `EntitlementEngine` → `EntitlementService`
- `entitlementEngine.hasAccess(userId, productCode)` → `entitlementService.hasModuleAccess(userId, moduleCode)`
- 移除 `isValidForRegion` 调用（老设计 Phase 1 只记日志不放行，本阶段不保留）
- 移除 `ServletRequestAttributes` / `HttpServletRequest` 区域读取逻辑

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/aspect/ProductAccessAspect.java
git commit -m "refactor: ProductAccessAspect switches to EntitlementService.hasModuleAccess"
```

---

### Task 13: 重写 AppSubscriptionController

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppSubscriptionController.java`

**Interfaces:**
- Consumes: EntitlementService.getUserEntitlements, PaymentSubscriptionRepository
- Produces: GET /my（返回权益列表）, POST /cancel

- [ ] **Step 1: 重写 Controller**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.modules.billing.domain.PaymentSubscription;
import com.naon.grid.modules.billing.repository.PaymentSubscriptionRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import com.naon.grid.modules.billing.service.PaymentGateway;
import com.naon.grid.modules.billing.service.dto.UserEntitlementVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/subscription")
@Api(tags = "用户：订阅接口")
public class AppSubscriptionController {

    private final EntitlementService entitlementService;
    private final PaymentGateway paymentGateway;
    private final PaymentSubscriptionRepository subscriptionRepository;

    @ApiOperation("查询我的订阅/权益状态")
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMySubscription() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        List<UserEntitlementVO> entitlements = entitlementService.getUserEntitlements(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("entitlements", entitlements);
        result.put("hasTrial", entitlementService.hasReceivedTrial(userId));

        List<PaymentSubscription> activeSubs =
                subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE");
        result.put("hasAutoRenew", !activeSubs.isEmpty());

        return ResponseEntity.ok(result);
    }

    @ApiOperation("取消自动续费订阅")
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        List<PaymentSubscription> activeSubs =
                subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE");
        for (PaymentSubscription sub : activeSubs) {
            if (sub.getChannelSubId() != null) {
                paymentGateway.cancelSubscription(sub.getChannelSubId());
            }
            sub.setStatus("CANCELLED");
            sub.setCancelAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
        }
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppSubscriptionController.java
git commit -m "refactor: AppSubscriptionController uses EntitlementService, returns entitlement list"
```

---

### Task 14: 更新 AppAuthServiceImpl — 试用发放切换

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java`

**Interfaces:**
- Consumes: EntitlementService.grantEntitlements 替代 entitlementEngine.grant
- Modifies: register(), createSocialUser(), socialBindEmail() 中的试用发放逻辑

- [ ] **Step 1: 改 imports 和注入**

移除旧的：
```java
import com.naon.grid.modules.app.domain.GridUserRole;
import com.naon.grid.modules.app.repository.GridUserRoleRepository;
import com.naon.grid.modules.app.service.SubscriptionService;
import com.naon.grid.modules.billing.service.EntitlementEngine;
```

替换为：
```java
import com.naon.grid.modules.billing.service.EntitlementService;
```

注入从：
```java
    private final GridUserRoleRepository userRoleRepository;
    private final SubscriptionService subscriptionService;
    private final EntitlementEngine entitlementEngine;
```

改为：
```java
    private final EntitlementService entitlementService;
```

- [ ] **Step 2: 改 register() 方法**

移除 `GridUserRole` 创建逻辑和旧的 trial grant，替换为：

```java
        // 移除：
        // GridUserRole normalRole = new GridUserRole();
        // normalRole.setUserId(user.getId());
        // normalRole.setRoleCode("NORMAL");
        // normalRole.setRoleName("普通用户");
        // userRoleRepository.save(normalRole);

        // Grant trial — 发放全部6个权益各7天
        try {
            List<Integer> allEntitlementIds = java.util.Arrays.asList(1, 2, 3, 4, 5, 6);
            entitlementService.grantEntitlements(
                    user.getId(), allEntitlementIds,
                    "TRIAL", null, 7, region);
        } catch (Exception e) {
            log.error("Failed to grant trial for userId={}", user.getId(), e);
        }
```

- [ ] **Step 3: 改 createSocialUser() 方法**

同样的改动——移除 GridUserRole 创建，替换 trial grant：

```java
        // 移除 GridUserRole normalRole = ... userRoleRepository.save(normalRole);
        // 替换 trial grant：
        try {
            List<Integer> allEntitlementIds = java.util.Arrays.asList(1, 2, 3, 4, 5, 6);
            entitlementService.grantEntitlements(
                    user.getId(), allEntitlementIds,
                    "TRIAL", null, 7, region);
        } catch (Exception e) {
            log.error("Failed to grant trial for social userId={}", user.getId(), e);
        }
```

- [ ] **Step 4: 改 socialBindEmail() 方法**

同样的改动——移除 GridUserRole 创建，替换 trial grant。

- [ ] **Step 5: 改 generateToken() 方法**

移除对 `GridUserRoleRepository` 的依赖。`roles` 参数不再从数据库读取：

```java
    private TokenDTO generateToken(GridUser user, String deviceId, String deviceName) {
        // roles 简化为空列表（已不使用 role-based auth）
        List<String> roles = new ArrayList<>();

        Integer orgId = user.getOrgId();
        String accessToken = appTokenProvider.createToken(
                user.getId(), user.getEmail(), deviceId, roles,
                user.getUserType(),
                orgId != null ? orgId.intValue() : null,
                user.getOrgRole(),
                user.getRegion());
        // ... rest unchanged
```

- [ ] **Step 6: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java
git commit -m "refactor: AppAuthServiceImpl uses EntitlementService for trial grant, removes GridUserRole dependency"
```

---

### Task 15: 清理旧文件 — grid-billing

**Files:**
- Remove: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/EntitlementSource.java`
- Remove: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/ProductModule.java`
- Remove: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/EntitlementSourceRepository.java`
- Remove: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/ProductModuleRepository.java`
- Remove: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/EntitlementEngine.java`
- Remove: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/EntitlementEngineImpl.java`
- Remove: `grid-billing/src/test/java/com/naon/grid/modules/billing/service/impl/EntitlementEngineImplTest.java`

**Interfaces:**
- None（纯删除）

- [ ] **Step 1: 删除旧文件**

```bash
rm grid-billing/src/main/java/com/naon/grid/modules/billing/domain/EntitlementSource.java
rm grid-billing/src/main/java/com/naon/grid/modules/billing/domain/ProductModule.java
rm grid-billing/src/main/java/com/naon/grid/modules/billing/repository/EntitlementSourceRepository.java
rm grid-billing/src/main/java/com/naon/grid/modules/billing/repository/ProductModuleRepository.java
rm grid-billing/src/main/java/com/naon/grid/modules/billing/service/EntitlementEngine.java
rm grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/EntitlementEngineImpl.java
rm grid-billing/src/test/java/com/naon/grid/modules/billing/service/impl/EntitlementEngineImplTest.java
```

- [ ] **Step 2: 提交**

```bash
git add -A grid-billing/
git commit -m "chore: remove legacy EntitlementSource, ProductModule, EntitlementEngine"
```

---

### Task 16: 清理旧文件 — grid-app

**Files:**
- Remove: `grid-app/src/main/java/com/naon/grid/modules/app/domain/GridUserRole.java`
- Remove: `grid-app/src/main/java/com/naon/grid/modules/app/repository/GridUserRoleRepository.java`
- Remove: `grid-app/src/main/java/com/naon/grid/modules/app/service/SubscriptionService.java`
- Remove: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/SubscriptionServiceImpl.java`
- Remove: `grid-app/src/main/java/com/naon/grid/modules/app/aspect/SubscriptionAspect.java`
- Remove: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/AppSubscriptionVO.java`
- Remove: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/ActivateSubscriptionDTO.java`
- Remove: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/CreateOrderDTO.java`

**Interfaces:**
- None（纯删除）

- [ ] **Step 1: 删除旧文件**

```bash
rm grid-app/src/main/java/com/naon/grid/modules/app/domain/GridUserRole.java
rm grid-app/src/main/java/com/naon/grid/modules/app/repository/GridUserRoleRepository.java
rm grid-app/src/main/java/com/naon/grid/modules/app/service/SubscriptionService.java
rm grid-app/src/main/java/com/naon/grid/modules/app/service/impl/SubscriptionServiceImpl.java
rm grid-app/src/main/java/com/naon/grid/modules/app/aspect/SubscriptionAspect.java
rm grid-app/src/main/java/com/naon/grid/modules/app/service/dto/AppSubscriptionVO.java
rm grid-app/src/main/java/com/naon/grid/modules/app/service/dto/ActivateSubscriptionDTO.java
rm grid-app/src/main/java/com/naon/grid/modules/app/service/dto/CreateOrderDTO.java
```

- [ ] **Step 2: 提交**

```bash
git add -A grid-app/
git commit -m "chore: remove legacy GridUserRole, SubscriptionService, SubscriptionAspect, Subscription VOs"
```

---

### Task 17: 清理旧文件 — grid-common

**Files:**
- Remove: `grid-common/src/main/java/com/naon/grid/enums/UserLevel.java`
- Remove: `grid-common/src/main/java/com/naon/grid/annotation/RequireSubscription.java`

**Interfaces:**
- None（纯删除）

- [ ] **Step 1: 删除旧文件**

```bash
rm grid-common/src/main/java/com/naon/grid/enums/UserLevel.java
rm grid-common/src/main/java/com/naon/grid/annotation/RequireSubscription.java
```

- [ ] **Step 2: 提交**

```bash
git add -A grid-common/
git commit -m "chore: remove legacy UserLevel enum and RequireSubscription annotation"
```

---

### Task 18: 验证 ProductController 兼容性（grid-bootstrap）

**Files:**
- 检查: `grid-bootstrap/src/main/java/com/naon/grid/modules/system/rest/ProductController.java`

**Interfaces:**
- None（验证任务，无需改动）

- [ ] **Step 1: 确认无需改动**

ProductController 直接返回 `GridProduct` 实体列表，新增的 `entitlementIds`、`institutionConfig`、`coverImage` 均为 String 字段，Jackson 会自动序列化。无需代码改动。

- [ ] **Step 2: 提交**

```bash
# 无改动，跳过提交
echo "No changes needed for ProductController"
```

---

### Task 19: 更新 EntitlementResult DTO

**Files:**
- Modify: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/dto/EntitlementResult.java`

**Interfaces:**
- Modifies: 适配新模型

- [ ] **Step 1: 简化为适配 UserEntitlementVO 的列表包装**

```java
package com.naon.grid.modules.billing.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementResult {
    private Long userId;
    private List<UserEntitlementVO> entitlements;
    private LocalDateTime overallExpireAt;
    private boolean hasTrial;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-billing/src/main/java/com/naon/grid/modules/billing/service/dto/EntitlementResult.java
git commit -m "refactor: simplify EntitlementResult DTO for new entitlement model"
```

---

### Task 20: 构建验证 + 修复编译问题

**Files:**
- 所有被修改的文件

**Interfaces:**
- None（验证任务）

- [ ] **Step 1: 编译项目**

```bash
cd /Users/nano/Desktop/nano-gemini
mvn clean compile -DskipTests 2>&1 | tail -50
```

- [ ] **Step 2: 逐个修复编译错误**

检查编译输出，修复可能的遗留引用问题：
- 其他文件中对 `EntitlementEngine` 的引用 → 改为 `EntitlementService`
- 其他文件中对 `GridUserRole` / `GridUserRoleRepository` 的引用 → 移除
- 其他文件中对 `AppSubscriptionVO` / `CreateOrderDTO` / `ActivateSubscriptionDTO` 的引用 → 修复或移除
- `PaymentSubscription` 字段变更导致 `OrderServiceImpl` 中编译错误 → 修复

需要特别检查的文件：
- `grid-bootstrap` 下的文件
- `grid-system` 下的文件
- 任何 `@Autowired` 或 `@RequiredArgsConstructor` 注入 `EntitlementEngine` / `GridUserRoleRepository` / `SubscriptionService` 的地方

- [ ] **Step 3: 确保编译通过**

```bash
mvn clean compile -DskipTests
```

预期：BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "fix: resolve compilation errors after entitlement refactor"
```

---

### Task 21: 编写 EntitlementServiceImpl 单元测试

**Files:**
- Create: `grid-billing/src/test/java/com/naon/grid/modules/billing/service/impl/EntitlementServiceImplTest.java`

**Interfaces:**
- Consumes: EntitlementService, EntitlementServiceImpl, UserEntitlementRecordRepository, UserEntitlementRepository, EntitlementRepository

- [ ] **Step 1: 写测试**

```java
package com.naon.grid.modules.billing.service.impl;

import com.naon.grid.modules.billing.domain.Entitlement;
import com.naon.grid.modules.billing.domain.UserEntitlement;
import com.naon.grid.modules.billing.repository.EntitlementRepository;
import com.naon.grid.modules.billing.repository.UserEntitlementRecordRepository;
import com.naon.grid.modules.billing.repository.UserEntitlementRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceImplTest {

    @Mock private UserEntitlementRecordRepository recordRepository;
    @Mock private UserEntitlementRepository userEntitlementRepository;
    @Mock private EntitlementRepository entitlementRepository;

    private EntitlementService entitlementService;

    @BeforeEach
    void setUp() {
        entitlementService = new EntitlementServiceImpl(
                recordRepository, userEntitlementRepository, entitlementRepository);
    }

    @Test
    void grantEntitlements_newUser_createsSummary() {
        when(userEntitlementRepository.findByUserIdAndEntitlementId(1L, 1))
                .thenReturn(Optional.empty());

        entitlementService.grantEntitlements(1L, Arrays.asList(1),
                "TRIAL", null, 7, "A");

        verify(recordRepository, times(1)).save(any());
        verify(userEntitlementRepository, times(1)).save(any());
    }

    @Test
    void grantEntitlements_existingUser_stacksExpiry() {
        UserEntitlement existing = new UserEntitlement();
        existing.setUserId(1L);
        existing.setEntitlementId(1);
        existing.setExpireAt(LocalDateTime.now().plusDays(5));
        existing.setStatus("ACTIVE");

        when(userEntitlementRepository.findByUserIdAndEntitlementId(1L, 1))
                .thenReturn(Optional.of(existing));

        entitlementService.grantEntitlements(1L, Arrays.asList(1),
                "PURCHASE", "ORD001", 30, "A");

        verify(recordRepository, times(1)).save(any());
        verify(userEntitlementRepository, times(1)).save(argThat(ue ->
                ue.getExpireAt() != null &&
                ue.getExpireAt().isAfter(LocalDateTime.now().plusDays(30))));
    }

    @Test
    void hasModuleAccess_activeEntitlement_returnsTrue() {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(1);
        entitlement.setModuleCode("VOCAB");

        UserEntitlement ue = new UserEntitlement();
        ue.setExpireAt(LocalDateTime.now().plusDays(10));
        ue.setStatus("ACTIVE");

        when(entitlementRepository.findByModuleCode("VOCAB"))
                .thenReturn(Optional.of(entitlement));
        when(userEntitlementRepository.findByUserIdAndEntitlementId(1L, 1))
                .thenReturn(Optional.of(ue));

        assertTrue(entitlementService.hasModuleAccess(1L, "VOCAB"));
    }

    @Test
    void hasModuleAccess_expired_returnsFalse() {
        Entitlement entitlement = new Entitlement();
        entitlement.setId(1);
        entitlement.setModuleCode("VOCAB");

        UserEntitlement ue = new UserEntitlement();
        ue.setExpireAt(LocalDateTime.now().minusDays(1));
        ue.setStatus("ACTIVE");

        when(entitlementRepository.findByModuleCode("VOCAB"))
                .thenReturn(Optional.of(entitlement));
        when(userEntitlementRepository.findByUserIdAndEntitlementId(1L, 1))
                .thenReturn(Optional.of(ue));

        assertFalse(entitlementService.hasModuleAccess(1L, "VOCAB"));
    }

    @Test
    void hasModuleAccess_unknownModule_returnsFalse() {
        when(entitlementRepository.findByModuleCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertFalse(entitlementService.hasModuleAccess(1L, "UNKNOWN"));
    }

    @Test
    void hasReceivedTrial_true() {
        when(recordRepository.existsByUserIdAndSourceType(1L, "TRIAL"))
                .thenReturn(true);

        assertTrue(entitlementService.hasReceivedTrial(1L));
    }

    @Test
    void hasReceivedTrial_false() {
        when(recordRepository.existsByUserIdAndSourceType(1L, "TRIAL"))
                .thenReturn(false);

        assertFalse(entitlementService.hasReceivedTrial(1L));
    }

    @Test
    void grantEntitlements_emptyList_noOp() {
        entitlementService.grantEntitlements(1L, Arrays.asList(),
                "TRIAL", null, 7, "A");

        verify(recordRepository, never()).save(any());
        verify(userEntitlementRepository, never()).save(any());
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
cd /Users/nano/Desktop/nano-gemini
mvn test -pl grid-billing -Dtest=EntitlementServiceImplTest
```

预期：所有测试 PASS

- [ ] **Step 3: 提交**

```bash
git add grid-billing/src/test/java/com/naon/grid/modules/billing/service/impl/EntitlementServiceImplTest.java
git commit -m "test: add EntitlementServiceImpl unit tests"
```

---

### Task 22: 最终构建验证

**Files:**
- 整个项目

**Interfaces:**
- None（验证任务）

- [ ] **Step 1: 全量编译 + 测试**

```bash
cd /Users/nano/Desktop/nano-gemini
mvn clean install -DskipTests 2>&1 | tail -30
```

预期：BUILD SUCCESS

- [ ] **Step 2: 运行全部测试**

```bash
mvn test 2>&1 | tail -50
```

预期：所有测试 PASS（忽略已删除的 EntitlementEngineImplTest）

- [ ] **Step 3: 检查 git status**

```bash
git status
```

确认没有意外的未跟踪文件

- [ ] **Step 4: 最终提交（如有残留修复）**

```bash
git add -A
git commit -m "chore: final cleanup after entitlement refactor build verification"
```
