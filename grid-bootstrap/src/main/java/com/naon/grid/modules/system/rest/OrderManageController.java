package com.naon.grid.modules.system.rest;

import com.naon.grid.exception.BadRequestException;
import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.repository.GridOrderRepository;
import com.naon.grid.modules.billing.service.PaymentGateway;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Api(tags = "系统：订单管理")
public class OrderManageController {

    private final GridOrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    @ApiOperation("订单列表")
    @GetMapping
    public ResponseEntity<Page<GridOrder>> listOrders(Pageable pageable) {
        return ResponseEntity.ok(orderRepository.findAll(pageable));
    }

    @ApiOperation("订单详情")
    @GetMapping("/{orderNo}")
    public ResponseEntity<GridOrder> getOrder(@PathVariable String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @ApiOperation("订单退款")
    @PostMapping("/{orderNo}/refund")
    public ResponseEntity<Void> refundOrder(@PathVariable String orderNo,
                                             @RequestParam(required = false) BigDecimal amount) {
        GridOrder order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BadRequestException("订单不存在: " + orderNo));
        paymentGateway.refund(order.getChannelOrderId(), amount);
        order.setStatus("REFUNDING");
        orderRepository.save(order);
        return ResponseEntity.ok().build();
    }
}
