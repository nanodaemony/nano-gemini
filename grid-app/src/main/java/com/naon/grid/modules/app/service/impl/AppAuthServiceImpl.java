package com.naon.grid.modules.app.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.naon.grid.config.properties.RsaProperties;
import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.enums.AppErrorCode;
import com.naon.grid.modules.app.enums.AppUserStatus;
import com.naon.grid.modules.app.enums.Gender;
import com.naon.grid.modules.app.repository.GridUserRepository;
import com.naon.grid.modules.app.security.DeviceManager;
import com.naon.grid.modules.app.service.AppAuthService;
import com.naon.grid.modules.app.service.dto.*;
import com.naon.grid.utils.RedisUtils;
import com.naon.grid.utils.RsaUtils;
import com.naon.grid.utils.StringUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuthServiceImpl implements AppAuthService {

    private final GridUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceManager deviceManager;
    private final RedisUtils redisUtils;

    @Value("${app.auth.token-validity-in-seconds:604800}")
    private long tokenValidityInSeconds;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenDTO register(RegisterDTO registerDTO, HttpServletRequest request) {
        // 检查用户名
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new BadRequestException(AppErrorCode.USERNAME_EXISTS.getMessage());
        }
        // 检查手机号
        if (userRepository.existsByPhone(registerDTO.getPhone())) {
            throw new BadRequestException(AppErrorCode.PHONE_EXISTS.getMessage());
        }
        // 解密密码
        String decryptedPassword;
        try {
            decryptedPassword = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, registerDTO.getPassword());
        } catch (Exception e) {
            throw new BadRequestException("密码解密失败");
        }
        // 创建用户
        GridUser user = new GridUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(decryptedPassword));
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setNickname(StrUtil.isNotBlank(registerDTO.getNickname()) ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setGender(Gender.UNKNOWN.getCode());
        user.setStatus(AppUserStatus.ENABLED.getCode());
        user.setRegisterIp(StringUtils.getIp(request));
        userRepository.save(user);
        // 生成Token
        return generateToken(user, registerDTO.getDeviceId());
    }

    @Override
    public TokenDTO login(LoginDTO loginDTO, HttpServletRequest request) {
        GridUser user = userRepository.findByPhone(loginDTO.getPhone())
                .orElseThrow(() -> new BadRequestException(AppErrorCode.INVALID_CREDENTIALS.getMessage()));
        if (AppUserStatus.DISABLED.getCode().equals(user.getStatus())) {
            throw new BadRequestException(AppErrorCode.USER_DISABLED.getMessage());
        }
        String decryptedPassword;
        try {
            decryptedPassword = RsaUtils.decryptByPrivateKey(RsaProperties.privateKey, loginDTO.getPassword());
        } catch (Exception e) {
            throw new BadRequestException("密码解密失败");
        }
        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            throw new BadRequestException(AppErrorCode.INVALID_CREDENTIALS.getMessage());
        }
        user.setLastLoginTime(new Date());
        user.setLastLoginIp(StringUtils.getIp(request));
        userRepository.save(user);
        return generateToken(user, loginDTO.getDeviceId());
    }

    @Override
    public void logout(Long userId, String deviceId) {
        deviceManager.removeDevice(userId, deviceId);
    }

    @Override
    public TokenDTO refreshToken(String refreshToken) {
        // 简化实现，实际需要解析refresh token
        return null;
    }

    private TokenDTO generateToken(GridUser user, String deviceId) {
        // 简化实现
        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setToken("mock_token_" + user.getId());
        tokenDTO.setRefreshToken("mock_refresh_" + user.getId());
        tokenDTO.setExpiresIn(tokenValidityInSeconds);
        tokenDTO.setUser(convertToDTO(user));
        deviceManager.registerDevice(user.getId(), deviceId, tokenDTO.getToken());
        return tokenDTO;
    }

    private AppUserDTO convertToDTO(GridUser user) {
        AppUserDTO dto = new AppUserDTO();
        BeanUtil.copyProperties(user, dto);
        if (StrUtil.isNotBlank(user.getPhone())) {
            dto.setPhone(user.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        }
        return dto;
    }
}
