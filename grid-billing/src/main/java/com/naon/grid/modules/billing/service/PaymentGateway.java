package com.naon.grid.modules.billing.service;

import com.naon.grid.modules.billing.service.dto.*;
import java.math.BigDecimal;

public interface PaymentGateway {

    PaymentCreateResponse createPayment(PaymentCreateRequest request);

    SubscriptionCreateResponse createSubscription(SubscriptionCreateRequest request);

    void cancelSubscription(String channelSubId);

    TransactionQueryResponse queryTransaction(String channelOrderId);

    RefundResponse refund(String channelOrderId, BigDecimal amount);

    boolean verifyWebhookSignature(String payload, String signature);
}
