package com.naon.grid.modules.billing.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class EntitlementResult {
    private Long userId;
    private List<ProductEntitlement> productEntitlements;
    private LocalDateTime overallExpireAt;

    @Data
    @Builder
    @AllArgsConstructor
    public static class ProductEntitlement {
        private String productCode;
        private LocalDateTime expireAt;
        private boolean active;
    }
}
