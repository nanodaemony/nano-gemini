# 代理体系合并到机构体系 — 设计方案

日期: 2026-07-04

## 背景

当前项目有两套并行的身份体系：机构（Institution）和代理商（Agent），各自有独立的注册、审核、代码文件和数据表。但业务上，代理必须依托机构存在——代理商本质上就是一个具备推广能力的机构。

## 核心思路

废弃独立的 `GridAgent` 体系，将代理能力作为机构的一个属性。机构通过 `orgRole` 字段区分普通机构和代理机构。

## 数据库变更

### grid_organization（修改 — normal_user.sql）

在现有 `grid_organization` 表末尾（`expire_time` 之后）增加 2 个字段：

```sql
`org_role`        VARCHAR(20)  NOT NULL DEFAULT 'INSTITUTION' COMMENT 'INSTITUTION（普通机构）/ AGENT（代理机构）',
`commission_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '佣金比例，仅代理机构有意义',
```

完整新字段位置（`expire_time` 之后、`create_by` 之前）：

```sql
  `expire_time`     DATETIME COMMENT '机构有效到期时间',
  `org_role`        VARCHAR(20)  NOT NULL DEFAULT 'INSTITUTION' COMMENT 'INSTITUTION/AGENT',
  `commission_rate` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '佣金比例',
  `create_by`       VARCHAR(50), `update_by` VARCHAR(50),
```

### grid_agent（删除）

从 `normal_user.sql` 和 `app_ext.sql` 中移除 `grid_agent` 建表语句。

### app_ext.sql（同步清理）

- 删除 `grid_agent` 建表语句
- 删除 `grid_organization` 重复定义（以 normal_user.sql 为准）
- `referral_record` 保留不变

## 实体变更

### GridOrganization.java

新增两个字段：

```java
@Column(length = 20, nullable = false)
private String orgRole = "INSTITUTION";  // INSTITUTION / AGENT

@Column(precision = 5, scale = 2, nullable = false)
private BigDecimal commissionRate = BigDecimal.ZERO;
```

### GridUser.java

**不改动**。现有字段 `userType`、`agentId`、`referralCode`、`orgId`、`orgRole` 全部保留。代理机构管理员的 `userType` 统一为 `"INSTITUTION"`，通过 `orgId` 关联到代理机构；`agentId` 不再写入。

## API 变更

### POST /api/app/institution/register（修改）

请求体新增：

```java
private Boolean applyAsAgent;  // 可选，默认false。true表示申请成为代理机构
```

### PUT /api/institutions/{id}/role（新增）

```java
@PutMapping("/{id}/role")
@PreAuthorize("@el.check('institution:edit')")
ResponseEntity<Void> updateRole(@PathVariable Integer id, @RequestParam String orgRole);
// orgRole: INSTITUTION 或 AGENT
```

### 废弃接口（删除）

| 接口 | 原用途 |
|------|--------|
| `POST /api/app/agent/register` | 代理商自助注册 |
| `GET /api/agents/pending` | 待审核代理商列表 |
| `POST /api/agents/{id}/approve` | 代理商审核通过 |
| `POST /api/agents/{id}/reject` | 代理商审核驳回 |

代理商的审核走机构审核接口（`/api/institutions/{id}/approve`），`approve()` 方法中根据 `applyAsAgent` 或 `orgRole` 决定是否设为代理机构。

## 业务逻辑变更

### OrganizationServiceImpl.register()

- 记录 `applyAsAgent` 字段（存储在 `GridOrganization.orgRole`，若 `true` 则设为 `"AGENT"` 而非默认 `"INSTITUTION"`）

### OrganizationServiceImpl.approve()

- 若 `org.getOrgRole().equals("AGENT")`，设置默认 `commissionRate = BigDecimal.ZERO`（管理员后续手动调整）

### OrganizationServiceImpl（新增方法）

```java
void updateRole(Integer orgId, String orgRole) {
    GridOrganization org = findById(orgId);
    org.setOrgRole(orgRole);
    organizationRepository.save(org);
}
```

### ReferralServiceImpl.processReferral()

改造前逻辑：
1. 查 `GridAgent` 表匹配推荐码 → AGENT 推荐
2. 查 `GridUser` 表匹配推荐码 → NORMAL 推荐

改造后逻辑：
1. 始终查 `GridUser.referralCode` 定位推荐人
2. 若推荐人 `orgId != null`，查 `GridOrganization.orgRole`
3. `orgRole == "AGENT"` → `referrerType = "AGENT"`, `rewardType = "CASH"`
4. 否则 → `referrerType = "NORMAL"`, `rewardType = "EXTEND_DAYS"`

## 待删除文件

| 文件 | 说明 |
|------|------|
| `grid-system/.../domain/GridAgent.java` | 代理商实体 |
| `grid-system/.../repository/GridAgentRepository.java` | 代理商 Repository |
| `grid-system/.../service/AgentService.java` | 代理商 Service 接口 |
| `grid-app/.../service/impl/AgentServiceImpl.java` | 代理商 Service 实现 |
| `grid-app/.../rest/AppAgentController.java` | 代理商注册 Controller |
| `grid-system/.../rest/AgentAuditController.java` | 代理商审核 Controller |
| `grid-system/.../service/dto/AgentRegisterDTO.java` | 代理商注册 DTO |

## 设计原则

- **GridUser 不感知机构角色**：`orgRole` 只在 `GridOrganization` 上，`GridUser` 只通过 `orgId` 关联机构，不存储机构是否是代理的信息。
- **无需数据迁移**：项目未上线，直接改 DDL 和代码逻辑。
- **推荐码无前缀**：统一随机字符串，通过唯一性校验保证不冲突。
