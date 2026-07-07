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

        String codeKey = "email:code:" + user.getEmail();
        String savedCode = redisUtils.getAndDel(codeKey);
        if (savedCode == null) {
            throw new BadRequestException("验证码不存在或已过期");
        }
        if (!savedCode.equals(request.getEmailCode())) {
            throw new BadRequestException("验证码错误");
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
