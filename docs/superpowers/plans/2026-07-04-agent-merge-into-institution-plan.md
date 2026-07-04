# 代理体系合并到机构体系 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 废弃独立的 GridAgent 体系，将代理能力合并到机构中，机构通过 orgRole 区分普通机构和代理机构。

**Architecture:** GridOrganization 新增 orgRole 和 commissionRate 字段；注册时通过 applyAsAgent 申请成为代理；审核时自动设置角色；推荐逻辑改为查 GridUser + 机构 orgRole 判类型；删除所有 Agent 相关文件。

**Tech Stack:** Spring Boot 2.7.18, JPA, MySQL, Lombok

## Global Constraints

- GridUser 不感知机构角色，orgRole 只在 GridOrganization 上
- 无需数据迁移（项目未上线）
- 推荐码统一无前缀，通过唯一性校验
- 编译通过（mvn clean compile -DskipTests）

---

### Task 1: SQL 变更 — normal_user.sql

**Files:**
- Modify: `sql/normal_user.sql:94-95`

**Interfaces:**
- Produces: `grid_organization.org_role VARCHAR(20) DEFAULT 'INSTITUTION'`, `grid_organization.commission_rate DECIMAL(5,2) DEFAULT 0.00`

- [ ] **Step 1: grid_organization 表增加 orgRole 和 commissionRate 字段**

在 `expire_time` 行之后、`create_by` 行之前插入两个新字段：

编辑 `sql/normal_user.sql`，在第 95 行 `expire_time` 之后插入：

```sql
  `org_role`        VARCHAR(20)  NOT NULL DEFAULT 'INSTITUTION' COMMENT 'INSTITUTION/AGENT',
  `commission_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '佣金比例',
```

修改后 `grid_organization` 尾部字段为：

```sql
  `expire_time`     DATETIME COMMENT '机构有效到期时间',
  `org_role`        VARCHAR(20)  NOT NULL DEFAULT 'INSTITUTION' COMMENT 'INSTITUTION/AGENT',
  `commission_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '佣金比例',
  `create_by`       VARCHAR(50), `update_by` VARCHAR(50),
  `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
```

- [ ] **Step 2: 删除 grid_agent 建表语句**

删除 `sql/normal_user.sql` 第 104-121 行（整个 `CREATE TABLE grid_agent` 块，包含注释行和空行）。

- [ ] **Step 3: 验证 SQL 语法**

```bash
# 检查 grid_agent 不再出现
grep -n "grid_agent" sql/normal_user.sql
# 预期：无输出
```

- [ ] **Step 4: Commit**

```bash
git add sql/normal_user.sql
git commit -m "feat: add org_role and commission_rate to grid_organization, remove grid_agent table"
```

---

### Task 2: SQL 变更 — app_ext.sql

**Files:**
- Modify: `sql/app_ext.sql`

**Interfaces:**
- Produces: 清理后的 app_ext.sql，无 grid_organization 和 grid_agent 重复定义，只保留 referral_record 和 grid_user ALTER TABLE

- [ ] **Step 1: 删除 grid_organization 重复建表**

删除 `sql/app_ext.sql` 第 1-27 行（注释头 + `grid_organization` 建表语句）。

- [ ] **Step 2: 删除 grid_agent 建表语句**

删除 `sql/app_ext.sql` 中原第 29-49 行（`grid_agent` 建表，标题注释 + CREATE TABLE）。

- [ ] **Step 3: 验证最终内容**

`app_ext.sql` 剩余内容应为：
1. `referral_record` 建表语句（保留）
2. `grid_user` ALTER TABLE 扩展字段（保留）

```bash
grep -n "CREATE TABLE\|ALTER TABLE" sql/app_ext.sql
# 预期仅输出：referral_record 和 grid_user
```

- [ ] **Step 4: Commit**

```bash
git add sql/app_ext.sql
git commit -m "feat: remove duplicate grid_organization and grid_agent from app_ext.sql"
```

---

### Task 3: GridOrganization 实体新增字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/modules/system/domain/GridOrganization.java:57-60`

**Interfaces:**
- Produces: `GridOrganization.getOrgRole(): String`, `GridOrganization.setOrgRole(String)`, `GridOrganization.getCommissionRate(): BigDecimal`, `GridOrganization.setCommissionRate(BigDecimal)`

- [ ] **Step 1: 添加 import**

在 `GridOrganization.java` 顶部 import 区域添加：

```java
import java.math.BigDecimal;
```

- [ ] **Step 2: 添加 orgRole 和 commissionRate 字段**

在 `GridOrganization.java` 中 `expireTime` 字段之后添加：

```java
    @Column(length = 20, nullable = false)
    private String orgRole = "INSTITUTION";

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionRate = BigDecimal.ZERO;
```

- [ ] **Step 3: 验证编译**

```bash
cd grid-system && mvn compile -DskipTests
# 预期：BUILD SUCCESS
```

- [ ] **Step 4: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/domain/GridOrganization.java
git commit -m "feat: add orgRole and commissionRate fields to GridOrganization"
```

---

### Task 4: InstitutionRegisterDTO 新增 applyAsAgent

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/modules/system/service/dto/InstitutionRegisterDTO.java`

**Interfaces:**
- Produces: `InstitutionRegisterDTO.getApplyAsAgent(): Boolean`

- [ ] **Step 1: 添加字段**

在 `InstitutionRegisterDTO.java` 的 `referredBy` 字段之后、类结束 `}` 之前添加：

```java
    private Boolean applyAsAgent;
```

- [ ] **Step 2: 验证编译**

```bash
cd grid-system && mvn compile -DskipTests
# 预期：BUILD SUCCESS
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/service/dto/InstitutionRegisterDTO.java
git commit -m "feat: add applyAsAgent field to InstitutionRegisterDTO"
```

---

### Task 5: OrganizationService 接口新增 updateRole

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/modules/system/service/OrganizationService.java`

**Interfaces:**
- Produces: `OrganizationService.updateRole(Integer orgId, String orgRole): void`

- [ ] **Step 1: 添加方法声明**

在 `OrganizationService` 接口中（`findById` 方法之前）添加：

```java
    /**
     * 变更机构角色（普通机构 ↔ 代理机构）
     */
    void updateRole(Integer orgId, String orgRole);
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/service/OrganizationService.java
git commit -m "feat: add updateRole method to OrganizationService interface"
```

---

### Task 6: OrganizationServiceImpl 业务逻辑改造

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/OrganizationServiceImpl.java`

**Interfaces:**
- Consumes: `GridOrganization.orgRole`, `GridOrganization.commissionRate`, `InstitutionRegisterDTO.applyAsAgent`, `OrganizationService.updateRole`
- Produces: register() 支持 applyAsAgent, approve() 支持代理机构, 新增 updateRole()

- [ ] **Step 1: 修改 register() — 记录 applyAsAgent**

将 `OrganizationServiceImpl.register()` 中 `org.setAuditStatus("PENDING");` 之前的一段修改为：

```java
        // 创建机构申请记录（PENDING）
        GridOrganization org = new GridOrganization();
        org.setName(dto.getName());
        org.setNameEn(dto.getNameEn());
        org.setOrgType(dto.getOrgType());
        org.setContactName(dto.getContactName());
        org.setContactPhone(dto.getContactPhone());
        org.setContactEmail(dto.getAdminEmail());
        org.setReferredBy(dto.getReferredBy());
        org.setAdminPassword(encryptedPassword);
        org.setRegion(region);
        // 若申请成为代理机构，设置 orgRole 为 AGENT
        if (Boolean.TRUE.equals(dto.getApplyAsAgent())) {
            org.setOrgRole("AGENT");
        }
        org.setAuditStatus("PENDING");
```

- [ ] **Step 2: 修改 approve() — 代理机构设初始佣金**

在 `OrganizationServiceImpl.approve()` 中，`org.setAuditStatus("APPROVED");` 之后、`organizationRepository.save(org);` 之前添加：

```java
        // 若为代理机构，设置初始佣金比例（管理员后续可调整）
        if ("AGENT".equals(org.getOrgRole())) {
            org.setCommissionRate(java.math.BigDecimal.ZERO);
        }

        org.setAuditStatus("APPROVED");
```

注意：需要确保 `org.setAuditStatus("APPROVED")` 在佣金设置之后执行。实际修改时需调整现有代码顺序。

- [ ] **Step 3: 新增 updateRole() 方法**

在 `OrganizationServiceImpl` 类中，`findById` 方法之前添加：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Integer orgId, String orgRole) {
        GridOrganization org = findById(orgId);
        if (!"INSTITUTION".equals(orgRole) && !"AGENT".equals(orgRole)) {
            throw new BadRequestException("无效的机构角色，仅支持 INSTITUTION 或 AGENT");
        }
        org.setOrgRole(orgRole);
        organizationRepository.save(org);
    }
```

- [ ] **Step 4: 验证编译**

```bash
cd grid-app && mvn compile -DskipTests
# 预期：BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/OrganizationServiceImpl.java
git commit -m "feat: support applyAsAgent in register, set commission in approve, add updateRole"
```

---

### Task 7: InstitutionAuditController 新增角色变更接口

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/modules/system/rest/InstitutionAuditController.java`

**Interfaces:**
- Produces: `PUT /api/institutions/{id}/role`

- [ ] **Step 1: 添加 updateRole 接口**

在 `InstitutionAuditController` 的 `reject()` 方法之后、类结束 `}` 之前添加：

```java
    @ApiOperation("变更机构角色（普通机构 ↔ 代理机构）")
    @PutMapping("/{id}/role")
    @PreAuthorize("@el.check('institution:edit')")
    public ResponseEntity<Void> updateRole(@PathVariable Integer id,
                                           @RequestParam String orgRole) {
        organizationService.updateRole(id, orgRole);
        return ResponseEntity.ok().build();
    }
```

- [ ] **Step 2: 验证编译**

```bash
cd grid-system && mvn compile -DskipTests
# 预期：BUILD SUCCESS
```

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/rest/InstitutionAuditController.java
git commit -m "feat: add institution role change endpoint PUT /api/institutions/{id}/role"
```

---

### Task 8: ReferralServiceImpl 推荐逻辑改造

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/ReferralServiceImpl.java`

**Interfaces:**
- Consumes: `GridOrganization.orgRole` (via GridOrganizationRepository)
- Produces: processReferral() 改为仅查 GridUser + 机构 orgRole 判定类型

- [ ] **Step 1: 添加依赖注入**

在 `ReferralServiceImpl` 类中，添加 `GridOrganizationRepository` 字段：

```java
    private final GridOrganizationRepository organizationRepository;
```

构造函数中已由 `@RequiredArgsConstructor` 自动注入。

- [ ] **Step 2: 添加 import**

```java
import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.repository.GridOrganizationRepository;
```

同时删除不再使用的 import：

```java
// 删除：import com.naon.grid.modules.system.domain.GridAgent;
// 删除：import com.naon.grid.modules.system.repository.GridAgentRepository;
```

- [ ] **Step 3: 改造 processReferral() 方法**

将方法体完整替换为：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long processReferral(String referralCode, Long referredUserId) {
        if (referralCode == null || referralCode.isEmpty()) return null;

        // 按推荐码查找推荐人用户
        GridUser referrer = userRepository.findByReferralCode(referralCode).orElse(null);
        if (referrer == null) return null;
        
        // 不能自己推荐自己
        if (referrer.getId().equals(referredUserId)) return null;

        // 判断推荐人是否为代理机构成员
        boolean isAgent = false;
        if (referrer.getOrgId() != null) {
            isAgent = organizationRepository.findById(referrer.getOrgId())
                    .map(org -> "AGENT".equals(org.getOrgRole()))
                    .orElse(false);
        }

        String referrerType = isAgent ? "AGENT" : "NORMAL";
        String rewardType = isAgent ? "CASH" : "EXTEND_DAYS";

        ReferralRecord record = new ReferralRecord();
        record.setReferrerId(referrer.getId());
        record.setReferrerType(referrerType);
        record.setReferredId(referredUserId);
        record.setReferralCode(referralCode);
        record.setRewardStatus("PENDING");
        record.setRewardType(rewardType);
        record.setCreateTime(LocalDateTime.now());
        referralRecordRepository.save(record);

        return referrer.getId();
    }
```

- [ ] **Step 4: 验证编译**

```bash
cd grid-app && mvn compile -DskipTests
# 预期：BUILD SUCCESS
```

- [ ] **Step 5: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/ReferralServiceImpl.java
git commit -m "feat: refactor processReferral to use GridUser + orgRole instead of GridAgent"
```

---

### Task 9: 删除 Agent 相关文件

**Files:**
- Delete: `grid-system/src/main/java/com/naon/grid/modules/system/domain/GridAgent.java`
- Delete: `grid-system/src/main/java/com/naon/grid/modules/system/repository/GridAgentRepository.java`
- Delete: `grid-system/src/main/java/com/naon/grid/modules/system/service/AgentService.java`
- Delete: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AgentServiceImpl.java`
- Delete: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppAgentController.java`
- Delete: `grid-system/src/main/java/com/naon/grid/modules/system/rest/AgentAuditController.java`
- Delete: `grid-system/src/main/java/com/naon/grid/modules/system/service/dto/AgentRegisterDTO.java`

- [ ] **Step 1: 删除 7 个文件**

```bash
rm grid-system/src/main/java/com/naon/grid/modules/system/domain/GridAgent.java
rm grid-system/src/main/java/com/naon/grid/modules/system/repository/GridAgentRepository.java
rm grid-system/src/main/java/com/naon/grid/modules/system/service/AgentService.java
rm grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AgentServiceImpl.java
rm grid-app/src/main/java/com/naon/grid/modules/app/rest/AppAgentController.java
rm grid-system/src/main/java/com/naon/grid/modules/system/rest/AgentAuditController.java
rm grid-system/src/main/java/com/naon/grid/modules/system/service/dto/AgentRegisterDTO.java
```

- [ ] **Step 2: 验证编译**

```bash
mvn compile -DskipTests
# 预期：BUILD SUCCESS，无编译错误
```

- [ ] **Step 3: Commit**

```bash
git add -u
git commit -m "feat: remove Agent-related files, merged into Institution system"
```

---

### Task 10: 最终验证

- [ ] **Step 1: 全量编译**

```bash
mvn clean compile -DskipTests
# 预期：BUILD SUCCESS
```

- [ ] **Step 2: grep 检查无残留引用**

```bash
grep -rn "GridAgent\|AgentService\|AgentServiceImpl\|AppAgentController\|AgentAuditController\|AgentRegisterDTO\|GridAgentRepository" --include="*.java" grid-system/src grid-app/src grid-common/src grid-tools/src
# 预期：无输出
```
```bash
grep -rn "import.*AgentServiceImpl\|import.*AppAgentController" --include="*.java" .
# 预期：无输出
```

- [ ] **Step 3: 验证 SQL 文件一致性**

```bash
# normal_user.sql 中 grid_organization 应有 org_role 和 commission_rate
grep -c "org_role" sql/normal_user.sql
# 预期：1
grep -c "commission_rate" sql/normal_user.sql
# 预期：1

# 两个 SQL 文件都不应再有 grid_agent
grep -r "grid_agent" sql/
# 预期：无输出
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: final verification after agent merge into institution"
```
