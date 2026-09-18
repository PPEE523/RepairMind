-- 添加知识案例菜单
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order) VALUES
(13, 4, '知识案例', '/case', 'CaseManagement', NULL, 2);

-- 所有角色都可以访问知识案例
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 13), (2, 13), (3, 13);
