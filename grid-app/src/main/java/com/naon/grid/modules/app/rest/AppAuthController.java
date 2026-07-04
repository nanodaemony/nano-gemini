package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.Log;
import com.naon.grid.annotation.rest.AnonymousGetMapping;
import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.config.properties.RsaProperties;
import com.naon.grid.modules.app.service.AppAuthService;
import com.naon.grid.modules.app.service.dto.LoginDTO;
import com.naon.grid.modules.app.service.dto.RefreshTokenDTO;
import com.naon.grid.modules.app.service.dto.RegisterDTO;
import com.naon.grid.modules.app.service.dto.SendCodeDTO;
import com.naon.grid.modules.system.service.dto.TokenDTO;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.utils.RsaUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import com.naon.grid.modules.app.exception.BindEmailRequiredException;
import com.naon.grid.modules.app.service.dto.SocialBindEmailDTO;
import com.naon.grid.modules.app.service.dto.SocialLoginDTO;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/auth")
@Api(tags = "用户：认证接口")
public class AppAuthController {

    private final AppAuthService appAuthService;

    @Log("APP用户注册")
    @ApiOperation("用户注册")
    @AnonymousPostMapping("/register")
    public ResponseEntity<TokenDTO> register(@Validated @RequestBody RegisterDTO registerDTO,
                                              HttpServletRequest request) {
        TokenDTO tokenDTO = appAuthService.register(registerDTO, request);
        return ResponseEntity.ok(tokenDTO);
    }

    @Log("APP用户登录")
    @ApiOperation("用户登录")
    @AnonymousPostMapping("/login")
    public ResponseEntity<TokenDTO> login(@Validated @RequestBody LoginDTO loginDTO,
                                           HttpServletRequest request) {
        TokenDTO tokenDTO = appAuthService.login(loginDTO, request);
        return ResponseEntity.ok(tokenDTO);
    }

    @Log("APP刷新Token")
    @ApiOperation("刷新Token")
    @AnonymousPostMapping("/refresh")
    public ResponseEntity<TokenDTO> refreshToken(@Validated @RequestBody RefreshTokenDTO refreshTokenDTO) {
        TokenDTO tokenDTO = appAuthService.refreshToken(refreshTokenDTO.getRefreshToken());
        return ResponseEntity.ok(tokenDTO);
    }

    @Log("APP用户退出")
    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String deviceId) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        appAuthService.logout(userId, deviceId);
        return ResponseEntity.ok().build();
    }

    @Log("发送邮箱验证码")
    @ApiOperation("发送邮箱验证码")
    @AnonymousPostMapping("/send-code")
    public ResponseEntity<Map<String, String>> sendCode(@Validated @RequestBody SendCodeDTO sendCodeDTO) {
        appAuthService.sendCode(sendCodeDTO);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送，5分钟内有效");
        return ResponseEntity.ok(result);
    }

    @Log("APP第三方登录")
    @ApiOperation("第三方登录")
    @AnonymousPostMapping("/social-login")
    public ResponseEntity<?> socialLogin(@Validated @RequestBody SocialLoginDTO socialLoginDTO,
                                          HttpServletRequest request) {
        try {
            TokenDTO tokenDTO = appAuthService.socialLogin(socialLoginDTO, request);
            return ResponseEntity.ok(tokenDTO);
        } catch (BindEmailRequiredException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("requireBindEmail", true);
            result.put("bindToken", e.getBindToken());
            return ResponseEntity.ok(result);
        }
    }

    @Log("发送邮箱绑定验证码")
    @ApiOperation("第三方登录-发送邮箱绑定验证码")
    @AnonymousPostMapping("/send-bind-code")
    public ResponseEntity<Map<String, String>> sendBindCode(@Validated @RequestBody SendCodeDTO sendCodeDTO) {
        appAuthService.sendBindCode(sendCodeDTO);
        Map<String, String> result = new HashMap<>();
        result.put("message", "验证码已发送，5分钟内有效");
        return ResponseEntity.ok(result);
    }

    @Log("APP第三方登录邮箱绑定")
    @ApiOperation("第三方登录-绑定邮箱")
    @AnonymousPostMapping("/social-bind-email")
    public ResponseEntity<TokenDTO> socialBindEmail(@Validated @RequestBody SocialBindEmailDTO socialBindEmailDTO,
                                                     HttpServletRequest request) {
        TokenDTO tokenDTO = appAuthService.socialBindEmail(socialBindEmailDTO, request);
        return ResponseEntity.ok(tokenDTO);
    }

    @ApiOperation("获取RSA公钥")
    @AnonymousGetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        Map<String, String> result = new HashMap<>();
        result.put("publicKey", getPublicKeyFromPrivate(RsaProperties.privateKey));
        return ResponseEntity.ok(result);
    }

    private String getPublicKeyFromPrivate(String privateKeyBase64) {
        try {
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            if (privateKey instanceof RSAPrivateCrtKey) {
                RSAPrivateCrtKey rsaPrivateCrtKey = (RSAPrivateCrtKey) privateKey;
                java.security.spec.RSAPublicKeySpec publicKeySpec =
                    new java.security.spec.RSAPublicKeySpec(
                        rsaPrivateCrtKey.getModulus(),
                        rsaPrivateCrtKey.getPublicExponent()
                    );
                java.security.PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
                return Base64.getEncoder().encodeToString(publicKey.getEncoded());
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive public key", e);
        }
    }
}
