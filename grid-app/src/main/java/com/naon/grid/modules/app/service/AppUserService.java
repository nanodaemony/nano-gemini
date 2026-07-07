package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.rest.vo.AppUserProfileVO;
import com.naon.grid.modules.app.rest.vo.AppSocialAccountVO;
import com.naon.grid.modules.app.service.dto.*;

import java.util.List;

public interface AppUserService {

    /**
     * 查询当前用户个人信息
     */
    AppUserProfileVO getProfile(Long userId);

    /**
     * 更新个人信息（昵称、性别、HSK等级、签名）
     */
    AppUserProfileVO updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 更新头像
     */
    AppUserProfileVO updateAvatar(Long userId, UpdateAvatarRequest request);

    /**
     * 修改密码
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 注销账号
     */
    void deleteAccount(Long userId, DeleteAccountRequest request);

    /**
     * 查询已绑定的三方账号
     */
    List<AppSocialAccountVO> getSocialAccounts(Long userId);

    /**
     * 绑定三方账号
     */
    AppSocialAccountVO bindSocialAccount(Long userId, BindSocialRequest request);

    /**
     * 解绑三方账号
     */
    void unbindSocialAccount(Long userId, Long authId);
}
