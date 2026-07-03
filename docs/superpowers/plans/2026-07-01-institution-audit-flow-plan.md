# 机构审核入驻流程 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现机构申请→后台审核→审核通过/驳回的入驻流程，审核通过后才创建管理员用户，并通过邮件告知结果

**Architecture:** 
- 申请阶段只创建 `GridOrganization`（auditStatus=PENDING），不创建用户。密码加密后暂存在 `admin_password` 字段
- 审核通过时创建 `GridUser`（管理员）并生成机构邀请码，审核驳回时写入原因
- 用户侧 3 接口（注册/查询/重新提交），后台 4 接口（列表/详情/通过/驳回）
- `GridOrganization`、`GridAgent` 实体/接口从 grid-app 迁到 grid-system，后台 Controller 随之放在 grid-system，grid-bootstrap 不再包含业务 Controller

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, spring-boot-starter-mail

## Global Constraints

- 字段命名规范：Java camelCase → SQL snake_case
- 密码传输：前端 RSA 加密 → 后端解密 → BCrypt 加密存储
- 查询条件复用现有 `@Query` 注解 + `QueryHelp` 模式
- 邮件发送新增到 grid-tools 模块（复用该模块的第三方工具定位）
- `normal_user.sql` 中直接修改 `grid_organization` 表的 CREATE TABLE（数据库空数据，无需迁移）
- `service` 层抛 `BadRequestException` 统一处理异常响应
- `@AnonymousPutMapping` 注解已存在（在 grid-common），直接可用

---
### Task 0: 模块重组 — 将机构/代理相关类从 grid-app 迁移到 grid-system

**原因：** `InstitutionAuditController` 和 `AgentAuditController` 作为后台管理 Controller 应放在 `grid-system` 模块。但当前 `GridOrganization`、`GridAgent`、`OrganizationService` 等定义在 `grid-app`，`grid-system` 无法引用。需要将它们迁到 `grid-system`，grid-app 保留实现类。

**迁移清单：**

| 文件 | 原位置 (grid-app) | 新位置 (grid-system) |
|------|-------------------|---------------------|
| GridOrganization.java | `.../app/domain/` | `.../modules/system/domain/` |
| GridAgent.java | `.../app/domain/` | `.../modules/system/domain/` |
| GridOrganizationRepository.java | `.../app/repository/` | `.../modules/system/repository/` |
| GridAgentRepository.java | `.../app/repository/` | `.../modules/system/repository/` |
| OrganizationService.java | `.../app/service/` | `.../modules/system/service/` |
| AgentService.java | `.../app/service/` | `.../modules/system/service/` |
| InstitutionRegisterDTO.java | `.../app/service/dto/` | `.../modules/system/service/dto/` |
| ApplicationQueryDTO.java | — (新建) | `.../modules/system/service/dto/` |
| OrganizationQueryCriteria.java | — (新建) | `.../backend/service/dto/` |

**留在 grid-app 的文件：**
- `OrganizationServiceImpl.java` — 实现（依赖 grid-app 的 GridUser、AppTokenProvider 等）
- `AgentServiceImpl.java` — 实现
- `AppInstitutionController.java` — 用户侧
- `AppAgentController.java` — 用户侧

**从 grid-bootstrap 删除的文件：**
- `InstitutionAuditController.java`
- `AgentAuditController.java`

> 注意：`OrganizationServiceImpl` 和 `AgentServiceImpl` 的 `@Service` 扫描路径在 `grid-app` 中（`com.naon.grid.modules.app`），被 `grid-bootstrap` 的 `@SpringBootApplication` 扫描到。迁移后 grid-system 的包路径为 `com.naon.grid.modules.system`，需要确保 `@SpringBootApplication` 的 `@ComponentScan` 或 `@EntityScan` 覆盖了此路径。`grid-system` 的包 `com.naon.grid.modules.system` 已经在 `GridApplicationRunner`（在 bootstrap）的扫描范围内（`com.naon.grid`）。

- [ ] **Step 1: 在 grid-system 创建 6 个新文件（复制并改包名）**

在 `grid-system/src/main/java/com/naon/grid/modules/system/` 下：
- `domain/GridOrganization.java` — 从 `com.naon.grid.modules.app.domain` 搬来，packge 改为 `com.naon.grid.modules.system.domain`
- `domain/GridAgent.java` — 同上
- `repository/GridOrganizationRepository.java` — package 改为 `com.naon.grid.modules.system.repository`
- `repository/GridAgentRepository.java` — 同上
- `service/OrganizationService.java` — package 改为 `com.naon.grid.modules.system.service`，import 中的 `GridOrganization` 改为 `com.naon.grid.modules.system.domain.GridOrganization`
- `service/AgentService.java` — 同上

同时 `OrganizationService.java` 的方法签名要保持不变（实现类在 grid-app 中）

- [ ] **Step 2: 删除 grid-app 中的原文件**

从 `grid-app/src/main/java/com/naon/grid/modules/app/` 删除：
- `domain/GridOrganization.java`
- `domain/GridAgent.java`
- `repository/GridOrganizationRepository.java`
- `repository/GridAgentRepository.java`
- `service/OrganizationService.java`
- `service/AgentService.java`

- [ ] **Step 3: 更新 grid-app 中 OrganizationServiceImpl 的 import**

`OrganizationServiceImpl.java` 中的 import 改为：
```java
import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.repository.GridOrganizationRepository;
import com.naon.grid.modules.system.service.OrganizationService;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
```
其他对 grid-app 本地类的引用不变（`GridUser`、`GridUserRole`、`GridUserRepository`、`AppTokenProvider` 等）。

同样更新 `AgentServiceImpl.java` 的 import。

- [ ] **Step 4: 更新 AppInstitutionController 和 AppAgentController 的 import**

将 `OrganizationService` 的 import 改为 `com.naon.grid.modules.system.service.OrganizationService`
将 `InstitutionRegisterDTO` 的 import 改为 `com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO`

- [ ] **Step 5: 删除 grid-bootstrap 中的旧 Controller**

删除：
- `grid-bootstrap/.../rest/InstitutionAuditController.java`
- `grid-bootstrap/.../rest/AgentAuditController.java`

- [ ] **Step 6: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/domain/GridOrganization.java \
      grid-system/src/main/java/com/naon/grid/modules/system/domain/GridAgent.java \
      grid-system/src/main/java/com/naon/grid/modules/system/repository/GridOrganizationRepository.java \
      grid-system/src/main/java/com/naon/grid/modules/system/repository/GridAgentRepository.java \
      grid-system/src/main/java/com/naon/grid/modules/system/service/OrganizationService.java \
      grid-system/src/main/java/com/naon/grid/modules/system/service/AgentService.java \
      grid-system/src/main/java/com/naon/grid/modules/system/rest/InstitutionAuditController.java \
      grid-system/src/main/java/com/naon/grid/modules/system/rest/AgentAuditController.java
      # 还有删除操作
git rm grid-app/src/main/java/com/naon/grid/modules/app/domain/GridOrganization.java \
      grid-app/src/main/java/com/naon/grid/modules/app/domain/GridAgent.java \
      grid-app/src/main/java/com/naon/grid/modules/app/repository/GridOrganizationRepository.java \
      grid-app/src/main/java/com/naon/grid/modules/app/repository/GridAgentRepository.java \
      grid-app/src/main/java/com/naon/grid/modules/app/service/OrganizationService.java \
      grid-app/src/main/java/com/naon/grid/modules/app/service/AgentService.java \
      grid-bootstrap/src/main/java/com/naon/grid/modules/system/rest/InstitutionAuditController.java \
      grid-bootstrap/src/main/java/com/naon/grid/modules/system/rest/AgentAuditController.java
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/OrganizationServiceImpl.java \
      grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AgentServiceImpl.java \
      grid-app/src/main/java/com/naon/grid/modules/app/rest/AppInstitutionController.java \
      grid-app/src/main/java/com/naon/grid/modules/app/rest/AppAgentController.java
git commit -m "refactor: move organization/agent entities and interfaces from grid-app to grid-system"
```

---

### Task 1: 数据模型变更 — GridOrganization 新增字段

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/modules/system/domain/GridOrganization.java`
- Modify: `sql/normal_user.sql`

**Interfaces:**
- Consumes: (none)
- Produces: `GridOrganization` 实体新增 `adminPassword`, `rejectReason`, `referredBy` 三个字段

- [ ] **Step 1: 修改 GridOrganization.java，新增三个字段**

```java
// 在 GridOrganization.java 中添加：

@Column(length = 100)
private String adminPassword;

@Column(length = 500)
private String rejectReason;

@Column(length = 32)
private String referredBy;
```

- [ ] **Step 2: 更新 normal_user.sql 的 grid_organization CREATE TABLE**

在 `grid_organization` 表的 CREATE TABLE 中，在 `contact_phone` 之后、`country` 之前插入三个新字段：

```sql
`admin_password`   VARCHAR(100) COMMENT '管理员密码(BCrypt)，审核通过后使用',
`reject_reason`    VARCHAR(500) COMMENT '审核驳回原因',
`referred_by`      VARCHAR(32) COMMENT '申请时填写的邀请码',
```

即在 `grid_organization` 表的字段列表中找到 `contact_phone` 行，在其后插入这三个字段定义。

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/domain/GridOrganization.java sql/normal_user.sql
git commit -m "feat: add admin_password, reject_reason, referred_by fields to GridOrganization"
```

---

### Task 2: DTO 变更 — 更新注册 DTO + 新增查询 DTO

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/service/dto/InstitutionRegisterDTO.java`（覆盖原 grid-app 版本）
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/service/dto/ApplicationQueryDTO.java`
- Create: `grid-system/src/main/java/com/naon/grid/backend/service/dto/OrganizationQueryCriteria.java` (admin 后台查询条件)

**Interfaces:**
- Consumes: (none)
- Produces: `InstitutionRegisterDTO`（更新）、`ApplicationQueryDTO`（新增）、`OrganizationQueryCriteria`（新增）

- [ ] **Step 1: 在 grid-system 创建 InstitutionRegisterDTO**

在 `grid-system/src/main/java/com/naon/grid/modules/system/service/dto/InstitutionRegisterDTO.java` 创建：
移除 `contactEmail`、`deviceId`、`deviceName`，新增 `referredBy`：

```java
package com.naon.grid.modules.system.service.dto;

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

    private String contactPhone;

    @NotBlank(message = "管理员邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String adminEmail;

    @NotBlank(message = "管理员密码不能为空")
    private String adminPassword;

    private String referredBy;
}
```

然后删除 `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/InstitutionRegisterDTO.java`（如果有旧版本）。

- [ ] **Step 2: 在 grid-system 创建 ApplicationQueryDTO**

```java
package com.naon.grid.modules.system.service.dto;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
public class ApplicationQueryDTO {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 3: 在 grid-system 创建 OrganizationQueryCriteria（后台管理分页查询条件）**

```java
package com.naon.grid.backend.service.dto;

import com.naon.grid.annotation.Query;
import lombok.Data;
import java.io.Serializable;

@Data
public class OrganizationQueryCriteria implements Serializable {

    @Query
    private String auditStatus;

    @Query(blurry = "name,nameEn,contactName,contactEmail")
    private String blurry;
}
```

- [ ] **Step 4: 更新 GridOrganizationRepository**

`GridOrganizationRepository.java` 在 grid-system 中已创建，添加 `findByContactEmail` 方法：

```java
Optional<GridOrganization> findByContactEmail(String contactEmail);
```

- [ ] **Step 5: 更新 AppInstitutionController 的 DTO import**

```java
// 改为引用 grid-system 的 DTO
import com.naon.grid.modules.system.service.dto.ApplicationQueryDTO;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
```

- [ ] **Step 6: Commit**

```bash
git add \
  grid-system/src/main/java/com/naon/grid/modules/system/service/dto/InstitutionRegisterDTO.java \
  grid-system/src/main/java/com/naon/grid/modules/system/service/dto/ApplicationQueryDTO.java \
  grid-system/src/main/java/com/naon/grid/backend/service/dto/OrganizationQueryCriteria.java \
  grid-system/src/main/java/com/naon/grid/modules/system/repository/GridOrganizationRepository.java \
  grid-app/src/main/java/com/naon/grid/modules/app/rest/AppInstitutionController.java \
  grid-app/src/main/java/com/naon/grid/modules/app/service/impl/OrganizationServiceImpl.java
git rm grid-app/src/main/java/com/naon/grid/modules/app/service/dto/InstitutionRegisterDTO.java 2>/dev/null || true
git commit -m "feat: update institution DTOs and add query criteria"
```

---

### Task 3: 创建邮件发送服务

**Files:**
- Modify: `grid-tools/pom.xml`
- Create: `grid-tools/src/main/java/com/naon/grid/service/EmailService.java`
- Modify: `.env.example`

**Interfaces:**
- Consumes: (none)
- Produces: `EmailService`（`sendHtmlEmail(to, subject, content)`）

- [ ] **Step 1: 在 grid-tools/pom.xml 添加 mail 依赖**

```xml
<!-- 邮件服务 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

- [ ] **Step 2: 创建 EmailService**

```java
package com.naon.grid.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("Email sent to: {}, subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}, subject: {}", to, subject, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }
}
```

- [ ] **Step 3: 在 application.yml 添加邮件配置**

在 `grid-bootstrap/src/main/resources/config/application.yml` 中，与现有 redis、datasource 等同级添加 spring.mail 配置：

```yaml
# ─── 邮件服务 ───
spring:
  mail:
    host: ${MAIL_HOST:smtp.example.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:noreply@yourroad.com}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

在 `.env.example` 中添加邮件配置项：
```
# ============================================================
# 邮件服务配置
# ============================================================
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=noreply@yourroad.com
MAIL_PASSWORD=your_mail_password
```

- [ ] **Step 4: Commit**

```bash
git add grid-tools/pom.xml \
      grid-tools/src/main/java/com/naon/grid/service/EmailService.java \
      grid-bootstrap/src/main/resources/config/application.yml \
      .env.example
git commit -m "feat: add email service for audit notifications"
```

---

### Task 4: 完善 OrganizationService 接口方法

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/modules/system/service/OrganizationService.java`

**Interfaces:**
- Consumes: (none)
- Produces: 更新后的 `OrganizationService` 接口

- [ ] **Step 1: 在 OrganizationService 接口中新增方法**

```java
package com.naon.grid.modules.system.service;

import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.service.dto.ApplicationQueryDTO;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface OrganizationService {

    /**
     * 机构申请入驻（提交申请信息，不创建用户）
     */
    void register(InstitutionRegisterDTO dto, HttpServletRequest request);

    /**
     * 查询申请状态（验证邮箱+密码后返回申请进度）
     */
    Map<String, Object> queryApplication(ApplicationQueryDTO dto);

    /**
     * 驳回后重新提交申请
     */
    void resubmit(InstitutionRegisterDTO dto, HttpServletRequest request);

    /**
     * 审核通过（后台管理员操作）
     */
    void approve(Integer orgId, String planProductCode);

    /**
     * 审核驳回
     */
    void reject(Integer orgId, String reason);

    /**
     * 根据ID查询
     */
    GridOrganization findById(Integer orgId);
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/service/OrganizationService.java
git commit -m "feat: update OrganizationService interface with new methods"
```

---

### Task 5: 实现 OrganizationServiceImpl（核心业务逻辑）

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/OrganizationServiceImpl.java`

**Interfaces:**
- Consumes: `OrganizationService` 接口、`EmailService`、
`GridOrganizationRepository`、`GridUserRepository`、`GridUserRoleRepository`、`ReferralService`
- Produces: 完整的机构审核入驻业务逻辑

- [ ] **Step 1: 注入新依赖**

```java
// 新增依赖注入
private final EmailService emailService;
private final ReferralService referralService;
```

原有 `GridUserRoleRepository` 已在现有代码中注入。

- [ ] **Step 2: 重写 register() 方法**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void register(InstitutionRegisterDTO dto, HttpServletRequest request) {
    // 检查邮箱是否已被注册（包括已审核通过的机构用户）
    if (userRepository.findByEmail(dto.getAdminEmail()).isPresent()) {
        throw new BadRequestException("该邮箱已被注册");
    }

    // 检查是否有 PENDING 或 APPROVED 的申请记录
    organizationRepository.findByContactEmail(dto.getAdminEmail())
            .filter(org -> !"REJECTED".equals(org.getAuditStatus()))
            .ifPresent(org -> {
                throw new BadRequestException("该邮箱已提交过申请");
            });

    String ip = StringUtils.getIp(request);
    String region = regionResolver.resolve(ip);

    // 加密密码（解密 RSA + BCrypt 加密）
    String encryptedPassword;
    try {
        String decryptedPassword = RsaUtils.decryptByPrivateKey(
                RsaProperties.privateKey, dto.getAdminPassword());
        encryptedPassword = passwordEncoder.encode(decryptedPassword);
    } catch (Exception e) {
        throw new BadRequestException("密码解密失败");
    }

    // 创建机构申请记录（PENDING）
    GridOrganization org = new GridOrganization();
    org.setName(dto.getName());
    org.setNameEn(dto.getNameEn());
    org.setOrgType(dto.getOrgType());
    org.setContactName(dto.getContactName());
    org.setContactPhone(dto.getContactPhone());
    org.setContactEmail(dto.getAdminEmail());  // adminEmail 作为联系邮箱
    org.setReferredBy(dto.getReferredBy());
    org.setAdminPassword(encryptedPassword);
    org.setRegion(region);
    org.setAuditStatus("PENDING");
    organizationRepository.save(org);
}
```

注意：`OrganizationServiceImpl` 需要增加 import：
```java
import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
```
原有的 `GridOrganization` import 从 `com.naon.grid.modules.app.domain` 改为 `com.naon.grid.modules.system.domain`。

- [ ] **Step 3: 实现 queryApplication() 方法**

```java
@Override
public Map<String, Object> queryApplication(ApplicationQueryDTO dto) {
    GridOrganization org = organizationRepository
            .findByContactEmail(dto.getEmail())
            .orElse(null);

    if (org == null) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "NOT_FOUND");
        result.put("message", "未找到申请记录");
        return result;
    }

    // 验证密码
    String decryptedPassword;
    try {
        decryptedPassword = RsaUtils.decryptByPrivateKey(
                RsaProperties.privateKey, dto.getPassword());
    } catch (Exception e) {
        throw new BadRequestException("密码解密失败");
    }

    if (!passwordEncoder.matches(decryptedPassword, org.getAdminPassword())) {
        throw new BadRequestException("邮箱或密码不正确");
    }

    Map<String, Object> result = new HashMap<>();

    switch (org.getAuditStatus()) {
        case "PENDING":
            result.put("status", "PENDING");
            result.put("message", "您的申请正在审核中，请耐心等待");
            break;
        case "APPROVED":
            result.put("status", "APPROVED");
            result.put("message", "审核已通过，请登录");
            break;
        case "REJECTED":
            Map<String, Object> data = new HashMap<>();
            data.put("name", org.getName());
            data.put("nameEn", org.getNameEn());
            data.put("orgType", org.getOrgType());
            data.put("contactName", org.getContactName());
            data.put("contactPhone", org.getContactPhone());
            data.put("rejectReason", org.getRejectReason());
            result.put("status", "REJECTED");
            result.put("message", "审核未通过");
            result.put("data", data);
            break;
        default:
            result.put("status", "ERROR");
            result.put("message", "状态异常");
    }

    return result;
}
```

- [ ] **Step 4: 实现 resubmit() 方法**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void resubmit(InstitutionRegisterDTO dto, HttpServletRequest request) {
    GridOrganization org = organizationRepository
            .findByContactEmail(dto.getAdminEmail())
            .orElseThrow(() -> new BadRequestException("未找到申请记录"));

    if (!"REJECTED".equals(org.getAuditStatus())) {
        throw new BadRequestException("当前状态不允许重新提交");
    }

    String ip = StringUtils.getIp(request);
    String region = regionResolver.resolve(ip);

    // 重新加密密码（如果提供了新密码）
    if (dto.getAdminPassword() != null && !dto.getAdminPassword().isEmpty()) {
        try {
            String decryptedPassword = RsaUtils.decryptByPrivateKey(
                    RsaProperties.privateKey, dto.getAdminPassword());
            String encryptedPassword = passwordEncoder.encode(decryptedPassword);
            org.setAdminPassword(encryptedPassword);
        } catch (Exception e) {
            throw new BadRequestException("密码解密失败");
        }
    }

    // 更新字段
    org.setName(dto.getName());
    org.setNameEn(dto.getNameEn());
    org.setOrgType(dto.getOrgType());
    org.setContactName(dto.getContactName());
    org.setContactPhone(dto.getContactPhone());
    org.setReferredBy(dto.getReferredBy());
    org.setRegion(region);
    org.setAuditStatus("PENDING");
    org.setRejectReason(null);  // 清除驳回原因
    organizationRepository.save(org);
}
```

- [ ] **Step 5: 重写 approve() 方法**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void approve(Integer orgId, String planProductCode) {
    GridOrganization org = organizationRepository.findById(orgId)
            .orElseThrow(() -> new BadRequestException("机构不存在"));

    if (!"PENDING".equals(org.getAuditStatus())) {
        throw new BadRequestException("当前状态不允许审核通过");
    }

    // 设置机构套餐限制
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

    org.setCurrentMembers(1);
    org.setAuditStatus("APPROVED");
    organizationRepository.save(org);

    // 创建管理员用户
    String encryptedPassword = org.getAdminPassword();
    if (encryptedPassword == null) {
        throw new BadRequestException("管理员密码缺失，请联系技术支持");
    }

    GridUser admin = new GridUser();
    admin.setEmail(org.getContactEmail());
    admin.setPassword(encryptedPassword);
    admin.setNickname(org.getContactName());
    admin.setGender(0);
    admin.setUserType("INSTITUTION");
    admin.setOrgId(org.getId());
    admin.setOrgRole("ADMIN");
    admin.setRegisterAuditStatus("APPROVED");
    admin.setRegion(org.getRegion());
    admin.setStatus(1);

    // 生成机构邀请码
    String referralCode = "UR" + IdUtil.fastSimpleUUID().substring(0, 8).toUpperCase();
    admin.setReferralCode(referralCode);
    userRepository.save(admin);

    // 创建 NORMAL 角色
    GridUserRole normalRole = new GridUserRole();
    normalRole.setUserId(admin.getId());
    normalRole.setRoleCode("NORMAL");
    normalRole.setRoleName("普通用户");
    userRoleRepository.save(normalRole);

    // 处理邀请码溯源（如果申请时填了推荐码）
    if (org.getReferredBy() != null && !org.getReferredBy().isEmpty()) {
        referralService.processReferral(org.getReferredBy(), admin.getId());
    }

    // 清空 admin_password
    org.setAdminPassword(null);
    organizationRepository.save(org);

    // 发送审核通过邮件
    sendApprovalEmail(org, admin);
}
```

- [ ] **Step 6: 重写 reject() 方法**

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void reject(Integer orgId, String reason) {
    GridOrganization org = organizationRepository.findById(orgId)
            .orElseThrow(() -> new BadRequestException("机构不存在"));

    org.setAuditStatus("REJECTED");
    org.setRejectReason(reason);
    organizationRepository.save(org);

    log.info("Organization rejected: orgId={}, reason={}", orgId, reason);

    // 发送驳回邮件
    sendRejectionEmail(org, reason);
}
```

- [ ] **Step 7: 添加邮件发送辅助方法**

```java
private void sendApprovalEmail(GridOrganization org, GridUser admin) {
    String subject = "您的机构【" + org.getName() + "】入驻审核已通过";
    String content = "<p>您好，</p>"
            + "<p>您的机构【" + org.getName() + "】已通过审核，现已正式入驻有路中文平台。</p>"
            + "<p>您可以点击以下链接使用邮箱【" + admin.getEmail() + "】登录平台：<br/>"
            + "<a href=\"https://yourroad.com/login\">https://yourroad.com/login</a></p>"
            + "<p>请及时设置机构信息并开始管理您的成员。</p>";
    try {
        emailService.sendHtmlEmail(admin.getEmail(), subject, content);
    } catch (Exception e) {
        log.error("Failed to send approval email to: {}", admin.getEmail(), e);
    }
}

private void sendRejectionEmail(GridOrganization org, String reason) {
    String subject = "您的机构【" + org.getName() + "】入驻审核未通过";
    String content = "<p>您好，</p>"
            + "<p>您的机构【" + org.getName() + "】审核未通过，原因如下：</p>"
            + "<p><strong>" + reason + "</strong></p>"
            + "<p>您可根据驳回原因修改信息后<a href=\"https://yourroad.com/institution/apply\">重新提交申请</a>。</p>";
    try {
        emailService.sendHtmlEmail(org.getContactEmail(), subject, content);
    } catch (Exception e) {
        log.error("Failed to send rejection email to: {}", org.getContactEmail(), e);
    }
}
```

- [ ] **Step 8: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/OrganizationServiceImpl.java
git commit -m "feat: implement institution audit flow logic"
```

---

### Task 6: 用户侧 Controller — AppInstitutionController

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppInstitutionController.java`

- [ ] **Step 1: 重写 AppInstitutionController**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.annotation.rest.AnonymousPutMapping;
import com.naon.grid.modules.system.service.OrganizationService;
import com.naon.grid.modules.system.service.dto.ApplicationQueryDTO;
import com.naon.grid.modules.system.service.dto.InstitutionRegisterDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/institution")
@Api(tags = "用户：机构注册")
public class AppInstitutionController {

    private final OrganizationService organizationService;

    @ApiOperation("机构申请入驻")
    @AnonymousPostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Validated @RequestBody InstitutionRegisterDTO dto,
            HttpServletRequest request) {
        organizationService.register(dto, request);
        return ResponseEntity.ok(Collections.singletonMap("message", "提交成功，请等待审核"));
    }

    @ApiOperation("查询申请状态")
    @AnonymousPostMapping("/application/query")
    public ResponseEntity<Map<String, Object>> queryApplication(
            @Validated @RequestBody ApplicationQueryDTO dto) {
        return ResponseEntity.ok(organizationService.queryApplication(dto));
    }

    @ApiOperation("驳回后重新提交申请")
    @AnonymousPutMapping("/application")
    public ResponseEntity<Map<String, String>> resubmit(
            @Validated @RequestBody InstitutionRegisterDTO dto,
            HttpServletRequest request) {
        organizationService.resubmit(dto, request);
        return ResponseEntity.ok(Collections.singletonMap("message", "重新提交成功，请等待审核"));
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppInstitutionController.java
git commit -m "feat: add institution application query and resubmit endpoints"
```

---

### Task 7: 后台管理 Controller — InstitutionAuditController（在 grid-system）

**Files:**
- Create: `grid-system/src/main/java/com/naon/grid/modules/system/rest/InstitutionAuditController.java`

**Interfaces:**
- Consumes: `OrganizationService`, `GridOrganizationRepository`, `OrganizationQueryCriteria`
- Produces: 后台 4 个端点（分页列表/详情/通过/驳回）

- [ ] **Step 1: 在 grid-system 创建 InstitutionAuditController**

```java
package com.naon.grid.modules.system.rest;

import com.naon.grid.modules.system.domain.GridOrganization;
import com.naon.grid.modules.system.repository.GridOrganizationRepository;
import com.naon.grid.modules.system.service.OrganizationService;
import com.naon.grid.utils.PageResult;
import com.naon.grid.utils.PageUtil;
import com.naon.grid.utils.QueryHelp;
import com.naon.grid.backend.service.dto.OrganizationQueryCriteria;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/institutions")
@Api(tags = "系统：机构审核")
public class InstitutionAuditController {

    private final OrganizationService organizationService;
    private final GridOrganizationRepository organizationRepository;

    @ApiOperation("分页查询机构列表")
    @GetMapping
    @PreAuthorize("@el.check('institution:list')")
    public ResponseEntity<PageResult<GridOrganization>> queryAll(
            OrganizationQueryCriteria criteria, Pageable pageable) {
        Page<GridOrganization> page = organizationRepository.findAll(
                (root, query, cb) -> QueryHelp.getPredicate(root, criteria, cb),
                pageable);
        return ResponseEntity.ok(PageUtil.toPage(page));
    }

    @ApiOperation("获取机构详情")
    @GetMapping("/{id}")
    @PreAuthorize("@el.check('institution:list')")
    public ResponseEntity<GridOrganization> getDetail(@PathVariable Integer id) {
        GridOrganization org = organizationService.findById(id);
        org.setAdminPassword(null); // 不返回密码
        return ResponseEntity.ok(org);
    }

    @ApiOperation("审核通过")
    @PostMapping("/{id}/approve")
    @PreAuthorize("@el.check('institution:approve')")
    public ResponseEntity<Void> approve(@PathVariable Integer id,
                                         @RequestParam String plan) {
        organizationService.approve(id, plan);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("审核驳回")
    @PostMapping("/{id}/reject")
    @PreAuthorize("@el.check('institution:reject')")
    public ResponseEntity<Void> reject(@PathVariable Integer id,
                                        @RequestParam String reason) {
        organizationService.reject(id, reason);
        return ResponseEntity.ok().build();
    }
}
```

> 注：`@PreAuthorize` 权限控制见 Task 8 补充说明。

- [ ] **Step 2: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/rest/InstitutionAuditController.java
git commit -m "feat: create InstitutionAuditController in grid-system with pagination and detail"
```

---

### Task 8: 权限与匿名访问配置

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/modules/system/rest/InstitutionAuditController.java` (如果不需要 PreAuthorize)
- 或确认 `grid-system` 中的 Security 权限配置

- [ ] **Step 1: 确认匿名访问**

`@AnonymousPostMapping` 和 `@AnonymousPutMapping` 已存在于 `grid-common`，`AppInstitutionController` 的三个接口均已标记，匿名访问无需额外配置。

确认 WebSecurity 配置（在 `grid-system` 或 `grid-bootstrap` 中）放行了 `/api/app/institution/**` 路径。

- [ ] **Step 2: 确认后台权限**

如果项目后台使用 `@PreAuthorize("@el.check('xxx')")` 模式，需要：
- 选择是否对 `InstitutionAuditController` 添加权限控制
- 如果添加，在权限数据中注册 `institution:list`、`institution:approve`、`institution:reject` 三个权限标识

也可以先不加权限控制（保持与旧 `grid-bootstrap` 版本一致），后续再补充。

- [ ] **Step 3: Commit**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/rest/InstitutionAuditController.java
git commit -m "fix: ensure institution application endpoints are anonymously accessible"
```
