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
import java.util.Date;
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
                user.getId(), user.getEmail(), deviceId, roles);

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
