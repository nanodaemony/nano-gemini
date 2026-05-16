-- 初始化角色数据
-- 先删除可能存在的角色-菜单关联表（因为我们移除了菜单模块）
DROP TABLE IF EXISTS `sys_roles_menus`;

-- 插入角色数据
INSERT INTO `sys_role` (`role_id`, `name`, `level`, `description`, `data_scope`, `create_by`, `update_by`, `create_time`, `update_time`) VALUES
(1, 'admin', 1, '超级管理员', '全部', NULL, NULL, NOW(), NOW()),
(2, 'editor', 2, '编辑者', '本级', NULL, NULL, NOW(), NOW()),
(3, 'visitor', 3, '访客', '本级', NULL, NULL, NOW(), NOW());
