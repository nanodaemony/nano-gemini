package com.naon.grid.modules.app.service;

import com.naon.grid.modules.app.service.dto.ActivateSubscriptionDTO;
import com.naon.grid.modules.app.service.dto.AppSubscriptionVO;

public interface SubscriptionService {

    /**
     * 查询当前用户订阅状态
     */
    AppSubscriptionVO getMySubscription(Long userId);

    /**
     * 激活订阅（支付回调时调用）
     * 支持：新购、续期、升级
     */
    void activateSubscription(ActivateSubscriptionDTO dto);

    /**
     * 注册时自动发放试用会员
     */
    void grantTrial(Long userId);
}
