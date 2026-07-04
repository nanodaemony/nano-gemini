package com.naon.grid.modules.billing.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntitlementVO {
    private Integer entitlementId;
    private String entitlementCode;
    private String entitlementName;
    private String moduleCode;
    private LocalDateTime expireAt;
    private boolean active;
}
