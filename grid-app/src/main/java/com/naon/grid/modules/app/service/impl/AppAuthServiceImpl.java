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
import com.naon.grid.modules.app.service.dto.*;
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

        GridUser user = new GridUser();
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(decryptedPassword));
        user.setNickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : registerDTO.getEmail().split("@")[0]);
        user.setGender(0);
        user.setStatus(1);
        user.setRegisterIp(StringUtils.getIp(request));
        user = userRepository.save(user);

        GridUserRole normalRole = new GridUserRole();
        normalRole.setUserId(user.getId());
        normalRole.setRoleCode("NORMAL");
        normalRole.setRoleName("普通用户");
        userRoleRepository.save(normalRole);

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
    @Transactional(rollbackFor = Exception.class)
    public void logout(Long userId, String deviceId) {
        userTokenRepository.deleteByUserIdAndDeviceId(userId, deviceId);
    }

    private TokenDTO generateToken(GridUser user, String deviceId, String deviceName) {
        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(GridUserRole::getRoleCode)
                .collect(Collectors.toList());

        String accessToken = appTokenProvider.createToken(user.getId(), user.getEmail(), deviceId, roles);
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
        return dto;
    }
}
