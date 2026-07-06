-- ============================================================
-- 测试用户
-- 邮箱: test@test.com  密码: 123456
-- ============================================================
INSERT INTO `grid_user` (
    `username`, `password`,
    `phone`, `phone_verified`,
    `email`, `email_verified`,
    `nickname`, `avatar`, `gender`,
    `user_type`, `org_id`, `org_role`, `agent_id`,
    `referral_code`, `referred_by`, `region`, `country`,
    `register_audit_status`,
    `status`, `register_ip`,
    `last_login_time`, `last_login_ip`,
    `create_time`, `update_time`
) VALUES (
    'test@test.com',
    '$2a$10$..4UBldLstn4yRNlIvBPxODMlHVcMD.h7qnYq6sC3oNcYSC/.eXM6',
    NULL, 0,
    'test@test.com', 1,
    'TestUser', NULL, 0,
    'NORMAL', NULL, NULL, NULL,
    NULL, NULL, 'A', NULL,
    'APPROVED',
    1, '127.0.0.1',
    NULL, NULL,
    NOW(), NOW()
);

-- ============================================================
-- 默认收藏夹（userId=1, 封面图ID=26）
-- ============================================================
INSERT INTO `biz_collection_folder` (
    `user_id`, `name`, `cover_image_id`,
    `is_default`, `is_pinned`, `sort_order`,
    `status`, `create_time`, `update_time`
) VALUES (
    1, '默认收藏夹', 26,
    1, 0, 0,
    1, NOW(), NOW()
);
