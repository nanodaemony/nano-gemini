package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.service.dto.UserEntitlementVO;

import java.time.LocalDateTime;
import java.util.List;

public interface EntitlementService {

    /**
     * 批量授予权益，在同一事务内写流水+更新汇总
     *
     * @param userId        用户ID
     * @param entitlementIds 权益ID列表
     * @param sourceType    来源类型 TRIAL/PURCHASE/ADMIN_GRANT
     * @param sourceId      来源业务ID（订单号等）
     * @param durationDays  有效天数
     * @param region        授予时的区域
     */
    void grantEntitlements(Long userId, List<Integer> entitlementIds,
                          String sourceType, String sourceId,
                          int durationDays, String region);

    /**
     * 检查用户是否有指定模块的访问权限
     */
    boolean hasModuleAccess(Long userId, String moduleCode);

    /**
     * 检查用户是否已领过试用
     */
    boolean hasReceivedTrial(Long userId);

    /**
     * 获取用户所有权益状态
     */
    List<UserEntitlementVO> getUserEntitlements(Long userId);
}
