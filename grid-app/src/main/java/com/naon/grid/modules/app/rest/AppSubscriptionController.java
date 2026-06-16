package com.naon.grid.modules.app.rest;

import com.naon.grid.modules.app.service.SubscriptionService;
import com.naon.grid.modules.app.service.dto.AppSubscriptionVO;
import com.naon.grid.modules.app.service.dto.CreateOrderDTO;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/subscription")
@Api(tags = "用户：订阅接口")
public class AppSubscriptionController {

    private final SubscriptionService subscriptionService;

    @ApiOperation("查询我的订阅状态")
    @GetMapping("/my")
    public ResponseEntity<AppSubscriptionVO> getMySubscription() {
        Long userId = AppSecurityUtils.getCurrentUserId();
        AppSubscriptionVO vo = subscriptionService.getMySubscription(userId);
        return ResponseEntity.ok(vo);
    }

    @ApiOperation("创建订阅订单（预留）")
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@Validated @RequestBody CreateOrderDTO dto) {
        Long userId = AppSecurityUtils.getCurrentUserId();

        // V1 简化：仅验证参数并返回 orderId，不做实际支付处理
        // 后续对接 StoreKit / Google Play / 支付宝时完善
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", System.currentTimeMillis());
        result.put("level", dto.getLevel());
        result.put("periodMonths", dto.getPeriodMonths());
        result.put("userId", userId);
        return ResponseEntity.ok(result);
    }
}
