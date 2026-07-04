package com.naon.grid.modules.billing.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionVO {
    private List<UserEntitlementVO> entitlements;
    private boolean hasTrial;
    private boolean hasAutoRenew;
}
