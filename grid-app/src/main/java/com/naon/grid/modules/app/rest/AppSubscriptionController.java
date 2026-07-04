package com.naon.grid.modules.app.rest;

import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.modules.billing.domain.PaymentSubscription;
import com.naon.grid.modules.billing.repository.PaymentSubscriptionRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import com.naon.grid.modules.billing.service.PaymentGateway;
import com.naon.grid.modules.billing.service.dto.UserEntitlementVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/subscription")
@Api(tags = "用户：订阅接口")
public class AppSubscriptionController {

    private final EntitlementService entitlementService;
    private final PaymentGateway paymentGateway;
    private final PaymentSubscriptionRepository subscriptionRepository;

    @ApiOperation("查询我的订阅/权益状态")
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMySubscription() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        List<UserEntitlementVO> entitlements = entitlementService.getUserEntitlements(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("entitlements", entitlements);
        result.put("hasTrial", entitlementService.hasReceivedTrial(userId));

        List<PaymentSubscription> activeSubs =
                subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE");
        result.put("hasAutoRenew", !activeSubs.isEmpty());

        return ResponseEntity.ok(result);
    }

    @ApiOperation("取消自动续费订阅")
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        List<PaymentSubscription> activeSubs =
                subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE");
        for (PaymentSubscription sub : activeSubs) {
            if (sub.getChannelSubId() != null) {
                paymentGateway.cancelSubscription(sub.getChannelSubId());
            }
            sub.setStatus("CANCELLED");
            sub.setCancelAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
        }
        return ResponseEntity.ok().build();
    }
}
