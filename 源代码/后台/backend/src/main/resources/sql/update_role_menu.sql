-- 更新实习检修人员的菜单权限，移除案例审核权限
-- 实习检修人员 (roleId=3): 数据看板、多模态检索、知识库(知识库管理、设备管理)、个人中心
DELETE FROM sys_role_menu WHERE role_id = 3;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 1), -- 数据看板
(3, 2), -- 多模态检索
(3, 4), -- 知识库
(3, 6), -- 个人中心
(3, 20), -- 知识库管理
(3, 21); -- 设备管理

-- 资深检修工程师 (roleId=2): 添加案例审核权限
DELETE FROM sys_role_menu WHERE role_id = 2;

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), -- 数据看板
(2, 2), -- 多模态检索
(2, 3), -- 作业管理
(2, 4), -- 知识库
(2, 6), -- 个人中心
(2, 10), -- 检修作业指引
(2, 11), -- 案例审核
(2, 20), -- 知识库管理
(2, 21); -- 设备管理
