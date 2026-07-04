-- ----------------------------
-- 推荐记录表
-- ----------------------------
CREATE TABLE `referral_record` (
    `id` BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `referrer_id` BIGINT NOT NULL COMMENT '推荐人用户ID',
    `referrer_type` VARCHAR(20) NOT NULL COMMENT 'NORMAL/INSTITUTION/AGENT',
    `referred_id` BIGINT COMMENT '被推荐人用户ID',
    `referral_code` VARCHAR(32) NOT NULL COMMENT '使用的推荐码',
    `order_id` BIGINT COMMENT '关联订单ID',
    `reward_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/SETTLED/PAID',
    `reward_amount` DECIMAL(12,2) COMMENT '奖励金额',
    `reward_type` VARCHAR(20) COMMENT 'EXTEND_DAYS/CASH/MEMBER_COUNT',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `settle_time` DATETIME COMMENT '结算时间',
    PRIMARY KEY (`id`),
    KEY `idx_referrer_id` (`referrer_id`),
    KEY `idx_referral_code` (`referral_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐记录表';

-- ----------------------------
-- grid_user 扩展字段
-- ----------------------------
ALTER TABLE `grid_user`
    ADD COLUMN `user_type` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '用户类型 NORMAL/INSTITUTION/AGENT' AFTER `gender`,
    ADD COLUMN `org_id` INT COMMENT '所属机构ID' AFTER `user_type`,
    ADD COLUMN `org_role` VARCHAR(20) COMMENT '机构角色 ADMIN/MEMBER' AFTER `org_id`,
    ADD COLUMN `agent_id` INT COMMENT '所属代理ID' AFTER `org_role`,
    ADD COLUMN `referral_code` VARCHAR(32) COMMENT '我的推荐码' AFTER `agent_id`,
    ADD UNIQUE KEY `uk_referral_code` (`referral_code`),
    ADD COLUMN `referred_by` VARCHAR(32) COMMENT '注册时填的推荐码' AFTER `referral_code`,
    ADD COLUMN `region` VARCHAR(10) COMMENT '所属区域 A/B/C/D/E' AFTER `referred_by`,
    ADD COLUMN `register_audit_status` VARCHAR(20) DEFAULT 'APPROVED' COMMENT '注册审核状态 PENDING/APPROVED/REJECTED' AFTER `region`;
