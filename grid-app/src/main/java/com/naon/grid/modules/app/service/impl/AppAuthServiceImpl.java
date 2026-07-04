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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import com.naon.grid.modules.app.domain.GridUserAuth;
import com.naon.grid.modules.app.exception.BindEmailRequiredException;
import com.naon.grid.modules.app.repository.GridUserAuthRepository;
import com.naon.grid.modules.app.security.IdTokenVerifier;
import com.naon.grid.modules.app.security.SocialUserInfo;
import com.naon.grid.modules.app.service.dto.SocialLoginDTO;
import com.naon.grid.modules.app.service.dto.SocialBindEmailDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    private final GridUserAuthRepository userAuthRepository;
    private final IdTokenVerifier idTokenVerifier;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.auth.token-expire-seconds:604800}")
    private long tokenExpireSeconds;

    @Value("${app.auth.refresh-token-expire-seconds:2592000}")
    private long refreshTokenExpireSeconds;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO register(RegisterDTO registerDTO, HttpServletRequest request) {
        String normalizedEmail = normalizeEmail(registerDTO.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("邮箱已被注册");
        }

        // 校验邮箱验证码
        String codeKey = "email:code:" + normalizedEmail;
        String savedCode = redisUtils.getAndDel(codeKey);
        if (savedCode == null) {
            throw new BadRequestException("验证码不存在或已过期");
        }
        if (!savedCode.equals(registerDTO.getCode())) {
            throw new BadRequestException("验证码错误");
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
        user.setEmail(normalizedEmail);
        user.setEmailVerified(1);
        user.setPassword(passwordEncoder.encode(decryptedPassword));
        user.setNickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : normalizedEmail.split("@")[0]);
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
        String normalizedEmail = normalizeEmail(dto.getEmail());

        // 1. 检查邮箱是否已被注册
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("邮箱已被注册");
        }

        // 2. 检查冷却期（60秒内不允许重复发送）
        String cooldownKey = "email:code:cooldown:" + normalizedEmail;
        if (redisUtils.hasKey(cooldownKey)) {
            throw new BadRequestException("验证码已发送，请60秒后重试");
        }

        // 3. 生成6位数字验证码
        String code = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));

        // 4. 设置冷却标记（60秒）
        redisUtils.set(cooldownKey, "1", 60, TimeUnit.SECONDS);

        // 5. 发送邮件
        emailService.sendHtmlEmail(normalizedEmail, "有路中文 - 邮箱验证码", buildCodeEmail(code));

        // 6. 邮件发送成功后存入Redis（5分钟有效）
        String codeKey = "email:code:" + normalizedEmail;
        redisUtils.set(codeKey, code, 5, TimeUnit.MINUTES);

        log.info("Verification code sent to: {}", normalizedEmail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO socialLogin(SocialLoginDTO socialLoginDTO, HttpServletRequest request) {
        String provider = socialLoginDTO.getProvider().toLowerCase();
        SocialUserInfo socialUser = idTokenVerifier.verify(provider, socialLoginDTO.getIdToken());

        // 1. 查 GridUserAuth — providerId 优先匹配
        Optional<GridUserAuth> existingAuth =
                userAuthRepository.findByProviderAndProviderId(provider, socialUser.getProviderId());
        if (existingAuth.isPresent()) {
            GridUser user = userRepository.findById(existingAuth.get().getUserId())
                    .orElseThrow(() -> new BadRequestException("用户不存在"));
            if (user.getStatus() == 0) {
                throw new BadRequestException("账号已被禁用");
            }
            updateSocialAuth(existingAuth.get(), socialLoginDTO.getIdToken(), socialUser);
            updateLoginMetadata(user, request);
            fillUserProfile(user, socialUser);
            return generateToken(user, socialLoginDTO.getDeviceId(), socialLoginDTO.getDeviceName());
        }

        // 2. 无邮箱 → 需要绑定
        if (socialUser.getEmail() == null || socialUser.getEmail().isEmpty()) {
            String bindTokenId = UUID.randomUUID().toString();
            Map<String, Object> bindClaims = new HashMap<>();
            bindClaims.put("provider", provider);
            bindClaims.put("providerId", socialUser.getProviderId());
            bindClaims.put("name", socialUser.getName());
            bindClaims.put("picture", socialUser.getPicture());
            String bindJson = JSON.toJSONString(bindClaims);
            redisUtils.set("social:bind:" + bindTokenId, bindJson, 300, TimeUnit.SECONDS);
            throw new BindEmailRequiredException(bindTokenId);
        }

        // 3. 有邮箱 → 按邮箱查找或创建用户
        return createOrLinkSocialUser(provider, socialUser, socialLoginDTO.getIdToken(),
                socialLoginDTO.getDeviceId(), socialLoginDTO.getDeviceName(), request);
    }

    private TokenDTO createOrLinkSocialUser(String provider, SocialUserInfo socialUser,
                                             String idToken, String deviceId, String deviceName,
                                             HttpServletRequest request) {
        String normalizedEmail = normalizeEmail(socialUser.getEmail());
        Optional<GridUser> existingUser = userRepository.findByEmail(normalizedEmail);

        GridUser user;
        boolean isNewUser = false;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getStatus() == 0) {
                throw new BadRequestException("账号已被禁用");
            }
        } else {
            user = createSocialUser(socialUser, request);
            isNewUser = true;
        }

        // 写 GridUserAuth 关联 (defensive check + retry-on-constraint for race condition)
        if (!userAuthRepository.findByProviderAndProviderId(provider, socialUser.getProviderId()).isPresent()) {
            GridUserAuth auth = new GridUserAuth();
            auth.setUserId(user.getId());
            auth.setProvider(provider);
            auth.setProviderId(socialUser.getProviderId());
            auth.setProviderName(socialUser.getName());
            auth.setProviderAvatar(socialUser.getPicture());
            auth.setAccessToken(idToken);
            auth.setExpireTime(socialUser.getExpireTime());
            try {
                userAuthRepository.save(auth);
            } catch (DataIntegrityViolationException e) {
                log.warn("Concurrent social login detected for provider={} providerId={}, retrying", provider, socialUser.getProviderId());
                GridUserAuth existingAuth = userAuthRepository.findByProviderAndProviderId(provider, socialUser.getProviderId())
                        .orElseThrow(() -> new BadRequestException("登录验证失败"));
                user = userRepository.findById(existingAuth.getUserId())
                        .orElseThrow(() -> new BadRequestException("用户不存在"));
                if (user.getStatus() == 0) {
                    throw new BadRequestException("账号已被禁用");
                }
                updateSocialAuth(existingAuth, idToken, socialUser);
                updateLoginMetadata(user, request);
                fillUserProfile(user, socialUser);
                return generateToken(user, deviceId, deviceName);
            }
        }

        if (!isNewUser) {
            fillUserProfile(user, socialUser);
        }
        updateLoginMetadata(user, request);
        return generateToken(user, deviceId, deviceName);
    }

    private GridUser createSocialUser(SocialUserInfo socialUser, HttpServletRequest request) {
        String normalizedEmail = normalizeEmail(socialUser.getEmail());
        String ip = StringUtils.getIp(request);
        String region = regionResolver.resolve(ip);

        GridUser user = new GridUser();
        user.setEmail(normalizedEmail);
        user.setEmailVerified(socialUser.isEmailVerified() ? 1 : 0);
        user.setPassword(null);
        user.setNickname(socialUser.getName() != null ? socialUser.getName() : normalizedEmail.split("@")[0]);
        user.setAvatar(socialUser.getPicture());
        user.setGender(0);
        user.setStatus(1);
        user.setUserType("NORMAL");
        user.setRegion(region);
        user.setRegisterIp(ip);
        user.setRegisterAuditStatus("APPROVED");
        user.setReferralCode(generateReferralCode(userRepository));
        userRepository.save(user);

        GridUserRole normalRole = new GridUserRole();
        normalRole.setUserId(user.getId());
        normalRole.setRoleCode("NORMAL");
        normalRole.setRoleName("普通用户");
        userRoleRepository.save(normalRole);

        try {
            entitlementEngine.grant(user.getId(), "TRIAL", null, "PLUS", 7, region);
        } catch (Exception e) {
            log.error("Failed to grant trial for social userId={}", user.getId(), e);
            try {
                subscriptionService.grantTrial(user.getId());
            } catch (Exception ex) {
                log.error("Fallback grantTrial also failed for userId={}", user.getId(), ex);
            }
        }

        return user;
    }

    private void fillUserProfile(GridUser user, SocialUserInfo socialUser) {
        boolean changed = false;
        // Fill name/avatar if user hasn't set them yet
        if ((user.getNickname() == null || user.getNickname().isEmpty())
                && socialUser.getName() != null && !socialUser.getName().isEmpty()) {
            user.setNickname(socialUser.getName());
            changed = true;
        }
        if ((user.getAvatar() == null || user.getAvatar().isEmpty())
                && socialUser.getPicture() != null && !socialUser.getPicture().isEmpty()) {
            user.setAvatar(socialUser.getPicture());
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private void updateSocialAuth(GridUserAuth auth, String idToken, SocialUserInfo socialUser) {
        auth.setAccessToken(idToken);
        auth.setExpireTime(socialUser.getExpireTime());
        if (socialUser.getName() != null) {
            auth.setProviderName(socialUser.getName());
        }
        if (socialUser.getPicture() != null) {
            auth.setProviderAvatar(socialUser.getPicture());
        }
        userAuthRepository.save(auth);
    }

    private void updateLoginMetadata(GridUser user, HttpServletRequest request) {
        user.setLastLoginTime(new Date());
        user.setLastLoginIp(StringUtils.getIp(request));
        String currentRegion = regionResolver.resolve(StringUtils.getIp(request));
        user.setRegion(currentRegion);
        userRepository.save(user);
    }

    @Override
    public void sendBindCode(SendCodeDTO dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());

        // 不检查邮箱是否已注册（允许绑定已有账号）

        String cooldownKey = "email:code:cooldown:" + normalizedEmail;
        if (redisUtils.hasKey(cooldownKey)) {
            throw new BadRequestException("验证码已发送，请60秒后重试");
        }

        String code = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        redisUtils.set(cooldownKey, "1", 60, TimeUnit.SECONDS);
        emailService.sendHtmlEmail(normalizedEmail, "有路中文 - 邮箱验证码", buildCodeEmail(code));

        String codeKey = "email:code:" + normalizedEmail;
        redisUtils.set(codeKey, code, 5, TimeUnit.MINUTES);
        log.info("Bind verification code sent to: {}", normalizedEmail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO socialBindEmail(SocialBindEmailDTO socialBindEmailDTO, HttpServletRequest request) {
        String bindTokenId = socialBindEmailDTO.getBindToken();
        String redisKey = "social:bind:" + bindTokenId;
        String bindJson = redisUtils.getAndDel(redisKey);
        if (bindJson == null) {
            throw new BadRequestException("操作超时，请重新登录");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> bindClaims = JSON.parseObject(bindJson, Map.class);
        String provider = (String) bindClaims.get("provider");
        String providerId = (String) bindClaims.get("providerId");

        // Verify email code
        String normalizedEmail = normalizeEmail(socialBindEmailDTO.getEmail());
        String codeKey = "email:code:" + normalizedEmail;
        String savedCode = redisUtils.getAndDel(codeKey);
        if (savedCode == null) {
            throw new BadRequestException("验证码不存在或已过期");
        }
        if (!savedCode.equals(socialBindEmailDTO.getCode())) {
            throw new BadRequestException("验证码错误");
        }

        // Find or create user by email
        Optional<GridUser> existingUser = userRepository.findByEmail(normalizedEmail);
        GridUser user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (user.getStatus() == 0) {
                throw new BadRequestException("账号已被禁用");
            }
        } else {
            String ip = StringUtils.getIp(request);
            String region = regionResolver.resolve(ip);
            user = new GridUser();
            user.setEmail(normalizedEmail);
            user.setEmailVerified(1);
            user.setPassword(null);
            user.setNickname(bindClaims.get("name") != null
                    ? (String) bindClaims.get("name") : normalizedEmail.split("@")[0]);
            user.setAvatar((String) bindClaims.get("picture"));
            user.setGender(0);
            user.setStatus(1);
            user.setUserType("NORMAL");
            user.setRegion(region);
            user.setRegisterIp(ip);
            user.setRegisterAuditStatus("APPROVED");
            user.setReferralCode(generateReferralCode(userRepository));
            userRepository.save(user);

            GridUserRole normalRole = new GridUserRole();
            normalRole.setUserId(user.getId());
            normalRole.setRoleCode("NORMAL");
            normalRole.setRoleName("普通用户");
            userRoleRepository.save(normalRole);

            try {
                entitlementEngine.grant(user.getId(), "TRIAL", null, "PLUS", 7, region);
            } catch (Exception e) {
                log.error("Failed to grant trial for socialBind userId={}", user.getId(), e);
                try {
                    subscriptionService.grantTrial(user.getId());
                } catch (Exception ex) {
                    log.error("Fallback grantTrial also failed for userId={}", user.getId(), ex);
                }
            }
        }

        // Write GridUserAuth
        Optional<GridUserAuth> existingAuth =
                userAuthRepository.findByProviderAndProviderId(provider, providerId);
        if (!existingAuth.isPresent()) {
            GridUserAuth auth = new GridUserAuth();
            auth.setUserId(user.getId());
            auth.setProvider(provider);
            auth.setProviderId(providerId);
            auth.setProviderName((String) bindClaims.get("name"));
            auth.setProviderAvatar((String) bindClaims.get("picture"));
            userAuthRepository.save(auth);
        }

        updateLoginMetadata(user, request);
        return generateToken(user, socialBindEmailDTO.getDeviceId(), socialBindEmailDTO.getDeviceName());
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

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : null;
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
