# 邀请与奖励系统重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将邀请系统从「单条关系+实时结算」重构为「事件流水+每日批量结算」模型

**Architecture:** ReferralRecord 变为事件记录表（REGISTER/SUBSCRIBE），ReferralService 提供 recordEvent() 写入事件和 settlePendingRewards() 批量结算，每日凌晨 2:00 定时任务聚合 PENDING 事件并按奖励矩阵发放权益

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, MySQL, Lombok, Fastjson2

## Global Constraints

- Java 1.8，不使用 Java 9+ API
- 实体使用 Lombok `@Getter/@Setter`，不用 `@Data`
- 所有数据库操作在同一 `@Transactional` 内
- 不需要数据迁移（系统未上线）
- AGENT 佣金结算不纳入本期

---

### Task 1: 更新 SQL schema

**Files:**
- Modify: `sql/app_ext.sql`

**Interfaces:**
- Produces: `referral_record` 表新结构（event_type, referrer_org_id, referred_org_id, 移除 reward_amount/reward_type）

- [ ] **Step 1: 替换 referral_record 建表语句**

将 `sql/app_ext.sql` 中原有的 `CREATE TABLE referral_record` 和 `ALTER TABLE grid_user` 替换为：

```sql
-- ----------------------------
-- 邀请记录表（事件模型）
-- ----------------------------
DROP TABLE IF EXISTS `referral_record`;

CREATE TABLE `referral_record` (
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `referrer_id`     BIGINT NOT NULL COMMENT '邀请人用户ID',
    `referrer_type`   VARCHAR(20) NOT NULL COMMENT 'NORMAL / INSTITUTION / AGENT',
    `referrer_org_id` INT COMMENT '邀请人所属机构ID（INSTITUTION类型时使用）',
    `referred_id`     BIGINT COMMENT '被邀请人用户ID',
    `referred_org_id` INT COMMENT '被邀请机构ID（被邀对象是机构时使用）',
    `referral_code`   VARCHAR(32) NOT NULL COMMENT '使用的邀请码',
    `event_type`      VARCHAR(30) NOT NULL COMMENT 'REGISTER / SUBSCRIBE',
    `order_id`        BIGINT COMMENT '关联订单ID（SUBSCRIBE事件时使用）',
    `reward_status`   VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / SETTLED',
    `settle_time`     DATETIME COMMENT '结算时间',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_referrer` (`referrer_id`),
    KEY `idx_referrer_org` (`referrer_org_id`),
    KEY `idx_referred` (`referred_id`),
    KEY `idx_reward_status` (`reward_status`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请记录表（事件模型）';
```

同时保留 `grid_user` 的 ALTER 语句（扩展字段不变）。

- [ ] **Step 2: 验证 SQL 语法**

Run: `grep -c "referral_record" /Users/nano/Desktop/nano-gemini/sql/app_ext.sql`
Expected: 2 (DROP + CREATE)

- [ ] **Step 3: Commit**

```bash
git add sql/app_ext.sql
git commit -m "feat: redesign referral_record table for event model

- Add event_type, referrer_org_id, referred_org_id columns
- Remove reward_amount, reward_type columns
- Simplify reward_status to PENDING/SETTLED
- Add indexes for settlement job queries

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: 更新 ReferralRecord 实体

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/domain/ReferralRecord.java`

**Interfaces:**
- Produces: ReferralRecord 实体匹配新表结构

- [ ] **Step 1: 重写 ReferralRecord.java**

```java
package com.naon.grid.modules.app.domain;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
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

    @Column
    private Integer referrerOrgId;

    @Column
    private Long referredId;

    @Column
    private Integer referredOrgId;

    @Column(length = 32, nullable = false)
    private String referralCode;

    @Column(length = 30, nullable = false)
    private String eventType;

    @Column
    private Long orderId;

    @Column(length = 20)
    private String rewardStatus = "PENDING";

    @Column
    private LocalDateTime settleTime;

    @Column
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl grid-app -am -DskipTests -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/domain/ReferralRecord.java
git commit -m "feat: update ReferralRecord entity for event model

- Add eventType, referrerOrgId, referredOrgId fields
- Remove rewardAmount, rewardType fields
- Simplify rewardStatus to PENDING/SETTLED

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: 更新 Repository 层

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/repository/ReferralRecordRepository.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/repository/GridUserRepository.java`

**Interfaces:**
- Produces: 支持按 reward_status/event_type/referred_id 查询，支持批量更新 reward_status，支持按 orgId 查成员

- [ ] **Step 1: 重写 ReferralRecordRepository.java**

```java
package com.naon.grid.modules.app.repository;

import com.naon.grid.modules.app.domain.ReferralRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRecordRepository extends JpaRepository<ReferralRecord, Long> {

    List<ReferralRecord> findByRewardStatus(String rewardStatus);

    List<ReferralRecord> findByReferrerId(Long referrerId);

    Optional<ReferralRecord> findFirstByReferredIdAndEventTypeOrderByCreateTimeDesc(
            Long referredId, String eventType);

    @Modifying
    @Query("UPDATE ReferralRecord r SET r.rewardStatus = 'SETTLED', r.settleTime = :now WHERE r.id IN :ids")
    int batchMarkSettled(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);
}
```

- [ ] **Step 2: 给 GridUserRepository 添加查询方法**

在 `grid-app/src/main/java/com/naon/grid/modules/app/repository/GridUserRepository.java` 末尾（`}` 前）添加：

```java
    /**
     * 根据机构ID查询所有成员
     */
    List<GridUser> findByOrgId(Integer orgId);
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl grid-app -am -DskipTests -q 2>&1 | tail -5`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/repository/ReferralRecordRepository.java \
        grid-app/src/main/java/com/naon/grid/modules/app/repository/GridUserRepository.java
git commit -m "feat: update repositories for event model queries

- Add batchMarkSettled, findByEventType to ReferralRecordRepository
- Add findByOrgId to GridUserRepository for institution member lookup

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: 重构 ReferralService 接口和实现

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/ReferralService.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/ReferralServiceImpl.java`
- Delete 内容: `processReferral()`, `settleReferralReward()`, 旧的依赖注入

**Interfaces:**
- Produces: `recordEvent(String, Long, String)`, `recordEvent(String, Long, String, Integer)`, `settlePendingRewards()`
- Consumes: ReferralRecordRepository, GridUserRepository, GridOrganizationRepository, EntitlementService, EntitlementRepository

- [ ] **Step 1: 重写 ReferralService.java**

```java
package com.naon.grid.modules.app.service;

public interface ReferralService {

    /**
     * 记录邀请事件（普通用户被邀请）
     * @param referralCode 邀请码
     * @param referredUserId 被邀请人用户ID
     * @param eventType 事件类型 REGISTER / SUBSCRIBE
     * @return 邀请人ID，无效邀请码返回 null
     */
    Long recordEvent(String referralCode, Long referredUserId, String eventType);

    /**
     * 记录邀请事件（机构被邀请）
     * @param referredOrgId 被邀请机构ID
     */
    Long recordEvent(String referralCode, Long referredUserId, String eventType, Integer referredOrgId);

    /**
     * 每日结算邀请奖励（由定时任务调用）
     */
    void settlePendingRewards();
}
```

- [ ] **Step 2: 重写 ReferralServiceImpl.java**

```java
package com.naon.grid.modules.app.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.ReferralRecord;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.repository.ReferralRecordRepository;
import com.naon.grid.modules.app.service.ReferralService;
import com.naon.grid.modules.billing.repository.EntitlementRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.repository.GridOrganizationRepository;
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
public class ReferralServiceImpl implements ReferralService {

    private final GridUserRepository userRepository;
    private final GridOrganizationRepository organizationRepository;
    private final ReferralRecordRepository referralRecordRepository;
    private final EntitlementService entitlementService;
    private final EntitlementRepository entitlementRepository;

    private static final List<String> ALL_ENTITLEMENT_CODES = Arrays.asList(
            "VOCAB_ACCESS", "GRAMMAR_ACCESS", "CHARACTER_ACCESS",
            "CONFUSING_WORDS_ACCESS", "CULTURE_ACCESS", "TOPIC_ACCESS");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordEvent(String referralCode, Long referredUserId, String eventType) {
        return recordEvent(referralCode, referredUserId, eventType, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordEvent(String referralCode, Long referredUserId,
                            String eventType, Integer referredOrgId) {
        if (referralCode == null || referralCode.isEmpty()) return null;

        GridUser referrer = userRepository.findByReferralCode(referralCode).orElse(null);
        if (referrer == null) return null;
        if (referrer.getId().equals(referredUserId)) return null;

        String referrerType = resolveReferrerType(referrer);
        Integer referrerOrgId = referrer.getOrgId();

        ReferralRecord record = new ReferralRecord();
        record.setReferrerId(referrer.getId());
        record.setReferrerType(referrerType);
        record.setReferrerOrgId(referrerOrgId);
        record.setReferredId(referredUserId);
        record.setReferredOrgId(referredOrgId);
        record.setReferralCode(referralCode);
        record.setEventType(eventType);
        record.setRewardStatus("PENDING");
        record.setCreateTime(LocalDateTime.now());
        referralRecordRepository.save(record);

        log.info("Referral event recorded: referrerId={}, type={}, event={}, referredId={}",
                referrer.getId(), referrerType, eventType, referredUserId);
        return referrer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settlePendingRewards() {
        List<ReferralRecord> pending = referralRecordRepository.findByRewardStatus("PENDING");
        if (pending.isEmpty()) return;

        // Group by composite key
        Map<SettleKey, List<ReferralRecord>> groups = pending.stream()
                .collect(Collectors.groupingBy(SettleKey::from));

        String batchId = "REFERRAL_" + LocalDateTime.now().toLocalDate();
        LocalDateTime now = LocalDateTime.now();
        List<Long> settledIds = new ArrayList<>();

        for (Map.Entry<SettleKey, List<ReferralRecord>> entry : groups.entrySet()) {
            SettleKey key = entry.getKey();
            List<ReferralRecord> records = entry.getValue();
            int count = records.size();

            int daysPerEvent = resolveDaysPerEvent(
                    key.referrerType, key.eventType, key.referredOrgId != null);
            if (daysPerEvent <= 0) continue;

            int totalDays;
            int settledCount;

            if ("INSTITUTION".equals(key.referrerType) && key.referredOrgId == null) {
                // Threshold-based: institution → normal user
                int threshold = "REGISTER".equals(key.eventType) ? 100 : 10;
                settledCount = (count / threshold) * threshold;
                totalDays = count / threshold; // 1 day for all members per threshold
            } else {
                // Per-event: total = count * daysPerEvent
                settledCount = count;
                totalDays = count * daysPerEvent;
            }

            if (totalDays <= 0) continue;

            // Grant entitlements
            List<Integer> entitleIds = resolveAllEntitlementIds();
            if ("INSTITUTION".equals(key.referrerType)) {
                grantToInstitutionMembers(key.referrerOrgId, entitleIds, batchId, totalDays);
            } else {
                entitlementService.grantEntitlements(
                        key.referrerId, entitleIds, "REFERRAL", batchId, totalDays, null);
            }

            // Mark settled
            List<Long> ids = records.subList(0, settledCount).stream()
                    .map(ReferralRecord::getId)
                    .collect(Collectors.toList());
            settledIds.addAll(ids);

            log.info("Referral settled: referrerId={}, type={}, event={}, "
                    + "isInstitution={}, count={}, settledCount={}, totalDays={}",
                    key.referrerId, key.referrerType, key.eventType,
                    key.referredOrgId != null, count, settledCount, totalDays);
        }

        if (!settledIds.isEmpty()) {
            referralRecordRepository.batchMarkSettled(settledIds, now);
        }
    }

    private String resolveReferrerType(GridUser referrer) {
        if (referrer.getOrgId() != null) {
            GridOrganization org = organizationRepository.findById(referrer.getOrgId()).orElse(null);
            if (org != null) {
                if ("AGENT".equals(org.getOrgRole())) return "AGENT";
                return "INSTITUTION";
            }
        }
        return "NORMAL";
    }

    /**
     * 按奖励矩阵返回每个事件的奖励天数
     */
    private int resolveDaysPerEvent(String referrerType, String eventType, boolean isInstitutionReferred) {
        if ("NORMAL".equals(referrerType) || "AGENT".equals(referrerType)) {
            if ("REGISTER".equals(eventType)) return isInstitutionReferred ? 10 : 1;
            if ("SUBSCRIBE".equals(eventType)) return isInstitutionReferred ? 100 : 10;
        }
        if ("INSTITUTION".equals(referrerType)) {
            if (isInstitutionReferred) {
                if ("REGISTER".equals(eventType)) return 1;
                if ("SUBSCRIBE".equals(eventType)) return 10;
            }
            // institution → normal user: handled by threshold logic in settlePendingRewards
            return 1; // per-threshold reward
        }
        return 0;
    }

    private List<Integer> resolveAllEntitlementIds() {
        return ALL_ENTITLEMENT_CODES.stream()
                .map(code -> entitlementRepository.findByCode(code)
                        .orElseThrow(() -> new RuntimeException("Entitlement not found: " + code)))
                .map(e -> e.getId())
                .collect(Collectors.toList());
    }

    private void grantToInstitutionMembers(Integer orgId, List<Integer> entitlementIds,
                                           String batchId, int days) {
        if (orgId == null) return;
        List<GridUser> members = userRepository.findByOrgId(orgId);
        for (GridUser member : members) {
            entitlementService.grantEntitlements(
                    member.getId(), entitlementIds, "REFERRAL", batchId, days, null);
        }
        log.info("Granted {} days to {} members of orgId={}", days, members.size(), orgId);
    }

    /**
     * 分组键：用于聚合 PENDING 记录
     */
    private static class SettleKey {
        final Long referrerId;
        final String referrerType;
        final Integer referrerOrgId;
        final String eventType;
        final Integer referredOrgId; // null = 普通用户, non-null = 机构

        SettleKey(Long referrerId, String referrerType, Integer referrerOrgId,
                  String eventType, Integer referredOrgId) {
            this.referrerId = referrerId;
            this.referrerType = referrerType;
            this.referrerOrgId = referrerOrgId;
            this.eventType = eventType;
            this.referredOrgId = referredOrgId;
        }

        static SettleKey from(ReferralRecord r) {
            return new SettleKey(r.getReferrerId(), r.getReferrerType(),
                    r.getReferrerOrgId(), r.getEventType(), r.getReferredOrgId());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SettleKey)) return false;
            SettleKey that = (SettleKey) o;
            return Objects.equals(referrerId, that.referrerId)
                    && Objects.equals(referrerType, that.referrerType)
                    && Objects.equals(referrerOrgId, that.referrerOrgId)
                    && Objects.equals(eventType, that.eventType)
                    && Objects.equals(referredOrgId, that.referredOrgId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(referrerId, referrerType, referrerOrgId, eventType, referredOrgId);
        }
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -pl grid-app -am -DskipTests -q 2>&1 | tail -10`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/ReferralService.java \
        grid-app/src/main/java/com/naon/grid/modules/app/service/impl/ReferralServiceImpl.java
git commit -m "feat: redesign ReferralService for event model

- Replace processReferral() with recordEvent()
- Replace settleReferralReward() with settlePendingRewards()
- Implement reward matrix calculation with threshold logic
- Add institution-wide member grant support

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5: 更新社交登录 DTO 和注册逻辑

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SocialLoginDTO.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SocialBindEmailDTO.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java`

**Interfaces:**
- Consumes: `ReferralService.recordEvent(String, Long, String)`
- Produces: register() 和社交登录流程调用新的 recordEvent()

- [ ] **Step 1: 修改 SocialLoginDTO.java，添加 referralCode 字段**

在 `private String deviceName;` 后面添加：

```java
    private String referralCode;
```

- [ ] **Step 2: 修改 SocialBindEmailDTO.java，添加 referralCode 字段**

文件路径：`grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SocialBindEmailDTO.java`

在 `private String deviceName;` 后面添加：

```java
    private String referralCode;
```

- [ ] **Step 3: 修改 AppAuthServiceImpl.register() 中的 referral 调用**

将第 137-139 行的：
```java
        // Record referral relationship (needs user ID)
        if (referralCode != null && !referralCode.isEmpty()) {
            referralService.processReferral(referralCode, user.getId());
        }
```

替换为：
```java
        // Record referral event
        if (referralCode != null && !referralCode.isEmpty()) {
            referralService.recordEvent(referralCode, user.getId(), "REGISTER");
        }
```

- [ ] **Step 4: 修改 createSocialUser 方法签名和逻辑**

将方法签名从：
```java
    private GridUser createSocialUser(SocialUserInfo socialUser, HttpServletRequest request) {
```

改为：
```java
    private GridUser createSocialUser(SocialUserInfo socialUser, HttpServletRequest request, String referralCode) {
```

在 `userRepository.save(user);` 之后、trial 发放之前添加：

```java
        // Record referral event
        if (referralCode != null && !referralCode.isEmpty()) {
            user.setReferredBy(referralCode);
            userRepository.save(user);
            referralService.recordEvent(referralCode, user.getId(), "REGISTER");
        }
```

- [ ] **Step 5: 修改 createOrLinkSocialUser 调用 createSocialUser 时传入 referralCode**

将方法签名从：
```java
    private TokenDTO createOrLinkSocialUser(String provider, SocialUserInfo socialUser,
                                             String idToken, String deviceId, String deviceName,
                                             HttpServletRequest request) {
```

改为：
```java
    private TokenDTO createOrLinkSocialUser(String provider, SocialUserInfo socialUser,
                                             String idToken, String deviceId, String deviceName,
                                             HttpServletRequest request, String referralCode) {
```

将 `createSocialUser(socialUser, request)` 调用改为：
```java
            user = createSocialUser(socialUser, request, referralCode);
```

- [ ] **Step 6: 修改 socialLogin() 调用 createOrLinkSocialUser 时传入 referralCode**

在第 288-289 行的 return 语句中，将调用改为：
```java
        return createOrLinkSocialUser(provider, socialUser, socialLoginDTO.getIdToken(),
                socialLoginDTO.getDeviceId(), socialLoginDTO.getDeviceName(), request,
                socialLoginDTO.getReferralCode());
```

同时，在 `socialLogin()` 方法中，当从 auth 记录中找到已有用户时，如果该用户尚未设置 `referredBy` 且本次传入了 `referralCode`，也需要处理。在 `updateLoginMetadata(user, request);` 之前添加：

```java
            // Process referral code for existing user if not already referred
            String refCode = socialLoginDTO.getReferralCode();
            if (refCode != null && !refCode.isEmpty()
                    && (user.getReferredBy() == null || user.getReferredBy().isEmpty())) {
                user.setReferredBy(refCode);
                userRepository.save(user);
                referralService.recordEvent(refCode, user.getId(), "REGISTER");
            }
```

（这段逻辑插入在 `fillUserProfile(user, socialUser);` 调用之后、`updateLoginMetadata(user, request);` 调用之前。）

- [ ] **Step 7: 修改 socialBindEmail() 支持 referralCode**

在 `socialBindEmail()` 方法中，新用户创建分支（`userRepository.save(user);` 之后）添加：

```java
            // Record referral event
            String refCode = socialBindEmailDTO.getReferralCode();
            if (refCode != null && !refCode.isEmpty()) {
                user.setReferredBy(refCode);
                userRepository.save(user);
                referralService.recordEvent(refCode, user.getId(), "REGISTER");
            }
```

- [ ] **Step 8: 验证编译**

Run: `mvn compile -pl grid-app -am -DskipTests -q 2>&1 | tail -10`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SocialLoginDTO.java \
        grid-app/src/main/java/com/naon/grid/modules/app/service/dto/SocialBindEmailDTO.java \
        grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java
git commit -m "feat: add referral code support to social login and update registration

- Add referralCode field to SocialLoginDTO and SocialBindEmailDTO
- Thread referral code through social login and bind email flows
- Replace processReferral() calls with recordEvent()
- Allow existing users to be referred via social login

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: 更新机构审批逻辑

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/OrganizationServiceImpl.java`

**Interfaces:**
- Consumes: `ReferralService.recordEvent(String, Long, String, Integer)`
- Produces: approve() 调用新的 recordEvent() 并传入 referredOrgId

- [ ] **Step 1: 修改 approve() 中的 referral 调用**

将第 270-272 行：
```java
        // 处理邀请码溯源（如果申请时填了推荐码）
        if (org.getReferredBy() != null && !org.getReferredBy().isEmpty()) {
            referralService.processReferral(org.getReferredBy(), admin.getId());
        }
```

替换为：
```java
        // 记录邀请事件（机构被邀请入驻）
        if (org.getReferredBy() != null && !org.getReferredBy().isEmpty()) {
            referralService.recordEvent(org.getReferredBy(), admin.getId(),
                    "REGISTER", org.getId());
        }
```

- [ ] **Step 2: 验证编译**

Run: `mvn compile -pl grid-app -am -DskipTests -q 2>&1 | tail -10`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/OrganizationServiceImpl.java
git commit -m "feat: update institution approval to use recordEvent

- Use recordEvent() with referredOrgId for institution referrals
- Pass the approved organization ID as referredOrgId

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 7: 支付回调新增 SUBSCRIBE 事件

**Files:**
- Modify: `grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/PaymentServiceImpl.java`

**Interfaces:**
- Consumes: `ReferralRecordRepository.findFirstByReferredIdAndEventTypeOrderByCreateTimeDesc()`, `ReferralService.recordEvent()`
- Produces: handlePaymentCallback() 末尾写入 SUBSCRIBE 事件

- [ ] **Step 1: 注入 ReferralService 和 ReferralRecordRepository**

在 `PaymentServiceImpl` 类的字段区域添加：

```java
    private final ReferralService referralService;
    private final ReferralRecordRepository referralRecordRepository;
```

Lombok `@RequiredArgsConstructor` 会自动生成构造函数参数（需确保字段为 `final`）。

- [ ] **Step 2: 在 handlePaymentCallback() 末尾添加 SUBSCRIBE 事件写入**

在方法最后的 `return true;` 之前添加：

```java
        // Record SUBSCRIBE referral event
        try {
            referralRecordRepository
                    .findFirstByReferredIdAndEventTypeOrderByCreateTimeDesc(
                            order.getUserId(), "REGISTER")
                    .ifPresent(regRecord -> {
                        Integer referredOrgId = order.getOrgId();
                        referralService.recordEvent(regRecord.getReferralCode(),
                                order.getUserId(), "SUBSCRIBE", referredOrgId);
                    });
        } catch (Exception e) {
            log.warn("Failed to record SUBSCRIBE referral event for userId={}: {}",
                    order.getUserId(), e.getMessage());
        }
```

注意：`GridOrder` 的 import 已在文件中存在，无需额外添加。

- [ ] **Step 3: 添加缺失的 import**

在文件头部添加：

```java
import com.naon.grid.modules.app.repository.ReferralRecordRepository;
import com.naon.grid.modules.app.service.ReferralService;
```

- [ ] **Step 4: 验证编译**

Run: `mvn compile -pl grid-billing -am -DskipTests -q 2>&1 | tail -10`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add grid-billing/src/main/java/com/naon/grid/modules/billing/service/impl/PaymentServiceImpl.java
git commit -m "feat: record SUBSCRIBE referral event on payment success

- Inject ReferralService and ReferralRecordRepository
- On successful payment, look up the user's REGISTER referral record
- Write a SUBSCRIBE event with the same referrer info
- Pass order's orgId as referredOrgId for institution purchases

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 8: 创建每日结算定时任务

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/scheduled/ReferralSettlementJob.java`
- Modify: `grid-bootstrap/src/main/java/com/naon/grid/AppRun.java`

**Interfaces:**
- Consumes: `ReferralService.settlePendingRewards()`
- Produces: 每天凌晨 2:00 自动执行结算

- [ ] **Step 1: 创建定时任务目录**

```bash
mkdir -p grid-app/src/main/java/com/naon/grid/modules/app/scheduled
```

- [ ] **Step 2: 创建 ReferralSettlementJob.java**

```java
package com.naon.grid.modules.app.scheduled;

import com.naon.grid.modules.app.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReferralSettlementJob {

    private final ReferralService referralService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void settleReferralRewards() {
        log.info("Referral settlement job started");
        try {
            referralService.settlePendingRewards();
            log.info("Referral settlement job completed");
        } catch (Exception e) {
            log.error("Referral settlement job failed", e);
        }
    }
}
```

- [ ] **Step 3: 在 AppRun.java 添加 @EnableScheduling**

读取 `grid-bootstrap/src/main/java/com/naon/grid/AppRun.java`，在 `@SpringBootApplication` 注解上方或 `@EnableAsync` 旁边添加 `@EnableScheduling`。

例如，在 `@EnableAsync` 之后添加：

```java
@EnableAsync
@EnableScheduling
```

- [ ] **Step 4: 验证编译**

Run: `mvn compile -pl grid-bootstrap -am -DskipTests -q 2>&1 | tail -10`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/scheduled/ReferralSettlementJob.java \
        grid-bootstrap/src/main/java/com/naon/grid/AppRun.java
git commit -m "feat: add daily referral settlement job

- Create ReferralSettlementJob with cron: 0 0 2 * * ?
- Enable @Scheduling in AppRun
- Job calls ReferralService.settlePendingRewards() daily

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 9: 清理和最终验证

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppReferralController.java`

- [ ] **Step 1: 更新 AppReferralController**

移除旧的 Phase 1 注释，保持基础骨架：

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

    @ApiOperation("推荐系统状态")
    @GetMapping("/info")
    public ResponseEntity<String> info() {
        return ResponseEntity.ok("Referral system active");
    }
}
```

- [ ] **Step 2: 全量编译验证**

Run: `mvn compile -DskipTests -q 2>&1 | tail -10`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppReferralController.java
git commit -m "chore: clean up AppReferralController comments

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Verification Checklist

完成所有 Task 后验证：

1. `mvn compile -DskipTests` — 全量编译成功
2. 注册流程：`POST /api/app/auth/register` 带 `referralCode` → referral_record 写入 REGISTER 事件
3. 社交登录：`POST /api/app/auth/social-login` 带 `referralCode` → 新用户写入 REGISTER 事件
4. 机构审批：`POST /api/institutions/{id}/approve` → 若申请时有 `referredBy`，写入 REGISTER 事件（含 referredOrgId）
5. 支付回调：`PaymentServiceImpl.handlePaymentCallback()` → 用户有 REGISTER 记录时写入 SUBSCRIBE 事件
6. 手动触发 JOB：`ReferralSettlementJob.settleReferralRewards()` → PENDING 记录按矩阵计算并标记 SETTLED
