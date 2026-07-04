package com.naon.grid.modules.billing.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementResult {
    private Long userId;
    private List<UserEntitlementVO> entitlements;
    private LocalDateTime overallExpireAt;
    private boolean hasTrial;
}
