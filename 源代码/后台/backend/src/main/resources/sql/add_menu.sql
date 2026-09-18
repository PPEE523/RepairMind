-- 添加AI修正统计菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order) VALUES
(12, 3, 'AI修正统计', '/ai-feedback', 'AiFeedbackDashboard', NULL, 3);

-- 管理员拥有所有菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 12);

-- 资深检修工程师拥有AI修正统计权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 12);
