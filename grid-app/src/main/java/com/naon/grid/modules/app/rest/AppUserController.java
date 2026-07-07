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
