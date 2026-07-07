# App 用户个人中心实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 App 端用户个人中心完整功能：个人信息查询/更新、头像上传、修改密码、账号注销、三方账号绑定/解绑，以及新增 HSK 等级和个性签名字段。

**Architecture:** 在 grid-app 模块新增 `AppUserController` + `AppUserService`，遵循项目现有 Controller→Service→Repository 三层架构和 Wrapper 模式。所有接口使用 `/api/app/user` 基础路径，需要 App Token 认证。

**Tech Stack:** Spring Boot 2.7.18, Spring Data JPA, BCrypt, RSA, JWT, Lombok, Fastjson2

## Global Constraints

- Java 1.8，Lombok `@Getter/@Setter`，不使用 `@ManyToOne`/数据库外键
- Controller 中不含转换逻辑，所有转换在 `wrapper/` 包下 `public static` 方法完成
- 密码使用 RSA 解密 + BCrypt 验证
- 头像改为 BIGINT 类型存储 `oss_resource_meta.id`
- HSK 等级 VARCHAR(20) DEFAULT '0'，不修改 `HskLevelEnum` 枚举
- 邮箱不可通过更新接口修改
- 修改密码和注销后清除所有设备 token 强制重新登录
- 注销需密码+邮箱验证码双重验证
- 直接修改 `sql/normal_user.sql` 建表语句，不需要数据迁移

---

### Task 1: 修改数据库建表语句

**Files:**
- Modify: `sql/normal_user.sql:15`

- [ ] **Step 1: 修改 `grid_user` 表 `avatar` 字段**

将 `sql/normal_user.sql` 第 15 行从：
```sql
`avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
```
改为：
```sql
`avatar` BIGINT DEFAULT NULL COMMENT '头像OSS资源ID',
```

- [ ] **Step 2: 在 `gender` 字段后新增 `hsk_level` 和 `signature`**

在第 16 行 `gender` 之后插入：
```sql
`hsk_level` VARCHAR(20) DEFAULT '0' COMMENT 'HSK等级，默认0表示未设置',
`signature` VARCHAR(200) DEFAULT NULL COMMENT '个性签名',
```

完整变更后的 `grid_user` 关键字段顺序：
```sql
`nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
`avatar` BIGINT DEFAULT NULL COMMENT '头像OSS资源ID',
`gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
`hsk_level` VARCHAR(20) DEFAULT '0' COMMENT 'HSK等级，默认0表示未设置',
`signature` VARCHAR(200) DEFAULT NULL COMMENT '个性签名',
`user_type` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '用户类型 NORMAL/INSTITUTION/AGENT',
```

- [ ] **Step 3: 提交**

```bash
git add sql/normal_user.sql
git commit -m "feat: change avatar to BIGINT, add hsk_level and signature columns"
```

---

### Task 2: 修改 GridUser 实体

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/domain/GridUser.java:52-56`

- [ ] **Step 1: 修改 avatar 字段类型**

将 `GridUser.java` 中 avatar 字段（约第 52 行）：
```java
@Column(name = "avatar", length = 500)
private String avatar;
```
改为：
```java
@Column(name = "avatar")
private Long avatar;
```

- [ ] **Step 2: 新增 hskLevel 和 signature 字段**

在 `gender` 字段后面添加：
```java
@Column(name = "hsk_level", length = 20)
private String hskLevel = "0";

@Column(name = "signature", length = 200)
private String signature;
```

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/domain/GridUser.java
git commit -m "feat: update GridUser entity - avatar to Long, add hskLevel and signature"
```

---

### Task 3: 修改 RegisterDTO 新增 HSK 和签名字段

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/RegisterDTO.java`

- [ ] **Step 1: 新增字段**

在 `RegisterDTO.java` 的 `referralCode` 字段后面添加：
```java
private String hskLevel;

@Size(max = 200, message = "个性签名长度不能超过200")
private String signature;
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/RegisterDTO.java
git commit -m "feat: add hskLevel and signature fields to RegisterDTO"
```

---

### Task 4: 修改 AppAuthServiceImpl 适配新字段

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java`

- [ ] **Step 1: 在 register() 方法中设置新字段**

在 `register()` 方法中，`user.setRegion(region);` 之前添加：

```java
// 设置HSK等级和个性签名
user.setHskLevel(registerDTO.getHskLevel() != null ? registerDTO.getHskLevel() : "0");
user.setSignature(registerDTO.getSignature());
```

- [ ] **Step 2: 修改 createSocialUser() 方法**

`createSocialUser()` 方法中第 369 行，将：
```java
user.setAvatar(socialUser.getPicture());
```
改为（avatar 现在是 Long 类型，社交头像 URL 不能直接存入）：
```java
// 社交登录用户头像不再直接写入 avatar（avatar 现在是 BIGINT OSS ID 类型）
// 社交头像 URL 存储在 GridUserAuth.providerAvatar 中
```

- [ ] **Step 3: 修改 fillUserProfile() 方法**

`fillUserProfile()` 方法中（约第 417 行），将检查 avatar 的代码：
```java
if ((user.getAvatar() == null || user.getAvatar().isEmpty())
        && socialUser.getPicture() != null && !socialUser.getPicture().isEmpty()) {
    user.setAvatar(socialUser.getPicture());
    changed = true;
}
```
改为（Long 类型的 null 检查）：
```java
if (user.getAvatar() == null
        && socialUser.getPicture() != null && !socialUser.getPicture().isEmpty()) {
    // 社交头像 URL 不直接存入 avatar 字段（avatar 现在是 BIGINT OSS ID）
    // 仅存入 GridUserAuth.providerAvatar
}
```

- [ ] **Step 4: 修改 socialBindEmail() 方法中的 setAvatar**

在 `socialBindEmail()` 方法中（约第 510 行），将：
```java
user.setAvatar((String) bindClaims.get("picture"));
```
改为：
```java
// 社交登录用户头像不直接设置 avatar 字段
```

- [ ] **Step 5: 修改 convertToDTO() 方法**

在 `convertToDTO()` 方法中（约第 617 行），avatar 的 getter 返回 Long 类型，但 `AppUserDTO.avatar` 也是 Long 类型（Task 5 会改）：
```java
dto.setAvatar(user.getAvatar());
```
（此行不变，但需确保 `AppUserDTO.avatar` 在 Task 5 中已改为 Long 类型）

- [ ] **Step 6: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppAuthServiceImpl.java
git commit -m "feat: adapt AppAuthServiceImpl for avatar Long type and new user fields"
```

---

### Task 5: 修改 AppUserDTO

**Files:**
- Modify: `grid-system/src/main/java/com/naon/grid/modules/system/service/dto/AppUserDTO.java`

- [ ] **Step 1: 修改 avatar 类型并新增字段**

将 `AppUserDTO.java` 完整改为：

```java
package com.naon.grid.modules.system.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class AppUserDTO {

    private Long id;
    private String email;
    private String nickname;
    private Long avatar;
    private Integer gender;
    private String hskLevel;
    private String signature;
    private List<String> roles;
    private String userType;
    private String orgRole;
    private String region;
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-system/src/main/java/com/naon/grid/modules/system/service/dto/AppUserDTO.java
git commit -m "feat: update AppUserDTO - avatar to Long, add hskLevel and signature"
```

---

### Task 6: 新增 Token 批量删除能力

**Files:**
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/repository/GridUserTokenRepository.java`
- Modify: `grid-app/src/main/java/com/naon/grid/modules/app/security/DeviceManager.java`

- [ ] **Step 1: GridUserTokenRepository 新增 deleteByUserId**

在 `GridUserTokenRepository.java` 中添加：
```java
void deleteByUserId(Long userId);
```

- [ ] **Step 2: DeviceManager 新增 removeAllDevices**

在 `DeviceManager.java` 中添加方法：
```java
public void removeAllDevices(Long userId) {
    gridUserTokenRepository.deleteByUserId(userId);
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/repository/GridUserTokenRepository.java
git add grid-app/src/main/java/com/naon/grid/modules/app/security/DeviceManager.java
git commit -m "feat: add removeAllDevices to DeviceManager for password change and account deletion"
```

---

### Task 7: 创建 VO 类

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppUserProfileVO.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppSocialAccountVO.java`

- [ ] **Step 1: 创建 AppUserProfileVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppUserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像URL")
    private String avatarUrl;

    @ApiModelProperty("性别：0-未知 1-男 2-女")
    private Integer gender;

    @ApiModelProperty("HSK等级")
    private String hskLevel;

    @ApiModelProperty("个性签名")
    private String signature;

    @ApiModelProperty("用户类型")
    private String userType;

    @ApiModelProperty("所属区域")
    private String region;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("邮箱是否验证：0-否 1-是")
    private Integer emailVerified;

    @ApiModelProperty("是否有密码（社交登录用户无密码）")
    private Boolean hasPassword;

    @ApiModelProperty("注册时间")
    private String createdAt;
}
```

- [ ] **Step 2: 创建 AppSocialAccountVO**

```java
package com.naon.grid.modules.app.rest.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class AppSocialAccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("绑定记录ID")
    private Long id;

    @ApiModelProperty("第三方平台")
    private String provider;

    @ApiModelProperty("第三方平台用户名")
    private String providerName;

    @ApiModelProperty("第三方平台头像")
    private String providerAvatar;

    @ApiModelProperty("绑定时间")
    private String createdAt;
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppUserProfileVO.java
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/vo/AppSocialAccountVO.java
git commit -m "feat: add AppUserProfileVO and AppSocialAccountVO"
```

---

### Task 8: 创建 Request DTO

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/UpdateProfileRequest.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/UpdateAvatarRequest.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/ChangePasswordRequest.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/DeleteAccountRequest.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/dto/BindSocialRequest.java`

- [ ] **Step 1: 创建 UpdateProfileRequest**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    private Integer gender;

    @Size(max = 20, message = "HSK等级长度不能超过20")
    private String hskLevel;

    @Size(max = 200, message = "个性签名长度不能超过200")
    private String signature;
}
```

- [ ] **Step 2: 创建 UpdateAvatarRequest**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UpdateAvatarRequest {

    @NotNull(message = "图片ID不能为空")
    private Long ossImageId;
}
```

- [ ] **Step 3: 创建 ChangePasswordRequest**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
```

- [ ] **Step 4: 创建 DeleteAccountRequest**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

@Data
public class DeleteAccountRequest {

    private String password;

    private String emailCode;
}
```

- [ ] **Step 5: 创建 BindSocialRequest**

```java
package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class BindSocialRequest {

    @NotBlank(message = "第三方平台不能为空")
    private String provider;

    @NotBlank(message = "身份令牌不能为空")
    private String idToken;
}
```

- [ ] **Step 6: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/UpdateProfileRequest.java
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/UpdateAvatarRequest.java
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/ChangePasswordRequest.java
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/DeleteAccountRequest.java
git add grid-app/src/main/java/com/naon/grid/modules/app/service/dto/BindSocialRequest.java
git commit -m "feat: add request DTOs for user profile operations"
```

---

### Task 9: 创建 AppUserWrapper

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppUserWrapper.java`

- [ ] **Step 1: 创建 AppUserWrapper**

```java
package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.GridUserAuth;
import com.naon.grid.modules.app.rest.vo.AppUserProfileVO;
import com.naon.grid.modules.app.rest.vo.AppSocialAccountVO;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AppUserWrapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 将 GridUser 实体转换为 AppUserProfileVO
     * @param user 用户实体
     * @param avatarUrl 通过 oss_resource_meta.id 解析后的头像 URL（可能为 null）
     */
    public static AppUserProfileVO toProfileVO(GridUser user, String avatarUrl) {
        AppUserProfileVO vo = new AppUserProfileVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(avatarUrl);
        vo.setGender(user.getGender());
        vo.setHskLevel(user.getHskLevel());
        vo.setSignature(user.getSignature());
        vo.setUserType(user.getUserType());
        vo.setRegion(user.getRegion());
        vo.setPhone(user.getPhone());
        vo.setEmailVerified(user.getEmailVerified());
        vo.setHasPassword(user.getPassword() != null && !user.getPassword().isEmpty());
        if (user.getCreateTime() != null) {
            vo.setCreatedAt(user.getCreateTime().format(DATE_FORMATTER));
        }
        return vo;
    }

    /**
     * 将 GridUserAuth 列表转换为 AppSocialAccountVO 列表
     */
    public static List<AppSocialAccountVO> toSocialAccountVOList(List<GridUserAuth> auths) {
        if (auths == null) {
            return Collections.emptyList();
        }
        return auths.stream().map(AppUserWrapper::toSocialAccountVO).collect(Collectors.toList());
    }

    /**
     * 将 GridUserAuth 实体转换为 AppSocialAccountVO
     */
    public static AppSocialAccountVO toSocialAccountVO(GridUserAuth auth) {
        AppSocialAccountVO vo = new AppSocialAccountVO();
        vo.setId(auth.getId());
        vo.setProvider(auth.getProvider());
        vo.setProviderName(auth.getProviderName());
        vo.setProviderAvatar(auth.getProviderAvatar());
        if (auth.getCreateTime() != null) {
            vo.setCreatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(auth.getCreateTime()));
        }
        return vo;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/wrapper/AppUserWrapper.java
git commit -m "feat: add AppUserWrapper for entity-to-VO conversion"
```

---

### Task 10: 创建 AppUserService 接口与实现

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/AppUserService.java`
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppUserServiceImpl.java`

- [ ] **Step 1: 创建 AppUserService 接口**

```java
package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.rest.vo.AppUserProfileVO;
import com.naon.grid.modules.app.rest.vo.AppSocialAccountVO;
import com.naon.grid.modules.app.service.dto.*;

import java.util.List;

public interface AppUserService {

    /**
     * 查询当前用户个人信息
     */
    AppUserProfileVO getProfile(Long userId);

    /**
     * 更新个人信息（昵称、性别、HSK等级、签名）
     */
    AppUserProfileVO updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 更新头像
     */
    AppUserProfileVO updateAvatar(Long userId, UpdateAvatarRequest request);

    /**
     * 修改密码
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 注销账号
     */
    void deleteAccount(Long userId, DeleteAccountRequest request);

    /**
     * 查询已绑定的三方账号
     */
    List<AppSocialAccountVO> getSocialAccounts(Long userId);

    /**
     * 绑定三方账号
     */
    AppSocialAccountVO bindSocialAccount(Long userId, BindSocialRequest request);

    /**
     * 解绑三方账号
     */
    void unbindSocialAccount(Long userId, Long authId);
}
```

- [ ] **Step 2: 创建 AppUserServiceImpl 实现**

```java
package com.naon.grid.modules.app.service.impl;

import com.naon.grid.config.properties.RsaProperties;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.GridUserAuth;
import com.naon.grid.modules.app.repository.GridUserAuthRepository;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.rest.vo.AppUserProfileVO;
import com.naon.grid.modules.app.rest.vo.AppSocialAccountVO;
import com.naon.grid.modules.app.rest.wrapper.AppUserWrapper;
import com.naon.grid.modules.app.security.DeviceManager;
import com.naon.grid.modules.app.security.IdTokenVerifier;
import com.naon.grid.modules.app.security.SocialUserInfo;
import com.naon.grid.modules.app.service.AppUserService;
import com.naon.grid.modules.app.service.dto.*;
import com.naon.grid.service.OssResourceService;
import com.naon.grid.service.dto.AliOssStorageDto;
import com.naon.grid.utils.RedisUtils;
import com.naon.grid.utils.RsaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {

    private final GridUserRepository userRepository;
    private final GridUserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceManager deviceManager;
    private final IdTokenVerifier idTokenVerifier;
    private final OssResourceService ossResourceService;
    private final RedisUtils redisUtils;

    @Override
    public AppUserProfileVO getProfile(Long userId) {
        GridUser user = findUserById(userId);
        String avatarUrl = resolveAvatarUrl(user.getAvatar());
        return AppUserWrapper.toProfileVO(user, avatarUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppUserProfileVO updateProfile(Long userId, UpdateProfileRequest request) {
        GridUser user = findUserById(userId);

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getHskLevel() != null) {
            user.setHskLevel(request.getHskLevel());
        }
        if (request.getSignature() != null) {
            user.setSignature(request.getSignature());
        }

        userRepository.save(user);
        String avatarUrl = resolveAvatarUrl(user.getAvatar());
        return AppUserWrapper.toProfileVO(user, avatarUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppUserProfileVO updateAvatar(Long userId, UpdateAvatarRequest request) {
        GridUser user = findUserById(userId);

        // 验证 OSS 资源存在
        AliOssStorageDto ossDto = ossResourceService.findById(request.getOssImageId());
        if (ossDto == null) {
            throw new BadRequestException("图片资源不存在");
        }

        user.setAvatar(request.getOssImageId());
        userRepository.save(user);

        String avatarUrl = ossDto.getFileUrl();
        return AppUserWrapper.toProfileVO(user, avatarUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        GridUser user = findUserById(userId);

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new BadRequestException("社交登录用户无密码，请先设置密码");
        }

        // RSA 解密
        String decryptedOldPassword;
        String decryptedNewPassword;
        try {
            decryptedOldPassword = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, request.getOldPassword());
            decryptedNewPassword = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, request.getNewPassword());
        } catch (Exception e) {
            throw new BadRequestException("密码解密失败");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(decryptedOldPassword, user.getPassword())) {
            throw new BadRequestException("旧密码不正确");
        }

        // BCrypt 加密新密码
        user.setPassword(passwordEncoder.encode(decryptedNewPassword));
        userRepository.save(user);

        // 清除所有设备 token，强制重新登录
        deviceManager.removeAllDevices(userId);
        log.info("Password changed for userId={}, all devices logged out", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        GridUser user = findUserById(userId);

        // 邮箱验证码验证
        if (request.getEmailCode() == null || request.getEmailCode().isEmpty()) {
            throw new BadRequestException("邮箱验证码不能为空");
        }

        if (user.getEmailVerified() == 0) {
            throw new BadRequestException("请先验证邮箱");
        }

        String codeKey = "email:code:" + user.getEmail();
        String savedCode = redisUtils.getAndDel(codeKey);
        if (savedCode == null) {
            throw new BadRequestException("验证码不存在或已过期");
        }
        if (!savedCode.equals(request.getEmailCode())) {
            throw new BadRequestException("验证码错误");
        }

        // 密码验证（社交登录用户无密码可跳过）
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                throw new BadRequestException("密码不能为空");
            }
            String decryptedPassword;
            try {
                decryptedPassword = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, request.getPassword());
            } catch (Exception e) {
                throw new BadRequestException("密码解密失败");
            }
            if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
                throw new BadRequestException("密码不正确");
            }
        }

        // 软删除
        user.setStatus(0);
        userRepository.save(user);

        // 清除所有设备 token
        deviceManager.removeAllDevices(userId);
        log.info("Account deleted for userId={}", userId);
    }

    @Override
    public List<AppSocialAccountVO> getSocialAccounts(Long userId) {
        List<GridUserAuth> auths = userAuthRepository.findByUserId(userId);
        return AppUserWrapper.toSocialAccountVOList(auths);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppSocialAccountVO bindSocialAccount(Long userId, BindSocialRequest request) {
        String provider = request.getProvider().toLowerCase();
        SocialUserInfo socialUser = idTokenVerifier.verify(provider, request.getIdToken());

        // 检查是否已被其他用户绑定
        userAuthRepository.findByProviderAndProviderId(provider, socialUser.getProviderId())
                .ifPresent(existing -> {
                    if (!existing.getUserId().equals(userId)) {
                        throw new BadRequestException("该第三方账号已绑定其他用户");
                    }
                    throw new BadRequestException("该第三方账号已绑定当前用户");
                });

        GridUser user = findUserById(userId);

        // 创建绑定
        GridUserAuth auth = new GridUserAuth();
        auth.setUserId(userId);
        auth.setProvider(provider);
        auth.setProviderId(socialUser.getProviderId());
        auth.setProviderName(socialUser.getName());
        auth.setProviderAvatar(socialUser.getPicture());
        auth.setAccessToken(request.getIdToken());
        auth.setExpireTime(socialUser.getExpireTime());

        // 若用户无昵称，从社交信息填充
        if ((user.getNickname() == null || user.getNickname().isEmpty())
                && socialUser.getName() != null && !socialUser.getName().isEmpty()) {
            user.setNickname(socialUser.getName());
            userRepository.save(user);
        }
        // 头像不写入 GridUser.avatar（avatar 现在是 BIGINT OSS ID）

        try {
            userAuthRepository.save(auth);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("该第三方账号已绑定其他用户");
        }

        return AppUserWrapper.toSocialAccountVO(auth);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindSocialAccount(Long userId, Long authId) {
        GridUserAuth auth = userAuthRepository.findById(authId)
                .orElseThrow(() -> new BadRequestException("绑定记录不存在"));

        if (!auth.getUserId().equals(userId)) {
            throw new BadRequestException("无权操作此绑定记录");
        }

        // 安全检查：至少保留一种登录方式
        GridUser user = findUserById(userId);
        boolean hasPassword = user.getPassword() != null && !user.getPassword().isEmpty();
        List<GridUserAuth> userAuths = userAuthRepository.findByUserId(userId);

        if (!hasPassword && userAuths.size() <= 1) {
            throw new BadRequestException("至少需要保留一种登录方式，请先设置密码");
        }

        userAuthRepository.delete(auth);
        log.info("Social account unbound: userId={}, provider={}, providerId={}",
                userId, auth.getProvider(), auth.getProviderId());
    }

    private GridUser findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("用户不存在"));
    }

    private String resolveAvatarUrl(Long avatarId) {
        if (avatarId == null) {
            return null;
        }
        try {
            AliOssStorageDto ossDto = ossResourceService.findById(avatarId);
            return ossDto != null ? ossDto.getFileUrl() : null;
        } catch (Exception e) {
            log.warn("Failed to resolve avatar URL for avatarId={}", avatarId, e);
            return null;
        }
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/service/AppUserService.java
git add grid-app/src/main/java/com/naon/grid/modules/app/service/impl/AppUserServiceImpl.java
git commit -m "feat: add AppUserService with profile, avatar, password, account, and social account operations"
```

---

### Task 11: 创建 AppUserController

**Files:**
- Create: `grid-app/src/main/java/com/naon/grid/modules/app/rest/AppUserController.java`

- [ ] **Step 1: 创建 AppUserController**

```java
package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.Log;
import com.naon.grid.modules.app.rest.vo.AppUserProfileVO;
import com.naon.grid.modules.app.rest.vo.AppSocialAccountVO;
import com.naon.grid.modules.app.service.AppUserService;
import com.naon.grid.modules.app.service.dto.*;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/user")
@Api(tags = "用户：个人中心接口")
public class AppUserController {

    private final AppUserService appUserService;

    @ApiOperation("查询个人信息")
    @GetMapping("/profile")
    public ResponseEntity<AppUserProfileVO> getProfile() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(appUserService.getProfile(userId));
    }

    @Log("更新个人信息")
    @ApiOperation("更新个人信息")
    @PutMapping("/profile")
    public ResponseEntity<AppUserProfileVO> updateProfile(
            @Validated @RequestBody UpdateProfileRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(appUserService.updateProfile(userId, request));
    }

    @Log("更新头像")
    @ApiOperation("更新头像")
    @PutMapping("/avatar")
    public ResponseEntity<AppUserProfileVO> updateAvatar(
            @Validated @RequestBody UpdateAvatarRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(appUserService.updateAvatar(userId, request));
    }

    @Log("修改密码")
    @ApiOperation("修改密码")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Validated @RequestBody ChangePasswordRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        appUserService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @Log("注销账号")
    @ApiOperation("注销账号")
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @Validated @RequestBody DeleteAccountRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        appUserService.deleteAccount(userId, request);
        return ResponseEntity.ok().build();
    }

    @ApiOperation("查询已绑定的三方账号")
    @GetMapping("/social-accounts")
    public ResponseEntity<List<AppSocialAccountVO>> getSocialAccounts() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(appUserService.getSocialAccounts(userId));
    }

    @Log("绑定三方账号")
    @ApiOperation("绑定三方账号")
    @PostMapping("/social-accounts")
    public ResponseEntity<AppSocialAccountVO> bindSocialAccount(
            @Validated @RequestBody BindSocialRequest request) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        return new ResponseEntity<>(
                appUserService.bindSocialAccount(userId, request), HttpStatus.OK);
    }

    @Log("解绑三方账号")
    @ApiOperation("解绑三方账号")
    @DeleteMapping("/social-accounts/{authId}")
    public ResponseEntity<Void> unbindSocialAccount(@PathVariable Long authId) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        appUserService.unbindSocialAccount(userId, authId);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add grid-app/src/main/java/com/naon/grid/modules/app/rest/AppUserController.java
git commit -m "feat: add AppUserController with 8 user profile endpoints"
```

---

### Task 12: 编译验证

- [ ] **Step 1: 编译整个项目**

```bash
cd C:\Users\nano\Desktop\nano-gemini
mvn clean compile -DskipTests
```
Expected: BUILD SUCCESS，所有模块编译通过。

- [ ] **Step 2: 修复编译错误（如有）**

根据编译输出修复任何类型不匹配或缺失引用问题。

- [ ] **Step 3: 提交修复（如有）**

```bash
git add -A
git commit -m "fix: compilation fixes for user profile feature"
```
