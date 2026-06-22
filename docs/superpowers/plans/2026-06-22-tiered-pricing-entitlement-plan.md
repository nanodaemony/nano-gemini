# Tiered Pricing & Entitlement System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the complete regional tiered pricing, entitlement stacking engine, three user types (NORMAL/INSTITUTION/AGENT), referral system, and product/order/payment stubs for the "有路中文 YourRoad" platform.

**Architecture:** New `grid-billing` module for product/pricing/entitlement/order/payment domain logic. Extend `grid-app` for user types, organization/agent/referral entities, region interceptor, and `@RequireProduct` annotation. Extend `grid-system` for admin-facing product CRUD and institution/agent audit controllers.

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MySQL, Redis, ip2region (via `mica-ip2region` already in pom.xml), Fastjson2, MapStruct, Lombok.

## Global Constraints

- Java 1.8 compatible (no `var`, no records, no switch expressions)
- All entities in `grid-billing` extend `com.naon.grid.base.BaseEntity` where audit fields are appropriate
- `grid-billing` entities DO NOT extend BaseEntity if they're pure record/tracking tables (payment_record, entitlement_source)
- Use Lombok `@Getter @Setter @RequiredArgsConstructor` on entities
- Use `@Repository` on all repositories
- Service interface + impl pattern (interface in `service/`, impl in `service/impl/`)
- `@Transactional(rollbackFor = Exception.class)` on all write services
- Table naming: `grid_*` in app/billing modules, lowercase with underscores
- All REST controllers return `ResponseEntity<>`
- Swagger: `@Api(tags = ...)` on class, `@ApiOperation(...)` on method
- Maven: no tests required (`-DskipTests` default), but EntitlementEngine must have a unit test
- Module dependency: `grid-billing` is new; `grid-app` and `grid-system` both add `grid-billing` dependency; `grid-bootstrap` does NOT directly depend on `grid-billing` (Spring Boot auto-config scans it via `@SpringBootApplication scanBasePackages`)

---

### Task 1: Create grid-billing Maven module

**Files:**
- Create: `grid-billing/pom.xml`
- Modify: `pom.xml` (root, add `<module>grid-billing</module>`)
- Modify: `grid-app/pom.xml` (add grid-billing dependency)
- Modify: `grid-system/pom.xml` (add grid-billing dependency)
- Modify: `grid-bootstrap/src/main/java/com/naon/grid/AppRun.java` (add scanBasePackages for billing)

**Interfaces:**
- Consumes: existing parent pom.xml structure
- Produces: compilable grid-billing module with correct dependencies

- [ ] **Step 1: Create `grid-billing/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>grid</artifactId>
        <groupId>com.naon.grid</groupId>
        <version>2.7</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>grid-billing</artifactId>
    <name>计费模块</name>
    <description>产品定价、权益堆叠、订单支付</description>

    <dependencies>
        <dependency>
            <groupId>com.naon.grid</groupId>
            <artifactId>grid-common</artifactId>
            <version>2.7</version>
        </dependency>
        <dependency>
            <groupId>com.naon.grid</groupId>
            <artifactId>grid-logging</artifactId>
            <version>2.7</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Add `<module>grid-billing</module>` to root `pom.xml`**

Find the `<modules>` block and add grid-billing after grid-app:

```xml
<module>grid-common</module>
<module>grid-logging</module>
<module>grid-tools</module>
<module>grid-system</module>
<module>grid-app</module>
<module>grid-billing</module>   <!-- ADD HERE -->
<module>grid-bootstrap</module>
```

- [ ] **Step 3: Add grid-billing dependency to `grid-app/pom.xml`**

Add after the existing grid-system dependency:

```xml
<dependencies>
    <dependency>
        <groupId>com.naon.grid</groupId>
        <artifactId>grid-system</artifactId>
        <version>2.7</version>
    </dependency>
    <dependency>
        <groupId>com.naon.grid</groupId>
        <artifactId>grid-billing</artifactId>
        <version>2.7</version>
    </dependency>
</dependencies>
```

- [ ] **Step 4: Add grid-billing dependency to `grid-system/pom.xml`**

```xml
<dependencies>
    <!-- existing dependencies... -->
    <dependency>
        <groupId>com.naon.grid</groupId>
        <artifactId>grid-tools</artifactId>
        <version>2.7</version>
    </dependency>
    <dependency>
        <groupId>com.naon.grid</groupId>
        <artifactId>grid-billing</artifactId>
        <version>2.7</version>
    </dependency>
    <!-- ... rest of existing dependencies -->
</dependencies>
```

- [ ] **Step 5: Modify `AppRun.java` scanBasePackages to include billing**

```java
@SpringBootApplication(scanBasePackages = {
    "com.naon.grid.modules.billing",
    "com.naon.grid.modules.app",
    "com.naon.grid.modules.system",
    "com.naon.grid.modules.tools",
    "com.naon.grid.common",
    "com.naon.grid.config",
    "com.naon.grid.annotation"
})
```

- [ ] **Step 6: Create directory structure for grid-billing**

```bash
mkdir -p grid-billing/src/main/java/com/naon/grid/modules/billing/domain
mkdir -p grid-billing/src/main/java/com/naon/grid/modules/billing/repository
mkdir -p grid-billing/src/main/java/com/naon/grid/modules/billing/service/dto
mkdir -p grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl
mkdir -p grid-billing/src/main/java/com/naon/grid/modules/billing/config
```

- [ ] **Step 7: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 2: Add new enums to grid-common

**Files:**
- Create: `grid-common/src/main/java/com/naon/grid/enums/UserTypeEnum.java`
- Create: `grid-common/src/main/java/com/naon/grid/enums/RegionEnum.java`
- Create: `grid-common/src/main/java/com/naon/grid/enums/BillingCycleEnum.java`
- Create: `grid-common/src/main/java/com/naon/grid/enums/ProductTypeEnum.java`
- Create: `grid-common/src/main/java/com/naon/grid/enums/SourceTypeEnum.java`
- Create: `grid-common/src/main/java/com/naon/grid/enums/EntitlementStatusEnum.java`
- Create: `grid-common/src/main/java/com/naon/grid/enums/OrderStatusEnum.java`
- Create: `grid-common/src/main/java/com/naon/grid/enums/AuditStatusEnum.java`
- Create: `grid-common/src/main/java/com/naon/grid/enums/OrgRoleEnum.java`

**Interfaces:**
- Consumes: existing enums in `com.naon.grid.enums` package
- Produces: reusable enums used by grid-billing and grid-app

- [ ] **Step 1: Create `UserTypeEnum.java`**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum UserTypeEnum {
    NORMAL("NORMAL", "普通用户"),
    INSTITUTION("INSTITUTION", "机构用户"),
    AGENT("AGENT", "代理商用户");

    private final String code;
    private final String description;

    UserTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserTypeEnum fromCode(String code) {
        for (UserTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return NORMAL;
    }
}
```

- [ ] **Step 2: Create `RegionEnum.java`**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum RegionEnum {
    A("A", "北美、西欧、北欧"),
    B("B", "日韩澳新、中东高收入、新加坡及港澳台"),
    C("C", "中国大陆"),
    D("D", "东南亚(除新加坡)、东欧、拉美"),
    E("E", "非洲、南亚、中亚及部分低收入地区");

    private final String code;
    private final String description;

    RegionEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RegionEnum fromCode(String code) {
        for (RegionEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
```

- [ ] **Step 3: Create `BillingCycleEnum.java`**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum BillingCycleEnum {
    MONTHLY("MONTHLY", "月度", 30),
    QUARTERLY("QUARTERLY", "季度", 90),
    YEARLY("YEARLY", "年度", 365);

    private final String code;
    private final String description;
    private final int days;

    BillingCycleEnum(String code, String description, int days) {
        this.code = code;
        this.description = description;
        this.days = days;
    }

    public static BillingCycleEnum fromCode(String code) {
        for (BillingCycleEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
```

- [ ] **Step 4: Create `ProductTypeEnum.java`**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum ProductTypeEnum {
    PLUS("PLUS", "全平台会员"),
    SINGLE_MODULE("SINGLE_MODULE", "单模块"),
    INSTITUTION("INSTITUTION", "机构套餐"),
    ENTERPRISE("ENTERPRISE", "企业定制");

    private final String code;
    private final String description;

    ProductTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 5: Create `SourceTypeEnum.java`**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum SourceTypeEnum {
    TRIAL("TRIAL", "注册试用"),
    PURCHASE("PURCHASE", "购买"),
    INSTITUTION("INSTITUTION", "机构授权"),
    REFERRAL("REFERRAL", "推荐奖励"),
    ADMIN_GRANT("ADMIN_GRANT", "后台发放");

    private final String code;
    private final String description;

    SourceTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 6: Create `EntitlementStatusEnum.java`**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum EntitlementStatusEnum {
    ACTIVE("ACTIVE", "有效"),
    REVOKED("REVOKED", "已撤销"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String description;

    EntitlementStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 7: Create `OrderStatusEnum.java`**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum OrderStatusEnum {
    PENDING("PENDING", "待支付"),
    PAID("PAID", "已支付"),
    REFUNDING("REFUNDING", "退款中"),
    REFUNDED("REFUNDED", "已退款"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String description;

    OrderStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 8: Create `AuditStatusEnum.java`**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum AuditStatusEnum {
    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String description;

    AuditStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 9: Create `OrgRoleEnum.java`**

```java
package com.naon.grid.enums;

import lombok.Getter;

@Getter
public enum OrgRoleEnum {
    ADMIN("ADMIN", "机构管理员"),
    MEMBER("MEMBER", "机构成员");

    private final String code;
    private final String description;

    OrgRoleEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

- [ ] **Step 10: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 3: Create grid-billing entities and repositories

**Files:**
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/GridProduct.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/ProductModule.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/RegionPricing.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/EntitlementSource.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/GridOrder.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/domain/PaymentRecord.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/GridProductRepository.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/ProductModuleRepository.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/RegionPricingRepository.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/EntitlementSourceRepository.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/GridOrderRepository.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/repository/PaymentRecordRepository.java`

**Interfaces:**
- Produces: JPA entities matching the database schema from the design doc, with repositories for data access

- [ ] **Step 1: Create `GridProduct.java`**

```java
package com.naon.grid.modules.billing.domain;

import com.naon.grid.base.BaseEntity;
import com.naon.grid.enums.ProductTypeEnum;
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

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer sortOrder;

    private Integer status = 1;
}
```

- [ ] **Step 2: Create `ProductModule.java`**

```java
package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "product_module", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"productId", "moduleCode"})
})
public class ProductModule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(length = 50, nullable = false)
    private String moduleCode;
}
```

- [ ] **Step 3: Create `RegionPricing.java`**

```java
package com.naon.grid.modules.billing.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "region_pricing", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"productId", "region", "billingCycle"})
})
public class RegionPricing extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(length = 10, nullable = false)
    private String region;

    @Column(length = 20, nullable = false)
    private String billingCycle;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(length = 10, nullable = false)
    private String currency;

    private Integer status = 1;
}
```

- [ ] **Step 4: Create `EntitlementSource.java`** (core stacking entity)

```java
package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "entitlement_source", indexes = {
    @Index(name = "idx_user_product", columnList = "userId, productCode, status"),
    @Index(name = "idx_source", columnList = "sourceType, sourceId")
})
public class EntitlementSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 30, nullable = false)
    private String sourceType;

    @Column(length = 100)
    private String sourceId;

    @Column(length = 50, nullable = false)
    private String productCode;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    @Column(nullable = false)
    private Integer durationDays;

    private LocalDateTime expireAt;

    @Column(length = 10)
    private String region;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(length = 500)
    private String remark;

    private LocalDateTime createTime;
}
```

- [ ] **Step 5: Create `GridOrder.java`**

```java
package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "grid_order")
public class GridOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, unique = true, nullable = false)
    private String orderNo;

    @Column(nullable = false)
    private Long userId;

    private Integer orgId;

    @Column(length = 50, nullable = false)
    private String productCode;

    @Column(length = 10, nullable = false)
    private String region;

    @Column(length = 20, nullable = false)
    private String billingCycle;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 10, nullable = false)
    private String currency;

    @Column(length = 30, nullable = false)
    private String status = "PENDING";

    @Column(length = 30)
    private String paymentMethod;

    private LocalDateTime paidAt;

    private LocalDateTime expireAt;

    private LocalDateTime createTime;
}
```

- [ ] **Step 6: Create `PaymentRecord.java`**

```java
package com.naon.grid.modules.billing.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "payment_record")
public class PaymentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(length = 30, nullable = false)
    private String paymentMethod;

    @Column(length = 200)
    private String transactionId;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 10, nullable = false)
    private String currency;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String rawCallback;

    private LocalDateTime createTime;
}
```

- [ ] **Step 7: Create `GridProductRepository.java`**

```java
package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.GridProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridProductRepository extends JpaRepository<GridProduct, Integer>, JpaSpecificationExecutor<GridProduct> {
    Optional<GridProduct> findByCode(String code);
    List<GridProduct> findByStatusOrderBySortOrder(Integer status);
}
```

- [ ] **Step 8: Create remaining repositories**

`ProductModuleRepository.java`:
```java
package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.ProductModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductModuleRepository extends JpaRepository<ProductModule, Integer> {
    List<ProductModule> findByProductId(Integer productId);
    List<String> findModuleCodeByProductId(Integer productId);
}
```

`RegionPricingRepository.java`:
```java
package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.RegionPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegionPricingRepository extends JpaRepository<RegionPricing, Integer> {
    Optional<RegionPricing> findByProductIdAndRegionAndBillingCycle(Integer productId, String region, String billingCycle);
    List<RegionPricing> findByProductIdAndRegion(Integer productId, String region);
    List<RegionPricing> findByProductId(Integer productId);
}
```

`EntitlementSourceRepository.java`:
```java
package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.EntitlementSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EntitlementSourceRepository extends JpaRepository<EntitlementSource, Long> {
    List<EntitlementSource> findByUserIdAndProductCodeAndStatusOrderByGrantedAtAsc(
            Long userId, String productCode, String status);
    List<EntitlementSource> findByUserIdAndStatusOrderByGrantedAtAsc(Long userId, String status);
    List<EntitlementSource> findByUserIdOrderByGrantedAtAsc(Long userId);
}
```

`GridOrderRepository.java`:
```java
package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.GridOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GridOrderRepository extends JpaRepository<GridOrder, Long> {
    Optional<GridOrder> findByOrderNo(String orderNo);
    List<GridOrder> findByUserIdOrderByCreateTimeDesc(Long userId);
    List<GridOrder> findByOrgIdOrderByCreateTimeDesc(Integer orgId);
}
```

`PaymentRecordRepository.java`:
```java
package com.naon.grid.modules.billing.repository;

import com.naon.grid.modules.billing.domain.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByOrderId(Long orderId);
    List<PaymentRecord> findByTransactionId(String transactionId);
}
```

- [ ] **Step 9: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 4: SQL migration script

**Files:**
- Create: `sql/billing.sql` (all new tables)

- [ ] **Step 1: Create `sql/billing.sql`**

```sql
-- ----------------------------
-- 产品表
-- ----------------------------
CREATE TABLE `grid_product` (
    `id` INT AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(50) NOT NULL COMMENT '产品代码 PLUS/VOCAB/GRAMMAR/...',
    `name` VARCHAR(200) NOT NULL COMMENT '产品名称',
    `product_type` VARCHAR(30) NOT NULL COMMENT 'PLUS / SINGLE_MODULE / INSTITUTION / ENTERPRISE',
    `description` TEXT COMMENT '产品描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` INT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-可用',
    `create_by` VARCHAR(50) COMMENT '创建人',
    `update_by` VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- ----------------------------
-- 产品模块关联表
-- ----------------------------
CREATE TABLE `product_module` (
    `id` INT AUTO_INCREMENT COMMENT '主键ID',
    `product_id` INT NOT NULL COMMENT '产品ID',
    `module_code` VARCHAR(50) NOT NULL COMMENT '模块代码 VOCAB/GRAMMAR/CHARACTER/CONFUSING_WORDS/CULTURE/TOPIC',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_module` (`product_id`, `module_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品模块关联表';

-- ----------------------------
-- 区域定价表
-- ----------------------------
CREATE TABLE `region_pricing` (
    `id` INT AUTO_INCREMENT COMMENT '主键ID',
    `product_id` INT NOT NULL COMMENT '产品ID',
    `region` VARCHAR(10) NOT NULL COMMENT '区域 A/B/C/D/E',
    `billing_cycle` VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY/YEARLY',
    `price` DECIMAL(12,2) NOT NULL COMMENT '金额',
    `currency` VARCHAR(10) NOT NULL COMMENT '币种 USD/EUR/CNY',
    `status` INT NOT NULL DEFAULT 1 COMMENT '状态',
    `create_by` VARCHAR(50) COMMENT '创建人',
    `update_by` VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_region_cycle` (`product_id`, `region`, `billing_cycle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域定价表';

-- ----------------------------
-- 权益来源表（核心堆叠表）
-- ----------------------------
CREATE TABLE `entitlement_source` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `source_type` VARCHAR(30) NOT NULL COMMENT 'TRIAL/PURCHASE/INSTITUTION/REFERRAL/ADMIN_GRANT',
    `source_id` VARCHAR(100) COMMENT '来源业务ID（订单号/机构ID等）',
    `product_code` VARCHAR(50) NOT NULL COMMENT '产品代码 PLUS/VOCAB/...',
    `granted_at` DATETIME NOT NULL COMMENT '授予时间',
    `duration_days` INT NOT NULL COMMENT '有效天数',
    `expire_at` DATETIME COMMENT '堆叠计算后的到期时间（缓存）',
    `region` VARCHAR(10) COMMENT '购买/授予时的区域',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REVOKED/EXPIRED',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_product` (`user_id`, `product_code`, `status`),
    KEY `idx_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权益来源表';

-- ----------------------------
-- 订单表
-- ----------------------------
CREATE TABLE `grid_order` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `user_id` INT NOT NULL COMMENT '下单用户ID',
    `org_id` INT COMMENT '机构下单时为机构ID',
    `product_code` VARCHAR(50) NOT NULL COMMENT '产品代码',
    `region` VARCHAR(10) NOT NULL COMMENT '区域',
    `billing_cycle` VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY/YEARLY',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
    `currency` VARCHAR(10) NOT NULL COMMENT '币种',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/REFUNDING/REFUNDED/EXPIRED',
    `payment_method` VARCHAR(30) COMMENT 'WECHAT/ALIPAY/STRIPE',
    `paid_at` DATETIME COMMENT '支付时间',
    `expire_at` DATETIME COMMENT '订单过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ----------------------------
-- 支付流水表
-- ----------------------------
CREATE TABLE `payment_record` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `payment_method` VARCHAR(30) NOT NULL COMMENT 'WECHAT/ALIPAY/STRIPE',
    `transaction_id` VARCHAR(200) COMMENT '支付平台交易号',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
    `currency` VARCHAR(10) NOT NULL COMMENT '币种',
    `status` VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED/REFUND',
    `raw_callback` TEXT COMMENT '原始回调JSON',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ----------------------------
-- 初始化产品数据
-- ----------------------------
INSERT INTO `grid_product` (`code`, `name`, `product_type`, `sort_order`, `status`) VALUES
('PLUS', '全平台Plus会员', 'PLUS', 1, 1),
('VOCAB', '词汇模块', 'SINGLE_MODULE', 2, 1),
('GRAMMAR', '语法模块', 'SINGLE_MODULE', 3, 1),
('CHARACTER', '汉字模块', 'SINGLE_MODULE', 4, 1),
('CONFUSING_WORDS', '易混淆词辨析模块', 'SINGLE_MODULE', 5, 1),
('CULTURE', '文化模块', 'SINGLE_MODULE', 6, 1),
('TOPIC', '话题模块', 'SINGLE_MODULE', 7, 1);

-- PLUS 包含所有子模块
INSERT INTO `product_module` (`product_id`, `module_code`) VALUES
(1, 'VOCAB'), (1, 'GRAMMAR'), (1, 'CHARACTER'),
(1, 'CONFUSING_WORDS'), (1, 'CULTURE'), (1, 'TOPIC');
```

- [ ] **Step 2: Create `sql/app_ext.sql`** (extensions to existing app tables)

```sql
-- ----------------------------
-- 机构表
-- ----------------------------
CREATE TABLE `grid_organization` (
    `id` INT AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(200) NOT NULL COMMENT '机构名称',
    `name_en` VARCHAR(200) COMMENT '机构英文名',
    `org_type` VARCHAR(20) NOT NULL COMMENT 'UNIVERSITY/SCHOOL/TRAINING/OTHER',
    `contact_name` VARCHAR(100) COMMENT '联系人姓名',
    `contact_email` VARCHAR(255) COMMENT '联系邮箱',
    `contact_phone` VARCHAR(50) COMMENT '联系电话',
    `country` VARCHAR(100) COMMENT '所在国家',
    `region` VARCHAR(10) COMMENT '区域 A/B/C/D/E',
    `status` INT NOT NULL DEFAULT 1 COMMENT '1-可用 0-已删除',
    `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `max_members` INT NOT NULL DEFAULT 0 COMMENT '最大成员数',
    `max_admins` INT NOT NULL DEFAULT 0 COMMENT '最大管理员数',
    `current_members` INT NOT NULL DEFAULT 0 COMMENT '当前成员数',
    `expire_time` DATETIME COMMENT '机构有效到期时间',
    `create_by` VARCHAR(50) COMMENT '创建人',
    `update_by` VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_audit_status` (`audit_status`),
    KEY `idx_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构表';

-- ----------------------------
-- 代理商表
-- ----------------------------
CREATE TABLE `grid_agent` (
    `id` INT AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(200) NOT NULL COMMENT '代理商名称',
    `contact_name` VARCHAR(100) COMMENT '联系人姓名',
    `contact_email` VARCHAR(255) COMMENT '联系邮箱',
    `contact_phone` VARCHAR(50) COMMENT '联系电话',
    `commission_rate` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '返现比例 %',
    `referral_code` VARCHAR(32) NOT NULL COMMENT '代理专用推荐码',
    `status` INT NOT NULL DEFAULT 1 COMMENT '1-可用 0-已删除',
    `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `create_by` VARCHAR(50) COMMENT '创建人',
    `update_by` VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_referral_code` (`referral_code`),
    KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商表';

-- ----------------------------
-- 推荐记录表
-- ----------------------------
CREATE TABLE `referral_record` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `referrer_id` INT NOT NULL COMMENT '推荐人用户ID',
    `referrer_type` VARCHAR(20) NOT NULL COMMENT 'NORMAL/INSTITUTION/AGENT',
    `referred_id` INT COMMENT '被推荐人用户ID',
    `referral_code` VARCHAR(32) NOT NULL COMMENT '使用的推荐码',
    `order_id` BIGINT COMMENT '关联订单ID',
    `reward_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/SETTLED/PAID',
    `reward_amount` DECIMAL(12,2) COMMENT '奖励金额',
    `reward_type` VARCHAR(20) COMMENT 'EXTEND_DAYS/CASH/MEMBER_COUNT',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `settle_time` DATETIME COMMENT '结算时间',
    PRIMARY KEY (`id`),
    KEY `idx_referrer_id` (`referrer_id`),
    KEY `idx_referral_code` (`referral_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐记录表';

-- ----------------------------
-- grid_user 扩展字段
-- ----------------------------
ALTER TABLE `grid_user`
    ADD COLUMN `user_type` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '用户类型 NORMAL/INSTITUTION/AGENT' AFTER `gender`,
    ADD COLUMN `org_id` INT COMMENT '所属机构ID' AFTER `user_type`,
    ADD COLUMN `org_role` VARCHAR(20) COMMENT '机构角色 ADMIN/MEMBER' AFTER `org_id`,
    ADD COLUMN `agent_id` INT COMMENT '所属代理ID' AFTER `org_role`,
    ADD COLUMN `referral_code` VARCHAR(32) COMMENT '我的推荐码' AFTER `agent_id`,
    ADD COLUMN `referred_by` VARCHAR(32) COMMENT '注册时填的推荐码' AFTER `referral_code`,
    ADD COLUMN `region` VARCHAR(10) COMMENT '所属区域 A/B/C/D/E' AFTER `referred_by`,
    ADD COLUMN `register_audit_status` VARCHAR(20) DEFAULT 'APPROVED' COMMENT '注册审核状态 PENDING/APPROVED/REJECTED' AFTER `region`;
```

---

### Task 5: Implement EntitlementEngine (core stacking algorithm)

**Files:**
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/EntitlementEngine.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/EntitlementEngineImpl.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/dto/EntitlementResult.java`

**Interfaces:**
- Produces: `EntitlementEngine.compute(userId)` → `EntitlementResult`, `EntitlementEngine.hasAccess(userId, productCode)` → `boolean`, `EntitlementEngine.grant(userId, sourceType, sourceId, productCode, days, region)` → `LocalDateTime`, `EntitlementEngine.revoke(sourceId)` → `void`, `EntitlementEngine.isValidForRegion(userId, currentRegion)` → `boolean`

- [ ] **Step 1: Create `EntitlementResult.java`**

```java
package com.naon.grid.modules.billing.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class EntitlementResult {
    private Long userId;
    private List<ProductEntitlement> productEntitlements;
    private LocalDateTime overallExpireAt;

    @Data
    @Builder
    @AllArgsConstructor
    public static class ProductEntitlement {
        private String productCode;
        private LocalDateTime expireAt;
        private boolean active;
    }
}
```

- [ ] **Step 2: Create `EntitlementEngine.java` interface**

```java
package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.service.dto.EntitlementResult;
import java.time.LocalDateTime;

public interface EntitlementEngine {

    /**
     * 计算用户的所有产品权益到期时间
     */
    EntitlementResult compute(Long userId);

    /**
     * 检查用户是否有指定产品代码的访问权限
     * PLUS 包含所有子模块
     */
    boolean hasAccess(Long userId, String productCode);

    /**
     * 检查用户是否有指定模块的访问权限（按 moduleCode，非 productCode）
     */
    boolean hasModuleAccess(Long userId, String moduleCode);

    /**
     * 授予权益，执行堆叠计算
     * @return 该产品的新的有效到期时间
     */
    LocalDateTime grant(Long userId, String sourceType, String sourceId,
                        String productCode, int durationDays, String region);

    /**
     * 撤销一条权益来源
     */
    void revoke(Long sourceId);

    /**
     * 检查用户的权益来源区域是否与当前区域匹配
     * 第一期：警告日志 + 返回 true
     */
    boolean isValidForRegion(Long userId, String currentRegion);
}
```

- [ ] **Step 3: Create `EntitlementEngineImpl.java`**

```java
package com.naon.grid.modules.billing.service.impl;

import com.naon.grid.modules.billing.domain.EntitlementSource;
import com.naon.grid.modules.billing.repository.EntitlementSourceRepository;
import com.naon.grid.modules.billing.repository.ProductModuleRepository;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import com.naon.grid.modules.billing.service.dto.EntitlementResult;
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
public class EntitlementEngineImpl implements EntitlementEngine {

    private static final String PLUS_PRODUCT_CODE = "PLUS";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final EntitlementSourceRepository sourceRepository;
    private final ProductModuleRepository productModuleRepository;

    @Override
    public EntitlementResult compute(Long userId) {
        List<EntitlementSource> sources = sourceRepository
                .findByUserIdAndStatusOrderByGrantedAtAsc(userId, STATUS_ACTIVE);

        // Group by productCode
        Map<String, List<EntitlementSource>> grouped = sources.stream()
                .collect(Collectors.groupingBy(EntitlementSource::getProductCode));

        List<EntitlementResult.ProductEntitlement> entitlements = new ArrayList<>();
        LocalDateTime overallExpireAt = null;

        for (Map.Entry<String, List<EntitlementSource>> entry : grouped.entrySet()) {
            LocalDateTime expireAt = computeStackedExpiry(entry.getValue());
            entitlements.add(EntitlementResult.ProductEntitlement.builder()
                    .productCode(entry.getKey())
                    .expireAt(expireAt)
                    .active(expireAt != null && expireAt.isAfter(LocalDateTime.now()))
                    .build());
            if (expireAt != null && (overallExpireAt == null || expireAt.isAfter(overallExpireAt))) {
                overallExpireAt = expireAt;
            }
        }

        return EntitlementResult.builder()
                .userId(userId)
                .productEntitlements(entitlements)
                .overallExpireAt(overallExpireAt)
                .build();
    }

    /**
     * 堆叠算法：对同一产品的多条来源，按 granted_at 排序后堆叠
     * cursor = max(cursor, source.granted_at) + source.duration_days
     */
    private LocalDateTime computeStackedExpiry(List<EntitlementSource> sources) {
        if (sources == null || sources.isEmpty()) return null;

        // Sort by grantedAt ascending
        sources.sort(Comparator.comparing(EntitlementSource::getGrantedAt));

        LocalDateTime cursor = LocalDateTime.now();

        for (EntitlementSource source : sources) {
            LocalDateTime grantTime = source.getGrantedAt();
            if (cursor.isBefore(grantTime)) {
                cursor = grantTime;
            }
            cursor = cursor.plusDays(source.getDurationDays());
        }

        return cursor;
    }

    @Override
    public boolean hasAccess(Long userId, String productCode) {
        EntitlementResult result = compute(userId);
        if (result.getProductEntitlements() == null) return false;

        // Check if user has PLUS (which includes all)
        boolean hasPlus = result.getProductEntitlements().stream()
                .anyMatch(e -> PLUS_PRODUCT_CODE.equals(e.getProductCode()) && e.isActive());

        if (hasPlus) return true;

        // Check specific product
        return result.getProductEntitlements().stream()
                .anyMatch(e -> productCode.equals(e.getProductCode()) && e.isActive());
    }

    @Override
    public boolean hasModuleAccess(Long userId, String moduleCode) {
        EntitlementResult result = compute(userId);
        if (result.getProductEntitlements() == null) return false;

        // Check PLUS first
        boolean hasPlus = result.getProductEntitlements().stream()
                .anyMatch(e -> PLUS_PRODUCT_CODE.equals(e.getProductCode()) && e.isActive());
        if (hasPlus) return true;

        // Check if user has a product that covers this module
        return result.getProductEntitlements().stream()
                .filter(EntitlementResult.ProductEntitlement::isActive)
                .anyMatch(e -> {
                    if (e.getProductCode().equals(moduleCode)) return true;
                    // Check if the product includes this module
                    return false; // Simplified: moduleCode matches productCode for single modules
                });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LocalDateTime grant(Long userId, String sourceType, String sourceId,
                               String productCode, int durationDays, String region) {
        // Compute current expiry for this product
        EntitlementResult result = compute(userId);
        LocalDateTime currentExpiry = null;
        if (result.getProductEntitlements() != null) {
            currentExpiry = result.getProductEntitlements().stream()
                    .filter(e -> productCode.equals(e.getProductCode()))
                    .findFirst()
                    .map(EntitlementResult.ProductEntitlement::getExpireAt)
                    .orElse(null);
        }

        // Stack: new expiry = max(currentExpiry, now) + durationDays
        LocalDateTime cursor = LocalDateTime.now();
        if (currentExpiry != null && currentExpiry.isAfter(cursor)) {
            cursor = currentExpiry;
        }
        LocalDateTime newExpireAt = cursor.plusDays(durationDays);

        EntitlementSource source = new EntitlementSource();
        source.setUserId(userId);
        source.setSourceType(sourceType);
        source.setSourceId(sourceId);
        source.setProductCode(productCode);
        source.setGrantedAt(LocalDateTime.now());
        source.setDurationDays(durationDays);
        source.setExpireAt(newExpireAt);
        source.setRegion(region);
        source.setStatus(STATUS_ACTIVE);
        source.setCreateTime(LocalDateTime.now());
        sourceRepository.save(source);

        log.info("Entitlement granted: userId={}, product={}, days={}, expireAt={}, source={}",
                userId, productCode, durationDays, newExpireAt, sourceType);

        return newExpireAt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long sourceId) {
        Optional<EntitlementSource> opt = sourceRepository.findById(sourceId);
        if (opt.isPresent()) {
            EntitlementSource source = opt.get();
            source.setStatus("REVOKED");
            sourceRepository.save(source);
            log.info("Entitlement revoked: sourceId={}, userId={}, product={}",
                    sourceId, source.getUserId(), source.getProductCode());
        }
    }

    @Override
    public boolean isValidForRegion(Long userId, String currentRegion) {
        List<EntitlementSource> sources = sourceRepository
                .findByUserIdAndStatusOrderByGrantedAtAsc(userId, STATUS_ACTIVE);

        boolean regionMatched = sources.stream()
                .anyMatch(s -> currentRegion.equals(s.getRegion()));

        if (!regionMatched) {
            log.warn("Region mismatch: userId={}, currentRegion={}, purchasedRegions={}",
                    userId, currentRegion,
                    sources.stream().map(EntitlementSource::getRegion).collect(Collectors.toSet()));
            // Phase 1: warn only, do not block
        }
        return true; // Phase 1: always pass
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 6: Create grid-billing services (ProductService, OrderService, PaymentService)

**Files:**
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/ProductService.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/ProductServiceImpl.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/OrderService.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/OrderServiceImpl.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/PaymentService.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/PaymentServiceImpl.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/dto/OrderCreateRequest.java`
- Create: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/dto/OrderCreateResponse.java`

**Interfaces:**
- Consumes: GridProductRepository, RegionPricingRepository, GridOrderRepository, EntitlementEngine
- Produces: OrderService.createOrder(...), PaymentService.handlePaymentCallback(...)

- [ ] **Step 1: Create `ProductService.java`**

```java
package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.domain.RegionPricing;
import java.util.List;
import java.util.Optional;

public interface ProductService {
    List<GridProduct> findAllActive();
    Optional<GridProduct> findByCode(String code);
    List<RegionPricing> getPricingByProductAndRegion(Integer productId, String region);
    Optional<RegionPricing> getPricing(Integer productId, String region, String billingCycle);
}
```

- [ ] **Step 2: Create `ProductServiceImpl.java`**

```java
package com.naon.grid.modules.billing.service.impl;

import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.domain.RegionPricing;
import com.naon.grid.modules.billing.repository.GridProductRepository;
import com.naon.grid.modules.billing.repository.RegionPricingRepository;
import com.naon.grid.modules.billing.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final GridProductRepository productRepository;
    private final RegionPricingRepository pricingRepository;

    @Override
    public List<GridProduct> findAllActive() {
        return productRepository.findByStatusOrderBySortOrder(1);
    }

    @Override
    public Optional<GridProduct> findByCode(String code) {
        return productRepository.findByCode(code);
    }

    @Override
    public List<RegionPricing> getPricingByProductAndRegion(Integer productId, String region) {
        return pricingRepository.findByProductIdAndRegion(productId, region);
    }

    @Override
    public Optional<RegionPricing> getPricing(Integer productId, String region, String billingCycle) {
        return pricingRepository.findByProductIdAndRegionAndBillingCycle(productId, region, billingCycle);
    }
}
```

- [ ] **Step 3: Create `OrderCreateRequest.java`**

```java
package com.naon.grid.modules.billing.service.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class OrderCreateRequest {
    @NotBlank(message = "产品代码不能为空")
    private String productCode;

    @NotBlank(message = "计费周期不能为空")
    private String billingCycle;

    private String region; // 不要求前端传入，后端从 request attribute 取
    private Integer orgId; // 机构下单时传入
}
```

- [ ] **Step 4: Create `OrderCreateResponse.java`**

```java
package com.naon.grid.modules.billing.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class OrderCreateResponse {
    private String orderNo;
    private String productCode;
    private String billingCycle;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String redirectUrl; // 支付跳转链接（第一期返回null）
}
```

- [ ] **Step 5: Create `OrderService.java`**

```java
package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.service.dto.OrderCreateRequest;
import com.naon.grid.modules.billing.service.dto.OrderCreateResponse;
import java.util.Optional;

public interface OrderService {
    OrderCreateResponse createOrder(Long userId, OrderCreateRequest request, String region);
    Optional<GridOrder> findByOrderNo(String orderNo);
    GridOrder save(GridOrder order);
}
```

- [ ] **Step 6: Create `OrderServiceImpl.java`**

```java
package com.naon.grid.modules.billing.service.impl;

import cn.hutool.core.util.IdUtil;
import com.naon.grid.enums.BillingCycleEnum;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.domain.RegionPricing;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.repository.RegionPricingRepository;
import com.naon.grid.modules.billing.service.OrderService;
import com.naon.grid.modules.billing.service.ProductService;
import com.naon.grid.modules.billing.service.dto.OrderCreateRequest;
import com.naon.grid.modules.billing.service.dto.OrderCreateResponse;
import com.naon.grid.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final GridOrderRepository orderRepository;
    private final RegionPricingRepository pricingRepository;
    private final ProductService productService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request, String region) {
        request.setRegion(region);

        // Find product and pricing
        var product = productService.findByCode(request.getProductCode())
                .orElseThrow(() -> new BadRequestException("产品不存在: " + request.getProductCode()));

        RegionPricing pricing = pricingRepository
                .findByProductIdAndRegionAndBillingCycle(product.getId(), region, request.getBillingCycle())
                .orElseThrow(() -> new BadRequestException(
                        "该产品在" + region + "区没有" + request.getBillingCycle() + "定价"));

        // Create order
        GridOrder order = new GridOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setOrgId(request.getOrgId());
        order.setProductCode(request.getProductCode());
        order.setRegion(region);
        order.setBillingCycle(request.getBillingCycle());
        order.setAmount(pricing.getPrice());
        order.setCurrency(pricing.getCurrency());
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());
        orderRepository.save(order);

        return OrderCreateResponse.builder()
                .orderNo(order.getOrderNo())
                .productCode(order.getProductCode())
                .billingCycle(order.getBillingCycle())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .redirectUrl(null) // Phase 1: no real payment
                .build();
    }

    @Override
    public Optional<GridOrder> findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo);
    }

    @Override
    public GridOrder save(GridOrder order) {
        return orderRepository.save(order);
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
    }
}
```

- [ ] **Step 7: Create `PaymentService.java`**

```java
package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.domain.PaymentRecord;
import java.util.Map;

public interface PaymentService {
    /**
     * 处理支付回调。Phase 1: 模拟成功，实际发放权益。
     * 真实接入时各平台（WeChat/Alipay/Stripe）实现此方法的内部逻辑。
     */
    boolean handlePaymentCallback(String orderNo, String paymentMethod, Map<String, Object> callbackData);
}
```

- [ ] **Step 8: Create `PaymentServiceImpl.java`**

```java
package com.naon.grid.modules.billing.service.impl;

import com.naon.grid.enums.BillingCycleEnum;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.domain.PaymentRecord;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.repository.PaymentRecordRepository;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import com.naon.grid.modules.billing.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final GridOrderRepository orderRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final EntitlementEngine entitlementEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaymentCallback(String orderNo, String paymentMethod, Map<String, Object> callbackData) {
        GridOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在: " + orderNo));

        if (!"PENDING".equals(order.getStatus())) {
            log.warn("订单 {} 状态已不是 PENDING: {}", orderNo, order.getStatus());
            return false;
        }

        // Phase 1: Always succeed (mock)
        order.setStatus("PAID");
        order.setPaymentMethod(paymentMethod);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // Record payment
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(order.getId());
        record.setPaymentMethod(paymentMethod);
        record.setTransactionId("MOCK_" + IdUtil.fastSimpleUUID().substring(0, 16));
        record.setAmount(order.getAmount());
        record.setCurrency(order.getCurrency());
        record.setStatus("SUCCESS");
        record.setRawCallback(callbackData != null ? callbackData.toString() : "mock");
        record.setCreateTime(LocalDateTime.now());
        paymentRecordRepository.save(record);

        // Grant entitlement
        BillingCycleEnum cycle = BillingCycleEnum.fromCode(order.getBillingCycle());
        int days = cycle != null ? cycle.getDays() : 365;
        entitlementEngine.grant(
                order.getUserId(),
                "PURCHASE",
                order.getOrderNo(),
                order.getProductCode(),
                days,
                order.getRegion()
        );

        log.info("Payment callback processed: orderNo={}, userId={}, product={}, days={}",
                orderNo, order.getUserId(), order.getProductCode(), days);
        return true;
    }
}
```

Note: Add `import cn.hutool.core.util.IdUtil;` to PaymentServiceImpl.

- [ ] **Step 9: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 7: Create grid-app entities (Organization, Agent, ReferralRecord) + repositories

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/domain/GridOrganization.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/domain/GridAgent.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/domain/ReferralRecord.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/repository/GridOrganizationRepository.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/repository/GridAgentRepository.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/repository/ReferralRecordRepository.java`

- [ ] **Step 1: Create `GridOrganization.java`**

```java
package com.naon.grid.modules.app.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "grid_organization")
public class GridOrganization extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 200)
    private String nameEn;

    @Column(length = 20, nullable = false)
    private String orgType;

    @Column(length = 100)
    private String contactName;

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 50)
    private String contactPhone;

    @Column(length = 100)
    private String country;

    @Column(length = 10)
    private String region;

    private Integer status = 1;

    @Column(length = 20, nullable = false)
    private String auditStatus = "PENDING";

    private Integer maxMembers = 0;
    private Integer maxAdmins = 0;
    private Integer currentMembers = 0;
    private LocalDateTime expireTime;
}
```

- [ ] **Step 2: Create `GridAgent.java`**

```java
package com.naon.grid.modules.app.domain;

import com.naon.grid.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "grid_agent")
public class GridAgent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 100)
    private String contactName;

    @Column(length = 255)
    private String contactEmail;

    @Column(length = 50)
    private String contactPhone;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(length = 32, unique = true, nullable = false)
    private String referralCode;

    private Integer status = 1;

    @Column(length = 20, nullable = false)
    private String auditStatus = "PENDING";
}
```

- [ ] **Step 3: Create `ReferralRecord.java`**

```java
package com.naon.grid.modules.app.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "referral_record")
public class ReferralRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long referrerId;

    @Column(length = 20, nullable = false)
    private String referrerType;

    private Long referredId;

    @Column(length = 32, nullable = false)
    private String referralCode;

    private Long orderId;

    @Column(length = 20)
    private String rewardStatus = "PENDING";

    @Column(precision = 12, scale = 2)
    private BigDecimal rewardAmount;

    @Column(length = 20)
    private String rewardType;

    private LocalDateTime createTime;
    private LocalDateTime settleTime;
}
```

- [ ] **Step 4: Create repositories**

`GridOrganizationRepository.java`:
```java
package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.GridOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GridOrganizationRepository extends JpaRepository<GridOrganization, Integer>,
        JpaSpecificationExecutor<GridOrganization> {
    List<GridOrganization> findByAuditStatus(String auditStatus);
}
```

`GridAgentRepository.java`:
```java
package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.GridAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GridAgentRepository extends JpaRepository<GridAgent, Integer>,
        JpaSpecificationExecutor<GridAgent> {
    Optional<GridAgent> findByReferralCode(String referralCode);
}
```

`ReferralRecordRepository.java`:
```java
package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.ReferralRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRecordRepository extends JpaRepository<ReferralRecord, Long> {
    Optional<ReferralRecord> findByReferralCodeAndReferredId(String referralCode, Long referredId);
    List<ReferralRecord> findByReferrerId(Long referrerId);
    List<ReferralRecord> findByRewardStatus(String rewardStatus);
}
```

- [ ] **Step 5: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 8: Extend GridUser entity and repository

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/domain/GridUser.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/RegisterDTO.java` (overwrite - add referralCode field)
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/AppUserDTO.java` (overwrite - add userType, orgRole, region)

**Interfaces:**
- Produces: Extended GridUser with new fields, updated DTOs

- [ ] **Step 1: Extend `GridUser.java` with new fields**

Add after the `gender` field:

```java
@Column(length = 20)
private String userType = "NORMAL";

private Integer orgId;

@Column(length = 20)
private String orgRole;

private Integer agentId;

@Column(length = 32)
private String referralCode;

@Column(length = 32)
private String referredBy;

@Column(length = 10)
private String region;

@Column(length = 20)
private String registerAuditStatus = "APPROVED";
```

- [ ] **Step 2: Update `RegisterDTO.java`** (add referralCode field)

```java
@Data
public class RegisterDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private String deviceName;

    private String referralCode; // 新增：注册时选填推荐码
}
```

- [ ] **Step 3: Update `AppUserDTO.java`** (return additional user info)

```java
@Data
public class AppUserDTO {
    private Long id;
    private String email;
    private String nickname;
    private String avatar;
    private Integer gender;
    private List<String> roles;
    private String userType;    // 新增
    private String orgRole;     // 新增
    private String region;      // 新增
}
```

- [ ] **Step 4: Add repository method for referral code**

Add to `GridUserRepository.java`:

```java
Optional<GridUser> findByReferralCode(String referralCode);
boolean existsByReferralCode(String referralCode);
```

- [ ] **Step 5: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 9: Implement IP Region Resolver

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/RegionResolver.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/IpRegionResolver.java`

**Note:** The project already has `mica-ip2region` in the parent pom, so ip2region is available. Check if it needs a local db file in resources.

**Interfaces:**
- Produces: `RegionResolver.resolve(String ip)` → `String` (region code A/B/C/D/E)

- [ ] **Step 1: Create `RegionResolver.java` interface**

```java
package com.naon.grid.modules.app.service;

public interface RegionResolver {
    /**
     * 根据IP地址解析所属区域 A/B/C/D/E
     */
    String resolve(String ip);
}
```

- [ ] **Step 2: Create `IpRegionResolver.java`**

```java
package com.naon.grid.modules.app.service.impl;

import com.naon.grid.modules.app.service.RegionResolver;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.InputStream;

@Slf4j
@Service
public class IpRegionResolver implements RegionResolver {

    private Searcher searcher;

    @PostConstruct
    public void init() {
        try {
            // Try to load from classpath; if ip2region.xdb not found, fallback to no-op
            InputStream is = new ClassPathResource("ip2region.xdb").getInputStream();
            byte[] cBuff = new byte[is.available()];
            is.read(cBuff);
            is.close();
            searcher = Searcher.newWithBuffer(cBuff);
            log.info("IP2Region searcher initialized successfully");
        } catch (Exception e) {
            log.warn("IP2Region db not found at classpath:ip2region.xdb, using default region C. Error: {}", e.getMessage());
            searcher = null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (Exception e) {
                log.warn("Error closing IP2Region searcher", e);
            }
        }
    }

    @Override
    public String resolve(String ip) {
        if (searcher == null) {
            return "C"; // Default to China
        }

        try {
            String result = searcher.search(ip);
            // ip2region returns format: "中国|华东|上海市|联通"
            if (result != null) {
                return mapToRegion(result);
            }
        } catch (Exception e) {
            log.debug("IP region lookup failed for IP: {}", ip);
        }
        return "C"; // Default fallback
    }

    /**
     * Map ip2region result to our region code A/B/C/D/E
     * Based on country/region mapping rules from the pricing spec.
     */
    String mapToRegion(String raw) {
        if (raw == null || raw.isEmpty()) return "C";

        String[] parts = raw.split("\\|");
        String country = parts.length > 0 ? parts[0] : "";
        String province = parts.length > 2 ? parts[2] : "";

        // C区: 中国大陆
        if ("中国".equals(country)) return "C";

        // A区: 北美、西欧、北欧
        if (containsAny(country, "美国", "加拿大", "英国", "德国", "法国", "意大利", "西班牙",
                "荷兰", "比利时", "瑞士", "瑞典", "挪威", "丹麦", "芬兰", "爱尔兰", "奥地利",
                "葡萄牙", "希腊", "卢森堡", "冰岛")) return "A";

        // B区: 日韩澳新、新加坡及港澳台
        if (containsAny(country, "日本", "韩国", "澳大利亚", "新西兰", "新加坡")) return "B";
        if (containsAny(country, "香港", "澳门", "台湾")) return "B";
        if (containsAny(province, "香港", "澳门", "台湾")) return "B";

        // 中东高收入国家 → B区
        if (containsAny(country, "沙特阿拉伯", "阿联酋", "卡塔尔", "科威特", "阿曼", "巴林")) return "B";

        // D区: 东南亚(除新加坡)、东欧、拉美
        if (containsAny(country, "泰国", "越南", "印度尼西亚", "马来西亚", "菲律宾", "缅甸",
                "柬埔寨", "老挝", "文莱", "东帝汶", "波兰", "捷克", "匈牙利", "罗马尼亚",
                "乌克兰", "俄罗斯", "巴西", "墨西哥", "阿根廷", "智利", "哥伦比亚",
                "秘鲁", "委内瑞拉")) return "D";

        // E区: 非洲、南亚、中亚
        if (containsAny(country, "印度", "巴基斯坦", "孟加拉国", "斯里兰卡", "尼泊尔",
                "南非", "尼日利亚", "肯尼亚", "埃及", "埃塞俄比亚", "坦桑尼亚", "加纳",
                "安哥拉", "乌干达", "哈萨克斯坦", "乌兹别克斯坦")) return "E";

        // Default to A for unrecognized high-income countries
        return "A";
    }

    private boolean containsAny(String text, String... values) {
        if (text == null) return false;
        for (String v : values) {
            if (text.contains(v)) return true;
        }
        return false;
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 10: Create grid-app services (OrganizationService, AgentService, ReferralService)

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/OrganizationService.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/OrganizationServiceImpl.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/AgentService.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AgentServiceImpl.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/ReferralService.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/ReferralServiceImpl.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/InstitutionRegisterDTO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/AgentRegisterDTO.java`

**Interfaces:**
- Produces: Complete service layer for institution/agent/referral management

- [ ] **Step 1: Create `InstitutionRegisterDTO.java`**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class InstitutionRegisterDTO {
    @NotBlank(message = "机构名称不能为空")
    private String name;

    private String nameEn;

    @NotBlank(message = "机构类型不能为空")
    private String orgType;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @NotBlank(message = "联系邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String contactEmail;

    @NotBlank(message = "管理员邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String adminEmail;

    @NotBlank(message = "管理员密码不能为空")
    private String adminPassword;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private String deviceName;
}
```

- [ ] **Step 2: Create `AgentRegisterDTO.java`**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class AgentRegisterDTO {
    @NotBlank(message = "代理商名称不能为空")
    private String name;

    private String contactName;

    @Email(message = "邮箱格式不正确")
    private String contactEmail;

    @NotBlank(message = "管理员邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String adminEmail;

    @NotBlank(message = "管理员密码不能为空")
    private String adminPassword;

    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    private String deviceName;
}
```

- [ ] **Step 3: Create `OrganizationService.java`**

```java
package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.domain.GridOrganization;
import com.naon.grid.modules.app.service.dto.InstitutionRegisterDTO;
import com.naon.grid.modules.app.service.dto.TokenDTO;
import javax.servlet.http.HttpServletRequest;

public interface OrganizationService {
    /**
     * 机构自助注册
     */
    TokenDTO register(InstitutionRegisterDTO dto, HttpServletRequest request);

    /**
     * 审核机构（后台管理员操作）
     */
    void approve(Integer orgId, String planProductCode);

    /**
     * 驳回机构
     */
    void reject(Integer orgId, String reason);

    /**
     * 根据ID查询
     */
    GridOrganization findById(Integer orgId);
}
```

- [ ] **Step 4: Create `OrganizationServiceImpl.java`**

```java
package com.naon.grid.modules.app.service.impl;

import com.naon.grid.config.properties.RsaProperties;
import com.naon.grid.enums.BillingCycleEnum;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.GridOrganization;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.GridUserRole;
import com.naon.grid.modules.app.repository.GridOrganizationRepository;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.repository.GridUserRoleRepository;
import com.naon.grid.modules.app.security.AppTokenProvider;
import com.naon.grid.modules.app.security.DeviceManager;
import com.naon.grid.modules.app.service.OrganizationService;
import com.naon.grid.modules.app.service.RegionResolver;
import com.naon.grid.modules.app.service.dto.AppUserDTO;
import com.naon.grid.modules.app.service.dto.InstitutionRegisterDTO;
import com.naon.grid.modules.app.service.dto.TokenDTO;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import com.naon.grid.utils.RsaUtils;
import com.naon.grid.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {
    private final GridOrganizationRepository organizationRepository;
    private final GridUserRepository userRepository;
    private final GridUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppTokenProvider appTokenProvider;
    private final DeviceManager deviceManager;
    private final RegionResolver regionResolver;
    private final EntitlementEngine entitlementEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO register(InstitutionRegisterDTO dto, HttpServletRequest request) {
        if (userRepository.existsByEmail(dto.getAdminEmail())) {
            throw new BadRequestException("管理员邮箱已被注册");
        }

        String ip = StringUtils.getIp(request);
        String region = regionResolver.resolve(ip);

        // Create organization (PENDING audit)
        GridOrganization org = new GridOrganization();
        org.setName(dto.getName());
        org.setNameEn(dto.getNameEn());
        org.setOrgType(dto.getOrgType());
        org.setContactName(dto.getContactName());
        org.setContactEmail(dto.getContactEmail());
        org.setRegion(region);
        org.setAuditStatus("PENDING");
        org.setCreateTime(LocalDateTime.now());
        org.setUpdateTime(LocalDateTime.now());
        organizationRepository.save(org);

        // Create admin user
        String decryptedPassword;
        try {
            decryptedPassword = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, dto.getAdminPassword());
        } catch (Exception e) {
            throw new BadRequestException("密码解密失败");
        }

        GridUser user = new GridUser();
        user.setEmail(dto.getAdminEmail());
        user.setPassword(passwordEncoder.encode(decryptedPassword));
        user.setNickname(dto.getContactName());
        user.setGender(0);
        user.setStatus(1);
        user.setUserType("INSTITUTION");
        user.setOrgId(org.getId());
        user.setOrgRole("ADMIN");
        user.setRegisterAuditStatus("PENDING");
        user.setRegion(region);
        user.setRegisterIp(ip);
        userRepository.save(user);

        // Create NORMAL role
        GridUserRole normalRole = new GridUserRole();
        normalRole.setUserId(user.getId());
        normalRole.setRoleCode("NORMAL");
        normalRole.setRoleName("普通用户");
        userRoleRepository.save(normalRole);

        // Generate token
        return generateToken(user, dto.getDeviceId(), dto.getDeviceName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Integer orgId, String planProductCode) {
        GridOrganization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("机构不存在"));
        org.setAuditStatus("APPROVED");
        // Set default limits based on plan product code
        if ("INST_STARTER".equals(planProductCode)) {
            org.setMaxMembers(30);
            org.setMaxAdmins(1);
        } else if ("INST_BASIC".equals(planProductCode)) {
            org.setMaxMembers(100);
            org.setMaxAdmins(2);
        } else if ("INST_PRO".equals(planProductCode)) {
            org.setMaxMembers(500);
            org.setMaxAdmins(5);
        } else {
            org.setMaxMembers(30);
            org.setMaxAdmins(1);
        }
        org.setCurrentMembers(1); // Admin only for now
        org.setUpdateTime(LocalDateTime.now());
        organizationRepository.save(org);

        // Activate admin user
        userRepository.findByEmail(org.getContactEmail()).ifPresent(admin -> {
            admin.setRegisterAuditStatus("APPROVED");
            userRepository.save(admin);

            // Grant 30-day trial
            entitlementEngine.grant(
                    admin.getId(), "INSTITUTION", String.valueOf(orgId),
                    "PLUS", 30, org.getRegion()
            );
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Integer orgId, String reason) {
        GridOrganization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("机构不存在"));
        org.setAuditStatus("REJECTED");
        org.setUpdateTime(LocalDateTime.now());
        organizationRepository.save(org);
        log.info("Organization rejected: orgId={}, reason={}", orgId, reason);
    }

    @Override
    public GridOrganization findById(Integer orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("机构不存在"));
    }

    private TokenDTO generateToken(GridUser user, String deviceId, String deviceName) {
        cn.hutool.core.util.IdUtil idUtil = null; // Placeholder — will be resolved by cn.hutool.core.util.IdUtil
        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(GridUserRole::getRoleCode)
                .collect(Collectors.toList());

        Integer orgId = user.getOrgId();
        String accessToken = appTokenProvider.createToken(
                user.getId(), user.getEmail(), deviceId, roles,
                user.getUserType(),
                orgId != null ? orgId.intValue() : null,
                user.getOrgRole(),
                user.getRegion());

        String refreshToken = cn.hutool.core.util.IdUtil.simpleUUID();
        java.util.Date expireTime = new java.util.Date(System.currentTimeMillis() + 2592000L * 1000);
        deviceManager.registerDevice(user.getId(), deviceId, deviceName, refreshToken, accessToken, expireTime);

        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setAccessToken(accessToken);
        tokenDTO.setRefreshToken(refreshToken);
        tokenDTO.setExpiresIn(604800L);
        AppUserDTO userDTO = new AppUserDTO();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setNickname(user.getNickname());
        userDTO.setAvatar(user.getAvatar());
        userDTO.setGender(user.getGender());
        userDTO.setRoles(roles);
        userDTO.setUserType(user.getUserType());
        userDTO.setOrgRole(user.getOrgRole());
        userDTO.setRegion(user.getRegion());
        tokenDTO.setUser(userDTO);
        return tokenDTO;
    }
}
```

- [ ] **Step 5: Create `AgentService.java` interface**

```java
package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.service.dto.AgentRegisterDTO;
import com.naon.grid.modules.app.service.dto.TokenDTO;
import javax.servlet.http.HttpServletRequest;

public interface AgentService {
    TokenDTO register(AgentRegisterDTO dto, HttpServletRequest request);
    void approve(Integer agentId);
    void reject(Integer agentId, String reason);
}
```

- [ ] **Step 6: Create `AgentServiceImpl.java`**

```java
package com.naon.grid.modules.app.service.impl;

import cn.hutool.core.util.IdUtil;
import com.naon.grid.config.properties.RsaProperties;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.GridAgent;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.GridUserRole;
import com.naon.grid.modules.app.repository.GridAgentRepository;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.repository.GridUserRoleRepository;
import com.naon.grid.modules.app.security.AppTokenProvider;
import com.naon.grid.modules.app.security.DeviceManager;
import com.naon.grid.modules.app.service.AgentService;
import com.naon.grid.modules.app.service.RegionResolver;
import com.naon.grid.modules.app.service.dto.AgentRegisterDTO;
import com.naon.grid.modules.app.service.dto.AppUserDTO;
import com.naon.grid.modules.app.service.dto.TokenDTO;
import com.naon.grid.utils.RsaUtils;
import com.naon.grid.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final GridAgentRepository agentRepository;
    private final GridUserRepository userRepository;
    private final GridUserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppTokenProvider appTokenProvider;
    private final DeviceManager deviceManager;
    private final RegionResolver regionResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO register(AgentRegisterDTO dto, HttpServletRequest request) {
        if (userRepository.existsByEmail(dto.getAdminEmail())) {
            throw new BadRequestException("管理员邮箱已被注册");
        }

        String ip = StringUtils.getIp(request);
        String region = regionResolver.resolve(ip);

        // Generate unique referral code for agent
        String referralCode;
        do {
            referralCode = "AG" + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
        } while (agentRepository.findByReferralCode(referralCode).isPresent());

        // Create agent (PENDING audit)
        GridAgent agent = new GridAgent();
        agent.setName(dto.getName());
        agent.setContactName(dto.getContactName());
        agent.setContactEmail(dto.getContactEmail());
        agent.setReferralCode(referralCode);
        agent.setCommissionRate(java.math.BigDecimal.ZERO);
        agent.setAuditStatus("PENDING");
        agentRepository.save(agent);

        // Create admin user
        String decryptedPassword;
        try {
            decryptedPassword = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, dto.getAdminPassword());
        } catch (Exception e) {
            throw new BadRequestException("密码解密失败");
        }

        GridUser user = new GridUser();
        user.setEmail(dto.getAdminEmail());
        user.setPassword(passwordEncoder.encode(decryptedPassword));
        user.setNickname(dto.getContactName() != null ? dto.getContactName() : dto.getAdminEmail().split("@")[0]);
        user.setGender(0);
        user.setStatus(1);
        user.setUserType("AGENT");
        user.setAgentId(agent.getId());
        user.setRegion(region);
        user.setRegisterIp(ip);
        user.setRegisterAuditStatus("PENDING");
        userRepository.save(user);

        // Create NORMAL role
        GridUserRole normalRole = new GridUserRole();
        normalRole.setUserId(user.getId());
        normalRole.setRoleCode("NORMAL");
        normalRole.setRoleName("普通用户");
        userRoleRepository.save(normalRole);

        return generateToken(user, dto.getDeviceId(), dto.getDeviceName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Integer agentId) {
        GridAgent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new BadRequestException("代理商不存在"));
        agent.setAuditStatus("APPROVED");
        agentRepository.save(agent);
        log.info("Agent approved: agentId={}", agentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Integer agentId, String reason) {
        GridAgent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new BadRequestException("代理商不存在"));
        agent.setAuditStatus("REJECTED");
        agentRepository.save(agent);
        log.info("Agent rejected: agentId={}, reason={}", agentId, reason);
    }

    private TokenDTO generateToken(GridUser user, String deviceId, String deviceName) {
        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(GridUserRole::getRoleCode)
                .collect(Collectors.toList());

        String accessToken = appTokenProvider.createToken(
                user.getId(), user.getEmail(), deviceId, roles,
                user.getUserType(), null, user.getOrgRole(), user.getRegion());

        String refreshToken = IdUtil.simpleUUID();
        java.util.Date expireTime = new java.util.Date(System.currentTimeMillis() + 2592000L * 1000);
        deviceManager.registerDevice(user.getId(), deviceId, deviceName, refreshToken, accessToken, expireTime);

        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setAccessToken(accessToken);
        tokenDTO.setRefreshToken(refreshToken);
        tokenDTO.setExpiresIn(604800L);
        AppUserDTO userDTO = new AppUserDTO();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setNickname(user.getNickname());
        userDTO.setAvatar(user.getAvatar());
        userDTO.setGender(user.getGender());
        userDTO.setRoles(roles);
        userDTO.setUserType(user.getUserType());
        userDTO.setOrgRole(user.getOrgRole());
        userDTO.setRegion(user.getRegion());
        tokenDTO.setUser(userDTO);
        return tokenDTO;
    }
}
```

- [ ] **Step 7: Create `ReferralService.java`**

```java
package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.domain.ReferralRecord;

public interface ReferralService {
    /**
     * 处理推荐码：注册时调用，记录推荐关系
     * @return 推荐人ID（如果推荐码有效），null表示无需关联
     */
    Long processReferral(String referralCode, Long referredUserId);

    /**
     * 被推荐用户支付成功时调用，发放推荐奖励
     */
    void settleReferralReward(Long referredUserId, String orderNo);
}
```

- [ ] **Step 8: Create `ReferralServiceImpl.java`**

```java
package com.naon.grid.modules.app.service.impl;

import com.naon.grid.modules.app.domain.GridAgent;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.ReferralRecord;
import com.naon.grid.modules.app.repository.GridAgentRepository;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.repository.ReferralRecordRepository;
import com.naon.grid.modules.app.service.ReferralService;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {
    private final GridUserRepository userRepository;
    private final GridAgentRepository agentRepository;
    private final ReferralRecordRepository referralRecordRepository;
    private final GridOrderRepository orderRepository;
    private final EntitlementEngine entitlementEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long processReferral(String referralCode, Long referredUserId) {
        if (referralCode == null || referralCode.isEmpty()) return null;

        // Check if it belongs to an agent
        Optional<GridAgent> agentOpt = agentRepository.findByReferralCode(referralCode);
        if (agentOpt.isPresent() && "APPROVED".equals(agentOpt.get().getAuditStatus())) {
            GridAgent agent = agentOpt.get();
            ReferralRecord record = new ReferralRecord();
            record.setReferrerId(agent.getId().longValue());
            record.setReferrerType("AGENT");
            record.setReferredId(referredUserId);
            record.setReferralCode(referralCode);
            record.setRewardStatus("PENDING");
            record.setRewardType("CASH");
            record.setCreateTime(LocalDateTime.now());
            referralRecordRepository.save(record);
            return agent.getId().longValue();
        }

        // Check if it belongs to a user
        Optional<GridUser> userOpt = userRepository.findByReferralCode(referralCode);
        if (userOpt.isPresent() && !userOpt.get().getId().equals(referredUserId)) {
            GridUser referrer = userOpt.get();
            ReferralRecord record = new ReferralRecord();
            record.setReferrerId(referrer.getId());
            record.setReferrerType("NORMAL");
            record.setReferredId(referredUserId);
            record.setReferralCode(referralCode);
            record.setRewardStatus("PENDING");
            record.setRewardType("EXTEND_DAYS");
            record.setCreateTime(LocalDateTime.now());
            referralRecordRepository.save(record);
            return referrer.getId();
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleReferralReward(Long referredUserId, String orderNo) {
        Optional<ReferralRecord> recordOpt = referralRecordRepository
                .findByReferralCodeAndReferredId(null, referredUserId);
        // Simplified: In production, find by order + referral relationship
        recordOpt.ifPresent(record -> {
            GridOrder order = orderRepository.findByOrderNo(orderNo).orElse(null);
            if (order == null || !"PENDING".equals(record.getRewardStatus())) return;

            if ("NORMAL".equals(record.getReferrerType())) {
                // Grant 30 days PLUS
                entitlementEngine.grant(record.getReferrerId(), "REFERRAL",
                        String.valueOf(record.getId()), "PLUS", 30, null);
                record.setRewardAmount(BigDecimal.valueOf(30));
            } else if ("AGENT".equals(record.getReferrerType())) {
                // Calculate commission
                // Phase 1: simplified, actual commission calc later
                record.setRewardAmount(BigDecimal.ZERO);
            }
            record.setOrderId(order.getId());
            record.setRewardStatus("SETTLED");
            record.setSettleTime(LocalDateTime.now());
            referralRecordRepository.save(record);
            log.info("Referral reward settled: recordId={}, referrerId={}, type={}",
                    record.getId(), record.getReferrerId(), record.getReferrerType());
        });
    }
}
```

- [ ] **Step 9: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 11: Extend App auth components (JWT + AppAuthenticationToken + Filter)

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/security/AppTokenProvider.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/security/AppAuthenticationToken.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/security/AppTokenFilter.java`

- [ ] **Step 1: Extend `AppTokenProvider.java`**

Add new claim constants and new createToken overload:

```java
// New constants
public static final String USER_TYPE_KEY = "userType";
public static final String ORG_ID_KEY = "orgId";
public static final String ORG_ROLE_KEY = "orgRole";
public static final String REGION_KEY = "region";

// New overloaded createToken
public String createToken(Long userId, String username, String deviceId,
                           List<String> roles, String userType,
                           Integer orgId, String orgRole, String region) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(AUTHORITIES_UID_KEY, userId);
    claims.put(DEVICE_ID_KEY, deviceId);
    claims.put(ROLES_KEY, roles);
    claims.put(TOKEN_TYPE_KEY, TOKEN_TYPE_ACCESS);
    claims.put(USER_TYPE_KEY, userType);
    if (orgId != null) claims.put(ORG_ID_KEY, orgId);
    if (orgRole != null) claims.put(ORG_ROLE_KEY, orgRole);
    if (region != null) claims.put(REGION_KEY, region);

    long now = System.currentTimeMillis();
    Date validity = new Date(now + tokenExpireSeconds * 1000);

    return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date(now))
            .setExpiration(validity)
            .signWith(signingKey, SignatureAlgorithm.HS512)
            .compact();
}
```

- [ ] **Step 2: Extend `AppAuthenticationToken.java`**

Add fields:

```java
private final String userType;
private final Integer orgId;
private final String orgRole;
private final String region;

public AppAuthenticationToken(Long userId, String username, String deviceId,
                               List<String> roles, String userType,
                               Integer orgId, String orgRole, String region) {
    super(roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList()));
    this.userId = userId;
    this.username = username;
    this.deviceId = deviceId;
    this.roles = roles;
    this.userType = userType;
    this.orgId = orgId;
    this.orgRole = orgRole;
    this.region = region;
    setAuthenticated(true);
}
```

Add getters for all new fields.

- [ ] **Step 3: Extend `AppTokenFilter.java`**

In the `doFilter` method, extract new claims:

```java
Long userId = appTokenProvider.getUserIdFromToken(token);
String username = appTokenProvider.getClaims(token).getSubject();
String deviceId = appTokenProvider.getDeviceIdFromToken(token);
List<String> roles = appTokenProvider.getRolesFromToken(token);
Claims claims = appTokenProvider.getClaims(token);
String userType = claims.get(AppTokenProvider.USER_TYPE_KEY, String.class);
Integer orgId = claims.get(AppTokenProvider.ORG_ID_KEY, Integer.class);
String orgRole = claims.get(AppTokenProvider.ORG_ROLE_KEY, String.class);
String region = claims.get(AppTokenProvider.REGION_KEY, String.class);

AppAuthenticationToken authentication = new AppAuthenticationToken(
    userId, username, deviceId, roles, userType, orgId, orgRole, region);
```

- [ ] **Step 4: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 12: Create @RequireProduct annotation + ProductAccessAspect

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/annotation/RequireProduct.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/enums/RequireOrgRole.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/aspect/ProductAccessAspect.java`

- [ ] **Step 1: Create `RequireOrgRole.java`**

```java
package com.naon.grid.modules.app.annotation;

public enum RequireOrgRole {
    NONE,
    ADMIN,
    MEMBER
}
```

- [ ] **Step 2: Create `RequireProduct.java`**

```java
package com.naon.grid.modules.app.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireProduct {
    String[] value() default {};           // 产品代码，如 "VOCAB"、"PLUS"
    RequireOrgRole orgRole() default RequireOrgRole.NONE;
}
```

- [ ] **Step 3: Create `ProductAccessAspect.java`**

```java
package com.naon.grid.modules.app.aspect;

import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.annotation.RequireOrgRole;
import com.naon.grid.modules.app.annotation.RequireProduct;
import com.naon.grid.modules.app.enums.AppErrorCode;
import com.naon.grid.modules.app.security.AppAuthenticationToken;
import com.naon.grid.modules.billing.service.EntitlementEngine;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ProductAccessAspect {

    private final EntitlementEngine entitlementEngine;

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

        // Get region from request attribute (set by RegionInterceptor)
        String currentRegion = "C";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String regionAttr = (String) request.getAttribute("_region");
            if (regionAttr != null) currentRegion = regionAttr;
        }

        // Region validation (Phase 1: warn only)
        entitlementEngine.isValidForRegion(userId, currentRegion);

        // Get annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireProduct annotation = AnnotationUtils.findAnnotation(method, RequireProduct.class);
        if (annotation == null) return joinPoint.proceed();

        // Check product access
        String[] requiredProducts = annotation.value();
        if (requiredProducts.length > 0) {
            boolean hasAccess = false;
            for (String productCode : requiredProducts) {
                if (entitlementEngine.hasAccess(userId, productCode)) {
                    hasAccess = true;
                    break;
                }
            }
            if (!hasAccess) {
                log.warn("Product access denied: userId={}, required={}", userId, requiredProducts);
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

Note: Check if `AppErrorCode.FORBIDDEN` exists. If not, add it or use `AppErrorCode.SUBSCRIPTION_REQUIRED` for now.

- [ ] **Step 4: Add FORBIDDEN to AppErrorCode if missing**

Open `grid-app/src/main/java/com/naon/grid/modules/app/enums/AppErrorCode.java` and check. If FORBIDDEN doesn't exist, add:

```java
FORBIDDEN(1403, "没有权限"),
```

- [ ] **Step 5: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 13: Create RegionInterceptor and register in AppSecurityConfig

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/interceptor/RegionInterceptor.java`
- Create/Modify: `grid-app/src/main/java/com/naon/grid/modules/app/config/AppWebMvcConfig.java`

- [ ] **Step 1: Create `RegionInterceptor.java`**

```java
package com.naon.grid.modules.app.interceptor;

import com.naon.grid.modules.app.service.RegionResolver;
import com.naon.grid.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegionInterceptor implements HandlerInterceptor {

    private final RegionResolver regionResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = StringUtils.getIp(request);
        String region = regionResolver.resolve(ip);
        request.setAttribute("_region", region);
        log.debug("Region resolved: ip={}, region={}", ip, region);
        return true;
    }
}
```

- [ ] **Step 2: Create `AppWebMvcConfig.java`**

```java
package com.naon.grid.modules.app.config;

import com.naon.grid.modules.app.interceptor.RegionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AppWebMvcConfig implements WebMvcConfigurer {

    private final RegionInterceptor regionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(regionInterceptor)
                .addPathPatterns("/api/app/**");
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 14: Create grid-app REST controllers

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppInstitutionController.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppAgentController.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppOrderController.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppReferralController.java`

- [ ] **Step 1: Create `AppInstitutionController.java`**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.modules.app.service.OrganizationService;
import com.naon.grid.modules.app.service.dto.InstitutionRegisterDTO;
import com.naon.grid.modules.app.service.dto.TokenDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/institution")
@Api(tags = "用户：机构注册")
public class AppInstitutionController {

    private final OrganizationService organizationService;

    @ApiOperation("机构自助注册")
    @AnonymousPostMapping("/register")
    public ResponseEntity<TokenDTO> register(@Validated @RequestBody InstitutionRegisterDTO dto,
                                              HttpServletRequest request) {
        TokenDTO tokenDTO = organizationService.register(dto, request);
        return ResponseEntity.ok(tokenDTO);
    }
}
```

- [ ] **Step 2: Create `AppAgentController.java`**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.modules.app.service.AgentService;
import com.naon.grid.modules.app.service.dto.AgentRegisterDTO;
import com.naon.grid.modules.app.service.dto.TokenDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/agent")
@Api(tags = "用户：代理商注册")
public class AppAgentController {

    private final AgentService agentService;

    @ApiOperation("代理商自助注册")
    @AnonymousPostMapping("/register")
    public ResponseEntity<TokenDTO> register(@Validated @RequestBody AgentRegisterDTO dto,
                                              HttpServletRequest request) {
        TokenDTO tokenDTO = agentService.register(dto, request);
        return ResponseEntity.ok(tokenDTO);
    }
}
```

- [ ] **Step 3: Create `AppOrderController.java`**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.modules.app.security.AppAuthenticationToken;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.modules.billing.service.OrderService;
import com.naon.grid.modules.billing.service.PaymentService;
import com.naon.grid.modules.billing.service.dto.OrderCreateRequest;
import com.naon.grid.modules.billing.service.dto.OrderCreateResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/orders")
@Api(tags = "用户：订单接口")
public class AppOrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @ApiOperation("创建订单")
    @PostMapping("/create")
    public ResponseEntity<OrderCreateResponse> createOrder(
            @Validated @RequestBody OrderCreateRequest request,
            HttpServletRequest servletRequest) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        String region = (String) servletRequest.getAttribute("_region");
        if (region == null) region = "C";
        OrderCreateResponse response = orderService.createOrder(userId, request, region);
        return ResponseEntity.ok(response);
    }

    @ApiOperation("支付回调（模拟）")
    @AnonymousPostMapping("/callback")
    public ResponseEntity<String> paymentCallback(@RequestBody Map<String, Object> callbackData) {
        String orderNo = (String) callbackData.get("order_no");
        String paymentMethod = (String) callbackData.get("payment_method");
        if (orderNo == null || paymentMethod == null) {
            return ResponseEntity.badRequest().body("Missing order_no or payment_method");
        }
        boolean success = paymentService.handlePaymentCallback(orderNo, paymentMethod, callbackData);
        return success ? ResponseEntity.ok("SUCCESS") : ResponseEntity.badRequest().body("FAILED");
    }
}
```

- [ ] **Step 4: Create `AppReferralController.java`**

```java
package com.naon.grid.modules.app.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/referral")
@Api(tags = "用户：推荐接口")
public class AppReferralController {

    // Phase 1: simple health check, full referral API in Phase 2
    @ApiOperation("推荐信息")
    @GetMapping("/info")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok("Referral system active");
    }
}
```

- [ ] **Step 5: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 15: Modify AppAuthServiceImpl for referral code and user type

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java`

- [ ] **Step 1: Modify `AppAuthServiceImpl.java`**

Inject new dependencies:

```java
private final EntitlementEngine entitlementEngine;
private final ReferralService referralService;
private final RegionResolver regionResolver;
```

Modify the `register` method:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public TokenDTO register(RegisterDTO registerDTO, HttpServletRequest request) {
    if (userRepository.existsByEmail(registerDTO.getEmail())) {
        throw new BadRequestException("邮箱已被注册");
    }

    String decryptedPassword;
    try {
        decryptedPassword = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, registerDTO.getPassword());
    } catch (Exception e) {
        throw new BadRequestException("密码解密失败");
    }

    String ip = StringUtils.getIp(request);
    String region = regionResolver.resolve(ip);

    GridUser user = new GridUser();
    user.setEmail(registerDTO.getEmail());
    user.setPassword(passwordEncoder.encode(decryptedPassword));
    user.setNickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : registerDTO.getEmail().split("@")[0]);
    user.setGender(0);
    user.setStatus(1);
    user.setUserType("NORMAL");
    user.setRegion(region);
    user.setRegisterIp(ip);
    user.setRegisterAuditStatus("APPROVED");

    // Generate referral code
    user.setReferralCode(generateReferralCode(userRepository));
    userRepository.save(user);

    GridUserRole normalRole = new GridUserRole();
    normalRole.setUserId(user.getId());
    normalRole.setRoleCode("NORMAL");
    normalRole.setRoleName("普通用户");
    userRoleRepository.save(normalRole);

    // Process referral code if provided
    String referralCode = registerDTO.getReferralCode();
    if (referralCode != null && !referralCode.isEmpty()) {
        user.setReferredBy(referralCode);
        referralService.processReferral(referralCode, user.getId());
    }

    // Grant trial
    try {
        entitlementEngine.grant(user.getId(), "TRIAL", null, "PLUS", 7, region);
    } catch (Exception e) {
        log.error("Failed to grant trial for userId={}", user.getId(), e);
    }

    return generateToken(user, registerDTO.getDeviceId(), registerDTO.getDeviceName());
}
```

Update `generateToken` to pass new claims:

```java
private TokenDTO generateToken(GridUser user, String deviceId, String deviceName) {
    List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
            .map(GridUserRole::getRoleCode)
            .collect(Collectors.toList());

    Integer orgId = user.getOrgId();
    String accessToken = appTokenProvider.createToken(
            user.getId(), user.getEmail(), deviceId, roles,
            user.getUserType(),
            orgId != null ? orgId.intValue() : null,
            user.getOrgRole(),
            user.getRegion());
    // ... rest same
}
```

Add the `generateReferralCode` helper:

```java
private String generateReferralCode(GridUserRepository userRepository) {
    String code;
    do {
        String random = IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
        code = "UR" + random;
    } while (userRepository.existsByReferralCode(code));
    return code;
}
```

Update `convertToDTO`:

```java
private AppUserDTO convertToDTO(GridUser user, List<String> roles) {
    AppUserDTO dto = new AppUserDTO();
    dto.setId(user.getId());
    dto.setEmail(user.getEmail());
    dto.setNickname(user.getNickname());
    dto.setAvatar(user.getAvatar());
    dto.setGender(user.getGender());
    dto.setRoles(roles);
    dto.setUserType(user.getUserType());
    dto.setOrgRole(user.getOrgRole());
    dto.setRegion(user.getRegion());
    return dto;
}
```

- [ ] **Step 2: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 16: Create grid-system admin controllers

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/rest/ProductController.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/rest/InstitutionAuditController.java`
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/rest/AgentAuditController.java`

- [ ] **Step 1: Create `ProductController.java`**

```java
package com.naon.grid.modules.system.rest;

import com.naon.grid.modules.billing.domain.GridProduct;
import com.naon.grid.modules.billing.domain.RegionPricing;
import com.naon.grid.modules.billing.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Api(tags = "系统：产品管理")
public class ProductController {

    private final ProductService productService;

    @ApiOperation("获取所有产品列表")
    @GetMapping
    public ResponseEntity<List<GridProduct>> getAllProducts() {
        return ResponseEntity.ok(productService.findAllActive());
    }

    @ApiOperation("获取产品的区域定价")
    @GetMapping("/{code}/pricing")
    public ResponseEntity<?> getProductPricing(@PathVariable String code,
                                                @RequestParam(defaultValue = "C") String region) {
        return productService.findByCode(code)
                .map(product -> ResponseEntity.ok(
                        productService.getPricingByProductAndRegion(product.getId(), region)))
                .orElse(ResponseEntity.notFound().build());
    }
}
```

- [ ] **Step 2: Create `InstitutionAuditController.java`**

```java
package com.naon.grid.modules.system.rest;

import com.naon.grid.modules.app.domain.GridOrganization;
import com.naon.grid.modules.app.repository.GridOrganizationRepository;
import com.naon.grid.modules.app.service.OrganizationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/institutions")
@Api(tags = "系统：机构审核")
public class InstitutionAuditController {

    private final OrganizationService organizationService;
    private final GridOrganizationRepository organizationRepository;

    @ApiOperation("获取待审核机构列表")
    @GetMapping("/pending")
    public ResponseEntity<List<GridOrganization>> getPendingInstitutions() {
        return ResponseEntity.ok(organizationRepository.findByAuditStatus("PENDING"));
    }

    @ApiOperation("审核通过")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Integer id,
                                         @RequestParam String plan) {
        organizationService.approve(id, plan);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("审核驳回")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Integer id,
                                        @RequestParam String reason) {
        organizationService.reject(id, reason);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 3: Create `AgentAuditController.java`**

```java
package com.naon.grid.modules.system.rest;

import com.naon.grid.modules.app.domain.GridAgent;
import com.naon.grid.modules.app.repository.GridAgentRepository;
import com.naon.grid.modules.app.service.AgentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agents")
@Api(tags = "系统：代理商审核")
public class AgentAuditController {

    private final AgentService agentService;
    private final GridAgentRepository agentRepository;

    @ApiOperation("获取待审核代理商列表")
    @GetMapping("/pending")
    public ResponseEntity<List<GridAgent>> getPendingAgents() {
        List<GridAgent> all = agentRepository.findAll();
        // Filter for pending (simplified, could add repository method)
        return ResponseEntity.ok(all);
    }

    @ApiOperation("审核通过")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Integer id) {
        agentService.approve(id);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("审核驳回")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Integer id,
                                        @RequestParam String reason) {
        agentService.reject(id, reason);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 17: Extend AppSubscriptionController and handle backward compatibility

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppSubscriptionController.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/SubscriptionServiceImpl.java`

**Goal:** Update old subscription endpoint to work with new EntitlementEngine, maintain backward compat.

- [ ] **Step 1: Modify `AppSubscriptionController.java`**

Update the `getMySubscription` endpoint to use EntitlementEngine:

```java
private final EntitlementEngine entitlementEngine;

@ApiOperation("查询我的订阅状态")
@GetMapping("/my")
public ResponseEntity<AppSubscriptionVO> getMySubscription() {
    Long userId = AppSecurityUtils.getCurrentUserId();
    EntitlementResult result = entitlementEngine.compute(userId);
    
    AppSubscriptionVO vo = new AppSubscriptionVO();
    if (result.getOverallExpireAt() != null) {
        vo.setLevel("VIP");
        vo.setExpireTime(java.util.Date.from(
                result.getOverallExpireAt()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()));
        vo.setExpiringSoon(java.time.LocalDateTime.now()
                .plusDays(15).isAfter(result.getOverallExpireAt()));
    } else {
        vo.setLevel("NORMAL");
    }
    return ResponseEntity.ok(vo);
}
```

- [ ] **Step 2: Update `SubscriptionServiceImpl.java`**

Keep `grantTrial` backward compatible by delegating to EntitlementEngine:

```java
private final EntitlementEngine entitlementEngine;
private final RegionResolver regionResolver;

@Override
public void grantTrial(Long userId) {
    // Delegate to new engine
    String region = "C";
    // Try to get user's region
    userRepository.findById(userId).ifPresent(user -> {
        if (user.getRegion() != null) region = user.getRegion();
    });
    entitlementEngine.grant(userId, "TRIAL", null, "PLUS", trialDays, region);
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn clean compile -DskipTests`
Expected: BUILD SUCCESS

---

### Task 18: Integration verification and sample data

**Files:**
- Modify: None (runtime verification)

- [ ] **Step 1: Create `grid-billing/src/main/java/com/naon/grid/modules/billing/config/BillingJpaConfig.java`**

```java
package com.naon.grid.modules.billing.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.naon.grid.modules.billing.domain")
@EnableJpaRepositories("com.naon.grid.modules.billing.repository")
public class BillingJpaConfig {
}
```

- [ ] **Step 2: Add `billing.sql` and `app_ext.sql` execution to project setup**

Run the SQL scripts against the database:

```bash
mysql -u root -p < sql/billing.sql
mysql -u root -p < sql/app_ext.sql
```

- [ ] **Step 3: Verify full startup**

```bash
mvn clean compile -DskipTests
```

Run the application and check:
```bash
cd grid-bootstrap && mvn spring-boot:run
```

Expected: Application starts without errors, all new beans load correctly, no classpath issues.

- [ ] **Step 4: Test the registration flow**

Using curl or a REST client:
```bash
# 1. Get public key for password encryption
curl http://localhost:8000/api/app/auth/public-key

# 2. Register a normal user with referral code
curl -X POST http://localhost:8000/api/app/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"<encrypted>","deviceId":"test-device","referralCode":"URABC123"}'

# 3. Register an institution
curl -X POST http://localhost:8000/api/app/institution/register \
  -H "Content-Type: application/json" \
  -d '{"name":"测试大学","orgType":"UNIVERSITY","contactName":"张三","contactEmail":"contact@test.edu","adminEmail":"admin@test.edu","adminPassword":"<encrypted>","deviceId":"test-device"}'

# 4. Check subscription status
curl http://localhost:8000/api/app/subscription/my \
  -H "Authorization: Bearer <token>"

# 5. Create an order
curl -X POST http://localhost:8000/api/app/orders/create \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"productCode":"PLUS","billingCycle":"YEARLY"}'
```

---

### File Reference Summary

**New files created (total: ~35):**

| # | File | Task |
|---|------|------|
| 1 | `grid-billing/pom.xml` | 1 |
| 2 | `grid-common/.../enums/UserTypeEnum.java` | 2 |
| 3 | `grid-common/.../enums/RegionEnum.java` | 2 |
| 4 | `grid-common/.../enums/BillingCycleEnum.java` | 2 |
| 5 | `grid-common/.../enums/ProductTypeEnum.java` | 2 |
| 6 | `grid-common/.../enums/SourceTypeEnum.java` | 2 |
| 7 | `grid-common/.../enums/EntitlementStatusEnum.java` | 2 |
| 8 | `grid-common/.../enums/OrderStatusEnum.java` | 2 |
| 9 | `grid-common/.../enums/AuditStatusEnum.java` | 2 |
| 10 | `grid-common/.../enums/OrgRoleEnum.java` | 2 |
| 11 | `grid-billing/.../domain/GridProduct.java` | 3 |
| 12 | `grid-billing/.../domain/ProductModule.java` | 3 |
| 13 | `grid-billing/.../domain/RegionPricing.java` | 3 |
| 14 | `grid-billing/.../domain/EntitlementSource.java` | 3 |
| 15 | `grid-billing/.../domain/GridOrder.java` | 3 |
| 16 | `grid-billing/.../domain/PaymentRecord.java` | 3 |
| 17 | `grid-billing/.../repository/GridProductRepository.java` | 3 |
| 18 | `grid-billing/.../repository/ProductModuleRepository.java` | 3 |
| 19 | `grid-billing/.../repository/RegionPricingRepository.java` | 3 |
| 20 | `grid-billing/.../repository/EntitlementSourceRepository.java` | 3 |
| 21 | `grid-billing/.../repository/GridOrderRepository.java` | 3 |
| 22 | `grid-billing/.../repository/PaymentRecordRepository.java` | 3 |
| 23 | `sql/billing.sql` | 4 |
| 24 | `sql/app_ext.sql` | 4 |
| 25 | `grid-billing/.../service/EntitlementEngine.java` | 5 |
| 26 | `grid-billing/.../service/impl/EntitlementEngineImpl.java` | 5 |
| 27 | `grid-billing/.../service/dto/EntitlementResult.java` | 5 |
| 28 | `grid-billing/.../service/ProductService.java` | 6 |
| 29 | `grid-billing/.../service/impl/ProductServiceImpl.java` | 6 |
| 30 | `grid-billing/.../service/OrderService.java` | 6 |
| 31 | `grid-billing/.../service/impl/OrderServiceImpl.java` | 6 |
| 32 | `grid-billing/.../service/PaymentService.java` | 6 |
| 33 | `grid-billing/.../service/impl/PaymentServiceImpl.java` | 6 |
| 34 | `grid-billing/.../service/dto/OrderCreateRequest.java` | 6 |
| 35 | `grid-billing/.../service/dto/OrderCreateResponse.java` | 6 |
| 36 | `grid-billing/.../config/BillingJpaConfig.java` | 18 |
| 37 | `grid-app/.../domain/GridOrganization.java` | 7 |
| 38 | `grid-app/.../domain/GridAgent.java` | 7 |
| 39 | `grid-app/.../domain/ReferralRecord.java` | 7 |
| 40 | `grid-app/.../repository/GridOrganizationRepository.java` | 7 |
| 41 | `grid-app/.../repository/GridAgentRepository.java` | 7 |
| 42 | `grid-app/.../repository/ReferralRecordRepository.java` | 7 |
| 43 | `grid-app/.../service/RegionResolver.java` | 9 |
| 44 | `grid-app/.../service/impl/IpRegionResolver.java` | 9 |
| 45 | `grid-app/.../service/OrganizationService.java` | 10 |
| 46 | `grid-app/.../service/impl/OrganizationServiceImpl.java` | 10 |
| 47 | `grid-app/.../service/AgentService.java` | 10 |
| 48 | `grid-app/.../service/impl/AgentServiceImpl.java` | 10 |
| 49 | `grid-app/.../service/ReferralService.java` | 10 |
| 50 | `grid-app/.../service/impl/ReferralServiceImpl.java` | 10 |
| 51 | `grid-app/.../service/dto/InstitutionRegisterDTO.java` | 10 |
| 52 | `grid-app/.../service/dto/AgentRegisterDTO.java` | 10 |
| 53 | `grid-app/.../annotation/RequireProduct.java` | 12 |
| 54 | `grid-app/.../annotation/RequireOrgRole.java` | 12 |
| 55 | `grid-app/.../aspect/ProductAccessAspect.java` | 12 |
| 56 | `grid-app/.../interceptor/RegionInterceptor.java` | 13 |
| 57 | `grid-app/.../config/AppWebMvcConfig.java` | 13 |
| 58 | `grid-app/.../rest/AppInstitutionController.java` | 14 |
| 59 | `grid-app/.../rest/AppAgentController.java` | 14 |
| 60 | `grid-app/.../rest/AppOrderController.java` | 14 |
| 61 | `grid-app/.../rest/AppReferralController.java` | 14 |
| 62 | `grid-system/.../rest/ProductController.java` | 16 |
| 63 | `grid-system/.../rest/InstitutionAuditController.java` | 16 |
| 64 | `grid-system/.../rest/AgentAuditController.java` | 16 |

**Modified files (total: ~10):**

| # | File | Task |
|---|------|------|
| 1 | `pom.xml` (root, add module) | 1 |
| 2 | `grid-app/pom.xml` (add billing dep) | 1 |
| 3 | `grid-system/pom.xml` (add billing dep) | 1 |
| 4 | `AppRun.java` (add scanBasePackages) | 1 |
| 5 | `GridUser.java` (add fields) | 8 |
| 6 | `RegisterDTO.java` (add referralCode) | 8 |
| 7 | `AppUserDTO.java` (add user fields) | 8 |
| 8 | `GridUserRepository.java` (add methods) | 8 |
| 9 | `AppTokenProvider.java` (add claims) | 11 |
| 10 | `AppAuthenticationToken.java` (add fields) | 11 |
| 11 | `AppTokenFilter.java` (extract claims) | 11 |
| 12 | `AppAuthServiceImpl.java` (new flow) | 15 |
| 13 | `AppSubscriptionController.java` (use engine) | 17 |
| 14 | `SubscriptionServiceImpl.java` (delegate) | 17 |
| 15 | `AppErrorCode.java` (add FORBIDDEN) | 12 |
