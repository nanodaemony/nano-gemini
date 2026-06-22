package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.domain.GridOrder;
import com.naon.grid.modules.billing.service.dto.OrderCreateRequest;
import com.naon.grid.modules.billing.service.dto.OrderCreateResponse;
import java.util.Optional;

public interface OrderService {
    OrderCreateResponse createOrder(Long userId, OrderCreateRequest request, String region);
    Optional<GridOrder> findByOrderNo(String orderNo);
    GridOrder save(GridOrder order);
}
