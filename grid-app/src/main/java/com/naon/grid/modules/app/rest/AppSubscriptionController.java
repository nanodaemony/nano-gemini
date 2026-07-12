package com.naon.grid.modules.app.rest;

import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.modules.billing.domain.PaymentSubscription;
import com.naon.grid.modules.billing.repository.PaymentSubscriptionRepository;
import com.naon.grid.modules.billing.service.EntitlementService;
import com.naon.grid.modules.billing.service.GatewayRouter;
import com.naon.grid.modules.billing.service.PaymentGateway;
import com.naon.grid.modules.billing.service.dto.UserEntitlementVO;
import com.naon.grid.modules.billing.service.dto.UserSubscriptionVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/subscription")
@Api(tags = "用户：订阅接口")
public class AppSubscriptionController {

    private final EntitlementService entitlementService;
    private final GatewayRouter gatewayRouter;
    private final PaymentSubscriptionRepository subscriptionRepository;

    @ApiOperation("查询我的订阅/权益状态")
    @GetMapping("/my")
    public ResponseEntity<UserSubscriptionVO> getMySubscription() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        List<UserEntitlementVO> entitlements = entitlementService.getUserEntitlements(userId);

        List<PaymentSubscription> activeSubs =
                subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE");

        return ResponseEntity.ok(UserSubscriptionVO.builder()
                .entitlements(entitlements)
                .hasTrial(entitlementService.hasReceivedTrial(userId))
                .hasAutoRenew(!activeSubs.isEmpty())
                .build());
    }

    @ApiOperation("取消自动续费订阅")
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        List<PaymentSubscription> activeSubs =
                subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE");
        for (PaymentSubscription sub : activeSubs) {
            if (sub.getChannelSubId() != null) {
                PaymentGateway gateway = gatewayRouter.resolve();
                gateway.cancelSubscription(sub.getChannelSubId());
            }
            sub.setStatus("CANCELLED");
            sub.setCancelAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
        }
        return ResponseEntity.ok().build();
    }
}
