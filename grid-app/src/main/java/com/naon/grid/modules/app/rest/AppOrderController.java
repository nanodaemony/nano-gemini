package com.naon.grid.modules.app.rest;

import com.naon.grid.annotation.rest.AnonymousPostMapping;
import com.naon.grid.modules.app.utils.AppSecurityUtils;
import com.naon.grid.modules.billing.service.OrderService;
import com.naon.grid.modules.billing.service.PaymentService;
import com.naon.grid.modules.billing.service.dto.OrderCreateRequest;
import com.naon.grid.modules.billing.service.dto.OrderCreateResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/orders")
@Api(tags = "用户：订单接口")
public class AppOrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @ApiOperation("创建订单")
    @PostMapping("/create")
    public ResponseEntity<OrderCreateResponse> createOrder(
            @Validated @RequestBody OrderCreateRequest request,
            HttpServletRequest servletRequest) {
        Long userId = AppSecurityUtils.getCurrentUserId();
        String region = (String) servletRequest.getAttribute("_region");
        if (region == null) region = "C";
        OrderCreateResponse response = orderService.createOrder(userId, request, region);
        return ResponseEntity.ok(response);
    }

    @ApiOperation("支付回调（模拟）")
    @AnonymousPostMapping("/callback")
    public ResponseEntity<String> paymentCallback(@RequestBody Map<String, Object> callbackData) {
        String orderNo = (String) callbackData.get("order_no");
        String paymentMethod = (String) callbackData.get("payment_method");
        if (orderNo == null || paymentMethod == null) {
            return ResponseEntity.badRequest().body("Missing order_no or payment_method");
        }
        boolean success = paymentService.handlePaymentCallback(orderNo, paymentMethod, callbackData);
        return success ? ResponseEntity.ok("SUCCESS") : ResponseEntity.badRequest().body("FAILED");
    }
}
