-- ----------------------------
-- 机构表
-- ----------------------------
CREATE TABLE `grid_organization` (
    `id` INT AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(200) NOT NULL COMMENT '机构名称',
    `name_en` VARCHAR(200) COMMENT '机构英文名',
    `org_type` VARCHAR(20) NOT NULL COMMENT 'UNIVERSITY/SCHOOL/TRAINING/OTHER',
    `contact_name` VARCHAR(100) COMMENT '联系人姓名',
    `contact_email` VARCHAR(255) COMMENT '联系邮箱',
    `contact_phone` VARCHAR(50) COMMENT '联系电话',
    `country` VARCHAR(100) COMMENT '所在国家',
    `region` VARCHAR(10) COMMENT '区域 A/B/C/D/E',
    `status` INT NOT NULL DEFAULT 1 COMMENT '1-可用 0-已删除',
    `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `max_members` INT NOT NULL DEFAULT 0 COMMENT '最大成员数',
    `max_admins` INT NOT NULL DEFAULT 0 COMMENT '最大管理员数',
    `current_members` INT NOT NULL DEFAULT 0 COMMENT '当前成员数',
    `expire_time` DATETIME COMMENT '机构有效到期时间',
    `create_by` VARCHAR(50) COMMENT '创建人',
    `update_by` VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_audit_status` (`audit_status`),
    KEY `idx_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机构表';

-- ----------------------------
-- 代理商表
-- ----------------------------
CREATE TABLE `grid_agent` (
    `id` INT AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(200) NOT NULL COMMENT '代理商名称',
    `contact_name` VARCHAR(100) COMMENT '联系人姓名',
    `contact_email` VARCHAR(255) COMMENT '联系邮箱',
    `contact_phone` VARCHAR(50) COMMENT '联系电话',
    `commission_rate` DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '返现比例 %',
    `referral_code` VARCHAR(32) NOT NULL COMMENT '代理专用推荐码',
    `status` INT NOT NULL DEFAULT 1 COMMENT '1-可用 0-已删除',
    `audit_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `create_by` VARCHAR(50) COMMENT '创建人',
    `update_by` VARCHAR(50) COMMENT '更新人',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_referral_code` (`referral_code`),
    KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代理商表';

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
    ADD COLUMN `referred_by` VARCHAR(32) COMMENT '注册时填的推荐码' AFTER `referral_code`,
    ADD COLUMN `region` VARCHAR(10) COMMENT '所属区域 A/B/C/D/E' AFTER `referred_by`,
    ADD COLUMN `register_audit_status` VARCHAR(20) DEFAULT 'APPROVED' COMMENT '注册审核状态 PENDING/APPROVED/REJECTED' AFTER `region`;
