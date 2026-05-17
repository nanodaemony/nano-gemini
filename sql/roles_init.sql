-- 初始化角色数据
-- 插入角色数据
INSERT INTO sys_role (name, description, create_time, update_time) VALUES
('admin', '超级管理员', NOW(), NOW()),
('editor', '编辑者', NOW(), NOW()),
('visitor', '访客', NOW(), NOW());
