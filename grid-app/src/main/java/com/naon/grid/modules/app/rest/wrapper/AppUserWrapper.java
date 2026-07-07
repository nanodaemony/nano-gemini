package com.naon.grid.modules.app.rest.wrapper;

import com.naon.grid.modules.app.domain.GridUser;
import com.naon.grid.modules.app.domain.GridUserAuth;
import com.naon.grid.modules.app.rest.vo.AppUserProfileVO;
import com.naon.grid.modules.app.rest.vo.AppSocialAccountVO;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AppUserWrapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 将 GridUser 实体转换为 AppUserProfileVO
     * @param user 用户实体
     * @param avatarUrl 通过 oss_resource_meta.id 解析后的头像 URL（可能为 null）
     */
    public static AppUserProfileVO toProfileVO(GridUser user, String avatarUrl) {
        AppUserProfileVO vo = new AppUserProfileVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(avatarUrl);
        vo.setGender(user.getGender());
        vo.setHskLevel(user.getHskLevel());
        vo.setSignature(user.getSignature());
        vo.setUserType(user.getUserType());
        vo.setRegion(user.getRegion());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setEmailVerified(user.getEmailVerified());
        vo.setHasPassword(user.getPassword() != null && !user.getPassword().isEmpty());
        if (user.getCreateTime() != null) {
            vo.setCreatedAt(user.getCreateTime().format(DATE_FORMATTER));
        }
        return vo;
    }

    /**
     * 将 GridUserAuth 列表转换为 AppSocialAccountVO 列表
     */
    public static List<AppSocialAccountVO> toSocialAccountVOList(List<GridUserAuth> auths) {
        if (auths == null) {
            return Collections.emptyList();
        }
        return auths.stream().map(AppUserWrapper::toSocialAccountVO).collect(Collectors.toList());
    }

    /**
     * 将 GridUserAuth 实体转换为 AppSocialAccountVO
     */
    public static AppSocialAccountVO toSocialAccountVO(GridUserAuth auth) {
        AppSocialAccountVO vo = new AppSocialAccountVO();
        vo.setId(auth.getId());
        vo.setProvider(auth.getProvider());
        vo.setProviderName(auth.getProviderName());
        vo.setProviderAvatar(auth.getProviderAvatar());
        if (auth.getCreateTime() != null) {
            vo.setCreatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(auth.getCreateTime()));
        }
        return vo;
    }

    /**
     * 对手机号进行脱敏处理：前3位 + **** + 后4位
     * @param phone 原始手机号
     * @return 脱敏后的手机号，如果为 null 则返回 null，长度不足7位则原样返回
     */
    private static String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
