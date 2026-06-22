package com.naon.grid.modules.app.service.impl;

import cn.hutool.core.util.IdUtil;
import com.naon.grid.config.properties.RsaProperties;
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
import java.util.Date;
import java.util.List;
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
        organizationRepository.save(org);

        // Activate admin user (find by org ID, not by email)
        List<GridUser> admins = userRepository.findByOrgIdAndOrgRole(orgId, "ADMIN");
        for (GridUser admin : admins) {
            admin.setRegisterAuditStatus("APPROVED");
            userRepository.save(admin);

            // Grant 30-day trial
            entitlementEngine.grant(
                    admin.getId(), "INSTITUTION", String.valueOf(orgId),
                    "PLUS", 30, org.getRegion()
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Integer orgId, String reason) {
        GridOrganization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("机构不存在"));
        org.setAuditStatus("REJECTED");
        organizationRepository.save(org);
        log.info("Organization rejected: orgId={}, reason={}", orgId, reason);
    }

    @Override
    public GridOrganization findById(Integer orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("机构不存在"));
    }

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

        String refreshToken = IdUtil.simpleUUID();
        Date expireTime = new Date(System.currentTimeMillis() + 2592000L * 1000);
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
