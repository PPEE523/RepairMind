-- ============================================
-- 设备检修知识检索与作业辅助系统 - 数据库初始化脚本
-- 数据库: PostgreSQL 14+
-- 创建日期: 2026-06-05
-- ============================================

-- 创建数据库（如果需要单独创建）
-- CREATE DATABASE repair_db WITH ENCODING='UTF8' LC_COLLATE='zh_CN.UTF-8';

-- 使用数据库
-- \c repair_db;

-- ============================================
-- 1. 用户角色表
-- ============================================
DROP TABLE IF EXISTS sys_role CASCADE;
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.id IS '角色ID';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.role_code IS '角色编码';
COMMENT ON COLUMN sys_role.description IS '角色描述';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';

-- ============================================
-- 2. 用户表
-- ============================================
DROP TABLE IF EXISTS sys_user CASCADE;
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    role_id BIGINT,
    dept_id BIGINT,
    status SMALLINT DEFAULT 1,
    password_update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码(BCrypt加密)';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.role_id IS '角色ID';
COMMENT ON COLUMN sys_user.dept_id IS '部门/产线ID';
COMMENT ON COLUMN sys_user.status IS '状态: 0-禁用 1-启用';
COMMENT ON COLUMN sys_user.password_update_time IS '密码更新时间';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';

-- ============================================
-- 3. 设备档案表
-- ============================================
DROP TABLE IF EXISTS equipment_info CASCADE;
CREATE TABLE equipment_info (
    id BIGSERIAL PRIMARY KEY,
    equip_type VARCHAR(50) NOT NULL,
    equip_model VARCHAR(100) NOT NULL,
    dept_id BIGINT,
    manual_path VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE equipment_info IS '设备档案表';
COMMENT ON COLUMN equipment_info.id IS '设备ID';
COMMENT ON COLUMN equipment_info.equip_type IS '设备类型';
COMMENT ON COLUMN equipment_info.equip_model IS '设备型号';
COMMENT ON COLUMN equipment_info.dept_id IS '所属部门/产线ID';
COMMENT ON COLUMN equipment_info.manual_path IS '维修手册路径';
COMMENT ON COLUMN equipment_info.create_time IS '创建时间';

-- ============================================
-- 4. 检修案例表
-- ============================================
DROP TABLE IF EXISTS repair_knowledge CASCADE;
CREATE TABLE repair_knowledge (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    symptom TEXT,
    solution TEXT,
    img_url VARCHAR(500),
    images TEXT,
    equip_model VARCHAR(100),
    audit_status SMALLINT DEFAULT 0,
    upload_user_id BIGINT,
    audit_user_id BIGINT,
    audit_remark VARCHAR(500),
    document_id VARCHAR(500),
    dataset_id VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE repair_knowledge IS '检修案例表';
COMMENT ON COLUMN repair_knowledge.id IS '案例ID';
COMMENT ON COLUMN repair_knowledge.title IS '案例标题';
COMMENT ON COLUMN repair_knowledge.content IS '案例内容';
COMMENT ON COLUMN repair_knowledge.img_url IS '故障图片URL';
COMMENT ON COLUMN repair_knowledge.images IS '案例图片URL列表（JSON数组格式，最多5张）';
COMMENT ON COLUMN repair_knowledge.equip_model IS '设备型号';
COMMENT ON COLUMN repair_knowledge.audit_status IS '审核状态: 0-待审核 1-已通过 2-已驳回';
COMMENT ON COLUMN repair_knowledge.upload_user_id IS '上传用户ID';
COMMENT ON COLUMN repair_knowledge.audit_user_id IS '审核用户ID';
COMMENT ON COLUMN repair_knowledge.audit_remark IS '审核备注';
COMMENT ON COLUMN repair_knowledge.create_time IS '创建时间';
COMMENT ON COLUMN repair_knowledge.update_time IS '更新时间';

-- ============================================
-- 5. 标准化作业表
-- ============================================
DROP TABLE IF EXISTS work_flow CASCADE;
CREATE TABLE work_flow (
    id BIGSERIAL PRIMARY KEY,
    equip_model VARCHAR(100) NOT NULL,
    repair_level VARCHAR(50) NOT NULL,
    step_json TEXT NOT NULL,
    version INTEGER DEFAULT 1,
    create_user BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE work_flow IS '标准化作业表';
COMMENT ON COLUMN work_flow.id IS '流程ID';
COMMENT ON COLUMN work_flow.equip_model IS '设备型号';
COMMENT ON COLUMN work_flow.repair_level IS '检修等级: 日常保养/故障维修/大修';
COMMENT ON COLUMN work_flow.step_json IS '步骤JSON数据';
COMMENT ON COLUMN work_flow.version IS '版本号';
COMMENT ON COLUMN work_flow.create_user IS '创建用户ID';
COMMENT ON COLUMN work_flow.create_time IS '创建时间';

-- ============================================
-- 5.1 SOP流程表（AI生成）
-- ============================================
DROP TABLE IF EXISTS work_sop_flow CASCADE;
CREATE TABLE work_sop_flow (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    source_document_id VARCHAR(100),
    source_document_name VARCHAR(200),
    dataset_id VARCHAR(100),
    step_json TEXT NOT NULL,
    total_steps INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'active',
    create_user BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE work_sop_flow IS 'SOP流程表（AI生成）';
COMMENT ON COLUMN work_sop_flow.id IS '流程ID';
COMMENT ON COLUMN work_sop_flow.name IS '流程名称';
COMMENT ON COLUMN work_sop_flow.source_document_id IS '来源文档ID';
COMMENT ON COLUMN work_sop_flow.source_document_name IS '来源文档名称';
COMMENT ON COLUMN work_sop_flow.dataset_id IS '来源知识库ID';
COMMENT ON COLUMN work_sop_flow.step_json IS '步骤JSON数据';
COMMENT ON COLUMN work_sop_flow.total_steps IS '总步骤数';
COMMENT ON COLUMN work_sop_flow.status IS '状态: active/archived';
COMMENT ON COLUMN work_sop_flow.create_user IS '创建用户ID';
COMMENT ON COLUMN work_sop_flow.create_time IS '创建时间';
COMMENT ON COLUMN work_sop_flow.update_time IS '更新时间';
CREATE INDEX idx_sop_flow_status ON work_sop_flow(status);
CREATE INDEX idx_sop_flow_create_user ON work_sop_flow(create_user);

-- ============================================
-- 6. 操作日志表
-- ============================================
DROP TABLE IF EXISTS sys_oper_log CASCADE;
CREATE TABLE sys_oper_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    oper_type VARCHAR(50),
    content VARCHAR(500),
    oper_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50)
);

COMMENT ON TABLE sys_oper_log IS '操作日志表';
COMMENT ON COLUMN sys_oper_log.id IS '日志ID';
COMMENT ON COLUMN sys_oper_log.user_id IS '用户ID';
COMMENT ON COLUMN sys_oper_log.oper_type IS '操作类型';
COMMENT ON COLUMN sys_oper_log.content IS '操作内容';
COMMENT ON COLUMN sys_oper_log.oper_time IS '操作时间';
COMMENT ON COLUMN sys_oper_log.ip_address IS 'IP地址';

-- ============================================
-- 7. Dify配置表
-- ============================================
DROP TABLE IF EXISTS sys_model_config CASCADE;
CREATE TABLE sys_model_config (
    id BIGSERIAL PRIMARY KEY,
    dify_base_url VARCHAR(200),
    dify_app_api_key VARCHAR(200),
    dify_dataset_api_key VARCHAR(200),
    dify_dataset_id VARCHAR(100),
    enable SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_model_config IS 'Dify配置表';
COMMENT ON COLUMN sys_model_config.id IS '配置ID';
COMMENT ON COLUMN sys_model_config.dify_base_url IS 'Dify服务地址';
COMMENT ON COLUMN sys_model_config.dify_app_api_key IS 'App API Key';
COMMENT ON COLUMN sys_model_config.dify_dataset_api_key IS 'Dataset API Key';
COMMENT ON COLUMN sys_model_config.dify_dataset_id IS 'Dataset ID';
COMMENT ON COLUMN sys_model_config.enable IS '是否启用: 0-禁用 1-启用';
COMMENT ON COLUMN sys_model_config.create_time IS '创建时间';

-- ============================================
-- 8. 系统参数表
-- ============================================
DROP TABLE IF EXISTS sys_config CASCADE;
CREATE TABLE sys_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    remark VARCHAR(200),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_config IS '系统参数表';
COMMENT ON COLUMN sys_config.id IS '配置ID';
COMMENT ON COLUMN sys_config.config_key IS '配置键';
COMMENT ON COLUMN sys_config.config_value IS '配置值';
COMMENT ON COLUMN sys_config.remark IS '备注';
COMMENT ON COLUMN sys_config.update_time IS '更新时间';

-- ============================================
-- 9. 检索历史表（新增）
-- ============================================
DROP TABLE IF EXISTS search_history CASCADE;
CREATE TABLE search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    query_text VARCHAR(500),
    equip_model VARCHAR(100),
    image_url VARCHAR(500),
    image_urls TEXT,
    file_types VARCHAR(500),
    file_names VARCHAR(1000),
    result_answer TEXT,
    conversation_id VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE search_history IS '检索历史表';
COMMENT ON COLUMN search_history.id IS '记录ID';
COMMENT ON COLUMN search_history.user_id IS '用户ID';
COMMENT ON COLUMN search_history.query_text IS '查询文本';
COMMENT ON COLUMN search_history.equip_model IS '设备型号';
COMMENT ON COLUMN search_history.image_url IS '图片URL';
COMMENT ON COLUMN search_history.result_answer IS 'AI回答';
COMMENT ON COLUMN search_history.conversation_id IS 'Dify会话ID';
COMMENT ON COLUMN search_history.create_time IS '创建时间';

-- ============================================
-- 10. 创建索引
-- ============================================
CREATE INDEX idx_user_username ON sys_user(username);
CREATE INDEX idx_user_role_id ON sys_user(role_id);
CREATE INDEX idx_user_dept_id ON sys_user(dept_id);
CREATE INDEX idx_knowledge_audit_status ON repair_knowledge(audit_status);
CREATE INDEX idx_knowledge_equip_model ON repair_knowledge(equip_model);
CREATE INDEX idx_knowledge_upload_user ON repair_knowledge(upload_user_id);
CREATE INDEX idx_workflow_equip_model ON work_flow(equip_model);
CREATE INDEX idx_workflow_repair_level ON work_flow(repair_level);
CREATE INDEX idx_operlog_user_id ON sys_oper_log(user_id);
CREATE INDEX idx_operlog_oper_time ON sys_oper_log(oper_time);
CREATE INDEX idx_search_history_user ON search_history(user_id);
CREATE INDEX idx_search_history_time ON search_history(create_time);

-- ============================================
-- 11. 初始化数据
-- ============================================

-- 初始化角色
INSERT INTO sys_role (role_name, role_code, description) VALUES
('系统管理员', 'ADMIN', '全平台最高权限'),
('资深检修工程师', 'ENGINEER', '检修案例上传、多模态故障检索'),
('实习检修人员', 'INTERN', '仅知识库检索、查看作业指引');

-- 初始化管理员用户 (密码: Admin@123456 BCrypt加密)
INSERT INTO sys_user (username, password, nickname, role_id, dept_id, status) VALUES
('admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Kz2G3kFALYkIiRmPNvCO2', '系统管理员', 1, 1, 1);

-- 初始化设备数据
INSERT INTO equipment_info (equip_type, equip_model, dept_id) VALUES
('发动机', 'ENGINE-V8-2024', 1),
('变速箱', 'TRANS-6AT-2024', 1),
('液压系统', 'HYD-SYS-2024', 2),
('电气系统', 'ELEC-SYS-2024', 2);

-- 初始化Dify配置
INSERT INTO sys_model_config (dify_base_url, dify_app_api_key, dify_dataset_api_key, dify_dataset_id, enable) VALUES
('http://192.168.110.107', 'app-Nt08PvVSlwfR4KaS2SVW6pHG', 'dataset-fzQHmDfXpDukf9HRk9uvg3Mk', 'b4ae60fa-d748-4259-b569-0366b31aff20', 1);

-- 初始化系统参数
INSERT INTO sys_config (config_key, config_value, remark) VALUES
('search.topK', '5', '检索返回结果数量'),
('search.threshold', '0.7', '相似度阈值'),
('search.temperature', '0.7', '大模型Temperature'),
('password.expireDays', '90', '密码有效期(天)'),
('login.maxRetry', '5', '最大登录失败次数'),
('login.lockMinutes', '15', '锁定时间(分钟)');

-- 初始化作业流程示例
INSERT INTO work_flow (equip_model, repair_level, step_json, version, create_user) VALUES
('ENGINE-V8-2024', '日常保养', '[
    {"title": "安全检查", "description": "确认设备已停机，切断电源，挂警示牌"},
    {"title": "外观检查", "description": "检查发动机外观是否有漏油、漏水现象"},
    {"title": "机油检查", "description": "检查机油液位和油质，必要时更换"},
    {"title": "滤清器检查", "description": "检查空气滤清器、机油滤清器是否需要更换"},
    {"title": "皮带检查", "description": "检查各传动皮带张紧度和磨损情况"},
    {"title": "冷却液检查", "description": "检查冷却液液位和浓度"},
    {"title": "启动测试", "description": "恢复供电，启动设备运行测试"},
    {"title": "记录归档", "description": "填写保养记录，归档保存"}
]', 1, 1);

-- ============================================
-- 12. AI反馈表
-- ============================================
DROP TABLE IF EXISTS ai_feedback CASCADE;
CREATE TABLE ai_feedback (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    search_history_id BIGINT,
    original_answer TEXT,
    rating SMALLINT,
    corrected_answer TEXT,
    feedback_type VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ai_feedback IS 'AI反馈表';
COMMENT ON COLUMN ai_feedback.id IS '反馈ID';
COMMENT ON COLUMN ai_feedback.user_id IS '用户ID';
COMMENT ON COLUMN ai_feedback.search_history_id IS '检索历史ID';
COMMENT ON COLUMN ai_feedback.original_answer IS '原始AI回答';
COMMENT ON COLUMN ai_feedback.rating IS '评分: 1-正确 0-错误';
COMMENT ON COLUMN ai_feedback.corrected_answer IS '修正后的回答';
COMMENT ON COLUMN ai_feedback.feedback_type IS '反馈类型';
COMMENT ON COLUMN ai_feedback.create_time IS '创建时间';

CREATE INDEX idx_ai_feedback_user ON ai_feedback(user_id);
CREATE INDEX idx_ai_feedback_history ON ai_feedback(search_history_id);

COMMENT ON TABLE work_flow IS '标准化作业表';

-- ============================================
-- 13. 菜单表
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
-- 14. 角色菜单关联表
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
(30, 5, '用户管理', '/user', 'UserManagement', NULL, 1),
(31, 5, '角色管理', '/role', 'RoleManagement', NULL, 2),
(32, 5, '系统配置', '/system', 'System', NULL, 3),
(34, 5, '菜单管理', '/menu', 'MenuManagement', NULL, 4);

-- ============================================
-- 初始化角色菜单关联
-- ============================================

-- 系统管理员 (roleId=1): 拥有所有菜单权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
(1, 10), (1, 11), (1, 20), (1, 21), (1, 30), (1, 31), (1, 32), (1, 34);

-- 资深检修工程师 (roleId=2): 数据看板、多模态检索、作业管理(检修作业指引)、知识库(知识库管理、设备管理)、个人中心
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 6),
(2, 10), (2, 20), (2, 21);

-- 实习检修人员 (roleId=3): 数据看板、多模态检索、知识库(只读)、个人中心
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 1), (3, 2), (3, 4), (3, 6),
(3, 20), (3, 21);

-- ============================================
-- 15. 对话附件文件表
-- ============================================
DROP TABLE IF EXISTS chat_file CASCADE;
CREATE TABLE chat_file (
    id BIGSERIAL PRIMARY KEY,
    chat_history_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(20) NOT NULL,
    dify_file_id VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE chat_file IS '对话附件文件表';
COMMENT ON COLUMN chat_file.id IS '文件ID';
COMMENT ON COLUMN chat_file.chat_history_id IS '关联对话历史ID';
COMMENT ON COLUMN chat_file.file_name IS '文件名称';
COMMENT ON COLUMN chat_file.file_path IS '文件存储路径';
COMMENT ON COLUMN chat_file.file_size IS '文件大小(字节)';
COMMENT ON COLUMN chat_file.file_type IS '文件类型: image/video/audio';
COMMENT ON COLUMN chat_file.dify_file_id IS 'Dify文件ID';
COMMENT ON COLUMN chat_file.create_time IS '上传时间';

CREATE INDEX idx_chat_file_history ON chat_file(chat_history_id);
