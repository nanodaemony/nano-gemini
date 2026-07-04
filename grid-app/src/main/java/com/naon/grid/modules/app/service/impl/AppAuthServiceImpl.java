package com.naon.grid.modules.app.service.impl;

import cn.hutool.core.util.IdUtil;
import com.naon.grid.config.properties.RsaProperties;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.GridUserRole;
import com.naon.grid.modules.app.domain.GridUserToken;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.repository.GridUserRoleRepository;
import com.naon.grid.modules.app.repository.GridUserTokenRepository;
import com.naon.grid.modules.app.security.AppTokenProvider;
import com.naon.grid.modules.app.security.DeviceManager;
import com.naon.grid.modules.app.service.AppAuthService;
import com.naon.grid.modules.app.service.ReferralService;
import com.naon.grid.modules.app.service.RegionResolver;
import com.naon.grid.modules.app.service.SubscriptionService;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import com.naon.grid.service.EmailService;
import com.naon.grid.modules.app.service.dto.LoginDTO;
import com.naon.grid.modules.app.service.dto.RegisterDTO;
import com.naon.grid.modules.system.service.dto.AppUserDTO;
import com.naon.grid.modules.app.service.dto.SendCodeDTO;
import com.naon.grid.modules.system.service.dto.TokenDTO;
import com.naon.grid.utils.RedisUtils;
import com.naon.grid.utils.RsaUtils;
import com.naon.grid.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuthServiceImpl implements AppAuthService {

    private final GridUserRepository userRepository;
    private final GridUserRoleRepository userRoleRepository;
    private final GridUserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppTokenProvider appTokenProvider;
    private final DeviceManager deviceManager;
    private final SubscriptionService subscriptionService;
    private final EntitlementEngine entitlementEngine;
    private final ReferralService referralService;
    private final RegionResolver regionResolver;
    private final RedisUtils redisUtils;
    private final EmailService emailService;

    @Value("${app.auth.token-expire-seconds:604800}")
    private long tokenExpireSeconds;

    @Value("${app.auth.refresh-token-expire-seconds:2592000}")
    private long refreshTokenExpireSeconds;

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

        // Process referral code if provided
        String referralCode = registerDTO.getReferralCode();
        if (referralCode != null && !referralCode.isEmpty()) {
            user.setReferredBy(referralCode);
        }

        userRepository.save(user);

        GridUserRole normalRole = new GridUserRole();
        normalRole.setUserId(user.getId());
        normalRole.setRoleCode("NORMAL");
        normalRole.setRoleName("普通用户");
        userRoleRepository.save(normalRole);

        // Record referral relationship (needs user ID)
        if (referralCode != null && !referralCode.isEmpty()) {
            referralService.processReferral(referralCode, user.getId());
        }

        // Grant trial
        try {
            entitlementEngine.grant(user.getId(), "TRIAL", null, "PLUS", 7, region);
        } catch (Exception e) {
            log.error("Failed to grant trial for userId={}", user.getId(), e);
            // Fallback: try old subscription service
            try {
                subscriptionService.grantTrial(user.getId());
            } catch (Exception ex) {
                log.error("Fallback grantTrial also failed for userId={}", user.getId(), ex);
            }
        }

        return generateToken(user, registerDTO.getDeviceId(), registerDTO.getDeviceName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO login(LoginDTO loginDTO, HttpServletRequest request) {
        GridUser user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new BadRequestException("邮箱或密码错误"));

        if (user.getStatus() == 0) {
            throw new BadRequestException("用户已被禁用");
        }

        // Check audit status for INSTITUTION/AGENT users
        if ("PENDING".equals(user.getRegisterAuditStatus())) {
            throw new BadRequestException("您的账号正在审核中，审核通过后方可登录");
        }
        if ("REJECTED".equals(user.getRegisterAuditStatus())) {
            throw new BadRequestException("您的账号审核未通过");
        }

        String decryptedPassword;
        try {
            decryptedPassword = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, loginDTO.getPassword());
        } catch (Exception e) {
            throw new BadRequestException("密码解密失败");
        }

        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            throw new BadRequestException("邮箱或密码错误");
        }

        user.setLastLoginTime(new Date());
        user.setLastLoginIp(StringUtils.getIp(request));
        // Update region on each login (region may change if user travels)
        String currentRegion = regionResolver.resolve(StringUtils.getIp(request));
        user.setRegion(currentRegion);
        userRepository.save(user);

        return generateToken(user, loginDTO.getDeviceId(), loginDTO.getDeviceName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO refreshToken(String refreshToken) {
        Optional<GridUserToken> tokenOpt = userTokenRepository.findByRefreshToken(refreshToken);
        if (!tokenOpt.isPresent()) {
            throw new BadRequestException("刷新令牌无效");
        }

        GridUserToken userToken = tokenOpt.get();
        if (userToken.getExpireTime().before(new Date())) {
            userTokenRepository.delete(userToken);
            throw new BadRequestException("刷新令牌已过期");
        }

        GridUser user = userRepository.findById(userToken.getUserId())
                .orElseThrow(() -> new BadRequestException("用户不存在"));

        userTokenRepository.delete(userToken);

        return generateToken(user, userToken.getDeviceId(), userToken.getDeviceName());
    }

    @Override
    public void sendCode(SendCodeDTO dto) {
        // 1. 检查邮箱是否已被注册
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("邮箱已被注册");
        }

        // 2. 检查冷却期（60秒内不允许重复发送）
        String cooldownKey = "email:code:cooldown:" + dto.getEmail();
        if (redisUtils.hasKey(cooldownKey)) {
            throw new BadRequestException("验证码已发送，请60秒后重试");
        }

        // 3. 生成6位数字验证码
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        // 4. 存入Redis（5分钟有效）
        String codeKey = "email:code:" + dto.getEmail();
        redisUtils.set(codeKey, code, 5, TimeUnit.MINUTES);

        // 5. 设置冷却标记（60秒）
        redisUtils.set(cooldownKey, "1", 60, TimeUnit.SECONDS);

        // 6. 发送邮件
        emailService.sendHtmlEmail(dto.getEmail(), "有路中文 - 邮箱验证码", buildCodeEmail(code));

        log.info("Verification code sent to: {}", dto.getEmail());
    }

    private String buildCodeEmail(String code) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;\">"
                + "<h2 style=\"color: #333;\">有路中文</h2>"
                + "<p>您的邮箱验证码是：</p>"
                + "<div style=\"font-size: 28px; font-weight: bold; color: #1890ff; letter-spacing: 6px; padding: 12px 0;\">"
                + code
                + "</div>"
                + "<p style=\"color: #999; font-size: 14px;\">验证码5分钟内有效，请勿转发给他人。</p>"
                + "<p style=\"color: #999; font-size: 14px;\">如非本人操作，请忽略此邮件。</p>"
                + "</div>";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(Long userId, String deviceId) {
        userTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId);
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

        Date expireTime = new Date(System.currentTimeMillis() + refreshTokenExpireSeconds * 1000);
        deviceManager.registerDevice(user.getId(), deviceId, deviceName, refreshToken, accessToken, expireTime);

        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setAccessToken(accessToken);
        tokenDTO.setRefreshToken(refreshToken);
        tokenDTO.setExpiresIn(tokenExpireSeconds);
        tokenDTO.setUser(convertToDTO(user, roles));

        return tokenDTO;
    }

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

    private String generateReferralCode(GridUserRepository userRepository) {
        String code;
        do {
            String random = IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
            code = "UR" + random;
        } while (userRepository.existsByReferralCode(code));
        return code;
    }
}
