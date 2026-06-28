package com.naon.grid.modules.app.rest;

import com.naon.grid.modules.app.service.SubscriptionService;
import com.naon.grid.modules.app.service.dto.AppSubscriptionVO;
import com.naon.grid.modules.app.service.dto.CreateOrderDTO;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.modules.billing.domain.PaymentSubscription;
import com.naon.grid.modules.billing.repository.PaymentSubscriptionRepository;
import com.naon.grid.modules.billing.service.EntitlementEngine;
import com.naon.grid.modules.billing.service.PaymentGateway;
import com.naon.grid.modules.billing.service.dto.EntitlementResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/subscription")
@Api(tags = "用户：订阅接口")
public class AppSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final EntitlementEngine entitlementEngine;
    private final PaymentGateway paymentGateway;
    private final PaymentSubscriptionRepository subscriptionRepository;

    @ApiOperation("查询我的订阅状态")
    @GetMapping("/my")
    public ResponseEntity<AppSubscriptionVO> getMySubscription() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        EntitlementResult result = entitlementEngine.compute(userId);

        AppSubscriptionVO vo = new AppSubscriptionVO();
        if (result.getOverallExpireAt() != null) {
            vo.setLevel("VIP");
            vo.setExpireTime(Date.from(
                    result.getOverallExpireAt()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()));
            vo.setExpiringSoon(LocalDateTime.now()
                    .plusDays(15).isAfter(result.getOverallExpireAt()));
        } else {
            vo.setLevel("NORMAL");
        }

        List<PaymentSubscription> activeSubs = subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE");
        if (!activeSubs.isEmpty()) {
            vo.setSubscriptionChannel("PHOTONPAY");
            vo.setAutoRenew(true);
        }

        return ResponseEntity.ok(vo);
    }

    @ApiOperation("取消自动续费订阅")
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        Optional<PaymentSubscription> sub = subscriptionRepository
                .findByUserIdAndProductCodeAndStatus(userId, "PLUS", "ACTIVE");
        if (sub.isPresent()) {
            paymentGateway.cancelSubscription(sub.get().getChannelSubId());
            sub.get().setStatus("CANCELLED");
            sub.get().setCancelAt(LocalDateTime.now());
            subscriptionRepository.save(sub.get());
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation("创建订阅订单（预留）")
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@Validated @RequestBody CreateOrderDTO dto) {
        Long userId = AppSecurityUtils.getCurrentUserId();

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", System.currentTimeMillis());
        result.put("level", dto.getLevel());
        result.put("periodMonths", dto.getPeriodMonths());
        result.put("userId", userId);
        return ResponseEntity.ok(result);
    }
}
