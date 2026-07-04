package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.service.dto.LoginDTO;
import com.naon.grid.modules.app.service.dto.RegisterDTO;
import com.naon.grid.modules.app.service.dto.SendCodeDTO;
import com.naon.grid.modules.app.service.dto.SocialBindEmailDTO;
import com.naon.grid.modules.app.service.dto.SocialLoginDTO;
import com.naon.grid.modules.system.service.dto.TokenDTO;

import javax.servlet.http.HttpServletRequest;

public interface AppAuthService {
    TokenDTO register(RegisterDTO registerDTO, HttpServletRequest request);
    TokenDTO login(LoginDTO loginDTO, HttpServletRequest request);
    void logout(Long userId, String deviceId);
    TokenDTO refreshToken(String refreshToken);
    void sendCode(SendCodeDTO dto);

    TokenDTO socialLogin(SocialLoginDTO socialLoginDTO, HttpServletRequest request);
    void sendBindCode(SendCodeDTO dto);
    TokenDTO socialBindEmail(SocialBindEmailDTO socialBindEmailDTO, HttpServletRequest request);
}
