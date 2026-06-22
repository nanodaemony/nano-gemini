package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.service.dto.EntitlementResult;

import java.time.LocalDateTime;

public interface EntitlementEngine {

    /**
     * 计算用户的所有产品权益到期时间
     */
    EntitlementResult compute(Long userId);

    /**
     * 检查用户是否有指定产品代码的访问权限
     * PLUS 包含所有子模块
     */
    boolean hasAccess(Long userId, String productCode);

    /**
     * 检查用户是否有指定模块的访问权限（按 moduleCode，非 productCode）
     */
    boolean hasModuleAccess(Long userId, String moduleCode);

    /**
     * 授予权益，执行堆叠计算
     *
     * @return 该产品的新的有效到期时间
     */
    LocalDateTime grant(Long userId, String sourceType, String sourceId,
                        String productCode, int durationDays, String region);

    /**
     * 撤销一条权益来源
     */
    void revoke(Long sourceId);

    /**
     * 检查用户的权益来源区域是否与当前区域匹配
     * 第一期：警告日志 + 返回 true
     */
    boolean isValidForRegion(Long userId, String currentRegion);
}
