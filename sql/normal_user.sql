################################################################## 普通用户相关 #####################################################################

  -- ----------------------------
  -- 用户表
  -- ----------------------------
  CREATE TABLE `grid_user` (
      `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `username` VARCHAR(50) NULL COMMENT '用户名',
      `password` VARCHAR(100) NULL COMMENT '密码（BCrypt加密）',
      `phone` VARCHAR(20) NULL COMMENT '手机号',
      `phone_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '手机号是否验证：0-否 1-是',
      `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
      `email_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '邮箱是否验证：0-否 1-是',
      `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
      `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
      `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
      `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
      `register_ip` VARCHAR(50) DEFAULT NULL COMMENT '注册IP',
      `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
      `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_username` (`username`),
      UNIQUE KEY `uk_phone` (`phone`),
      UNIQUE KEY `uk_email` (`email`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP用户表';

-- APP用户第三方登录关联表
CREATE TABLE `grid_user_auth` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `provider` VARCHAR(30) NOT NULL COMMENT '第三方提供商：google/wechat/github/apple等',
    `provider_id` VARCHAR(100) NOT NULL COMMENT '第三方唯一标识',
    `provider_name` VARCHAR(100) DEFAULT NULL COMMENT '第三方用户名',
    `provider_avatar` VARCHAR(500) DEFAULT NULL COMMENT '第三方头像',
    `access_token` VARCHAR(500) DEFAULT NULL COMMENT '第三方访问令牌',
    `refresh_token` VARCHAR(500) DEFAULT NULL COMMENT '第三方刷新令牌',
    `expire_time` DATETIME DEFAULT NULL COMMENT '令牌过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider` (`provider`, `provider_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP用户第三方登录关联表';

-- APP用户角色表
CREATE TABLE `grid_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_code` VARCHAR(30) NOT NULL COMMENT '角色编码：NORMAL/VIP',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称：普通用户/VIP用户',
    `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间（VIP用）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_code`),
    KEY `idx_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP用户角色表';

-- APP用户Token表
CREATE TABLE `grid_user_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `device_id` VARCHAR(100) NOT NULL COMMENT '设备ID',
    `device_name` VARCHAR(100) DEFAULT NULL COMMENT '设备名称',
    `refresh_token` VARCHAR(500) NOT NULL COMMENT '刷新令牌',
    `access_token` VARCHAR(500) DEFAULT NULL COMMENT '当前访问令牌',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_device` (`user_id`, `device_id`),
    KEY `idx_refresh_token` (`refresh_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP用户Token表';

