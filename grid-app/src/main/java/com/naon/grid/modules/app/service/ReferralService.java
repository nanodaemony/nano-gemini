package com.naon.grid.modules.app.service;

public interface ReferralService {

    /**
     * 记录邀请事件（普通用户被邀请）
     * @param referralCode 邀请码
     * @param referredUserId 被邀请人用户ID
     * @param eventType 事件类型 REGISTER / SUBSCRIBE
     * @return 邀请人ID，无效邀请码返回 null
     */
    Long recordEvent(String referralCode, Long referredUserId, String eventType);

    /**
     * 记录邀请事件（机构被邀请）
     * @param referredOrgId 被邀请机构ID
     */
    Long recordEvent(String referralCode, Long referredUserId, String eventType, Integer referredOrgId);

    /**
     * 每日结算邀请奖励（由定时任务调用）
     */
    void settlePendingRewards();
}
