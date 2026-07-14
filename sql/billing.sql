-- ----------------------------
-- 权益元数据表
-- ----------------------------
CREATE TABLE `entitlement` (
    `id`          INT AUTO_INCREMENT COMMENT '主键ID',
    `code`        VARCHAR(50) NOT NULL COMMENT '权益唯一标识 VOCAB_ACCESS等',
    `name`        VARCHAR(200) NOT NULL COMMENT '权益名称',
    `module_code` VARCHAR(50) COMMENT '关联业务模块, BizModuleEnum',
    `sort_order`  INT DEFAULT 0 COMMENT '排序',
    `status`      INT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `create_by`   VARCHAR(50) COMMENT '创建人',
    `update_by`   VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    UNIQUE KEY `uk_module_code` (`module_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权益元数据表';

-- ----------------------------
-- 用户权益流水表
-- ----------------------------
CREATE TABLE `user_entitlement_record` (
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `entitlement_id`  INT NOT NULL COMMENT '权益ID',
    `source_type`     VARCHAR(30) NOT NULL COMMENT 'TRIAL / PURCHASE / ADMIN_GRANT',
    `source_id`       VARCHAR(100) COMMENT '来源业务ID(订单号等)',
    `duration_days`   INT NOT NULL COMMENT '本次有效天数',
    `expire_at`       DATETIME COMMENT '原始到期时间',
    `region`          VARCHAR(10) COMMENT '授予时区域',
    `remark`          VARCHAR(500) COMMENT '备注',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_entitlement` (`user_id`, `entitlement_id`),
    KEY `idx_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权益流水表';

-- ----------------------------
-- 用户权益汇总表
-- ----------------------------
CREATE TABLE `user_entitlement` (
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `entitlement_id`  INT NOT NULL COMMENT '权益ID',
    `expire_at`       DATETIME COMMENT '堆叠后到期时间',
    `status`          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / EXPIRED',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次获得时间',
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_entitlement` (`user_id`, `entitlement_id`),
    KEY `idx_expire` (`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权益汇总表';

-- ----------------------------
-- 商品表（重建）
-- ----------------------------
CREATE TABLE `grid_product` (
    `id`                 INT AUTO_INCREMENT COMMENT '主键ID',
    `code`               VARCHAR(50) NOT NULL COMMENT '商品代码',
    `name`               VARCHAR(200) NOT NULL COMMENT '商品名称',
    `product_type`       VARCHAR(30) NOT NULL COMMENT 'PLUS/SINGLE_MODULE/INSTITUTION/ENTERPRISE',
    `entitlement_ids`    VARCHAR(500) COMMENT 'JSON数组, 购买后获得的权益ID列表',
    `institution_config` VARCHAR(500) COMMENT 'JSON, 机构商品配置 {"maxMembers":30,"maxAdmins":1}',
    `cover_image`        VARCHAR(500) COMMENT '封面图URL',
    `description`        TEXT COMMENT '商品描述',
    `sort_order`         INT DEFAULT 0 COMMENT '排序',
    `status`             INT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    `create_by`          VARCHAR(50) COMMENT '创建人',
    `update_by`          VARCHAR(50) COMMENT '更新人',
    `create_time`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ----------------------------
-- 区域定价表
-- ----------------------------
CREATE TABLE `region_pricing` (
    `id`              INT AUTO_INCREMENT PRIMARY KEY,
    `product_id`      INT NOT NULL,
    `region`          VARCHAR(10) NOT NULL COMMENT 'A/B/C/D/E',
    `billing_cycle`   VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY/YEARLY',
    `price`           DECIMAL(12,2) NOT NULL COMMENT '金额',
    `currency`        VARCHAR(10) NOT NULL COMMENT 'USD/EUR/CNY',
    `status`          INT NOT NULL DEFAULT 1,
    `create_by`       VARCHAR(50), `update_by` VARCHAR(50),
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_product_region_cycle_cur` (`product_id`, `region`, `billing_cycle`, `currency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域定价表';

-- ----------------------------
-- 订阅记录表
-- ----------------------------
CREATE TABLE `payment_subscription` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT NOT NULL,
    `product_code`    VARCHAR(50) NOT NULL COMMENT '商品代码',
    `channel`         VARCHAR(30) NOT NULL COMMENT 'FASTSPRING',
    `channel_sub_id`  VARCHAR(200) COMMENT '渠道侧订阅ID',
    `status`          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CANCELLED/EXPIRED',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `cancel_at`       DATETIME COMMENT '取消时间',
    KEY `idx_user` (`user_id`),
    KEY `idx_channel_sub` (`channel`, `channel_sub_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅记录表';

-- ----------------------------
-- 订单表
-- ----------------------------
CREATE TABLE `grid_order` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no`        VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    `user_id`         BIGINT NOT NULL,
    `org_id`          INT COMMENT '机构下单时为机构ID',
    `product_code`    VARCHAR(50) NOT NULL,
    `region`          VARCHAR(10) NOT NULL,
    `billing_cycle`   VARCHAR(20) NOT NULL,
    `amount`          DECIMAL(12,2) NOT NULL COMMENT '含税总金额',
    `subtotal`        DECIMAL(12,2) COMMENT '税前金额',
    `tax_amount`      DECIMAL(12,2) DEFAULT 0 COMMENT '税额',
    `discount_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '优惠金额',
    `coupon_code`     VARCHAR(32) COMMENT '优惠券代码',
    `tax_region`      VARCHAR(10) COMMENT '税务地区',
    `currency`        VARCHAR(10) NOT NULL,
    `status`          VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/REFUNDING/REFUNDED/EXPIRED',
    `payment_method`  VARCHAR(30),
    `paid_at`         DATETIME,
    `expire_at`       DATETIME COMMENT '订单过期时间',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    `channel`         VARCHAR(30),
    `channel_order_id` VARCHAR(200),
    `channel_sub_id`  VARCHAR(200),
    `invoice_no`      VARCHAR(64),
    `version`         INT DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ----------------------------
-- 支付流水表
-- ----------------------------
CREATE TABLE `payment_record` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id`        BIGINT NOT NULL,
    `payment_method`  VARCHAR(30) NOT NULL,
    `transaction_id`  VARCHAR(200) COMMENT '支付平台交易号',
    `amount`          DECIMAL(12,2) NOT NULL,
    `currency`        VARCHAR(10) NOT NULL,
    `status`          VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED/REFUND',
    `gateway`         VARCHAR(30) COMMENT '支付网关 FASTSPRING',
    `gateway_fee`     DECIMAL(12,2) COMMENT '网关手续费',
    `net_amount`      DECIMAL(12,2) COMMENT '净收入（扣费后）',
    `raw_callback`    TEXT COMMENT '原始回调JSON',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_order_id` (`order_id`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ----------------------------
-- 发票/收据表
-- ----------------------------
CREATE TABLE `billing_invoice` (
    `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
    `invoice_no`       VARCHAR(64) NOT NULL UNIQUE COMMENT '发票号 INV-YYYYMMDD-XXXXXX',
    `order_id`         BIGINT NOT NULL COMMENT '关联订单',
    `user_id`          BIGINT NOT NULL COMMENT '用户',
    `org_id`           INT COMMENT '机构',
    `invoice_type`     VARCHAR(30) NOT NULL DEFAULT 'FASTSPRING' COMMENT 'FASTSPRING/SELF_GEN',
    `invoice_format`   VARCHAR(30) NOT NULL DEFAULT 'INTERNATIONAL' COMMENT 'INTERNATIONAL/CHINESE/INSTITUTION',
    `currency`         VARCHAR(10) NOT NULL,
    `subtotal`         DECIMAL(12,2) NOT NULL COMMENT '税前金额',
    `tax_amount`       DECIMAL(12,2) DEFAULT 0 COMMENT '税额',
    `total_amount`     DECIMAL(12,2) NOT NULL COMMENT '含税总金额',
    `buyer_name`       VARCHAR(200) COMMENT '买方名称',
    `buyer_tax_id`     VARCHAR(100) COMMENT '买方税号',
    `buyer_address`    VARCHAR(500) COMMENT '买方地址',
    `buyer_email`      VARCHAR(200) COMMENT '买方邮箱',
    `seller_name`      VARCHAR(200) NOT NULL DEFAULT 'YourRoad 有路中文' COMMENT '卖方名称',
    `seller_tax_id`    VARCHAR(100) COMMENT '卖方税号',
    `seller_address`   VARCHAR(500) COMMENT '卖方地址',
    `notes`            VARCHAR(1000) COMMENT '备注',
    `pdf_url`          VARCHAR(500) COMMENT 'OSS PDF地址',
    `fastspring_url`   VARCHAR(500) COMMENT 'FastSpring原始发票链接',
    `region`           VARCHAR(10) COMMENT '区域',
    `status`           VARCHAR(20) NOT NULL DEFAULT 'ISSUED' COMMENT 'DRAFT/ISSUED/VOIDED',
    `issued_at`        DATETIME COMMENT '开具时间',
    `create_time`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_order` (`order_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_org` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票/收据表';

-- ----------------------------
-- 账单信息表
-- ----------------------------
CREATE TABLE `billing_profile` (
    `id`               INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`          BIGINT COMMENT '用户（个人）',
    `org_id`           INT COMMENT '机构',
    `company_name`     VARCHAR(200) COMMENT '公司名称',
    `tax_id`           VARCHAR(100) COMMENT '税号/VAT号',
    `billing_address`  VARCHAR(500) COMMENT '账单地址',
    `billing_email`    VARCHAR(200) COMMENT '账单邮箱',
    `billing_phone`    VARCHAR(50) COMMENT '账单电话',
    `region`           VARCHAR(10) COMMENT '区域',
    `is_default`       TINYINT DEFAULT 1 COMMENT '默认',
    `create_time`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_org` (`user_id`, `org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单信息表';

-- ----------------------------
-- 种子数据
-- ----------------------------
-- 权益元数据
INSERT INTO `entitlement` (`code`, `name`, `module_code`, `sort_order`, `status`) VALUES
('VOCAB_ACCESS', '词汇模块使用权', 'VOCAB', 1, 1),
('GRAMMAR_ACCESS', '语法模块使用权', 'GRAMMAR', 2, 1),
('CHARACTER_ACCESS', '汉字模块使用权', 'CHARACTER', 3, 1),
('CONFUSING_WORDS_ACCESS', '易混淆词辨析使用权', 'CONFUSING_WORDS', 4, 1),
('CULTURE_ACCESS', '文化模块使用权', 'CULTURE', 5, 1),
('TOPIC_ACCESS', '话题模块使用权', 'TOPIC', 6, 1);

-- 商品
INSERT INTO `grid_product` (`code`, `name`, `product_type`, `entitlement_ids`, `institution_config`, `sort_order`, `status`) VALUES
('PLUS', '全平台Plus会员', 'PLUS',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 NULL, 1, 1),
('VOCAB', '词汇模块', 'SINGLE_MODULE', '["VOCAB_ACCESS"]', NULL, 2, 1),
('GRAMMAR', '语法模块', 'SINGLE_MODULE', '["GRAMMAR_ACCESS"]', NULL, 3, 1),
('CHARACTER', '汉字模块', 'SINGLE_MODULE', '["CHARACTER_ACCESS"]', NULL, 4, 1),
('CONFUSING_WORDS', '易混淆词辨析模块', 'SINGLE_MODULE', '["CONFUSING_WORDS_ACCESS"]', NULL, 5, 1),
('CULTURE', '文化模块', 'SINGLE_MODULE', '["CULTURE_ACCESS"]', NULL, 6, 1),
('TOPIC', '话题模块', 'SINGLE_MODULE', '["TOPIC_ACCESS"]', NULL, 7, 1),
('INST_STARTER', 'Institution Starter', 'INSTITUTION',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 '{"maxMembers":30,"maxAdmins":1}', 10, 1),
('INST_BASIC', 'Institution Basic', 'INSTITUTION',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 '{"maxMembers":100,"maxAdmins":2}', 11, 1),
('INST_PRO', 'Institution Pro', 'INSTITUTION',
 '["VOCAB_ACCESS","GRAMMAR_ACCESS","CHARACTER_ACCESS","CONFUSING_WORDS_ACCESS","CULTURE_ACCESS","TOPIC_ACCESS"]',
 '{"maxMembers":500,"maxAdmins":5}', 12, 1);

-- 区域定价 — PLUS 全平台会员 (product_id=1)
INSERT INTO `region_pricing` (`product_id`, `region`, `billing_cycle`, `price`, `currency`, `status`) VALUES
(1, 'A', 'MONTHLY',    11.99, 'USD', 1),
(1, 'A', 'MONTHLY',    10.99, 'EUR', 1),
(1, 'B', 'MONTHLY',     9.99, 'USD', 1),
(1, 'C', 'MONTHLY',    69.00, 'CNY', 1),
(1, 'D', 'MONTHLY',     7.99, 'USD', 1),
(1, 'E', 'MONTHLY',     5.99, 'USD', 1),
(1, 'A', 'QUARTERLY',  29.99, 'USD', 1),
(1, 'A', 'QUARTERLY',  26.99, 'EUR', 1),
(1, 'B', 'QUARTERLY',  22.99, 'USD', 1),
(1, 'C', 'QUARTERLY', 129.00, 'CNY', 1),
(1, 'D', 'QUARTERLY',  18.99, 'USD', 1),
(1, 'E', 'QUARTERLY',  11.99, 'USD', 1),
(1, 'A', 'YEARLY',     99.99, 'USD', 1),
(1, 'A', 'YEARLY',     89.99, 'EUR', 1),
(1, 'B', 'YEARLY',     79.99, 'USD', 1),
(1, 'C', 'YEARLY',    399.00, 'CNY', 1),
(1, 'D', 'YEARLY',     59.99, 'USD', 1),
(1, 'E', 'YEARLY',     39.99, 'USD', 1);

-- 单模块年度订阅 (product_id=2..7)
INSERT INTO `region_pricing` (`product_id`, `region`, `billing_cycle`, `price`, `currency`, `status`) VALUES
-- VOCAB
(2, 'A', 'YEARLY', 39.99, 'USD', 1), (2, 'A', 'YEARLY', 35.99, 'EUR', 1),
(2, 'B', 'YEARLY', 29.99, 'USD', 1), (2, 'C', 'YEARLY', 149.00, 'CNY', 1),
(2, 'D', 'YEARLY', 24.99, 'USD', 1), (2, 'E', 'YEARLY', 16.99, 'USD', 1),
-- GRAMMAR
(3, 'A', 'YEARLY', 29.99, 'USD', 1), (3, 'A', 'YEARLY', 26.99, 'EUR', 1),
(3, 'B', 'YEARLY', 24.99, 'USD', 1), (3, 'C', 'YEARLY', 169.00, 'CNY', 1),
(3, 'D', 'YEARLY', 19.99, 'USD', 1), (3, 'E', 'YEARLY', 12.99, 'USD', 1),
-- CHARACTER
(4, 'A', 'YEARLY', 19.99, 'USD', 1), (4, 'A', 'YEARLY', 17.99, 'EUR', 1),
(4, 'B', 'YEARLY', 14.99, 'USD', 1), (4, 'C', 'YEARLY', 99.00, 'CNY', 1),
(4, 'D', 'YEARLY', 12.99, 'USD', 1), (4, 'E', 'YEARLY', 8.99, 'USD', 1),
-- CONFUSING_WORDS
(5, 'A', 'YEARLY', 19.99, 'USD', 1), (5, 'A', 'YEARLY', 17.99, 'EUR', 1),
(5, 'B', 'YEARLY', 14.99, 'USD', 1), (5, 'C', 'YEARLY', 99.00, 'CNY', 1),
(5, 'D', 'YEARLY', 12.99, 'USD', 1), (5, 'E', 'YEARLY', 8.99, 'USD', 1),
-- CULTURE
(6, 'A', 'YEARLY', 19.99, 'USD', 1), (6, 'A', 'YEARLY', 17.99, 'EUR', 1),
(6, 'B', 'YEARLY', 14.99, 'USD', 1), (6, 'C', 'YEARLY', 99.00, 'CNY', 1),
(6, 'D', 'YEARLY', 12.99, 'USD', 1), (6, 'E', 'YEARLY', 8.99, 'USD', 1),
-- TOPIC
(7, 'A', 'YEARLY', 24.99, 'USD', 1), (7, 'A', 'YEARLY', 22.99, 'EUR', 1),
(7, 'B', 'YEARLY', 19.99, 'USD', 1), (7, 'C', 'YEARLY', 59.00, 'CNY', 1),
(7, 'D', 'YEARLY', 16.99, 'USD', 1), (7, 'E', 'YEARLY', 11.99, 'USD', 1);

-- 机构版套餐 (product_id=8..10)
INSERT INTO `region_pricing` (`product_id`, `region`, `billing_cycle`, `price`, `currency`, `status`) VALUES
-- INST_STARTER
(8, 'A', 'YEARLY', 1199.00, 'USD', 1), (8, 'A', 'YEARLY', 1099.00, 'EUR', 1),
(8, 'B', 'YEARLY',  999.00, 'USD', 1), (8, 'C', 'YEARLY', 6800.00, 'CNY', 1),
(8, 'D', 'YEARLY',  799.00, 'USD', 1), (8, 'E', 'YEARLY',  599.00, 'USD', 1),
-- INST_BASIC
(9, 'A', 'YEARLY', 2499.00, 'USD', 1), (9, 'A', 'YEARLY', 2199.00, 'EUR', 1),
(9, 'B', 'YEARLY', 1999.00, 'USD', 1), (9, 'C', 'YEARLY', 12800.00, 'CNY', 1),
(9, 'D', 'YEARLY', 1499.00, 'USD', 1), (9, 'E', 'YEARLY',  999.00, 'USD', 1),
-- INST_PRO
(10, 'A', 'YEARLY', 9999.00, 'USD', 1), (10, 'A', 'YEARLY', 8999.00, 'EUR', 1),
(10, 'B', 'YEARLY', 7999.00, 'USD', 1), (10, 'C', 'YEARLY', 49800.00, 'CNY', 1),
(10, 'D', 'YEARLY', 5999.00, 'USD', 1), (10, 'E', 'YEARLY', 3999.00, 'USD', 1);
