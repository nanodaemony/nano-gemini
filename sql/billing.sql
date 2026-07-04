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
    UNIQUE KEY `uk_product_region_cycle` (`product_id`, `region`, `billing_cycle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域定价表';

-- ----------------------------
-- 订阅记录表
-- ----------------------------
CREATE TABLE `payment_subscription` (
    `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`         BIGINT NOT NULL,
    `product_code`    VARCHAR(50) NOT NULL COMMENT '商品代码',
    `channel`         VARCHAR(30) NOT NULL COMMENT 'STRIPE/PHOTONPAY',
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
    `amount`          DECIMAL(12,2) NOT NULL,
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
    `raw_callback`    TEXT COMMENT '原始回调JSON',
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_order_id` (`order_id`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

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
