package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.domain.PaymentRecord;
import java.util.Map;

public interface PaymentService {
    /**
     * 处理支付回调。Phase 1: 模拟成功，实际发放权益。
     * 真实接入时各平台（WeChat/Alipay/Stripe）实现此方法的内部逻辑。
     */
    boolean handlePaymentCallback(String orderNo, String paymentMethod, Map<String, Object> callbackData);
}
