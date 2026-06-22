-- ----------------------------
-- 产品表
-- ----------------------------
CREATE TABLE `grid_product` (
    `id` INT AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(50) NOT NULL COMMENT '产品代码 PLUS/VOCAB/GRAMMAR/...',
    `name` VARCHAR(200) NOT NULL COMMENT '产品名称',
    `product_type` VARCHAR(30) NOT NULL COMMENT 'PLUS / SINGLE_MODULE / INSTITUTION / ENTERPRISE',
    `description` TEXT COMMENT '产品描述',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` INT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-可用',
    `create_by` VARCHAR(50) COMMENT '创建人',
    `update_by` VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- ----------------------------
-- 产品模块关联表
-- ----------------------------
CREATE TABLE `product_module` (
    `id` INT AUTO_INCREMENT COMMENT '主键ID',
    `product_id` INT NOT NULL COMMENT '产品ID',
    `module_code` VARCHAR(50) NOT NULL COMMENT '模块代码 VOCAB/GRAMMAR/CHARACTER/CONFUSING_WORDS/CULTURE/TOPIC',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_module` (`product_id`, `module_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品模块关联表';

-- ----------------------------
-- 区域定价表
-- ----------------------------
CREATE TABLE `region_pricing` (
    `id` INT AUTO_INCREMENT COMMENT '主键ID',
    `product_id` INT NOT NULL COMMENT '产品ID',
    `region` VARCHAR(10) NOT NULL COMMENT '区域 A/B/C/D/E',
    `billing_cycle` VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY/YEARLY',
    `price` DECIMAL(12,2) NOT NULL COMMENT '金额',
    `currency` VARCHAR(10) NOT NULL COMMENT '币种 USD/EUR/CNY',
    `status` INT NOT NULL DEFAULT 1 COMMENT '状态',
    `create_by` VARCHAR(50) COMMENT '创建人',
    `update_by` VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_region_cycle` (`product_id`, `region`, `billing_cycle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域定价表';

-- ----------------------------
-- 权益来源表（核心堆叠表）
-- ----------------------------
CREATE TABLE `entitlement_source` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `source_type` VARCHAR(30) NOT NULL COMMENT 'TRIAL/PURCHASE/INSTITUTION/REFERRAL/ADMIN_GRANT',
    `source_id` VARCHAR(100) COMMENT '来源业务ID（订单号/机构ID等）',
    `product_code` VARCHAR(50) NOT NULL COMMENT '产品代码 PLUS/VOCAB/...',
    `granted_at` DATETIME NOT NULL COMMENT '授予时间',
    `duration_days` INT NOT NULL COMMENT '有效天数',
    `expire_at` DATETIME COMMENT '堆叠计算后的到期时间（缓存）',
    `region` VARCHAR(10) COMMENT '购买/授予时的区域',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REVOKED/EXPIRED',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_product` (`user_id`, `product_code`, `status`),
    KEY `idx_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权益来源表';

-- ----------------------------
-- 订单表
-- ----------------------------
CREATE TABLE `grid_order` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `user_id` BIGINT NOT NULL COMMENT '下单用户ID',
    `org_id` INT COMMENT '机构下单时为机构ID',
    `product_code` VARCHAR(50) NOT NULL COMMENT '产品代码',
    `region` VARCHAR(10) NOT NULL COMMENT '区域',
    `billing_cycle` VARCHAR(20) NOT NULL COMMENT 'MONTHLY/QUARTERLY/YEARLY',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
    `currency` VARCHAR(10) NOT NULL COMMENT '币种',
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/REFUNDING/REFUNDED/EXPIRED',
    `payment_method` VARCHAR(30) COMMENT 'WECHAT/ALIPAY/STRIPE',
    `paid_at` DATETIME COMMENT '支付时间',
    `expire_at` DATETIME COMMENT '订单过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ----------------------------
-- 支付流水表
-- ----------------------------
CREATE TABLE `payment_record` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `payment_method` VARCHAR(30) NOT NULL COMMENT 'WECHAT/ALIPAY/STRIPE',
    `transaction_id` VARCHAR(200) COMMENT '支付平台交易号',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
    `currency` VARCHAR(10) NOT NULL COMMENT '币种',
    `status` VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED/REFUND',
    `raw_callback` TEXT COMMENT '原始回调JSON',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ----------------------------
-- 初始化产品数据
-- ----------------------------
INSERT INTO `grid_product` (`code`, `name`, `product_type`, `sort_order`, `status`) VALUES
('PLUS', '全平台Plus会员', 'PLUS', 1, 1),
('VOCAB', '词汇模块', 'SINGLE_MODULE', 2, 1),
('GRAMMAR', '语法模块', 'SINGLE_MODULE', 3, 1),
('CHARACTER', '汉字模块', 'SINGLE_MODULE', 4, 1),
('CONFUSING_WORDS', '易混淆词辨析模块', 'SINGLE_MODULE', 5, 1),
('CULTURE', '文化模块', 'SINGLE_MODULE', 6, 1),
('TOPIC', '话题模块', 'SINGLE_MODULE', 7, 1);

-- PLUS 包含所有子模块
INSERT INTO `product_module` (`product_id`, `module_code`) VALUES
(1, 'VOCAB'), (1, 'GRAMMAR'), (1, 'CHARACTER'),
(1, 'CONFUSING_WORDS'), (1, 'CULTURE'), (1, 'TOPIC');
