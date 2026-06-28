package com.naon.grid.modules.app.service.dto;

import lombok.Data;

import java.util.Date;

/**
 * 用户订阅状态 VO
 */
@Data
public class AppSubscriptionVO {
    /** 会员级别：NORMAL / VIP / SVIP */
    private String level;
    /** 过期时间，null 表示未订阅 */
    private Date expireTime;
    /** 是否即将到期（15天内） */
    private boolean expiringSoon;

    /** 订阅渠道: null=非订阅, PHOTONPAY=光子易 */
    private String subscriptionChannel;

    /** 是否开启自动续费 */
    private Boolean autoRenew;
}
