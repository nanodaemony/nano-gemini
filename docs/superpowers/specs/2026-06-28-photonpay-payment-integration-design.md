# 有路中文 YourRoad PhotonPay 海外支付集成设计

> **日期**: 2026-06-28
> **状态**: 设计稿
> **涉及模块**: grid-billing（修改）、grid-app（修改）、grid-common（修改）

---

## 1. 概述

### 1.1 背景

有路中文（YourRoad）是一个面向全球中文学习者的多模态教学资源平台。目前已完成区域分层定价体系（A-E 五区）、权益堆叠引擎（EntitlementEngine）的开发和 Mock 支付流程的搭建。**当前支付流程为阶段一 Mock 实现**，需要替换为真实的海外支付渠道。

### 1.2 目标

- 接入 PhotonPay（光子易）作为海外支付渠道，替换当前的 Mock 支付
- 支持海外用户通过 **Visa/Mastercard 信用卡** 和 **Apple Pay/Google Pay** 支付
- 支持**订阅模式**的自动循环扣款（recurring billing）
- 支持退款、订单查询、财务对账
- 支持海外用户的电子收据（Receipt / Invoice）
- 对接中国公司合法合规纳税
- 为移动端（App Store / Google Play IAP）预留扩展点

### 1.3 对接平台

| 项目 | 内容 |
|------|------|
| 支付平台 | **PhotonPay（光子易）** — Checkout 收单产品 |
| 卡组覆盖 | Visa, Mastercard, Discover/Diners Club |
| 移动支付 | Apple Pay, Google Pay（通过 Mastercard MDES 令牌化） |
| 结算账户 | 中国对公银行账户，合规结汇 |
| 接入方式 | RESTful API + Webhook |

---

## 2. 系统架构

### 2.1 模块职责

```
                     ┌──────────────────────┐
                     │     PhotonPay         │
                     │    (支付渠道)          │
                     └──────┬───────────────┘
                            │ REST API + Webhook
                            ▼
┌──────────────────────────────────────────────────┐
│              grid-billing（支付核心）               │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  │
│  │PhotonPay   │  │OrderService│  │Payment     │  │
│  │PaymentGate │  │(创建订单)   │  │Service     │  │
│  │(新增)      │  │            │  │(回调处理)   │  │
│  └────────────┘  └────────────┘  └─────┬──────┘  │
│                                        │         │
│  ┌────────────┐  ┌─────────────────────┘         │
│  │Product     │  │  EntitlementEngine              │
│  │Service     │  │  (权益发放)                      │
│  └────────────┘  └────────────────────────────────┘
└──────────────────────────────────────────────────┘
                        │
                        ▼
┌──────────────────────────────────────────────────┐
│              grid-app（业务层）                     │
│  ┌────────────────┐  ┌────────────────────────┐  │
│  │AppOrderControll│  │AppSubscriptionControll │  │
│  │er (下单/回调)   │  │er (订阅状态查询)        │  │
│  └────────────────┘  └────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

### 2.2 调用流程

```
┌─────────┐   ┌──────────────┐   ┌────────────┐   ┌───────────┐
│  Browser │   │  AppOrder    │   │  Payment   │   │ PhotonPay │
│   (Web)  │   │  Controller  │   │  Service   │   │           │
└────┬─────┘   └──────┬───────┘   └─────┬──────┘   └─────┬─────┘
     │ POST /create    │                  │                │
     │ (productCode,   │                  │                │
     │  billingCycle)  │                  │                │
     │────────────────►│                  │                │
     │                 │ createOrder()    │                │
     │                 │─────────────────►│                │
     │                 │                  │                │
     │                 │  调 PhotonPay    │                │
     │                 │  /checkout       │                │
     │                 │─────────────────────────────────►│
     │                 │                  │                │
     │                 │ 返回 paymentUrl  │                │
     │                 │◄─────────────────────────────────│
     │                 │                  │                │
     │ 返回 {redirect  │                  │                │
     │  Url, orderNo}  │                  │                │
     │◄────────────────│                  │                │
     │                 │                  │                │
     │ 跳转支付收银台    │                  │                │
     │──────────────────────────────────────────────────►│
     │                 │                  │                │
     │                 │                  │    Webhook     │
     │                 │   handleCallback  │◄──────────────│
     │                 │◄─────────────────│                │
     │                 │                  │                │
     │                 │  更新订单 PAID    │                │
     │                 │  grant权益        │                │
     │                 │                  │                │
     │  轮询订单状态    │                  │                │
     │────────────────►│                  │                │
     │◄────────────────│                  │                │
```

---

## 3. 数据模型变更

### 3.1 新增表：支付渠道订阅关联

用于存储 PhotonPay 返回的订阅 ID，用于后续管理自动续费。

```sql
CREATE TABLE payment_subscription (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    order_id        BIGINT NOT NULL COMMENT '首单ID',
    product_code    VARCHAR(50) NOT NULL COMMENT '产品代码',
    billing_cycle   VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY/YEARLY',
    region          VARCHAR(10) NOT NULL COMMENT '区域',
    channel         VARCHAR(30) NOT NULL COMMENT '支付渠道: PHOTONPAY',
    channel_sub_id  VARCHAR(200) COMMENT 'PhotonPay 订阅ID',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/CANCELLED/EXPIRED',
    next_billing_at DATETIME COMMENT '下次扣款时间',
    last_charged_at DATETIME COMMENT '最近扣款时间',
    cancel_at       DATETIME COMMENT '取消时间',
    create_time     DATETIME NOT NULL,
    update_time     DATETIME,
    INDEX idx_user (user_id),
    INDEX idx_channel_sub (channel, channel_sub_id)
);
```

> **注意**: 此表仅存储 PhotonPay 订阅的关联信息，订阅的**权益管理**完全由现有的 `entitlement_source` 表处理。

### 3.2 GridOrder 表扩展

在现有 `grid_order` 表中新增字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `channel` | VARCHAR(30) | 支付渠道: `PHOTONPAY`（当前唯一值），后续可扩展 |
| `channel_order_id` | VARCHAR(200) | PhotonPay 侧的订单ID，用于对账和退款 |
| `channel_sub_id` | VARCHAR(200) | PhotonPay 订阅ID（仅订阅订单有值） |
| `invoice_no` | VARCHAR(64) | 发票/收据编号 |

### 3.3 PaymentRecord 表增强

现有 `payment_record` 表字段已基本满足需求，在 `raw_callback` 中存储完整 PhotonPay 回调 JSON 用于对账。

### 3.4 现有表无需变更

以下表使用现有的结构即可：

| 表名 | 说明 |
|------|------|
| `grid_product` | 产品定义，已有的 `PLUS`/`VOCAB`/`INST_STARTER` 等代码不变 |
| `region_pricing` | 区域定价，已有的 A-E 五区定价不变 |
| `entitlement_source` | 权益堆叠，支付成功后的权益发放逻辑不变 |
| `grid_order` | 扩展后使用 |
| `payment_record` | 不变 |

---

## 4. 支付集成设计

### 4.1 新增接口：PaymentGateway

创建支付渠道抽象接口，当前仅 PhotonPay 实现，后续可扩展其他渠道。

```java
package com.naon.grid.modules.billing.service;

public interface PaymentGateway {

    /**
     * 创建支付（标准一次性支付）
     * @return 支付链接
     */
    PaymentCreateResponse createPayment(PaymentCreateRequest request);

    /**
     * 创建订阅支付（自动续费）
     */
    SubscriptionCreateResponse createSubscription(SubscriptionCreateRequest request);

    /**
     * 取消订阅（用户手动取消）
     */
    void cancelSubscription(String channelSubId);

    /**
     * 查询单笔交易状态
     */
    TransactionQueryResponse queryTransaction(String channelOrderId);

    /**
     * 发起退款
     */
    RefundResponse refund(String channelOrderId, BigDecimal amount);

    /**
     * 验证 Webhook 签名
     */
    boolean verifyWebhookSignature(String payload, String signature);
}
```

### 4.2 PhotonPay 集成实现

```java
package com.naon.grid.modules.billing.service.impl;

@Service
@ConditionalOnProperty(name = "billing.payment.gateway", havingValue = "photonpay")
public class PhotonPayPaymentGateway implements PaymentGateway {

    // ─── 配置项（见第 8 章配置管理）──────────────
    // billing.photonpay.api-key
    // billing.photonpay.api-secret
    // billing.photonpay.webhook-secret
    // billing.photonpay.base-url (沙箱: https://sandbox.photonpay.com, 生产: https://api.photonpay.com)
    // billing.photonpay.merchant-display-name = YourRoad 有路中文
    // billing.photonpay.return-url = https://yourroad.com/payment/return
    // billing.photonpay.webhook-url = https://yourroad.com/api/app/payments/webhook

    @Override
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        // 1. 调用 PhotonPay /checkout API
        //    参数: amount, currency, orderNo, description, returnUrl, cancelUrl
        // 2. 返回 paymentUrl（用户跳转支付页面）
        // 3. 将 PhotonPay 侧订单ID 写入 channel_order_id
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        // 使用 webhook-secret 验证 HMAC-SHA256 签名
    }
}
```

### 4.3 OrderService 修改

现有 `OrderServiceImpl.createOrder()` 修改为：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request, String region) {
    // 1. 校验产品 + 定价（不变）
    GridProduct product = productService.findByCode(request.getProductCode())...
    RegionPricing pricing = pricingRepository.findByProductIdAndRegionAndBillingCycle()...

    // 2. 创建 grid_order（status=PENDING）（不变）
    GridOrder order = new GridOrder();
    order.setOrderNo(generateOrderNo());
    order.setUserId(userId);
    // ... 其他字段同现有逻辑
    order.setRegion(region);
    order.setBillingCycle(request.getBillingCycle());
    order.setAmount(pricing.getPrice());
    order.setCurrency(pricing.getCurrency());
    order.setStatus("PENDING");
    order = orderRepository.save(order);

    // 3. 根据是否订阅，调用 PhotonPay API
    PaymentGateway gateway = getGateway();
    if (this.isSubscriptionBilling(request.getBillingCycle())) {
        // 订阅：创建 photopay 订阅
        SubscriptionCreateResponse subResp = gateway.createSubscription(
            new SubscriptionCreateRequest(...)
        );
        order.setChannelSubId(subResp.getSubscriptionId());
        order.setChannelOrderId(subResp.getInitialOrderId());
        redirectUrl = subResp.getPaymentUrl();
    } else {
        // 一次性支付
        PaymentCreateResponse payResp = gateway.createPayment(
            new PaymentCreateRequest(...)
        );
        order.setChannelOrderId(payResp.getTransactionId());
        redirectUrl = payResp.getPaymentUrl();
    }

    order.setChannel("PHOTONPAY");
    orderRepository.save(order);

    // 4. 返回带支付链接的响应
    return OrderCreateResponse.builder()
            .orderNo(order.getOrderNo())
            .productCode(order.getProductCode())
            .amount(order.getAmount())
            .currency(order.getCurrency())
            .status(order.getStatus())
            .redirectUrl(redirectUrl)  // ← 之前是 null，现在有值了
            .build();
}
```

### 4.4 PaymentService 修改 — Webhook 处理

```java
@Override
@Transactional(rollbackFor = Exception.class)
public boolean handlePaymentCallback(String orderNo, String channel, Map<String, Object> callbackData) {
    GridOrder order = orderRepository.findByOrderNo(orderNo)...;

    if (!"PENDING".equals(order.getStatus())) {
        return false; // 防重复处理
    }

    // 1. 校验回调签名（由 Webhook 端点处理，这里仅处理业务）

    // 2. 更新订单
    order.setStatus("PAID");
    order.setPaymentMethod(channel);
    order.setPaidAt(LocalDateTime.now());
    orderRepository.save(order);

    // 3. 记录支付流水
    PaymentRecord record = new PaymentRecord();
    record.setOrderId(order.getId());
    record.setPaymentMethod(channel);
    record.setTransactionId(callbackData.get("transactionId").toString());
    record.setAmount(order.getAmount());
    record.setCurrency(order.getCurrency());
    record.setStatus("SUCCESS");
    record.setRawCallback(JSON.toJSONString(callbackData));
    record.setCreateTime(LocalDateTime.now());
    paymentRecordRepository.save(record);

    // 4. 发放权益（现有逻辑不变）
    BillingCycleEnum cycle = BillingCycleEnum.fromCode(order.getBillingCycle());
    int days = cycle != null ? cycle.getDays() : 365;
    entitlementEngine.grant(
            order.getUserId(),
            "PURCHASE",
            order.getOrderNo(),
            order.getProductCode(),
            days,
            order.getRegion()
    );

    // 5. 如果是订阅订单，创建或更新 payment_subscription 记录
    if (order.getChannelSubId() != null) {
        saveOrUpdateSubscription(order);
    }

    return true;
}
```

### 4.5 Webhook 端点

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/app/payments")
@Api(tags = "支付：Webhook 回调")
public class PaymentWebhookController {

    private final PaymentGateway paymentGateway;
    private final PaymentService paymentService;
    private final PhotonPayConfig photonPayConfig;

    @ApiOperation("PhotonPay 支付回调 Webhook")
    @PostMapping("/webhook/photonpay")
    public ResponseEntity<String> handlePhotonPayWebhook(
            HttpServletRequest request,
            @RequestBody String payload) {

        // 1. 验证签名
        String signature = request.getHeader("X-PhotonPay-Signature");
        if (!paymentGateway.verifyWebhookSignature(payload, signature)) {
            return ResponseEntity.status(401).body("INVALID_SIGNATURE");
        }

        // 2. 解析回调数据
        JSONObject data = JSON.parseObject(payload);
        String eventType = data.getString("event_type");
        JSONObject eventData = data.getJSONObject("data");

        // 3. 处理不同事件类型
        switch (eventType) {
            case "payment.succeeded":
                String orderNo = eventData.getString("merchant_order_no");
                paymentService.handlePaymentCallback(orderNo, "PHOTONPAY", eventData);
                break;
            case "payment.refunded":
                handleRefund(eventData);
                break;
            case "subscription.cancelled":
                handleSubscriptionCancelled(eventData);
                break;
            case "subscription.failed":
                handleSubscriptionFailed(eventData);
                break;
        }

        return ResponseEntity.ok("OK");
    }
}
```

---

## 5. 订阅管理

### 5.1 首次订阅下单

用户选择"月度/季度/年度 Plus → 创建订单 → 调 PhotonPay 创建订阅 → 返回支付链接 → 用户支付 → Webhook 通知 → 发放权益"

### 5.2 续费处理

PhotonPay 在订阅到期前自动发起扣款。结果通过 Webhook 通知：

| Webhook 事件 | 处理 |
|-------------|------|
| `payment.succeeded` | 创建新订单（status=PAID）+ 发放权益（堆叠） |
| `payment.failed` | 重试（PhotonPay 内置）+ 记录日志 |
| `payment.failed`（多次） | 发送通知提醒用户更新支付方式 |
| `subscription.cancelled` | 标记 `payment_subscription.status=CANCELLED` |

### 5.3 用户取消订阅

```
POST /api/app/subscription/cancel
→ 调用 PhotonPay 取消订阅 API
→ 更新 payment_subscription.status=CANCELLED
→ 现有权益继续生效至到期日（堆叠记录不受影响）
```

### 5.4 订阅状态查询

```
GET /api/app/subscription/my
→ 返回现有 AppSubscriptionVO（level, expireTime, expiringSoon）
→ 增加 subscriptionActive（是否存在活跃的自动续费订阅）
```

---

## 6. 退款流程

### 6.1 发起退款

```java
// Admin 接口或后台操作
@ApiOperation("退款")
@PostMapping("/api/payments/{orderNo}/refund")
public ResponseEntity<Void> refund(@PathVariable String orderNo,
                                   @RequestParam(required = false) BigDecimal amount) {
    GridOrder order = orderService.findByOrderNo(orderNo)...;
    // 1. 调 PhotonPay 退款 API
    paymentGateway.refund(order.getChannelOrderId(), amount);
    // 2. 更新订单状态
    order.setStatus("REFUNDING");
    orderService.save(order);
    // 3. 收到 refund.succeeded Webhook 后
    //    → order.status = "REFUNDED"
    //    → 调用 EntitlementEngine.revoke(对应权益来源)
}
```

### 6.2 部分退款

支持对一笔订单部分退款，PhotonPay 原生支持。退款后按比例调整权益：

| 退款情况 | 权益处理 |
|---------|---------|
| 全额退款（7天内） | `EntitlementEngine.revoke(sourceId)` 撤销对应权益 |
| 部分退款 | 记录退款金额，权益不调整（V1 简化） |
| 订阅中途退款 | 取消订阅，权益保留至当前周期结束 |

---

## 7. 发票与收据

### 7.1 个人用户：电子收据（Receipt）

海外个人用户通常不需要正式发票，提供交易收据即可：

| 来源 | 收据内容 |
|------|---------|
| PhotonPay 自动发送 | 支付成功后 PhotonPay 发送邮件收据 |
| 你的系统生成 | 在"我的订单"页面生成带订单详情的收据 |

收据应包含：
- 订单号、下单时间、支付时间
- 产品名称、订阅周期
- 金额、币种、税率（如有）
- 商户名称："YourRoad 有路中文"
- 支付方式（信用卡尾号四位）

### 7.2 机构/企业用户：正式 Invoice

对需要正式 Invoice 的机构客户：

```
GET /api/app/orders/{orderNo}/invoice
→ 生成 PDF，包含：
  - 标题: "Invoice / 商业发票"
  - 商户信息（中国公司名称、地址、税号）
  - 客户信息（机构名称、地址）
  - 产品明细、金额、税费
  - Invoice Number（系统自动生成）
```

### 7.3 税务处理

| 场景 | 处理方式 |
|------|---------|
| 海外个人用户 | 不代扣税，价格含税或不含税由各区域定价决定 |
| 中国个人用户 | 不涉及（走国内支付渠道） |
| 海外机构客户 | 可按需开具 Invoice |
| 中国税务申报 | 按公司整体收入申报增值税，PhotonPay 结算流水为记账依据 |
| **配置项** | 各区域是否含税、税率均设为配置，可调整 |

> 税务本身由财务人员处理，系统只需提供完整的交易对账数据。

---

## 8. 配置管理

以下所有配置项均通过 `application.yml` 或 `.env` 管理，**后续替换值即可**，无需改代码。

### 8.1 PhotonPay 支付配置

```yaml
billing:
  payment:
    gateway: photonpay           # 支付渠道：photonpay（当前唯一值）

  photonpay:
    # ─── 基础配置 ───
    api-key: "${PHOTONPAY_API_KEY}"          # PhotonPay API Key
    api-secret: "${PHOTONPAY_API_SECRET}"    # PhotonPay API Secret
    webhook-secret: "${PHOTONPAY_WEBHOOK_SECRET}"  # Webhook 签名密钥

    # ─── 环境配置 ───
    base-url: "https://sandbox.photonpay.com"  # 沙箱环境；生产改为 https://api.photonpay.com

    # ─── 商户信息 ───
    merchant-display-name: "YourRoad 有路中文"
    merchant-description: "International Chinese Learning Platform"

    # ─── 回调/跳转地址 ───
    return-url: "https://yourroad.com/payment/return"   # 支付成功后跳转
    cancel-url: "https://yourroad.com/payment/cancel"   # 用户取消后跳转
    webhook-url: "https://yourroad.com/api/app/payments/webhook/photonpay"

    # ─── Apple Pay 配置（移动端用） ───
    apple-pay:
      enabled: true
      merchant-id: "${PHOTONPAY_APPLE_MERCHANT_ID}"      # Apple Merchant ID
      merchant-name: "YourRoad"
      supported-networks: "visa,mastercard,discover"
```

### 8.2 税务配置

```yaml
billing:
  tax:
    enabled: true
    # 各区域税率（后续根据实际情况调整）
    rates:
      A: 0.00      # 北美/西欧 — 不含税或以当地数字服务税为准
      B: 0.00      # 成熟市场 — 不含税
      C: 0.00      # 中国大陆 — 不含税（由国内支付渠道处理）
      D: 0.00      # 重点发展市场 — 不含税
      E: 0.00      # 教育特惠市场 — 不含税
    # 是否含税显示在价格中
    price-inclusive: false
```

### 8.3 订阅配置

```yaml
billing:
  subscription:
    # ─── 自动续费 ───
    auto-renewal: true
    # ─── 续费提前提醒天数 ───
    renewal-reminder-days: 7
    # ─── 扣款失败重试策略（PhotonPay 内置） ───
    retry:
      max-attempts: 3
      interval-days: 3
```

### 8.4 统一配置加载

所有配置通过 `@ConfigurationProperties` 加载，方便后续集中管理：

```java
@Configuration
@ConfigurationProperties(prefix = "billing.photonpay")
@Data
public class PhotonPayConfig {
    private String apiKey;
    private String apiSecret;
    private String webhookSecret;
    private String baseUrl;
    private String merchantDisplayName;
    private String merchantDescription;
    private String returnUrl;
    private String cancelUrl;
    private String webhookUrl;
    private ApplePayConfig applePay = new ApplePayConfig();

    @Data
    public static class ApplePayConfig {
        private boolean enabled = true;
        private String merchantId;
        private String merchantName;
        private String supportedNetworks = "visa,mastercard,discover";
    }
}
```

---

## 9. 订单管理与对账

### 9.1 商户后台订单查询

现有 `ProductController`（admin 端）扩展订单查询能力：

```
GET /api/orders                      # 订单列表（分页、搜索）
GET /api/orders/{orderNo}            # 订单详情
GET /api/orders/{orderNo}/refunds    # 退款记录
GET /api/payments/settlements        # 结算记录（从 PhotonPay 导入或手动上传）
```

### 9.2 对账流程

| 步骤 | 说明 |
|------|------|
| 1. 从 PhotonPay 商户后台下载结算报表 | CSV 格式，周期性下载 |
| 2. 与系统内订单比对 | 按 `channel_order_id` 匹配 |
| 3. 标记差异订单 | 漏单、金额不一致等 |
| 4. 财务入账 | 按结算周期汇总确认收入 |

V1 采用**半自动对账**（人工下载报表 + 系统比对），未来可扩展为全自动（PhotonPay API 拉取）。

### 9.3 结算资金流向

```
用户支付 → PhotonPay 收款 → 结算周期到 → 结算至中国对公账户 → 财务入账
                                                          ↓
                                                    合规结汇（CNY）
```

结算周期通常为 **T+3 到 T+7**（以 PhotonPay 实际执行为准）。

---

## 10. 退款与争议处理

### 10.1 退款流程

```
用户请求退款 → Admin 审核 → 调用退款 API → PhotonPay 处理 → Webhook 通知 → 撤销权益
```

### 10.2 拒付（Chargeback）

| 阶段 | 处理方式 |
|------|---------|
| **V1** | 被动应对：收到拒付通知后人工处理+撤销权益 |
| **未来** | 接入 PhotonPay 拒付预警，主动拦截 |

---

## 11. 文件清单

### 11.1 新增文件

| 路径 | 说明 |
|------|------|
| `grid-billing/.../service/PaymentGateway.java` | 支付渠道接口 |
| `grid-billing/.../service/impl/PhotonPayPaymentGateway.java` | PhotonPay 实现 |
| `grid-billing/.../service/impl/dto/PaymentCreateRequest.java` | 创建支付请求 DTO |
| `grid-billing/.../service/impl/dto/PaymentCreateResponse.java` | 创建支付响应 DTO |
| `grid-billing/.../service/impl/dto/SubscriptionCreateRequest.java` | 创建订阅请求 DTO |
| `grid-billing/.../service/impl/dto/SubscriptionCreateResponse.java` | 创建订阅响应 DTO |
| `grid-billing/.../service/impl/dto/TransactionQueryResponse.java` | 交易查询响应 DTO |
| `grid-billing/.../service/impl/dto/RefundResponse.java` | 退款响应 DTO |
| `grid-billing/.../config/PhotonPayConfig.java` | PhotonPay 配置类 |
| `grid-billing/.../domain/PaymentSubscription.java` | 支付订阅关联表实体 |
| `grid-billing/.../repository/PaymentSubscriptionRepository.java` | 支付订阅关联 Repository |
| `grid-app/.../rest/PaymentWebhookController.java` | Webhook 回调端点 |

### 11.2 修改文件

| 路径 | 修改内容 |
|------|---------|
| `grid-billing/.../domain/GridOrder.java` | 新增 `channel`, `channelOrderId`, `channelSubId`, `invoiceNo` 字段 |
| `grid-billing/.../service/impl/OrderServiceImpl.java` | 支付流程接入 PaymentGateway |
| `grid-billing/.../service/impl/PaymentServiceImpl.java` | 替换 Mock 为真实 Webhook 处理 |
| `grid-billing/.../service/OrderService.java` | 接口不变，实现修改 |
| `grid-app/.../rest/AppOrderController.java` | 调整回调端点为 Webhook |
| `grid-system/.../rest/ProductController.java` | 扩展订单查询管理接口 |

---

## 12. 安全与合规

### 12.1 签名验证

所有 PhotonPay Webhook 回调使用 HMAC-SHA256 签名验证：

```java
// PhotonPay 通常在 Header 中携带 X-PhotonPay-Signature
// 使用 webhook-secret 计算 payload 的 HMAC 并比对
```

### 12.2 防重复通知

Webhook 可能因网络原因重复发送，通过 `order.status != PENDING` 判定是否已处理。

### 12.3 Webhook 安全

- Webhook 端点无需用户认证（`@AnonymousPostMapping`）
- 仅依赖签名验证
- 建议 IP 白名单（PhotonPay 提供固定回调 IP）

---

## 13. 后续可扩展点（本期不做）

| 功能 | 说明 |
|------|------|
| 自动对账系统 | 通过 PhotonPay API 自动拉取结算数据 |
| 更多支付方式 | PayPal、欧洲 iDEAL、巴西 Pix 等本地支付 |
| App Store / Google Play IAP | 移动端原生订阅，需要独立处理（渠道抽成+不同回调格式） |
| Stripe 备选 | 如果未来注册海外公司，可作为备选渠道 |
| 拒付自动处理 | 接入 PhotonPay 拒付预警 API |
| 定时订阅提醒 | 续费前发送邮件/推送通知 |
| 多级退款审批 | Admin 后台退款审核工作流 |

---

## 14. 附录：PhotonPay 对接配置参考

### 14.1 需要从 PhotonPay 获取的信息

| 信息 | 获取方式 |
|------|---------|
| API Key | PhotonPay 商户后台 → API 设置 |
| API Secret | PhotonPay 商户后台 → API 设置 |
| Webhook Secret | PhotonPay 商户后台 → Webhook 设置 |
| 沙箱环境地址 | PhotonPay 文档，通常为 `https://sandbox.photonpay.com` |
| 生产环境地址 | PhotonPay 文档，通常为 `https://api.photonpay.com` |
| Apple Pay Merchant ID | PhotonPay 商户后台 → Apple Pay 配置 |
| 结算账户绑定 | 入驻时已绑定中国对公银行账户 |

### 14.2 沙箱测试流程

```
1. 配置沙箱环境（base-url → sandbox 地址）
2. 使用 PhotonPay 提供的测试卡号
3. 测试完整下单 → 支付 → 回调 → 权益发放流程
4. 测试退款流程
5. 测试订阅创建 → 自动续费回调
6. 切换生产环境（替换 base-url + API Key）
```

### 14.3 API 端点和回调

| 场景 | 端点和配置 |
|------|-----------|
| 创建支付 | `POST /checkout`（PhotonPay API） |
| 创建订阅 | `POST /subscription`（PhotonPay API） |
| 退款 | `POST /payment/{id}/refund`（PhotonPay API） |
| Webhook 地址 | `POST /api/app/payments/webhook/photonpay`（你的系统） |
| 支付成功跳转 | 配置为 `https://yourroad.com/payment/return` |
| 支付取消跳转 | 配置为 `https://yourroad.com/payment/cancel` |

> 以上 API 端点为占位格式，具体路径以 PhotonPay 官方文档为准。接入时修改 `PhotonPayPaymentGateway` 中的 URL。

---

## 15. 环境切换

### 15.1 沙箱 → 生产切换清单

准备上线时，替换以下配置值：

| 配置项 | 沙箱值 → 生产值 |
|--------|--------------|
| `billing.photonpay.base-url` | `https://sandbox.photonpay.com` → `https://api.photonpay.com` |
| `billing.photonpay.api-key` | 沙箱 Key → 生产 Key |
| `billing.photonpay.api-secret` | 沙箱 Secret → 生产 Secret |
| `billing.photonpay.webhook-secret` | 沙箱 Secret → 生产 Secret |
| `billing.photonpay.return-url` | 测试域名 → 正式域名 |
| `billing.photonpay.cancel-url` | 测试域名 → 正式域名 |
| `billing.photonpay.webhook-url` | 测试域名 → 正式域名 |
