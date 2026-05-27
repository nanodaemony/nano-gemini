################################################################## 后台用户相关 #####################################################################

-- ----------------------------
-- 系统用户表
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
-- 初始化系统用户数据
-- ----------------------------
INSERT INTO `sys_user` (`user_id`, `dept_id`, `username`, `nick_name`, `gender`, `phone`, `email`, `avatar_name`, `avatar_path`, `password`, `is_admin`, `enabled`, `create_by`, `update_by`, `pwd_reset_time`, `create_time`, `update_time`) VALUES (1, 2, 'admin', '管理员', '男', '18888888888', '201507802@qq.com', 'avatar-20250122102642222.png', '/Users/jie/Documents/work/private/eladmin/~/avatar/avatar-20250122102642222.png', '$2a$10$Egp1/gvFlt7zhlXVfEFw4OfWQCGPw0ClmMcc6FjTnvXNRVf9zdMRa', b'1', b'1', NULL, 'admin', '2020-05-03 16:38:31', '2018-08-23 09:11:56', '2025-01-22 10:26:42');
INSERT INTO `sys_user` (`user_id`, `dept_id`, `username`, `nick_name`, `gender`, `phone`, `email`, `avatar_name`, `avatar_path`, `password`, `is_admin`, `enabled`, `create_by`, `update_by`, `pwd_reset_time`, `create_time`, `update_time`) VALUES (2, 7, 'test', '测试', '男', '19999999999', '231@qq.com', NULL, NULL, '$2a$10$4XcyudOYTSz6fue6KFNMHeUQnCX5jbBQypLEnGk1PmekXt5c95JcK', b'0', b'1', 'admin', 'admin', NULL, '2020-05-05 11:15:49', '2025-01-21 14:53:04');

-- ----------------------------
-- 系统角色表
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
-- 初始化系统角色
-- ----------------------------
INSERT INTO `sys_role` (`role_id`, `name`, `level`, `description`, `data_scope`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (1, '管理员', 1, '-', '全部', NULL, 'admin', '2018-11-23 11:04:37', '2025-01-21 14:53:13');
INSERT INTO `sys_role` (`role_id`, `name`, `level`, `description`, `data_scope`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (2, '普通用户', 2, '-', '本级', NULL, 'admin', '2018-11-23 13:09:06', '2020-09-05 10:45:12');


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
-- 初始化用户角色关联表
-- ----------------------------
INSERT INTO `sys_users_roles` (`user_id`, `role_id`) VALUES (1, 1);
INSERT INTO `sys_users_roles` (`user_id`, `role_id`) VALUES (2, 2);
