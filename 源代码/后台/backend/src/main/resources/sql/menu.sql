-- ============================================
-- 菜单表
-- ============================================
DROP TABLE IF EXISTS sys_menu CASCADE;
CREATE TABLE sys_menu (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    name VARCHAR(50) NOT NULL,
    path VARCHAR(100),
    component VARCHAR(100),
    icon VARCHAR(50),
    sort_order INTEGER DEFAULT 0,
    visible SMALLINT DEFAULT 1,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_menu IS '菜单表';
COMMENT ON COLUMN sys_menu.id IS '菜单ID';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单ID';
COMMENT ON COLUMN sys_menu.name IS '菜单名称';
COMMENT ON COLUMN sys_menu.path IS '路由路径';
COMMENT ON COLUMN sys_menu.component IS '组件路径';
COMMENT ON COLUMN sys_menu.icon IS '图标';
COMMENT ON COLUMN sys_menu.sort_order IS '排序';
COMMENT ON COLUMN sys_menu.visible IS '是否可见: 0-隐藏 1-显示';
COMMENT ON COLUMN sys_menu.status IS '状态: 0-禁用 1-启用';

-- ============================================
-- 角色菜单关联表
-- ============================================
DROP TABLE IF EXISTS sys_role_menu CASCADE;
CREATE TABLE sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role_id, menu_id)
);

COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';
COMMENT ON COLUMN sys_role_menu.id IS 'ID';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单ID';

CREATE INDEX idx_role_menu_role ON sys_role_menu(role_id);
CREATE INDEX idx_role_menu_menu ON sys_role_menu(menu_id);

-- ============================================
-- 初始化菜单数据
-- ============================================
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort_order) VALUES
-- 一级菜单
(1, 0, '数据看板', '/dashboard', 'Dashboard', 'DataAnalysis', 1),
(2, 0, '多模态检索', '/search', 'Search', 'Search', 2),
(3, 0, '作业管理', NULL, NULL, 'Document', 3),
(4, 0, '知识库', NULL, NULL, 'Collection', 4),
(5, 0, '系统管理', NULL, NULL, 'Setting', 5),
(6, 0, '个人中心', '/profile', 'Profile', 'UserFilled', 6),

-- 作业管理子菜单
(10, 3, '检修作业指引', '/workflow', 'WorkFlow', NULL, 1),
(11, 3, '案例审核', '/audit', 'Audit', NULL, 2),

-- 知识库子菜单
(20, 4, '知识库管理', '/knowledge', 'Knowledge', NULL, 1),
(21, 4, '设备管理', '/equipment', 'EquipmentManagement', NULL, 2),

-- 系统管理子菜单
(30, 5, '用户管理', '/user-management', 'UserManagement', NULL, 1),
(31, 5, '角色管理', '/role', 'RoleManagement', NULL, 2),
(32, 5, '系统配置', '/system', 'System', NULL, 3);

-- ============================================
-- 初始化角色菜单关联
-- ============================================

-- 系统管理员 (roleId=1): 拥有所有菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
(1, 10), (1, 11), (1, 20), (1, 21), (1, 30), (1, 31), (1, 32);

-- 资深检修工程师 (roleId=2): 数据看板、多模态检索、作业管理(检修作业指引)、知识库(知识库管理、设备管理)、个人中心
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 6),
(2, 10), (2, 20), (2, 21);

-- 实习检修人员 (roleId=3): 数据看板、多模态检索、知识库(只读)、个人中心
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 1), (3, 2), (3, 4), (3, 6),
(3, 20), (3, 21);
