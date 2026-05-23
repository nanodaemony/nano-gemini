/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MariaDB
 Source Server Version : 110206 (11.2.6-MariaDB)
 Source Host           : localhost:3306
 Source Schema         : eladmin

 Target Server Type    : MariaDB
 Target Server Version : 110206 (11.2.6-MariaDB)
 File Encoding         : 65001

 Date: 25/06/2025 15:56:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for code_column
-- ----------------------------
DROP TABLE IF EXISTS `code_column`;
CREATE TABLE `code_column` (
  `column_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `table_name` varchar(180) DEFAULT NULL COMMENT '表名',
  `column_name` varchar(255) DEFAULT NULL COMMENT '数据库字段名称',
  `column_type` varchar(255) DEFAULT NULL COMMENT '数据库字段类型',
  `dict_name` varchar(255) DEFAULT NULL COMMENT '字典名称',
  `extra` varchar(255) DEFAULT NULL COMMENT '字段额外的参数',
  `form_show` bit(1) DEFAULT NULL COMMENT '是否表单显示',
  `form_type` varchar(255) DEFAULT NULL COMMENT '表单类型',
  `key_type` varchar(255) DEFAULT NULL COMMENT '数据库字段键类型',
  `list_show` bit(1) DEFAULT NULL COMMENT '是否在列表显示',
  `not_null` bit(1) DEFAULT NULL COMMENT '是否为空',
  `query_type` varchar(255) DEFAULT NULL COMMENT '查询类型',
  `remark` varchar(255) DEFAULT NULL COMMENT '描述',
  `date_annotation` varchar(255) DEFAULT NULL COMMENT '日期注解',
  PRIMARY KEY (`column_id`) USING BTREE,
  KEY `idx_table_name` (`table_name`)
) ENGINE=InnoDB AUTO_INCREMENT=266 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='代码生成字段信息存储';

-- ----------------------------
-- Records of code_column
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mnt_app
-- ----------------------------
DROP TABLE IF EXISTS `mnt_app`;
CREATE TABLE `mnt_app` (
  `app_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(255) DEFAULT NULL COMMENT '应用名称',
  `upload_path` varchar(255) DEFAULT NULL COMMENT '上传目录',
  `deploy_path` varchar(255) DEFAULT NULL COMMENT '部署路径',
  `backup_path` varchar(255) DEFAULT NULL COMMENT '备份路径',
  `port` int(255) DEFAULT NULL COMMENT '应用端口',
  `start_script` varchar(4000) DEFAULT NULL COMMENT '启动脚本',
  `deploy_script` varchar(4000) DEFAULT NULL COMMENT '部署脚本',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`app_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='应用管理';

-- ----------------------------
-- Records of mnt_app
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mnt_database
-- ----------------------------
DROP TABLE IF EXISTS `mnt_database`;
CREATE TABLE `mnt_database` (
  `db_id` varchar(50) NOT NULL COMMENT 'ID',
  `name` varchar(255) NOT NULL COMMENT '名称',
  `jdbc_url` varchar(255) NOT NULL COMMENT 'jdbc连接',
  `user_name` varchar(255) NOT NULL COMMENT '账号',
  `pwd` varchar(255) NOT NULL COMMENT '密码',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`db_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='数据库管理';

-- ----------------------------
-- Records of mnt_database
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mnt_deploy
-- ----------------------------
DROP TABLE IF EXISTS `mnt_deploy`;
CREATE TABLE `mnt_deploy` (
  `deploy_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `app_id` bigint(20) DEFAULT NULL COMMENT '应用编号',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`deploy_id`) USING BTREE,
  KEY `idx_app_id` (`app_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='部署管理';

-- ----------------------------
-- Records of mnt_deploy
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mnt_deploy_history
-- ----------------------------
DROP TABLE IF EXISTS `mnt_deploy_history`;
CREATE TABLE `mnt_deploy_history` (
  `history_id` varchar(50) NOT NULL COMMENT 'ID',
  `app_name` varchar(255) NOT NULL COMMENT '应用名称',
  `deploy_date` datetime NOT NULL COMMENT '部署日期',
  `deploy_user` varchar(50) NOT NULL COMMENT '部署用户',
  `ip` varchar(20) NOT NULL COMMENT '服务器IP',
  `deploy_id` bigint(20) DEFAULT NULL COMMENT '部署编号',
  PRIMARY KEY (`history_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='部署历史管理';

-- ----------------------------
-- Records of mnt_deploy_history
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mnt_deploy_server
-- ----------------------------
DROP TABLE IF EXISTS `mnt_deploy_server`;
CREATE TABLE `mnt_deploy_server` (
  `deploy_id` bigint(20) NOT NULL COMMENT '部署ID',
  `server_id` bigint(20) NOT NULL COMMENT '服务ID',
  PRIMARY KEY (`deploy_id`,`server_id`) USING BTREE,
  KEY `idx_deploy_id` (`deploy_id`),
  KEY `idx_server_id` (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='应用与服务器关联';

-- ----------------------------
-- Records of mnt_deploy_server
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for mnt_server
-- ----------------------------
DROP TABLE IF EXISTS `mnt_server`;
CREATE TABLE `mnt_server` (
  `server_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `account` varchar(50) DEFAULT NULL COMMENT '账号',
  `ip` varchar(20) DEFAULT NULL COMMENT 'IP地址',
  `name` varchar(100) DEFAULT NULL COMMENT '名称',
  `password` varchar(100) DEFAULT NULL COMMENT '密码',
  `port` int(11) DEFAULT NULL COMMENT '端口',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`server_id`) USING BTREE,
  KEY `idx_ip` (`ip`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='服务器管理';

-- ----------------------------
-- Records of mnt_server
-- ----------------------------
BEGIN;
COMMIT;


-- ----------------------------
-- Table structure for sys_quartz_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_quartz_job`;
CREATE TABLE `sys_quartz_job` (
  `job_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `bean_name` varchar(255) DEFAULT NULL COMMENT 'Spring Bean名称',
  `cron_expression` varchar(255) DEFAULT NULL COMMENT 'cron 表达式',
  `is_pause` bit(1) DEFAULT NULL COMMENT '状态：1暂停、0启用',
  `job_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `method_name` varchar(255) DEFAULT NULL COMMENT '方法名称',
  `params` varchar(255) DEFAULT NULL COMMENT '参数',
  `description` varchar(255) DEFAULT NULL COMMENT '备注',
  `person_in_charge` varchar(100) DEFAULT NULL COMMENT '负责人',
  `email` varchar(100) DEFAULT NULL COMMENT '报警邮箱',
  `sub_task` varchar(100) DEFAULT NULL COMMENT '子任务ID',
  `pause_after_failure` bit(1) DEFAULT NULL COMMENT '任务失败后是否暂停',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`job_id`) USING BTREE,
  KEY `idx_is_pause` (`is_pause`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='定时任务';

-- ----------------------------
-- Records of sys_quartz_job
-- ----------------------------
BEGIN;
INSERT INTO `sys_quartz_job` (`job_id`, `bean_name`, `cron_expression`, `is_pause`, `job_name`, `method_name`, `params`, `description`, `person_in_charge`, `email`, `sub_task`, `pause_after_failure`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (2, 'testTask', '0/5 * * * * ?', b'1', '测试1', 'run1', 'test', '带参测试，多参使用json', '测试', NULL, NULL, NULL, NULL, 'admin', '2019-08-22 14:08:29', '2020-05-24 13:58:33');
INSERT INTO `sys_quartz_job` (`job_id`, `bean_name`, `cron_expression`, `is_pause`, `job_name`, `method_name`, `params`, `description`, `person_in_charge`, `email`, `sub_task`, `pause_after_failure`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (3, 'testTask', '0/5 * * * * ?', b'1', '测试', 'run', '', '不带参测试', 'Zheng Jie', '', '6', b'1', NULL, 'admin', '2019-09-26 16:44:39', '2020-05-24 14:48:12');
INSERT INTO `sys_quartz_job` (`job_id`, `bean_name`, `cron_expression`, `is_pause`, `job_name`, `method_name`, `params`, `description`, `person_in_charge`, `email`, `sub_task`, `pause_after_failure`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (5, 'Test', '0/5 * * * * ?', b'1', '任务告警测试', 'run', NULL, '测试', 'test', '', NULL, b'1', 'admin', 'admin', '2020-05-05 20:32:41', '2020-05-05 20:36:13');
INSERT INTO `sys_quartz_job` (`job_id`, `bean_name`, `cron_expression`, `is_pause`, `job_name`, `method_name`, `params`, `description`, `person_in_charge`, `email`, `sub_task`, `pause_after_failure`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (6, 'testTask', '0/5 * * * * ?', b'1', '测试3', 'run2', NULL, '测试3', 'Zheng Jie', '', NULL, b'1', 'admin', 'admin', '2020-05-05 20:35:41', '2020-05-05 20:36:07');
COMMIT;

-- ----------------------------
-- Table structure for sys_quartz_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_quartz_log`;
CREATE TABLE `sys_quartz_log` (
  `log_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `bean_name` varchar(255) DEFAULT NULL COMMENT 'Bean名称',
  `cron_expression` varchar(255) DEFAULT NULL COMMENT 'cron 表达式',
  `is_success` bit(1) DEFAULT NULL COMMENT '是否执行成功',
  `job_name` varchar(255) DEFAULT NULL COMMENT '任务名称',
  `method_name` varchar(255) DEFAULT NULL COMMENT '方法名称',
  `params` varchar(255) DEFAULT NULL COMMENT '参数',
  `time` bigint(20) DEFAULT NULL COMMENT '执行耗时',
  `exception_detail` text DEFAULT NULL COMMENT '异常详情',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=262 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='定时任务日志';

-- ----------------------------
-- Records of sys_quartz_log
-- ----------------------------
BEGIN;
COMMIT;


-- ----------------------------
-- Table structure for sys_roles_depts
-- ----------------------------
DROP TABLE IF EXISTS `sys_roles_depts`;
CREATE TABLE `sys_roles_depts` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='角色部门关联';

-- ----------------------------
-- Records of sys_roles_depts
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_roles_menus
-- ----------------------------
DROP TABLE IF EXISTS `sys_roles_menus`;
CREATE TABLE `sys_roles_menus` (
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`menu_id`,`role_id`) USING BTREE,
  KEY `idx_menu_id` (`menu_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='角色菜单关联';

-- ----------------------------
-- Records of sys_roles_menus
-- ----------------------------
BEGIN;
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (1, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (1, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (2, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (2, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (3, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (5, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (6, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (6, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (7, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (7, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (9, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (9, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (10, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (10, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (11, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (11, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (14, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (14, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (15, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (15, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (18, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (19, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (19, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (21, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (21, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (22, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (22, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (23, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (23, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (24, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (24, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (27, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (27, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (28, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (30, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (30, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (32, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (32, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (33, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (33, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (34, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (34, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (35, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (36, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (36, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (37, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (39, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (41, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (44, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (45, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (46, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (48, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (49, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (50, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (52, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (53, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (54, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (56, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (57, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (58, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (60, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (61, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (62, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (64, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (65, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (66, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (73, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (74, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (75, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (77, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (78, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (79, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (80, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (80, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (82, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (82, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (83, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (83, 2);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (90, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (92, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (93, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (94, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (97, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (98, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (102, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (103, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (104, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (105, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (106, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (107, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (108, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (109, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (110, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (111, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (116, 1);
INSERT INTO `sys_roles_menus` (`menu_id`, `role_id`) VALUES (116, 2);
COMMIT;


-- ----------------------------
-- Table structure for sys_users_jobs
-- ----------------------------
DROP TABLE IF EXISTS `sys_users_jobs`;
CREATE TABLE `sys_users_jobs` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `job_id` bigint(20) NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`job_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci COMMENT='用户与岗位关联表';

-- ----------------------------
-- Records of sys_users_jobs
-- ----------------------------
BEGIN;
INSERT INTO `sys_users_jobs` (`user_id`, `job_id`) VALUES (1, 11);
INSERT INTO `sys_users_jobs` (`user_id`, `job_id`) VALUES (2, 12);
COMMIT;



-- ----------------------------
-- Table structure for tool_alipay_config
-- ----------------------------
DROP TABLE IF EXISTS `tool_alipay_config`;
CREATE TABLE `tool_alipay_config` (
  `config_id` bigint(20) NOT NULL COMMENT 'ID',
  `app_id` varchar(255) DEFAULT NULL COMMENT '应用ID',
  `charset` varchar(255) DEFAULT NULL COMMENT '编码',
  `format` varchar(255) DEFAULT NULL COMMENT '类型 固定格式json',
  `gateway_url` varchar(255) DEFAULT NULL COMMENT '网关地址',
  `notify_url` varchar(255) DEFAULT NULL COMMENT '异步回调',
  `private_key` text DEFAULT NULL COMMENT '私钥',
  `public_key` text DEFAULT NULL COMMENT '公钥',
  `return_url` varchar(255) DEFAULT NULL COMMENT '回调地址',
  `sign_type` varchar(255) DEFAULT NULL COMMENT '签名方式',
  `sys_service_provider_id` varchar(255) DEFAULT NULL COMMENT '商户号',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='支付宝配置类';

-- ----------------------------
-- Records of tool_alipay_config
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for tool_email_config
-- ----------------------------
DROP TABLE IF EXISTS `tool_email_config`;
CREATE TABLE `tool_email_config` (
  `config_id` bigint(20) NOT NULL COMMENT 'ID',
  `from_user` varchar(255) DEFAULT NULL COMMENT '收件人',
  `host` varchar(255) DEFAULT NULL COMMENT '邮件服务器SMTP地址',
  `pass` varchar(255) DEFAULT NULL COMMENT '密码',
  `port` varchar(255) DEFAULT NULL COMMENT '端口',
  `user` varchar(255) DEFAULT NULL COMMENT '发件者用户名',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='邮箱配置';

-- ----------------------------
-- Records of tool_email_config
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for tool_local_storage
-- ----------------------------
DROP TABLE IF EXISTS `tool_local_storage`;
CREATE TABLE `tool_local_storage` (
  `storage_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `real_name` varchar(255) DEFAULT NULL COMMENT '文件真实的名称',
  `name` varchar(255) DEFAULT NULL COMMENT '文件名',
  `suffix` varchar(255) DEFAULT NULL COMMENT '后缀',
  `path` varchar(255) DEFAULT NULL COMMENT '路径',
  `type` varchar(255) DEFAULT NULL COMMENT '类型',
  `size` varchar(100) DEFAULT NULL COMMENT '大小',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建日期',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`storage_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=COMPACT COMMENT='本地存储';

-- ----------------------------
-- Records of tool_local_storage
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for tool_s3_storage
-- ----------------------------
DROP TABLE IF EXISTS `tool_s3_storage`;
CREATE TABLE `tool_s3_storage` (
  `storage_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `file_real_name` varchar(255) NOT NULL COMMENT '真实存储的名称',
  `file_size` varchar(100) NOT NULL COMMENT '文件大小',
  `file_mime_type` varchar(50) NOT NULL COMMENT '文件MIME 类型',
  `file_type` varchar(50) NOT NULL COMMENT '文件类型',
  `file_path` tinytext NOT NULL COMMENT '文件路径',
  `create_by` varchar(255) NOT NULL COMMENT '创建者',
  `update_by` varchar(255) NOT NULL COMMENT '更新者',
  `create_time` datetime NOT NULL COMMENT '创建日期',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`storage_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='s3 协议对象存储';

-- ----------------------------
-- Records of tool_s3_storage
-- ----------------------------
BEGIN;
INSERT INTO `tool_s3_storage` (`storage_id`, `file_name`, `file_real_name`, `file_size`, `file_mime_type`, `file_type`, `file_path`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES (4, 'tx.jpg', '2ca1de24d8fa422eae4ede30e97c46d8.jpg', '29.67KB', 'image/jpeg', 'jpg', '2025-06/2ca1de24d8fa422eae4ede30e97c46d8.jpg', 'admin', 'admin', '2025-06-25 15:48:22', '2025-06-25 15:48:22');
COMMIT;


SET FOREIGN_KEY_CHECKS = 1;
