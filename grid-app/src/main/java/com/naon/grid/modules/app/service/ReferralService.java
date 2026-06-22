package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.domain.ReferralRecord;

public interface ReferralService {
    /**
     * 处理推荐码：注册时调用，记录推荐关系
     * @return 推荐人ID（如果推荐码有效），null表示无需关联
     */
    Long processReferral(String referralCode, Long referredUserId);

    /**
     * 被推荐用户支付成功时调用，发放推荐奖励
     */
    void settleReferralReward(Long referredUserId, String orderNo);
}
