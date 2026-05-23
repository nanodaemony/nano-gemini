

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

-- 创建 APP用户第三方登录关联表
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

-- 创建 APP用户角色表
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

-- 创建 APP用户Token表
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

################################################################## 后台用户相关 #####################################################################

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '部门名称',
  `username` varchar(180) DEFAULT NULL COMMENT '用户名',
  `nick_name` varchar(255) DEFAULT NULL COMMENT '昵称',
  `gender` varchar(2) DEFAULT NULL COMMENT '性别',
  `phone` varchar(255) DEFAULT NULL COMMENT '手机号码',
  `email` varchar(180) DEFAULT NULL COMMENT '邮箱',
  `avatar_name` varchar(255) DEFAULT NULL COMMENT '头像地址',
  `avatar_path` varchar(255) DEFAULT NULL COMMENT '头像真实路径',
  `password` varchar(255) DEFAULT NULL COMMENT '密码',
  `is_admin` bit(1) DEFAULT b'0' COMMENT '是否为admin账号',
  `enabled` bit(1) DEFAULT NULL COMMENT '状态：1启用、0禁用',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `pwd_reset_time` datetime DEFAULT NULL COMMENT '修改密码的时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE KEY `uniq_email` (`email`) USING BTREE,
  UNIQUE KEY `uniq_username` (`username`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='系统用户表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` (`user_id`, `dept_id`, `username`, `nick_name`, `gender`, `phone`, `email`, `avatar_name`, `avatar_path`, `password`, `is_admin`, `enabled`, `create_by`, `update_by`, `pwd_reset_time`, `create_time`, `update_time`) VALUES (1, 2, 'admin', '管理员', '男', '18888888888', '201507802@qq.com', 'avatar-20250122102642222.png', '/Users/jie/Documents/work/private/eladmin/~/avatar/avatar-20250122102642222.png', '$2a$10$Egp1/gvFlt7zhlXVfEFw4OfWQCGPw0ClmMcc6FjTnvXNRVf9zdMRa', b'1', b'1', NULL, 'admin', '2020-05-03 16:38:31', '2018-08-23 09:11:56', '2025-01-22 10:26:42');
INSERT INTO `sys_user` (`user_id`, `dept_id`, `username`, `nick_name`, `gender`, `phone`, `email`, `avatar_name`, `avatar_path`, `password`, `is_admin`, `enabled`, `create_by`, `update_by`, `pwd_reset_time`, `create_time`, `update_time`) VALUES (2, 7, 'test', '测试', '男', '19999999999', '231@qq.com', NULL, NULL, '$2a$10$4XcyudOYTSz6fue6KFNMHeUQnCX5jbBQypLEnGk1PmekXt5c95JcK', b'0', b'1', 'admin', 'admin', NULL, '2020-05-05 11:15:49', '2025-01-21 14:53:04');
COMMIT;


-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(100) NOT NULL COMMENT '名称',
  `level` int(50) DEFAULT NULL COMMENT '角色级别',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `data_scope` varchar(255) DEFAULT NULL COMMENT '数据权限',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`role_id`) USING BTREE,
  UNIQUE KEY `uniq_name` (`name`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='角色表';

-- ----------------------------
-- Records of sys_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_role` (`role_id`, `name`, `level`, `description`, `data_scope`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (1, '管理员', 1, '-', '全部', NULL, 'admin', '2018-11-23 11:04:37', '2025-01-21 14:53:13');
INSERT INTO `sys_role` (`role_id`, `name`, `level`, `description`, `data_scope`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (2, '普通用户', 2, '-', '本级', NULL, 'admin', '2018-11-23 13:09:06', '2020-09-05 10:45:12');
COMMIT;


-- ----------------------------
-- 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_users_roles`;
CREATE TABLE `sys_users_roles` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='用户角色关联';

-- ----------------------------
-- Records of sys_users_roles
-- ----------------------------
BEGIN;
INSERT INTO `sys_users_roles` (`user_id`, `role_id`) VALUES (1, 1);
INSERT INTO `sys_users_roles` (`user_id`, `role_id`) VALUES (2, 2);
COMMIT;

################################################################## 基础资源表 #####################################################################


-- ----------------------------
-- OSS资源元数据表
-- ----------------------------
DROP TABLE IF EXISTS `oss_resource_meta`;
CREATE TABLE `oss_resource_meta` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `file_real_name` varchar(255) NOT NULL COMMENT '真实存储的名称',
  `file_size` varchar(100) NOT NULL COMMENT '文件大小',
  `file_mime_type` varchar(50) NOT NULL COMMENT '文件MIME 类型',
  `file_type` varchar(50) NOT NULL COMMENT '文件类型',
  `file_url` tinytext NOT NULL COMMENT '文件访问地址',
  `bucket_name` varchar(255) NOT NULL COMMENT 'Bucket 名称',
  `business_type` varchar(100) DEFAULT NULL COMMENT '业务类型',
  `custom_path` varchar(255) DEFAULT NULL COMMENT '自定义路径',
  `create_by` varchar(255) NOT NULL COMMENT '创建者',
  `update_by` varchar(255) NOT NULL COMMENT '更新者',
  `create_time` datetime NOT NULL COMMENT '创建日期',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='OSS资源元数据';

-- ----------------------------
-- TTS语音合成记录表
-- ----------------------------
DROP TABLE IF EXISTS `tts_record`;
CREATE TABLE `tts_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `voice` varchar(255) NOT NULL COMMENT '音色',
    `text` text NOT NULL COMMENT '合成文本',
    `instructions` text DEFAULT NULL COMMENT '指令',
    `model` varchar(255) NOT NULL COMMENT '模型',
    `vendor` varchar(50) DEFAULT 'aliyun' COMMENT '厂商: aliyun/volcengine',
    `language_type` varchar(255) DEFAULT NULL COMMENT '语言类型',
    `final_audio_url` varchar(255) NOT NULL COMMENT '最终音频地址',
    `request_id` varchar(255) NOT NULL COMMENT '请求ID',
    `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
    `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
    `create_time` datetime DEFAULT NULL COMMENT '创建日期',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='TTS语音合成记录';

-- ----------------------------
-- 系统日志表
-- ----------------------------
CREATE TABLE `sys_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `log_type` varchar(10) NOT NULL COMMENT '日志类型：INFI/ERROR',
  `method` varchar(255) DEFAULT NULL COMMENT '方法名',
  `params` text DEFAULT NULL COMMENT '参数',
  `request_ip` varchar(255) DEFAULT NULL COMMENT '请求IP',
  `time` bigint(20) DEFAULT NULL COMMENT '执行时间',
  `username` varchar(255) DEFAULT NULL COMMENT '用户名',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `browser` varchar(255) DEFAULT NULL COMMENT '浏览器',
  `exception_detail` text DEFAULT NULL COMMENT '异常',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  KEY `idx_create_time_index` (`create_time`),
  KEY `idx_log_type` (`log_type`)
) ENGINE=InnoDB AUTO_INCREMENT=3636 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='系统日志';
